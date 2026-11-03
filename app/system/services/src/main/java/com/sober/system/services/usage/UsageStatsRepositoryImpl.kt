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
 *     used today. Folded from `queryEvents`, not `queryUsageStats`: the latter
 *     is bucketed by calendar day and expands the requested range to the
 *     nearest whole bucket, so asking for "today at 6am" can return hours that
 *     were used yesterday. Events carry timestamps, so clipping them to the
 *     range gives a total that cannot exceed the time elapsed today.
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
    override val excludedPackages: Set<String> by lazy {
        buildSet {
            add(context.packageName)
            homePackage?.let(::add)
        }
    }

    /**
     * The one ignored package whose pause means "the app underneath came
     * back". Sober's own pause must not be read that way: opening Sober and
     * then pressing Home would resurrect the app the user just left.
     */
    private val launcherPackages: Set<String> by lazy {
        setOfNotNull(homePackage)
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

    /**
     * The last real app on screen, kept while the launcher is. On devices whose
     * overview belongs to the launcher, closing it resumes the app underneath
     * without a resume event — only the launcher's own pause — so this is the
     * only way the poll can learn the app came back.
     */
    private var cachedLastApp: String? = null
    private var cacheInitialized = false

    override fun getUsageStatsForToday(): List<AppUsage> {
        val startTime = startOfToday()
        val endTime = System.currentTimeMillis()

        // A locked device returns null from queryEvents; reporting nothing is
        // better than inventing totals from a coarser source.
        val events = readEvents(startTime, endTime) ?: return emptyList()

        return aggregateUsageByPackage(events, startTime, endTime).map { (packageName, usage) ->
            AppUsage(
                packageName = packageName,
                totalTimeInForeground = usage.millis,
                lastTimeUsed = usage.lastResumedAt
            )
        }
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
            val replay = replayForegroundState(
                history,
                initialForeground = null,
                initialLastApp = null,
                ignoredPackages = excludedPackages,
                launcherPackages = launcherPackages
            )
            cachedForeground = replay.foreground
            cachedLastApp = replay.lastApp
            return cachedForeground
        }

        val recent = readEvents(endTime - FOREGROUND_LOOKBACK_MS, endTime) ?: return null
        // Nothing happened: whatever was on screen when the last poll ran still
        // is. Replaying an empty window would only repeat the cached answer.
        if (recent.isEmpty()) return cachedForeground

        val replay = replayForegroundState(
            recent,
            initialForeground = cachedForeground,
            initialLastApp = cachedLastApp,
            ignoredPackages = excludedPackages,
            launcherPackages = launcherPackages
        )
        cachedForeground = replay.foreground
        cachedLastApp = replay.lastApp
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
            transitions += ForegroundEvent(
                kind = kind,
                packageName = event.packageName.orEmpty(),
                timestampMillis = event.timeStamp
            )
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

    /**
     * Counts keyguard dismissals.
     *
     * `KEYGUARD_HIDDEN` is API 28+, so older devices report null rather than a
     * fabricated zero — "0 unlocks" would be a claim, and this is not one the
     * platform can support there.
     */
    override fun getScreenUnlockCount(fromMillis: Long, toMillis: Long): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        if (toMillis <= fromMillis) return 0
        return try {
            val events = usageStatsManager.queryEvents(fromMillis, toMillis) ?: return null
            var count = 0
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) count++
            }
            count
        } catch (e: SecurityException) {
            null
        }
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
    val packageName: String = "",
    val timestampMillis: Long = 0L
)

/**
 * The answer of a replay: what is on screen and, separately, the last real app
 * it saw. The second value outlives a launcher visit, which is what lets a
 * later replay recognise the app coming back.
 */
internal data class ForegroundReplay(
    val foreground: String?,
    val lastApp: String?
)

/**
 * Folds [events], in timestamp order, into the package on screen.
 *
 * [initialForeground] and [initialLastApp] carry the previous call's answer.
 * Events only report changes, so starting from the last known screen is what
 * keeps an eventless stretch of use resolving to the app that is open.
 * [ignoredPackages] (Sober itself and the launcher) map to "nothing to block".
 *
 * A [launcherPackages] pause with nothing resuming is read as the app underneath
 * coming back. Launcher-hosted overviews resume the app without a resume event,
 * so without this rule the poll reports "nothing on screen" from then on and a
 * block that had already cleared for the launcher cannot return.
 */
internal fun replayForegroundState(
    events: List<ForegroundEvent>,
    initialForeground: String?,
    initialLastApp: String?,
    ignoredPackages: Set<String>,
    launcherPackages: Set<String>
): ForegroundReplay {
    var foreground = initialForeground
    var lastApp = initialLastApp ?: initialForeground?.takeUnless { it in ignoredPackages }

    events.forEach { event ->
        when (event.kind) {
            ForegroundEventKind.RESUMED ->
                when {
                    event.packageName.isEmpty() -> foreground = null
                    event.packageName in ignoredPackages -> foreground = null
                    else -> {
                        foreground = event.packageName
                        lastApp = event.packageName
                    }
                }
            // Only clear if the pausing app is the one we think is on screen; a
            // stale background event from another app must not wipe it.
            ForegroundEventKind.PAUSED ->
                when {
                    event.packageName == foreground -> foreground = null
                    event.packageName in launcherPackages && foreground == null ->
                        foreground = lastApp
                    else -> {}
                }
            // The lock screen is a real "nothing on screen", unlike the
            // launcher: the app underneath must not be restored on return.
            ForegroundEventKind.SCREEN_OFF -> {
                foreground = null
                lastApp = null
            }
        }
    }
    return ForegroundReplay(foreground = foreground, lastApp = lastApp)
}

/** [replayForegroundState] without the carried state, for a standalone replay. */
internal fun replayForeground(
    events: List<ForegroundEvent>,
    initial: String?,
    ignoredPackages: Set<String>,
    launcherPackages: Set<String> = ignoredPackages
): String? = replayForegroundState(
    events = events,
    initialForeground = initial,
    initialLastApp = null,
    ignoredPackages = ignoredPackages,
    launcherPackages = launcherPackages
).foreground

internal data class PackageUsage(val millis: Long, val lastResumedAt: Long)

/**
 * Folds [events] into per-package foreground time within [fromMillis, toMillis].
 *
 * Same pairing rules as [replayForeground]: a pause only closes the app that
 * is open, and an interval still open at [toMillis] is closed there so an app
 * on screen right now counts. Intervals are clipped to the range, so the sum
 * can never exceed the time elapsed in it.
 *
 * An app already on screen at [fromMillis] has no resume inside the range and
 * therefore starts counting at its next resume; the rollup in
 * `UsageSessionRepository` covers the stretch before that. Under-counting is
 * deliberate: an inflated total would enforce a limit early.
 */
internal fun aggregateUsageByPackage(
    events: List<ForegroundEvent>,
    fromMillis: Long,
    toMillis: Long
): Map<String, PackageUsage> {
    if (toMillis <= fromMillis) return emptyMap()

    val totals = HashMap<String, Long>()
    val lastResumed = HashMap<String, Long>()
    var currentPackage: String? = null
    var currentStart = 0L

    fun closeCurrent(at: Long) {
        val pkg = currentPackage ?: return
        val start = currentStart.coerceAtLeast(fromMillis)
        val end = at.coerceAtMost(toMillis)
        if (end > start) totals[pkg] = (totals[pkg] ?: 0L) + (end - start)
        currentPackage = null
        currentStart = 0L
    }

    events.forEach { event ->
        val timestamp = event.timestampMillis.coerceIn(fromMillis, toMillis)
        when (event.kind) {
            ForegroundEventKind.RESUMED -> {
                closeCurrent(timestamp)
                if (event.packageName.isNotEmpty()) {
                    currentPackage = event.packageName
                    currentStart = timestamp
                    lastResumed[event.packageName] = timestamp
                }
            }
            ForegroundEventKind.PAUSED ->
                if (event.packageName == currentPackage) closeCurrent(timestamp)
            ForegroundEventKind.SCREEN_OFF -> closeCurrent(timestamp)
        }
    }
    closeCurrent(toMillis)

    return totals.mapValues { (packageName, millis) ->
        PackageUsage(millis = millis, lastResumedAt = lastResumed[packageName] ?: 0L)
    }
}