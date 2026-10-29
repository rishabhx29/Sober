package com.sober.core.domain.usecase

import com.sober.core.domain.model.ScheduleRule
import com.sober.core.domain.model.ScheduleSuggestionKind
import com.sober.core.domain.model.UsageInterval
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class GetScheduleSuggestionsUseCaseTest {

    private val zone = ZoneId.of("UTC")

    /** Monday 2026-03-09, midday: the lookback week is Mar 3–9. */
    private val now: Long = LocalDateTime.of(2026, 3, 9, 12, 0)
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli()

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun day(monthDay: Int) = LocalDate.of(2026, 3, monthDay)

    private fun interval(date: LocalDate, startHour: Int, endHour: Int, endMinute: Int = 0) =
        UsageInterval(at(date, startHour), at(date, endHour, endMinute))

    private fun useCase(
        intervals: List<UsageInterval>,
        dismissed: Set<ScheduleSuggestionKind> = emptySet(),
        rules: List<ScheduleRule> = emptyList()
    ) = GetScheduleSuggestionsUseCase(
        usageStatsRepository = FakeUsageStatsRepository(intervals = intervals),
        scheduleRuleRepository = FakeScheduleRuleRepository(rules),
        suggestionRepository = FakeScheduleSuggestionRepository(dismissed)
    )

    private fun lateNights(vararg dates: LocalDate) = dates.map {
        UsageInterval(at(it, 23), at(it, 23, 30))
    }

    @Test
    fun `three late nights suggest Sleep Mode`() = runTest {
        val result = useCase(
            lateNights(day(4), day(5), day(6))
        )(nowMillis = now, zone = zone)

        val sleep = result.firstOrNull { it.kind == ScheduleSuggestionKind.LATE_NIGHT }
        assertTrue(sleep != null)
        assertEquals("Sleep Mode", sleep!!.ruleName)
        assertEquals("22:00", sleep.startTime)
        assertEquals("08:00", sleep.endTime)
        assertEquals(emptyList<Int>(), sleep.daysOfWeek)
        assertTrue(sleep.message.contains("3 of the last 7 nights"))
    }

    @Test
    fun `two late nights are not a pattern`() = runTest {
        val result = useCase(lateNights(day(4), day(6)))(nowMillis = now, zone = zone)

        assertTrue(result.none { it.kind == ScheduleSuggestionKind.LATE_NIGHT })
    }

    @Test
    fun `a dismissed suggestion is not raised again`() = runTest {
        val result = useCase(
            intervals = lateNights(day(4), day(5), day(6)),
            dismissed = setOf(ScheduleSuggestionKind.LATE_NIGHT)
        )(nowMillis = now, zone = zone)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `an existing Sleep Mode rule suppresses the suggestion`() = runTest {
        val result = useCase(
            intervals = lateNights(day(4), day(5), day(6)),
            rules = listOf(
                ScheduleRule(
                    ruleName = "Sleep Mode",
                    blockedPackages = listOf("com.instagram.android"),
                    startTime = "22:00",
                    endTime = "08:00"
                )
            )
        )(nowMillis = now, zone = zone)

        assertTrue(result.none { it.kind == ScheduleSuggestionKind.LATE_NIGHT })
    }

    @Test
    fun `four busy weekdays suggest Work Mode`() = runTest {
        val result = useCase(
            listOf(day(3), day(4), day(5), day(6)).map { interval(it, 9, 12) }
        )(nowMillis = now, zone = zone)

        val work = result.firstOrNull { it.kind == ScheduleSuggestionKind.WORK_HOURS }
        assertTrue(work != null)
        assertEquals("Work Mode", work!!.ruleName)
        assertEquals(listOf(1, 2, 3, 4, 5), work.daysOfWeek)
        assertEquals("09:00", work.startTime)
        assertEquals("18:00", work.endTime)
    }

    @Test
    fun `weekend usage does not count towards work hours`() = runTest {
        val result = useCase(
            listOf(day(3), day(4), day(5), day(7)).map { interval(it, 9, 12) }
        )(nowMillis = now, zone = zone)

        assertTrue(result.none { it.kind == ScheduleSuggestionKind.WORK_HOURS })
    }

    @Test
    fun `short weekday use does not count towards work hours`() = runTest {
        val result = useCase(
            listOf(day(3), day(4), day(5), day(6)).map { interval(it, 9, 10) }
        )(nowMillis = now, zone = zone)

        assertTrue(result.none { it.kind == ScheduleSuggestionKind.WORK_HOURS })
    }

    @Test
    fun `no intervals means no suggestions`() = runTest {
        val result = useCase(emptyList())(nowMillis = now, zone = zone)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `interval minutes are clipped to the window`() {
        val minutes = minutesInWindowByDate(
            intervals = listOf(
                UsageInterval(at(day(3), 8), at(day(3), 10)),
                UsageInterval(at(day(3), 17), at(day(3), 20))
            ),
            startHour = 9,
            endHour = 18,
            zone = zone
        )

        assertEquals(120, minutes["2026-03-03"])
    }

    @Test
    fun `interval minutes are attributed to each day they cross`() {
        val minutes = minutesInWindowByDate(
            intervals = listOf(
                UsageInterval(at(day(3), 17), at(day(4), 10))
            ),
            startHour = 9,
            endHour = 18,
            zone = zone
        )

        assertEquals(60, minutes["2026-03-03"])
        assertEquals(60, minutes["2026-03-04"])
    }

    @Test
    fun `usage entirely outside the window is ignored`() {
        val minutes = minutesInWindowByDate(
            intervals = listOf(UsageInterval(at(day(3), 20), at(day(3), 22))),
            startHour = 9,
            endHour = 18,
            zone = zone
        )

        assertTrue(minutes.isEmpty())
    }

    @Test
    fun `both suggestions can be raised together`() = runTest {
        val result = useCase(
            lateNights(day(4), day(5), day(6)) +
                listOf(day(3), day(4), day(5), day(6)).map { interval(it, 9, 12) }
        )(nowMillis = now, zone = zone)

        assertEquals(2, result.size)
        assertTrue(result.any { it.kind == ScheduleSuggestionKind.LATE_NIGHT })
        assertTrue(result.any { it.kind == ScheduleSuggestionKind.WORK_HOURS })
        assertFalse(result.any { it.message.isBlank() })
    }
}
