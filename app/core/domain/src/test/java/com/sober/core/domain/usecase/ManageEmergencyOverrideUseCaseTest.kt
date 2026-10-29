package com.sober.core.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageEmergencyOverrideUseCaseTest {

    private val state = FakeOverrideStateRepository()
    private val logs = FakeOverrideLogRepository()
    private val useCase = ManageEmergencyOverrideUseCase(state, logs)

    private val now = 1_000_000L
    private val pkg = "com.example.social"

    @Test
    fun `request records the package and moment`() = runTest {
        useCase.request(pkg, now)

        val current = useCase.state.first()
        assertEquals(pkg, current.requestedPackage)
        assertEquals(now, current.requestedAt)
    }

    @Test
    fun `cannot grant before the five minute wait`() = runTest {
        useCase.request(pkg, now)
        val current = useCase.state.first()

        assertFalse(useCase.canGrant(current, pkg, now + 4 * 60_000L))
    }

    @Test
    fun `can grant once the wait has elapsed`() = runTest {
        useCase.request(pkg, now)
        val current = useCase.state.first()

        assertTrue(useCase.canGrant(current, pkg, now + ManageEmergencyOverrideUseCase.WAIT_MILLIS))
    }

    @Test
    fun `cannot grant a request made for another app`() = runTest {
        useCase.request(pkg, now)
        val current = useCase.state.first()

        assertFalse(useCase.canGrant(current, "com.example.other", now + 6 * 60_000L))
    }

    @Test
    fun `a request past its lifetime must be made again`() = runTest {
        useCase.request(pkg, now)
        val current = useCase.state.first()

        assertFalse(
            useCase.canGrant(
                current,
                pkg,
                now + ManageEmergencyOverrideUseCase.REQUEST_TTL_MILLIS + 1
            )
        )
    }

    @Test
    fun `grant opens a fifteen minute window, logs it and clears the request`() = runTest {
        useCase.request(pkg, now)

        useCase.grant(pkg, sessionId = 42, now = now)

        val current = useCase.state.first()
        assertEquals(pkg, current.overridePackage)
        assertEquals(now + ManageEmergencyOverrideUseCase.GRANT_MILLIS, current.overrideUntil)
        assertNull(current.requestedPackage)

        assertEquals(1, logs.logs.size)
        val log = logs.logs.single()
        assertEquals(pkg, log.packageName)
        assertEquals(15, log.durationGrantedMinutes)
        assertEquals(42, log.sessionId)
    }

    @Test
    fun `lift applies only to the granted package and only inside the window`() = runTest {
        useCase.grant(pkg, sessionId = null, now = now)

        assertTrue(useCase.isLifted(pkg, now + 60_000L))
        assertFalse(useCase.isLifted("com.example.other", now + 60_000L))
        assertFalse(useCase.isLifted(pkg, now + ManageEmergencyOverrideUseCase.GRANT_MILLIS))
    }

    @Test
    fun `cancel drops a pending request without granting anything`() = runTest {
        useCase.request(pkg, now)

        useCase.cancelRequest()

        val current = useCase.state.first()
        assertNull(current.requestedPackage)
        assertNull(current.overridePackage)
        assertTrue(logs.logs.isEmpty())
    }

    @Test
    fun `clearExpired removes an expired grant and a stale request`() = runTest {
        useCase.grant(pkg, sessionId = null, now = now)
        useCase.request("com.example.other", now)

        useCase.clearExpired(now + ManageEmergencyOverrideUseCase.REQUEST_TTL_MILLIS + 1)

        val current = useCase.state.first()
        assertNull(current.overridePackage)
        assertNull(current.overrideUntil)
        assertNull(current.requestedPackage)
    }

    @Test
    fun `clearExpired keeps a live grant`() = runTest {
        useCase.grant(pkg, sessionId = null, now = now)

        useCase.clearExpired(now + 60_000L)

        val current = useCase.state.first()
        assertEquals(pkg, current.overridePackage)
        assertTrue(useCase.isLifted(pkg, now + 60_000L))
    }
}
