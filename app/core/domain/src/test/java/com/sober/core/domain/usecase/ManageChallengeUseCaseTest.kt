package com.sober.core.domain.usecase

import com.sober.core.domain.model.ActiveChallenge
import com.sober.core.domain.model.ChallengeId
import com.sober.core.domain.model.UsageSession
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageChallengeUseCaseTest {

    private val today = LocalDate.now()

    private fun manager(
        challenges: FakeChallengeRepository = FakeChallengeRepository(),
        usage: FakeUsageSessionRepository = FakeUsageSessionRepository(),
        categories: FakeAppCategoryRepository = FakeAppCategoryRepository(),
        sessionState: FakeSessionStateRepository = FakeSessionStateRepository(),
        appLimits: FakeAppLimitRepository = FakeAppLimitRepository()
    ): ManageChallengeUseCase {
        val streak = CalculateStreakUseCase(
            usageSessionRepository = usage,
            appLimitRepository = appLimits,
            sessionStateRepository = sessionState,
            focusSessionRepository = FakeFocusSessionRepository(),
            streakPreferencesRepository = FakeStreakPreferencesRepository()
        )
        val state = GetChallengeStateUseCase(
            challengeRepository = challenges,
            usageSessionRepository = usage,
            appCategoryRepository = categories,
            calculateStreakUseCase = streak
        )
        return ManageChallengeUseCase(
            challengeRepository = challenges,
            appLimitRepository = appLimits,
            sessionStateRepository = sessionState,
            appCategoryRepository = categories,
            getChallengeStateUseCase = state
        )
    }

    @Test
    fun `a clean week needs a limit to judge`() = runTest {
        val challenges = FakeChallengeRepository()

        val result = manager(challenges = challenges).start(ChallengeId.CLEAN_WEEK, today)

        assertEquals(ChallengeStart.NothingToMeasure, result)
        assertNull(challenges.active.value)
    }

    @Test
    fun `a clean week starts when the daily cap is set`() = runTest {
        val challenges = FakeChallengeRepository()
        val sessionState = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }

        val result = manager(challenges = challenges, sessionState = sessionState)
            .start(ChallengeId.CLEAN_WEEK, today)

        assertEquals(ChallengeStart.Started, result)
        assertEquals(ActiveChallenge(ChallengeId.CLEAN_WEEK, today), challenges.active.value)
    }

    @Test
    fun `a social challenge needs a social app installed`() = runTest {
        val challenges = FakeChallengeRepository()

        val result = manager(challenges = challenges).start(ChallengeId.FEED_FREE_WEEK, today)

        assertEquals(ChallengeStart.NothingToMeasure, result)
        assertNull(challenges.active.value)
    }

    @Test
    fun `a social challenge starts when there is something to measure`() = runTest {
        val challenges = FakeChallengeRepository()
        val categories = FakeAppCategoryRepository(social = setOf("com.instagram.android"))

        val result = manager(challenges = challenges, categories = categories)
            .start(ChallengeId.FEED_FREE_WEEK, today)

        assertEquals(ChallengeStart.Started, result)
        assertEquals(ChallengeId.FEED_FREE_WEEK, challenges.active.value?.id)
    }

    @Test
    fun `only one challenge runs at a time`() = runTest {
        val challenges = FakeChallengeRepository(
            initialActive = ActiveChallenge(ChallengeId.QUIET_WEEKEND, today.minusDays(1))
        )

        val result = manager(challenges = challenges).start(ChallengeId.CLEAN_WEEK, today)

        assertEquals(ChallengeStart.AlreadyRunning, result)
        assertEquals(ChallengeId.QUIET_WEEKEND, challenges.active.value?.id)
    }

    @Test
    fun `finishing early records nothing`() = runTest {
        val started = today.minusDays(2)
        val challenges = FakeChallengeRepository(
            initialActive = ActiveChallenge(ChallengeId.CLEAN_WEEK, started)
        )

        val finished = manager(challenges = challenges).finish(today)

        assertFalse(finished)
        assertTrue(challenges.completed.value.isEmpty())
        assertEquals(ChallengeId.CLEAN_WEEK, challenges.active.value?.id)
    }

    @Test
    fun `a completed challenge moves to the finished set`() = runTest {
        val started = today.minusDays(8)
        val judged = challengeTargetDates(ChallengeId.CLEAN_WEEK, started)
        val usage = FakeUsageSessionRepository(
            judged.map { UsageSession(
                packageName = "com.android.chrome",
                date = it.toString(),
                usedMinutes = 20
            ) }
        )
        val sessionState = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }
        val challenges = FakeChallengeRepository(
            initialActive = ActiveChallenge(ChallengeId.CLEAN_WEEK, started)
        )

        val finished = manager(
            challenges = challenges,
            usage = usage,
            sessionState = sessionState
        ).finish(today)

        assertTrue(finished)
        assertNull(challenges.active.value)
        assertEquals(
            listOf(ActiveChallenge(ChallengeId.CLEAN_WEEK, started)),
            challenges.completed.value
        )
    }

    @Test
    fun `leaving a challenge clears it without recording a win`() = runTest {
        val challenges = FakeChallengeRepository(
            initialActive = ActiveChallenge(ChallengeId.CLEAN_WEEK, today)
        )

        manager(challenges = challenges).abandon()

        assertNull(challenges.active.value)
        assertTrue(challenges.completed.value.isEmpty())
    }
}
