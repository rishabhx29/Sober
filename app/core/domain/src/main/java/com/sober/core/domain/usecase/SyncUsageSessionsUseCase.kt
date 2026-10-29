package com.sober.core.domain.usecase

import com.sober.core.domain.model.UsageSession
import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Copies today's usage totals from [UsageStatsRepository] into the
 * `usage_sessions` table.
 *
 * This is the write side of the loop that was previously missing: limits were
 * checked against `usage_sessions`, but nothing ever populated it, so every
 * limit check read an empty table and could never fire. Without this, per-app
 * enforcement cannot work regardless of how the check is called.
 *
 * Totals are written as absolute values (not increments) because
 * `UsageStatsManager` already reports cumulative daily usage — adding them would
 * compound on every poll.
 */
class SyncUsageSessionsUseCase @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val usageSessionRepository: UsageSessionRepository
) {
    suspend operator fun invoke() {
        val today = todayStamp()
        val usage = usageStatsRepository.getUsageStatsForToday()

        usage.forEach { appUsage ->
            val usedMinutes = (appUsage.totalTimeInForeground / 60_000L).toInt()
            if (usedMinutes <= 0) return@forEach

            usageSessionRepository.insertOrUpdateSession(
                UsageSession(
                    packageName = appUsage.packageName,
                    date = today,
                    usedMinutes = usedMinutes
                )
            )
        }
    }

    private fun todayStamp(): String =
        SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())

    companion object {
        const val DATE_FORMAT = "yyyy-MM-dd"
    }
}