package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.data.rules.SectionRuleCodec
import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.DetectionType
import com.sober.core.domain.repository.CustomSectionRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User-recorded section rules, encoded in one DataStore string set.
 *
 * The whole set is rewritten per edit: this is a handful of rows the user made
 * by hand, so a per-row store would add migration surface for no gain. The id
 * is minted here rather than by the caller so two saves of the same screen can
 * be recognised as one rule.
 */
@Singleton
class CustomSectionRuleRepositoryImpl @Inject constructor(
    private val preferences: SoberPreferences
) : CustomSectionRuleRepository {

    override fun rules(): Flow<List<DetectionRule>> =
        preferences.customSectionRulesRaw.map { raw ->
            raw.mapNotNull(SectionRuleCodec::decode)
                .distinctBy { it.id }
                .sortedWith(
                    compareBy(
                        { it.appName.lowercase() },
                        { it.sectionName.lowercase() }
                    )
                )
        }

    override suspend fun add(rule: DetectionRule): DetectionRule {
        val sectionName = rule.sectionName.trim().take(MAX_NAME_LENGTH)
        require(sectionName.isNotEmpty()) { "A section needs a name" }
        require(rule.detectionValue.isNotBlank()) { "A section needs something to match" }

        // Recording the same screen twice is the same promise; a second row the
        // user cannot tell apart would be a trap when they remove one of them.
        val existing = rules().first().firstOrNull {
            it.packageName == rule.packageName &&
                it.detectionType == rule.detectionType &&
                it.detectionValue == rule.detectionValue
        }
        if (existing != null) {
            preferences.setSectionRuleEnabled(existing.id, true)
            return existing
        }

        val suffix = UUID.randomUUID().toString()
        val stored = rule.copy(
            // No separator in the id: the enabled-set removal matches on it.
            id = ID_PREFIX + suffix,
            sectionId = ID_PREFIX + suffix,
            sectionName = sectionName,
            // The activity is the match itself when the candidate is one.
            activityValue = rule.activityValue
                ?.takeIf { rule.detectionType != DetectionType.ACTIVITY },
            isCustom = true
        )
        preferences.addCustomSectionRule(SectionRuleCodec.encode(stored), stored.id)
        return stored
    }

    override suspend fun remove(id: String) {
        preferences.removeCustomSectionRule(id)
    }

    internal companion object {
        /** Also tells restore which enabled ids belong to a rule, not the catalog. */
        const val ID_PREFIX = "custom:"
        const val MAX_NAME_LENGTH = 40
    }
}
