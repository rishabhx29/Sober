package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.repository.DetectionRuleCatalog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateBlockDecisionUseCaseTest {

    private fun useCase(
        state: FakeSessionStateRepository = FakeSessionStateRepository(),
        limits: FakeAppLimitRepository = FakeAppLimitRepository(),
        usage: FakeUsageStatsRepository = FakeUsageStatsRepository(),
        overrides: FakeOverrideStateRepository = FakeOverrideStateRepository(),
        schedules: FakeScheduleRuleRepository = FakeScheduleRuleRepository(),
        accessibility: FakeAccessibilityStateRepository = FakeAccessibilityStateRepository(),
        rules: FakeDetectionRuleRepository = FakeDetectionRuleRepository(),
        sectionRules: FakeSectionRulePreferencesRepository =
            FakeSectionRulePreferencesRepository(),
        customRules: FakeCustomRuleRepository = FakeCustomRuleRepository()
    ) = EvaluateBlockDecisionUseCase(
        appLimitRepository = limits,
        usageSessionRepository = FakeUsageSessionRepository(),
        sessionStateRepository = state,
        usageStatsRepository = usage,
        manageEmergencyOverrideUseCase = ManageEmergencyOverrideUseCase(
            overrideStateRepository = overrides,
            overrideLogRepository = FakeOverrideLogRepository()
        ),
        evaluateScheduleUseCase = EvaluateScheduleUseCase(schedules),
        accessibilityStateRepository = accessibility,
        detectionRuleRepository = rules,
        sectionRulePreferencesRepository = sectionRules,
        customRuleRepository = customRules
    )

    private fun activeSession(vararg blocked: String) = FakeSessionStateRepository().apply {
        withActiveSession(
            startTime = System.currentTimeMillis() - 60_000L,
            durationMs = 25 * 60_000L,
            blockedApps = blocked.toList()
        )
    }

    @Test
    fun `a running meal break lifts focus-session blocks`() = runTest {
        val state = activeSession("com.instagram.android").apply {
            mealBreakUntil.value = System.currentTimeMillis() + 60_000L
        }

        assertNull(useCase(state = state)("com.instagram.android"))
    }

    @Test
    fun `an expired meal break does not lift blocks`() = runTest {
        val state = activeSession("com.instagram.android").apply {
            mealBreakUntil.value = System.currentTimeMillis() - 1_000L
        }

        val decision = useCase(state = state)("com.instagram.android")

        assertEquals(BlockReason.FOCUS_SESSION, decision?.reason)
    }

    @Test
    fun `a running meal break lifts app daily limits`() = runTest {
        val limits = FakeAppLimitRepository(
            listOf(
                com.sober.core.domain.model.AppLimit(
                    packageName = "com.instagram.android",
                    appName = "Instagram",
                    dailyLimitMinutes = 30
                )
            )
        )
        val state = FakeSessionStateRepository().apply {
            mealBreakUntil.value = System.currentTimeMillis() + 60_000L
        }
        val usage = FakeUsageStatsRepository(
            usage = listOf(
                AppUsage(
                    packageName = "com.instagram.android",
                    totalTimeInForeground = 45 * 60_000L,
                    lastTimeUsed = System.currentTimeMillis()
                )
            )
        )

        assertNull(useCase(state = state, limits = limits, usage = usage)("com.instagram.android"))
    }

    @Test
    fun `a blocked app is still reported when no meal break is running`() = runTest {
        val state = activeSession("com.instagram.android")

        val decision = useCase(state = state)("com.instagram.android")

        assertEquals(BlockReason.FOCUS_SESSION, decision?.reason)
        assertEquals("Deep Work", decision?.sessionLabel)
    }

    @Test
    fun `an open-ended session blocks without reporting an end time`() = runTest {
        val state = FakeSessionStateRepository().apply {
            withActiveSession(
                startTime = System.currentTimeMillis() - 60_000L,
                durationMs = 0L,
                blockedApps = listOf("com.instagram.android")
            )
        }

        val decision = useCase(state = state)("com.instagram.android")

        assertEquals(BlockReason.FOCUS_SESSION, decision?.reason)
        assertNull(decision?.sessionEndsAt)
    }

    @Test
    fun `an active override lifts the block for that app only`() = runTest {
        val state = activeSession("com.instagram.android", "com.example.other")
        val overrides = FakeOverrideStateRepository().apply {
            overridePackage.value = "com.instagram.android"
            overrideUntil.value = System.currentTimeMillis() + 60_000L
        }
        val evaluate = useCase(state = state, overrides = overrides)

        assertNull(evaluate("com.instagram.android"))
        assertEquals(BlockReason.FOCUS_SESSION, evaluate("com.example.other")?.reason)
    }

    @Test
    fun `an expired override blocks again`() = runTest {
        val state = activeSession("com.instagram.android")
        val overrides = FakeOverrideStateRepository().apply {
            overridePackage.value = "com.instagram.android"
            overrideUntil.value = System.currentTimeMillis() - 1_000L
        }

        val decision = useCase(state = state, overrides = overrides)("com.instagram.android")

        assertEquals(BlockReason.FOCUS_SESSION, decision?.reason)
    }

    /**
     * A window that covers the whole day, so the test does not depend on the
     * clock at the moment it runs.
     */
    private fun allDaySchedule() = FakeScheduleRuleRepository(
        listOf(
            com.sober.core.domain.model.ScheduleRule(
                id = 1,
                ruleName = "Work Mode",
                blockedPackages = listOf("com.instagram.android"),
                daysOfWeek = emptyList(),
                startTime = "00:00",
                endTime = "23:59",
                isActive = true
            )
        )
    )

    @Test
    fun `an open schedule window blocks a listed app`() = runTest {
        val decision = useCase(schedules = allDaySchedule())("com.instagram.android")

        assertEquals(BlockReason.SCHEDULE, decision?.reason)
        assertEquals("Work Mode", decision?.scheduleName)
        assertTrue((decision?.scheduleEndsAt ?: 0L) > System.currentTimeMillis())
    }

    @Test
    fun `a schedule outranks the app's daily limit`() = runTest {
        val limits = FakeAppLimitRepository(
            listOf(
                com.sober.core.domain.model.AppLimit(
                    packageName = "com.instagram.android",
                    appName = "Instagram",
                    dailyLimitMinutes = 1
                )
            )
        )
        val usage = FakeUsageStatsRepository(
            usage = listOf(
                AppUsage(
                    packageName = "com.instagram.android",
                    totalTimeInForeground = 45 * 60_000L,
                    lastTimeUsed = System.currentTimeMillis()
                )
            )
        )

        val decision = useCase(limits = limits, usage = usage, schedules = allDaySchedule())(
            "com.instagram.android"
        )

        assertEquals(BlockReason.SCHEDULE, decision?.reason)
    }

    @Test
    fun `an emergency override lifts a schedule block`() = runTest {
        val overrides = FakeOverrideStateRepository().apply {
            overridePackage.value = "com.instagram.android"
            overrideUntil.value = System.currentTimeMillis() + 60_000L
        }

        assertNull(useCase(overrides = overrides, schedules = allDaySchedule())("com.instagram.android"))
    }

    @Test
    fun `a focus session outranks a schedule`() = runTest {
        val decision = useCase(
            state = activeSession("com.instagram.android"),
            schedules = allDaySchedule()
        )("com.instagram.android")

        assertEquals(BlockReason.FOCUS_SESSION, decision?.reason)
    }

    @Test
    fun `a schedule never blocks an app that is not listed`() = runTest {
        assertNull(useCase(schedules = allDaySchedule())("com.google.android.youtube"))
    }

    /** Accessibility off, one section rule switched on: the app is held shut. */
    private fun sectionFallback(
        accessibility: FakeAccessibilityStateRepository = FakeAccessibilityStateRepository(
            sectionDetectionAvailable = false
        ),
        enabledRuleIds: Set<String> = setOf("com.instagram.android:reels")
    ) = useCase(
        accessibility = accessibility,
        rules = FakeDetectionRuleRepository(
            DetectionRuleCatalog(version = 1, rules = listOf(reelsRule()))
        ),
        sectionRules = FakeSectionRulePreferencesRepository(enabledRuleIds)
    )

    @Test
    fun `an enabled section rule blocks the whole app when detection is unavailable`() = runTest {
        val decision = sectionFallback()("com.instagram.android")

        assertEquals(BlockReason.SECTION_RULE_UNAVAILABLE, decision?.reason)
    }

    @Test
    fun `the fallback does not fire while accessibility is on`() = runTest {
        val evaluate = sectionFallback(
            accessibility = FakeAccessibilityStateRepository(sectionDetectionAvailable = true)
        )

        assertNull(evaluate("com.instagram.android"))
    }

    @Test
    fun `the fallback ignores an app whose section rule is switched off`() = runTest {
        val evaluate = sectionFallback(enabledRuleIds = emptySet())

        assertNull(evaluate("com.instagram.android"))
    }

    @Test
    fun `the fallback ignores apps with no rule at all`() = runTest {
        assertNull(sectionFallback()("com.google.android.youtube"))
    }

    @Test
    fun `a section rule switched off lifts the fallback without a restart`() = runTest {
        val sectionRules = FakeSectionRulePreferencesRepository(
            setOf("com.instagram.android:reels")
        )
        val evaluate = useCase(
            accessibility = FakeAccessibilityStateRepository(sectionDetectionAvailable = false),
            rules = FakeDetectionRuleRepository(
                DetectionRuleCatalog(version = 1, rules = listOf(reelsRule()))
            ),
            sectionRules = sectionRules
        )

        assertEquals(BlockReason.SECTION_RULE_UNAVAILABLE, evaluate("com.instagram.android")?.reason)

        sectionRules.setEnabled("com.instagram.android:reels", false)

        assertNull(evaluate("com.instagram.android"))
    }

    @Test
    fun `a schedule outranks the section fallback`() = runTest {
        val decision = useCase(
            schedules = allDaySchedule(),
            accessibility = FakeAccessibilityStateRepository(sectionDetectionAvailable = false),
            rules = FakeDetectionRuleRepository(
                DetectionRuleCatalog(version = 1, rules = listOf(reelsRule()))
            ),
            sectionRules = FakeSectionRulePreferencesRepository(
                setOf("com.instagram.android:reels")
            )
        )("com.instagram.android")

        assertEquals(BlockReason.SCHEDULE, decision?.reason)
    }

    @Test
    fun `a broken rule catalog does not accidentally block anything`() = runTest {
        val evaluate = useCase(
            accessibility = FakeAccessibilityStateRepository(sectionDetectionAvailable = false),
            sectionRules = FakeSectionRulePreferencesRepository(
                setOf("com.instagram.android:reels")
            )
        )

        // The default fake catalog is empty, which is what a failed load
        // produces; an empty catalog must never turn a rule id into a block.
        assertNull(evaluate("com.instagram.android"))
    }

    private fun blockedAppRule(
        packageName: String = "com.instagram.android",
        enabled: Boolean = true
    ) = FakeCustomRuleRepository(
        listOf(
            CustomRule(
                id = "rule-1",
                type = CustomRuleType.BLOCKED_APP,
                value = packageName,
                enabled = enabled
            )
        )
    )

    @Test
    fun `an app on the user's block list is blocked with its own reason`() = runTest {
        val decision = useCase(customRules = blockedAppRule())("com.instagram.android")

        assertEquals(BlockReason.CUSTOM_APP_RULE, decision?.reason)
    }

    @Test
    fun `a disabled custom rule blocks nothing`() = runTest {
        val rules = blockedAppRule(enabled = false)

        assertNull(useCase(customRules = rules)("com.instagram.android"))
    }

    @Test
    fun `a custom rule for one app leaves the others alone`() = runTest {
        assertNull(
            useCase(customRules = blockedAppRule())("com.google.android.youtube")
        )
    }

    @Test
    fun `a keyword rule never blocks an app`() = runTest {
        val rules = FakeCustomRuleRepository(
            listOf(
                CustomRule(
                    id = "kw",
                    type = CustomRuleType.NOTIFICATION_KEYWORD,
                    value = "com.instagram.android"
                )
            )
        )

        assertNull(useCase(customRules = rules)("com.instagram.android"))
    }

    @Test
    fun `the custom block outranks the app's daily limit`() = runTest {
        val limits = FakeAppLimitRepository(
            listOf(
                com.sober.core.domain.model.AppLimit(
                    packageName = "com.instagram.android",
                    appName = "Instagram",
                    dailyLimitMinutes = 1
                )
            )
        )
        val usage = FakeUsageStatsRepository(
            usage = listOf(
                AppUsage(
                    packageName = "com.instagram.android",
                    totalTimeInForeground = 45 * 60_000L,
                    lastTimeUsed = System.currentTimeMillis()
                )
            )
        )

        val decision = useCase(
            limits = limits,
            usage = usage,
            customRules = blockedAppRule()
        )("com.instagram.android")

        assertEquals(BlockReason.CUSTOM_APP_RULE, decision?.reason)
    }

    @Test
    fun `a meal break lifts a custom block`() = runTest {
        val state = FakeSessionStateRepository().apply {
            mealBreakUntil.value = System.currentTimeMillis() + 60_000L
        }

        assertNull(useCase(state = state, customRules = blockedAppRule())("com.instagram.android"))
    }

    @Test
    fun `an emergency override lifts a custom block`() = runTest {
        val overrides = FakeOverrideStateRepository().apply {
            overridePackage.value = "com.instagram.android"
            overrideUntil.value = System.currentTimeMillis() + 60_000L
        }

        assertNull(
            useCase(overrides = overrides, customRules = blockedAppRule())("com.instagram.android")
        )
    }

    @Test
    fun `a schedule outranks the custom block`() = runTest {
        val decision = useCase(
            schedules = allDaySchedule(),
            customRules = blockedAppRule()
        )("com.instagram.android")

        assertEquals(BlockReason.SCHEDULE, decision?.reason)
    }
}
