package com.sober.core.domain.usecase

import com.sober.core.domain.model.UsageSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportUsageCsvUseCaseTest {

    private val dates = GetUsageHistoryUseCase.recentDates(ExportUsageCsvUseCase.DEFAULT_DAYS)

    /** The oldest and newest dates inside the export window. */
    private val firstDate = dates.first()
    private val lastDate = dates.last()

    private fun useCase(rows: List<UsageSession>) =
        ExportUsageCsvUseCase(FakeUsageSessionRepository(rows))

    @Test
    fun `an empty history exports the header only`() = runTest {
        val csv = useCase(emptyList())()

        assertEquals("${ExportUsageCsvUseCase.HEADER}\n", csv)
    }

    @Test
    fun `rows are ordered by date, then usage descending, then package`() = runTest {
        val csv = useCase(
            listOf(
                UsageSession(packageName = "com.b", date = lastDate, usedMinutes = 5),
                UsageSession(packageName = "com.a", date = firstDate, usedMinutes = 10),
                UsageSession(packageName = "com.c", date = lastDate, usedMinutes = 40),
                UsageSession(packageName = "com.a", date = lastDate, usedMinutes = 10)
            )
        )()

        assertEquals(
            buildString {
                append(ExportUsageCsvUseCase.HEADER).append('\n')
                append("$firstDate,com.a,10\n")
                append("$lastDate,com.c,40\n")
                append("$lastDate,com.a,10\n")
                append("$lastDate,com.b,5\n")
            },
            csv
        )
    }

    @Test
    fun `rows outside the window are not exported`() = runTest {
        val csv = useCase(
            listOf(
                UsageSession(packageName = "com.inside", date = lastDate, usedMinutes = 5),
                UsageSession(packageName = "com.outside", date = "2000-01-01", usedMinutes = 99)
            )
        )()

        assertEquals(
            "${ExportUsageCsvUseCase.HEADER}\n$lastDate,com.inside,5\n",
            csv
        )
    }

    @Test
    fun `fields containing separators are quoted`() = runTest {
        val csv = useCase(
            listOf(UsageSession(packageName = "comma,name", date = lastDate, usedMinutes = 1))
        )()

        val row = csv.lines()[1]
        assertEquals("$lastDate,\"comma,name\",1", row)
    }

    @Test
    fun `embedded quotes are doubled`() = runTest {
        val csv = useCase(
            listOf(UsageSession(packageName = "say\"hi\"", date = lastDate, usedMinutes = 1))
        )()

        val row = csv.lines()[1]
        assertEquals("$lastDate,\"say\"\"hi\"\"\",1", row)
    }

    @Test
    fun `a custom window is respected`() = runTest {
        val csv = useCase(
            listOf(
                UsageSession(packageName = "com.a", date = lastDate, usedMinutes = 5),
                UsageSession(packageName = "com.a", date = dates[dates.size - 3], usedMinutes = 7),
                UsageSession(packageName = "com.a", date = firstDate, usedMinutes = 9)
            )
        )(days = 2)

        // The file ends with a newline, which lines() reports as a trailing
        // empty element; that is not a row.
        val rows = csv.lines().drop(1).filter { it.isNotBlank() }
        assertEquals(1, rows.size)
        assertEquals("$lastDate,com.a,5", rows.single())
    }
}
