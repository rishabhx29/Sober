package com.sober.core.domain.usecase

import com.sober.core.domain.repository.AlertNotifier
import com.sober.core.domain.repository.AlertKind
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.NotificationPreferencesRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Fires the "you're at 80%" warning once per app and once for the overall cap.
 *
 * Runs on every enforcement poll, so the two things that must not happen are a
 * repeat notification every 15 seconds and a warning after the block has
 * already landed. Both are handled here rather than in the service:
 *
 *  - the fired keys are persisted per calendar day, so a service restart does
 *    not replay them;
 *  - the window is 80% up to (but excluding) 100%, because at 100% the overlay
 *    itself is the notification and a second alert would be noise.
 *
 * Minutes are computed the same way [EvaluateBlockDecisionUseCase] computes
 * them (the larger of live and stored usage), so the warning cannot fire on a
 * number the block does not agree with.
 */
class CheckUsageWarningsUseCase @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val usageSessionRepository: UsageSessionRepository,
    private val appLimitRepository: AppLimitRepository,
    private val sessionStateRepository: SessionStateRepository,
    private val getDailyScreenTimeUseCase: GetDailyScreenTimeUseCase,
    private val alertNotifier: AlertNotifier,
    private val notificationPreferences: NotificationPreferencesRepository
) {
    suspend operator fun invoke() {
        // Returning before the fired keys are written is deliberate: if the
        // user turns warnings back on later today, the ones still in their
        // window should fire rather than be suppressed by a key that was
        // recorded while they were muted.
        if (!notificationPreferences.isEnabled(AlertKind.USAGE_WARNING).first()) return

        val day = today()
        val alreadyWarned = sessionStateRepository.warnedUsageKeys(day).toMutableSet()

        val liveUsage = runCatching {
            usageStatsRepository.getUsageStatsForToday()
                .associate { it.packageName to (it.totalTimeInForeground / 60_000L).toInt() }
        }.getOrDefault(emptyMap())

        appLimitRepository.getAllLimits()
            .first()
            .filter { it.isActive && it.dailyLimitMinutes > 0 }
            .forEach { limit ->
                val key = appKey(limit.packageName)
                if (key in alreadyWarned) return@forEach

                val live = liveUsage[limit.packageName] ?: 0
                val stored = usageSessionRepository.getUsageForToday(limit.packageName)
                    ?.usedMinutes
                    ?: 0
                val used = maxOf(live, stored)

                if (!isInWarningWindow(used, limit.dailyLimitMinutes)) return@forEach

                alertNotifier.notifyUsageWarning(limit.packageName, used, limit.dailyLimitMinutes)
                alreadyWarned += key
                sessionStateRepository.markUsageWarningFired(day, key)
            }

        val cap = sessionStateRepository.dailyScreenTimeLimitMinutes.first()
        if (cap > 0 && OVERALL_KEY !in alreadyWarned) {
            val total = getDailyScreenTimeUseCase()
            if (isInWarningWindow(total, cap)) {
                alertNotifier.notifyUsageWarning(null, total, cap)
                sessionStateRepository.markUsageWarningFired(day, OVERALL_KEY)
            }
        }
    }

    private fun isInWarningWindow(usedMinutes: Int, limitMinutes: Int): Boolean {
        if (limitMinutes <= 0) return false
        val atEightyPercent = usedMinutes.toLong() * 10 >= limitMinutes.toLong() * 8
        return atEightyPercent && usedMinutes < limitMinutes
    }

    private fun today(): String =
        SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US).format(Date())

    private fun appKey(packageName: String) = "app:$packageName"

    private companion object {
        const val OVERALL_KEY = "overall"
    }
}
