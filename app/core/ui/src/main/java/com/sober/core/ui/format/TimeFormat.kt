package com.sober.core.ui.format

/**
 * Countdowns are formatted as "MM:SS" for the eye, but TalkBack reads that as a
 * clock time. Converting it to words gives screen-reader users "4 minutes 30
 * seconds" instead of an ambiguous "04:30".
 */
fun spokenRemaining(text: String): String {
    val parts = text.split(":").mapNotNull { it.toIntOrNull() }
    return when {
        parts.size == 2 -> "${plural(parts[0], "minute")} ${plural(parts[1], "second")}"
        parts.size == 3 -> "${plural(parts[0], "hour")} ${plural(parts[1], "minute")}"
        text == "∞" -> "open-ended"
        else -> text
    }
}

/** "1 minute", not "1 minutes" — screen readers play that back verbatim. */
private fun plural(value: Int, unit: String): String =
    if (value == 1) "1 $unit" else "$value ${unit}s"

/**
 * Minute counts are drawn as "1h 5m" to keep the ring compact. TalkBack should
 * not read those abbreviations aloud, so summaries use this form instead.
 */
fun spokenDuration(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> plural(rest, "minute")
        rest == 0 -> plural(hours, "hour")
        else -> "${plural(hours, "hour")} ${plural(rest, "minute")}"
    }
}
