package com.sober.core.domain.usecase

import com.sober.core.domain.model.ScheduleSuggestion
import com.sober.core.domain.model.ScheduleSuggestionKind
import com.sober.core.domain.model.UsageInterval
import com.sober.core.domain.repository.ScheduleRuleRepository
import com.sober.core.domain.repository.ScheduleSuggestionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import javax.inject.Inject

/**
 * Suggests a schedule when the last week of foreground events shows a pattern.
 *
 * Only two patterns are offered, and both are concrete: repeated late-night
 * use, and a lot of use during weekday working hours. A suggestion is raised
 * only when the pattern is strong enough to be worth acting on, is not already
 * covered by an existing rule, and has not been dismissed before.
 *
 * Everything is computed from `UsageEvents` on the device. Nothing is uploaded
 * — the app has no network permission at all.
 */
class GetScheduleSuggestionsUseCase @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val scheduleRuleRepository: ScheduleRuleRepository,
    private val suggestionRepository: ScheduleSuggestionRepository
) {

    suspend operator fun invoke(
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<ScheduleSuggestion> {
        val intervals = runCatching {
            usageStatsRepository.getForegroundIntervals(
                fromMillis = GetUsageHeatmapUseCase.startOfDaysAgo(
                    days = LOOKBACK_DAYS,
                    zone = zone,
                    nowMillis = nowMillis
                ),
                toMillis = nowMillis
            )
        }.getOrDefault(emptyList())

        if (intervals.isEmpty()) return emptyList()

        val dismissed = runCatching { suggestionRepository.getDismissed().first() }
            .getOrDefault(emptySet())
        val existingNames = runCatching { scheduleRuleRepository.getAllRules().first() }
            .getOrDefault(emptyList())
            .map { it.ruleName }
            .toSet()

        return buildList {
            if (isEligible(ScheduleSuggestionKind.LATE_NIGHT, dismissed, existingNames)) {
                val lateNights = lateNightDates(intervals, NIGHT_CUTOFF_HOUR, zone).size
                if (lateNights >= LATE_NIGHT_NIGHTS) {
                    add(
                        SLEEP.copy(
                            message = "You used your phone after 10 PM on $lateNights of the " +
                                "last 7 nights. Sleep Mode blocks social and video apps " +
                                "from 10 PM to 8 AM."
                        )
                    )
                }
            }

            if (isEligible(ScheduleSuggestionKind.WORK_HOURS, dismissed, existingNames)) {
                val workDays = minutesInWindowByDate(
                    intervals,
                    startHour = WORK_START_HOUR,
                    endHour = WORK_END_HOUR,
                    zone = zone
                )
                    .filterKeys { date ->
                        val day = runCatching { LocalDate.parse(date).dayOfWeek.value }.getOrNull()
                        day in MONDAY..FRIDAY
                    }
                    .filterValues { it >= WORK_MINUTES }

                if (workDays.size >= WORK_DAYS) {
                    val average = workDays.values.average().roundToInt()
                    add(
                        WORK.copy(
                            message = "You used your phone about $average min during work " +
                                "hours on ${workDays.size} weekdays. Work Mode blocks " +
                                "distracting apps Mon–Fri, 9 AM–6 PM."
                        )
                    )
                }
            }
        }
    }

    private fun isEligible(
        kind: ScheduleSuggestionKind,
        dismissed: Set<ScheduleSuggestionKind>,
        existingNames: Set<String>
    ): Boolean = kind !in dismissed && ruleNameFor(kind) !in existingNames

    private fun ruleNameFor(kind: ScheduleSuggestionKind): String = when (kind) {
        ScheduleSuggestionKind.LATE_NIGHT -> SLEEP.ruleName
        ScheduleSuggestionKind.WORK_HOURS -> WORK.ruleName
    }

    companion object {
        const val LOOKBACK_DAYS = 7
        const val LATE_NIGHT_NIGHTS = 3
        const val WORK_START_HOUR = 9
        const val WORK_END_HOUR = 18
        const val WORK_MINUTES = 120
        const val WORK_DAYS = 4

        private const val MONDAY = 1
        private const val FRIDAY = 5

        private val SLEEP = ScheduleSuggestion(
            kind = ScheduleSuggestionKind.LATE_NIGHT,
            title = "You're often up late",
            message = "",
            ruleName = "Sleep Mode",
            daysOfWeek = emptyList(),
            startTime = "22:00",
            endTime = "08:00"
        )

        private val WORK = ScheduleSuggestion(
            kind = ScheduleSuggestionKind.WORK_HOURS,
            title = "Work hours look distracted",
            message = "",
            ruleName = "Work Mode",
            daysOfWeek = listOf(MONDAY, 2, 3, 4, FRIDAY),
            startTime = "09:00",
            endTime = "18:00"
        )
    }
}

/**
 * Minutes of foreground use inside [startHour]..[endHour] per calendar date.
 *
 * Intervals that span the window boundary contribute only their overlap, so a
 * session already running at 9 AM is not charged for the hours before it.
 */
fun minutesInWindowByDate(
    intervals: List<UsageInterval>,
    startHour: Int,
    endHour: Int,
    zone: ZoneId = ZoneId.systemDefault()
): Map<String, Int> {
    require(startHour < endHour) { "startHour must be before endHour" }
    val millisByDate = mutableMapOf<String, Long>()

    intervals.forEach { interval ->
        if (interval.endMillis <= interval.startMillis) return@forEach
        val firstDay = Instant.ofEpochMilli(interval.startMillis).atZone(zone).toLocalDate()
        val lastDay = Instant.ofEpochMilli(interval.endMillis - 1).atZone(zone).toLocalDate()

        var day = firstDay
        while (!day.isAfter(lastDay)) {
            val windowStart = day.atTime(startHour, 0).atZone(zone).toInstant().toEpochMilli()
            val windowEnd = day.atTime(endHour, 0).atZone(zone).toInstant().toEpochMilli()
            val overlap = minOf(interval.endMillis, windowEnd) -
                maxOf(interval.startMillis, windowStart)
            if (overlap > 0) {
                millisByDate[day.toString()] = (millisByDate[day.toString()] ?: 0L) + overlap
            }
            day = day.plusDays(1)
        }
    }

    return millisByDate.mapValues { (_, millis) -> (millis / 60_000L).toInt() }
}
