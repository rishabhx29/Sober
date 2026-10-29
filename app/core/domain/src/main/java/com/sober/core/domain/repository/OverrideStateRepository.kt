package com.sober.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * The emergency-override window, as two separate pieces of state:
 *
 *  - a **request**, created after the PIN gate, which only serves the wait
 *    timer;
 *  - a **grant**, which is the actual lift and is scoped to one package.
 *
 * Keeping the package alongside each timestamp is what stops an override
 * earned for one app from silently unblocking another.
 */
interface OverrideStateRepository {
    val requestPackage: Flow<String?>
    val requestedAt: Flow<Long?>
    val overridePackage: Flow<String?>
    val overrideUntil: Flow<Long?>

    suspend fun setRequest(packageName: String, requestedAt: Long)
    suspend fun clearRequest()
    suspend fun setOverride(packageName: String, until: Long)
    suspend fun clearOverride()
}
