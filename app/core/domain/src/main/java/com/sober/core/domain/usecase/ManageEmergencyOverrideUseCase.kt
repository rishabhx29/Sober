package com.sober.core.domain.usecase

import com.sober.core.domain.model.OverrideLog
import com.sober.core.domain.repository.OverrideLogRepository
import com.sober.core.domain.repository.OverrideStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** The stored override state, flattened so callers do not read four flows. */
data class OverrideState(
    val requestedPackage: String? = null,
    val requestedAt: Long? = null,
    val overridePackage: String? = null,
    val overrideUntil: Long? = null
)

/**
 * The emergency override (PRD 5.3).
 *
 * The wait timer is deliberately the only thing a request stores. A grant is a
 * *separate* act, so the sequence cannot be collapsed: PIN + math earns a
 * request, the request earns a grant after the wait, and the grant is 15
 * minutes against one package.
 *
 * This class does not check the PIN. Authorisation is the caller's job (the
 * block screen verifies it with `VerifyPinUseCase`), which keeps this use case
 * testable without a hashing dependency and keeps "who may grant" in one place
 * instead of two.
 */
class ManageEmergencyOverrideUseCase @Inject constructor(
    private val overrideStateRepository: OverrideStateRepository,
    private val overrideLogRepository: OverrideLogRepository
) {
    val state: Flow<OverrideState> = combine(
        overrideStateRepository.requestPackage,
        overrideStateRepository.requestedAt,
        overrideStateRepository.overridePackage,
        overrideStateRepository.overrideUntil
    ) { packageName, requestedAt, overridePackage, overrideUntil ->
        OverrideState(
            requestedPackage = packageName,
            requestedAt = requestedAt,
            overridePackage = overridePackage,
            overrideUntil = overrideUntil
        )
    }

    /** Records the moment the PIN gate was passed and starts the wait. */
    suspend fun request(packageName: String, now: Long = System.currentTimeMillis()) {
        overrideStateRepository.setRequest(packageName, now)
    }

    suspend fun cancelRequest() {
        overrideStateRepository.clearRequest()
    }

    /**
     * True when [state] holds a request for [packageName] whose wait has
     * elapsed. A request older than [REQUEST_TTL_MINUTES] is treated as stale:
     * leaving the block screen for an hour should not let someone skip the wait
     * later by reopening the same app.
     */
    fun canGrant(state: OverrideState, packageName: String, now: Long): Boolean {
        if (state.requestedPackage != packageName) return false
        val requestedAt = state.requestedAt ?: return false
        val elapsed = now - requestedAt
        return elapsed >= WAIT_MILLIS && elapsed <= REQUEST_TTL_MILLIS
    }

    /** Grants 15 minutes to [packageName] and records it in history. */
    suspend fun grant(
        packageName: String,
        sessionId: Int?,
        now: Long = System.currentTimeMillis()
    ) {
        overrideStateRepository.setOverride(packageName, now + GRANT_MILLIS)
        overrideStateRepository.clearRequest()
        overrideLogRepository.insertLog(
            OverrideLog(
                packageName = packageName,
                overrideAt = now,
                durationGrantedMinutes = GRANT_MINUTES,
                sessionId = sessionId
            )
        )
    }

    /** Whether enforcement should treat [packageName] as lifted right now. */
    suspend fun isLifted(packageName: String, now: Long = System.currentTimeMillis()): Boolean {
        val current = state.first()
        if (current.overridePackage != packageName) return false
        val until = current.overrideUntil ?: return false
        return now < until
    }

    /**
     * Drops state that can no longer do anything: an expired grant, or a request
     * whose window has passed. Called by the engine so the stored values do not
     * accumulate meanings that are only true in the past.
     */
    suspend fun clearExpired(now: Long = System.currentTimeMillis()) {
        val current = state.first()
        if (current.overrideUntil != null && current.overrideUntil <= now) {
            overrideStateRepository.clearOverride()
        }
        val requestedAt = current.requestedAt
        if (requestedAt != null && now - requestedAt > REQUEST_TTL_MILLIS) {
            overrideStateRepository.clearRequest()
        }
    }

    companion object {
        const val WAIT_MINUTES = 5
        const val GRANT_MINUTES = 15
        const val REQUEST_TTL_MINUTES = 30

        val WAIT_MILLIS = WAIT_MINUTES * 60_000L
        val GRANT_MILLIS = GRANT_MINUTES * 60_000L
        val REQUEST_TTL_MILLIS = REQUEST_TTL_MINUTES * 60_000L
    }
}
