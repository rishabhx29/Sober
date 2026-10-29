package com.sober.core.domain.usecase

import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Total screen time for today, in minutes, across every app.
 *
 * Live UsageStatsManager data is authoritative; the `usage_sessions` total is a
 * floor, so time logged earlier today is not lost if usage access is revoked
 * partway through.
 */
class GetDailyScreenTimeUseCase @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val usageSessionRepository: UsageSessionRepository
) {
    suspend operator fun invoke(): Int {
        val live = runCatching {
            usageStatsRepository.getUsageStatsForToday()
                .sumOf { it.totalTimeInForeground / 60_000L }
                .toInt()
        }.getOrDefault(0)

        val today = SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US).format(Date())
        val stored = runCatching {
            usageSessionRepository.getAllUsageForDate(today).first().sumOf { it.usedMinutes }
        }.getOrDefault(0)

        return maxOf(live, stored)
    }
}
