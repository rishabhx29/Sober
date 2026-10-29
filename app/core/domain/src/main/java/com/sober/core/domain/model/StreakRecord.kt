package com.sober.core.domain.model

import java.time.LocalDate

/**
 * The streak as it was last written down, so the longest streak can outlive the
 * 60-day window that `usage_sessions` is queried for.
 *
 * Only closed days are folded in: today can still turn into a failure, so
 * [lastSuccessDate] never points at a day that has not finished yet.
 */
data class StreakRecord(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastSuccessDate: String? = null
)

/**
 * Folds one successful, finished day into this record.
 *
 * [streakEndingOn] is the chain length read back from history on [date], which
 * can never exceed the lookback window. When this record already knows the day
 * before, the stored count is carried forward instead, so a streak that grows
 * past the window keeps counting rather than freezing at the cap.
 */
fun StreakRecord.withSuccessfulDay(streakEndingOn: Int, date: LocalDate): StreakRecord {
    if (streakEndingOn <= 0) return this

    val isoDate = date.toString()
    val newCurrent = when (lastSuccessDate) {
        // Home can load several times a day; re-recording the same finished day
        // must not inflate the count.
        isoDate -> maxOf(currentStreak, streakEndingOn)
        date.minusDays(1).toString() -> currentStreak + 1
        else -> streakEndingOn
    }

    return StreakRecord(
        currentStreak = newCurrent,
        longestStreak = maxOf(longestStreak, newCurrent),
        lastSuccessDate = isoDate
    )
}
