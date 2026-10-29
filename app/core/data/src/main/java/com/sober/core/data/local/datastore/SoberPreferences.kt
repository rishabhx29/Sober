package com.sober.core.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.sober.core.domain.model.NotificationSilenceScope
import com.sober.core.domain.model.StreakRecord
import com.sober.core.domain.repository.AlertKind
import com.sober.core.domain.repository.StreakRule
import com.sober.core.domain.repository.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sober_preferences")

@Singleton
class SoberPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
        val KEY_IS_USAGE_ACCESS_GRANTED = booleanPreferencesKey("is_usage_access_granted")

        // Active Focus Session
        val KEY_ACTIVE_SESSION_ID = androidx.datastore.preferences.core.intPreferencesKey("active_session_id")
        val KEY_ACTIVE_SESSION_START_TIME = androidx.datastore.preferences.core.longPreferencesKey("active_session_start_time")
        val KEY_ACTIVE_SESSION_DURATION = androidx.datastore.preferences.core.longPreferencesKey("active_session_duration")
        val KEY_ACTIVE_SESSION_LABEL = androidx.datastore.preferences.core.stringPreferencesKey("active_session_label")
        val KEY_ACTIVE_SESSION_BLOCKED_APPS = androidx.datastore.preferences.core.stringPreferencesKey("active_session_blocked_apps")
        val KEY_ACTIVE_SESSION_SILENCE_NOTIFICATIONS = androidx.datastore.preferences.core.booleanPreferencesKey("active_session_silence_notifications")

        // Daily rollover bookkeeping (YYYY-MM-DD)
        val KEY_LAST_USAGE_ROLLOVER_DAY = androidx.datastore.preferences.core.stringPreferencesKey("last_usage_rollover_day")

        // Overall daily screen-time cap, independent of per-app limits
        val KEY_DAILY_SCREEN_TIME_LIMIT = androidx.datastore.preferences.core.intPreferencesKey("daily_screen_time_limit_minutes")
        const val DEFAULT_DAILY_SCREEN_TIME_LIMIT = 120

        // Notification silencing scope for the active session
        val KEY_NOTIFICATION_SILENCE_SCOPE = androidx.datastore.preferences.core.stringPreferencesKey("notification_silence_scope")

        // 80% usage warnings already fired, as "yyyy-MM-dd|key;key"
        val KEY_USAGE_WARNING_LOG = androidx.datastore.preferences.core.stringPreferencesKey("usage_warning_log")

        // Epoch millis until which all blocking is lifted (Meal Break)
        val KEY_MEAL_BREAK_UNTIL = androidx.datastore.preferences.core.longPreferencesKey("meal_break_until")

        // PBKDF2 record for the override PIN, as "iterations:salt:hash"
        val KEY_PIN_RECORD = androidx.datastore.preferences.core.stringPreferencesKey("pin_record")

        // Emergency override: a pending request (after the PIN gate) and the
        // granted window. Package names are stored so neither can leak across
        // apps — an override for one app must never lift another's block.
        val KEY_OVERRIDE_REQUEST_PACKAGE = androidx.datastore.preferences.core.stringPreferencesKey("override_request_package")
        val KEY_OVERRIDE_REQUESTED_AT = androidx.datastore.preferences.core.longPreferencesKey("override_requested_at")
        val KEY_OVERRIDE_PACKAGE = androidx.datastore.preferences.core.stringPreferencesKey("override_package")
        val KEY_OVERRIDE_UNTIL = androidx.datastore.preferences.core.longPreferencesKey("override_until")

        // Per-alert opt-outs. Absent means enabled; see AlertKind.
        val KEY_ALERT_USAGE_WARNING = androidx.datastore.preferences.core.booleanPreferencesKey("alert_usage_warning")
        val KEY_ALERT_SESSION_COMPLETE = androidx.datastore.preferences.core.booleanPreferencesKey("alert_session_complete")
        val KEY_ALERT_STREAK_WARNING = androidx.datastore.preferences.core.booleanPreferencesKey("alert_streak_warning")
        val KEY_ALERT_DAILY_STREAK = androidx.datastore.preferences.core.booleanPreferencesKey("alert_daily_streak")
        val KEY_ALERT_BEDTIME_WARNING = androidx.datastore.preferences.core.booleanPreferencesKey("alert_bedtime_warning")

        // What counts as a successful day, stored by name for readability.
        val KEY_STREAK_RULE = androidx.datastore.preferences.core.stringPreferencesKey("streak_rule")

        // Persisted streak record. The longest streak has to outlive the
        // 60-day history window, so it can't be re-derived from usage_sessions.
        val KEY_STREAK_CURRENT = androidx.datastore.preferences.core.intPreferencesKey("streak_current")
        val KEY_STREAK_LONGEST = androidx.datastore.preferences.core.intPreferencesKey("streak_longest")
        val KEY_STREAK_LAST_SUCCESS =
            androidx.datastore.preferences.core.stringPreferencesKey("streak_last_success_date")

        // SYSTEM / LIGHT / DARK
        val KEY_THEME_MODE = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")

        // Achievement unlocks as "ID:epochMillis;ID:epochMillis"
        val KEY_ACHIEVEMENTS = androidx.datastore.preferences.core.stringPreferencesKey("achievements")

        // Dismissed schedule suggestions, by enum name
        val KEY_DISMISSED_SUGGESTIONS =
            androidx.datastore.preferences.core.stringPreferencesKey("dismissed_suggestions")

        // The section rules document last parsed successfully. Section rules
        // ship as an asset, so this only matters if the asset cannot be read.
        val KEY_DETECTION_RULES_JSON =
            androidx.datastore.preferences.core.stringPreferencesKey("detection_rules_json")

        // Section rules the user switched on, by DetectionRule.id
        val KEY_SECTION_RULES =
            androidx.datastore.preferences.core.stringSetPreferencesKey("section_rules")

        // The user's own rules, one encoded record per entry. See
        // CustomRuleCodec for the field order.
        val KEY_CUSTOM_RULES =
            androidx.datastore.preferences.core.stringSetPreferencesKey("custom_rules")
    }

    val isOnboardingComplete: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_IS_ONBOARDING_COMPLETE] ?: false
        }

    val isUsageAccessGranted: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_IS_USAGE_ACCESS_GRANTED] ?: false
        }

    suspend fun setOnboardingComplete(isComplete: Boolean) {

        dataStore.edit { preferences ->
            preferences[KEY_IS_ONBOARDING_COMPLETE] = isComplete
        }
    }

    suspend fun setUsageAccessGranted(isGranted: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_USAGE_ACCESS_GRANTED] = isGranted
        }
    }

    val activeSessionId: Flow<Int?> = dataStore.data.map { it[KEY_ACTIVE_SESSION_ID] }
    val activeSessionStartTime: Flow<Long?> = dataStore.data.map { it[KEY_ACTIVE_SESSION_START_TIME] }
    val activeSessionDuration: Flow<Long?> = dataStore.data.map { it[KEY_ACTIVE_SESSION_DURATION] }
    val activeSessionLabel: Flow<String?> = dataStore.data.map { it[KEY_ACTIVE_SESSION_LABEL] }
    val activeSessionBlockedApps: Flow<String?> = dataStore.data.map { it[KEY_ACTIVE_SESSION_BLOCKED_APPS] }
    val activeSessionSilenceNotifications: Flow<Boolean> = dataStore.data.map { it[KEY_ACTIVE_SESSION_SILENCE_NOTIFICATIONS] ?: false }

    val lastUsageRolloverDay: Flow<String?> = dataStore.data.map { it[KEY_LAST_USAGE_ROLLOVER_DAY] }

    val dailyScreenTimeLimitMinutes: Flow<Int> = dataStore.data.map {
        it[KEY_DAILY_SCREEN_TIME_LIMIT] ?: DEFAULT_DAILY_SCREEN_TIME_LIMIT
    }

    /**
     * Parsed leniently: an unknown stored value falls back to null (treat as
     * unset) instead of throwing, so a downgrade cannot brick notification
     * filtering.
     */
    val notificationSilenceScope: Flow<NotificationSilenceScope?> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATION_SILENCE_SCOPE]?.let { raw ->
            runCatching { NotificationSilenceScope.valueOf(raw) }.getOrNull()
        }
    }

    suspend fun setLastUsageRolloverDay(day: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_USAGE_ROLLOVER_DAY] = day
        }
    }

    suspend fun setDailyScreenTimeLimitMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_SCREEN_TIME_LIMIT] = minutes.coerceAtLeast(0)
        }
    }

    suspend fun setNotificationSilenceScope(scope: NotificationSilenceScope) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_SILENCE_SCOPE] = scope.name
        }
    }

    suspend fun setActiveSession(
        id: Int,
        startTime: Long,
        durationMs: Long,
        label: String,
        blockedApps: String,
        silenceScope: NotificationSilenceScope
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_SESSION_ID] = id
            preferences[KEY_ACTIVE_SESSION_START_TIME] = startTime
            preferences[KEY_ACTIVE_SESSION_DURATION] = durationMs
            preferences[KEY_ACTIVE_SESSION_LABEL] = label
            preferences[KEY_ACTIVE_SESSION_BLOCKED_APPS] = blockedApps
            preferences[KEY_ACTIVE_SESSION_SILENCE_NOTIFICATIONS] = silenceScope != NotificationSilenceScope.NONE
            preferences[KEY_NOTIFICATION_SILENCE_SCOPE] = silenceScope.name
        }
    }

    suspend fun clearActiveSession() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACTIVE_SESSION_ID)
            preferences.remove(KEY_ACTIVE_SESSION_START_TIME)
            preferences.remove(KEY_ACTIVE_SESSION_DURATION)
            preferences.remove(KEY_ACTIVE_SESSION_LABEL)
            preferences.remove(KEY_ACTIVE_SESSION_BLOCKED_APPS)
            preferences.remove(KEY_ACTIVE_SESSION_SILENCE_NOTIFICATIONS)
        }
    }

    /**
     * Warnings already fired today, as `yyyy-MM-dd|key;key`.
     *
     * Day and keys live in one string so a stale key can never be read as
     * belonging to a new day: the day prefix is checked on every read.
     */
    val usageWarningLog: Flow<UsageWarningLog> = dataStore.data.map { preferences ->
        parseWarningLog(preferences[KEY_USAGE_WARNING_LOG])
    }

    /** Epoch millis until which all blocking is lifted, or null. */
    val mealBreakUntil: Flow<Long?> = dataStore.data.map { it[KEY_MEAL_BREAK_UNTIL] }

    suspend fun setMealBreakUntil(until: Long?) {
        dataStore.edit { preferences ->
            if (until == null) {
                preferences.remove(KEY_MEAL_BREAK_UNTIL)
            } else {
                preferences[KEY_MEAL_BREAK_UNTIL] = until
            }
        }
    }

    /** The PBKDF2 PIN record, or null when no PIN has been set. */
    val pinRecord: Flow<String?> = dataStore.data.map { it[KEY_PIN_RECORD] }

    suspend fun setPinRecord(record: String) {
        dataStore.edit { it[KEY_PIN_RECORD] = record }
    }

    suspend fun clearPinRecord() {
        dataStore.edit { it.remove(KEY_PIN_RECORD) }
    }

    /** True unless the user explicitly turned this alert off. */
    fun alertEnabled(kind: AlertKind): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[alertKey(kind)] ?: true
    }

    suspend fun setAlertEnabled(kind: AlertKind, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[alertKey(kind)] = enabled
        }
    }

    private fun alertKey(kind: AlertKind) = when (kind) {
        AlertKind.USAGE_WARNING -> KEY_ALERT_USAGE_WARNING
        AlertKind.SESSION_COMPLETE -> KEY_ALERT_SESSION_COMPLETE
        AlertKind.STREAK_WARNING -> KEY_ALERT_STREAK_WARNING
        AlertKind.DAILY_STREAK -> KEY_ALERT_DAILY_STREAK
        AlertKind.BEDTIME_WARNING -> KEY_ALERT_BEDTIME_WARNING
    }

    /**
     * Lenient by design: an unknown stored name falls back to the default rule
     * rather than throwing, so a downgrade cannot make the Home screen crash.
     */
    val streakRule: Flow<StreakRule> = dataStore.data.map { preferences ->
        preferences[KEY_STREAK_RULE]?.let { raw ->
            runCatching { StreakRule.valueOf(raw) }.getOrNull()
        } ?: StreakRule.UNDER_LIMITS
    }

    suspend fun setStreakRule(rule: StreakRule) {
        dataStore.edit { preferences ->
            preferences[KEY_STREAK_RULE] = rule.name
        }
    }

    val streakRecord: Flow<StreakRecord> = dataStore.data.map { preferences ->
        StreakRecord(
            currentStreak = preferences[KEY_STREAK_CURRENT] ?: 0,
            longestStreak = preferences[KEY_STREAK_LONGEST] ?: 0,
            lastSuccessDate = preferences[KEY_STREAK_LAST_SUCCESS]
        )
    }

    /** Read-modify-write inside one `edit` so concurrent recorders can't clobber each other. */
    suspend fun updateStreakRecord(transform: (StreakRecord) -> StreakRecord) {
        dataStore.edit { preferences ->
            val current = StreakRecord(
                currentStreak = preferences[KEY_STREAK_CURRENT] ?: 0,
                longestStreak = preferences[KEY_STREAK_LONGEST] ?: 0,
                lastSuccessDate = preferences[KEY_STREAK_LAST_SUCCESS]
            )
            val next = transform(current)
            preferences[KEY_STREAK_CURRENT] = next.currentStreak
            preferences[KEY_STREAK_LONGEST] = next.longestStreak
            next.lastSuccessDate?.let { preferences[KEY_STREAK_LAST_SUCCESS] = it }
        }
    }

    /** Lenient like the other stored enums: an unknown name means SYSTEM. */
    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE]?.let { raw ->
            runCatching { ThemeMode.valueOf(raw) }.getOrNull()
        } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    /** Encoded achievement unlocks; decoding belongs to the repository. */
    val achievementsRaw: Flow<String?> = dataStore.data.map { it[KEY_ACHIEVEMENTS] }

    suspend fun setAchievementsRaw(value: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ACHIEVEMENTS] = value
        }
    }

    /** Encoded dismissals; decoding belongs to the repository. */
    val dismissedScheduleSuggestionsRaw: Flow<String?> =
        dataStore.data.map { it[KEY_DISMISSED_SUGGESTIONS] }

    suspend fun setDismissedScheduleSuggestionsRaw(value: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DISMISSED_SUGGESTIONS] = value
        }
    }

    val overrideRequestPackage: Flow<String?> = dataStore.data.map { it[KEY_OVERRIDE_REQUEST_PACKAGE] }
    val overrideRequestedAt: Flow<Long?> = dataStore.data.map { it[KEY_OVERRIDE_REQUESTED_AT] }
    val overridePackage: Flow<String?> = dataStore.data.map { it[KEY_OVERRIDE_PACKAGE] }
    val overrideUntil: Flow<Long?> = dataStore.data.map { it[KEY_OVERRIDE_UNTIL] }

    suspend fun setOverrideRequest(packageName: String, requestedAt: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERRIDE_REQUEST_PACKAGE] = packageName
            preferences[KEY_OVERRIDE_REQUESTED_AT] = requestedAt
        }
    }

    suspend fun clearOverrideRequest() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_OVERRIDE_REQUEST_PACKAGE)
            preferences.remove(KEY_OVERRIDE_REQUESTED_AT)
        }
    }

    suspend fun setOverride(packageName: String, until: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_OVERRIDE_PACKAGE] = packageName
            preferences[KEY_OVERRIDE_UNTIL] = until
        }
    }

    suspend fun clearOverride() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_OVERRIDE_PACKAGE)
            preferences.remove(KEY_OVERRIDE_UNTIL)
        }
    }

    suspend fun markUsageWarningFired(day: String, key: String) {
        dataStore.edit { preferences ->
            val existing = parseWarningLog(preferences[KEY_USAGE_WARNING_LOG])
            val keys = if (existing.day == day) existing.keys.toMutableSet() else mutableSetOf()
            keys.add(key)
            preferences[KEY_USAGE_WARNING_LOG] = "$day|${keys.joinToString(";")}"
        }
    }

    private fun parseWarningLog(raw: String?): UsageWarningLog {
        if (raw.isNullOrBlank()) return UsageWarningLog(day = null, keys = emptySet())
        val day = raw.substringBefore('|', "")
        val keys = raw.substringAfter('|', "")
            .split(';')
            .filter { it.isNotBlank() }
            .toSet()
        return UsageWarningLog(day = day.ifBlank { null }, keys = keys)
    }

    val detectionRulesJson: Flow<String?> = dataStore.data.map { it[KEY_DETECTION_RULES_JSON] }

    suspend fun setDetectionRulesJson(raw: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DETECTION_RULES_JSON] = raw
        }
    }

    val enabledSectionRules: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[KEY_SECTION_RULES] ?: emptySet()
    }

    suspend fun setSectionRuleEnabled(ruleId: String, enabled: Boolean) {
        dataStore.edit { preferences ->
            val current = preferences[KEY_SECTION_RULES] ?: emptySet()
            preferences[KEY_SECTION_RULES] =
                if (enabled) current + ruleId else current - ruleId
        }
    }

    val customRulesRaw: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_RULES] ?: emptySet()
    }

    /**
     * Read-modify-write inside one edit so two callers (the settings screen and
     * a service reacting to the same change) cannot drop each other's writes.
     */
    suspend fun updateCustomRules(transform: (Set<String>) -> Set<String>) {
        dataStore.edit { preferences ->
            val current = preferences[KEY_CUSTOM_RULES] ?: emptySet()
            preferences[KEY_CUSTOM_RULES] = transform(current)
        }
    }
}

/** The day a warning log was written for, plus the warning keys fired that day. */
data class UsageWarningLog(val day: String?, val keys: Set<String>)
