package com.sober.feature.stats

import com.sober.core.domain.model.ChallengeId
import com.sober.core.domain.model.ChallengeProgress
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The copy a challenge card shows, kept out of the composable so the day maths
 * can be tested without a screen.
 *
 * A card states the position and the dates instead of praising or scolding:
 * a failed run is information the user can act on, not a verdict.
 */

/** "DAY 3 OF 7" while running, the start date before the first judged day. */
internal fun challengeStatusLabel(
    progress: ChallengeProgress,
    today: LocalDate,
    firstTarget: LocalDate?
): String = when {
    progress.isComplete -> "DONE"
    progress.isFailed -> "ENDED"
    firstTarget != null && today.isBefore(firstTarget) ->
        "STARTS ${challengeDate(firstTarget).uppercase(Locale.US)}"
    else -> {
        val day = progress.targetDates.indexOfLast { !it.isAfter(today) } + 1
        "DAY ${day.coerceAtLeast(1)} OF ${progress.daysTarget}"
    }
}

internal fun challengeBodyLabel(progress: ChallengeProgress, today: LocalDate): String {
    val failedOn = progress.failedOn
    return when {
        progress.isComplete ->
            "Every day held. This one is yours to keep."
        failedOn != null ->
            "The run ended on ${challengeDate(failedOn)}. Nothing was taken away — " +
                "start again when you are ready."
        progress.targetDates.firstOrNull()?.let { today.isBefore(it) } == true ->
            "The clock starts at midnight. Today does not count, so there is nothing to lose yet."
        else ->
            "${progress.daysDone} of ${progress.daysTarget} full days done · " +
                "${progress.daysRemaining} to go. Today counts once it ends."
    }
}

internal fun offerMeta(id: ChallengeId): String = when (id) {
    ChallengeId.QUIET_WEEKEND -> "SAT + SUN · NEXT WEEKEND"
    ChallengeId.CLEAN_WEEK, ChallengeId.FEED_FREE_WEEK -> "${id.targetDays} DAYS · FROM TOMORROW"
}

private fun challengeDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US))
