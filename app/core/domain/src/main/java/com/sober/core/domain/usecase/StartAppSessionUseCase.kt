package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppSession
import com.sober.core.domain.repository.AppSessionRepository
import javax.inject.Inject

/**
 * Starts a session of [minutes] for [packageName], replacing any old record.
 *
 * The picker only offers short lengths, but the bound is enforced here too so
 * a hand-edited backup or a future caller cannot write a session that outlives
 * the day it was chosen on.
 */
class StartAppSessionUseCase @Inject constructor(
    private val appSessionRepository: AppSessionRepository
) {
    suspend operator fun invoke(
        packageName: String,
        minutes: Int,
        now: Long = System.currentTimeMillis()
    ): AppSession {
        val length = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
        val session = AppSession(
            packageName = packageName,
            startedAt = now,
            endsAt = now + length * 60_000L
        )
        appSessionRepository.start(session)
        return session
    }

    private companion object {
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 180
    }
}
