package com.sober.core.domain.usecase

import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.repository.UsageSessionRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Zeroes daily usage counters once the calendar day changes.
 *
 * The `usage_sessions` table is keyed by date, so a new day naturally starts
 * empty; this handles the in-place case the app actually renders against, and
 * records the rollover day so a process restart does not replay it.
 *
 * Runs on every poll rather than via an alarm: the service is already awake
 * every cycle, and polling makes this immune to missed alarms, timezone
 * changes, and Doze deferral on OEM ROMs.
 */
class ResetDailyUsageUseCase @Inject constructor(
    private val usageSessionRepository: UsageSessionRepository,
    private val sessionStateRepository: SessionStateRepository
) {
    /** Returns true when a rollover was performed on this call. */
    suspend operator fun invoke(): Boolean {
        val today = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
        val lastRollover = sessionStateRepository.lastUsageRolloverDay.first()

        // Equal means the rollover already ran today. Later means the marker
        // is ahead of the calendar (the clock was moved backwards, or a
        // timezone change landed on the other side of midnight); resetting
        // then would zero rows the user still sees as today's. Leave the
        // marker alone and let the real date catch up.
        if (lastRollover != null && lastRollover >= today) return false

        // Only zero rows that are not already today's. `resetDailyUsage()`
        // clears for today's date, so guard against wiping fresh data when
        // the stored marker is simply absent on a first run.
        if (lastRollover != null) {
            usageSessionRepository.resetDailyUsage()
        }
        sessionStateRepository.setLastUsageRolloverDay(today)
        return true
    }

    companion object {
        const val DATE_FORMAT = "yyyy-MM-dd"
    }
}