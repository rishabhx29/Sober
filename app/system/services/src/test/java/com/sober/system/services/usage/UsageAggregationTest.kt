package com.sober.system.services.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Today's totals are folded from usage events so they can be clipped to
 * midnight. The regression these tests pin down: `queryUsageStats` expands the
 * range to a whole calendar bucket, which let an untouched app report hours it
 * never held the screen.
 */
class UsageAggregationTest {

    private val midnight = 1_000_000L
    private val now = midnight + 60 * 60_000L // one hour into the day
    private val later = midnight + 9 * 60 * 60_000L // enough room for a night and a morning

    private fun resumed(pkg: String, at: Long) =
        ForegroundEvent(ForegroundEventKind.RESUMED, pkg, at)

    private fun paused(pkg: String, at: Long) =
        ForegroundEvent(ForegroundEventKind.PAUSED, pkg, at)

    private fun screenOff(at: Long) =
        ForegroundEvent(ForegroundEventKind.SCREEN_OFF, "", at)

    private fun screenOn(at: Long) =
        ForegroundEvent(ForegroundEventKind.SCREEN_ON, "", at)

    @Test
    fun `a session still open is counted up to now`() {
        val usage = aggregateUsageByPackage(
            listOf(resumed("com.chrome", midnight + 10 * 60_000L)),
            midnight,
            now
        )

        assertEquals(50 * 60_000L, usage.getValue("com.chrome").millis)
    }

    @Test
    fun `a closed session counts only its own span`() {
        val usage = aggregateUsageByPackage(
            listOf(
                resumed("com.chrome", midnight + 10 * 60_000L),
                paused("com.chrome", midnight + 25 * 60_000L)
            ),
            midnight,
            now
        )

        assertEquals(15 * 60_000L, usage.getValue("com.chrome").millis)
    }

    @Test
    fun `several sessions for one app add up`() {
        val usage = aggregateUsageByPackage(
            listOf(
                resumed("com.chrome", midnight + 1 * 60_000L),
                paused("com.chrome", midnight + 6 * 60_000L),
                resumed("com.chrome", midnight + 11 * 60_000L),
                paused("com.chrome", midnight + 14 * 60_000L)
            ),
            midnight,
            now
        )

        assertEquals(8 * 60_000L, usage.getValue("com.chrome").millis)
    }

    @Test
    fun `a pause from another app does not close the open one`() {
        val usage = aggregateUsageByPackage(
            listOf(
                resumed("com.chrome", midnight + 10 * 60_000L),
                paused("com.mail", midnight + 20 * 60_000L)
            ),
            midnight,
            now
        )

        assertEquals(50 * 60_000L, usage.getValue("com.chrome").millis)
        assertTrue("com.mail" !in usage)
    }

    @Test
    fun `switching apps closes the previous session`() {
        val usage = aggregateUsageByPackage(
            listOf(
                resumed("com.chrome", midnight + 10 * 60_000L),
                resumed("com.youtube", midnight + 40 * 60_000L)
            ),
            midnight,
            now
        )

        assertEquals(30 * 60_000L, usage.getValue("com.chrome").millis)
        assertEquals(20 * 60_000L, usage.getValue("com.youtube").millis)
    }

    @Test
    fun `screen off closes the session`() {
        val usage = aggregateUsageByPackage(
            listOf(
                resumed("com.chrome", midnight + 10 * 60_000L),
                screenOff(midnight + 20 * 60_000L),
                // Waking the phone is what lets the next resume count; without
                // it that resume is a background start and stays uncounted.
                screenOn(midnight + 45 * 60_000L),
                resumed("com.chrome", midnight + 50 * 60_000L)
            ),
            midnight,
            now
        )

        assertEquals(20 * 60_000L, usage.getValue("com.chrome").millis)
    }

    @Test
    fun `time outside the range is clipped away`() {
        val usage = aggregateUsageByPackage(
            listOf(
                resumed("com.chrome", midnight - 30 * 60_000L),
                paused("com.chrome", midnight + 5 * 60_000L)
            ),
            midnight,
            now
        )

        assertEquals(5 * 60_000L, usage.getValue("com.chrome").millis)
    }

    @Test
    fun `the last resume is reported as the last used time`() {
        val lastResume = midnight + 45 * 60_000L
        val usage = aggregateUsageByPackage(
            listOf(
                resumed("com.chrome", midnight + 5 * 60_000L),
                paused("com.chrome", midnight + 10 * 60_000L),
                resumed("com.chrome", lastResume),
                paused("com.chrome", midnight + 50 * 60_000L)
            ),
            midnight,
            now
        )

        assertEquals(lastResume, usage.getValue("com.chrome").lastResumedAt)
    }

    @Test
    fun `an app with no foreground time is absent`() {
        val usage = aggregateUsageByPackage(
            listOf(paused("com.photos", midnight + 5 * 60_000L)),
            midnight,
            now
        )

        assertTrue(usage.isEmpty())
    }

    @Test
    fun `an empty range yields nothing`() {
        assertTrue(aggregateUsageByPackage(emptyList(), now, now).isEmpty())
    }

    @Test
    fun `a resume while the screen is off earns no time`() {
        // The reported bug: a background activity resumes at 3am, the phone is
        // asleep, and the app keeps the screen until someone else resumes.
        val usage = aggregateUsageByPackage(
            listOf(
                screenOff(midnight + 1 * 60_000L),
                resumed("com.photos", midnight + 3 * 60 * 60_000L),
                screenOn(midnight + 7 * 60 * 60_000L),
                resumed("com.chrome", midnight + 8 * 60 * 60_000L)
            ),
            midnight,
            later,
            initiallyInteractive = true
        )

        assertTrue("com.photos" !in usage)
        assertEquals(60 * 60_000L, usage.getValue("com.chrome").millis)
    }

    @Test
    fun `a resume between midnight and the first screen-on is still ignored`() {
        // The day starts with the screen off, as it does when the phone slept
        // through midnight; a resume at 2am is a background start, not a person.
        val usage = aggregateUsageByPackage(
            listOf(
                resumed("com.photos", midnight + 2 * 60 * 60_000L),
                screenOn(midnight + 7 * 60 * 60_000L),
                resumed("com.chrome", midnight + 8 * 60 * 60_000L)
            ),
            midnight,
            later,
            initiallyInteractive = false
        )

        assertTrue("com.photos" !in usage)
        assertEquals(60 * 60_000L, usage.getValue("com.chrome").millis)
    }

    @Test
    fun `waking the screen does not credit the app underneath`() {
        val usage = aggregateUsageByPackage(
            listOf(
                resumed("com.photos", midnight + 2 * 60 * 60_000L),
                screenOn(midnight + 7 * 60 * 60_000L)
            ),
            midnight,
            later,
            initiallyInteractive = false
        )

        assertTrue(usage.isEmpty())
    }

    @Test
    fun `time asleep inside a session is not counted`() {
        val usage = aggregateUsageByPackage(
            listOf(
                resumed("com.chrome", midnight + 10 * 60_000L),
                screenOff(midnight + 20 * 60_000L),
                screenOn(midnight + 50 * 60_000L)
            ),
            midnight,
            later
        )

        assertEquals(10 * 60_000L, usage.getValue("com.chrome").millis)
    }
}
