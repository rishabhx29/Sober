package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.model.AppPauseRule
import com.sober.core.domain.repository.PauseRulesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PauseRulesRepositoryImpl @Inject constructor(
    private val soberPreferences: SoberPreferences
) : PauseRulesRepository {

    override val rules: Flow<List<AppPauseRule>> = soberPreferences.appPauseRules
    override val passes: Flow<Map<String, Long>> = soberPreferences.pausePasses

    override suspend fun ruleFor(packageName: String): AppPauseRule? =
        rules.first().firstOrNull { it.packageName == packageName && it.isActive }

    override suspend fun setRule(packageName: String, rule: AppPauseRule?) =
        soberPreferences.setAppPauseRule(packageName, rule)

    override suspend fun grantPass(packageName: String, until: Long) =
        soberPreferences.grantPausePass(packageName, until, System.currentTimeMillis())

    override suspend fun passUntil(packageName: String): Long? =
        passes.first()[packageName]

    override suspend fun clearExpiredPasses(now: Long) =
        soberPreferences.clearExpiredPasses(now)
}
