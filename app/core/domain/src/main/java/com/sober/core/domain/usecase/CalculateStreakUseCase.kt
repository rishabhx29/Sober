package com.sober.core.domain.usecase

import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.FocusSessionRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.repository.StreakPreferencesRepository
import com.sober.core.domain.repository.StreakRule
import com.sober.core.domain.repository.UsageSessionRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Consecutive days the user stayed within their own rules, counting back from
 * today.
 *
 * A day counts when total usage stayed under the overall daily cap (when one
 * is set) and every active per-app limit held. This is the same comparison the
 * enforcement engine makes — `used >= limit` is a violation — so the streak
 * can never claim a day the app would have blocked.
 *
 * Under [StreakRule.LIMITS_AND_NO_CANCELLATIONS] a day also fails when the user
 * cancelled a focus session that day, matching the PRD's "a cancellation breaks
 * the streak" wording. Cancellations live in `focus_sessions`, not the usage
 * table, so they are read separately; the rule is opt-in because it makes the
 * streak depend on a different table and the user should choose that trade.
 *
 * Untracked days end the chain: `usage_sessions` only has rows for days the
 * service actually observed, so treating an empty history as "zero usage"
 * would invent a streak reaching back before install.
 */
class CalculateStreakUseCase @Inject constructor(
    private val usageSessionRepository: UsageSessionRepository,
    private val appLimitRepository: AppLimitRepository,
    private val sessionStateRepository: SessionStateRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val streakPreferencesRepository: StreakPreferencesRepository
) {
    suspend operator fun invoke(): Int =
        streakEndingOn(LocalDate.now(ZoneId.systemDefault()))

    /**
     * The same streak, but ending on [endDate] instead of today.
     *
     * Persisting a streak requires judging a day that is already over: today
     * can still cross a limit, so the record is written from
     * `streakEndingOn(yesterday)`. [endDate] is an explicit parameter rather
     * than a clock read so the window is unambiguous at the call site.
     */
    suspend fun streakEndingOn(endDate: LocalDate): Int {
        val overallLimit = sessionStateRepository.dailyScreenTimeLimitMinutes.first()
        val limits = appLimitRepository.getAllLimits().first()
            .filter { it.isActive && it.dailyLimitMinutes > 0 }

        // Nothing to stay under means nothing to succeed at.
        if (overallLimit <= 0 && limits.isEmpty()) return 0

        val dates = (MAX_LOOKBACK_DAYS - 1 downTo 0).map { offset ->
            endDate.minusDays(offset.toLong()).toString()
        }
        val rows = runCatching { usageSessionRepository.getUsageForDates(dates) }
            .getOrDefault(emptyList())
        val rowsByDate = rows.groupBy { it.date }
        val today = LocalDate.now(ZoneId.systemDefault()).toString()

        val cancelledDays =
            if (streakPreferencesRepository.rule().first() ==
                StreakRule.LIMITS_AND_NO_CANCELLATIONS
            ) {
                cancelledDays(dates)
            } else {
                emptySet()
            }

        var streak = 0
        val newestFirst = dates.asReversed()
        for (date in newestFirst) {
            // Checked before the empty-history shortcut below: a cancellation
            // today is a known fact even when today has no usage rows yet.
            if (date in cancelledDays) break

            val dayRows = rowsByDate[date]
            // Today may legitimately have no rows yet; older empty days cannot
            // be verified, so the chain stops there. Keyed on the real date,
            // not the loop index: a window ending yesterday must not treat
            // yesterday as an unobserved-but-fine day.
            if (dayRows.isNullOrEmpty()) {
                if (date == today) {
                    streak++
                    continue
                }
                break
            }

            val total = dayRows.sumOf { it.usedMinutes }
            val underOverall = overallLimit <= 0 || total < overallLimit
            val underAppLimits = limits.all { limit ->
                val appMinutes = dayRows
                    .filter { it.packageName == limit.packageName }
                    .sumOf { it.usedMinutes }
                appMinutes < limit.dailyLimitMinutes
            }

            if (!underOverall || !underAppLimits) break
            streak++
        }
        return streak
    }

    /**
     * One query for the whole window rather than one per day: the loop can run
     * sixty times and a query per day would make the Home screen noticeably
     * slower for a value the user just glances at.
     */
    private suspend fun cancelledDays(dates: List<String>): Set<String> {
        val zone = ZoneId.systemDefault()
        val from = LocalDate.parse(dates.first()).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = LocalDate.parse(dates.last()).plusDays(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

        val sessions = runCatching {
            focusSessionRepository.getCancelledBetween(from, to)
        }.getOrDefault(emptyList())

        val formatter = SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US)
        return sessions.mapNotNull { it.endedAt }
            .map { formatter.format(Date(it)) }
            .toSet()
    }

    companion object {
        /** Two months of history, enough for any streak the UI reports. */
        const val MAX_LOOKBACK_DAYS = 60
    }
}
