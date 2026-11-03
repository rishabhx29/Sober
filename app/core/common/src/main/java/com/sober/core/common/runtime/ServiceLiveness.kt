package com.sober.core.common.runtime

import android.os.SystemClock

/**
 * In-process liveness for the two components that can die without telling
 * anyone.
 *
 * Both services run in the app's own process, so a plain in-memory flag is the
 * only evidence that cannot lie. A timestamp persisted to disk would survive
 * the process death it is supposed to reveal, and a check that reads
 * `Settings.Secure` only proves the switch is on, not that the system has
 * actually bound the service.
 *
 * Times use [SystemClock.elapsedRealtime], which keeps counting in deep sleep
 * and cannot be moved by the user changing the wall clock.
 */
object ServiceLiveness {

    @Volatile
    var isEngineRunning: Boolean = false
        private set

    @Volatile
    var accessibilityConnectedAtMillis: Long = 0L
        private set

    @Volatile
    var accessibilityLastEventAtMillis: Long = 0L
        private set

    fun engineStarted() {
        isEngineRunning = true
    }

    fun engineStopped() {
        isEngineRunning = false
    }

    fun accessibilityConnected() {
        accessibilityConnectedAtMillis = SystemClock.elapsedRealtime()
    }

    fun accessibilityDisconnected() {
        accessibilityConnectedAtMillis = 0L
    }

    fun accessibilityEventSeen() {
        accessibilityLastEventAtMillis = SystemClock.elapsedRealtime()
    }
}
