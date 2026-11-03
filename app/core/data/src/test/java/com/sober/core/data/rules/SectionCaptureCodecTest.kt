package com.sober.core.data.rules

import com.sober.core.domain.model.CapturedCandidate
import com.sober.core.domain.model.CapturedSection
import com.sober.core.domain.model.DetectionType
import com.sober.core.domain.model.SectionCaptureRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionCaptureCodecTest {

    @Test
    fun `a request round-trips`() {
        val request = SectionCaptureRequest("com.example.app", 1_700_000_000_000L)

        assertEquals(request, SectionCaptureCodec.decodeRequest(SectionCaptureCodec.encodeRequest(request)))
    }

    @Test
    fun `a request without a timestamp is dropped`() {
        assertNull(SectionCaptureCodec.decodeRequest("com.example.app"))
        assertNull(SectionCaptureCodec.decodeRequest("com.example.app|not-a-time"))
        assertNull(SectionCaptureCodec.decodeRequest(null))
    }

    @Test
    fun `an empty capture still round-trips`() {
        val section = CapturedSection(
            packageName = "com.example.app",
            activityName = null,
            capturedAt = 42L,
            candidates = emptyList()
        )

        assertEquals(section, SectionCaptureCodec.decodeSection(SectionCaptureCodec.encodeSection(section)))
    }

    @Test
    fun `labels containing every separator survive a round trip`() {
        val section = CapturedSection(
            packageName = "com.example.app",
            activityName = "com.example.app.MainActivity",
            capturedAt = 42L,
            candidates = listOf(
                CapturedCandidate(
                    type = DetectionType.SELECTED_TAB,
                    value = "com.example.app:id/reels_tab",
                    label = "Reels | 100% \"late\"; feed, really"
                ),
                CapturedCandidate(
                    type = DetectionType.ACTIVITY,
                    value = "com.example.app.MainActivity",
                    label = "MainActivity",
                    isBroad = true
                )
            )
        )

        val decoded = SectionCaptureCodec.decodeSection(SectionCaptureCodec.encodeSection(section))

        assertEquals(section, decoded)
        assertTrue(decoded!!.candidates.last().isBroad)
    }

    @Test
    fun `one bad candidate is dropped without losing the others`() {
        val encoded = SectionCaptureCodec.encodeSection(
            CapturedSection(
                packageName = "com.example.app",
                activityName = null,
                capturedAt = 42L,
                candidates = listOf(
                    CapturedCandidate(DetectionType.SELECTED_TAB, "id/reels", "Reels")
                )
            )
        ).replace("SELECTED_TAB", "WIDGET")

        val decoded = SectionCaptureCodec.decodeSection(encoded)

        assertEquals("com.example.app", decoded!!.packageName)
        assertTrue(decoded.candidates.isEmpty())
    }

    @Test
    fun `a capture without a package is dropped`() {
        assertNull(SectionCaptureCodec.decodeSection("|activity|42|"))
        assertNull(SectionCaptureCodec.decodeSection("com.example.app|activity"))
        assertNull(SectionCaptureCodec.decodeSection(null))
    }
}
