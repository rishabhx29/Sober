package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppLimit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StartAppSessionUseCaseTest {

    private val now = 1_760_000_000_000L
    private val packageName = "com.instagram.android"

    private fun rules(vararg rest: Int) = FakeAppLimitRepository(
        rest.map {
            AppLimit(
                packageName = packageName,
                appName = "Instagram",
                dailyLimitMinutes = 0,
                sessionTimerEnabled = true,
                sessionRestMinutes = it
            )
        }
    )

    @Test
    fun `the chosen length becomes the session`() = runTest {
        val sessions = FakeAppSessionRepository()

        val session = StartAppSessionUseCase(sessions, rules())(packageName, minutes = 10, now = now)

        assertEquals(now, session.startedAt)
        assertEquals(now + 10 * 60_000L, session.endsAt)
        assertEquals(session, sessions.stored[packageName])
    }

    @Test
    fun `a second pick replaces the old session`() = runTest {
        val sessions = FakeAppSessionRepository()
        val useCase = StartAppSessionUseCase(sessions, rules())
        useCase(packageName, minutes = 2, now = now)

        val latest = useCase(packageName, minutes = 30, now = now + 60_000L)

        assertEquals(latest, sessions.stored[packageName])
        assertEquals(now + 60_000L + 30 * 60_000L, latest.endsAt)
    }

    @Test
    fun `lengths outside the picker are clamped`() = runTest {
        val sessions = FakeAppSessionRepository()
        val useCase = StartAppSessionUseCase(sessions, rules())

        assertEquals(now + 60_000L, useCase(packageName, minutes = 0, now = now).endsAt)
        assertEquals(
            now + 180 * 60_000L,
            useCase(packageName, minutes = 1_000, now = now).endsAt
        )
    }

    @Test
    fun `the session carries the rest configured in the rule`() = runTest {
        val sessions = FakeAppSessionRepository()

        val session = StartAppSessionUseCase(sessions, rules(30))(packageName, minutes = 10, now = now)

        assertEquals(30, session.restMinutes)
        assertEquals(now + 10 * 60_000L + 30 * 60_000L, session.restEndsAt)
    }

    @Test
    fun `no rule yet falls back to the default rest`() = runTest {
        val sessions = FakeAppSessionRepository()

        val session = StartAppSessionUseCase(sessions, FakeAppLimitRepository())(
            packageName,
            minutes = 5,
            now = now
        )

        assertEquals(AppLimit.DEFAULT_SESSION_REST_MINUTES, session.restMinutes)
    }

    @Test
    fun `a rest outside the allowed range is clamped`() = runTest {
        val sessions = FakeAppSessionRepository()
        val tooShort = StartAppSessionUseCase(sessions, rules(0))(packageName, minutes = 5, now = now)
        val tooLong = StartAppSessionUseCase(sessions, rules(1_000))(packageName, minutes = 5, now = now)

        assertEquals(AppLimit.MIN_SESSION_REST_MINUTES, tooShort.restMinutes)
        assertEquals(AppLimit.MAX_SESSION_REST_MINUTES, tooLong.restMinutes)
    }
}
