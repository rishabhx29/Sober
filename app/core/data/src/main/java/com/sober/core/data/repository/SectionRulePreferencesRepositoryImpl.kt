package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.repository.SectionRulePreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SectionRulePreferencesRepositoryImpl @Inject constructor(
    private val preferences: SoberPreferences
) : SectionRulePreferencesRepository {

    override fun enabledRuleIds(): Flow<Set<String>> = preferences.enabledSectionRules

    override suspend fun setEnabled(ruleId: String, enabled: Boolean) {
        preferences.setSectionRuleEnabled(ruleId, enabled)
    }
}
