package com.sober.core.domain.usecase

import com.sober.core.domain.model.UsageSession
import com.sober.core.domain.repository.UsageSessionRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** One day's total usage, oldest first, with gaps zero-filled. */
data class DailyUsageTotal(
    val date: String,
    val totalMinutes: Int
)

/**
 * Aggregated usage for the last [days] days, read from the local
 * `usage_sessions` history.
 *
 * Days with no rows are zero-filled so chart x-axes stay evenly spaced, and
 * this never touches the network. [rows] is kept alongside the aggregates so a
 * caller that needs a different slice (a shorter range, one weekday) does not
 * have to query again.
 */
data class UsageHistory(
    val dailyTotals: List<DailyUsageTotal>,
    /** packageName -> minutes used across the whole range. */
    val perAppMinutes: Map<String, Int>,
    val rows: List<UsageSession> = emptyList()
) {
    val totalMinutes: Int get() = dailyTotals.sumOf { it.totalMinutes }

    val averageDailyMinutes: Int
        get() = if (dailyTotals.isEmpty()) 0 else totalMinutes / dailyTotals.size
}

class GetUsageHistoryUseCase @Inject constructor(
    private val usageSessionRepository: UsageSessionRepository
) {
    suspend operator fun invoke(days: Int): UsageHistory {
        require(days > 0) { "days must be positive" }
        val dates = recentDates(days)

        val rows = runCatching {
            usageSessionRepository.getUsageForDates(dates)
        }.getOrDefault(emptyList())

        val minutesByDate = rows.groupBy { it.date }
            .mapValues { (_, dayRows) -> dayRows.sumOf { it.usedMinutes } }
        val minutesByPackage = rows.groupBy { it.packageName }
            .mapValues { (_, appRows) -> appRows.sumOf { it.usedMinutes } }

        return UsageHistory(
            dailyTotals = dates.map { DailyUsageTotal(it, minutesByDate[it] ?: 0) },
            perAppMinutes = minutesByPackage,
            rows = rows
        )
    }

    companion object {
        /** YYYY-MM-DD strings for the last [days] days, oldest first. */
        fun recentDates(days: Int): List<String> {
            require(days > 0) { "days must be positive" }
            val formatter = SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US)
            return (days - 1 downTo 0).map { offset ->
                val calendar = Calendar.getInstance().apply {
                    time = Date()
                    add(Calendar.DAY_OF_YEAR, -offset)
                }
                formatter.format(calendar.time)
            }
        }
    }
}
