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
     * We replay the last few minutes of events rather than the whole day: the
     * window only needs to be long enough to contain the most recent
     * transition, and a short window keeps this cheap enough to call every
     * poll. Events are applied in timestamp order, so the final state wins.
     */
    override fun getForegroundPackage(): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - FOREGROUND_LOOKBACK_MS

        val events = try {
            usageStatsManager.queryEvents(startTime, endTime)
        } catch (e: SecurityException) {
            // Usage access revoked between checks; report unknown rather than
            // guessing a foreground app we cannot verify.
            return null
        } ?: return null

        var foreground: String? = null
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            when (event.eventType) {
                // MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND are the API 21+
                // constants. ACTIVITY_RESUMED / ACTIVITY_PAUSED are the API 29+
                // renamings of the same values (1 / 2) — using the older names
                // keeps this correct for minSdk 26 without a version branch.
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    // Sober's own UI is never blocked. Neither is the launcher:
                    // the overall cap applies to apps, and blocking the home
                    // screen would leave a full-screen overlay with no way to
                    // navigate anywhere.
                    foreground = if (
                        event.packageName == context.packageName ||
                        event.packageName == homePackage
                    ) {
                        null
                    } else {
                        event.packageName
                    }
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    // Only clear if the pausing app is the one we think is on
                    // screen; a stale background event from another app must not
                    // wipe it.
                    if (event.packageName == foreground) {
                        foreground = null
                    }
                }

                // SCREEN_NON_INTERACTIVE is API 28+ (minSdk is 26), so this is
                // checked behind a version guard rather than as a branch label.
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                        event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE
                    ) {
                        // Screen off: nothing is meaningfully foreground.
                        foreground = null
                    }
                }
            }
        }

        return foreground
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
         * How far back to replay events. Comfortably longer than a poll
         * interval so a transition can never fall outside the window, while
         * staying cheap to query every cycle.
         */
        private const val FOREGROUND_LOOKBACK_MS = 10 * 60 * 1000L
    }
}