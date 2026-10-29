package com.sober.core.domain.usecase

import com.sober.core.domain.repository.OverrideLogRepository
import javax.inject.Inject

/** Emergency overrides the user granted themselves in a period. */
data class OverrideSummary(
    val count: Int = 0,
    val totalMinutesGranted: Int = 0
) {
    val hasOverrides: Boolean get() = count > 0
}

/**
 * Turns the override log into the one number Stats needs.
 *
 * The log rows are the source of truth for "this happened"; nothing is derived
 * from current block state, because the flag is a history of past decisions,
 * not a snapshot of what is blocked right now.
 */
class GetOverrideSummaryUseCase @Inject constructor(
    private val overrideLogRepository: OverrideLogRepository
) {
    suspend operator fun invoke(fromMillis: Long, toMillis: Long): OverrideSummary {
        val logs = runCatching {
            overrideLogRepository.getLogsBetween(fromMillis, toMillis)
        }.getOrDefault(emptyList())

        return OverrideSummary(
            count = logs.size,
            totalMinutesGranted = logs.sumOf { it.durationGrantedMinutes }
        )
    }
}
