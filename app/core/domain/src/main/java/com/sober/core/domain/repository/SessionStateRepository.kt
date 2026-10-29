package com.sober.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface SessionStateRepository {
    val activeSessionId: Flow<Int?>
    val activeSessionStartTime: Flow<Long?>
    val activeSessionDuration: Flow<Long?>
    val activeSessionLabel: Flow<String?>
    val activeSessionBlockedApps: Flow<String?>
    val activeSessionSilenceNotifications: Flow<Boolean>

    /** The last day (YYYY-MM-DD) the daily usage rollover was processed. */
    val lastUsageRolloverDay: Flow<String?>

    /** Overall daily screen-time cap in minutes, independent of per-app limits. */
    val dailyScreenTimeLimitMinutes: Flow<Int>

    /** How aggressively the active session silences notifications. */
    val notificationSilenceScope: Flow<com.sober.core.domain.model.NotificationSilenceScope?>

    /**
     * Epoch millis until which every block is lifted (Meal Break), or null.
     * It is a separate window rather than a session change: the session keeps
     * its history and timer, only enforcement pauses.
     */
    val mealBreakUntil: Flow<Long?>

    suspend fun setActiveSession(
        id: Int,
        startTime: Long,
        durationMs: Long,
        label: String,
        blockedApps: String,
        notificationSilenceScope: com.sober.core.domain.model.NotificationSilenceScope
    )

    suspend fun clearActiveSession()

    /** Sets the pause window; null ends a meal break early. */
    suspend fun setMealBreakUntil(until: Long?)

    suspend fun setLastUsageRolloverDay(day: String)

    /**
     * Warning keys already fired for [day] (YYYY-MM-DD): `app:<package>` or
     * `overall`. Empty when the stored log belongs to a different day.
     */
    suspend fun warnedUsageKeys(day: String): Set<String>

    suspend fun markUsageWarningFired(day: String, key: String)
}
