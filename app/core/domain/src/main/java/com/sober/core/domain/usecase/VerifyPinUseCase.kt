package com.sober.core.domain.usecase

import com.sober.core.domain.repository.PinRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Checks a PIN against the stored record.
 *
 * Returns false when no PIN has been set, so a caller can never treat "nothing
 * configured" as "everything allowed" for a screen that is meant to be locked.
 */
class VerifyPinUseCase @Inject constructor(
    private val pinRepository: PinRepository
) {
    suspend operator fun invoke(pin: String): Boolean {
        if (pin.isBlank()) return false
        // Checked here, not left to the repository: "no record" must mean
        // "locked", and that invariant should not depend on each data-layer
        // implementation remembering to return false for a null record.
        if (!pinRepository.hasPin.first()) return false
        return pinRepository.verifyPin(pin)
    }
}
