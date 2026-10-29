package com.sober.core.domain.repository

/**
 * Whether the section detector is actually running.
 *
 * Exposed as an interface so the block policy can ask the question without
 * reaching for `Context`; the answer itself is a system setting, not app state.
 */
interface AccessibilityStateRepository {

    /**
     * True when the Accessibility service is enabled in system settings. The
     * Apps screen reads the same signal, so the UI and the policy never
     * disagree about whether section detection is working.
     */
    fun isSectionDetectionAvailable(): Boolean
}
