package com.sober.app.shortcuts

import com.sober.core.common.presets.PresetMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The tag payload is written by a third-party NFC app, so the parser has to
 * accept what writers produce and refuse everything else.
 */
class FocusTagTest {

    @Test
    fun `every preset name is accepted`() {
        PresetMode.entries.forEach { mode ->
            assertEquals(mode, FocusTag.presetFor("sober://focus?preset=${mode.name}"))
        }
    }

    @Test
    fun `scheme, host and value are case and whitespace insensitive`() {
        assertEquals(PresetMode.STUDY, FocusTag.presetFor(" SOBER://Focus?preset= study "))
        assertEquals(PresetMode.WORK, FocusTag.presetFor("sober://focus?preset=Work"))
    }

    @Test
    fun `extra parameters and a fragment do not matter`() {
        assertEquals(
            PresetMode.GYM,
            FocusTag.presetFor("sober://focus?source=tag&preset=GYM&x=1#top")
        )
    }

    @Test
    fun `a different scheme or host is ignored`() {
        assertNull(FocusTag.presetFor("https://focus?preset=STUDY"))
        assertNull(FocusTag.presetFor("sober://timer?preset=STUDY"))
        assertNull(FocusTag.presetFor("sober://focusx?preset=STUDY"))
    }

    @Test
    fun `an unknown or missing preset is ignored rather than substituted`() {
        assertNull(FocusTag.presetFor("sober://focus?preset=DEEP_WORK"))
        assertNull(FocusTag.presetFor("sober://focus?preset="))
        assertNull(FocusTag.presetFor("sober://focus"))
        assertNull(FocusTag.presetFor(""))
        assertNull(FocusTag.presetFor(null))
    }
}
