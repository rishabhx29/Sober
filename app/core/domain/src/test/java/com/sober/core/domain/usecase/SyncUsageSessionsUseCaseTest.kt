package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.UsageSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The rollup is a floor under the live query, so a row that the live query no
 * longer reports has to go. Otherwise an app that was once credited hours it
 * never held — a background resume counted as screen time — keeps that figure
 * on Stats and in the daily cap for the rest of the day.
 */
class SyncUsageSessionsUseCaseTest {

    private fun today(): String =
        SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US).format(Date())

    private fun usage(vararg pairs: Pair<String, Int>) = FakeUsageStatsRepository(
        usage = pairs.map { (pkg, minutes) ->
            AppUsage(packageName = pkg, totalTimeInForeground = minutes * 60_000L, lastTimeUsed = 0L)
        }
    )

    private fun session(pkg: String, minutes: Int) = UsageSession(
        packageName = pkg,
        date = today(),
        usedMinutes = minutes
    )

    @Test
    fun `a stale row the live query no longer reports is removed`() = runTest {
        val sessions = FakeUsageSessionRepository(
            listOf(session("com.photos", 300), session("com.chrome", 5))
        )

        SyncUsageSessionsUseCase(usage("com.chrome" to 12), sessions)()

        assertNull(sessions.sessions.firstOrNull { it.packageName == "com.photos" })
        val chrome = sessions.sessions.firstOrNull { it.packageName == "com.chrome" }
        assertNotNull(chrome)
        assertEquals(12, chrome?.usedMinutes)
    }

    @Test
    fun `an empty live query leaves the rollup alone`() = runTest {
        // A revoked usage-access grant reads as "nothing used today". Deleting
        // on that would erase the totals the floor exists to keep.
        val sessions = FakeUsageSessionRepository(
            listOf(session("com.photos", 300), session("com.chrome", 5))
        )

        SyncUsageSessionsUseCase(usage(), sessions)()

        assertEquals(300, sessions.sessions.first { it.packageName == "com.photos" }.usedMinutes)
        assertEquals(5, sessions.sessions.first { it.packageName == "com.chrome" }.usedMinutes)
    }

    @Test
    fun `excluded packages are neither written nor deleted from another date`() = runTest {
        val yesterday = session("com.photos", 200).copy(date = "2000-01-01")
        val sessions = FakeUsageSessionRepository(listOf(yesterday, session("com.sober.app", 9)))

        SyncUsageSessionsUseCase(
            FakeUsageStatsRepository(
                usage = listOf(
                    AppUsage("com.sober.app", 9 * 60_000L, 0L),
                    AppUsage("com.chrome", 4 * 60_000L, 0L)
                ),
                excludedPackages = setOf("com.sober.app")
            ),
            sessions
        )()

        assertNull(sessions.sessions.firstOrNull { it.packageName == "com.sober.app" })
        assertEquals(200, sessions.sessions.first { it.date == "2000-01-01" }.usedMinutes)
        assertEquals(4, sessions.sessions.first { it.packageName == "com.chrome" }.usedMinutes)
    }
}
