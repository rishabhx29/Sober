package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppCategory
import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.model.SessionTimerState
import com.sober.core.domain.repository.AccessibilityStateRepository
import com.sober.core.domain.repository.AppCategoryRepository
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.AppOpenRepository
import com.sober.core.domain.repository.AppSessionRepository
import com.sober.core.domain.repository.BlockableAppsRepository
import com.sober.core.domain.repository.CustomRuleRepository
import com.sober.core.domain.repository.DetectionRuleCatalog
import com.sober.core.domain.repository.DetectionRuleRepository
import com.sober.core.domain.repository.ProtectionSettingsRepository
import com.sober.core.domain.repository.SectionRulePreferencesRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

/**
 * Why a foreground app is being blocked. The notification and overlay both
 * render this, so the reason a user sees always matches the rule that fired.
 */
data class BlockDecision(
    val packageName: String,
    val reason: BlockReason,
    /** Minutes used today against the limit that fired. */
    val usedMinutes: Int,
    /** The limit that fired, in minutes. */
    val limitMinutes: Int,
    /** Session label shown on the overlay when [reason] is FOCUS_SESSION. */
    val sessionLabel: String? = null,
    /** Epoch millis the focus session began, for the overlay's depleting ring. */
    val sessionStartedAt: Long? = null,
    /** Epoch millis the focus session ends, for the overlay's countdown. */
    val sessionEndsAt: Long? = null,
    /** Rule name shown on the overlay when [reason] is SCHEDULE. */
    val scheduleName: String? = null,
    /** Epoch millis the current schedule window closes. */
    val scheduleEndsAt: Long? = null,
    /** Category whose shared budget fired, when [reason] is CATEGORY_LIMIT. */
    val category: AppCategory? = null,
    /** Opens today, when [reason] is APP_OPEN_LIMIT. */
    val openCount: Int = 0,
    /** The open budget that fired, when [reason] is APP_OPEN_LIMIT. */
    val openLimit: Int = 0,
    /** Epoch millis the bedtime window closes, when [reason] is BEDTIME. */
    val bedtimeEndsAt: Long? = null
)

enum class BlockReason {
    /** The app's own daily limit was reached. */
    APP_DAILY_LIMIT,

    /** The user's overall daily screen-time cap was reached. */
    OVERALL_DAILY_LIMIT,

    /** A focus session is active and this app is in its block list. */
    FOCUS_SESSION,

    /** A recurring schedule window is open and this app is in its block list. */
    SCHEDULE,

    /** The user added this app to their own block list. No end, no limit. */
    CUSTOM_APP_RULE,

    /**
     * The app has an enabled section rule but the detector that would tell
     * which section is open is not running, so the whole app is held shut
     * rather than letting the rule silently do nothing.
     */
    SECTION_RULE_UNAVAILABLE,

    /** The app was opened more times today than its own budget allows. */
    APP_OPEN_LIMIT,

    /** A shared daily budget for every app in the app's category was used up. */
    CATEGORY_LIMIT,

    /** Allow-only mode is on and this app is not on the allowed list. */
    ALLOW_ONLY,

    /** The bedtime window is open and this app is not on its allowed list. */
    BEDTIME,

    /**
     * The app's chosen session ran out, and the matching rest is still running.
     * The rest lasts as long as the session the user picked.
     */
    SESSION_TIMER
}

/**
 * Decides whether the app currently in the foreground should be blocked.
 *
 * This is the single place blocking policy lives. It previously existed in
 * three unrelated fragments (a focus-session check inside the service, an
 * unused limit use case, and nothing for the overall cap), which meant the
 * service could only ever enforce focus sessions.
 *
 * Precedence is deliberate: meal break lifts everything, an emergency override
 * lifts one app, then an active focus session, then an open schedule window,
 * then the user's own block list, then a session timer's rest, then a section
 * rule with no running detector, then daily limits. The session is the stricter, more explicit
 * promise the user made and its overlay copy is what they expect to see; a
 * schedule follows because it too has a known end the overlay can name.
 */
class EvaluateBlockDecisionUseCase @Inject constructor(
    private val appLimitRepository: AppLimitRepository,
    private val usageSessionRepository: UsageSessionRepository,
    private val sessionStateRepository: SessionStateRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val manageEmergencyOverrideUseCase: ManageEmergencyOverrideUseCase,
    private val evaluateScheduleUseCase: EvaluateScheduleUseCase,
    private val accessibilityStateRepository: AccessibilityStateRepository,
    private val detectionRuleRepository: DetectionRuleRepository,
    private val sectionRulePreferencesRepository: SectionRulePreferencesRepository,
    private val customRuleRepository: CustomRuleRepository,
    private val blockableAppsRepository: BlockableAppsRepository,
    private val protectionSettingsRepository: ProtectionSettingsRepository,
    private val appOpenRepository: AppOpenRepository,
    private val appCategoryRepository: AppCategoryRepository,
    private val appSessionRepository: AppSessionRepository
) {
    suspend operator fun invoke(foregroundPackage: String?): BlockDecision? {
        if (foregroundPackage.isNullOrBlank()) return null

        // 0a. The device's own chrome is never a block target. The keyboard,
        //     SystemUI and Settings can come to the foreground while a block is
        //     in force; covering them would make the phone unusable and could
        //     trap the user inside Sober.
        if (!blockableAppsRepository.isBlockable(foregroundPackage)) return null

        // One clock read for the whole decision: every window below is compared
        // against the same instant, so a decision cannot straddle a boundary.
        val now = System.currentTimeMillis()

        // 0. A meal break lifts every block, including an active focus
        //    session's, for its twenty minutes. The window expires on its own;
        //    a stale value simply stops matching.
        val mealBreakUntil = sessionStateRepository.mealBreakUntil.first()
        if (mealBreakUntil != null && now < mealBreakUntil) {
            return null
        }

        // 0b. An emergency override lifts this one app for its fifteen minutes.
        //     Checked before the session rules so an override earned during a
        //     focus session actually releases the block it was earned for.
        if (manageEmergencyOverrideUseCase.isLifted(foregroundPackage)) {
            return null
        }

        // 0c. Pause everything (F9). A deliberate, time-boxed suspension of all
        //     enforcement; unlike a meal break it is not tied to a session and
        //     the user can end it early from Home.
        val pausedUntil = protectionSettingsRepository.pausedUntil.first()
        if (pausedUntil != null && now < pausedUntil) {
            return null
        }

        // 1. Active focus session: enforce its block list.
        val sessionId = sessionStateRepository.activeSessionId.first()
        if (sessionId != null) {
            // The overlay needs the session window so it can count down to
            // session end instead of to midnight. Read once here rather than
            // in the renderer: policy values belong to this use case.
            val sessionStart = sessionStateRepository.activeSessionStartTime.first()
            val sessionDuration = sessionStateRepository.activeSessionDuration.first()
            // A duration of 0 marks an open-ended session, so there is no end
            // to count down to; the overlay shows that instead of a bogus 00:00.
            val sessionEndsAt = if (sessionStart != null && sessionDuration != null &&
                sessionDuration > 0
            ) {
                sessionStart + sessionDuration
            } else {
                null
            }
            // A session whose stored state outlived its own countdown must not
            // keep an app shut. The service clears the state on its next tick,
            // but until then the user would see a block with 00:00 left that
            // nothing on screen can end.
            val sessionRunning = sessionEndsAt == null || now < sessionEndsAt

            val blocked = sessionStateRepository.activeSessionBlockedApps.first()
                ?.split(',')
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            if (sessionRunning && foregroundPackage in blocked) {
                val sessionLabel = sessionStateRepository.activeSessionLabel.first()

                return BlockDecision(
                    packageName = foregroundPackage,
                    reason = BlockReason.FOCUS_SESSION,
                    usedMinutes = 0,
                    limitMinutes = 0,
                    sessionLabel = sessionLabel,
                    sessionStartedAt = sessionStart,
                    sessionEndsAt = sessionEndsAt
                )
            }
        }

        // 1b. A schedule window the user set in advance. Checked before the
        //     daily limits because it has a known end, and the overlay tells
        //     the user when the app comes back — copy a limit block cannot
        //     offer. The active-list read is one query, so this costs nothing
        //     on the common path where no rule matches.
        val schedule = evaluateScheduleUseCase(foregroundPackage)
        if (schedule != null) {
            return BlockDecision(
                packageName = foregroundPackage,
                reason = BlockReason.SCHEDULE,
                usedMinutes = 0,
                limitMinutes = 0,
                scheduleName = schedule.rule.ruleName,
                scheduleEndsAt = schedule.endsAt
            )
        }

        // 1b2. An app the user put on their own block list. Checked before the
        //      daily limits: the user's standing decision is indefinite, so it
        //      is the truthful reason to show, and it does not depend on a
        //      limit existing at all.
        if (isCustomBlocked(foregroundPackage)) {
            return BlockDecision(
                packageName = foregroundPackage,
                reason = BlockReason.CUSTOM_APP_RULE,
                usedMinutes = 0,
                limitMinutes = 0
            )
        }

        // 1b3. Allow-only mode: only the named apps may run at all. It sits
        //      above limits and below sessions/schedules so the stricter,
        //      more specific promise still owns the overlay copy.
        val allowOnly = protectionSettingsRepository.allowOnly.first()
        if (allowOnly.enabled && foregroundPackage !in allowOnly.allowedPackages) {
            return BlockDecision(
                packageName = foregroundPackage,
                reason = BlockReason.ALLOW_ONLY,
                usedMinutes = 0,
                limitMinutes = 0
            )
        }

        // 1b4. Bedtime: an overnight window with its own short allowlist. It is
        //      checked before usage limits because "it is bedtime" is the true
        //      reason at 23:00 regardless of how much budget is left.
        val bedtime = protectionSettingsRepository.bedtime.first()
        if (bedtime.isActive(minuteOfDay(now)) &&
            foregroundPackage !in bedtime.allowedPackages
        ) {
            return BlockDecision(
                packageName = foregroundPackage,
                reason = BlockReason.BEDTIME,
                usedMinutes = 0,
                limitMinutes = 0,
                scheduleName = "Bedtime",
                bedtimeEndsAt = bedtime.endFor(now)
            )
        }

        // 1b5. The per-app session timer's rest. The user named a length, used
        //      it, and the app now rests for the same length. It sits with the
        //      other rules that have a known end, above plain daily limits,
        //      because the overlay copy can say when it lifts and a limit could
        //      otherwise claim credit for the same block. A running or absent
        //      session does not block: the picker handles the absent case, and
        //      the service decides when to show it.
        //
        //      The limit row is read once here and reused by the day budgets
        //      below, so this extra check costs one DataStore read only.
        val limit = appLimitRepository.getLimit(foregroundPackage)
        if (limit != null && limit.isActive && limit.sessionTimerEnabled) {
            val session = appSessionRepository.sessionFor(foregroundPackage)
            val timerState = SessionTimerState.of(session, now)
            if (timerState is SessionTimerState.Resting) {
                val minutes = (timerState.session.durationMillis / 60_000L)
                    .toInt()
                    .coerceAtLeast(1)
                return BlockDecision(
                    packageName = foregroundPackage,
                    reason = BlockReason.SESSION_TIMER,
                    usedMinutes = minutes,
                    limitMinutes = minutes,
                    // The ring depletes across the rest, so its window starts
                    // at the instant the session ended, not when it began.
                    sessionStartedAt = timerState.session.endsAt,
                    sessionEndsAt = timerState.session.restEndsAt
                )
            }
        }

        // 1c. A section rule whose detector is not running. The user asked for
        //     "Reels is blocked"; without Accessibility we cannot tell Reels
        //     from the feed, so letting the app through would silently void
        //     that promise. Holding the whole package shut is the conservative
        //     reading, and the overlay copy says which rule caused it.
        if (!accessibilityStateRepository.isSectionDetectionAvailable() &&
            hasEnabledSectionRule(foregroundPackage)
        ) {
            return BlockDecision(
                packageName = foregroundPackage,
                reason = BlockReason.SECTION_RULE_UNAVAILABLE,
                usedMinutes = 0,
                limitMinutes = 0
            )
        }

        // Read live totals from UsageStatsManager rather than only from
        // `usage_sessions`: against a single app the two should agree, but the
        // live figure is authoritative and cannot lag a polling cycle.
        val liveMinutes = liveUsageMinutes(foregroundPackage)
        val storedMinutes = usageSessionRepository.getUsageForToday(foregroundPackage)
            ?.usedMinutes
            ?: 0
        val usedMinutes = maxOf(liveMinutes, storedMinutes)

        // 2. The app's own daily limit. Zero minutes means there is no time
        //    budget (an opens-only rule), not a limit of zero — without the
        //    guard every app with an open budget would block on its first use.
        if (limit != null && limit.isActive && limit.dailyLimitMinutes > 0 &&
            usedMinutes >= limit.dailyLimitMinutes
        ) {
            return BlockDecision(
                packageName = foregroundPackage,
                reason = BlockReason.APP_DAILY_LIMIT,
                usedMinutes = usedMinutes,
                limitMinutes = limit.dailyLimitMinutes
            )
        }

        // 2b. Opens per day. A time budget answers "how long" and an open budget
        //     answers "how often"; the count is read only when a budget exists so
        //     the common path stays one limit lookup.
        if (limit != null && limit.isActive && limit.dailyOpenLimit > 0) {
            val opens = appOpenRepository.countToday(foregroundPackage)
            if (opens >= limit.dailyOpenLimit) {
                return BlockDecision(
                    packageName = foregroundPackage,
                    reason = BlockReason.APP_OPEN_LIMIT,
                    usedMinutes = 0,
                    limitMinutes = 0,
                    openCount = opens,
                    openLimit = limit.dailyOpenLimit
                )
            }
        }

        // 2c. A shared budget for the app's category. Summed from every package
        //     the platform puts in that bucket, so the limit covers apps the
        //     user never added one by one.
        val categoryLimits = protectionSettingsRepository.categoryLimits.first()
        if (categoryLimits.isNotEmpty()) {
            val category = appCategoryRepository.getCategory(foregroundPackage)
            val categoryLimit = categoryLimits.firstOrNull {
                it.isActive && it.category == category && it.dailyLimitMinutes > 0
            }
            if (categoryLimit != null) {
                val packages = appCategoryRepository.getPackagesInCategory(category) +
                    foregroundPackage
                val perApp = UsageTotals.perAppMinutesToday(
                    usageStatsRepository,
                    usageSessionRepository
                )
                val categoryMinutes = packages.sumOf { perApp[it] ?: 0 }
                if (categoryMinutes >= categoryLimit.dailyLimitMinutes) {
                    return BlockDecision(
                        packageName = foregroundPackage,
                        reason = BlockReason.CATEGORY_LIMIT,
                        usedMinutes = categoryMinutes,
                        limitMinutes = categoryLimit.dailyLimitMinutes,
                        category = category
                    )
                }
            }
        }

        // 3. The overall daily cap, applied to every app except the launcher,
        //    Sober itself and any app the user exempted in Settings. This cap
        //    is intentionally independent of per-app limits: a user can be
        //    under every app limit and still over their total budget for the
        //    day. Exempt apps still count toward the total; exemption only
        //    means the cap does not cover them.
        val overallLimit = sessionStateRepository.dailyScreenTimeLimitMinutes.first()
        val exemptPackages = sessionStateRepository.dailyCapExemptPackages.first()
        if (overallLimit > 0 && foregroundPackage !in exemptPackages) {
            // Summed across all apps, because this cap spans the whole device.
            val totalMinutes = UsageTotals.deviceMinutesToday(
                usageStatsRepository,
                usageSessionRepository
            )
            if (totalMinutes >= overallLimit) {
                return BlockDecision(
                    packageName = foregroundPackage,
                    reason = BlockReason.OVERALL_DAILY_LIMIT,
                    usedMinutes = totalMinutes,
                    limitMinutes = overallLimit
                )
            }
        }

        return null
    }

    private suspend fun isCustomBlocked(packageName: String): Boolean =
        runCatching { customRuleRepository.rules().first() }
            .getOrDefault(emptyList())
            .any {
                it.enabled &&
                    it.type == CustomRuleType.BLOCKED_APP &&
                    it.value == packageName
            }

    /**
     * Only reached when Accessibility is off, so the preference read and the
     * catalog load (cached by the repository after the first call) cost
     * nothing on the path where section detection is working.
     */
    private suspend fun hasEnabledSectionRule(packageName: String): Boolean {
        val enabled = runCatching { sectionRulePreferencesRepository.enabledRuleIds().first() }
            .getOrDefault(emptySet())
        if (enabled.isEmpty()) return false

        val catalog = runCatching { detectionRuleRepository.load() }
            .getOrDefault(DetectionRuleCatalog.EMPTY)
        return catalog.rules.any { it.packageName == packageName && it.id in enabled }
    }

    private fun minuteOfDay(now: Long): Int = Calendar.getInstance().apply {
        timeInMillis = now
    }.let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

    private suspend fun liveUsageMinutes(packageName: String): Int =
        try {
            (usageStatsRepository.getUsageForApp(packageName)?.totalTimeInForeground ?: 0L)
                .div(60_000L)
                .toInt()
        } catch (e: Exception) {
            0
        }
}