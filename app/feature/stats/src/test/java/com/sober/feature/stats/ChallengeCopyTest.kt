package com.sober.feature.stats

import com.sober.core.domain.model.ChallengeId
import com.sober.core.domain.model.ChallengeProgress
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeCopyTest {

    private val started = LocalDate.of(2026, 11, 2)
    private val targets = (1..7).map { started.plusDays(it.toLong()) }

    private fun progress(
        done: Int,
        failedOn: LocalDate? = null
    ) = ChallengeProgress(
        id = ChallengeId.CLEAN_WEEK,
        startedOn = started,
        targetDates = targets,
        daysDone = done,
        failedOn = failedOn
    )

    @Test
    fun `before the first judged day the card names the start date`() {
        val label = challengeStatusLabel(progress(done = 0), started, targets.first())

        assertEquals("STARTS TUE, NOV 3", label)
    }

    @Test
    fun `a running challenge names the day it is on`() {
        val thirdDay = targets[2]

        assertEquals("DAY 3 OF 7", challengeStatusLabel(progress(done = 2), thirdDay, targets.first()))
    }

    @Test
    fun `the last day still reads as a running day`() {
        val lastDay = targets.last()

        assertEquals("DAY 7 OF 7", challengeStatusLabel(progress(done = 6), lastDay, targets.first()))
    }

    @Test
    fun `a finished run reads DONE and a broken one ENDED`() {
        assertEquals(
            "DONE",
            challengeStatusLabel(progress(done = 7), started.plusDays(9), targets.first())
        )
        assertEquals(
            "ENDED",
            challengeStatusLabel(
                progress(done = 2, failedOn = targets[2]),
                targets[3],
                targets.first()
            )
        )
    }

    @Test
    fun `a broken run names the day it ended`() {
        val body = challengeBodyLabel(
            progress(done = 2, failedOn = targets[2]),
            targets[3]
        )

        assertTrue(body.contains("Thu, Nov 5"))
    }

    @Test
    fun `a running run states the count without praise`() {
        val body = challengeBodyLabel(progress(done = 2), targets[2])

        assertTrue(body.startsWith("2 of 7 full days done"))
        assertTrue(body.contains("5 to go"))
    }
}
