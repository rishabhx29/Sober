package com.sober.core.domain.model

/**
 * A schedule the app noticed the user might want, derived from local usage.
 *
 * Suggestions are proposals only: nothing is created until the user taps
 * Apply, and Dismiss is remembered so the same observation is not repeated.
 */
enum class ScheduleSuggestionKind {
    LATE_NIGHT,
    WORK_HOURS
}

data class ScheduleSuggestion(
    val kind: ScheduleSuggestionKind,
    val title: String,
    val message: String,
    val ruleName: String,
    val daysOfWeek: List<Int>,
    val startTime: String,
    val endTime: String
)
