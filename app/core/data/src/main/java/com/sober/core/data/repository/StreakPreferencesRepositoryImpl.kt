package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.repository.StreakPreferencesRepository
import com.sober.core.domain.repository.StreakRule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StreakPreferencesRepositoryImpl @Inject constructor(
    private val preferences: SoberPreferences
) : StreakPreferencesRepository {

    override fun rule(): Flow<StreakRule> = preferences.streakRule

    override suspend fun setRule(rule: StreakRule) {
        preferences.setStreakRule(rule)
    }
}
