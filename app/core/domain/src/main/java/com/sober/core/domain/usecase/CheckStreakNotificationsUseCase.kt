package com.sober.core.domain.usecase

import com.sober.core.domain.repository.AlertKind
import com.sober.core.domain.repository.AlertNotifier
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.NotificationPreferencesRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * The evening streak notifications.
 *
 * Runs on the enforcement poll and only between 22:00 and midnight, when the
 * day's outcome is effectively decided and a nudge can still change behaviour:
 *
 *  - a limit has been reached → "your streak ends tonight", using the streak as
 *    it stood *before* today, because today can no longer count;
 *  - no limit reached → "day N, still under every limit".
 *
 * The two are mutually exclusive, and each is recorded as a fired key for the
 * day, so a 15-second poll cannot turn into a stream of notifications. Whether
 * a limit is reached is computed with the same `UsageTotals` helper the block
 * policy uses, so the message cannot contradict the overlay.
 */
class CheckStreakNotificationsUseCase @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val usageSessionRepository: UsageSessionRepository,
    private val appLimitRepository: AppLimitRepository,
    private val sessionStateRepository: SessionStateRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val alertNotifier: AlertNotifier,
    private val notificationPreferences: NotificationPreferencesRepository
) {
    suspend operator fun invoke(
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ) {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        if (now.hour * 60 + now.minute < WINDOW_START_MINUTE) return

        val today = now.toLocalDate()
        val day = SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US)
            .format(Date(nowMillis))
        val warned = sessionStateRepository.warnedUsageKeys(day)

        val perApp = UsageTotals.perAppMinutesToday(usageStatsRepository, usageSessionRepository)
        val total = UsageTotals.deviceMinutesToday(usageStatsRepository, usageSessionRepository)

        if (anyLimitReached(perApp, total)) {
            if (STREAK_RISK_KEY in warned) return
            if (!notificationPreferences.isEnabled(AlertKind.STREAK_WARNING).first()) return

            val priorStreak = calculateStreakUseCase.streakEndingOn(today.minusDays(1))
            if (priorStreak <= 0) return

            alertNotifier.notifyStreakAtRisk(priorStreak)
            sessionStateRepository.markUsageWarningFired(day, STREAK_RISK_KEY)
            return
        }

        if (DAILY_STREAK_KEY in warned) return
        if (!notificationPreferences.isEnabled(AlertKind.DAILY_STREAK).first()) return

        // No observed usage means the day cannot be verified: an untouched
        // phone and an engine that never ran look identical in the tables, and
        // congratulating the second one would be inventing a streak.
        if (total <= 0) return

        // Today-inclusive: a zero here means the day is already over a limit.
        val streak = calculateStreakUseCase.streakEndingOn(today)
        if (streak <= 0) return

        alertNotifier.notifyDailyStreak(streak)
        sessionStateRepository.markUsageWarningFired(day, DAILY_STREAK_KEY)
    }

    private suspend fun anyLimitReached(perApp: Map<String, Int>, total: Int): Boolean {
        val cap = sessionStateRepository.dailyScreenTimeLimitMinutes.first()
        val limits = appLimitRepository.getAllLimits().first()
            .filter { it.isActive && it.dailyLimitMinutes > 0 }

        // With nothing configured there is nothing to stay under, so there is
        // no streak to defend either.
        if (cap <= 0 && limits.isEmpty()) return false

        val overallReached = cap > 0 && total >= cap
        val appReached = limits.any { (perApp[it.packageName] ?: 0) >= it.dailyLimitMinutes }
        return overallReached || appReached
    }

    private companion object {
        /** 22:00 local time. */
        const val WINDOW_START_MINUTE = 22 * 60

        const val STREAK_RISK_KEY = "streak_risk"
        const val DAILY_STREAK_KEY = "streak_daily"
    }
}
