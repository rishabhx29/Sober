package com.sober.core.domain.usecase

import com.sober.core.domain.model.AllowOnlyMode
import com.sober.core.domain.model.AppCategory
import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.BedtimeSettings
import com.sober.core.domain.model.CategoryLimit
import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.model.ScheduleRule
import com.sober.core.domain.model.UsageSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class IsBlockingInForceUseCaseTest {

    private val now = LocalDateTime.of(2026, 5, 20, 10, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    private fun useCase(
        state: FakeSessionStateRepository = FakeSessionStateRepository(),
        limits: FakeAppLimitRepository = FakeAppLimitRepository(),
        stats: FakeUsageStatsRepository = FakeUsageStatsRepository(),
        usage: FakeUsageSessionRepository = FakeUsageSessionRepository(),
        custom: FakeCustomRuleRepository = FakeCustomRuleRepository(),
        schedules: FakeScheduleRuleRepository = FakeScheduleRuleRepository(),
        protection: FakeProtectionSettingsRepository = FakeProtectionSettingsRepository(),
        categories: FakeAppCategoryRepository = FakeAppCategoryRepository()
    ) = IsBlockingInForceUseCase(
        sessionStateRepository = state,
        appLimitRepository = limits,
        usageStatsRepository = stats,
        usageSessionRepository = usage,
        customRuleRepository = custom,
        scheduleRuleRepository = schedules,
        protectionSettingsRepository = protection,
        appCategoryRepository = categories
    )

    @Test
    fun `nothing configured means nothing is in force`() = runTest {
        assertNull(useCase()(now))
    }

    @Test
    fun `an active focus session is in force`() = runTest {
        val state = FakeSessionStateRepository().apply { activeSessionId.value = 7 }

        assertEquals(BlockInForceReason.FOCUS_SESSION, useCase(state = state)(now))
    }

    @Test
    fun `a session with no blocked apps still counts while it runs`() = runTest {
        // Notification muting can be all-scope, so the session itself, not its
        // app list, is what the guard protects.
        val state = FakeSessionStateRepository().apply {
            activeSessionId.value = 7
            activeSessionBlockedApps.value = ""
        }

        assertEquals(BlockInForceReason.FOCUS_SESSION, useCase(state = state)(now))
    }

    @Test
    fun `a meal break lifts everything`() = runTest {
        val state = FakeSessionStateRepository().apply {
            activeSessionId.value = 7
            mealBreakUntil.value = now + 60_000L
        }

        assertNull(useCase(state = state)(now))
    }

    @Test
    fun `an expired meal break does not lift the session`() = runTest {
        val state = FakeSessionStateRepository().apply {
            activeSessionId.value = 7
            mealBreakUntil.value = now - 1L
        }

        assertEquals(BlockInForceReason.FOCUS_SESSION, useCase(state = state)(now))
    }

    @Test
    fun `a session that outlived its duration is no longer in force`() = runTest {
        val state = FakeSessionStateRepository().apply {
            withActiveSession(
                startTime = now - 30 * 60_000L,
                durationMs = 25 * 60_000L,
                blockedApps = listOf(INSTAGRAM)
            )
        }

        assertNull(useCase(state = state)(now))
    }

    @Test
    fun `pause everything lifts every reason`() = runTest {
        val state = FakeSessionStateRepository().apply { activeSessionId.value = 7 }
        val protection = FakeProtectionSettingsRepository().apply {
            pausedUntil.value = now + 60_000L
        }

        assertNull(useCase(state = state, protection = protection)(now))
    }

    @Test
    fun `allow only mode is in force`() = runTest {
        val protection = FakeProtectionSettingsRepository().apply {
            allowOnly.value = AllowOnlyMode(enabled = true, allowedPackages = setOf("com.example.notes"))
        }

        assertEquals(BlockInForceReason.ALLOW_ONLY, useCase(protection = protection)(now))
    }

    @Test
    fun `the bedtime window is in force`() = runTest {
        val protection = FakeProtectionSettingsRepository().apply {
            bedtime.value = BedtimeSettings(
                enabled = true,
                startMinuteOfDay = 0,
                endMinuteOfDay = 23 * 60 + 58,
                allowedPackages = setOf("com.example.alarm")
            )
        }

        assertEquals(BlockInForceReason.BEDTIME, useCase(protection = protection)(now))
    }

    @Test
    fun `a used-up category budget is in force`() = runTest {
        val protection = FakeProtectionSettingsRepository().apply {
            categoryLimits.value = listOf(
                CategoryLimit(category = AppCategory.SOCIAL, dailyLimitMinutes = 30)
            )
        }
        val categories = FakeAppCategoryRepository(
            categories = mapOf(INSTAGRAM to AppCategory.SOCIAL)
        )
        val stats = FakeUsageStatsRepository(
            listOf(AppUsage(INSTAGRAM, totalTimeInForeground = 31 * 60_000L, lastTimeUsed = now))
        )

        assertEquals(
            BlockInForceReason.CATEGORY_LIMIT,
            useCase(protection = protection, categories = categories, stats = stats)(now)
        )
    }

    @Test
    fun `a category budget under its cap is not in force`() = runTest {
        val protection = FakeProtectionSettingsRepository().apply {
            categoryLimits.value = listOf(
                CategoryLimit(category = AppCategory.SOCIAL, dailyLimitMinutes = 30)
            )
        }
        val categories = FakeAppCategoryRepository(
            categories = mapOf(INSTAGRAM to AppCategory.SOCIAL)
        )
        val stats = FakeUsageStatsRepository(
            listOf(AppUsage(INSTAGRAM, totalTimeInForeground = 29 * 60_000L, lastTimeUsed = now))
        )

        assertNull(useCase(protection = protection, categories = categories, stats = stats)(now))
    }

    @Test
    fun `a hand-blocked app is in force`() = runTest {
        val custom = FakeCustomRuleRepository()
        custom.add(CustomRuleType.BLOCKED_APP, "com.instagram.android")

        assertEquals(BlockInForceReason.CUSTOM_APP, useCase(custom = custom)(now))
    }

    @Test
    fun `a switched-off custom app is not in force`() = runTest {
        val custom = FakeCustomRuleRepository()
        val rule = custom.add(CustomRuleType.BLOCKED_APP, "com.instagram.android")
        custom.setEnabled(rule.id, false)

        assertNull(useCase(custom = custom)(now))
    }

    @Test
    fun `a muted word is not a block`() = runTest {
        val custom = FakeCustomRuleRepository()
        custom.add(CustomRuleType.NOTIFICATION_KEYWORD, "sale")

        assertNull(useCase(custom = custom)(now))
    }

    @Test
    fun `an open schedule window is in force`() = runTest {
        val schedules = FakeScheduleRuleRepository(
            listOf(
                ScheduleRule(
                    id = 1,
                    ruleName = "Work",
                    blockedPackages = listOf("com.instagram.android"),
                    startTime = "09:00",
                    endTime = "17:00"
                )
            )
        )

        assertEquals(BlockInForceReason.SCHEDULE, useCase(schedules = schedules)(now))
    }

    @Test
    fun `a schedule with no apps blocks nothing`() = runTest {
        val schedules = FakeScheduleRuleRepository(
            listOf(
                ScheduleRule(
                    id = 1,
                    ruleName = "Work",
                    blockedPackages = emptyList(),
                    startTime = "09:00",
                    endTime = "17:00"
                )
            )
        )

        assertNull(useCase(schedules = schedules)(now))
    }

    @Test
    fun `a switched-off schedule is ignored`() = runTest {
        val schedules = FakeScheduleRuleRepository(
            listOf(
                ScheduleRule(
                    id = 1,
                    ruleName = "Work",
                    blockedPackages = listOf("com.instagram.android"),
                    startTime = "09:00",
                    endTime = "17:00",
                    isActive = false
                )
            )
        )

        assertNull(useCase(schedules = schedules)(now))
    }

    @Test
    fun `a per-app limit that has been reached is in force`() = runTest {
        val limits = FakeAppLimitRepository(
            listOf(AppLimit(packageName = INSTAGRAM, appName = "Instagram", dailyLimitMinutes = 30))
        )
        val usage = FakeUsageSessionRepository(
            listOf(UsageSession(packageName = INSTAGRAM, date = today(), usedMinutes = 31))
        )

        assertEquals(BlockInForceReason.DAILY_LIMIT, useCase(limits = limits, usage = usage)(now))
    }

    @Test
    fun `a per-app limit under the cap is not in force`() = runTest {
        val limits = FakeAppLimitRepository(
            listOf(AppLimit(packageName = INSTAGRAM, appName = "Instagram", dailyLimitMinutes = 30))
        )
        val usage = FakeUsageSessionRepository(
            listOf(UsageSession(packageName = INSTAGRAM, date = today(), usedMinutes = 29))
        )

        assertNull(useCase(limits = limits, usage = usage)(now))
    }

    @Test
    fun `live usage counts when nothing has been stored yet`() = runTest {
        val limits = FakeAppLimitRepository(
            listOf(AppLimit(packageName = INSTAGRAM, appName = "Instagram", dailyLimitMinutes = 30))
        )
        val stats = FakeUsageStatsRepository(
            listOf(AppUsage(INSTAGRAM, totalTimeInForeground = 31 * 60_000L, lastTimeUsed = now))
        )

        assertEquals(BlockInForceReason.DAILY_LIMIT, useCase(limits = limits, stats = stats)(now))
    }

    @Test
    fun `a switched-off limit is ignored however much was used`() = runTest {
        val limits = FakeAppLimitRepository(
            listOf(
                AppLimit(
                    packageName = INSTAGRAM,
                    appName = "Instagram",
                    dailyLimitMinutes = 30,
                    isActive = false
                )
            )
        )
        val usage = FakeUsageSessionRepository(
            listOf(UsageSession(packageName = INSTAGRAM, date = today(), usedMinutes = 300))
        )

        assertNull(useCase(limits = limits, usage = usage)(now))
    }

    @Test
    fun `the overall cap is in force once the device total crosses it`() = runTest {
        val state = FakeSessionStateRepository().apply { dailyScreenTimeLimitMinutes.value = 60 }
        val stats = FakeUsageStatsRepository(
            listOf(
                AppUsage(INSTAGRAM, totalTimeInForeground = 30 * 60_000L, lastTimeUsed = now),
                AppUsage("com.google.android.youtube", totalTimeInForeground = 31 * 60_000L, lastTimeUsed = now)
            )
        )

        assertEquals(BlockInForceReason.DAILY_LIMIT, useCase(state = state, stats = stats)(now))
    }

    @Test
    fun `an overall cap of zero is off, not a zero-minute limit`() = runTest {
        val state = FakeSessionStateRepository().apply { dailyScreenTimeLimitMinutes.value = 0 }
        val stats = FakeUsageStatsRepository(
            listOf(AppUsage(INSTAGRAM, totalTimeInForeground = 600 * 60_000L, lastTimeUsed = now))
        )

        assertNull(useCase(state = state, stats = stats)(now))
    }

    @Test
    fun `a session outranks a limit in the reported reason`() = runTest {
        val state = FakeSessionStateRepository().apply { activeSessionId.value = 3 }
        val usage = FakeUsageSessionRepository(
            listOf(UsageSession(packageName = INSTAGRAM, date = today(), usedMinutes = 300))
        )
        val limits = FakeAppLimitRepository(
            listOf(AppLimit(packageName = INSTAGRAM, appName = "Instagram", dailyLimitMinutes = 30))
        )

        assertEquals(
            BlockInForceReason.FOCUS_SESSION,
            useCase(state = state, limits = limits, usage = usage)(now)
        )
    }

    private fun today(): String =
        SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US).format(Date())

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
    }
}
