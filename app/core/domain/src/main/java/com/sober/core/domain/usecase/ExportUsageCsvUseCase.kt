package com.sober.core.domain.usecase

import com.sober.core.domain.repository.UsageSessionRepository
import javax.inject.Inject

/**
 * Builds the CSV offered from Settings.
 *
 * Only historical usage is exported — no limits, no app names, no device ids.
 * The file is meant to answer "what did I actually do on my phone", and adding
 * derived columns would make it look authoritative about limits that the user
 * may have changed since.
 *
 * Rows are ordered by date, then by usage descending, so a spreadsheet opens on
 * the shape the user asked for without sorting anything.
 */
class ExportUsageCsvUseCase @Inject constructor(
    private val usageSessionRepository: UsageSessionRepository
) {
    suspend operator fun invoke(days: Int = DEFAULT_DAYS): String {
        val dates = GetUsageHistoryUseCase.recentDates(days)
        val rows = runCatching {
            usageSessionRepository.getUsageForDates(dates)
        }.getOrDefault(emptyList())

        return buildString {
            append(HEADER).append('\n')
            rows.asSequence()
                .sortedWith(
                    compareBy(
                        { it.date },
                        { -it.usedMinutes },
                        { it.packageName }
                    )
                )
                .forEach { row ->
                    append(row.date)
                        .append(',')
                        .append(escape(row.packageName))
                        .append(',')
                        .append(row.usedMinutes)
                        .append('\n')
                }
        }
    }

    /**
     * RFC 4180 quoting. Package names cannot contain commas today, but a field
     * that breaks the column count breaks the whole export, so this is not the
     * place to rely on that.
     */
    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    companion object {
        const val DEFAULT_DAYS = 30
        const val HEADER = "date,package,used_minutes"
    }
}
