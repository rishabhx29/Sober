package com.sober.core.domain.usecase

import com.sober.core.domain.repository.UsageSessionRepository
import javax.inject.Inject

/**
 * Per-day usage for a single app over the last [days] days, oldest first.
 *
 * The app detail sheet draws this as a mini bar chart. Days with no rows are
 * zero-filled so the chart always shows the same number of columns.
 */
class GetAppUsageHistoryUseCase @Inject constructor(
    private val usageSessionRepository: UsageSessionRepository
) {
    suspend operator fun invoke(packageName: String, days: Int = 7): List<Int> {
        require(days > 0) { "days must be positive" }
        val dates = GetUsageHistoryUseCase.recentDates(days)

        val rows = runCatching {
            usageSessionRepository.getUsageForDates(dates)
        }.getOrDefault(emptyList())

        val minutesByDate = rows.asSequence()
            .filter { it.packageName == packageName }
            .groupBy { it.date }
            .mapValues { (_, dayRows) -> dayRows.sumOf { it.usedMinutes } }

        return dates.map { minutesByDate[it] ?: 0 }
    }
}
