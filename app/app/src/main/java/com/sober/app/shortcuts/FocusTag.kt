package com.sober.app.shortcuts

import com.sober.core.common.presets.PresetMode

/**
 * Parses the URI written to an NFC tag: `sober://focus?preset=STUDY`.
 *
 * A tag is just another way to deliver an intent, so the payload is untrusted
 * input with the same rule as [FocusShortcut]: an unknown or malformed URI does
 * nothing rather than falling back to a default session the writer never asked
 * for. Parsing is hand-written and stays off `android.net.Uri` so the contract
 * can be tested as a pure function.
 */
object FocusTag {

    private const val PREFIX = "sober://focus"

    fun presetFor(uri: String?): PresetMode? {
        val value = uri?.trim() ?: return null
        if (!value.regionMatches(0, PREFIX, 0, PREFIX.length, ignoreCase = true)) return null

        // `sober://focusx` must not match; a real tag continues with a query.
        val rest = value.substring(PREFIX.length)
        if (rest.isEmpty() || rest[0] != '?') return null

        val parameters = rest.substringAfter('?', "").substringBefore('#')
        val rawPreset = parameters.split('&')
            .map { it.substringBefore('=') to it.substringAfter('=', "") }
            .firstOrNull { (name, _) -> name.trim().equals("preset", ignoreCase = true) }
            ?.second
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        return PresetMode.entries.firstOrNull {
            it.name.equals(rawPreset, ignoreCase = true)
        }
    }
}
