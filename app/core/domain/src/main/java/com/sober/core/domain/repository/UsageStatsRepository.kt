package com.sober.core.domain.repository

import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.UsageInterval

interface UsageStatsRepository {
    fun getUsageStatsForToday(): List<AppUsage>
    fun getUsageForApp(packageName: String): AppUsage?

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
}
