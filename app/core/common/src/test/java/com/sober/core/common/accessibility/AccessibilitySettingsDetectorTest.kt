package com.sober.core.common.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilitySettingsDetectorTest {

    @Test
    fun `the accessibility activity is recognised by class name`() {
        assertTrue(
            AccessibilitySettingsDetector.isAccessibilitySettings(
                packageName = "com.android.settings",
                activityName = "com.android.settings.accessibility.AccessibilitySettings",
                texts = emptyList()
            )
        )
    }

    @Test
    fun `an oem class name that still says accessibility is recognised`() {
        assertTrue(
            AccessibilitySettingsDetector.isAccessibilitySettings(
                packageName = "com.android.settings",
                activityName = "com.samsung.android.settings.accessibility.AccessibilitySettingsActivity",
                texts = emptyList()
            )
        )
    }

    @Test
    fun `a generic activity falls back to the title plus our service row`() {
        assertTrue(
            AccessibilitySettingsDetector.isAccessibilitySettings(
                packageName = "com.android.settings",
                activityName = "com.android.settings.SettingsActivity",
                texts = listOf("Accessibility", "Sober", "TalkBack")
            )
        )
    }

    @Test
    fun `the main settings list is not the accessibility screen`() {
        assertFalse(
            AccessibilitySettingsDetector.isAccessibilitySettings(
                packageName = "com.android.settings",
                activityName = "com.android.settings.homepage.SettingsHomepageActivity",
                texts = listOf("Accessibility")
            )
        )
    }

    @Test
    fun `another app's accessibility activity is ignored`() {
        assertFalse(
            AccessibilitySettingsDetector.isAccessibilitySettings(
                packageName = "com.instagram.android",
                activityName = "com.instagram.android.AccessibilitySettingsActivity",
                texts = listOf("Accessibility", "Sober")
            )
        )
    }

    @Test
    fun `a non-settings package is ignored even with matching text`() {
        assertFalse(
            AccessibilitySettingsDetector.isAccessibilitySettings(
                packageName = "com.example.reader",
                activityName = null,
                texts = listOf("Accessibility", "Sober")
            )
        )
    }

    @Test
    fun `the label match is case insensitive`() {
        assertTrue(
            AccessibilitySettingsDetector.isAccessibilitySettings(
                packageName = "com.android.settings",
                activityName = "com.android.settings.SettingsActivity",
                texts = listOf("ACCESSIBILITY", "sober", "TalkBack")
            )
        )
    }

    @Test
    fun `a missing package is never the accessibility screen`() {
        assertFalse(
            AccessibilitySettingsDetector.isAccessibilitySettings(
                packageName = null,
                activityName = "com.android.settings.accessibility.AccessibilitySettings",
                texts = listOf("Accessibility", "Sober")
            )
        )
    }

    @Test
    fun `an empty window is not the accessibility screen`() {
        assertFalse(
            AccessibilitySettingsDetector.isAccessibilitySettings(
                packageName = "com.android.settings",
                activityName = null,
                texts = emptyList()
            )
        )
    }
}
