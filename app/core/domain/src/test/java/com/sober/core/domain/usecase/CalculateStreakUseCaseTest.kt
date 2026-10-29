package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.model.FocusSession
import com.sober.core.domain.model.SessionStatus
import com.sober.core.domain.model.UsageSession
import com.sober.core.domain.repository.StreakRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CalculateStreakUseCaseTest {

    private val dates = GetUsageHistoryUseCase.recentDates(3)
    private val dayBeforeYesterday = dates[0]
    private val yesterday = dates[1]
    private val today = dates[2]

    private fun useCase(
        usage: FakeUsageSessionRepository = FakeUsageSessionRepository(),
        limits: FakeAppLimitRepository = FakeAppLimitRepository(),
        state: FakeSessionStateRepository = FakeSessionStateRepository(),
        sessions: FakeFocusSessionRepository = FakeFocusSessionRepository(),
        streakPrefs: FakeStreakPreferencesRepository = FakeStreakPreferencesRepository()
    ): CalculateStreakUseCase = CalculateStreakUseCase(
        usageSessionRepository = usage,
        appLimitRepository = limits,
        sessionStateRepository = state,
        focusSessionRepository = sessions,
        streakPreferencesRepository = streakPrefs
    )

    private fun row(packageName: String, date: String, minutes: Int) =
        UsageSession(packageName = packageName, date = date, usedMinutes = minutes)

    private fun cancelledSessionOn(date: String, hour: Int = 12) = FocusSession(
        label = "Deep Work",
        durationMinutes = 25,
        startedAt = noonOf(date, hour) - 25 * 60_000L,
        endedAt = noonOf(date, hour),
        status = SessionStatus.CANCELLED
    )

    private fun noonOf(date: String, hour: Int): Long =
        LocalDate.parse(date).atTime(hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    @Test
    fun `consecutive days under the overall cap build a streak`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(
                row("com.instagram.android", yesterday, 60),
                row("com.instagram.android", today, 70)
            )
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }

        assertEquals(2, useCase(usage = usage, state = state)())
    }

    @Test
    fun `a day at or over the cap ends the streak`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(
                row("com.instagram.android", yesterday, 130),
                row("com.instagram.android", today, 10)
            )
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }

        // Today is fine, yesterday is not, so only today counts.
        assertEquals(1, useCase(usage = usage, state = state)())
    }

    @Test
    fun `today over the cap reports no streak`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(
                row("com.instagram.android", yesterday, 10),
                row("com.instagram.android", today, 121)
            )
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }

        assertEquals(0, useCase(usage = usage, state = state)())
    }

    @Test
    fun `an untracked day stops the chain instead of counting as zero usage`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", today, 10))
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }

        assertEquals(1, useCase(usage = usage, state = state)())
    }

    @Test
    fun `today with no usage yet still counts`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", yesterday, 10))
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }

        assertEquals(2, useCase(usage = usage, state = state)())
    }

    @Test
    fun `an app over its own limit breaks the day even under the overall cap`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", today, 45))
        )
        val limits = FakeAppLimitRepository(
            listOf(AppLimit(packageName = "com.instagram.android", appName = "Instagram", dailyLimitMinutes = 30))
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }

        assertEquals(0, useCase(usage = usage, limits = limits, state = state)())
    }

    @Test
    fun `a day is successful only when app and overall limits both hold`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(
                row("com.instagram.android", today, 29),
                row("com.youtube.android", today, 40)
            )
        )
        val limits = FakeAppLimitRepository(
            listOf(AppLimit(packageName = "com.instagram.android", appName = "Instagram", dailyLimitMinutes = 30))
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }

        assertEquals(1, useCase(usage = usage, limits = limits, state = state)())
    }

    @Test
    fun `inactive app limits are ignored`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", today, 90))
        )
        val limits = FakeAppLimitRepository(
            listOf(
                AppLimit(
                    packageName = "com.instagram.android",
                    appName = "Instagram",
                    dailyLimitMinutes = 30,
                    isActive = false
                )
            )
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }

        assertEquals(1, useCase(usage = usage, limits = limits, state = state)())
    }

    @Test
    fun `no limits configured reports no streak`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", today, 300))
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 0
        }

        assertEquals(0, useCase(usage = usage, state = state)())
    }

    @Test
    fun `a cancellation does not break the day under the default rule`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(
                row("com.instagram.android", yesterday, 60),
                row("com.instagram.android", today, 70)
            )
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }
        val sessions = FakeFocusSessionRepository(listOf(cancelledSessionOn(yesterday)))

        assertEquals(
            2,
            useCase(usage = usage, state = state, sessions = sessions)()
        )
    }

    @Test
    fun `under the strict rule a cancelled session breaks that day`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(
                row("com.instagram.android", yesterday, 60),
                row("com.instagram.android", today, 70),
                row("com.instagram.android", dayBeforeYesterday, 80)
            )
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }
        val sessions = FakeFocusSessionRepository(listOf(cancelledSessionOn(yesterday)))
        val prefs = FakeStreakPreferencesRepository(StreakRule.LIMITS_AND_NO_CANCELLATIONS)

        // Today counts, yesterday broke, and the chain cannot reach the day
        // before it.
        assertEquals(
            1,
            useCase(usage = usage, state = state, sessions = sessions, streakPrefs = prefs)()
        )
    }

    @Test
    fun `under the strict rule a cancellation today reports no streak`() = runTest {
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }
        val sessions = FakeFocusSessionRepository(listOf(cancelledSessionOn(today)))
        val prefs = FakeStreakPreferencesRepository(StreakRule.LIMITS_AND_NO_CANCELLATIONS)

        assertEquals(
            0,
            useCase(state = state, sessions = sessions, streakPrefs = prefs)()
        )
    }

    @Test
    fun `a completed session does not break the strict day`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", today, 10))
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }
        val completed = cancelledSessionOn(today).copy(status = SessionStatus.COMPLETED)
        val prefs = FakeStreakPreferencesRepository(StreakRule.LIMITS_AND_NO_CANCELLATIONS)

        assertEquals(
            1,
            useCase(
                usage = usage,
                state = state,
                sessions = FakeFocusSessionRepository(listOf(completed)),
                streakPrefs = prefs
            )()
        )
    }

    @Test
    fun `a window ending yesterday counts the closed day`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", yesterday, 60))
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }

        assertEquals(
            1,
            useCase(usage = usage, state = state)
                .streakEndingOn(LocalDate.parse(yesterday))
        )
    }

    @Test
    fun `a window ending yesterday does not count an unobserved yesterday`() = runTest {
        // Day-before-yesterday is clean, but yesterday has no rows at all. A
        // closed day cannot be assumed successful just because it is the first
        // day in the window.
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", dayBeforeYesterday, 10))
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }

        assertEquals(
            0,
            useCase(usage = usage, state = state)
                .streakEndingOn(LocalDate.parse(yesterday))
        )
    }

    @Test
    fun `today's failure does not affect a window ending yesterday`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(
                row("com.instagram.android", yesterday, 60),
                row("com.instagram.android", today, 200)
            )
        )
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }
        val useCase = useCase(usage = usage, state = state)

        assertEquals(1, useCase.streakEndingOn(LocalDate.parse(yesterday)))
        // The live streak has already lost today, which is exactly why the
        // record is written from the closed day instead.
        assertEquals(0, useCase())
    }
}
