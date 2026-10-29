package com.sober.system.services.foreground

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "This app is on screen now", reported by the accessibility service.
 *
 * The engine's poll is the fallback that always works, but it can only be as
 * fast as its interval. The accessibility service already sees every window
 * transition, so it reports them here and the engine re-evaluates immediately —
 * a blocked app is covered the moment it opens instead of up to a poll later.
 *
 * Only the latest screen matters; a slow consumer must never queue a backlog of
 * stale screens, hence the one-slot buffer that drops the oldest value.
 */
@Singleton
class ForegroundSignal @Inject constructor() {

    private val _packages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val packages: SharedFlow<String> = _packages

    private var lastReported: String? = null

    /**
     * Repeated state changes for the same package (a dialog opening, a tab
     * change) are the common case and would each cost a policy evaluation.
     */
    fun report(packageName: String) {
        if (packageName == lastReported) return
        lastReported = packageName
        _packages.tryEmit(packageName)
    }
}
