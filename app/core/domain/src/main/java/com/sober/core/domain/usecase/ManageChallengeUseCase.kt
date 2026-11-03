package com.sober.core.domain.usecase

import com.sober.core.domain.model.ActiveChallenge
import com.sober.core.domain.model.ChallengeId
import com.sober.core.domain.repository.AppCategoryRepository
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.ChallengeRepository
import com.sober.core.domain.repository.SessionStateRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/** Why a challenge could not be started, so the UI can say something useful. */
enum class ChallengeStart {
    Started,
    AlreadyRunning,
    NothingToMeasure
}

/**
 * Starts, finishes and abandons challenges.
 *
 * The prerequisites live here rather than in the UI because they are properties
 * of the rules, not of the button: a clean week with no limits and a feed-free
 * week on a phone with no social apps would both be awarded for doing nothing.
 */
class ManageChallengeUseCase @Inject constructor(
    private val challengeRepository: ChallengeRepository,
    private val appLimitRepository: AppLimitRepository,
    private val sessionStateRepository: SessionStateRepository,
    private val appCategoryRepository: AppCategoryRepository,
    private val getChallengeStateUseCase: GetChallengeStateUseCase
) {

    suspend fun start(
        id: ChallengeId,
        today: LocalDate = LocalDate.now()
    ): ChallengeStart {
        val active = runCatching { challengeRepository.getActive().first() }.getOrNull()
        if (active != null) return ChallengeStart.AlreadyRunning

        val hasAnythingToMeasure = when {
            id.isSocial -> runCatching { appCategoryRepository.getSocialPackages() }
                .getOrDefault(emptySet())
                .isNotEmpty()
            else -> hasLimits()
        }
        if (!hasAnythingToMeasure) return ChallengeStart.NothingToMeasure

        challengeRepository.start(ActiveChallenge(id = id, startedOn = today))
        return ChallengeStart.Started
    }

    /**
     * Records the win, and only when the challenge really is over.
     *
     * Completion is re-derived here instead of trusting the screen that showed
     * the button: the same read that drew the card is the one that decides, so
     * a stale render cannot award a badge early.
     */
    suspend fun finish(today: LocalDate = LocalDate.now()): Boolean {
        val active = runCatching { challengeRepository.getActive().first() }.getOrNull()
            ?: return false
        val state = runCatching { getChallengeStateUseCase(today) }.getOrNull()
        if (state?.active?.isComplete != true) return false

        challengeRepository.complete(active)
        return true
    }

    /** Leaves the challenge without recording a completion. */
    suspend fun abandon() {
        challengeRepository.clearActive()
    }

    private suspend fun hasLimits(): Boolean {
        val cap = runCatching { sessionStateRepository.dailyScreenTimeLimitMinutes.first() }
            .getOrDefault(0)
        val limits = runCatching { appLimitRepository.getAllLimits().first() }
            .getOrDefault(emptyList())
        return cap > 0 || limits.any { it.isActive && it.dailyLimitMinutes > 0 }
    }
}
