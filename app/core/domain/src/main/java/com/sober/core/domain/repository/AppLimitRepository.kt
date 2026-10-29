package com.sober.core.domain.repository

import com.sober.core.domain.model.AppLimit
import kotlinx.coroutines.flow.Flow

interface AppLimitRepository {
    suspend fun insertLimit(limit: AppLimit)
    suspend fun updateLimit(limit: AppLimit)
    suspend fun deleteLimit(limit: AppLimit)
    suspend fun getLimit(packageName: String): AppLimit?
    fun getAllLimits(): Flow<List<AppLimit>>
}
