package com.sober.feature.focus

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * History rows used to show only a date and status, so a completed 90-minute
 * session and a cancelled five-minute one looked identical at a glance. The
 * length is the missing half of "duration, and status"; the formatter is pure,
 * which is why it is tested here instead of through a screen that needs a
 * ViewModel.
 */
class SessionLengthLabelTest {

    @Test
    fun `minutes under an hour stay in minutes`() {
        assertEquals("15m", sessionLengthLabel(15))
        assertEquals("45m", sessionLengthLabel(45))
    }

    @Test
    fun `whole hours collapse to hours`() {
        assertEquals("1h", sessionLengthLabel(60))
        assertEquals("2h", sessionLengthLabel(120))
    }

    @Test
    fun `mixed durations show both parts`() {
        assertEquals("1h 30m", sessionLengthLabel(90))
        assertEquals("1h 5m", sessionLengthLabel(65))
    }

    @Test
    fun `an open-ended session is not zero minutes`() {
        assertEquals("Open-ended", sessionLengthLabel(0))
    }
}
