package com.sober.system.services.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.UsageInterval
import com.sober.core.domain.repository.UsageStatsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject

/**
 * Wraps [UsageStatsManager].
 *
 * Two distinct jobs live here and they must not be confused:
 *
 *  1. **Usage totals** (`getUsageStatsForToday`) — how long each app has been
 *     used today. Sourced from `queryUsageStats`. Fine for reporting.
 *
 *  2. **Foreground detection** (`getForegroundPackage`) — which app is on
 *     screen *right now*. This must **not** be derived from
 *     `UsageStats.lastTimeUsed`, which is historical: it reports when an app
 *     was last used, not that it still is. Deriving foreground from it leaves
 *     the blocker acting on a stale app after the user switches away.
 *     Instead we replay recent `UsageEvents` transitions and track the last
 *     resume/pause, which is the authoritative signal available without the
 *     accessibility service.
 */
class UsageStatsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UsageStatsRepository {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /** The default home app; resolved once because it cannot change mid-process. */
    private val homePackage: String? by lazy {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
    }

    /** Sober's own UI and the launcher are never block targets. */
    private val ignoredPackages: Set<String> by lazy {
        buildSet {
            add(context.packageName)
            homePackage?.let(::add)
        }
    }

    /**
     * The answer from the previous poll.
     *
     * Usage events only describe *changes*. A user watching a long video emits
     * no event at all, so a fixed look-back window stops containing the resume
     * and the app would be reported as "nothing on screen" a few minutes in.
     * That is not merely a missed block: the poll clears the overlay for an
     * unknown screen, so a block that had already landed would vanish by
     * itself. Seeding each replay with the previous answer keeps the steady
     * state correct without widening the query every cycle.
     */
    private var cachedForeground: String? = null
    private var cacheInitialized = false

    override fun getUsageStatsForToday(): List<AppUsage> {
        val startTime = startOfToday()
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return stats?.map {
            AppUsage(
                packageName = it.packageName,
                totalTimeInForeground = it.totalTimeInForeground,
                lastTimeUsed = it.lastTimeUsed
            )
        } ?: emptyList()
    }

    override fun getUsageForApp(packageName: String): AppUsage? {
        return getUsageStatsForToday().find { it.packageName == packageName }
    }

    /**
     * Returns the package currently in the foreground, or null when it cannot
     * be determined.
     *
     * The first poll after the process starts replays a long history, because
     * the resume that put the current app on screen may be hours old and only
     * events *since* then can be replayed. Every later poll applies just the
     * recent window on top of the previous answer, which is both cheap enough
     * to run every cycle and correct while the screen is not changing.
     */
    override fun getForegroundPackage(): String? {
        val endTime = System.currentTimeMillis()

        if (!cacheInitialized) {
            val history = readEvents(endTime - INITIAL_LOOKBACK_MS, endTime) ?: return null
            cacheInitialized = true
            cachedForeground = replayForeground(history, initial = null, ignoredPackages)
            return cachedForeground
        }

        val recent = readEvents(endTime - FOREGROUND_LOOKBACK_MS, endTime) ?: return null
        // Nothing happened: whatever was on screen when the last poll ran still
        // is. Replaying an empty window would only repeat the cached answer.
        if (recent.isEmpty()) return cachedForeground

        cachedForeground = replayForeground(recent, cachedForeground, ignoredPackages)
        return cachedForeground
    }

    /** Foreground-relevant transitions in [fromMillis, toMillis], or null when unavailable. */
    private fun readEvents(fromMillis: Long, toMillis: Long): List<ForegroundEvent>? {
        val events = try {
            usageStatsManager.queryEvents(fromMillis, toMillis)
        } catch (e: SecurityException) {
            // Usage access revoked between checks; report unknown rather than
            // guessing a foreground app we cannot verify.
            return null
        } ?: return null

        val transitions = ArrayList<ForegroundEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND are the API 21+
            // constants. ACTIVITY_RESUMED / ACTIVITY_PAUSED are the API 29+
            // renamings of the same values (1 / 2) — using the older names
            // keeps this correct for minSdk 26 without a version branch.
            // SCREEN_NON_INTERACTIVE is API 28+, so it is checked behind a
            // version guard rather than as a branch label.
            val kind = when {
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND -> ForegroundEventKind.RESUMED
                event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND -> ForegroundEventKind.PAUSED
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE ->
                    ForegroundEventKind.SCREEN_OFF
                else -> continue
            }
            transitions += ForegroundEvent(kind, event.packageName.orEmpty())
        }
        return transitions
    }

    /**
     * Replays usage events and pairs each resume with the next pause to build
     * foreground intervals.
     *
     * Two pairing rules matter for correctness:
     *
     *  - A pause only closes the interval of the app that is currently open.
     *    A stale background event from a different app must not truncate the
     *    real one.
     *  - An interval still open at [toMillis] (the app is on screen right now)
     *    is closed at `toMillis`, otherwise the current session would vanish
     *    from the heatmap until the user switched away.
     *
     * Intervals are clipped to the requested range, so a session that began
     * before it does not contribute time outside it.
     */
    override fun getForegroundIntervals(fromMillis: Long, toMillis: Long): List<UsageInterval> {
        if (toMillis <= fromMillis) return emptyList()

        val events = try {
            usageStatsManager.queryEvents(fromMillis, toMillis) ?: return emptyList()
        } catch (e: SecurityException) {
            return emptyList()
        }

        val intervals = ArrayList<UsageInterval>()
        var currentPackage: String? = null
        var currentStart = 0L

        fun closeCurrent(at: Long) {
            val start = currentStart
            val pkg = currentPackage
            if (pkg != null && at > start) {
                intervals += UsageInterval(
                    startMillis = start.coerceAtLeast(fromMillis),
                    endMillis = at.coerceAtMost(toMillis)
                )
            }
            currentPackage = null
            currentStart = 0L
        }

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val timestamp = event.timeStamp.coerceIn(fromMillis, toMillis)

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    // A new resume implicitly ends whatever was before it, even
                    // if that app's pause event was never delivered.
                    closeCurrent(timestamp)
                    currentPackage = event.packageName
                    currentStart = timestamp
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (event.packageName == currentPackage) {
                        closeCurrent(timestamp)
                    }
                }

                // SCREEN_NON_INTERACTIVE is API 28+, so it is matched behind a
                // version guard instead of as a branch label. Below 28 a screen
                // that turns off while an app is open leaves that interval open
                // until the next app switch; the alternative is guessing from a
                // keyguard event that also does not exist on 26–27.
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                        event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE
                    ) {
                        closeCurrent(timestamp)
                    }
                }
            }
        }

        closeCurrent(toMillis)
        return intervals.filter { it.endMillis > it.startMillis }
    }

    private fun startOfToday(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    companion object {
        /**
         * How far back to replay events once the cached foreground is known.
         * Comfortably longer than a poll interval so a transition can never
         * fall outside the window, while staying cheap to query every cycle.
         */
        private const val FOREGROUND_LOOKBACK_MS = 10 * 60 * 1000L

        /**
         * How far back the first poll after a process start looks. A day covers
         * an app opened at any point today, including one opened just before
         * midnight and still open now.
         */
        private const val INITIAL_LOOKBACK_MS = 24 * 60 * 60 * 1000L
    }
}

internal enum class ForegroundEventKind { RESUMED, PAUSED, SCREEN_OFF }

internal data class ForegroundEvent(
    val kind: ForegroundEventKind,
    val packageName: String = ""
)

/**
 * Folds [events], in timestamp order, into the package on screen.
 *
 * [initial] is the answer from the previous call. Events only report changes,
 * so starting from the last known screen is what keeps an eventless stretch of
 * use resolving to the app that is open. [ignoredPackages] (Sober itself and
 * the launcher) map to "nothing to block".
 */
internal fun replayForeground(
    events: List<ForegroundEvent>,
    initial: String?,
    ignoredPackages: Set<String>
): String? {
    var foreground = initial
    events.forEach { event ->
        when (event.kind) {
            ForegroundEventKind.RESUMED ->
                foreground = event.packageName.takeUnless {
                    it.isEmpty() || it in ignoredPackages
                }
            // Only clear if the pausing app is the one we think is on screen; a
            // stale background event from another app must not wipe it.
            ForegroundEventKind.PAUSED ->
                if (event.packageName == foreground) foreground = null
            ForegroundEventKind.SCREEN_OFF -> foreground = null
        }
    }
    return foreground
}