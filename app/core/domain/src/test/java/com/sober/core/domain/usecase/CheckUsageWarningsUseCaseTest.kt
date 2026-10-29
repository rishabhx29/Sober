package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.UsageSession
import com.sober.core.domain.repository.AlertKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CheckUsageWarningsUseCaseTest {

    private fun minutes(m: Int): Long = m * 60_000L

    private fun today(): String = SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US)
        .format(Date())

    private fun usage(vararg pairs: Pair<String, Int>): FakeUsageStatsRepository =
        FakeUsageStatsRepository(
            usage = pairs.map { (pkg, mins) ->
                AppUsage(packageName = pkg, totalTimeInForeground = minutes(mins), lastTimeUsed = 0L)
            }
        )

    private fun limit(packageName: String, minutes: Int, active: Boolean = true) =
        AppLimit(
            packageName = packageName,
            appName = packageName,
            dailyLimitMinutes = minutes,
            isActive = active
        )

    private fun useCase(
        usage: FakeUsageStatsRepository,
        limits: List<AppLimit> = emptyList(),
        state: FakeSessionStateRepository = FakeSessionStateRepository(),
        alerts: FakeAlertNotifier = FakeAlertNotifier(),
        stored: FakeUsageSessionRepository = FakeUsageSessionRepository(),
        prefs: FakeNotificationPreferencesRepository = FakeNotificationPreferencesRepository()
    ): Pair<CheckUsageWarningsUseCase, FakeAlertNotifier> {
        val case = CheckUsageWarningsUseCase(
            usageStatsRepository = usage,
            usageSessionRepository = stored,
            appLimitRepository = FakeAppLimitRepository(limits),
            sessionStateRepository = state,
            getDailyScreenTimeUseCase = GetDailyScreenTimeUseCase(usage, stored),
            alertNotifier = alerts,
            notificationPreferences = prefs
        )
        return case to alerts
    }

    @Test
    fun `warns exactly at eighty percent of an app limit`() = runTest {
        val (case, alerts) = useCase(
            usage = usage("com.instagram.android" to 48),
            limits = listOf(limit("com.instagram.android", 60))
        )

        case()

        assertEquals(
            listOf(Triple("com.instagram.android", 48, 60)),
            alerts.usageWarnings
        )
    }

    @Test
    fun `does not warn below eighty percent`() = runTest {
        val (case, alerts) = useCase(
            usage = usage("com.instagram.android" to 40),
            limits = listOf(limit("com.instagram.android", 60))
        )

        case()

        assertTrue(alerts.usageWarnings.isEmpty())
    }

    @Test
    fun `does not warn once the limit is already reached`() = runTest {
        val (case, alerts) = useCase(
            usage = usage("com.instagram.android" to 60),
            limits = listOf(limit("com.instagram.android", 60))
        )

        case()

        assertTrue(alerts.usageWarnings.isEmpty())
    }

    @Test
    fun `warns only once per day per app`() = runTest {
        val (case, alerts) = useCase(
            usage = usage("com.instagram.android" to 50),
            limits = listOf(limit("com.instagram.android", 60))
        )

        case()
        case()
        case()

        assertEquals(1, alerts.usageWarnings.size)
    }

    @Test
    fun `warns for the overall cap using total across apps`() = runTest {
        val state = FakeSessionStateRepository()
        state.dailyScreenTimeLimitMinutes.value = 120
        val (case, alerts) = useCase(
            usage = usage("a.b" to 50, "c.d" to 50),
            state = state
        )

        case()

        assertEquals(listOf(Triple(null, 100, 120)), alerts.usageWarnings)
    }

    @Test
    fun `ignores inactive limits and a disabled overall cap`() = runTest {
        val state = FakeSessionStateRepository()
        state.dailyScreenTimeLimitMinutes.value = 0
        val (case, alerts) = useCase(
            usage = usage("com.instagram.android" to 55),
            limits = listOf(limit("com.instagram.android", 60, active = false)),
            state = state
        )

        case()

        assertTrue(alerts.usageWarnings.isEmpty())
    }

    @Test
    fun `counts stored usage when live usage has not caught up`() = runTest {
        val stored = FakeUsageSessionRepository(
            initial = listOf(
                UsageSession(
                    packageName = "com.instagram.android",
                    date = today(),
                    usedMinutes = 50
                )
            )
        )
        val (case, alerts) = useCase(
            usage = usage(),
            limits = listOf(limit("com.instagram.android", 60)),
            stored = stored
        )

        case()

        assertEquals(listOf(Triple("com.instagram.android", 50, 60)), alerts.usageWarnings)
    }

    @Test
    fun `turning usage warnings off suppresses them without marking them fired`() = runTest {
        val prefs = FakeNotificationPreferencesRepository().apply {
            disabled += AlertKind.USAGE_WARNING
        }
        val state = FakeSessionStateRepository()
        val (case, alerts) = useCase(
            usage = usage("com.instagram.android" to 50),
            limits = listOf(limit("com.instagram.android", 60)),
            state = state,
            prefs = prefs
        )

        case()

        assertTrue(alerts.usageWarnings.isEmpty())
        // Nothing recorded, so re-enabling later today still delivers the
        // warning while the app is inside its 80% window.
        prefs.disabled -= AlertKind.USAGE_WARNING
        case()
        assertEquals(1, alerts.usageWarnings.size)
    }
}
