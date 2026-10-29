package com.sober.core.domain.usecase

import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Today's usage as every enforcer must read it: the live UsageStatsManager
 * figure with the stored rollup as a floor, so a revoked usage-access grant
 * cannot make earlier hours disappear from the totals.
 *
 * Shared rather than duplicated because the block decision and the "is any
 * block in force" check must never disagree about whether a limit is reached.
 */
internal object UsageTotals {

    /** Total foreground minutes across every app today. */
    suspend fun deviceMinutesToday(
        stats: UsageStatsRepository,
        sessions: UsageSessionRepository
    ): Int {
        val live = try {
            stats.getUsageStatsForToday()
                .sumOf { it.totalTimeInForeground / 60_000L }
                .toInt()
        } catch (e: Exception) {
            0
        }

        val stored = sessions.getAllUsageForDate(today())
            .first()
            .sumOf { it.usedMinutes }

        return maxOf(live, stored)
    }

    /** Per-package foreground minutes today, live and stored maxed. */
    suspend fun perAppMinutesToday(
        stats: UsageStatsRepository,
        sessions: UsageSessionRepository
    ): Map<String, Int> {
        val live = try {
            stats.getUsageStatsForToday().associate {
                it.packageName to (it.totalTimeInForeground / 60_000L).toInt()
            }
        } catch (e: Exception) {
            emptyMap()
        }

        val stored = sessions.getAllUsageForDate(today())
            .first()
            .groupBy { it.packageName }
            .mapValues { (_, rows) -> rows.sumOf { it.usedMinutes } }

        return (live.keys + stored.keys).associateWith { packageName ->
            maxOf(live[packageName] ?: 0, stored[packageName] ?: 0)
        }
    }

    private fun today(): String =
        SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US).format(Date())
}
