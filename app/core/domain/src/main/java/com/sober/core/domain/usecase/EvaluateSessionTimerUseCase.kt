package com.sober.core.domain.usecase

import com.sober.core.domain.model.SessionTimerState
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.AppSessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Answers what the session timer wants for one app right now.
 *
 * The rule only applies while its limit row is active, so switching the app's
 * limit off in the Apps list also stops the picker and lifts a running rest
 * without touching the stored session. Turning it back on resumes the same
 * clock rather than silently granting a fresh one.
 */
class EvaluateSessionTimerUseCase @Inject constructor(
    private val appLimitRepository: AppLimitRepository,
    private val appSessionRepository: AppSessionRepository
) {
    suspend operator fun invoke(
        packageName: String,
        now: Long = System.currentTimeMillis()
    ): SessionTimerState {
        val limit = appLimitRepository.getLimit(packageName)
        if (limit == null || !limit.isActive || !limit.sessionTimerEnabled) {
            return SessionTimerState.Off
        }
        val session = appSessionRepository.sessionFor(packageName)
        return SessionTimerState.of(session, now)
    }
}
