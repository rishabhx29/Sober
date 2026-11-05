package com.sober.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StartAppSessionUseCaseTest {

    private val now = 1_760_000_000_000L
    private val packageName = "com.instagram.android"

    @Test
    fun `the chosen length becomes the session`() = runTest {
        val sessions = FakeAppSessionRepository()

        val session = StartAppSessionUseCase(sessions)(packageName, minutes = 10, now = now)

        assertEquals(now, session.startedAt)
        assertEquals(now + 10 * 60_000L, session.endsAt)
        assertEquals(session, sessions.stored[packageName])
    }

    @Test
    fun `a second pick replaces the old session`() = runTest {
        val sessions = FakeAppSessionRepository()
        val useCase = StartAppSessionUseCase(sessions)
        useCase(packageName, minutes = 2, now = now)

        val latest = useCase(packageName, minutes = 30, now = now + 60_000L)

        assertEquals(latest, sessions.stored[packageName])
        assertEquals(now + 60_000L + 30 * 60_000L, latest.endsAt)
    }

    @Test
    fun `lengths outside the picker are clamped`() = runTest {
        val sessions = FakeAppSessionRepository()
        val useCase = StartAppSessionUseCase(sessions)

        assertEquals(now + 60_000L, useCase(packageName, minutes = 0, now = now).endsAt)
        assertEquals(
            now + 180 * 60_000L,
            useCase(packageName, minutes = 1_000, now = now).endsAt
        )
    }
}
