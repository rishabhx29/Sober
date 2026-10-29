package com.sober.core.common.accessibility

/**
 * Recognises the Settings → Accessibility screen from a window event.
 *
 * Android gives no "this is the accessibility settings screen" flag, so this
 * reads the same evidence a person does: which package the window belongs to,
 * the activity class name, and the visible text. Class names are not
 * localised, which is why they are trusted first; text is only a fallback for
 * OEM builds whose settings activities are generic.
 *
 * Deliberately conservative. Covering the wrong screen would obstruct someone
 * changing an unrelated setting, so anything ambiguous reports false.
 */
object AccessibilitySettingsDetector {

    /**
     * Settings packages only. OEM settings apps keep these IDs even when the
     * rest of their build differs, so the list stays short and explicit.
     */
    private val SETTINGS_PACKAGES = setOf(
        "com.android.settings",
        "com.google.android.settings"
    )

    private const val ACCESSIBILITY = "accessibility"

    fun isAccessibilitySettings(
        packageName: String?,
        activityName: String?,
        texts: List<String>,
        serviceLabel: String = "Sober"
    ): Boolean {
        if (packageName !in SETTINGS_PACKAGES) return false

        // e.g. com.android.settings.accessibility.AccessibilitySettings
        if (activityName?.contains(ACCESSIBILITY, ignoreCase = true) == true) return true

        // Fallback: the screen title and our own service row must both be
        // visible. Requiring both keeps the main Settings list out of scope,
        // where "Accessibility" can appear as a row the user is not opening.
        val mentionsAccessibility = texts.any { it.contains(ACCESSIBILITY, ignoreCase = true) }
        val listsOurService = texts.any { it.contains(serviceLabel, ignoreCase = true) }
        return mentionsAccessibility && listsOurService
    }
}
