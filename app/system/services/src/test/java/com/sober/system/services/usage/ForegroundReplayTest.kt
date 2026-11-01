package com.sober.system.services.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Foreground detection is folded from usage events, and events only report
 * *changes*. The regression these tests pin down: a user watching a long video
 * emits nothing, so a replay that starts from "nothing on screen" reports no
 * app at all — which both misses a block and clears an overlay already shown.
 */
class ForegroundReplayTest {

    private val ignored = setOf("com.sober.app", "com.launcher")

    private fun resumed(pkg: String) = ForegroundEvent(ForegroundEventKind.RESUMED, pkg)
    private fun paused(pkg: String) = ForegroundEvent(ForegroundEventKind.PAUSED, pkg)
    private val screenOff = ForegroundEvent(ForegroundEventKind.SCREEN_OFF)

    @Test
    fun `a resume puts that app on screen`() {
        assertEquals("com.youtube", replayForeground(listOf(resumed("com.youtube")), null, ignored))
    }

    @Test
    fun `a pause for the app on screen clears it`() {
        assertNull(replayForeground(listOf(resumed("com.youtube"), paused("com.youtube")), null, ignored))
    }

    @Test
    fun `an empty window keeps the previous answer`() {
        assertEquals(
            "com.youtube",
            replayForeground(emptyList(), initial = "com.youtube", ignored)
        )
        assertNull(replayForeground(emptyList(), initial = null, ignored))
    }

    @Test
    fun `a stale pause from another app does not clear the screen`() {
        val events = listOf(paused("com.mail"))

        assertEquals("com.youtube", replayForeground(events, initial = "com.youtube", ignored))
    }

    @Test
    fun `switching apps moves the answer to the new app`() {
        val events = listOf(resumed("com.chrome"))

        assertEquals("com.chrome", replayForeground(events, initial = "com.youtube", ignored))
    }

    @Test
    fun `sober itself and the launcher count as nothing on screen`() {
        assertNull(replayForeground(listOf(resumed("com.sober.app")), "com.youtube", ignored))
        assertNull(replayForeground(listOf(resumed("com.launcher")), "com.youtube", ignored))
    }

    @Test
    fun `screen off clears whatever was open`() {
        assertNull(replayForeground(listOf(screenOff), initial = "com.youtube", ignored))
    }

    @Test
    fun `a wake and resume resolves to the app opened after unlock`() {
        val events = listOf(screenOff, resumed("com.instagram"))

        assertEquals("com.instagram", replayForeground(events, "com.youtube", ignored))
    }

    @Test
    fun `a resume without a package name is not treated as an app`() {
        assertNull(replayForeground(listOf(resumed("")), "com.youtube", ignored))
    }
}
