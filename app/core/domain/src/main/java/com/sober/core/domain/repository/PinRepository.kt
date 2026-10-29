package com.sober.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * The local override PIN. Only a salted PBKDF2 record is stored — a 4-digit
 * PIN is not a secret worth protecting with the platform keystore, but it
 * should never be readable from the preferences file either.
 */
interface PinRepository {
    val hasPin: Flow<Boolean>

    suspend fun setPin(pin: String)

    /** Verifies in constant time against the stored record. */
    suspend fun verifyPin(pin: String): Boolean

    suspend fun clearPin()

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 6
    }
}
