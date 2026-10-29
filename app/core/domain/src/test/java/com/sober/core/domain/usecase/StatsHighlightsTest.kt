package com.sober.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsHighlightsTest {

    private fun day(date: String, minutes: Int) = DailyUsageTotal(date, minutes)

    /**
     * 2026-03-02 is a Monday; the list runs oldest first, the way the history
     * use case returns it.
     */
    private val week = listOf(
        day("2026-03-02", 120),
        day("2026-03-03", 30),
        day("2026-03-04", 200),
        day("2026-03-05", 0),
        day("2026-03-06", 45),
        day("2026-03-07", 90),
        day("2026-03-08", 60)
    )

    @Test
    fun `the quietest day with any usage wins`() {
        assertEquals("Tuesday", mostProductiveWeekday(week))
    }

    @Test
    fun `days without usage are not productive days`() {
        val totals = listOf(
            day("2026-03-02", 90),
            day("2026-03-03", 0),
            day("2026-03-04", 10)
        )

        assertEquals("Wednesday", mostProductiveWeekday(totals))
    }

    @Test
    fun `a tie goes to the most recent day`() {
        val totals = listOf(
            day("2026-03-02", 20),
            day("2026-03-05", 20)
        )

        assertEquals("Thursday", mostProductiveWeekday(totals))
    }

    @Test
    fun `no usage at all has no productive day`() {
        assertNull(mostProductiveWeekday(emptyList()))
        assertNull(mostProductiveWeekday(listOf(day("2026-03-02", 0))))
    }

    @Test
    fun `week over week needs two full weeks`() {
        assertNull(weekOverWeekChange(week.take(7)))
        assertNull(weekOverWeekChange(week.take(13)))
    }

    @Test
    fun `week over week compares the last week against the one before it`() {
        val history = List(14) { index ->
            day("2026-03-${(index + 1).toString().padStart(2, '0')}", if (index < 7) 100 else 50)
        }

        assertEquals(-50, weekOverWeekChange(history))
    }

    @Test
    fun `an increase is reported as a positive percentage`() {
        val history = List(14) { index ->
            day("2026-03-${(index + 1).toString().padStart(2, '0')}", if (index < 7) 100 else 150)
        }

        assertEquals(50, weekOverWeekChange(history))
    }

    @Test
    fun `a week without usage has no baseline to compare against`() {
        val history = List(14) { index ->
            day("2026-03-${(index + 1).toString().padStart(2, '0')}", if (index < 7) 0 else 100)
        }

        assertNull(weekOverWeekChange(history))
    }

    @Test
    fun `adherence counts days at or under the limit`() {
        val totals = listOf(
            day("2026-03-02", 30),
            day("2026-03-03", 60),
            day("2026-03-04", 90),
            day("2026-03-05", 0)
        )

        assertEquals(2 to 3, limitAdherence(totals, dailyLimitMinutes = 60))
    }

    @Test
    fun `adherence is unmeasurable without an overall limit`() {
        assertEquals(0 to 0, limitAdherence(week, dailyLimitMinutes = 0))
        assertEquals(0 to 0, limitAdherence(week, dailyLimitMinutes = -1))
    }

    @Test
    fun `adherence is empty when nothing was measured`() {
        assertEquals(0 to 0, limitAdherence(emptyList(), dailyLimitMinutes = 60))
    }

    @Test
    fun `the trend label describes the direction`() {
        assertEquals("No earlier week to compare", weekOverWeekLabel(null))
        assertEquals("About the same as last week", weekOverWeekLabel(2))
        assertEquals("About the same as last week", weekOverWeekLabel(-2))
        assertEquals("12% less than last week", weekOverWeekLabel(-12))
        assertEquals("8% more than last week", weekOverWeekLabel(8))
    }
}
