package com.sober.core.data.repository

import com.sober.core.data.local.dao.AppLimitDao
import com.sober.core.data.local.entity.toEntity
import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.repository.AppLimitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppLimitRepositoryImpl @Inject constructor(
    private val dao: AppLimitDao
) : AppLimitRepository {
    override suspend fun insertLimit(limit: AppLimit) {
        dao.insertLimit(limit.toEntity())
    }

    override suspend fun updateLimit(limit: AppLimit) {
        dao.updateLimit(limit.toEntity())
    }

    override suspend fun deleteLimit(limit: AppLimit) {
        dao.deleteLimit(limit.toEntity())
    }

    override suspend fun getLimit(packageName: String): AppLimit? {
        return dao.getLimit(packageName)?.toDomainModel()
    }

    override fun getAllLimits(): Flow<List<AppLimit>> {
        return dao.getAllLimits().map { list -> list.map { it.toDomainModel() } }
    }
}
