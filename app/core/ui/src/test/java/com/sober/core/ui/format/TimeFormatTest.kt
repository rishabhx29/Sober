package com.sober.core.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `two part countdown reads as minutes and seconds`() {
        assertEquals("25 minutes 0 seconds", spokenRemaining("25:00"))
        assertEquals("0 minutes 7 seconds", spokenRemaining("00:07"))
    }

    @Test
    fun `three part countdown drops seconds so it stays short`() {
        assertEquals("1 hour 30 minutes", spokenRemaining("01:30:00"))
        assertEquals("2 hours 5 minutes", spokenRemaining("02:05:00"))
    }

    @Test
    fun `a single unit is not pluralised`() {
        assertEquals("1 minute 1 second", spokenRemaining("01:01"))
    }

    @Test
    fun `hour counts do not leak a leading zero`() {
        assertEquals("2 minutes 5 seconds", spokenRemaining("2:05"))
    }

    @Test
    fun `open ended sessions are spoken rather than reading an infinity glyph`() {
        assertEquals("open-ended", spokenRemaining("∞"))
    }

    @Test
    fun `unparsable text passes through unchanged`() {
        assertEquals("soon", spokenRemaining("soon"))
    }

    @Test
    fun `minute totals speak hours only when there are whole hours`() {
        assertEquals("22 minutes", spokenDuration(22))
        assertEquals("1 hour", spokenDuration(60))
        assertEquals("1 hour 5 minutes", spokenDuration(65))
        assertEquals("3 hours", spokenDuration(180))
        assertEquals("0 minutes", spokenDuration(0))
    }
}
