package com.sober.system.services.foreground

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskSwitcherTest {

    @Test
    fun `system ui recents window is the task switcher`() {
        assertTrue(
            isSystemTaskSwitcher(
                "com.android.systemui",
                "com.android.systemui.recents.RecentsActivity"
            )
        )
    }

    @Test
    fun `overview window counts as well`() {
        assertTrue(
            isSystemTaskSwitcher(
                "com.android.systemui",
                "com.android.systemui.overview.OverviewActivity"
            )
        )
    }

    @Test
    fun `match ignores class name case`() {
        assertTrue(
            isSystemTaskSwitcher("com.android.systemui", "com.android.systemui.RECENTS.Recents")
        )
    }

    @Test
    fun `the notification shade is not the task switcher`() {
        assertFalse(
            isSystemTaskSwitcher(
                "com.android.systemui",
                "com.android.systemui.statusbar.phone.NotificationShadeWindowView"
            )
        )
    }

    @Test
    fun `an app activity that mentions recents is untouched`() {
        assertFalse(
            isSystemTaskSwitcher("com.example.files", "com.example.files.RecentFilesActivity")
        )
    }

    @Test
    fun `a missing class name cannot be classified`() {
        assertFalse(isSystemTaskSwitcher("com.android.systemui", null))
    }
}