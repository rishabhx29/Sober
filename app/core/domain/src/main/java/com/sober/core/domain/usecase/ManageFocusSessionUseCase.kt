package com.sober.core.domain.usecase

import com.sober.core.domain.model.FocusSession
import com.sober.core.domain.model.SessionStatus
import com.sober.core.domain.repository.AlertKind
import com.sober.core.domain.repository.AlertNotifier
import com.sober.core.domain.repository.FocusSessionRepository
import com.sober.core.domain.repository.NotificationPreferencesRepository
import com.sober.core.domain.repository.SessionStateRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class ManageFocusSessionUseCase @Inject constructor(
    private val focusSessionRepository: FocusSessionRepository,
    private val sessionStateRepository: SessionStateRepository,
    private val alertNotifier: AlertNotifier,
    private val notificationPreferences: NotificationPreferencesRepository
) {
    /**
     * [durationMinutes] of 0 starts an open-ended session (PRD 2.6): the
     * persisted duration is 0, which every countdown treats as "no end", and
     * only a manual stop ends it.
     */
    suspend fun startSession(
        label: String,
        durationMinutes: Int,
        blockedApps: List<String>,
        silenceScope: com.sober.core.domain.model.NotificationSilenceScope =
            com.sober.core.domain.model.NotificationSilenceScope.SELECTED_APPS
    ): Long {
        val session = FocusSession(
            label = label,
            durationMinutes = durationMinutes,
            startedAt = System.currentTimeMillis(),
            endedAt = null,
            status = SessionStatus.ACTIVE,
            blockedApps = blockedApps
        )
        val id = focusSessionRepository.insertSession(session).toInt()

        // Sync to DataStore for active session state
        val blockedAppsStr = blockedApps.joinToString(",")
        sessionStateRepository.setActiveSession(
            id = id,
            startTime = session.startedAt,
            durationMs = durationMinutes * 60_000L,
            label = label,
            blockedApps = blockedAppsStr,
            notificationSilenceScope = silenceScope
        )

        return id.toLong()
    }

    suspend fun completeSession(sessionId: Int) {
        val session = focusSessionRepository.getSessionById(sessionId) ?: return
        // Both the service ticker and the Focus screen ticker can reach the end
        // of the same session; without this guard the user gets two "session
        // complete" notifications.
        if (session.status != SessionStatus.ACTIVE) return

        focusSessionRepository.updateSession(
            session.copy(
                status = SessionStatus.COMPLETED,
                endedAt = System.currentTimeMillis()
            )
        )
        sessionStateRepository.clearActiveSession()
        if (notificationPreferences.isEnabled(AlertKind.SESSION_COMPLETE).first()) {
            alertNotifier.notifySessionComplete(session.label, session.durationMinutes)
        }
    }

    suspend fun cancelSession(sessionId: Int) {
        val session = focusSessionRepository.getSessionById(sessionId) ?: return
        if (session.status != SessionStatus.ACTIVE) return
        val elapsedMinutes = ((System.currentTimeMillis() - session.startedAt) / 60000).toInt()

        focusSessionRepository.updateSession(
            session.copy(
                status = SessionStatus.CANCELLED,
                endedAt = System.currentTimeMillis(),
                cancelledAtMinute = elapsedMinutes
            )
        )
        sessionStateRepository.clearActiveSession()

        // PRD 4.2.4: the third cancellation in a day is the moment a streak is
        // effectively lost, so warn then. `==` rather than `>=` keeps it to one
        // notification per day; later cancellations do not re-fire it.
        val cancelledToday = focusSessionRepository.countCancelledSince(startOfToday())
        if (cancelledToday == CANCELLATIONS_BEFORE_WARNING &&
            notificationPreferences.isEnabled(AlertKind.STREAK_WARNING).first()
        ) {
            alertNotifier.notifyStreakWarning(cancelledToday)
        }
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        const val CANCELLATIONS_BEFORE_WARNING = 3
    }
}
