package com.sober.core.domain.usecase

import com.sober.core.domain.model.ActiveChallenge
import com.sober.core.domain.model.ChallengeId
import com.sober.core.domain.model.UsageSession
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetChallengeStateUseCaseTest {

    private val today = LocalDate.now()

    private fun useCase(
        challenges: FakeChallengeRepository = FakeChallengeRepository(),
        usage: FakeUsageSessionRepository = FakeUsageSessionRepository(),
        categories: FakeAppCategoryRepository = FakeAppCategoryRepository(),
        sessionState: FakeSessionStateRepository = FakeSessionStateRepository(),
        appLimits: FakeAppLimitRepository = FakeAppLimitRepository(),
        streakPreferences: FakeStreakPreferencesRepository = FakeStreakPreferencesRepository()
    ): GetChallengeStateUseCase {
        val streak = CalculateStreakUseCase(
            usageSessionRepository = usage,
            appLimitRepository = appLimits,
            sessionStateRepository = sessionState,
            focusSessionRepository = FakeFocusSessionRepository(),
            streakPreferencesRepository = streakPreferences
        )
        return GetChallengeStateUseCase(
            challengeRepository = challenges,
            usageSessionRepository = usage,
            appCategoryRepository = categories,
            calculateStreakUseCase = streak
        )
    }

    @Test
    fun `no active challenge reports only the finished count`() = runTest {
        val challenges = FakeChallengeRepository(
            initialCompleted = listOf(ActiveChallenge(ChallengeId.CLEAN_WEEK, today.minusDays(10)))
        )

        val state = useCase(challenges = challenges)(today)

        assertNull(state.active)
        assertEquals(1, state.completedCount)
    }

    @Test
    fun `a clean week counts the days that ended under the cap`() = runTest {
        val started = today.minusDays(4)
        val targets = challengeTargetDates(ChallengeId.CLEAN_WEEK, started)
        val judged = targets.filter { it.isBefore(today) }
        val usage = FakeUsageSessionRepository(
            judged.map { UsageSession(
                packageName = "com.android.chrome",
                date = it.toString(),
                usedMinutes = 30
            ) }
        )
        val sessionState = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }
        val challenges = FakeChallengeRepository(initialActive = ActiveChallenge(ChallengeId.CLEAN_WEEK, started))

        val progress = useCase(
            challenges = challenges,
            usage = usage,
            sessionState = sessionState
        )(today).active

        assertEquals(judged.size, progress?.daysDone)
        assertFalse(progress!!.isFailed)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `one day over the cap fails the clean week on that date`() = runTest {
        val started = today.minusDays(4)
        val targets = challengeTargetDates(ChallengeId.CLEAN_WEEK, started)
        val judged = targets.filter { it.isBefore(today) }
        val badDay = judged[1]
        val usage = FakeUsageSessionRepository(
            judged.map {
                UsageSession(
                    packageName = "com.android.chrome",
                    date = it.toString(),
                    usedMinutes = if (it == badDay) 200 else 30
                )
            }
        )
        val sessionState = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }
        val challenges = FakeChallengeRepository(initialActive = ActiveChallenge(ChallengeId.CLEAN_WEEK, started))

        val progress = useCase(
            challenges = challenges,
            usage = usage,
            sessionState = sessionState
        )(today).active!!

        assertTrue(progress.isFailed)
        assertEquals(badDay, progress.failedOn)
        assertEquals(1, progress.daysDone)
    }

    @Test
    fun `an unobserved day is not evidence of a clean day`() = runTest {
        val started = today.minusDays(3)
        val targets = challengeTargetDates(ChallengeId.CLEAN_WEEK, started)
        val judged = targets.filter { it.isBefore(today) }
        val usage = FakeUsageSessionRepository(
            judged.dropLast(1).map { UsageSession(
                packageName = "com.android.chrome",
                date = it.toString(),
                usedMinutes = 30
            ) }
        )
        val sessionState = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }
        val challenges = FakeChallengeRepository(initialActive = ActiveChallenge(ChallengeId.CLEAN_WEEK, started))

        val progress = useCase(
            challenges = challenges,
            usage = usage,
            sessionState = sessionState
        )(today).active!!

        assertTrue(progress.isFailed)
        assertEquals(judged.last(), progress.failedOn)
    }

    @Test
    fun `a feed free week counts days with no social minutes`() = runTest {
        val started = today.minusDays(4)
        val targets = challengeTargetDates(ChallengeId.FEED_FREE_WEEK, started)
        val judged = targets.filter { it.isBefore(today) }
        val usage = FakeUsageSessionRepository(
            judged.map { UsageSession(
                packageName = "com.android.chrome",
                date = it.toString(),
                usedMinutes = 40
            ) }
        )
        val categories = FakeAppCategoryRepository(social = setOf("com.instagram.android"))
        val challenges = FakeChallengeRepository(
            initialActive = ActiveChallenge(ChallengeId.FEED_FREE_WEEK, started)
        )

        val progress = useCase(
            challenges = challenges,
            usage = usage,
            categories = categories
        )(today).active!!

        assertEquals(judged.size, progress.daysDone)
        assertFalse(progress.isFailed)
    }

    @Test
    fun `a social minute ends the feed free week`() = runTest {
        val started = today.minusDays(4)
        val targets = challengeTargetDates(ChallengeId.FEED_FREE_WEEK, started)
        val judged = targets.filter { it.isBefore(today) }
        val usage = FakeUsageSessionRepository(
            judged.map { UsageSession(
                packageName = "com.android.chrome",
                date = it.toString(),
                usedMinutes = 40
            ) } +
                UsageSession(
                    packageName = "com.instagram.android",
                    date = judged[1].toString(),
                    usedMinutes = 5
                )
        )
        val categories = FakeAppCategoryRepository(social = setOf("com.instagram.android"))
        val challenges = FakeChallengeRepository(
            initialActive = ActiveChallenge(ChallengeId.FEED_FREE_WEEK, started)
        )

        val progress = useCase(
            challenges = challenges,
            usage = usage,
            categories = categories
        )(today).active!!

        assertTrue(progress.isFailed)
        assertEquals(judged[1], progress.failedOn)
        assertEquals(1, progress.daysDone)
    }
}
