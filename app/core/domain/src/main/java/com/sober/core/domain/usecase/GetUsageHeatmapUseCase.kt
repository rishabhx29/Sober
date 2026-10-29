package com.sober.core.domain.usecase

import com.sober.core.domain.model.UsageInterval
import com.sober.core.domain.repository.UsageStatsRepository
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import javax.inject.Inject

/**
 * Usage by weekday and hour.
 *
 * Rows are Monday..Sunday and columns are hours 0..23, so the same weekday
 * lines up across weeks and a recurring habit shows as a column, not as a
 * one-off spike.
 */
data class UsageHeatmap(
    val cells: List<List<Int>>,
    /** Hour with the most usage across the whole range, or null without data. */
    val busiestHour: Int?,
    /** "10 PM–12 AM" style window around [busiestHour], or null. */
    val busiestWindowLabel: String?
) {
    val hasData: Boolean get() = cells.any { row -> row.any { it > 0 } }

    companion object {
        val EMPTY = UsageHeatmap(
            cells = List(7) { List(24) { 0 } },
            busiestHour = null,
            busiestWindowLabel = null
        )
    }
}

/**
 * Builds the heatmap from foreground intervals.
 *
 * The range is capped at a week. Events are replayed by the platform query, and
 * seven days is both what the 7x24 grid means and the point past which the cost
 * stops being worth it for a screen the user opens and closes.
 */
class GetUsageHeatmapUseCase @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository
) {
    suspend operator fun invoke(
        days: Int = MAX_DAYS,
        zone: ZoneId = ZoneId.systemDefault()
    ): UsageHeatmap {
        val window = days.coerceIn(1, MAX_DAYS)
        val toMillis = System.currentTimeMillis()
        val fromMillis = startOfDaysAgo(window, zone)

        val intervals = runCatching {
            usageStatsRepository.getForegroundIntervals(fromMillis, toMillis)
        }.getOrDefault(emptyList())

        return buildUsageHeatmap(intervals, fromMillis, toMillis, zone)
    }

    companion object {
        const val MAX_DAYS = 7

        /** Midnight at the start of the oldest day in a [days]-day window. */
        fun startOfDaysAgo(
            days: Int,
            zone: ZoneId = ZoneId.systemDefault(),
            nowMillis: Long = System.currentTimeMillis()
        ): Long =
            Calendar.getInstance(java.util.TimeZone.getTimeZone(zone)).apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -(days.coerceAtLeast(1) - 1))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
    }
}

/**
 * Splits each interval at hour boundaries and accumulates minutes per cell.
 *
 * Splitting — rather than charging the whole interval to its start hour — is
 * what makes a long session show up as a band across the hours it actually
 * covered.
 */
fun buildUsageHeatmap(
    intervals: List<UsageInterval>,
    fromMillis: Long,
    toMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): UsageHeatmap {
    if (toMillis <= fromMillis) return UsageHeatmap.EMPTY

    val millis = Array(7) { LongArray(24) }

    intervals.forEach { interval ->
        var cursor = maxOf(interval.startMillis, fromMillis)
        val end = minOf(interval.endMillis, toMillis)
        while (cursor < end) {
            val at = Instant.ofEpochMilli(cursor).atZone(zone)
            val nextHour = at.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
            val sliceEnd = minOf(end, nextHour)
            millis[at.dayOfWeek.value - 1][at.hour] += sliceEnd - cursor
            cursor = sliceEnd
        }
    }

    val cells = millis.map { row -> row.map { (it / 60_000L).toInt() } }

    val hourTotals = IntArray(24) { hour -> cells.sumOf { it[hour] } }
    val busiestHour = hourTotals.indices
        .filter { hourTotals[it] > 0 }
        .maxByOrNull { hourTotals[it] }

    return UsageHeatmap(
        cells = cells,
        busiestHour = busiestHour,
        busiestWindowLabel = busiestHour?.let { windowLabel(it) }
    )
}

/** "10 PM–12 AM" for a two-hour window starting at [hour]. */
internal fun windowLabel(hour: Int): String =
    "${hourLabel(hour)}–${hourLabel((hour + 2) % 24)}"

private fun hourLabel(hour: Int): String {
    val hour12 = when (val h = hour % 12) {
        0 -> 12
        else -> h
    }
    return "$hour12 ${if (hour < 12) "AM" else "PM"}"
}
