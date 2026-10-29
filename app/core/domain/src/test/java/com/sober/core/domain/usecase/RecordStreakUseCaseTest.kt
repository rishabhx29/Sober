package com.sober.core.domain.usecase

import com.sober.core.domain.model.FocusSession
import com.sober.core.domain.model.SessionStatus
import com.sober.core.domain.model.StreakRecord
import com.sober.core.domain.model.UsageSession
import com.sober.core.domain.repository.StreakRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RecordStreakUseCaseTest {

    private val dates = GetUsageHistoryUseCase.recentDates(3)
    private val dayBeforeYesterday = dates[0]
    private val yesterday = dates[1]
    private val today = dates[2]
    private val todayDate = LocalDate.parse(today)

    private fun row(packageName: String, date: String, minutes: Int) =
        UsageSession(packageName = packageName, date = date, usedMinutes = minutes)

    private fun capState(minutes: Int = 120) = FakeSessionStateRepository().apply {
        dailyScreenTimeLimitMinutes.value = minutes
    }

    private fun useCase(
        usage: FakeUsageSessionRepository = FakeUsageSessionRepository(),
        state: FakeSessionStateRepository = capState(),
        sessions: FakeFocusSessionRepository = FakeFocusSessionRepository(),
        streakPrefs: FakeStreakPreferencesRepository = FakeStreakPreferencesRepository(),
        records: FakeStreakRecordRepository = FakeStreakRecordRepository()
    ): Pair<RecordStreakUseCase, FakeStreakRecordRepository> {
        val calculate = CalculateStreakUseCase(
            usageSessionRepository = usage,
            appLimitRepository = FakeAppLimitRepository(),
            sessionStateRepository = state,
            focusSessionRepository = sessions,
            streakPreferencesRepository = streakPrefs
        )
        return RecordStreakUseCase(calculate, records) to records
    }

    private fun cancelledSessionOn(date: String): FocusSession {
        val noon = LocalDate.parse(date).atTime(12, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return FocusSession(
            label = "Deep Work",
            durationMinutes = 25,
            startedAt = noon - 25 * 60_000L,
            endedAt = noon,
            status = SessionStatus.CANCELLED
        )
    }

    @Test
    fun `the closed clean day is recorded under yesterday's date`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", yesterday, 60))
        )
        val (record, records) = useCase(usage = usage)

        record(todayDate)

        assertEquals(listOf(1 to LocalDate.parse(yesterday)), records.recordedDays)
        val stored = records.record().first()
        assertEquals(1, stored.currentStreak)
        assertEquals(1, stored.longestStreak)
        assertEquals(yesterday, stored.lastSuccessDate)
    }

    @Test
    fun `today's in-progress overage is not what gets recorded`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(
                row("com.instagram.android", yesterday, 60),
                row("com.instagram.android", today, 500)
            )
        )
        val (record, records) = useCase(usage = usage)

        record(todayDate)

        assertEquals(1, records.record().first().currentStreak)
        assertEquals(yesterday, records.record().first().lastSuccessDate)
    }

    @Test
    fun `a failed yesterday records nothing`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", yesterday, 200))
        )
        val (record, records) = useCase(usage = usage)

        record(todayDate)

        assertTrue(records.recordedDays.isEmpty())
        assertEquals(StreakRecord(), records.record().first())
    }

    @Test
    fun `an unobserved yesterday is not recorded as a success`() = runTest {
        val (record, records) = useCase()

        record(todayDate)

        // No usage rows at all means nothing was observed, not that the user
        // spent zero minutes.
        assertTrue(records.recordedDays.isEmpty())
    }

    @Test
    fun `no limits leave nothing to succeed at`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", yesterday, 10))
        )
        val (record, records) = useCase(usage = usage, state = capState(minutes = 0))

        record(todayDate)

        assertTrue(records.recordedDays.isEmpty())
    }

    @Test
    fun `recording twice in a day does not double the streak`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", yesterday, 60))
        )
        val (record, records) = useCase(usage = usage)

        record(todayDate)
        record(todayDate)

        assertEquals(1, records.record().first().currentStreak)
        assertEquals(1, records.record().first().longestStreak)
    }

    @Test
    fun `a streak longer than the history window keeps growing`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", yesterday, 60))
        )
        // The derived chain can only see yesterday, so the stored count is what
        // carries the 60 days that already fell out of the window.
        val stored = StreakRecord(
            currentStreak = 60,
            longestStreak = 60,
            lastSuccessDate = dayBeforeYesterday
        )
        val (record, records) = useCase(
            usage = usage,
            records = FakeStreakRecordRepository(stored)
        )

        record(todayDate)

        assertEquals(61, records.record().first().currentStreak)
        assertEquals(61, records.record().first().longestStreak)
    }

    @Test
    fun `a stale record falls back to the computed chain`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(
                row("com.instagram.android", dayBeforeYesterday, 30),
                row("com.instagram.android", yesterday, 40)
            )
        )
        val stored = StreakRecord(
            currentStreak = 20,
            longestStreak = 20,
            lastSuccessDate = LocalDate.parse(yesterday).minusDays(10).toString()
        )
        val (record, records) = useCase(
            usage = usage,
            records = FakeStreakRecordRepository(stored)
        )

        record(todayDate)

        val result = records.record().first()
        assertEquals(2, result.currentStreak)
        assertEquals(20, result.longestStreak)
        assertEquals(yesterday, result.lastSuccessDate)
    }

    @Test
    fun `a cancelled session breaks a day that would otherwise count`() = runTest {
        val usage = FakeUsageSessionRepository(
            listOf(row("com.instagram.android", yesterday, 60))
        )
        val (record, records) = useCase(
            usage = usage,
            sessions = FakeFocusSessionRepository(listOf(cancelledSessionOn(yesterday))),
            streakPrefs = FakeStreakPreferencesRepository(StreakRule.LIMITS_AND_NO_CANCELLATIONS)
        )

        record(todayDate)

        assertTrue(records.recordedDays.isEmpty())
    }
}
