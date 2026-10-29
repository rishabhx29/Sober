package com.sober.core.data.repository

import com.sober.core.data.local.dao.OverrideLogDao
import com.sober.core.data.local.entity.toEntity
import com.sober.core.domain.model.OverrideLog
import com.sober.core.domain.repository.OverrideLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OverrideLogRepositoryImpl @Inject constructor(
    private val dao: OverrideLogDao
) : OverrideLogRepository {
    override suspend fun insertLog(log: OverrideLog) {
        dao.insertLog(log.toEntity())
    }

    override fun getRecentLogs(limit: Int): Flow<List<OverrideLog>> {
        return dao.getRecentLogs(limit).map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getLogsBetween(fromMillis: Long, toMillis: Long): List<OverrideLog> =
        dao.getLogsBetween(fromMillis, toMillis).map { it.toDomainModel() }
}
