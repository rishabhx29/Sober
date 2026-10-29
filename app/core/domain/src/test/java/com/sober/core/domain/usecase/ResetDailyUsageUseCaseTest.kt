package com.sober.core.domain.usecase

import com.sober.core.domain.model.UsageSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ResetDailyUsageUseCaseTest {

    private fun today(): String = SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US)
        .format(Date())

    private fun yesterday(): String = SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US)
        .format(
            Calendar.getInstance().apply {
                time = Date()
                add(Calendar.DAY_OF_YEAR, -1)
            }.time
        )

    @Test
    fun `first run records the day without wiping fresh counters`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(UsageSession(packageName = "com.example", date = today(), usedMinutes = 12))
        )
        val state = FakeSessionStateRepository()
        val useCase = ResetDailyUsageUseCase(usage, state)

        val didRollover = useCase()

        assertEquals(true, didRollover)
        assertEquals(12, usage.sessions.single().usedMinutes)
        assertEquals(today(), state.lastUsageRolloverDay.first())
    }

    @Test
    fun `same day is a no-op`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(UsageSession(packageName = "com.example", date = today(), usedMinutes = 12))
        )
        val state = FakeSessionStateRepository()
        state.setLastUsageRolloverDay(today())
        val useCase = ResetDailyUsageUseCase(usage, state)

        assertEquals(false, useCase())
        assertEquals(12, usage.sessions.single().usedMinutes)
    }

    @Test
    fun `a new day zeroes the stored counters`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(UsageSession(packageName = "com.example", date = today(), usedMinutes = 12))
        )
        val state = FakeSessionStateRepository()
        state.setLastUsageRolloverDay(yesterday())
        val useCase = ResetDailyUsageUseCase(usage, state)

        val didRollover = useCase()

        assertEquals(true, didRollover)
        assertEquals(0, usage.sessions.single().usedMinutes)
        assertEquals(today(), state.lastUsageRolloverDay.first())
    }
}
