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

    /**
     * "The system task switcher opened." Not a package, because Recents is
     * chrome rather than an app: the engine must read it as the user leaving
     * the current app, not as another foreground app to hold a block over.
     */
    private val _taskSwitcher = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val taskSwitcher: SharedFlow<Unit> = _taskSwitcher

    private var lastReported: String? = null
    private var taskSwitcherReported = false

    /**
     * Repeated state changes for the same package (a dialog opening, a tab
     * change) are the common case and would each cost a policy evaluation.
     */
    fun report(packageName: String) {
        taskSwitcherReported = false
        if (packageName == lastReported) return
        lastReported = packageName
        _packages.tryEmit(packageName)
    }

    /**
     * A switcher leaves the app, so the next [report] must not be swallowed as
     * a repeat: returning to the same blocked app has to be re-evaluated.
     */
    fun reportTaskSwitcher() {
        lastReported = null
        if (taskSwitcherReported) return
        taskSwitcherReported = true
        _taskSwitcher.tryEmit(Unit)
    }
}
