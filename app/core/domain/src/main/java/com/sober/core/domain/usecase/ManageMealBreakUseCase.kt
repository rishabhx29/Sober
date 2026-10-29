package com.sober.core.domain.usecase

import com.sober.core.domain.repository.SessionStateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Meal Break: a PRD 2.5 mode that lifts every block for twenty minutes.
 *
 * It is a time window rather than a change to limits, so nothing the user
 * configured is lost — enforcement simply treats the window as "no decision"
 * and resumes on its own when the window closes. An active focus session keeps
 * running and keeps its history; only the blocking pauses.
 */
class ManageMealBreakUseCase @Inject constructor(
    private val sessionStateRepository: SessionStateRepository
) {
    /** Epoch millis the pause ends, or null when no break is running. */
    val until: Flow<Long?> = sessionStateRepository.mealBreakUntil

    suspend fun start(now: Long = System.currentTimeMillis()): Long {
        val end = now + DURATION_MINUTES * 60_000L
        sessionStateRepository.setMealBreakUntil(end)
        return end
    }

    suspend fun endNow() {
        sessionStateRepository.setMealBreakUntil(null)
    }

    companion object {
        const val DURATION_MINUTES = 20
    }
}
