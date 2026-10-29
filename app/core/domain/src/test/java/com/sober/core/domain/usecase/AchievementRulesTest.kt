package com.sober.core.domain.usecase

import com.sober.core.domain.model.AchievementId
import com.sober.core.domain.model.UsageInterval
import com.sober.core.domain.model.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class AchievementRulesTest {

    private val zone = ZoneId.of("UTC")

    /** 2026-03-02 is a Monday. */
    private fun at(hour: Int, minute: Int = 0, dayOfMonth: Int = 2): Long =
        LocalDateTime.of(2026, 3, dayOfMonth, hour, minute)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

    private fun session(date: String, packageName: String, minutes: Int) =
        UsageSession(packageName = packageName, date = date, usedMinutes = minutes)

    private fun snapshot(
        streakDays: Int = 0,
        completedSessions: Int = 0,
        socialFreeDay: Boolean = false,
        pomodoroDates: Set<String> = emptySet(),
        nightDates: List<String> = emptyList(),
        observedDates: Set<String> = emptySet(),
        lateNightDates: Set<String> = emptySet()
    ) = AchievementSnapshot(
        streakDays = streakDays,
        completedSessions = completedSessions,
        socialFreeDay = socialFreeDay,
        pomodoroDates = pomodoroDates,
        nightDates = nightDates,
        observedDates = observedDates,
        lateNightDates = lateNightDates
    )

    @Test
    fun `consecutive days are detected regardless of input order`() {
        assertTrue(
            hasConsecutiveDays(
                setOf("2026-03-06", "2026-03-02", "2026-03-04", "2026-03-03", "2026-03-05"),
                5
            )
        )
    }

    @Test
    fun `a gap breaks the chain`() {
        assertFalse(
            hasConsecutiveDays(
                listOf("2026-03-02", "2026-03-03", "2026-03-05", "2026-03-06", "2026-03-07"),
                5
            )
        )
    }

    @Test
    fun `duplicate dates do not inflate the chain`() {
        assertFalse(
            hasConsecutiveDays(
                listOf("2026-03-02", "2026-03-02", "2026-03-03", "2026-03-04", "2026-03-05"),
                5
            )
        )
    }

    @Test
    fun `too few days cannot satisfy the chain`() {
        assertFalse(hasConsecutiveDays(listOf("2026-03-02", "2026-03-03"), 5))
        assertFalse(hasConsecutiveDays(emptyList(), 5))
    }

    @Test
    fun `one day satisfies a chain of one`() {
        assertTrue(hasConsecutiveDays(listOf("2026-03-02"), 1))
    }

    @Test
    fun `social detox needs a day with other usage and no social minutes`() {
        val rows = listOf(
            session("2026-03-02", "com.instagram.android", 20),
            session("2026-03-02", "com.android.chrome", 30),
            session("2026-03-03", "com.android.chrome", 40)
        )

        assertTrue(
            socialFreeDay(
                rows,
                listOf("2026-03-02", "2026-03-03"),
                socialPackages = setOf("com.instagram.android")
            )
        )
    }

    @Test
    fun `a day with social usage is not a detox day`() {
        val rows = listOf(
            session("2026-03-02", "com.instagram.android", 5),
            session("2026-03-02", "com.android.chrome", 30)
        )

        assertFalse(
            socialFreeDay(
                rows,
                listOf("2026-03-02"),
                socialPackages = setOf("com.instagram.android")
            )
        )
    }

    @Test
    fun `a day without any usage is not counted`() {
        val rows = listOf(session("2026-03-02", "com.android.chrome", 30))

        assertFalse(
            socialFreeDay(
                rows,
                listOf("2026-03-03"),
                socialPackages = setOf("com.instagram.android")
            )
        )
    }

    @Test
    fun `no social apps installed means nothing to abstain from`() {
        val rows = listOf(session("2026-03-02", "com.android.chrome", 30))

        assertFalse(socialFreeDay(rows, listOf("2026-03-02"), socialPackages = emptySet()))
    }

    @Test
    fun `usage after the cutoff marks the night`() {
        val result = lateNightDates(
            listOf(UsageInterval(at(23), at(23, 30))),
            cutoffHour = 22,
            zone = zone
        )

        assertEquals(setOf("2026-03-02"), result)
    }

    @Test
    fun `an interval crossing the cutoff marks the night`() {
        val result = lateNightDates(
            listOf(UsageInterval(at(21, 30), at(22, 30))),
            cutoffHour = 22,
            zone = zone
        )

        assertEquals(setOf("2026-03-02"), result)
    }

    @Test
    fun `evening usage before the cutoff does not mark the night`() {
        val result = lateNightDates(
            listOf(UsageInterval(at(20), at(21, 30))),
            cutoffHour = 22,
            zone = zone
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `an interval running past midnight marks only its starting evening`() {
        val result = lateNightDates(
            listOf(UsageInterval(at(23), at(1, 0, dayOfMonth = 3))),
            cutoffHour = 22,
            zone = zone
        )

        assertEquals(setOf("2026-03-02"), result)
    }

    @Test
    fun `early morning usage belongs to the previous night only`() {
        val result = lateNightDates(
            listOf(UsageInterval(at(0, 30, dayOfMonth = 4), at(1, 0, dayOfMonth = 4))),
            cutoffHour = 22,
            zone = zone
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `night owl requires five observed nights without late usage`() {
        val nights = listOf("2026-03-01", "2026-03-02", "2026-03-03", "2026-03-04", "2026-03-05")

        assertTrue(
            isNightOwl(nights, observedDates = nights.toSet(), lateNightDates = emptySet())
        )
        assertFalse(
            isNightOwl(
                nights,
                observedDates = nights.toSet(),
                lateNightDates = setOf("2026-03-03")
            )
        )
        assertFalse(
            isNightOwl(
                nights,
                observedDates = nights.toSet() - "2026-03-04",
                lateNightDates = emptySet()
            )
        )
        assertFalse(isNightOwl(nights.take(4), nights.take(4).toSet(), emptySet()))
    }

    @Test
    fun `streak achievements unlock at their exact thresholds`() {
        assertFalse(AchievementId.SEVEN_DAY_STREAK in evaluateAchievements(snapshot(streakDays = 6)))
        assertTrue(AchievementId.SEVEN_DAY_STREAK in evaluateAchievements(snapshot(streakDays = 7)))
        assertFalse(AchievementId.SOBER_PRO in evaluateAchievements(snapshot(streakDays = 29)))
        assertTrue(AchievementId.SOBER_PRO in evaluateAchievements(snapshot(streakDays = 30)))
    }

    @Test
    fun `focus master needs ten completed sessions`() {
        assertFalse(AchievementId.FOCUS_MASTER in evaluateAchievements(snapshot(completedSessions = 9)))
        assertTrue(AchievementId.FOCUS_MASTER in evaluateAchievements(snapshot(completedSessions = 10)))
    }

    @Test
    fun `speed demon needs five consecutive pomodoro days`() {
        val consecutive = setOf("2026-03-02", "2026-03-03", "2026-03-04", "2026-03-05", "2026-03-06")
        val broken = setOf("2026-03-02", "2026-03-03", "2026-03-05", "2026-03-06", "2026-03-07")

        assertTrue(
            AchievementId.SPEED_DEMON in evaluateAchievements(snapshot(pomodoroDates = consecutive))
        )
        assertFalse(
            AchievementId.SPEED_DEMON in evaluateAchievements(snapshot(pomodoroDates = broken))
        )
    }

    @Test
    fun `social and night achievements follow their inputs`() {
        val nights = listOf("2026-03-01", "2026-03-02", "2026-03-03", "2026-03-04", "2026-03-05")
        val earned = evaluateAchievements(
            snapshot(
                socialFreeDay = true,
                nightDates = nights,
                observedDates = nights.toSet()
            )
        )

        assertTrue(AchievementId.SOCIAL_DETOX in earned)
        assertTrue(AchievementId.NIGHT_OWL in earned)
    }

    @Test
    fun `an empty snapshot unlocks nothing`() {
        assertTrue(evaluateAchievements(snapshot()).isEmpty())
    }
}
