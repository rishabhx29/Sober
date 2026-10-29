package com.sober.core.domain.repository

import com.sober.core.domain.model.OverrideLog
import kotlinx.coroutines.flow.Flow

interface OverrideLogRepository {
    suspend fun insertLog(log: OverrideLog)
    fun getRecentLogs(limit: Int): Flow<List<OverrideLog>>

    /** Logs in `[fromMillis, toMillis)`, newest first. */
    suspend fun getLogsBetween(fromMillis: Long, toMillis: Long): List<OverrideLog>
}
