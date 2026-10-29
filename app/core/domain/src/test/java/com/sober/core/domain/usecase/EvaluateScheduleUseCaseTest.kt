package com.sober.core.domain.usecase

import com.sober.core.domain.model.ScheduleRule
import com.sober.core.domain.model.formatTimeOfDay
import com.sober.core.domain.model.isActiveAt
import com.sober.core.domain.model.minutesOfDay
import com.sober.core.domain.model.windowEndingAt
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class EvaluateScheduleUseCaseTest {

    // A fixed zone keeps the assertions independent of the machine running the
    // tests. No DST rules, so "plus one hour" is unambiguous.
    private val zone: ZoneId = ZoneId.of("UTC")

    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 3, day, hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    // 2026-03-02 is a Monday (ISO day 1), so `day` doubles as an offset from it.
    private fun monday(hour: Int, minute: Int = 0) = at(2, hour, minute)

    private fun rule(
        name: String = "Work Mode",
        packages: List<String> = listOf("com.instagram.android"),
        days: List<Int> = listOf(1),
        start: String = "09:00",
        end: String = "18:00",
        active: Boolean = true
    ) = ScheduleRule(
        id = 1,
        ruleName = name,
        blockedPackages = packages,
        daysOfWeek = days,
        startTime = start,
        endTime = end,
        isActive = active
    )

    private fun useCase(vararg rules: ScheduleRule) =
        EvaluateScheduleUseCase(FakeScheduleRuleRepository(rules.toList()))

    @Test
    fun `inside the window on a selected day matches`() = runTest {
        val match = useCase(rule())("com.instagram.android", monday(12), zone)

        assertEquals("Work Mode", match?.rule?.ruleName)
    }

    @Test
    fun `the start minute is inside the window and the end minute is not`() = runTest {
        val evaluate = useCase(rule())
        assertTrue(evaluate("com.instagram.android", monday(9), zone) != null)
        assertNull(evaluate("com.instagram.android", monday(18), zone))
    }

    @Test
    fun `a day that is not selected never matches`() = runTest {
        // Tuesday the 3rd, same clock time as a matching Monday.
        assertNull(useCase(rule())("com.instagram.android", at(3, 12), zone))
    }

    @Test
    fun `an empty day list means every day`() = runTest {
        val evaluate = useCase(rule(days = emptyList()))

        assertTrue(evaluate("com.instagram.android", at(3, 12), zone) != null)
        assertTrue(evaluate("com.instagram.android", at(8, 12), zone) != null)
    }

    @Test
    fun `an app outside the rule's package list is not blocked`() = runTest {
        assertNull(useCase(rule())("com.google.android.youtube", monday(12), zone))
    }

    @Test
    fun `an inactive rule is skipped`() = runTest {
        assertNull(useCase(rule(active = false))("com.instagram.android", monday(12), zone))
    }

    @Test
    fun `the reported end is today's wall clock for a same-day window`() = runTest {
        val match = useCase(rule())("com.instagram.android", monday(12), zone)

        assertEquals(monday(18), match?.endsAt)
    }

    @Test
    fun `a window crossing midnight is active after it starts`() = runTest {
        val rule = rule(name = "Sleep", start = "22:00", end = "08:00")

        val match = useCase(rule)("com.instagram.android", monday(23), zone)

        // Ends at 08:00 on Tuesday the 3rd.
        assertEquals(at(3, 8), match?.endsAt)
    }

    @Test
    fun `a window crossing midnight stays active into the next morning`() = runTest {
        val rule = rule(name = "Sleep", start = "22:00", end = "08:00")

        val match = useCase(rule)("com.instagram.android", at(3, 3), zone)

        // Still Monday night's window: it closes at 08:00 Tuesday.
        assertEquals(at(3, 8), match?.endsAt)
    }

    @Test
    fun `a crossing window belongs to the day it starts, not the day it ends`() = runTest {
        // Monday-only Sleep rule. Tuesday 03:00 is inside it; Wednesday 03:00 is
        // not, because Tuesday is not a selected day.
        val evaluate = useCase(rule(name = "Sleep", days = listOf(1), start = "22:00", end = "08:00"))

        assertTrue(evaluate("com.instagram.android", at(3, 3), zone) != null)
        assertNull(evaluate("com.instagram.android", at(4, 3), zone))
    }

    @Test
    fun `a zero-length window blocks nothing`() = runTest {
        val equal = rule(start = "09:00", end = "09:00")

        assertNull(useCase(equal)("com.instagram.android", monday(9, 30), zone))
    }

    @Test
    fun `a malformed time never blocks`() = runTest {
        assertNull(useCase(rule(start = "9am"))("com.instagram.android", monday(12), zone))
        assertNull(useCase(rule(end = "25:00"))("com.instagram.android", monday(12), zone))
    }

    @Test
    fun `the window that ends soonest wins when two rules overlap`() = runTest {
        val long = rule(name = "Long", start = "09:00", end = "18:00")
        val short = rule(name = "Short", start = "12:00", end = "13:00")

        val match = useCase(long, short)("com.instagram.android", monday(12, 30), zone)

        assertEquals("Short", match?.rule?.ruleName)
    }

    @Test
    fun `window helpers agree with the use case`() {
        val work = rule()

        assertTrue(work.isActiveAt(monday(12), zone))
        assertEquals(monday(18), work.windowEndingAt(monday(12), zone))
        assertFalse(work.isActiveAt(monday(8, 59), zone))
    }

    @Test
    fun `time strings round-trip through the parser`() {
        assertEquals(0, minutesOfDay("00:00"))
        assertEquals(23 * 60 + 59, minutesOfDay("23:59"))
        assertNull(minutesOfDay("24:00"))
        assertNull(minutesOfDay("12:60"))
        assertNull(minutesOfDay(""))
        assertNull(minutesOfDay("12"))
        assertEquals("05:07", formatTimeOfDay(5, 7))
    }
}
