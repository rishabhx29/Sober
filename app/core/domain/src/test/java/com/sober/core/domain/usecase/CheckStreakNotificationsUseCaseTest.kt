package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.UsageSession
import com.sober.core.domain.repository.AlertKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Calendar

class CheckStreakNotificationsUseCaseTest {

    private val pkg = "com.example.social"

    private class Harness(
        val useCase: CheckStreakNotificationsUseCase,
        val notifier: FakeAlertNotifier,
        val state: FakeSessionStateRepository,
        val prefs: FakeNotificationPreferencesRepository
    )

    /**
     * [historyDays] seeds complete past days at [historyMinutes] each, which is
     * under the default 60-minute limit, so the streak seen today is
     * `historyDays + 1` when today is under too.
     */
    private fun harness(
        usageMinutes: Int,
        cap: Int = 0,
        appLimitMinutes: Int = 60,
        historyDays: Int = 0,
        historyMinutes: Int = 10
    ): Harness {
        val notifier = FakeAlertNotifier()
        val state = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = cap
        }
        val prefs = FakeNotificationPreferencesRepository()
        val limits = FakeAppLimitRepository(
            if (appLimitMinutes > 0) {
                listOf(
                    AppLimit(
                        packageName = pkg,
                        appName = "Social",
                        dailyLimitMinutes = appLimitMinutes
                    )
                )
            } else {
                emptyList()
            }
        )

        val today = LocalDate.now()
        val history = (1..historyDays).map { offset ->
            UsageSession(
                packageName = pkg,
                date = today.minusDays(offset.toLong()).toString(),
                usedMinutes = historyMinutes
            )
        }
        val sessions = FakeUsageSessionRepository(history)
        val stats = FakeUsageStatsRepository(
            usage = listOf(
                AppUsage(
                    packageName = pkg,
                    totalTimeInForeground = usageMinutes * 60_000L,
                    lastTimeUsed = 0
                )
            )
        )
        val calculateStreak = CalculateStreakUseCase(
            sessions,
            limits,
            state,
            FakeFocusSessionRepository(),
            FakeStreakPreferencesRepository()
        )

        return Harness(
            useCase = CheckStreakNotificationsUseCase(
                stats,
                sessions,
                limits,
                state,
                calculateStreak,
                notifier,
                prefs
            ),
            notifier = notifier,
            state = state,
            prefs = prefs
        )
    }

    private fun todayAt(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `nothing fires before ten in the evening`() = runTest {
        val h = harness(usageMinutes = 20, historyDays = 2)

        h.useCase(todayAt(21, 59))

        assertTrue(h.notifier.dailyStreaks.isEmpty())
        assertTrue(h.notifier.streakAtRisk.isEmpty())
    }

    @Test
    fun `ten in the evening is inside the window`() = runTest {
        val h = harness(usageMinutes = 20, historyDays = 2)

        h.useCase(todayAt(22, 0))

        assertEquals(listOf(3), h.notifier.dailyStreaks)
    }

    @Test
    fun `midnight is outside the window`() = runTest {
        val h = harness(usageMinutes = 20, historyDays = 2)

        h.useCase(todayAt(0, 0))

        assertTrue(h.notifier.dailyStreaks.isEmpty())
    }

    @Test
    fun `staying under every limit celebrates the whole streak`() = runTest {
        val h = harness(usageMinutes = 20, historyDays = 2)

        h.useCase(todayAt(23, 0))

        assertEquals(listOf(3), h.notifier.dailyStreaks)
        assertTrue(h.notifier.streakAtRisk.isEmpty())
    }

    @Test
    fun `the evening note fires once per day`() = runTest {
        val h = harness(usageMinutes = 20, historyDays = 1)

        h.useCase(todayAt(22, 10))
        h.useCase(todayAt(22, 30))
        h.useCase(todayAt(23, 40))

        assertEquals(1, h.notifier.dailyStreaks.size)
    }

    @Test
    fun `reaching a limit warns using the streak from before today`() = runTest {
        val h = harness(usageMinutes = 60, historyDays = 2)

        h.useCase(todayAt(22, 30))

        assertEquals(listOf(2), h.notifier.streakAtRisk)
        assertTrue(h.notifier.dailyStreaks.isEmpty())
    }

    @Test
    fun `a limit reached with no prior streak warns nobody`() = runTest {
        val h = harness(usageMinutes = 60, historyDays = 0)

        h.useCase(todayAt(22, 30))

        assertTrue(h.notifier.streakAtRisk.isEmpty())
        assertTrue(h.notifier.dailyStreaks.isEmpty())
    }

    @Test
    fun `the overall cap counts as a reached limit`() = runTest {
        val h = harness(usageMinutes = 60, cap = 60, appLimitMinutes = 0, historyDays = 2)

        h.useCase(todayAt(22, 30))

        assertEquals(listOf(2), h.notifier.streakAtRisk)
    }

    @Test
    fun `a disabled preference suppresses its own alert only`() = runTest {
        val risk = harness(usageMinutes = 60, historyDays = 2)
        risk.prefs.disabled += AlertKind.STREAK_WARNING
        risk.useCase(todayAt(22, 30))
        assertTrue(risk.notifier.streakAtRisk.isEmpty())

        val daily = harness(usageMinutes = 20, historyDays = 2)
        daily.prefs.disabled += AlertKind.DAILY_STREAK
        daily.useCase(todayAt(22, 30))
        assertTrue(daily.notifier.dailyStreaks.isEmpty())
    }

    @Test
    fun `an unobserved day is never celebrated`() = runTest {
        // No live usage and no stored rows: the streak calculation still counts
        // today as live, but there is nothing to congratulate.
        val h = harness(usageMinutes = 0, historyDays = 3)

        h.useCase(todayAt(22, 30))

        assertTrue(h.notifier.dailyStreaks.isEmpty())
    }

    @Test
    fun `with nothing configured nothing fires`() = runTest {
        val h = harness(usageMinutes = 20, cap = 0, appLimitMinutes = 0, historyDays = 2)

        h.useCase(todayAt(22, 30))

        assertTrue(h.notifier.dailyStreaks.isEmpty())
        assertTrue(h.notifier.streakAtRisk.isEmpty())
    }

    @Test
    fun `the risk alert is also once per day`() = runTest {
        val h = harness(usageMinutes = 90, historyDays = 1)

        h.useCase(todayAt(22, 10))
        h.useCase(todayAt(23, 50))

        assertEquals(1, h.notifier.streakAtRisk.size)
    }
}
