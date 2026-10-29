package com.sober.core.domain.usecase

import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.DetectionType
import com.sober.core.domain.model.WindowSnapshot
import javax.inject.Inject

/**
 * Returns the first enabled rule whose section is on screen, or null.
 *
 * Order is the catalog's order, so a more specific rule listed first wins over
 * a broader one. Rules for another package never match, even if their values
 * appear in the snapshot (shared Compose/AndroidX ids, for example).
 */
class DetectBlockedSectionUseCase @Inject constructor() {

    operator fun invoke(
        snapshot: WindowSnapshot,
        rules: List<DetectionRule>,
        enabledRuleIds: Set<String>
    ): DetectionRule? = rules.firstOrNull { rule ->
        rule.id in enabledRuleIds &&
            rule.packageName == snapshot.packageName &&
            matchesActivity(rule, snapshot) &&
            matchesSignal(rule, snapshot)
    }

    private fun matchesActivity(rule: DetectionRule, snapshot: WindowSnapshot): Boolean {
        val required = rule.activityValue ?: return true
        val activity = snapshot.activityName ?: return false
        return activity.contains(required, ignoreCase = true)
    }

    private fun matchesSignal(rule: DetectionRule, snapshot: WindowSnapshot): Boolean =
        when (rule.detectionType) {
            DetectionType.ACTIVITY ->
                snapshot.activityName?.contains(rule.detectionValue, ignoreCase = true) == true

            DetectionType.RESOURCE_ID -> rule.detectionValue in snapshot.resourceIds

            DetectionType.SELECTED_TAB -> rule.detectionValue in snapshot.selectedResourceIds

            // Labels are localised, so an exact case-insensitive match is the
            // only comparison that does not start blocking random text.
            DetectionType.TEXT -> snapshot.texts.any {
                it.equals(rule.detectionValue, ignoreCase = true)
            }
        }
}
