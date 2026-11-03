package com.sober.core.data.rules

import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.DetectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionRuleCodecTest {

    private fun rule(
        id: String = "custom:abc",
        sectionName: String = "Reels",
        activityValue: String? = "com.example.app.MainActivity",
        overlayTitle: String = "Reels is blocked",
        overlaySubtitle: String = "You recorded this section."
    ) = DetectionRule(
        id = id,
        packageName = "com.example.app",
        appName = "Example",
        sectionId = id,
        sectionName = sectionName,
        detectionType = DetectionType.SELECTED_TAB,
        detectionValue = "com.example.app:id/reels_tab",
        overlayTitle = overlayTitle,
        overlaySubtitle = overlaySubtitle,
        activityValue = activityValue,
        isCustom = true
    )

    @Test
    fun `a recorded rule round-trips with its activity scope`() {
        val decoded = SectionRuleCodec.decode(SectionRuleCodec.encode(rule()))

        assertEquals(rule(), decoded)
        assertTrue(decoded!!.isCustom)
    }

    @Test
    fun `separators and percent signs in the name survive a round trip`() {
        val awkward = rule(sectionName = "Shorts | 100% late; really, truly")

        val decoded = SectionRuleCodec.decode(SectionRuleCodec.encode(awkward))

        assertEquals(awkward.sectionName, decoded!!.sectionName)
        assertEquals(awkward.overlayTitle, decoded.overlayTitle)
    }

    @Test
    fun `a blank activity decodes as null`() {
        val decoded = SectionRuleCodec.decode(SectionRuleCodec.encode(rule(activityValue = null)))

        assertNull(decoded!!.activityValue)
    }

    @Test
    fun `an unknown detection type is dropped`() {
        val encoded = SectionRuleCodec.encode(rule()).replace("SELECTED_TAB", "WIDGET")

        assertNull(SectionRuleCodec.decode(encoded))
    }

    @Test
    fun `a record with the wrong field count is dropped`() {
        assertNull(SectionRuleCodec.decode("custom:abc|com.example.app"))
    }

    @Test
    fun `blank identity fields are dropped`() {
        assertNull(SectionRuleCodec.decode(SectionRuleCodec.encode(rule(id = " "))))
        assertNull(SectionRuleCodec.decode(SectionRuleCodec.encode(rule(sectionName = " "))))
    }

    @Test
    fun `missing copy falls back to readable defaults`() {
        val encoded = SectionRuleCodec.encode(rule(overlayTitle = "", overlaySubtitle = ""))

        val decoded = SectionRuleCodec.decode(encoded)!!

        assertEquals("Reels is blocked", decoded.overlayTitle)
        assertEquals("You set this rule in Sober.", decoded.overlaySubtitle)
    }

    @Test
    fun `an invalid escape is dropped rather than guessed`() {
        assertNull(SectionRuleCodec.decode("custom%ZZ:abc|com.example.app|x|x|Reels|ACTIVITY|x|x|x|"))
    }
}
