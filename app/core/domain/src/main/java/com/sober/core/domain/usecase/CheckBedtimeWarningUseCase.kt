package com.sober.core.domain.usecase

import com.sober.core.domain.model.minutesUntilStart
import com.sober.core.domain.repository.AlertKind
import com.sober.core.domain.repository.AlertNotifier
import com.sober.core.domain.repository.NotificationPreferencesRepository
import com.sober.core.domain.repository.ScheduleRuleRepository
import com.sober.core.domain.repository.SessionStateRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * "Sleep Mode starts in 15 minutes."
 *
 * Only rules whose name contains "sleep" are considered: a schedule is a plain
 * block window with no type field, and warning someone about the start of their
 * work hours under the heading "Bedtime" would be wrong. Templates and
 * suggestions both name the rule "Sleep Mode", so the match is reliable for
 * rules Sober created; a hand-named bedtime rule that does not say "sleep" is
 * the one case that goes unwarned.
 *
 * The warning is keyed per rule per day, so a schedule that starts once fires
 * once. It is a poll-driven check rather than an alarm: the engine already runs
 * every 15 seconds, and a warning that needs a precise wake-up is exactly the
 * kind of alarm that fails on a killed process.
 */
class CheckBedtimeWarningUseCase @Inject constructor(
    private val scheduleRuleRepository: ScheduleRuleRepository,
    private val sessionStateRepository: SessionStateRepository,
    private val alertNotifier: AlertNotifier,
    private val notificationPreferences: NotificationPreferencesRepository
) {
    suspend operator fun invoke(
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ) {
        if (!notificationPreferences.isEnabled(AlertKind.BEDTIME_WARNING).first()) return

        // Earliest upcoming sleep window wins when several are close together:
        // warning about the later one first would be useless.
        val upcoming = scheduleRuleRepository.getActiveRules().first()
            .asSequence()
            .filter { it.ruleName.contains(SLEEP_NAME_HINT, ignoreCase = true) }
            .mapNotNull { rule ->
                rule.minutesUntilStart(nowMillis, zone)?.let { minutes -> rule to minutes }
            }
            .filter { (_, minutes) -> minutes in 1..LEAD_MINUTES }
            .minByOrNull { (_, minutes) -> minutes }
            ?: return

        val (rule, minutes) = upcoming
        val day = SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, Locale.US)
            .format(Date(nowMillis))
        val key = "bedtime:${rule.id}"
        if (key in sessionStateRepository.warnedUsageKeys(day)) return

        alertNotifier.notifyBedtimeWarning(rule.ruleName, minutes)
        sessionStateRepository.markUsageWarningFired(day, key)
    }

    private companion object {
        const val LEAD_MINUTES = 15
        const val SLEEP_NAME_HINT = "sleep"
    }
}
