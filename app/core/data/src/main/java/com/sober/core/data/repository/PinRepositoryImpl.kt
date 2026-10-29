package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.repository.PinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinRepositoryImpl @Inject constructor(
    private val soberPreferences: SoberPreferences
) : PinRepository {

    override val hasPin: Flow<Boolean> =
        soberPreferences.pinRecord.map { !it.isNullOrBlank() }

    override suspend fun setPin(pin: String) {
        // 120k PBKDF2 rounds is tens of milliseconds of CPU. Callers are UI
        // coroutines on Main, so the hashing itself moves off the main thread
        // rather than freezing the frame that triggered it.
        val record = withContext(Dispatchers.Default) { PinHasher.hash(pin) }
        soberPreferences.setPinRecord(record)
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val record = soberPreferences.pinRecord.first() ?: return false
        return withContext(Dispatchers.Default) { PinHasher.verify(pin, record) }
    }

    override suspend fun clearPin() {
        soberPreferences.clearPinRecord()
    }
}
