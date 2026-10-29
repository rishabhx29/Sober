package com.sober.core.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A recurring block window: if any of [blockedPackages] is in the foreground
 * while the rule is active, it is blocked.
 *
 * Times are "HH:MM" wall-clock strings rather than instants because a schedule
 * is a daily habit, not a one-off: the same rule must mean 9:00–18:00 today,
 * tomorrow and after a time-zone change.
 */
data class ScheduleRule(
    val id: Int = 0,
    val ruleName: String,
    val blockedPackages: List<String> = emptyList(),
    /** ISO day numbers, 1 = Monday … 7 = Sunday. Empty means every day. */
    val daysOfWeek: List<Int> = emptyList(),
    val startTime: String,
    val endTime: String,
    val isActive: Boolean = true
)

/**
 * Epoch millis at which the window containing [nowMillis] closes, or null when
 * this rule is not active at that instant.
 *
 * Returning the closing instant, not just a boolean, is what lets the overlay
 * say "until 6:00 PM" instead of a vague "this app is blocked".
 *
 * Two edge cases are deliberate:
 *
 *  - `startTime == endTime` never matches. Read literally it would mean either
 *    a zero-length window or a 24-hour one, and silently blocking an app all
 *    day because of a copy-paste mistake is far worse than blocking nothing.
 *  - A window whose end is earlier than its start crosses midnight. The day it
 *    belongs to is the day it *starts*, so "Friday 22:00–08:00" covers Saturday
 *    07:00 only if Friday is a selected day. Checking the end day instead would
 *    make Monday's 22:00 window leak into Tuesday morning even when Monday is
 *    the only selected day.
 */
fun ScheduleRule.windowEndingAt(
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long? {
    if (!isActive) return null

    val start = minutesOfDay(startTime) ?: return null
    val end = minutesOfDay(endTime) ?: return null
    if (start == end) return null

    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    // DayOfWeek.value follows ISO numbering, which is the numbering stored in
    // daysOfWeek, so no conversion table is needed.
    val today = now.dayOfWeek.value
    val minuteNow = now.hour * 60 + now.minute
    val todayListed = daysOfWeek.isEmpty() || today in daysOfWeek

    return if (start < end) {
        if (todayListed && minuteNow >= start && minuteNow < end) {
            endEpoch(now.toLocalDate(), end, zone)
        } else {
            null
        }
    } else {
        // Crossing midnight: either the window began earlier today, or it began
        // yesterday and today's small hours still belong to it.
        val yesterday = if (today == 1) 7 else today - 1
        val yesterdayListed = daysOfWeek.isEmpty() || yesterday in daysOfWeek
        when {
            todayListed && minuteNow >= start ->
                endEpoch(now.toLocalDate().plusDays(1), end, zone)

            yesterdayListed && minuteNow < end ->
                endEpoch(now.toLocalDate(), end, zone)

            else -> null
        }
    }
}

fun ScheduleRule.isActiveAt(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
    windowEndingAt(nowMillis, zone) != null

/**
 * Whole minutes until this rule's next start, or null when the next start is
 * beyond today/tomorrow or the rule's days exclude it.
 *
 * Used for the "starts in 15 minutes" warning. A start equal to "now" counts as
 * passed: the warning is only useful *before* the window opens, and reporting
 * zero minutes would be a notification about something already happening.
 */
fun ScheduleRule.minutesUntilStart(
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Int? {
    if (!isActive) return null

    val start = minutesOfDay(startTime) ?: return null
    val end = minutesOfDay(endTime) ?: return null
    if (start == end) return null

    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    val minuteNow = now.hour * 60 + now.minute
    val today = now.dayOfWeek.value
    val todayListed = daysOfWeek.isEmpty() || today in daysOfWeek

    val tomorrowDate = now.toLocalDate().plusDays(1)
    val tomorrowListed = daysOfWeek.isEmpty() || tomorrowDate.dayOfWeek.value in daysOfWeek

    // Building from the date (rather than adding minutes to "now") keeps a
    // daylight-saving shift from moving the start by an hour.
    val nextStart = when {
        todayListed && start > minuteNow ->
            now.toLocalDate().atStartOfDay(zone).plusMinutes(start.toLong())
        tomorrowListed ->
            tomorrowDate.atStartOfDay(zone).plusMinutes(start.toLong())
        else -> return null
    }

    val diffMs = nextStart.toInstant().toEpochMilli() - nowMillis
    if (diffMs < 0) return null
    return (diffMs / 60_000L).toInt()
}

/** Minutes since local midnight for an "HH:MM" value, or null when malformed. */
fun minutesOfDay(time: String): Int? {
    val parts = time.split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

fun formatTimeOfDay(hour: Int, minute: Int): String =
    "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))

/**
 * Epoch millis for [minuteOfDay] on [date]. Built from the date rather than by
 * adding to "now" so a daylight-saving shift inside the window cannot make the
 * end land an hour off.
 */
private fun endEpoch(date: LocalDate, minuteOfDay: Int, zone: ZoneId): Long =
    date.atStartOfDay(zone).plusMinutes(minuteOfDay.toLong()).toInstant().toEpochMilli()
