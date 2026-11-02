package com.sober.core.domain.usecase

import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import javax.inject.Inject

/**
 * Total screen time for today, in minutes, across every app the user can open.
 *
 * Delegates to [UsageTotals] so the Home ring, the daily-cap notification and
 * the enforcer can never disagree about the number they are comparing to the
 * cap. Live UsageStatsManager data is authoritative; the `usage_sessions`
 * total is a floor, so time logged earlier today is not lost if usage access
 * is revoked partway through.
 */
class GetDailyScreenTimeUseCase @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val usageSessionRepository: UsageSessionRepository
) {
    suspend operator fun invoke(): Int =
        UsageTotals.deviceMinutesToday(usageStatsRepository, usageSessionRepository)
}
