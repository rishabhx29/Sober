package com.sober.core.domain.usecase

import com.sober.core.domain.model.ScheduleRule
import com.sober.core.domain.model.windowEndingAt
import com.sober.core.domain.repository.ScheduleRuleRepository
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import javax.inject.Inject

data class ScheduleMatch(
    val rule: ScheduleRule,
    /** Epoch millis this window closes, for the overlay's "until …" copy. */
    val endsAt: Long
)

/**
 * Finds the schedule that blocks [packageName] right now, if any.
 *
 * Only active rules are read (`getActiveRules`), so the sweep never has to
 * filter disabled ones. When two windows overlap, the one that ends soonest
 * wins: it is the rule the user is closest to being released from, and its
 * overlay copy is the one that will become true first.
 */
class EvaluateScheduleUseCase @Inject constructor(
    private val scheduleRuleRepository: ScheduleRuleRepository
) {
    suspend operator fun invoke(
        packageName: String,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): ScheduleMatch? =
        scheduleRuleRepository.getActiveRules().first()
            .asSequence()
            .filter { packageName in it.blockedPackages }
            .mapNotNull { rule ->
                rule.windowEndingAt(nowMillis, zone)?.let { ScheduleMatch(rule, it) }
            }
            .minByOrNull { it.endsAt }
}
