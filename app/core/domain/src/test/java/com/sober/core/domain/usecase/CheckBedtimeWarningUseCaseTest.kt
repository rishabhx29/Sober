package com.sober.core.domain.usecase

import com.sober.core.domain.model.ScheduleRule
import com.sober.core.domain.repository.AlertKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CheckBedtimeWarningUseCaseTest {

    private fun todayAt(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun sleepRule(
        id: Int = 1,
        name: String = "Sleep Mode",
        start: String = "22:30",
        end: String = "07:00",
        days: List<Int> = emptyList(),
        active: Boolean = true
    ) = ScheduleRule(
        id = id,
        ruleName = name,
        blockedPackages = listOf("com.example.social"),
        daysOfWeek = days,
        startTime = start,
        endTime = end,
        isActive = active
    )

    private fun useCase(
        rules: List<ScheduleRule>,
        notifier: FakeAlertNotifier,
        state: FakeSessionStateRepository = FakeSessionStateRepository(),
        prefs: FakeNotificationPreferencesRepository = FakeNotificationPreferencesRepository()
    ) = CheckBedtimeWarningUseCase(
        FakeScheduleRuleRepository(rules),
        state,
        notifier,
        prefs
    )

    @Test
    fun `a sleep rule starting in fifteen minutes warns`() = runTest {
        val notifier = FakeAlertNotifier()

        useCase(listOf(sleepRule(start = "22:45")), notifier)(todayAt(22, 30))

        assertEquals(listOf("Sleep Mode" to 15), notifier.bedtimeWarnings)
    }

    @Test
    fun `a sleep rule starting in twenty minutes does not warn yet`() = runTest {
        val notifier = FakeAlertNotifier()

        useCase(listOf(sleepRule(start = "22:50")), notifier)(todayAt(22, 30))

        assertTrue(notifier.bedtimeWarnings.isEmpty())
    }

    @Test
    fun `a non-sleep rule is ignored`() = runTest {
        val notifier = FakeAlertNotifier()

        useCase(listOf(sleepRule(name = "Work Mode", start = "09:00")), notifier)(todayAt(8, 50))

        assertTrue(notifier.bedtimeWarnings.isEmpty())
    }

    @Test
    fun `a rule one minute away reports one minute`() = runTest {
        val notifier = FakeAlertNotifier()

        useCase(listOf(sleepRule(start = "22:31")), notifier)(todayAt(22, 30))

        assertEquals(listOf("Sleep Mode" to 1), notifier.bedtimeWarnings)
    }

    @Test
    fun `an inactive rule never warns`() = runTest {
        val notifier = FakeAlertNotifier()

        useCase(listOf(sleepRule(start = "22:45", active = false)), notifier)(todayAt(22, 30))

        assertTrue(notifier.bedtimeWarnings.isEmpty())
    }

    @Test
    fun `the warning fires once per rule per day`() = runTest {
        val notifier = FakeAlertNotifier()
        val useCase = useCase(listOf(sleepRule(start = "22:45")), notifier)

        useCase(todayAt(22, 30))
        useCase(todayAt(22, 35))

        assertEquals(1, notifier.bedtimeWarnings.size)
    }

    @Test
    fun `the earliest upcoming sleep rule wins`() = runTest {
        val notifier = FakeAlertNotifier()

        useCase(
            listOf(
                sleepRule(id = 1, name = "Sleep Mode", start = "22:44"),
                sleepRule(id = 2, name = "Sleep early", start = "22:36")
            ),
            notifier
        )(todayAt(22, 30))

        assertEquals(listOf("Sleep early" to 6), notifier.bedtimeWarnings)
    }

    @Test
    fun `a start after midnight is measured from today`() = runTest {
        val notifier = FakeAlertNotifier()

        useCase(listOf(sleepRule(start = "00:05", end = "07:00")), notifier)(todayAt(23, 55))

        assertEquals(listOf("Sleep Mode" to 10), notifier.bedtimeWarnings)
    }

    @Test
    fun `a rule that already started today does not warn`() = runTest {
        val notifier = FakeAlertNotifier()

        // 08:00 start, now 22:00: today's window has opened and tomorrow's is
        // hours away, so there is nothing to announce.
        useCase(listOf(sleepRule(start = "08:00", end = "09:00")), notifier)(todayAt(22, 0))

        assertTrue(notifier.bedtimeWarnings.isEmpty())
    }

    @Test
    fun `the rule must be scheduled for today`() = runTest {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val isoToday = if (today == Calendar.SUNDAY) 7 else today - 1
        val notifier = FakeAlertNotifier()

        val otherDay = if (isoToday == 7) 1 else isoToday + 1
        useCase(
            listOf(sleepRule(start = "22:45", days = listOf(otherDay))),
            notifier
        )(todayAt(22, 30))

        assertTrue(notifier.bedtimeWarnings.isEmpty())
    }

    @Test
    fun `a disabled preference suppresses the warning`() = runTest {
        val notifier = FakeAlertNotifier()
        val prefs = FakeNotificationPreferencesRepository().apply {
            disabled += AlertKind.BEDTIME_WARNING
        }

        useCase(listOf(sleepRule(start = "22:45")), notifier, prefs = prefs)(todayAt(22, 30))

        assertTrue(notifier.bedtimeWarnings.isEmpty())
    }

    @Test
    fun `a malformed start time never warns`() = runTest {
        val notifier = FakeAlertNotifier()

        useCase(listOf(sleepRule(start = "25:99")), notifier)(todayAt(22, 30))

        assertTrue(notifier.bedtimeWarnings.isEmpty())
    }
}
