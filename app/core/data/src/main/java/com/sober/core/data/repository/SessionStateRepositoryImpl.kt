package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.model.NotificationSilenceScope
import com.sober.core.domain.repository.SessionStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionStateRepositoryImpl @Inject constructor(
    private val soberPreferences: SoberPreferences
) : SessionStateRepository {

    override val activeSessionId: Flow<Int?> = soberPreferences.activeSessionId
    override val activeSessionStartTime: Flow<Long?> = soberPreferences.activeSessionStartTime
    override val activeSessionDuration: Flow<Long?> = soberPreferences.activeSessionDuration
    override val activeSessionLabel: Flow<String?> = soberPreferences.activeSessionLabel
    override val activeSessionBlockedApps: Flow<String?> = soberPreferences.activeSessionBlockedApps
    override val activeSessionSilenceNotifications: Flow<Boolean> = soberPreferences.activeSessionSilenceNotifications
    override val lastUsageRolloverDay: Flow<String?> = soberPreferences.lastUsageRolloverDay
    override val dailyScreenTimeLimitMinutes: Flow<Int> = soberPreferences.dailyScreenTimeLimitMinutes
    override val notificationSilenceScope: Flow<NotificationSilenceScope?> =
        soberPreferences.notificationSilenceScope
    override val mealBreakUntil: Flow<Long?> = soberPreferences.mealBreakUntil

    override suspend fun setActiveSession(
        id: Int,
        startTime: Long,
        durationMs: Long,
        label: String,
        blockedApps: String,
        notificationSilenceScope: NotificationSilenceScope
    ) {
        soberPreferences.setActiveSession(
            id = id,
            startTime = startTime,
            durationMs = durationMs,
            label = label,
            blockedApps = blockedApps,
            silenceScope = notificationSilenceScope
        )
    }

    override suspend fun clearActiveSession() {
        soberPreferences.clearActiveSession()
    }

    override suspend fun setMealBreakUntil(until: Long?) {
        soberPreferences.setMealBreakUntil(until)
    }

    override suspend fun setLastUsageRolloverDay(day: String) {
        soberPreferences.setLastUsageRolloverDay(day)
    }

    override suspend fun warnedUsageKeys(day: String): Set<String> {
        val log = soberPreferences.usageWarningLog.first()
        // A log written on a previous day says nothing about today.
        return if (log.day == day) log.keys else emptySet()
    }

    override suspend fun markUsageWarningFired(day: String, key: String) {
        soberPreferences.markUsageWarningFired(day, key)
    }
}
