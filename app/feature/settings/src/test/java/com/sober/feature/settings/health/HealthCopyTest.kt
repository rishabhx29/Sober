package com.sober.feature.settings.health

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthCopyTest {

    @Test
    fun `the switch off is the whole story`() {
        assertEquals(
            "Off",
            HealthCopy.accessibilityStatus(enabled = false, connected = false, lastEventAgeMillis = null)
        )
    }

    @Test
    fun `enabled but unbound is reported separately`() {
        assertEquals(
            "Not connected",
            HealthCopy.accessibilityStatus(enabled = true, connected = false, lastEventAgeMillis = null)
        )
    }

    @Test
    fun `connected without an event yet`() {
        assertEquals(
            "Connected",
            HealthCopy.accessibilityStatus(enabled = true, connected = true, lastEventAgeMillis = null)
        )
    }

    @Test
    fun `recent events are rounded to a sentence`() {
        assertEquals("just now", HealthCopy.relativeAge(20_000))
        assertEquals("2 min ago", HealthCopy.relativeAge(120_000))
        assertEquals("3 h ago", HealthCopy.relativeAge(3 * 60 * 60_000L))
        assertEquals("2 d ago", HealthCopy.relativeAge(2 * 24 * 60 * 60_000L))
    }

    @Test
    fun `connected carries the age of the last event`() {
        assertEquals(
            "Connected · 2 min ago",
            HealthCopy.accessibilityStatus(
                enabled = true,
                connected = true,
                lastEventAgeMillis = 120_000
            )
        )
    }

    @Test
    fun `a negative age is treated as just now`() {
        assertEquals("just now", HealthCopy.relativeAge(-5_000))
    }

    @Test
    fun `summary counts only the required checks`() {
        assertEquals("All required checks passed", HealthCopy.requiredSummary(4, 4))
        assertEquals("3 of 4 required checks pass", HealthCopy.requiredSummary(3, 4))
        assertEquals("0 of 4 required checks pass", HealthCopy.requiredSummary(0, 4))
    }

    @Test
    fun `an empty checklist does not claim success`() {
        assertEquals("No required checks", HealthCopy.requiredSummary(0, 0))
    }

    @Test
    fun `battery exemption reads as a state, not a permission`() {
        assertEquals("Exempt", HealthCopy.batteryStatus(exempt = true))
        assertEquals("Optimised", HealthCopy.batteryStatus(exempt = false))
    }
}
