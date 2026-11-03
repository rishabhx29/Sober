package com.sober.core.domain.model

/**
 * The line the user writes for themselves and sees on the block screen.
 *
 * Stored as one short line, not free text: the block screen is read at a
 * glance, and a paragraph would either be clipped or push the countdown out of
 * view. Normalising at the storage boundary keeps the stored value and every
 * render agreeing about what counts as blank and what is too long.
 */
object YourReason {

    const val MAX_LENGTH = 120

    fun normalize(raw: String): String =
        raw.trim()
            .replace(WHITESPACE_RUN, " ")
            .take(MAX_LENGTH)
            .trimEnd()

    private val WHITESPACE_RUN = Regex("\\s+")
}
