package com.sober.core.domain.usecase

import com.sober.core.domain.model.OverrideLog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetOverrideSummaryUseCaseTest {

    private val start = 1_000_000_000_000L
    private val end = start + 24 * 60 * 60 * 1000L

    private fun log(at: Long, minutes: Int = 15, id: Int = 0) = OverrideLog(
        id = id,
        packageName = "com.instagram.android",
        overrideAt = at,
        durationGrantedMinutes = minutes
    )

    @Test
    fun `no logs in the period report no overrides`() = runTest {
        val summary = GetOverrideSummaryUseCase(FakeOverrideLogRepository())(start, end)

        assertFalse(summary.hasOverrides)
        assertEquals(0, summary.count)
        assertEquals(0, summary.totalMinutesGranted)
    }

    @Test
    fun `the period is half-open so an old grant cannot leak in`() = runTest {
        val repo = FakeOverrideLogRepository().apply {
            logs += log(at = start - 1)
            logs += log(at = end)
            logs += log(at = start)
            logs += log(at = end - 1)
        }

        val summary = GetOverrideSummaryUseCase(repo)(start, end)

        assertEquals(2, summary.count)
        assertEquals(30, summary.totalMinutesGranted)
    }

    @Test
    fun `granted minutes are summed rather than assumed`() = runTest {
        val repo = FakeOverrideLogRepository().apply {
            logs += log(at = start + 10, minutes = 15)
            logs += log(at = start + 20, minutes = 30)
            logs += log(at = start + 30, minutes = 5)
        }

        val summary = GetOverrideSummaryUseCase(repo)(start, end)

        assertTrue(summary.hasOverrides)
        assertEquals(3, summary.count)
        assertEquals(50, summary.totalMinutesGranted)
    }

    @Test
    fun `a failing repository reports no overrides instead of crashing Stats`() = runTest {
        val failing = object : com.sober.core.domain.repository.OverrideLogRepository {
            override suspend fun insertLog(log: OverrideLog) = Unit
            override fun getRecentLogs(limit: Int) = kotlinx.coroutines.flow.MutableStateFlow(emptyList<OverrideLog>())
            override suspend fun getLogsBetween(fromMillis: Long, toMillis: Long): List<OverrideLog> =
                throw IllegalStateException("database unavailable")
        }

        val summary = GetOverrideSummaryUseCase(failing)(start, end)

        assertFalse(summary.hasOverrides)
    }
}
