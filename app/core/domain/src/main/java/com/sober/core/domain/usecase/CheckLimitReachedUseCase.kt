package com.sober.core.domain.usecase

import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.UsageSessionRepository
import javax.inject.Inject

class CheckLimitReachedUseCase @Inject constructor(
    private val usageRepository: UsageSessionRepository,
    private val limitRepository: AppLimitRepository
) {
    suspend operator fun invoke(packageName: String): Boolean {
        val usage = usageRepository.getUsageForToday(packageName) ?: return false
        val limit = limitRepository.getLimit(packageName) ?: return false

        if (!limit.isActive) return false
        // A zero-minute row is an opens-only rule: it has no time budget, so
        // "reached" is never true for time.
        if (limit.dailyLimitMinutes <= 0) return false

        return usage.usedMinutes >= limit.dailyLimitMinutes
    }
}
