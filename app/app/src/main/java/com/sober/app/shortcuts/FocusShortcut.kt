package com.sober.app.shortcuts

import com.sober.core.common.presets.PresetMode

/**
 * The one exported entry point for automation.
 *
 * Launcher shortcuts and external tools (Tasker, MacroDroid, `am start`) use
 * the same action, so there is a single parsing rule to test and document.
 * Only *starting* a preset is exported. Pausing protection, ending a session
 * or lifting a block would let any installed app undo Sober, so those actions
 * stay inside the UI.
 *
 * Deliberately a plain object with no Android types: the intent extras are
 * untrusted input from outside the app, and the parser is the thing worth
 * testing.
 */
object FocusShortcut {

    /** Exported. `preset` must name one of [PresetMode]. */
    const val ACTION_START_PRESET = "com.sober.app.action.START_PRESET"

    const val EXTRA_PRESET = "preset"

    /**
     * @return the preset to start, or null when this is some other intent.
     * Unknown preset names are null rather than a fallback: silently starting
     * a different session than the caller asked for would be worse than
     * doing nothing.
     */
    fun presetFor(action: String?, rawPreset: String?): PresetMode? {
        if (action != ACTION_START_PRESET) return null
        val name = rawPreset?.trim()?.uppercase() ?: return null
        return PresetMode.entries.firstOrNull { it.name == name }
    }
}
