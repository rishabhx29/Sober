package com.sober.feature.home

import com.sober.core.domain.model.AppUsage
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The tile used to print `topApps.size`, and `topApps` is capped at three for
 * the row list, so anyone using more than three apps saw "3". The count now
 * comes from the full usage list.
 */
class AppsUsedCountTest {

    private val ignored = setOf("com.sober.app", "com.launcher")

    private fun usage(pkg: String, millis: Long) =
        AppUsage(packageName = pkg, totalTimeInForeground = millis, lastTimeUsed = 0L)

    @Test
    fun `every app with foreground time counts once`() {
        val rows = listOf(
            usage("com.a", 60_000),
            usage("com.b", 5_000),
            usage("com.c", 90_000),
            usage("com.d", 1_000)
        )

        assertEquals(4, countAppsUsed(rows, ignored))
    }

    @Test
    fun `apps with no foreground time do not count`() {
        val rows = listOf(
            usage("com.a", 60_000),
            usage("com.b", 0)
        )

        assertEquals(1, countAppsUsed(rows, ignored))
    }

    @Test
    fun `sober itself does not count`() {
        val rows = listOf(
            usage("com.sober.app", 120_000),
            usage("com.a", 60_000)
        )

        assertEquals(1, countAppsUsed(rows, ignored))
    }

    @Test
    fun `time on the launcher is not an app being used`() {
        // queryUsageStats reports the home screen as foreground time, so an
        // unfiltered count adds an app that was never opened.
        val rows = listOf(
            usage("com.launcher", 30 * 60_000),
            usage("com.a", 60_000)
        )

        assertEquals(1, countAppsUsed(rows, ignored))
    }
}
