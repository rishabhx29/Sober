package com.sober.core.domain.usecase

import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.model.isActiveAt
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.CustomRuleRepository
import com.sober.core.domain.repository.ScheduleRuleRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Why a block is in force right now, for the accessibility guard's copy. */
enum class BlockInForceReason {
    FOCUS_SESSION,
    DAILY_LIMIT,
    SCHEDULE,
    CUSTOM_APP
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
 * blocked", and so has no foreground package to inspect. Meal Break lifts
 * every block, which is why it short-circuits to null.
 */
class IsBlockingInForceUseCase @Inject constructor(
    private val sessionStateRepository: SessionStateRepository,
    private val appLimitRepository: AppLimitRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val usageSessionRepository: UsageSessionRepository,
    private val customRuleRepository: CustomRuleRepository,
    private val scheduleRuleRepository: ScheduleRuleRepository
) {
    suspend operator fun invoke(nowMillis: Long = System.currentTimeMillis()): BlockInForceReason? {
        val mealBreakUntil = sessionStateRepository.mealBreakUntil.first()
        if (mealBreakUntil != null && nowMillis < mealBreakUntil) return null

        // A running session is the promise the guard is protecting, even when
        // its block list is empty: notification silencing can still be on.
        if (sessionStateRepository.activeSessionId.first() != null) {
            return BlockInForceReason.FOCUS_SESSION
        }

        if (hasCustomBlockedApp()) return BlockInForceReason.CUSTOM_APP

        val scheduleBlocking = runCatching {
            scheduleRuleRepository.getActiveRules().first()
        }.getOrDefault(emptyList()).any { rule ->
            rule.blockedPackages.isNotEmpty() && rule.isActiveAt(nowMillis)
        }
        if (scheduleBlocking) return BlockInForceReason.SCHEDULE

        if (isDailyLimitReached()) return BlockInForceReason.DAILY_LIMIT

        return null
    }

    private suspend fun hasCustomBlockedApp(): Boolean =
        runCatching { customRuleRepository.rules().first() }
            .getOrDefault(emptyList())
            .any {
                it.enabled && it.type == CustomRuleType.BLOCKED_APP && it.value.isNotBlank()
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
}
