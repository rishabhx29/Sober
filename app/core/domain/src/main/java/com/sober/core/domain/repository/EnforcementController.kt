package com.sober.core.domain.repository

/**
 * Starts the enforcement engine.
 *
 * Feature modules must not reference `system:services` directly, so the
 * service is reached through this port. A focus session has to guarantee the
 * engine is running, because the session is only useful if the block overlay
 * actually fires.
 */
interface EnforcementController {
    fun ensureRunning()
}
