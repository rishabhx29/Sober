package com.sober.core.data.repository

import com.sober.core.data.local.dao.ScheduleRuleDao
import com.sober.core.data.local.entity.toEntity
import com.sober.core.domain.model.ScheduleRule
import com.sober.core.domain.repository.ScheduleRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ScheduleRuleRepositoryImpl @Inject constructor(
    private val dao: ScheduleRuleDao
) : ScheduleRuleRepository {
    override suspend fun insertRule(rule: ScheduleRule) {
        dao.insertRule(rule.toEntity())
    }

    override suspend fun updateRule(rule: ScheduleRule) {
        dao.updateRule(rule.toEntity())
    }

    override suspend fun deleteRule(rule: ScheduleRule) {
        dao.deleteRule(rule.toEntity())
    }

    override fun getAllRules(): Flow<List<ScheduleRule>> {
        return dao.getAllRules().map { list -> list.map { it.toDomainModel() } }
    }

    override fun getActiveRules(): Flow<List<ScheduleRule>> {
        return dao.getActiveRules().map { list -> list.map { it.toDomainModel() } }
    }
}
