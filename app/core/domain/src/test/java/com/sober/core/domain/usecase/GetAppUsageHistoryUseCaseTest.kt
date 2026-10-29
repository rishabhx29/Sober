package com.sober.core.domain.usecase

import com.sober.core.domain.model.UsageSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GetAppUsageHistoryUseCaseTest {

    private val formatter = SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US)

    private fun daysAgo(days: Int): String = formatter.format(
        Calendar.getInstance().apply {
            time = Date()
            add(Calendar.DAY_OF_YEAR, -days)
        }.time
    )

    @Test
    fun `returns one bucket per day, oldest first, zeros for missing days`() = runTest {
        val repo = FakeUsageSessionRepository(
            listOf(
                UsageSession(packageName = "target", date = daysAgo(3), usedMinutes = 20),
                UsageSession(packageName = "other", date = daysAgo(1), usedMinutes = 99),
                UsageSession(packageName = "target", date = daysAgo(0), usedMinutes = 5)
            )
        )

        val series = GetAppUsageHistoryUseCase(repo)("target", days = 4)

        assertEquals(listOf(20, 0, 0, 5), series)
    }

    @Test
    fun `sums duplicate rows for the same app and day`() = runTest {
        val repo = FakeUsageSessionRepository(
            listOf(
                UsageSession(packageName = "target", date = daysAgo(0), usedMinutes = 3),
                UsageSession(packageName = "target", date = daysAgo(0), usedMinutes = 4)
            )
        )

        val series = GetAppUsageHistoryUseCase(repo)("target", days = 1)

        assertEquals(listOf(7), series)
    }

    @Test
    fun `unknown package yields all zeroes`() = runTest {
        val repo = FakeUsageSessionRepository(
            listOf(UsageSession(packageName = "something", date = daysAgo(0), usedMinutes = 30))
        )

        assertEquals(List(7) { 0 }, GetAppUsageHistoryUseCase(repo)("missing", days = 7))
    }
}
