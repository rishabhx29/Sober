package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.model.UsageSession
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
        val dates = (MAX_LOOKBACK_DAYS - 1 downTo 0).map { offset ->
            endDate.minusDays(offset.toLong()).toString()
        }
        val rules = cleanRules(dates) ?: return 0
        val rows = runCatching { usageSessionRepository.getUsageForDates(dates) }
            .getOrDefault(emptyList())
        val rowsByDate = rows.groupBy { it.date }
        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        val cancelledDays = rules.cancelledDates

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

            if (!stayedUnderLimits(dayRows, rules)) break
            streak++
        }
        return streak
    }

    /**
     * Which of [dates] were clean, by exactly the rule [streakEndingOn] applies.
     *
     * Challenges need the per-day answer rather than a run length, because a
     * failure in the middle must end the challenge at that date. Unlike the
     * streak, today is never folded in: a challenge only counts whole days, so
     * this returns an empty set for a day that has not finished. An unobserved
     * day is not clean — the service did not see it, and treating silence as
     * success would hand out a week on a dead service.
     */
    suspend fun cleanDates(dates: List<String>): Set<String> {
        if (dates.isEmpty()) return emptySet()
        val rules = cleanRules(dates) ?: return emptySet()
        val rows = runCatching { usageSessionRepository.getUsageForDates(dates) }
            .getOrDefault(emptyList())
            .groupBy { it.date }

        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        return dates.filterTo(mutableSetOf()) { date ->
            if (date == today || date in rules.cancelledDates) return@filterTo false
            val dayRows = rows[date] ?: return@filterTo false
            stayedUnderLimits(dayRows, rules)
        }
    }

    /**
     * The limits a day is judged against, or null when there are none.
     *
     * Nothing to stay under means nothing to succeed at: without a cap or an
     * active per-app limit every day would be trivially clean, which would turn
     * the streak and the clean-week challenge into fiction.
     */
    private suspend fun cleanRules(dates: List<String>): CleanRules? {
        val overallLimit = sessionStateRepository.dailyScreenTimeLimitMinutes.first()
        val limits = appLimitRepository.getAllLimits().first()
            .filter { it.isActive && it.dailyLimitMinutes > 0 }
        if (overallLimit <= 0 && limits.isEmpty()) return null

        val cancelled =
            if (streakPreferencesRepository.rule().first() ==
                StreakRule.LIMITS_AND_NO_CANCELLATIONS
            ) {
                cancelledDays(dates)
            } else {
                emptySet()
            }

        return CleanRules(overallLimit, limits, cancelled)
    }

    private fun stayedUnderLimits(rows: List<UsageSession>, rules: CleanRules): Boolean {
        val total = rows.sumOf { it.usedMinutes }
        val underOverall = rules.overallLimit <= 0 || total < rules.overallLimit
        val underAppLimits = rules.limits.all { limit ->
            val appMinutes = rows
                .filter { it.packageName == limit.packageName }
                .sumOf { it.usedMinutes }
            appMinutes < limit.dailyLimitMinutes
        }
        return underOverall && underAppLimits
    }

    private data class CleanRules(
        val overallLimit: Int,
        val limits: List<AppLimit>,
        val cancelledDates: Set<String>
    )

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
