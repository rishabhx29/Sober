package com.sober.core.domain.repository

import com.sober.core.domain.model.AppSession
import kotlinx.coroutines.flow.Flow

/**
 * The sessions started by the per-app session timer.
 *
 * A session is policy, like a pass or a pause rule, so it lives in the same
 * preferences file: losing it on process death would hand the user an open app
 * they had already used up, while a few extra milliseconds on a poll costs
 * nothing. Entries are one per package: starting a session replaces the old
 * record, so the set can only grow with the number of apps, not with use.
 */
interface AppSessionRepository {
    val sessions: Flow<Map<String, AppSession>>

    /** The stored session for [packageName], including one already resting. */
    suspend fun sessionFor(packageName: String): AppSession?

    /** Replaces any previous session for the same package. */
    suspend fun start(session: AppSession)

    /** Forgets the session, so the next entry asks for a length again. */
    suspend fun clear(packageName: String)

    /** Drops records whose rest has ended; they no longer affect a decision. */
    suspend fun clearConcluded(now: Long = System.currentTimeMillis())
}
