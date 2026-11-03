package com.sober.core.domain.usecase

import com.sober.core.domain.model.ChallengeId
import com.sober.core.domain.model.ChallengeProgress
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * The days a challenge will be judged on.
 *
 * Week challenges take the [ChallengeId.targetDays] days after the opt-in day.
 * A quiet weekend takes the next Saturday and Sunday, strictly after the start:
 * starting on a Saturday cannot include a Saturday already spent, so it waits
 * for the following weekend rather than judging a half-day.
 */
fun challengeTargetDates(id: ChallengeId, startedOn: LocalDate): List<LocalDate> =
    when (id) {
        ChallengeId.CLEAN_WEEK, ChallengeId.FEED_FREE_WEEK ->
            (1..id.targetDays).map { startedOn.plusDays(it.toLong()) }
        ChallengeId.QUIET_WEEKEND -> {
            val saturday = startedOn.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
            listOf(saturday, saturday.plusDays(1))
        }
    }

/**
 * Pure evaluation, so the date maths can be tested without repositories.
 *
 * Only days before [today] are judged: a day in progress can still be crossed
 * and reporting it as done would let a challenge be won on the morning of its
 * last day. A bad judged day ends the run at [ChallengeProgress.failedOn]; the
 * days after it are not examined, because the commitment has already failed and
 * the user is told the date it happened.
 */
fun evaluateChallenge(
    id: ChallengeId,
    startedOn: LocalDate,
    today: LocalDate,
    isGoodDay: (LocalDate) -> Boolean
): ChallengeProgress {
    val targets = challengeTargetDates(id, startedOn)
    var done = 0
    var failedOn: LocalDate? = null

    for (date in targets) {
        if (!date.isBefore(today)) break
        if (isGoodDay(date)) {
            done++
        } else {
            failedOn = date
            break
        }
    }

    return ChallengeProgress(
        id = id,
        startedOn = startedOn,
        targetDates = targets,
        daysDone = done,
        failedOn = failedOn
    )
}
