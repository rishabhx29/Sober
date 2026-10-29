package com.sober.core.domain.repository

import com.sober.core.domain.model.FocusSession
import kotlinx.coroutines.flow.Flow

interface FocusSessionRepository {
    suspend fun insertSession(session: FocusSession): Long
    suspend fun updateSession(session: FocusSession)
    suspend fun getSessionById(id: Int): FocusSession?
    fun getRecentSessions(limit: Int): Flow<List<FocusSession>>
    suspend fun getActiveSession(): FocusSession?
    suspend fun countCancelledSince(since: Long): Int

    /** Cancelled sessions whose end fell inside [from, to), oldest first. */
    suspend fun getCancelledBetween(from: Long, to: Long): List<FocusSession>

    /** Sessions that ran to completion and ended inside [from, to). */
    suspend fun countCompletedBetween(from: Long, to: Long): Int

    /** Completed sessions whose end fell inside [from, to), oldest first. */
    suspend fun getCompletedBetween(from: Long, to: Long): List<FocusSession>
}
