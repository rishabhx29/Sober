package com.sober.app.shortcuts

import com.sober.core.common.presets.PresetMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The extras come from outside the app, so every branch that decides whether
 * to start a session is pinned here.
 */
class FocusShortcutTest {

    private val action = FocusShortcut.ACTION_START_PRESET

    @Test
    fun `every preset name is accepted`() {
        PresetMode.entries.forEach { mode ->
            assertEquals(mode, FocusShortcut.presetFor(action, mode.name))
        }
    }

    @Test
    fun `names are case and whitespace insensitive`() {
        assertEquals(PresetMode.STUDY, FocusShortcut.presetFor(action, " study "))
        assertEquals(PresetMode.WORK, FocusShortcut.presetFor(action, "Work"))
        assertEquals(PresetMode.SLEEP, FocusShortcut.presetFor(action, "sleep"))
    }

    @Test
    fun `a different action is ignored`() {
        assertNull(FocusShortcut.presetFor("android.intent.action.MAIN", "STUDY"))
        assertNull(FocusShortcut.presetFor(null, "STUDY"))
    }

    @Test
    fun `an unknown preset is ignored rather than substituted`() {
        assertNull(FocusShortcut.presetFor(action, "DEEP_WORK"))
        assertNull(FocusShortcut.presetFor(action, ""))
        assertNull(FocusShortcut.presetFor(action, null))
    }
}
