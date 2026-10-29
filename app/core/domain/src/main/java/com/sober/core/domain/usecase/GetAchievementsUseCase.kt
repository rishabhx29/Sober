package com.sober.core.domain.usecase

import com.sober.core.domain.model.Achievement
import com.sober.core.domain.model.AchievementId
import com.sober.core.domain.repository.AchievementRepository
import com.sober.core.domain.repository.AppCategoryRepository
import com.sober.core.domain.repository.FocusSessionRepository
import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Evaluates the six PRD achievements and merges them with the unlocks already
 * stored on the device.
 *
 * Evaluation is derived, only the *first unlock* is persisted: the streak is
 * recomputed from history on every read, but once a badge is earned it stays
 * earned even if the user later breaks the streak. New unlocks are written from
 * here because this is the one place that knows both the rules and the store.
 */
class GetAchievementsUseCase @Inject constructor(
    private val usageSessionRepository: UsageSessionRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val appCategoryRepository: AppCategoryRepository,
    private val achievementRepository: AchievementRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase
) {

    suspend operator fun invoke(
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<Achievement> {
        val dates = GetUsageHistoryUseCase.recentDates(HISTORY_DAYS)
        val rows = runCatching {
            usageSessionRepository.getUsageForDates(dates)
        }.getOrDefault(emptyList())

        val streak = runCatching { calculateStreakUseCase() }.getOrDefault(0)
        val completedSessions = runCatching {
            focusSessionRepository.countCompletedBetween(0L, nowMillis)
        }.getOrDefault(0)

        val recentCompleted = runCatching {
            focusSessionRepository.getCompletedBetween(
                from = nowMillis - HISTORY_DAYS * DAY_MS,
                to = nowMillis
            )
        }.getOrDefault(emptyList())

        val socialPackages = runCatching {
            appCategoryRepository.getSocialPackages()
        }.getOrDefault(emptySet())

        val pomodoroDates = recentCompleted
            .filter { it.durationMinutes == POMODORO_DURATION_MINUTES }
            .mapNotNull { session -> session.endedAt?.let { dateOf(it, zone) } }
            .toSet()

        val totalByDate = rows.groupBy { it.date }
            .mapValues { (_, dayRows) -> dayRows.sumOf { it.usedMinutes } }
        val observedDates = totalByDate.filterValues { it > 0 }.keys

        // Today's night is not over yet, so the windows being judged are the
        // five complete nights before it.
        val nightDates = dates.dropLast(1).takeLast(NIGHT_OWL_NIGHTS)
        val nightStart = nightDates.firstOrNull()?.let {
            LocalDate.parse(it).atStartOfDay(zone).toInstant().toEpochMilli()
        }
        val intervals = if (nightStart == null) {
            emptyList()
        } else {
            runCatching {
                usageStatsRepository.getForegroundIntervals(nightStart, nowMillis)
            }.getOrDefault(emptyList())
        }

        val earned = evaluateAchievements(
            AchievementSnapshot(
                streakDays = streak,
                completedSessions = completedSessions,
                socialFreeDay = socialFreeDay(rows, dates, socialPackages),
                pomodoroDates = pomodoroDates,
                nightDates = nightDates,
                observedDates = observedDates,
                lateNightDates = lateNightDates(intervals, NIGHT_CUTOFF_HOUR, zone)
            )
        )

        val stored = runCatching { achievementRepository.getUnlocked().first() }
            .getOrDefault(emptyMap())

        return AchievementId.entries.map { id ->
            val unlockedAt = when {
                stored.containsKey(id) -> stored[id]
                id in earned -> nowMillis.also { at ->
                    runCatching { achievementRepository.unlock(id, at) }
                }
                else -> null
            }
            Achievement(id = id, isUnlocked = unlockedAt != null, unlockedAtMillis = unlockedAt)
        }
    }

    private fun dateOf(epochMillis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toString()

    companion object {
        /** Long enough for a 30-day streak and a month of focus history. */
        const val HISTORY_DAYS = 30
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
