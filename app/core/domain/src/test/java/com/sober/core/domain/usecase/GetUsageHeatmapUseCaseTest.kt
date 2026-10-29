package com.sober.core.domain.usecase

import com.sober.core.domain.model.UsageInterval
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class GetUsageHeatmapUseCaseTest {

    private val zone = ZoneId.of("UTC")

    /** 2026-03-02 is a Monday, which makes row 0 easy to reason about. */
    private fun monday(hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 3, 2, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun minutes(m: Int): Long = m * 60_000L

    private val mondayMidnight = monday(0)
    private val weekLater = mondayMidnight + 7 * 24 * 60 * 60 * 1000L

    private fun heatmap(vararg intervals: UsageInterval) =
        buildUsageHeatmap(intervals.toList(), mondayMidnight, weekLater, zone)

    private fun cell(heatmap: UsageHeatmap, weekday: Int, hour: Int): Int =
        heatmap.cells[weekday][hour]

    @Test
    fun `gives every weekday a full day of columns`() {
        assertEquals(7, heatmap().cells.size)
        assertTrue(heatmap().cells.all { it.size == 24 })
    }

    @Test
    fun `an interval inside one hour lands in that weekday and hour`() {
        val result = heatmap(UsageInterval(monday(9, 10), monday(9, 25)))

        assertEquals(15, cell(result, weekday = 0, hour = 9))
        assertEquals(0, cell(result, weekday = 0, hour = 8))
        assertEquals(0, cell(result, weekday = 1, hour = 9))
    }

    @Test
    fun `an interval across an hour boundary is split between the columns`() {
        val result = heatmap(UsageInterval(monday(9, 30), monday(10, 15)))

        assertEquals(30, cell(result, weekday = 0, hour = 9))
        assertEquals(15, cell(result, weekday = 0, hour = 10))
    }

    @Test
    fun `an interval across midnight is split between weekdays`() {
        val result = heatmap(UsageInterval(monday(23, 30), mondayMidnight + minutes(24 * 60 + 30)))

        assertEquals(30, cell(result, weekday = 0, hour = 23))
        assertEquals(30, cell(result, weekday = 1, hour = 0))
    }

    @Test
    fun `usage is mapped onto the matching weekday row`() {
        val wednesday = monday(0) + 2 * 24 * 60 * 60 * 1000L
        val result = heatmap(
            UsageInterval(wednesday + minutes(15 * 60), wednesday + minutes(15 * 60 + 20))
        )

        assertEquals(20, cell(result, weekday = 2, hour = 15))
        assertEquals(0, cell(result, weekday = 0, hour = 15))
    }

    @Test
    fun `intervals outside the range are clipped to the range`() {
        val result = buildUsageHeatmap(
            listOf(UsageInterval(mondayMidnight - minutes(30), mondayMidnight + minutes(30))),
            mondayMidnight,
            weekLater,
            zone
        )

        assertEquals(30, cell(result, weekday = 0, hour = 0))
    }

    @Test
    fun `whole minutes are kept and partial minutes are discarded`() {
        val result = heatmap(UsageInterval(monday(9), monday(9) + 90_000L))

        assertEquals(1, cell(result, weekday = 0, hour = 9))
    }

    @Test
    fun `empty input yields no data and no busiest hour`() {
        val result = heatmap()

        assertFalse(result.hasData)
        assertNull(result.busiestHour)
        assertNull(result.busiestWindowLabel)
    }

    @Test
    fun `the busiest hour is reported with a two hour window label`() {
        val result = heatmap(
            UsageInterval(monday(22), monday(22) + minutes(40)),
            UsageInterval(monday(9), monday(9) + minutes(10))
        )

        assertEquals(22, result.busiestHour)
        assertEquals("10 PM–12 AM", result.busiestWindowLabel)
        assertTrue(result.hasData)
    }

    @Test
    fun `an inverted range is empty rather than an error`() {
        val result = buildUsageHeatmap(
            listOf(UsageInterval(mondayMidnight, mondayMidnight + minutes(10))),
            weekLater,
            mondayMidnight,
            zone
        )

        assertFalse(result.hasData)
    }

    @Test
    fun `window labels wrap around midnight correctly`() {
        assertEquals("12 AM–2 AM", windowLabel(0))
        assertEquals("12 PM–2 PM", windowLabel(12))
        assertEquals("11 PM–1 AM", windowLabel(23))
    }

    @Test
    fun `the use case reads intervals and builds the grid`() = runTest {
        val repository = com.sober.core.domain.usecase.FakeUsageStatsRepository(
            intervals = listOf(
                UsageInterval(
                    System.currentTimeMillis() - minutes(20),
                    System.currentTimeMillis()
                )
            )
        )

        val result = GetUsageHeatmapUseCase(repository)(days = 7, zone = zone)

        assertTrue(result.hasData)
    }
}
