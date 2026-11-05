package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.model.AppSession
import com.sober.core.domain.model.SessionTimerState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateSessionTimerUseCaseTest {

    private val now = 1_760_000_000_000L
    private val packageName = "com.instagram.android"

    private fun useCase(
        limits: FakeAppLimitRepository = limitRepository(),
        sessions: FakeAppSessionRepository = FakeAppSessionRepository()
    ) = EvaluateSessionTimerUseCase(appLimitRepository = limits, appSessionRepository = sessions)

    private fun limitRepository(
        enabled: Boolean = true,
        active: Boolean = true
    ) = FakeAppLimitRepository(
        listOf(
            AppLimit(
                packageName = packageName,
                appName = "Instagram",
                dailyLimitMinutes = 0,
                isActive = active,
                sessionTimerEnabled = enabled
            )
        )
    )

    private fun session(startedMsAgo: Long, endsInMs: Long) = AppSession(
        packageName = packageName,
        startedAt = now - startedMsAgo,
        endsAt = now + endsInMs
    )

    @Test
    fun `an app with no limit row is off`() = runTest {
        assertEquals(
            SessionTimerState.Off,
            useCase(limits = FakeAppLimitRepository())(packageName, now)
        )
    }

    @Test
    fun `a paused limit row is off`() = runTest {
        assertEquals(
            SessionTimerState.Off,
            useCase(limits = limitRepository(active = false))(packageName, now)
        )
    }

    @Test
    fun `a row without the session flag is off`() = runTest {
        assertEquals(
            SessionTimerState.Off,
            useCase(limits = limitRepository(enabled = false))(packageName, now)
        )
    }

    @Test
    fun `a missing session asks for a length`() = runTest {
        assertEquals(
            SessionTimerState.NeedsSession,
            useCase()(packageName, now)
        )
    }

    @Test
    fun `a running session is running`() = runTest {
        val stored = session(startedMsAgo = 60_000L, endsInMs = 9 * 60_000L)

        val state = useCase(sessions = FakeAppSessionRepository(listOf(stored)))(packageName, now)

        assertTrue("Expected Running but was $state", state is SessionTimerState.Running)
        assertEquals(stored, (state as SessionTimerState.Running).session)
    }

    @Test
    fun `an ended session is resting`() = runTest {
        val stored = session(startedMsAgo = 15 * 60_000L, endsInMs = -5 * 60_000L)

        val state = useCase(sessions = FakeAppSessionRepository(listOf(stored)))(packageName, now)

        assertTrue("Expected Resting but was $state", state is SessionTimerState.Resting)
        assertEquals(stored, (state as SessionTimerState.Resting).session)
    }

    @Test
    fun `a concluded session asks for a new length`() = runTest {
        val stored = session(startedMsAgo = 30 * 60_000L, endsInMs = -20 * 60_000L)

        assertEquals(
            SessionTimerState.NeedsSession,
            useCase(sessions = FakeAppSessionRepository(listOf(stored)))(packageName, now)
        )
    }
}
