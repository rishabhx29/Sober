package com.sober.core.domain.usecase

import com.sober.core.domain.model.AchievementId
import com.sober.core.domain.model.UsageInterval
import com.sober.core.domain.model.UsageSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val STREAK_ACHIEVEMENT_DAYS = 7
const val SOBER_PRO_DAYS = 30
const val FOCUS_MASTER_SESSIONS = 10
const val SPEED_DEMON_DAYS = 5
const val NIGHT_OWL_NIGHTS = 5

/** A "Pomodoro" is the classic 25-minute interval. */
const val POMODORO_DURATION_MINUTES = 25

/** "After 10 PM" is measured as usage between 22:00 and midnight. */
const val NIGHT_CUTOFF_HOUR = 22

/**
 * Everything the achievement rules need, gathered once.
 *
 * Keeping the rules behind this value object means they can be tested without
 * repositories, clocks or Android — the interesting part is the date maths.
 */
data class AchievementSnapshot(
    val streakDays: Int,
    val completedSessions: Int,
    val socialFreeDay: Boolean,
    /** Dates (ISO yyyy-MM-dd) with a completed 25-minute session. */
    val pomodoroDates: Set<String>,
    /** The most recent complete nights to check, oldest first. */
    val nightDates: List<String>,
    /** Dates on which the phone was actually used, so a night can be judged. */
    val observedDates: Set<String>,
    val lateNightDates: Set<String>
)

fun evaluateAchievements(snapshot: AchievementSnapshot): Set<AchievementId> = buildSet {
    if (snapshot.streakDays >= STREAK_ACHIEVEMENT_DAYS) add(AchievementId.SEVEN_DAY_STREAK)
    if (snapshot.streakDays >= SOBER_PRO_DAYS) add(AchievementId.SOBER_PRO)
    if (snapshot.completedSessions >= FOCUS_MASTER_SESSIONS) add(AchievementId.FOCUS_MASTER)
    if (snapshot.socialFreeDay) add(AchievementId.SOCIAL_DETOX)
    if (hasConsecutiveDays(snapshot.pomodoroDates, SPEED_DEMON_DAYS)) {
        add(AchievementId.SPEED_DEMON)
    }
    if (isNightOwl(snapshot.nightDates, snapshot.observedDates, snapshot.lateNightDates)) {
        add(AchievementId.NIGHT_OWL)
    }
}

/** True when [dates] contains [needed] calendar days in a row. */
fun hasConsecutiveDays(dates: Collection<String>, needed: Int): Boolean {
    if (needed <= 0) return true
    val sorted = dates
        .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        .distinct()
        .sorted()

    var run = 0
    var previous: LocalDate? = null
    for (date in sorted) {
        run = if (previous != null && date == previous.plusDays(1)) run + 1 else 1
        if (run >= needed) return true
        previous = date
    }
    return false
}

/**
 * True when at least one day saw other usage but no social-app minutes.
 *
 * Zero-usage days are excluded: the phone being off is not a detox, and rows
 * only exist for days the enforcement service observed. An empty social-app set
 * returns false — there is nothing to have abstained from.
 */
fun socialFreeDay(
    rows: List<UsageSession>,
    dates: List<String>,
    socialPackages: Set<String>
): Boolean {
    if (socialPackages.isEmpty()) return false

    val totalByDate = rows.groupBy { it.date }
        .mapValues { (_, dayRows) -> dayRows.sumOf { it.usedMinutes } }
    val socialByDate = rows
        .filter { it.packageName in socialPackages }
        .groupBy { it.date }
        .mapValues { (_, dayRows) -> dayRows.sumOf { it.usedMinutes } }

    return dates.any { date ->
        (totalByDate[date] ?: 0) > 0 && (socialByDate[date] ?: 0) == 0
    }
}

/**
 * Dates whose 22:00–midnight window overlaps any foreground interval.
 *
 * The evening rather than the whole following morning: "no phone after 10 PM"
 * is a bedtime habit, and the morning belongs to the next day's night.
 */
fun lateNightDates(
    intervals: List<UsageInterval>,
    cutoffHour: Int = NIGHT_CUTOFF_HOUR,
    zone: ZoneId = ZoneId.systemDefault()
): Set<String> {
    val dates = mutableSetOf<String>()

    intervals.forEach { interval ->
        if (interval.endMillis <= interval.startMillis) return@forEach
        val firstDay = Instant.ofEpochMilli(interval.startMillis).atZone(zone).toLocalDate()
        val lastDay = Instant.ofEpochMilli(interval.endMillis - 1).atZone(zone).toLocalDate()

        var day = firstDay
        while (!day.isAfter(lastDay)) {
            val windowStart = day.atTime(cutoffHour, 0).atZone(zone).toInstant().toEpochMilli()
            val windowEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            if (interval.startMillis < windowEnd && interval.endMillis > windowStart) {
                dates.add(day.toString())
            }
            day = day.plusDays(1)
        }
    }

    return dates
}

/**
 * Five complete nights, each actually observed, none with usage after [cutoffHour].
 *
 * An unobserved night cannot be judged — treating no data as "no phone" would
 * hand out the badge on a fresh install.
 */
fun isNightOwl(
    nightDates: List<String>,
    observedDates: Set<String>,
    lateNightDates: Set<String>
): Boolean =
    nightDates.size >= NIGHT_OWL_NIGHTS &&
        nightDates.take(NIGHT_OWL_NIGHTS).all { it in observedDates && it !in lateNightDates }
