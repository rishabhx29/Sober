package com.sober.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakRecordTest {

    private val today = LocalDate.of(2026, 3, 10)
    private val yesterday = today.minusDays(1)

    @Test
    fun `the first successful day starts the streak`() {
        val result = StreakRecord().withSuccessfulDay(streakEndingOn = 1, date = yesterday)

        assertEquals(
            StreakRecord(currentStreak = 1, longestStreak = 1, lastSuccessDate = "2026-03-09"),
            result
        )
    }

    @Test
    fun `a day right after the recorded one extends the stored count`() {
        val stored = StreakRecord(currentStreak = 4, longestStreak = 7, lastSuccessDate = "2026-03-08")

        val result = stored.withSuccessfulDay(streakEndingOn = 9, date = yesterday)

        // The stored count is the only one that can exceed the window, so
        // continuity trusts it over a shorter derived chain.
        assertEquals(5, result.currentStreak)
        assertEquals(7, result.longestStreak)
        assertEquals("2026-03-09", result.lastSuccessDate)
    }

    @Test
    fun `a streak longer than the history window keeps growing`() {
        // 60 is the most CalculateStreakUseCase can ever report. The stored
        // record is what lets the real count go past it.
        val stored = StreakRecord(currentStreak = 60, longestStreak = 60, lastSuccessDate = "2026-03-08")

        val result = stored.withSuccessfulDay(streakEndingOn = 60, date = yesterday)

        assertEquals(61, result.currentStreak)
        assertEquals(61, result.longestStreak)
    }

    @Test
    fun `recording the same day twice is idempotent`() {
        val stored = StreakRecord(currentStreak = 5, longestStreak = 5, lastSuccessDate = "2026-03-09")

        val result = stored.withSuccessfulDay(streakEndingOn = 5, date = yesterday)

        assertEquals(stored, result)
    }

    @Test
    fun `re-recording the same day never shrinks the count`() {
        val stored = StreakRecord(currentStreak = 61, longestStreak = 61, lastSuccessDate = "2026-03-09")

        // A shortened window can only report the capped 60.
        val result = stored.withSuccessfulDay(streakEndingOn = 60, date = yesterday)

        assertEquals(61, result.currentStreak)
        assertEquals(61, result.longestStreak)
    }

    @Test
    fun `a gap in the record falls back to the computed chain`() {
        val stored = StreakRecord(currentStreak = 12, longestStreak = 30, lastSuccessDate = "2026-02-01")

        val result = stored.withSuccessfulDay(streakEndingOn = 3, date = yesterday)

        assertEquals(3, result.currentStreak)
        assertEquals(30, result.longestStreak)
        assertEquals("2026-03-09", result.lastSuccessDate)
    }

    @Test
    fun `a day that did not count leaves the record untouched`() {
        val stored = StreakRecord(currentStreak = 12, longestStreak = 30, lastSuccessDate = "2026-03-08")

        assertEquals(stored, stored.withSuccessfulDay(streakEndingOn = 0, date = yesterday))
        assertEquals(stored, stored.withSuccessfulDay(streakEndingOn = -1, date = yesterday))
    }
}
