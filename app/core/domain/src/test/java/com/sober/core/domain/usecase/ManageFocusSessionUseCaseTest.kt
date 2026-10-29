package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.model.NotificationSilenceScope
import com.sober.core.domain.model.UsageSession
import com.sober.core.domain.repository.AlertKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageFocusSessionUseCaseTest {

    private fun minutes(m: Long): Long = m * 60_000L

    private fun today(): String = SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US)
        .format(Date())

    private fun useCase(
        sessions: FakeFocusSessionRepository = FakeFocusSessionRepository(),
        state: FakeSessionStateRepository = FakeSessionStateRepository(),
        alerts: FakeAlertNotifier = FakeAlertNotifier(),
        prefs: FakeNotificationPreferencesRepository = FakeNotificationPreferencesRepository()
    ): Triple<ManageFocusSessionUseCase, FakeFocusSessionRepository, FakeSessionStateRepository> {
        val case = ManageFocusSessionUseCase(sessions, state, alerts, prefs)
        return Triple(case, sessions, state)
    }

    private fun decisionUseCase(
        limits: FakeAppLimitRepository = FakeAppLimitRepository(),
        usage: FakeUsageSessionRepository = FakeUsageSessionRepository(),
        state: FakeSessionStateRepository = FakeSessionStateRepository(),
        stats: FakeUsageStatsRepository = FakeUsageStatsRepository(),
        schedules: FakeScheduleRuleRepository = FakeScheduleRuleRepository()
    ) = EvaluateBlockDecisionUseCase(
        appLimitRepository = limits,
        usageSessionRepository = usage,
        sessionStateRepository = state,
        usageStatsRepository = stats,
        manageEmergencyOverrideUseCase = ManageEmergencyOverrideUseCase(
            FakeOverrideStateRepository(),
            FakeOverrideLogRepository()
        ),
        evaluateScheduleUseCase = EvaluateScheduleUseCase(schedules),
        accessibilityStateRepository = FakeAccessibilityStateRepository(),
        detectionRuleRepository = FakeDetectionRuleRepository(),
        sectionRulePreferencesRepository = FakeSectionRulePreferencesRepository(),
        customRuleRepository = FakeCustomRuleRepository()
    )

    @Test
    fun `starting a session persists it and publishes active state`() = runTest {
        val state = FakeSessionStateRepository()
        val (case, sessions, _) = useCase(state = state)

        val id = case.startSession("Deep Work", 25, listOf("com.instagram.android"))

        val stored = sessions.getSessionById(id.toInt())!!
        assertEquals("Deep Work", stored.label)
        assertEquals(25, stored.durationMinutes)
        assertEquals(id.toInt(), state.activeSessionId.value)
        assertEquals(minutes(25), state.activeSessionDuration.value)
        assertEquals("com.instagram.android", state.activeSessionBlockedApps.value)
    }

    @Test
    fun `completing a session marks it completed and clears active state`() = runTest {
        val state = FakeSessionStateRepository()
        val (case, sessions, _) = useCase(state = state)
        val id = case.startSession("Study", 15, emptyList()).toInt()

        case.completeSession(id)

        assertEquals("COMPLETED", sessions.getSessionById(id)!!.status.name)
        assertNull(state.activeSessionId.value)
    }

    @Test
    fun `cancelling records the minute and clears active state`() = runTest {
        val state = FakeSessionStateRepository()
        val (case, sessions, _) = useCase(state = state)
        val id = case.startSession("Gym", 60, listOf("a")).toInt()

        case.cancelSession(id)

        val stored = sessions.getSessionById(id)!!
        assertEquals("CANCELLED", stored.status.name)
        assertEquals(0, stored.cancelledAtMinute)
        assertNull(state.activeSessionId.value)
    }

    @Test
    fun `third cancellation of the day raises one streak warning`() = runTest {
        val alerts = FakeAlertNotifier()
        val (case, _, _) = useCase(alerts = alerts)

        repeat(3) {
            val id = case.startSession("Session $it", 5, emptyList()).toInt()
            case.cancelSession(id)
        }
        // A fourth cancellation must not re-fire the warning.
        val fourth = case.startSession("Session 4", 5, emptyList()).toInt()
        case.cancelSession(fourth)

        assertEquals(listOf(3), alerts.streakWarnings)
    }

    @Test
    fun `session complete alert honours the opt-out`() = runTest {
        val alerts = FakeAlertNotifier()
        val prefs = FakeNotificationPreferencesRepository().apply {
            disabled += AlertKind.SESSION_COMPLETE
        }
        val (case, sessions, _) = useCase(alerts = alerts, prefs = prefs)
        val id = case.startSession("Study", 15, emptyList()).toInt()

        case.completeSession(id)

        assertEquals("COMPLETED", sessions.getSessionById(id)!!.status.name)
        assertEquals(0, alerts.completedSessions.size)
    }

    @Test
    fun `streak warning honours the opt-out`() = runTest {
        val alerts = FakeAlertNotifier()
        val prefs = FakeNotificationPreferencesRepository().apply {
            disabled += AlertKind.STREAK_WARNING
        }
        val (case, _, _) = useCase(alerts = alerts, prefs = prefs)

        repeat(3) {
            val id = case.startSession("Session $it", 5, emptyList()).toInt()
            case.cancelSession(id)
        }

        assertEquals(0, alerts.streakWarnings.size)
    }

    @Test
    fun `daily limit alone does not block an app under its minutes`() = runTest {
        val limits = FakeAppLimitRepository(
            listOf(AppLimit(packageName = "com.instagram.android", appName = "Instagram", dailyLimitMinutes = 30))
        )
        val usage = FakeUsageSessionRepository(
            listOf(UsageSession(packageName = "com.instagram.android", date = today(), usedMinutes = 10))
        )
        val stats = FakeUsageStatsRepository(usage = emptyList())
        val state = FakeSessionStateRepository()
        state.dailyScreenTimeLimitMinutes.value = 0

        val decision = decisionUseCase(limits, usage, state, stats)("com.instagram.android")

        assertNull(decision)
    }

    @Test
    fun `focus session outranks a per-app daily limit`() = runTest {
        val limits = FakeAppLimitRepository(
            listOf(AppLimit(packageName = "com.instagram.android", appName = "Instagram", dailyLimitMinutes = 10))
        )
        val usage = FakeUsageSessionRepository(
            listOf(UsageSession(packageName = "com.instagram.android", date = today(), usedMinutes = 40))
        )
        val stats = FakeUsageStatsRepository(usage = emptyList())
        val start = System.currentTimeMillis() - minutes(5)
        val state = FakeSessionStateRepository().withActiveSession(
            startTime = start,
            durationMs = minutes(25),
            label = "Deep Work",
            blockedApps = listOf("com.instagram.android")
        )

        val decision = decisionUseCase(limits, usage, state, stats)("com.instagram.android")!!

        assertEquals("FOCUS_SESSION", decision.reason.name)
        assertEquals("Deep Work", decision.sessionLabel)
        assertEquals(start + minutes(25), decision.sessionEndsAt)
    }

    @Test
    fun `null foreground never produces a decision`() = runTest {
        val state = FakeSessionStateRepository()
        val decision = decisionUseCase(state = state)(null)

        assertNull(decision)
    }

    @Test
    fun `overall cap blocks any app once the device total is reached`() = runTest {
        val stats = FakeUsageStatsRepository(
            usage = listOf(
                com.sober.core.domain.model.AppUsage("a", minutes(120), 0),
                com.sober.core.domain.model.AppUsage("b", minutes(70), 0)
            )
        )
        val state = FakeSessionStateRepository()
        state.dailyScreenTimeLimitMinutes.value = 180

        val decision = decisionUseCase(state = state, stats = stats)("b")!!

        assertEquals("OVERALL_DAILY_LIMIT", decision.reason.name)
        assertEquals(190, decision.usedMinutes)
        assertEquals(180, decision.limitMinutes)
    }
}
