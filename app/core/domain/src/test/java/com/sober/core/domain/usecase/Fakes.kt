package com.sober.core.domain.usecase

import com.sober.core.domain.model.AchievementId
import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.DetectionType
import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.FocusSession
import com.sober.core.domain.model.NotificationSilenceScope
import com.sober.core.domain.model.OverrideLog
import com.sober.core.domain.model.ScheduleRule
import com.sober.core.domain.model.ScheduleSuggestionKind
import com.sober.core.domain.model.SessionStatus
import com.sober.core.domain.model.StreakRecord
import com.sober.core.domain.model.UsageInterval
import com.sober.core.domain.model.UsageSession
import com.sober.core.domain.model.withSuccessfulDay
import com.sober.core.domain.repository.AccessibilityStateRepository
import com.sober.core.domain.repository.AchievementRepository
import com.sober.core.domain.repository.AlertKind
import com.sober.core.domain.repository.AlertNotifier
import com.sober.core.domain.repository.AppCategoryRepository
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.CustomRuleRepository
import com.sober.core.domain.repository.DetectionRuleCatalog
import com.sober.core.domain.repository.DetectionRuleRepository
import com.sober.core.domain.repository.SectionRulePreferencesRepository
import com.sober.core.domain.repository.FocusSessionRepository
import com.sober.core.domain.repository.NotificationPreferencesRepository
import com.sober.core.domain.repository.OverrideLogRepository
import com.sober.core.domain.repository.OverrideStateRepository
import com.sober.core.domain.repository.PinRepository
import com.sober.core.domain.repository.ScheduleRuleRepository
import com.sober.core.domain.repository.ScheduleSuggestionRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.repository.StreakPreferencesRepository
import com.sober.core.domain.repository.StreakRecordRepository
import com.sober.core.domain.repository.StreakRule
import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Hand-written fakes instead of mocks: the repository contracts are small, and
 * fakes keep the tests readable when a use case reads several repositories in
 * one call.
 */

class FakeUsageSessionRepository(
    initial: List<UsageSession> = emptyList()
) : UsageSessionRepository {
    val sessions = initial.toMutableList()

    override suspend fun insertOrUpdateSession(session: UsageSession) {
        sessions.removeAll { it.packageName == session.packageName && it.date == session.date }
        sessions.add(session)
    }

    override suspend fun getUsageForToday(packageName: String): UsageSession? =
        sessions.firstOrNull { it.packageName == packageName && it.date == today() }

    override fun getAllUsageForDate(date: String): Flow<List<UsageSession>> =
        MutableStateFlow(sessions.filter { it.date == date })

    override suspend fun getUsageForDates(dates: List<String>): List<UsageSession> =
        sessions.filter { it.date in dates }

    override suspend fun resetDailyUsage() {
        val index = sessions.indices.filter { sessions[it].date == today() }
        index.forEach { i ->
            sessions[i] = sessions[i].copy(usedMinutes = 0)
        }
    }

    private fun today(): String =
        java.text.SimpleDateFormat(SyncUsageSessionsUseCase.DATE_FORMAT, java.util.Locale.US)
            .format(java.util.Date())
}

class FakeUsageStatsRepository(
    private val usage: List<AppUsage> = emptyList(),
    private val foreground: String? = null,
    private val intervals: List<UsageInterval> = emptyList()
) : UsageStatsRepository {
    override fun getUsageStatsForToday(): List<AppUsage> = usage
    override fun getUsageForApp(packageName: String): AppUsage? =
        usage.firstOrNull { it.packageName == packageName }
    override fun getForegroundPackage(): String? = foreground
    override fun getForegroundIntervals(fromMillis: Long, toMillis: Long): List<UsageInterval> =
        intervals.filter { it.endMillis > fromMillis && it.startMillis < toMillis }
}

class FakeAppLimitRepository(
    initial: List<AppLimit> = emptyList()
) : AppLimitRepository {
    val limits = initial.toMutableList()

    override suspend fun insertLimit(limit: AppLimit) {
        limits.add(limit)
    }

    override suspend fun updateLimit(limit: AppLimit) {
        limits.removeAll { it.packageName == limit.packageName }
        limits.add(limit)
    }

    override suspend fun deleteLimit(limit: AppLimit) {
        limits.removeAll { it.packageName == limit.packageName }
    }

    override suspend fun getLimit(packageName: String): AppLimit? =
        limits.firstOrNull { it.packageName == packageName }

    override fun getAllLimits(): Flow<List<AppLimit>> = MutableStateFlow(limits.toList())
}

class FakeSessionStateRepository : SessionStateRepository {
    override val activeSessionId = MutableStateFlow<Int?>(null)
    override val activeSessionStartTime = MutableStateFlow<Long?>(null)
    override val activeSessionDuration = MutableStateFlow<Long?>(null)
    override val activeSessionLabel = MutableStateFlow<String?>(null)
    override val activeSessionBlockedApps = MutableStateFlow<String?>(null)
    override val activeSessionSilenceNotifications = MutableStateFlow(false)
    override val lastUsageRolloverDay = MutableStateFlow<String?>(null)
    override val dailyScreenTimeLimitMinutes = MutableStateFlow(0)
    override val notificationSilenceScope =
        MutableStateFlow<NotificationSilenceScope?>(null)
    override val mealBreakUntil = MutableStateFlow<Long?>(null)

    override suspend fun setMealBreakUntil(until: Long?) {
        mealBreakUntil.value = until
    }

    override suspend fun setActiveSession(
        id: Int,
        startTime: Long,
        durationMs: Long,
        label: String,
        blockedApps: String,
        notificationSilenceScope: NotificationSilenceScope
    ) {
        activeSessionId.value = id
        activeSessionStartTime.value = startTime
        activeSessionDuration.value = durationMs
        activeSessionLabel.value = label
        activeSessionBlockedApps.value = blockedApps
        this.notificationSilenceScope.value = notificationSilenceScope
    }

    override suspend fun clearActiveSession() {
        activeSessionId.value = null
        activeSessionStartTime.value = null
        activeSessionDuration.value = null
        activeSessionLabel.value = null
        activeSessionBlockedApps.value = null
    }

    override suspend fun setLastUsageRolloverDay(day: String) {
        lastUsageRolloverDay.value = day
    }

    private val warned = mutableMapOf<String, MutableSet<String>>()

    override suspend fun warnedUsageKeys(day: String): Set<String> =
        warned[day]?.toSet() ?: emptySet()

    override suspend fun markUsageWarningFired(day: String, key: String) {
        warned.getOrPut(day) { mutableSetOf() }.add(key)
    }

    fun withActiveSession(
        id: Int = 1,
        startTime: Long,
        durationMs: Long,
        label: String = "Deep Work",
        blockedApps: List<String>
    ): FakeSessionStateRepository = apply {
        activeSessionId.value = id
        activeSessionStartTime.value = startTime
        activeSessionDuration.value = durationMs
        activeSessionLabel.value = label
        activeSessionBlockedApps.value = blockedApps.joinToString(",")
    }
}

class FakeFocusSessionRepository(
    initial: List<FocusSession> = emptyList()
) : FocusSessionRepository {
    val sessions = initial.toMutableList()
    private var nextId = 1

    override suspend fun insertSession(session: FocusSession): Long {
        val id = nextId++
        sessions.add(session.copy(id = id))
        return id.toLong()
    }

    override suspend fun updateSession(session: FocusSession) {
        val index = sessions.indexOfFirst { it.id == session.id }
        if (index >= 0) sessions[index] = session
    }

    override suspend fun getSessionById(id: Int): FocusSession? =
        sessions.firstOrNull { it.id == id }

    override fun getRecentSessions(limit: Int): Flow<List<FocusSession>> =
        MutableStateFlow(sessions.sortedByDescending { it.startedAt }.take(limit))

    override suspend fun getActiveSession(): FocusSession? =
        sessions.firstOrNull { it.status == SessionStatus.ACTIVE }

    override suspend fun countCancelledSince(since: Long): Int =
        sessions.count { it.status == SessionStatus.CANCELLED && (it.endedAt ?: 0L) >= since }

    override suspend fun getCancelledBetween(from: Long, to: Long): List<FocusSession> =
        sessions.filter {
            it.status == SessionStatus.CANCELLED &&
                (it.endedAt ?: Long.MIN_VALUE) in from until to
        }

    override suspend fun countCompletedBetween(from: Long, to: Long): Int =
        sessions.count {
            it.status == SessionStatus.COMPLETED &&
                (it.endedAt ?: Long.MIN_VALUE) in from until to
        }

    override suspend fun getCompletedBetween(from: Long, to: Long): List<FocusSession> =
        sessions.filter {
            it.status == SessionStatus.COMPLETED &&
                (it.endedAt ?: Long.MIN_VALUE) in from until to
        }.sortedBy { it.endedAt }
}

class FakeAlertNotifier : AlertNotifier {
    val streakWarnings = mutableListOf<Int>()
    val streakAtRisk = mutableListOf<Int>()
    val dailyStreaks = mutableListOf<Int>()
    val bedtimeWarnings = mutableListOf<Pair<String, Int>>()
    val usageWarnings = mutableListOf<Triple<String?, Int, Int>>()
    val completedSessions = mutableListOf<Pair<String, Int>>()

    override fun notifyStreakWarning(cancelledToday: Int) {
        streakWarnings.add(cancelledToday)
    }

    override fun notifyStreakAtRisk(streakDays: Int) {
        streakAtRisk.add(streakDays)
    }

    override fun notifyDailyStreak(streakDays: Int) {
        dailyStreaks.add(streakDays)
    }

    override fun notifyBedtimeWarning(ruleName: String, minutesUntil: Int) {
        bedtimeWarnings.add(ruleName to minutesUntil)
    }

    override fun notifyUsageWarning(packageName: String?, usedMinutes: Int, limitMinutes: Int) {
        usageWarnings.add(Triple(packageName, usedMinutes, limitMinutes))
    }

    override fun notifySessionComplete(label: String, durationMinutes: Int) {
        completedSessions.add(label to durationMinutes)
    }

    var notificationAccessRevocations = 0

    override fun notifyNotificationAccessRevoked() {
        notificationAccessRevocations++
    }
}

class FakePinRepository(initial: String? = null) : PinRepository {
    private val record = MutableStateFlow(initial)
    var verifyCalls = 0
        private set

    override val hasPin: Flow<Boolean> = record.map { !it.isNullOrBlank() }

    override suspend fun setPin(pin: String) {
        record.value = pin
    }

    override suspend fun verifyPin(pin: String): Boolean {
        verifyCalls++
        return record.value == pin
    }

    override suspend fun clearPin() {
        record.value = null
    }
}

class FakeOverrideStateRepository : OverrideStateRepository {
    override val requestPackage = MutableStateFlow<String?>(null)
    override val requestedAt = MutableStateFlow<Long?>(null)
    override val overridePackage = MutableStateFlow<String?>(null)
    override val overrideUntil = MutableStateFlow<Long?>(null)

    override suspend fun setRequest(packageName: String, requestedAt: Long) {
        requestPackage.value = packageName
        this.requestedAt.value = requestedAt
    }

    override suspend fun clearRequest() {
        requestPackage.value = null
        requestedAt.value = null
    }

    override suspend fun setOverride(packageName: String, until: Long) {
        overridePackage.value = packageName
        overrideUntil.value = until
    }

    override suspend fun clearOverride() {
        overridePackage.value = null
        overrideUntil.value = null
    }
}

class FakeOverrideLogRepository : OverrideLogRepository {
    val logs = mutableListOf<OverrideLog>()

    override suspend fun insertLog(log: OverrideLog) {
        logs.add(log)
    }

    override fun getRecentLogs(limit: Int): Flow<List<OverrideLog>> =
        MutableStateFlow(logs.take(limit))

    override suspend fun getLogsBetween(fromMillis: Long, toMillis: Long): List<OverrideLog> =
        logs.filter { it.overrideAt >= fromMillis && it.overrideAt < toMillis }
            .sortedByDescending { it.overrideAt }
}

/** Every alert is enabled unless a test turns it off. */class FakeNotificationPreferencesRepository : NotificationPreferencesRepository {
    val disabled = mutableSetOf<AlertKind>()

    override fun isEnabled(kind: AlertKind): Flow<Boolean> =
        MutableStateFlow(kind !in disabled)

    override suspend fun setEnabled(kind: AlertKind, enabled: Boolean) {
        if (enabled) disabled.remove(kind) else disabled.add(kind)
    }
}

class FakeStreakPreferencesRepository(
    var current: StreakRule = StreakRule.UNDER_LIMITS
) : StreakPreferencesRepository {
    override fun rule(): Flow<StreakRule> = MutableStateFlow(current)

    override suspend fun setRule(rule: StreakRule) {
        current = rule
    }
}

class FakeStreakRecordRepository(
    initial: StreakRecord = StreakRecord()
) : StreakRecordRepository {
    private val state = MutableStateFlow(initial)
    val recordedDays = mutableListOf<Pair<Int, java.time.LocalDate>>()

    override fun record(): Flow<StreakRecord> = state

    override suspend fun recordSuccessfulDay(streakEndingOn: Int, date: java.time.LocalDate) {
        recordedDays += streakEndingOn to date
        if (streakEndingOn <= 0) return
        state.value = state.value.withSuccessfulDay(streakEndingOn, date)
    }
}

class FakeScheduleRuleRepository(
    initial: List<ScheduleRule> = emptyList()
) : ScheduleRuleRepository {
    val rules = MutableStateFlow(initial)

    override suspend fun insertRule(rule: ScheduleRule) {
        val withId = if (rule.id == 0) rule.copy(id = (rules.value.maxOfOrNull { it.id } ?: 0) + 1) else rule
        rules.value = rules.value + withId
    }

    override suspend fun updateRule(rule: ScheduleRule) {
        rules.value = rules.value.map { if (it.id == rule.id) rule else it }
    }

    override suspend fun deleteRule(rule: ScheduleRule) {
        rules.value = rules.value.filterNot { it.id == rule.id }
    }

    override fun getAllRules(): Flow<List<ScheduleRule>> = rules

    override fun getActiveRules(): Flow<List<ScheduleRule>> =
        rules.map { list -> list.filter { it.isActive } }
}

class FakeAchievementRepository(
    initial: Map<AchievementId, Long> = emptyMap()
) : AchievementRepository {
    val unlocked = MutableStateFlow(initial)

    override fun getUnlocked(): Flow<Map<AchievementId, Long>> = unlocked

    override suspend fun unlock(id: AchievementId, atMillis: Long) {
        if (unlocked.value.containsKey(id)) return
        unlocked.value = unlocked.value + (id to atMillis)
    }
}

class FakeAppCategoryRepository(
    var social: Set<String> = emptySet()
) : AppCategoryRepository {
    override fun getSocialPackages(): Set<String> = social
}

class FakeScheduleSuggestionRepository(
    initial: Set<ScheduleSuggestionKind> = emptySet()
) : ScheduleSuggestionRepository {
    val dismissed = MutableStateFlow(initial)

    override fun getDismissed(): Flow<Set<ScheduleSuggestionKind>> = dismissed

    override suspend fun dismiss(kind: ScheduleSuggestionKind) {
        dismissed.value = dismissed.value + kind
    }
}

/** Defaults to "Accessibility is on": tests that need the gap opt into it. */
class FakeAccessibilityStateRepository(
    var sectionDetectionAvailable: Boolean = true
) : AccessibilityStateRepository {
    override fun isSectionDetectionAvailable(): Boolean = sectionDetectionAvailable
}

class FakeDetectionRuleRepository(
    var catalog: DetectionRuleCatalog = DetectionRuleCatalog.EMPTY
) : DetectionRuleRepository {
    override suspend fun load(): DetectionRuleCatalog = catalog
}

class FakeSectionRulePreferencesRepository(
    initial: Set<String> = emptySet()
) : SectionRulePreferencesRepository {
    val enabled = MutableStateFlow(initial)

    override fun enabledRuleIds(): Flow<Set<String>> = enabled

    override suspend fun setEnabled(ruleId: String, enabled: Boolean) {
        this.enabled.value = if (enabled) this.enabled.value + ruleId else this.enabled.value - ruleId
    }
}

/** One Instagram Reels rule, the shape the bundled catalog uses. */
fun reelsRule(packageName: String = "com.instagram.android") = DetectionRule(
    id = "$packageName:reels",
    packageName = packageName,
    appName = "Instagram",
    sectionId = "reels",
    sectionName = "Reels",
    detectionType = DetectionType.SELECTED_TAB,
    detectionValue = "$packageName:id/reels_tab",
    overlayTitle = "Reels is blocked",
    overlaySubtitle = "You set this rule in Sober."
)

/**
 * In-memory custom rules. Ids are the values themselves so a test can address a
 * row without digging it out first.
 */
class FakeCustomRuleRepository(
    initial: List<CustomRule> = emptyList()
) : CustomRuleRepository {
    val stored = MutableStateFlow(initial)

    override fun rules(): Flow<List<CustomRule>> = stored

    override suspend fun add(type: CustomRuleType, value: String): CustomRule {
        val trimmed = value.trim()
        stored.value.firstOrNull {
            it.type == type && it.value.equals(trimmed, ignoreCase = true)
        }?.let { return it }

        return CustomRule(id = trimmed, type = type, value = trimmed).also {
            stored.value = stored.value + it
        }
    }

    override suspend fun updateValue(id: String, value: String) {
        stored.value = stored.value.map {
            if (it.id == id) it.copy(value = value.trim()) else it
        }
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        stored.value = stored.value.map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
    }

    override suspend fun remove(id: String) {
        stored.value = stored.value.filterNot { it.id == id }
    }
}
