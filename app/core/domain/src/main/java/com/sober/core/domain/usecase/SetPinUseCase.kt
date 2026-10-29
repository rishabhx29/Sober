package com.sober.core.domain.usecase

import com.sober.core.domain.repository.PinRepository
import javax.inject.Inject

/**
 * Stores a new override PIN.
 *
 * Validation lives here rather than in the composable so every entry point —
 * the settings dialog today, the emergency override later — accepts exactly the
 * same shape. Digits only: the PIN travels through a NumberPassword field, and
 * a stray letter would make the field show fewer characters than were entered.
 */
class SetPinUseCase @Inject constructor(
    private val pinRepository: PinRepository
) {
    suspend operator fun invoke(pin: String): Boolean {
        if (!isValid(pin)) return false
        pinRepository.setPin(pin)
        return true
    }

    fun isValid(pin: String): Boolean =
        pin.length in PinRepository.MIN_LENGTH..PinRepository.MAX_LENGTH &&
            pin.all { it.isDigit() }
}
