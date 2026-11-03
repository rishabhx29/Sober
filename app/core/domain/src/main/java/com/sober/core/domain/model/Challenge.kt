package com.sober.core.domain.model

import java.time.LocalDate

/**
 * Opt-in challenges: a short, named commitment rather than another number.
 *
 * A challenge is deliberately stricter than the streak. The streak is a passive
 * record of what happened; a challenge is a promise the user made on a
 * particular day, so it can also be *broken* — and a broken run is reported
 * plainly instead of quietly rolling into a new one.
 *
 * Days are judged from history the app already keeps. Nothing here needs a
 * network and nothing is guessed: a clean day is the same comparison the
 * enforcement engine makes, and a "social" minute is a minute recorded against
 * a package the platform labels social.
 */
enum class ChallengeId(
    val title: String,
    val description: String
) {
    CLEAN_WEEK(
        title = "Seven clean days",
        description = "A week under every limit you have set."
    ),
    QUIET_WEEKEND(
        title = "Quiet weekend",
        description = "One Saturday and Sunday with no social apps."
    ),
    FEED_FREE_WEEK(
        title = "Feed-free week",
        description = "Seven days without opening a social app."
    );

    /** Judged from social-app usage rather than from the limit rules. */
    val isSocial: Boolean
        get() = this != CLEAN_WEEK

    /** Full days the challenge asks for. */
    val targetDays: Int
        get() = if (this == QUIET_WEEKEND) 2 else 7
}

/**
 * The challenge the user opted into on [startedOn].
 *
 * The first judged day is the one after [startedOn]: a commitment made at 9pm
 * should not be judged against the morning it was made in, and a day that is
 * already half spent is not evidence either way.
 */
data class ActiveChallenge(
    val id: ChallengeId,
    val startedOn: LocalDate
)

/**
 * How far an active challenge has got.
 *
 * [daysDone] counts only whole days that passed, so a challenge is never
 * reported complete before its last day is over. [isFailed] is separate from a
 * low [daysDone]: a challenge cannot be resumed after a bad day, it can only be
 * started again.
 */
data class ChallengeProgress(
    val id: ChallengeId,
    val startedOn: LocalDate,
    val targetDates: List<LocalDate>,
    val daysDone: Int,
    val failedOn: LocalDate? = null
) {
    val daysTarget: Int get() = targetDates.size

    val isFailed: Boolean get() = failedOn != null

    val isComplete: Boolean get() = !isFailed && daysDone == daysTarget

    val daysRemaining: Int get() = (daysTarget - daysDone).coerceAtLeast(0)

    /** 0f..1f for a progress bar; a failed run keeps the progress it reached. */
    val fraction: Float
        get() = if (daysTarget == 0) 0f else daysDone.toFloat() / daysTarget
}

/** Everything the Stats challenge section renders. */
data class ChallengeState(
    val active: ChallengeProgress? = null,
    val completedCount: Int = 0
)
