package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.model.AppSession
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.AppSessionRepository
import javax.inject.Inject

/**
 * Starts a session of [minutes] for [packageName], replacing any old record.
 *
 * The rest comes from the app's rule at the moment the session starts, so the
 * block time the user set in the Apps sheet is what this session honours even
 * if the setting is edited later.
 *
 * The picker only offers short lengths, but the bounds are enforced here too so
 * a hand-edited backup or a future caller cannot write a session that outlives
 * the day it was chosen on.
 */
class StartAppSessionUseCase @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val appLimitRepository: AppLimitRepository
) {
    suspend operator fun invoke(
        packageName: String,
        minutes: Int,
        now: Long = System.currentTimeMillis()
    ): AppSession {
        val length = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
        val rule = runCatching { appLimitRepository.getLimit(packageName) }.getOrNull()
        val restMinutes = (rule?.sessionRestMinutes ?: AppLimit.DEFAULT_SESSION_REST_MINUTES)
            .coerceIn(
                AppLimit.MIN_SESSION_REST_MINUTES,
                AppLimit.MAX_SESSION_REST_MINUTES
            )
        val session = AppSession(
            packageName = packageName,
            startedAt = now,
            endsAt = now + length * 60_000L,
            restMinutes = restMinutes
        )
        appSessionRepository.start(session)
        return session
    }

    private companion object {
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 180
    }
}
