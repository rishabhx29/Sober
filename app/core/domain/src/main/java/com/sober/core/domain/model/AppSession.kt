package com.sober.core.domain.model

/**
 * One deliberate visit to an app, chosen up front by the session timer.
 *
 * The rest is stored with the session rather than derived from its length:
 * the user sets how long an app stays closed when they turn the timer on, and
 * the session they start carries that promise. Storing it also keeps the
 * record self-contained, so the rest ends at a known instant even if the rule
 * is edited in Settings while the app is resting.
 */
data class AppSession(
    val packageName: String,
    val startedAt: Long,
    val endsAt: Long,
    /** Minutes the app stays closed after [endsAt]. Zero means no rest. */
    val restMinutes: Int = 0
) {
    /** The length the user picked. Never negative, so a clock change is harmless. */
    val durationMillis: Long get() = (endsAt - startedAt).coerceAtLeast(0L)

    /** Epoch millis at which the matching rest ends and a new session may start. */
    val restEndsAt: Long get() = endsAt + restMinutes.coerceAtLeast(0) * 60_000L

    fun isRunning(now: Long): Boolean = now < endsAt

    fun isResting(now: Long): Boolean = now >= endsAt && now < restEndsAt

    /** True once the rest is over and only a fresh pick can reopen the app. */
    fun isConcluded(now: Long): Boolean = now >= restEndsAt
}

/** What the session timer wants for one app at a given instant. */
sealed interface SessionTimerState {
    /** The app has no active session timer rule. */
    data object Off : SessionTimerState

    /** The rule is on and the app is waiting for the user to pick a length. */
    data object NeedsSession : SessionTimerState

    /** A chosen session is running; the app is usable until [session] ends. */
    data class Running(val session: AppSession) : SessionTimerState

    /** The session is over and the app is resting for the chosen block time. */
    data class Resting(val session: AppSession) : SessionTimerState

    companion object {
        /** The state of [session] at [now]; a missing record means "pick one". */
        fun of(session: AppSession?, now: Long): SessionTimerState = when {
            session == null -> NeedsSession
            session.isRunning(now) -> Running(session)
            session.isResting(now) -> Resting(session)
            else -> NeedsSession
        }
    }
}
