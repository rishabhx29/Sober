package com.sober.core.data.repository

import com.sober.core.data.local.dao.FocusSessionDao
import com.sober.core.data.local.entity.toEntity
import com.sober.core.domain.model.FocusSession
import com.sober.core.domain.repository.FocusSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FocusSessionRepositoryImpl @Inject constructor(
    private val dao: FocusSessionDao
) : FocusSessionRepository {
    override suspend fun insertSession(session: FocusSession): Long {
        return dao.insertSession(session.toEntity())
    }

    override suspend fun updateSession(session: FocusSession) {
        dao.updateSession(session.toEntity())
    }

    override suspend fun getSessionById(id: Int): FocusSession? {
        return dao.getSessionById(id)?.toDomainModel()
    }

    override fun getRecentSessions(limit: Int): Flow<List<FocusSession>> {
        return dao.getRecentSessions(limit).map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getActiveSession(): FocusSession? {
        return dao.getActiveSession()?.toDomainModel()
    }

    override suspend fun countCancelledSince(since: Long): Int {
        return dao.countCancelledSince(since)
    }

    override suspend fun getCancelledBetween(from: Long, to: Long): List<FocusSession> {
        return dao.getCancelledBetween(from, to).map { it.toDomainModel() }
    }

    override suspend fun countCompletedBetween(from: Long, to: Long): Int =
        dao.countCompletedBetween(from, to)

    override suspend fun getCompletedBetween(from: Long, to: Long): List<FocusSession> =
        dao.getCompletedBetween(from, to).map { it.toDomainModel() }
}
