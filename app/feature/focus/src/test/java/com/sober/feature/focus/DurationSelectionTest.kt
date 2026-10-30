package com.sober.feature.focus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Custom button used to derive its "active" state from a remembered custom
 * value rather than from the current selection, so tapping a preset after a
 * custom duration left the button claiming the old value. The state is gone;
 * what remains is this classification, which decides which control shows as
 * selected.
 */
class DurationSelectionTest {

    @Test
    fun `every preset is a preset`() {
        listOf(15, 25, 30, 45, 60, 90, 120).forEach {
            assertTrue("$it should be a preset", isPresetDuration(it))
        }
    }

    @Test
    fun `a typed duration is not a preset`() {
        listOf(5, 20, 50, 100, 12 * 60).forEach {
            assertFalse("$it should be custom", isPresetDuration(it))
        }
    }
}
