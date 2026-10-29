package com.sober.core.domain.usecase

import com.sober.core.domain.model.AchievementId
import com.sober.core.domain.model.FocusSession
import com.sober.core.domain.model.SessionStatus
import com.sober.core.domain.model.UsageInterval
import com.sober.core.domain.model.UsageSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetAchievementsUseCaseTest {

    private val now = System.currentTimeMillis()
    private val zone = ZoneId.systemDefault()

    private fun epochAt(date: String, hour: Int): Long =
        LocalDate.parse(date).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun session(packageName: String, date: String, minutes: Int) =
        UsageSession(packageName = packageName, date = date, usedMinutes = minutes)

    private fun completedSession(endedAt: Long, durationMinutes: Int = 50) = FocusSession(
        label = "Focus",
        durationMinutes = durationMinutes,
        startedAt = endedAt - durationMinutes * 60_000L,
        endedAt = endedAt,
        status = SessionStatus.COMPLETED
    )

    private fun useCase(
        usage: FakeUsageSessionRepository = FakeUsageSessionRepository(),
        focus: FakeFocusSessionRepository = FakeFocusSessionRepository(),
        stats: FakeUsageStatsRepository = FakeUsageStatsRepository(),
        categories: FakeAppCategoryRepository = FakeAppCategoryRepository(),
        achievements: FakeAchievementRepository = FakeAchievementRepository(),
        sessionState: FakeSessionStateRepository = FakeSessionStateRepository(),
        appLimits: FakeAppLimitRepository = FakeAppLimitRepository()
    ): GetAchievementsUseCase {
        val streak = CalculateStreakUseCase(
            usageSessionRepository = usage,
            appLimitRepository = appLimits,
            sessionStateRepository = sessionState,
            focusSessionRepository = focus,
            streakPreferencesRepository = FakeStreakPreferencesRepository()
        )
        return GetAchievementsUseCase(
            usageSessionRepository = usage,
            focusSessionRepository = focus,
            usageStatsRepository = stats,
            appCategoryRepository = categories,
            achievementRepository = achievements,
            calculateStreakUseCase = streak
        )
    }

    @Test
    fun `everything is locked when there is no evidence`() = runTest {
        val achievements = FakeAchievementRepository()

        val result = useCase(achievements = achievements)(nowMillis = now, zone = zone)

        assertEquals(AchievementId.entries.size, result.size)
        assertTrue(result.none { it.isUnlocked })
        assertTrue(achievements.unlocked.value.isEmpty())
    }

    @Test
    fun `ten completed sessions unlock Focus Master and are persisted`() = runTest {
        val focus = FakeFocusSessionRepository(
            (0 until 10).map { completedSession(now - (it + 1) * 60_000L) }
        )
        val achievements = FakeAchievementRepository()

        val result = useCase(
            focus = focus,
            achievements = achievements
        )(nowMillis = now, zone = zone)

        val master = result.first { it.id == AchievementId.FOCUS_MASTER }
        assertTrue(master.isUnlocked)
        assertNotNull(master.unlockedAtMillis)
        assertTrue(achievements.unlocked.value.containsKey(AchievementId.FOCUS_MASTER))
    }

    @Test
    fun `nine completed sessions are not enough`() = runTest {
        val focus = FakeFocusSessionRepository(
            (0 until 9).map { completedSession(now - (it + 1) * 60_000L) }
        )

        val master = useCase(focus = focus)(nowMillis = now, zone = zone)
            .first { it.id == AchievementId.FOCUS_MASTER }

        assertFalse(master.isUnlocked)
    }

    @Test
    fun `previously stored unlocks survive without current evidence`() = runTest {
        val storedAt = now - 5 * 24 * 60 * 60 * 1000L
        val achievements = FakeAchievementRepository(
            mapOf(AchievementId.SEVEN_DAY_STREAK to storedAt)
        )

        val result = useCase(achievements = achievements)(nowMillis = now, zone = zone)

        val streak = result.first { it.id == AchievementId.SEVEN_DAY_STREAK }
        assertTrue(streak.isUnlocked)
        assertEquals(storedAt, streak.unlockedAtMillis)
    }

    @Test
    fun `a new unlock keeps the instant it was first earned`() = runTest {
        val firstSeen = now - 30_000L
        val focus = FakeFocusSessionRepository(
            (0 until 10).map { completedSession(now - (it + 1) * 60_000L) }
        )
        val achievements = FakeAchievementRepository()

        useCase(focus = focus, achievements = achievements)(nowMillis = firstSeen, zone = zone)

        assertEquals(firstSeen, achievements.unlocked.value[AchievementId.FOCUS_MASTER])
    }

    @Test
    fun `seven under-limit days unlock the streak badge`() = runTest {
        val dates = GetUsageHistoryUseCase.recentDates(8).takeLast(7)
        val usage = FakeUsageSessionRepository(
            dates.map { session("com.android.chrome", it, 30) }
        )
        val sessionState = FakeSessionStateRepository().apply {
            dailyScreenTimeLimitMinutes.value = 120
        }

        val result = useCase(usage = usage, sessionState = sessionState)(nowMillis = now, zone = zone)

        assertTrue(result.first { it.id == AchievementId.SEVEN_DAY_STREAK }.isUnlocked)
        assertFalse(result.first { it.id == AchievementId.SOBER_PRO }.isUnlocked)
    }

    @Test
    fun `five consecutive pomodoros unlock Speed Demon`() = runTest {
        val dates = GetUsageHistoryUseCase.recentDates(30).dropLast(6).takeLast(5)
        val focus = FakeFocusSessionRepository(
            dates.map { completedSession(epochAt(it, 10), durationMinutes = 25) }
        )

        val result = useCase(focus = focus)(nowMillis = now, zone = zone)

        assertTrue(result.first { it.id == AchievementId.SPEED_DEMON }.isUnlocked)
    }

    @Test
    fun `pomodoros with a missing day do not unlock Speed Demon`() = runTest {
        val dates = GetUsageHistoryUseCase.recentDates(30).dropLast(7).takeLast(6)
            .filterIndexed { index, _ -> index != 2 }
        val focus = FakeFocusSessionRepository(
            dates.map { completedSession(epochAt(it, 10), durationMinutes = 25) }
        )

        val result = useCase(focus = focus)(nowMillis = now, zone = zone)

        assertFalse(result.first { it.id == AchievementId.SPEED_DEMON }.isUnlocked)
    }

    @Test
    fun `five quiet observed nights unlock Night Owl`() = runTest {
        val nights = GetUsageHistoryUseCase.recentDates(30).dropLast(1).takeLast(5)
        val usage = FakeUsageSessionRepository(
            nights.map { session("com.android.chrome", it, 30) }
        )

        val result = useCase(usage = usage)(nowMillis = now, zone = zone)

        assertTrue(result.first { it.id == AchievementId.NIGHT_OWL }.isUnlocked)
    }

    @Test
    fun `one late night blocks Night Owl`() = runTest {
        val nights = GetUsageHistoryUseCase.recentDates(30).dropLast(1).takeLast(5)
        val usage = FakeUsageSessionRepository(
            nights.map { session("com.android.chrome", it, 30) }
        )
        val lateNight = nights[2]
        val stats = FakeUsageStatsRepository(
            intervals = listOf(
                UsageInterval(epochAt(lateNight, 23), epochAt(lateNight, 23) + 30 * 60_000L)
            )
        )

        val result = useCase(usage = usage, stats = stats)(nowMillis = now, zone = zone)

        assertFalse(result.first { it.id == AchievementId.NIGHT_OWL }.isUnlocked)
    }

    @Test
    fun `a day without social apps unlocks Social Detox`() = runTest {
        val date = GetUsageHistoryUseCase.recentDates(2).first()
        val usage = FakeUsageSessionRepository(listOf(session("com.android.chrome", date, 30)))
        val categories = FakeAppCategoryRepository(setOf("com.instagram.android"))

        val result = useCase(usage = usage, categories = categories)(nowMillis = now, zone = zone)

        assertTrue(result.first { it.id == AchievementId.SOCIAL_DETOX }.isUnlocked)
    }

    @Test
    fun `social usage on every used day keeps Social Detox locked`() = runTest {
        val dates = GetUsageHistoryUseCase.recentDates(3)
        val usage = FakeUsageSessionRepository(
            dates.flatMap { date ->
                listOf(
                    session("com.instagram.android", date, 10),
                    session("com.android.chrome", date, 20)
                )
            }
        )
        val categories = FakeAppCategoryRepository(setOf("com.instagram.android"))

        val result = useCase(usage = usage, categories = categories)(nowMillis = now, zone = zone)

        assertFalse(result.first { it.id == AchievementId.SOCIAL_DETOX }.isUnlocked)
    }
}
