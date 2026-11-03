package com.sober.core.domain.usecase

import com.sober.core.domain.model.ActiveChallenge
import com.sober.core.domain.model.ChallengeProgress
import com.sober.core.domain.model.ChallengeState
import com.sober.core.domain.repository.AppCategoryRepository
import com.sober.core.domain.repository.ChallengeRepository
import com.sober.core.domain.repository.UsageSessionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Derives the active challenge's progress, and the count of finished ones.
 *
 * Read-only on purpose: the ViewModel decides when a challenge is finished, so
 * a progress read from Stats never changes what the user committed to. The
 * judged days come from the two sources the rest of the app already trusts —
 * the limit comparison in [CalculateStreakUseCase] and the social-app minutes
 * in the usage table.
 */
class GetChallengeStateUseCase @Inject constructor(
    private val challengeRepository: ChallengeRepository,
    private val usageSessionRepository: UsageSessionRepository,
    private val appCategoryRepository: AppCategoryRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase
) {

    suspend operator fun invoke(today: LocalDate = LocalDate.now()): ChallengeState {
        val active = runCatching { challengeRepository.getActive().first() }.getOrNull()
        val completed = runCatching { challengeRepository.getCompleted().first() }
            .getOrDefault(emptyList())

        return ChallengeState(
            active = active?.let { progress(it, today) },
            completedCount = completed.size
        )
    }

    private suspend fun progress(
        active: ActiveChallenge,
        today: LocalDate
    ): ChallengeProgress {
        val targets = challengeTargetDates(active.id, active.startedOn)
            .filter { it.isBefore(today) }
        val good = when {
            targets.isEmpty() -> emptySet()
            active.id.isSocial -> quietSocialDates(targets)
            else -> cleanDates(targets)
        }

        return evaluateChallenge(active.id, active.startedOn, today) { it in good }
    }

    private suspend fun cleanDates(dates: List<LocalDate>): Set<LocalDate> {
        val isoTargets = dates.map { it.toString() }
        return runCatching { calculateStreakUseCase.cleanDates(isoTargets) }
            .getOrDefault(emptySet())
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSet()
    }

    /**
     * Days with no social-app usage.
     *
     * A day with no usage rows at all counts as quiet: the goal is the absence
     * of social minutes, so a day spent offline is a success rather than an
     * unknown. This is the opposite of the limit challenge, which needs
     * evidence to judge a day — there is nothing to prove if the user deliberately
     * used no phone, but there is plenty to prove about staying under a cap.
     */
    private suspend fun quietSocialDates(dates: List<LocalDate>): Set<LocalDate> {
        val social = runCatching { appCategoryRepository.getSocialPackages() }
            .getOrDefault(emptySet())
        if (social.isEmpty()) return emptySet()

        val isoTargets = dates.map { it.toString() }
        val socialDates = runCatching {
            usageSessionRepository.getUsageForDates(isoTargets)
        }.getOrDefault(emptyList())
            .filter { it.packageName in social && it.usedMinutes > 0 }
            .map { it.date }
            .toSet()

        return dates.filter { it.toString() !in socialDates }.toSet()
    }
}
