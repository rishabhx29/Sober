package com.sober.core.domain.usecase

import com.sober.core.domain.model.UsageSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GetUsageHistoryUseCaseTest {

    private val formatter = SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US)

    private fun daysAgo(days: Int): String = formatter.format(
        Calendar.getInstance().apply {
            time = Date()
            add(Calendar.DAY_OF_YEAR, -days)
        }.time
    )

    @Test
    fun `zero-fills days with no rows and keeps oldest-first order`() = runTest {
        val repo = FakeUsageSessionRepository(
            listOf(
                UsageSession(packageName = "com.instagram.android", date = daysAgo(2), usedMinutes = 40),
                UsageSession(packageName = "com.google.android.youtube", date = daysAgo(0), usedMinutes = 15)
            )
        )

        val history = GetUsageHistoryUseCase(repo)(days = 3)

        assertEquals(3, history.dailyTotals.size)
        assertEquals(daysAgo(2), history.dailyTotals[0].date)
        assertEquals(40, history.dailyTotals[0].totalMinutes)
        assertEquals(0, history.dailyTotals[1].totalMinutes)
        assertEquals(15, history.dailyTotals[2].totalMinutes)
    }

    @Test
    fun `sums usage per app across the whole range`() = runTest {
        val repo = FakeUsageSessionRepository(
            listOf(
                UsageSession(packageName = "a", date = daysAgo(1), usedMinutes = 10),
                UsageSession(packageName = "a", date = daysAgo(0), usedMinutes = 5),
                UsageSession(packageName = "b", date = daysAgo(0), usedMinutes = 7)
            )
        )

        val history = GetUsageHistoryUseCase(repo)(days = 2)

        assertEquals(mapOf("a" to 15, "b" to 7), history.perAppMinutes)
        assertEquals(22, history.totalMinutes)
        assertEquals(11, history.averageDailyMinutes)
    }

    @Test
    fun `rejects non-positive ranges`() = runTest {
        val repo = FakeUsageSessionRepository()
        var threw = false
        try {
            GetUsageHistoryUseCase(repo)(days = 0)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("expected a range validation failure", threw)
    }
}
