package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.UsageSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GetDailyScreenTimeUseCaseTest {

    private fun today(): String = SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US)
        .format(Date())

    private fun minutes(m: Long): Long = m * 60_000L

    @Test
    fun `prefers live stats when they are ahead of stored rows`() = runTest {
        val stats = FakeUsageStatsRepository(
            usage = listOf(
                AppUsage("a", minutes(30), 0),
                AppUsage("b", minutes(20), 0)
            )
        )
        val sessions = FakeUsageSessionRepository(
            listOf(UsageSession(packageName = "a", date = today(), usedMinutes = 10))
        )

        val total = GetDailyScreenTimeUseCase(stats, sessions)()

        assertEquals(50, total)
    }

    @Test
    fun `falls back to stored rows when usage access returns nothing`() = runTest {
        val stats = FakeUsageStatsRepository(usage = emptyList())
        val sessions = FakeUsageSessionRepository(
            listOf(
                UsageSession(packageName = "a", date = today(), usedMinutes = 25),
                UsageSession(packageName = "b", date = today(), usedMinutes = 5)
            )
        )

        assertEquals(30, GetDailyScreenTimeUseCase(stats, sessions)())
    }

    @Test
    fun `ignores rows from other days`() = runTest {
        val stats = FakeUsageStatsRepository(usage = emptyList())
        val sessions = FakeUsageSessionRepository(
            listOf(
                UsageSession(packageName = "a", date = today(), usedMinutes = 25),
                UsageSession(packageName = "a", date = "2000-01-01", usedMinutes = 400)
            )
        )

        assertEquals(25, GetDailyScreenTimeUseCase(stats, sessions)())
    }
}
