package com.sober.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Settings steps the overall cap by 30 minutes, so the card used to advertise a
 * 90-minute cap as "1h" and a 150-minute one as "2h". The formatter is the only
 * thing between that picker and the ring, so it is pinned here.
 */
class LimitLabelTest {

    @Test
    fun `whole hours drop the minutes`() {
        assertEquals("2h", limitLabel(120))
        assertEquals("24h", limitLabel(24 * 60))
    }

    @Test
    fun `half hours keep both parts`() {
        assertEquals("1h 30m", limitLabel(90))
        assertEquals("2h 30m", limitLabel(150))
        assertEquals("30m", limitLabel(30))
    }
}
