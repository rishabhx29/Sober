package com.sober.core.domain.usecase

import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.repository.AccessibilityStateRepository
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.CustomRuleRepository
import com.sober.core.domain.repository.DetectionRuleCatalog
import com.sober.core.domain.repository.DetectionRuleRepository
import com.sober.core.domain.repository.SectionRulePreferencesRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import kotlinx.coroutines.flow.first
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
    val scheduleEndsAt: Long? = null
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
    SECTION_RULE_UNAVAILABLE
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
 * then the user's own block list, then a section rule with no running
 * detector, then daily limits. The session is the stricter, more explicit
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
    private val customRuleRepository: CustomRuleRepository
) {
    suspend operator fun invoke(foregroundPackage: String?): BlockDecision? {
        if (foregroundPackage.isNullOrBlank()) return null

        // 0. A meal break lifts every block, including an active focus
        //    session's, for its twenty minutes. The window expires on its own;
        //    a stale value simply stops matching.
        val mealBreakUntil = sessionStateRepository.mealBreakUntil.first()
        if (mealBreakUntil != null && System.currentTimeMillis() < mealBreakUntil) {
            return null
        }

        // 0b. An emergency override lifts this one app for its fifteen minutes.
        //     Checked before the session rules so an override earned during a
        //     focus session actually releases the block it was earned for.
        if (manageEmergencyOverrideUseCase.isLifted(foregroundPackage)) {
            return null
        }

        // 1. Active focus session: enforce its block list.
        val sessionId = sessionStateRepository.activeSessionId.first()
        if (sessionId != null) {
            val blocked = sessionStateRepository.activeSessionBlockedApps.first()
                ?.split(',')
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            if (foregroundPackage in blocked) {
                // The overlay needs the session window so it can count down to
                // session end instead of to midnight. Read once here rather
                // than in the renderer: policy values belong to this use case.
                val sessionStart = sessionStateRepository.activeSessionStartTime.first()
                val sessionDuration = sessionStateRepository.activeSessionDuration.first()
                val sessionLabel = sessionStateRepository.activeSessionLabel.first()

                return BlockDecision(
                    packageName = foregroundPackage,
                    reason = BlockReason.FOCUS_SESSION,
                    usedMinutes = 0,
                    limitMinutes = 0,
                    sessionLabel = sessionLabel,
                    sessionStartedAt = sessionStart,
                    // A duration of 0 marks an open-ended session, so there is
                    // no end to count down to; the overlay shows that instead
                    // of a bogus 00:00.
                    sessionEndsAt = if (sessionStart != null && sessionDuration != null &&
                        sessionDuration > 0
                    ) {
                        sessionStart + sessionDuration
                    } else {
                        null
                    }
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

        // 2. The app's own daily limit.
        val limit = appLimitRepository.getLimit(foregroundPackage)
        if (limit != null && limit.isActive && usedMinutes >= limit.dailyLimitMinutes) {
            return BlockDecision(
                packageName = foregroundPackage,
                reason = BlockReason.APP_DAILY_LIMIT,
                usedMinutes = usedMinutes,
                limitMinutes = limit.dailyLimitMinutes
            )
        }

        // 3. The overall daily cap, applied to every app except the launcher
        //    and Sober itself. This cap is intentionally independent of
        //    per-app limits: a user can be under every app limit and still
        //    over their total budget for the day.
        val overallLimit = sessionStateRepository.dailyScreenTimeLimitMinutes.first()
        if (overallLimit > 0) {
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

    private suspend fun liveUsageMinutes(packageName: String): Int =
        try {
            (usageStatsRepository.getUsageForApp(packageName)?.totalTimeInForeground ?: 0L)
                .div(60_000L)
                .toInt()
        } catch (e: Exception) {
            0
        }
}