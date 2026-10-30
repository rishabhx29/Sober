package com.sober.feature.home

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.NotificationSilenceScope
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.EnforcementController
import com.sober.core.domain.repository.FocusSessionRepository
import com.sober.core.domain.repository.StreakRecordRepository
import com.sober.core.domain.repository.UsageStatsRepository
import com.sober.core.domain.usecase.CalculateStreakUseCase
import com.sober.core.domain.usecase.ManageFocusSessionUseCase
import com.sober.core.domain.usecase.RecordStreakUseCase
import com.sober.core.common.presets.PresetMode
import com.sober.core.common.presets.PresetResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

data class TopAppInfo(
    val packageName: String,
    val appName: String,
    val usageMinutes: Int,
    val limitMinutes: Int? = null,
    val icon: Drawable? = null
) {
    val isAtLimit: Boolean
        get() = limitMinutes != null && usageMinutes >= limitMinutes

    /** Null when the app has no limit, because a bar needs a denominator. */
    val progress: Float?
        get() = limitMinutes
            ?.takeIf { it > 0 }
            ?.let { (usageMinutes.toFloat() / it).coerceIn(0f, 1f) }
}

data class HomeUiState(
    val totalScreenTimeMinutes: Int = 0,
    val dailyLimitMinutes: Int = SoberPreferences.DEFAULT_DAILY_SCREEN_TIME_LIMIT,
    val topApps: List<TopAppInfo> = emptyList(),
    val appsUsedToday: Int = 0,
    val streakDays: Int = 0,
    /** Best streak ever, which can predate the history window. */
    val longestStreakDays: Int = 0,
    val sessionsToday: Int = 0,
    /** Explains why the Focus Mode toggle did nothing, e.g. no apps to block. */
    val focusModeNotice: String? = null,
    val isLoading: Boolean = true
)

/** The running session, already formatted for display. */
data class ActiveSessionUi(
    val id: Int,
    val label: String,
    val remainingText: String,
    /** Duration 0: no end time, so the toggle — not a countdown — stops it. */
    val isOpenEnded: Boolean = false
)

private data class SessionSnapshot(
    val id: Int?,
    val start: Long?,
    val duration: Long?,
    val label: String?
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsRepository: UsageStatsRepository,
    private val appLimitRepository: AppLimitRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val manageFocusSessionUseCase: ManageFocusSessionUseCase,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val recordStreakUseCase: RecordStreakUseCase,
    private val streakRecordRepository: StreakRecordRepository,
    private val enforcementController: EnforcementController,
    private val soberPreferences: SoberPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _activeSession = MutableStateFlow<ActiveSessionUi?>(null)
    val activeSession: StateFlow<ActiveSessionUi?> = _activeSession

    init {
        loadUsageData()
        observeActiveSession()
        observeSessionsToday()
    }

    fun loadUsageData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val usageList = usageStatsRepository.getUsageStatsForToday()
                val totalMinutes = usageList.sumOf { it.totalTimeInForeground / 60000 }.toInt()
                val appsUsed = countAppsUsed(usageList, context.packageName)
                val topApps = usageList
                    // Filter before taking, otherwise a filtered-out app eats one
                    // of the five slots and the card shows fewer than three rows.
                    .filter { it.totalTimeInForeground > 60000 }
                    .filter { it.packageName != context.packageName }
                    .sortedByDescending { it.totalTimeInForeground }
                    .take(TOP_APP_COUNT)
                    .map { usage ->
                        val limit = try {
                            appLimitRepository.getLimit(usage.packageName)
                                ?.dailyLimitMinutes
                                ?.takeIf { it > 0 }
                        } catch (e: Exception) {
                            null
                        }
                        TopAppInfo(
                            packageName = usage.packageName,
                            appName = appLabelFor(usage.packageName),
                            usageMinutes = (usage.totalTimeInForeground / 60000).toInt(),
                            limitMinutes = limit,
                            icon = appIconFor(usage.packageName)
                        )
                    }

                // The ring's denominator must be the real enforced cap, not a
                // hardcoded value: otherwise the progress the user sees and the
                // limit the engine blocks on disagree.
                val limit = soberPreferences.dailyScreenTimeLimitMinutes.first()
                val streakDays = runCatching { calculateStreakUseCase() }.getOrDefault(0)
                // Bookkeeping for yesterday, so the best-ever streak survives
                // the 60-day history window. Failure here must not hide the
                // Home screen, hence runCatching.
                runCatching { recordStreakUseCase() }
                val longestStreak = runCatching {
                    streakRecordRepository.record().first().longestStreak
                }.getOrDefault(0)

                _uiState.update {
                    it.copy(
                        totalScreenTimeMinutes = totalMinutes,
                        dailyLimitMinutes = limit,
                        topApps = topApps,
                        appsUsedToday = appsUsed,
                        streakDays = streakDays,
                        longestStreakDays = maxOf(longestStreak, streakDays),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Ticks the running session locally rather than recomposing from a stored
     * string, so Home and the notification can never disagree about how long is
     * left: both derive it from the same persisted start time and duration.
     */
    private fun observeActiveSession() {
        viewModelScope.launch {
            combine(
                soberPreferences.activeSessionId,
                soberPreferences.activeSessionStartTime,
                soberPreferences.activeSessionDuration,
                soberPreferences.activeSessionLabel
            ) { id, start, duration, label ->
                SessionSnapshot(id, start, duration, label)
            }.collectLatest { snapshot ->
                val id = snapshot.id
                val start = snapshot.start
                val duration = snapshot.duration
                if (id == null || start == null || duration == null) {
                    _activeSession.value = null
                    return@collectLatest
                }

                // Duration 0 is an open-ended session (PRD 2.6): there is no
                // countdown to tick, but the session is very much running.
                if (duration <= 0L) {
                    _activeSession.value = ActiveSessionUi(
                        id = id,
                        label = snapshot.label ?: "Focus session",
                        remainingText = "Open-ended",
                        isOpenEnded = true
                    )
                    return@collectLatest
                }

                while (true) {
                    val elapsed = (System.currentTimeMillis() - start).coerceIn(0L, duration)
                    val remaining = duration - elapsed
                    if (remaining <= 0L) {
                        _activeSession.value = null
                        return@collectLatest
                    }

                    _activeSession.value = ActiveSessionUi(
                        id = id,
                        label = snapshot.label ?: "Focus session",
                        remainingText = remainingText(remaining)
                    )
                    delay(1_000)
                }
            }
        }
    }

    private fun observeSessionsToday() {
        viewModelScope.launch {
            focusSessionRepository.getRecentSessions(SESSION_HISTORY_LIMIT).collect { sessions ->
                val startOfDay = startOfToday()
                _uiState.update {
                    it.copy(sessionsToday = sessions.count { session -> session.startedAt >= startOfDay })
                }
            }
        }
    }

    /** Ends the running session after the hold-to-cancel and regret timer. */
    fun endSession() {
        val sessionId = _activeSession.value?.id ?: return
        viewModelScope.launch { manageFocusSessionUseCase.cancelSession(sessionId) }
    }

    /**
     * Focus Mode (PRD 2.6) is an open-ended session: duration 0, no countdown,
     * stopped by the same toggle that started it. It blocks the same
     * entertainment set the Study preset does, resolved against what is
     * installed — a toggle that blocks nothing would be worse than no toggle.
     */
    fun startFocusMode() {
        viewModelScope.launch {
            val plan = withContext(Dispatchers.IO) { PresetResolver.resolve(context, PresetMode.STUDY) }
            if (plan.blockedPackages.isEmpty()) {
                _uiState.update {
                    it.copy(
                        focusModeNotice = "No distracting apps found on this device, so " +
                            "there is nothing for Focus Mode to block."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(focusModeNotice = null) }
            manageFocusSessionUseCase.startSession(
                label = FOCUS_MODE_LABEL,
                durationMinutes = 0,
                blockedApps = plan.blockedPackages,
                silenceScope = NotificationSilenceScope.SELECTED_APPS
            )
            // A session is only real if the engine that enforces it is running.
            enforcementController.ensureRunning()
        }
    }

    /**
     * Turning Focus Mode off is a cancellation, which is what every other stop
     * path in the app records. An ended open-ended session is indistinguishable
     * from a cancelled timed one, and the strict streak rule is meant to treat
     * "started then bailed" the same way regardless of duration.
     */
    fun stopFocusMode() {
        val session = _activeSession.value ?: return
        if (!session.isOpenEnded) return
        viewModelScope.launch { manageFocusSessionUseCase.cancelSession(session.id) }
    }

    private fun appLabelFor(packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    private fun appIconFor(packageName: String): Drawable? = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: Exception) {
        null
    }

    private fun remainingText(remainingMs: Long): String {
        val totalSeconds = remainingMs / 1_000L
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object {
        const val TOP_APP_COUNT = 3
        const val SESSION_HISTORY_LIMIT = 50
        const val FOCUS_MODE_LABEL = "Focus Mode"
    }
}

/**
 * The app count on Home must not come from `topApps`, which is capped at three
 * for the card. Zero-foreground rows are apps paused onscreen and Sober itself
 * is not usage the user chose.
 */
internal fun countAppsUsed(usage: List<AppUsage>, selfPackage: String): Int =
    usage.count { it.totalTimeInForeground > 0 && it.packageName != selfPackage }
