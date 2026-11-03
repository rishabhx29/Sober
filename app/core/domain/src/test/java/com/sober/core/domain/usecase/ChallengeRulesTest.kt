package com.sober.core.domain.usecase

import com.sober.core.domain.model.ChallengeId
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeRulesTest {

    private val monday = LocalDate.of(2026, 11, 2)

    @Test
    fun `a week challenge starts the day after opt-in`() {
        val targets = challengeTargetDates(ChallengeId.CLEAN_WEEK, monday)

        assertEquals(7, targets.size)
        assertEquals(monday.plusDays(1), targets.first())
        assertEquals(monday.plusDays(7), targets.last())
    }

    @Test
    fun `a quiet weekend is the coming Saturday and Sunday`() {
        val targets = challengeTargetDates(ChallengeId.QUIET_WEEKEND, monday)

        assertEquals(DayOfWeek.SATURDAY, targets[0].dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, targets[1].dayOfWeek)
        assertEquals(monday.plusDays(5), targets[0])
    }

    @Test
    fun `opting in on a Saturday waits for the next weekend`() {
        val saturday = LocalDate.of(2026, 11, 7)

        val targets = challengeTargetDates(ChallengeId.QUIET_WEEKEND, saturday)

        assertEquals(saturday.plusWeeks(1), targets[0])
        assertEquals(saturday.plusWeeks(1).plusDays(1), targets[1])
    }

    @Test
    fun `nothing is judged on the opt-in day`() {
        val progress = evaluateChallenge(ChallengeId.CLEAN_WEEK, monday, monday) { true }

        assertEquals(0, progress.daysDone)
        assertFalse(progress.isFailed)
        assertFalse(progress.isComplete)
        assertEquals(7, progress.daysRemaining)
    }

    @Test
    fun `a challenge completes only after the last day is over`() {
        val started = monday
        val dayBeforeLast = started.plusDays(6)

        val stillRunning = evaluateChallenge(ChallengeId.CLEAN_WEEK, started, dayBeforeLast) { true }
        assertFalse(stillRunning.isComplete)
        assertEquals(5, stillRunning.daysDone)

        val done = evaluateChallenge(ChallengeId.CLEAN_WEEK, started, started.plusDays(8)) { true }
        assertTrue(done.isComplete)
        assertEquals(7, done.daysDone)
        assertEquals(1f, done.fraction, 0f)
    }

    @Test
    fun `a bad day ends the run at that date`() {
        val started = monday
        val badDay = started.plusDays(3)

        val progress = evaluateChallenge(ChallengeId.CLEAN_WEEK, started, started.plusDays(8)) {
            it != badDay
        }

        assertTrue(progress.isFailed)
        assertEquals(badDay, progress.failedOn)
        assertEquals(2, progress.daysDone)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `later good days do not resume a failed run`() {
        val started = monday
        val progress = evaluateChallenge(ChallengeId.CLEAN_WEEK, started, started.plusDays(8)) {
            it != started.plusDays(1)
        }

        assertTrue(progress.isFailed)
        assertFalse(progress.isComplete)
        assertEquals(0, progress.daysDone)
    }

    @Test
    fun `a quiet weekend needs both days`() {
        val started = monday
        val saturday = started.plusDays(5)

        val afterSaturday = evaluateChallenge(ChallengeId.QUIET_WEEKEND, started, saturday.plusDays(1)) { true }
        assertEquals(1, afterSaturday.daysDone)
        assertFalse(afterSaturday.isComplete)

        val afterSunday = evaluateChallenge(ChallengeId.QUIET_WEEKEND, started, saturday.plusDays(2)) { true }
        assertTrue(afterSunday.isComplete)
    }
}
