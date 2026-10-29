package com.sober.core.domain.repository

import com.sober.core.domain.model.DetectionRule
import kotlinx.coroutines.flow.Flow

/** The versioned rules document. */
data class DetectionRuleCatalog(
    val version: Int = 0,
    val rules: List<DetectionRule> = emptyList()
) {
    /** App name and sections, in catalog order, for the section toggles. */
    val apps: List<DetectionCatalogApp>
        get() = rules.groupBy { it.packageName }
            .map { (packageName, appRules) ->
                DetectionCatalogApp(
                    packageName = packageName,
                    appName = appRules.first().appName,
                    sections = appRules
                )
            }

    companion object {
        val EMPTY = DetectionRuleCatalog()
    }
}

data class DetectionCatalogApp(
    val packageName: String,
    val appName: String,
    val sections: List<DetectionRule>
)

interface DetectionRuleRepository {
    suspend fun load(): DetectionRuleCatalog
}

/** Which section rules the user switched on, by [DetectionRule.id]. */
interface SectionRulePreferencesRepository {
    fun enabledRuleIds(): Flow<Set<String>>
    suspend fun setEnabled(ruleId: String, enabled: Boolean)
}
