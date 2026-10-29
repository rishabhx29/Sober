package com.sober.core.domain.model

/**
 * How a rule recognises a section. All four are signals the Accessibility
 * framework exposes; none of them need to read screen pixels.
 */
enum class DetectionType {
    /** The foreground activity class contains [DetectionRule.detectionValue]. */
    ACTIVITY,

    /** A node with this resource id exists anywhere in the window. */
    RESOURCE_ID,

    /**
     * The node with this resource id (or a descendant) is selected. Tab-based
     * apps mark the open tab selected, which is the only signal that separates
     * "Reels icon is visible" from "Reels is what you are looking at".
     */
    SELECTED_TAB,

    /** Any visible text or content description equals the value. */
    TEXT
}

/**
 * One section a rule can block. The values are data, not code: when an app
 * renames a tab, fixing it is a rules-file change, not a release.
 */
data class DetectionRule(
    val id: String,
    val packageName: String,
    val appName: String,
    val sectionId: String,
    val sectionName: String,
    val detectionType: DetectionType,
    val detectionValue: String,
    val overlayTitle: String,
    val overlaySubtitle: String,
    /**
     * Optional activity the rule only applies in. A nav tab id can exist in
     * more than one activity, and blocking the wrong screen is worse than not
     * blocking.
     */
    val activityValue: String? = null,
    val isCustom: Boolean = false
)

/**
 * Everything matching needs, flattened from `AccessibilityNodeInfo`.
 *
 * The service walks the live tree (Android-only); the matcher stays a pure
 * function over this snapshot so the rules can be tested without a device.
 */
data class WindowSnapshot(
    val packageName: String,
    val activityName: String? = null,
    val resourceIds: Set<String> = emptySet(),
    val selectedResourceIds: Set<String> = emptySet(),
    val texts: Set<String> = emptySet()
)
