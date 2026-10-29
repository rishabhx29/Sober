package com.sober.system.overlay.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class BlockOverlayFormattingTest {

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis

    @Test
    fun `session countdown formats as MM SS under an hour`() {
        val now = 1_000_000L
        assertEquals("24:59", sessionRemainingText(now + 24 * 60_000L + 59_000L, now))
    }

    @Test
    fun `session countdown includes hours for long sessions`() {
        val now = 1_000_000L
        assertEquals("1:30:00", sessionRemainingText(now + 90 * 60_000L, now))
    }

    @Test
    fun `session countdown clamps to zero once the window has passed`() {
        assertEquals("00:00", sessionRemainingText(endsAt = 500L, nowMillis = 9_000L))
    }

    @Test
    fun `unknown session window renders as placeholders`() {
        assertEquals("--:--", sessionRemainingText(endsAt = null, nowMillis = 0L))
    }

    @Test
    fun `remaining fraction shrinks across the session and clamps at the edges`() {
        val start = 1_000L
        val end = 61_000L

        assertEquals(1f, sessionRemainingFraction(start, end, start)!!, 0.001f)
        assertEquals(0.5f, sessionRemainingFraction(start, end, start + 30_000L)!!, 0.001f)
        assertEquals(0f, sessionRemainingFraction(start, end, end + 10_000L)!!, 0.001f)
        assertNull(sessionRemainingFraction(null, end, start))
        assertNull(sessionRemainingFraction(start, start, start))
    }

    @Test
    fun `quote is stable within a day and changes across days`() {
        val monday = millis(2026, Calendar.JANUARY, 5)
        val mondayLater = monday + 3 * 60 * 60 * 1000L
        val tuesday = millis(2026, Calendar.JANUARY, 6)

        assertEquals(quoteOfTheDay(monday), quoteOfTheDay(mondayLater))
        assertNotEquals(quoteOfTheDay(monday), quoteOfTheDay(tuesday))
    }
}
