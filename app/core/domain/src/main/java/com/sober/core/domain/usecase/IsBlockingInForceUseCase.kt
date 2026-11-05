package com.sober.core.domain.usecase

import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.model.isActiveAt
import com.sober.core.domain.repository.AppCategoryRepository
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.CustomRuleRepository
import com.sober.core.domain.repository.ProtectionSettingsRepository
import com.sober.core.domain.repository.ScheduleRuleRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

/** Why a block is in force right now, for the accessibility guard's copy. */
enum class BlockInForceReason {
    FOCUS_SESSION,
    SCHEDULE,
    CUSTOM_APP,
    ALLOW_ONLY,
    BEDTIME,
    CATEGORY_LIMIT,
    DAILY_LIMIT
}

/**
 * Whether anything is actually being blocked at this instant.
 *
 * The accessibility guard asks this before it covers the Accessibility
 * settings screen: a speed bump that fires when nothing is blocked is just an
 * obstacle, so the guard has to be inert whenever the user has no block on.
 *
 * Deliberately narrower than [EvaluateBlockDecisionUseCase]: it answers "does
 * some rule currently withhold something from the user", not "is this app
 * blocked", and so has no foreground package to inspect. It must still cover
 * every rule that decides blocks — a guard that only knows about sessions and
 * limits would let someone switch Accessibility off during bedtime or
 * allow-only mode and quietly void section blocking. Meal Break and Pause
 * everything lift every block, which is why they short-circuit to null.
 */
class IsBlockingInForceUseCase @Inject constructor(
    private val sessionStateRepository: SessionStateRepository,
    private val appLimitRepository: AppLimitRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val usageSessionRepository: UsageSessionRepository,
    private val customRuleRepository: CustomRuleRepository,
    private val scheduleRuleRepository: ScheduleRuleRepository,
    private val protectionSettingsRepository: ProtectionSettingsRepository,
    private val appCategoryRepository: AppCategoryRepository
) {
    suspend operator fun invoke(nowMillis: Long = System.currentTimeMillis()): BlockInForceReason? {
        val mealBreakUntil = sessionStateRepository.mealBreakUntil.first()
        if (mealBreakUntil != null && nowMillis < mealBreakUntil) return null

        val pausedUntil = protectionSettingsRepository.pausedUntil.first()
        if (pausedUntil != null && nowMillis < pausedUntil) return null

        // A running session is the promise the guard is protecting, even when
        // its block list is empty: notification silencing can still be on. A
        // session whose countdown has passed is not running, whatever the
        // stored state says.
        if (isSessionRunning(nowMillis)) return BlockInForceReason.FOCUS_SESSION

        val scheduleBlocking = runCatching {
            scheduleRuleRepository.getActiveRules().first()
        }.getOrDefault(emptyList()).any { rule ->
            rule.blockedPackages.isNotEmpty() && rule.isActiveAt(nowMillis)
        }
        if (scheduleBlocking) return BlockInForceReason.SCHEDULE

        if (hasCustomBlockedApp()) return BlockInForceReason.CUSTOM_APP

        if (protectionSettingsRepository.allowOnly.first().enabled) {
            return BlockInForceReason.ALLOW_ONLY
        }

        if (protectionSettingsRepository.bedtime.first().isActive(minuteOfDay(nowMillis))) {
            return BlockInForceReason.BEDTIME
        }

        if (isCategoryLimitReached()) return BlockInForceReason.CATEGORY_LIMIT

        if (isDailyLimitReached()) return BlockInForceReason.DAILY_LIMIT

        return null
    }

    private suspend fun isSessionRunning(nowMillis: Long): Boolean {
        if (sessionStateRepository.activeSessionId.first() == null) return false
        val start = sessionStateRepository.activeSessionStartTime.first() ?: return true
        val duration = sessionStateRepository.activeSessionDuration.first() ?: return true
        // Zero is an open-ended session: only a manual stop ends it.
        return duration <= 0 || nowMillis < start + duration
    }

    private suspend fun hasCustomBlockedApp(): Boolean =
        runCatching { customRuleRepository.rules().first() }
            .getOrDefault(emptyList())
            .any {
                it.enabled && it.type == CustomRuleType.BLOCKED_APP && it.value.isNotBlank()
            }

    private suspend fun isCategoryLimitReached(): Boolean {
        val limits = runCatching { protectionSettingsRepository.categoryLimits.first() }
            .getOrDefault(emptyList())
            .filter { it.isActive && it.dailyLimitMinutes > 0 }
        // Same gate as the per-app limits: no category budget means no sweep.
        if (limits.isEmpty()) return false

        val used = UsageTotals.perAppMinutesToday(usageStatsRepository, usageSessionRepository)
        return limits.any { limit ->
            appCategoryRepository.getPackagesInCategory(limit.category)
                .sumOf { used[it] ?: 0 } >= limit.dailyLimitMinutes
        }
    }

    private suspend fun isDailyLimitReached(): Boolean {
        val limits = runCatching { appLimitRepository.getAllLimits().first() }
            .getOrDefault(emptyList())
            .filter { it.isActive && it.dailyLimitMinutes > 0 }
        val overall = runCatching {
            sessionStateRepository.dailyScreenTimeLimitMinutes.first()
        }.getOrDefault(0)

        // The common case is no limits at all: do not sweep usage stats for it.
        if (limits.isEmpty() && overall <= 0) return false

        if (limits.isNotEmpty()) {
            val used = UsageTotals.perAppMinutesToday(usageStatsRepository, usageSessionRepository)
            if (limits.any { (used[it.packageName] ?: 0) >= it.dailyLimitMinutes }) return true
        }

        return overall > 0 &&
            UsageTotals.deviceMinutesToday(usageStatsRepository, usageSessionRepository) >= overall
    }

    private fun minuteOfDay(nowMillis: Long): Int = Calendar.getInstance().apply {
        timeInMillis = nowMillis
    }.let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
}
