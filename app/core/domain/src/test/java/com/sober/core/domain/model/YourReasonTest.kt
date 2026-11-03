package com.sober.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class YourReasonTest {

    @Test
    fun `blank input becomes empty`() {
        assertEquals("", YourReason.normalize(""))
        assertEquals("", YourReason.normalize("   \n\t "))
    }

    @Test
    fun `runs of whitespace collapse to one space`() {
        assertEquals(
            "be present with my kids",
            YourReason.normalize("  be   present\n\nwith\tmy kids  ")
        )
    }

    @Test
    fun `long text is capped without a trailing space`() {
        val reason = YourReason.normalize("a".repeat(119) + "  b")

        assertEquals(119, reason.length)
        assertEquals("a".repeat(119), reason)
    }

    @Test
    fun `text at the limit is kept whole`() {
        val atLimit = "a".repeat(YourReason.MAX_LENGTH)

        assertEquals(atLimit, YourReason.normalize(atLimit))
    }
}
