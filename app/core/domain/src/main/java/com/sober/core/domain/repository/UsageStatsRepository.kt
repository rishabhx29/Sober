package com.sober.core.domain.repository

import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.UsageInterval

interface UsageStatsRepository {
    fun getUsageStatsForToday(): List<AppUsage>
    fun getUsageForApp(packageName: String): AppUsage?

    /**
     * Packages whose foreground time is not screen time the user chose: Sober
     * itself and the launcher.
     *
     * Sober is the important one. Its blocker runs a foreground service, and a
     * foreground service is reported as foreground usage, so an unfiltered
     * total charges the app for the very time it spends protecting the user —
     * and once that total crosses the cap, Sober blocks its own settings.
     */
    val excludedPackages: Set<String>

    /**
     * Foreground intervals between two instants, derived from usage-event
     * transitions.
     *
     * Totals from `queryUsageStats` say how long each app was used but not
     * *when*, so they cannot feed an hour-of-day view. Intervals can. Callers
     * should keep the range short: replaying events is linear in event count.
     */
    fun getForegroundIntervals(fromMillis: Long, toMillis: Long): List<UsageInterval>

    /**
     * The package currently in the foreground, inferred from usage-event
     * transitions (resume/pause) rather than from usage totals.
     *
     * Returns null when the foreground cannot be determined, or when Sober
     * itself is foreground. Callers must treat null as "unknown" and must not
     * act on it.
     */
    fun getForegroundPackage(): String?

    /**
     * Times the screen was unlocked in [fromMillis, toMillis], or null when the
     * platform cannot report it (below API 28). Unlocks are a coarse proxy for
     * pickups: the device records the keyguard leaving, not the intent behind
     * it.
     */
    fun getScreenUnlockCount(fromMillis: Long, toMillis: Long): Int?
}
