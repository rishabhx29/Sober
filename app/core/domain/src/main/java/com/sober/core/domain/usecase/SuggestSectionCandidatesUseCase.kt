package com.sober.core.domain.usecase

import com.sober.core.domain.model.CapturedCandidate
import com.sober.core.domain.model.DetectionType
import javax.inject.Inject

/**
 * A selected node the service found, flattened so this stays a pure decision
 * that can be tested without an AccessibilityService.
 */
data class SelectedSectionNode(
    val resourceId: String,
    val label: String?
)

/**
 * Turns the selected nodes a capture saw into the rule shapes worth offering.
 *
 * Tabs come first because a selected tab is the strongest signal the platform
 * exposes: the id is stable across app restarts and only the open tab carries
 * the selected state. The activity is still offered because some apps give each
 * section its own screen, but it is marked broad so the review can warn that it
 * may cover the whole app.
 */
class SuggestSectionCandidatesUseCase @Inject constructor() {

    operator fun invoke(
        activityName: String?,
        selected: List<SelectedSectionNode>
    ): List<CapturedCandidate> {
        val seen = mutableSetOf<String>()
        val tabs = selected.mapNotNull { node ->
            val id = node.resourceId.trim()
            if (id.isEmpty() || !seen.add(id)) return@mapNotNull null
            val label = node.label?.trim()?.takeIf { it.isNotEmpty() } ?: humanize(id)
            CapturedCandidate(
                type = DetectionType.SELECTED_TAB,
                value = id,
                label = label
            )
        }

        val activity = activityName?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                CapturedCandidate(
                    type = DetectionType.ACTIVITY,
                    value = it,
                    label = it.substringAfterLast('.').ifBlank { it },
                    isBroad = true
                )
            }

        return tabs + listOfNotNull(activity)
    }

    /**
     * "com.example:id/reels_tab" -> "Reels tab". Only a suggestion: the review
     * screen lets the user replace it, and a raw id is never used as the name.
     */
    private fun humanize(resourceId: String): String {
        val leaf = resourceId.substringAfterLast('/')
            .ifBlank { resourceId }
            .replace('_', ' ')
            .trim()
            .replace(Regex("\\s+"), " ")
        return leaf.replaceFirstChar { it.uppercase() }.take(MAX_LABEL_LENGTH)
    }

    private companion object {
        const val MAX_LABEL_LENGTH = 40
    }
}
