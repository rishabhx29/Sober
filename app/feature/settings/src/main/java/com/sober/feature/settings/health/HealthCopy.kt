package com.sober.feature.settings.health

/**
 * The words the health check uses for state it did not read itself.
 *
 * Kept pure so the thresholds can be tested without a device: "just now"
 * versus "2 min ago" is the difference between a service that is working and
 * one that is enabled but dead, and that distinction is the whole point of the
 * screen.
 */
internal object HealthCopy {

    /**
     * @param enabled the switch in Settings → Accessibility
     * @param connected whether the system has actually bound the service
     * @param lastEventAgeMillis time since the last delivered event, or null
     */
    fun accessibilityStatus(
        enabled: Boolean,
        connected: Boolean,
        lastEventAgeMillis: Long?
    ): String = when {
        !enabled -> "Off"
        // The switch can be on while the system has not bound the service:
        // after a force-stop, or after Android kills the app in the background.
        !connected -> "Not connected"
        lastEventAgeMillis == null -> "Connected"
        else -> "Connected · ${relativeAge(lastEventAgeMillis)}"
    }

    fun relativeAge(ageMillis: Long): String {
        val seconds = ageMillis.coerceAtLeast(0L) / 1_000
        return when {
            seconds < 60 -> "just now"
            seconds < 60 * 60 -> "${seconds / 60} min ago"
            seconds < 48 * 60 * 60 -> "${seconds / (60 * 60)} h ago"
            else -> "${seconds / (24 * 60 * 60)} d ago"
        }
    }

    fun requiredSummary(passing: Int, total: Int): String = when {
        total == 0 -> "No required checks"
        passing >= total -> "All required checks passed"
        else -> "$passing of $total required checks pass"
    }

    fun batteryStatus(exempt: Boolean): String =
        if (exempt) "Exempt" else "Optimised"
}
