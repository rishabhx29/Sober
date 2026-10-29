package com.sober.core.domain.usecase

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The one-line answers under the charts.
 *
 * Everything here is derived from local history only: the values are computed
 * on the device and never uploaded, which is what the privacy policy promises.
 */
data class StatsHighlights(
    val focusCompleted: Int = 0,
    val focusCancelled: Int = 0,
    /** Full weekday name of the quietest day in the range, or null without data. */
    val mostProductiveWeekday: String? = null,
    /** Signed percent change against the previous 7 days, or null when there is no baseline. */
    val weekOverWeekPercent: Int? = null,
    val daysUnderLimit: Int = 0,
    val daysMeasured: Int = 0
) {
    val hasAdherence: Boolean get() = daysMeasured > 0
}

/**
 * The day with the least screen time among days that saw any use.
 *
 * Zero-usage days are excluded on purpose: a day the phone was off is not a
 * productive day, and it would always "win". Ties go to the most recent day,
 * because that is the habit the user can still act on.
 */
fun mostProductiveWeekday(totals: List<DailyUsageTotal>): String? {
    val quietest = totals
        .withIndex()
        .filter { it.value.totalMinutes > 0 }
        .minWithOrNull(
            compareBy<IndexedValue<DailyUsageTotal>> { it.value.totalMinutes }
                .thenByDescending { it.index }
        )
        ?: return null

    val date = runCatching { LocalDate.parse(quietest.value.date) }.getOrNull() ?: return null
    return date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
}

/**
 * Percent change of the last 7 days against the 7 before them.
 *
 * Returns null when the previous week has no recorded minutes: "+100%" against
 * nothing is noise, not a trend.
 */
fun weekOverWeekChange(history: List<DailyUsageTotal>): Int? {
    if (history.size < 14) return null
    val current = history.takeLast(7).sumOf { it.totalMinutes }
    val previous = history.dropLast(7).takeLast(7).sumOf { it.totalMinutes }
    if (previous == 0) return null
    return (((current - previous).toDouble() / previous) * 100).roundToInt()
}

/**
 * Days at or under the daily cap, counted only over days that had usage.
 *
 * A cap of zero means "no overall limit", so adherence is unmeasurable rather
 * than perfect.
 */
fun limitAdherence(
    totals: List<DailyUsageTotal>,
    dailyLimitMinutes: Int
): Pair<Int, Int> {
    if (dailyLimitMinutes <= 0) return 0 to 0
    val measured = totals.filter { it.totalMinutes > 0 }
    return measured.count { it.totalMinutes <= dailyLimitMinutes } to measured.size
}

/** "12% less", "+8%", "about the same" — the label under the trend. */
fun weekOverWeekLabel(percent: Int?): String = when {
    percent == null -> "No earlier week to compare"
    abs(percent) < 3 -> "About the same as last week"
    percent < 0 -> "${abs(percent)}% less than last week"
    else -> "$percent% more than last week"
}
