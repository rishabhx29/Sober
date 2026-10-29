package com.sober.core.domain.repository

import com.sober.core.domain.model.ScheduleRule
import kotlinx.coroutines.flow.Flow

interface ScheduleRuleRepository {
    suspend fun insertRule(rule: ScheduleRule)
    suspend fun updateRule(rule: ScheduleRule)
    suspend fun deleteRule(rule: ScheduleRule)
    fun getAllRules(): Flow<List<ScheduleRule>>
    fun getActiveRules(): Flow<List<ScheduleRule>>
}
