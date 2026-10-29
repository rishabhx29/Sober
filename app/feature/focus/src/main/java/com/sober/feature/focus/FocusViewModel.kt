package com.sober.feature.focus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.common.presets.PresetMode
import com.sober.core.common.presets.PresetResolver
import com.sober.core.domain.model.FocusSession
import com.sober.core.domain.model.NotificationSilenceScope
import com.sober.core.domain.model.SessionStatus
import com.sober.core.domain.repository.EnforcementController
import com.sober.core.domain.repository.FocusSessionRepository
import com.sober.core.domain.usecase.CalculateStreakUseCase
import com.sober.core.domain.usecase.ManageFocusSessionUseCase
import com.sober.core.domain.usecase.ManageMealBreakUseCase
import com.sober.core.domain.usecase.RecordStreakUseCase
import com.sober.core.data.local.datastore.SoberPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FocusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val focusSessionRepository: FocusSessionRepository,
    private val manageFocusSessionUseCase: ManageFocusSessionUseCase,
    private val manageMealBreakUseCase: ManageMealBreakUseCase,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val recordStreakUseCase: RecordStreakUseCase,
    private val enforcementController: EnforcementController,
    private val soberPreferences: SoberPreferences
) : ViewModel() {

    val activeSessionId: StateFlow<Int?> = soberPreferences.activeSessionId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val activeSessionStartTime: StateFlow<Long?> = soberPreferences.activeSessionStartTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val activeSessionDuration: StateFlow<Long?> = soberPreferences.activeSessionDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Silence scope of the running session. The Focus screen needs it to know
     * whether notification blocking is even expected — a missing listener
     * permission is only a problem when the session promised to silence.
     */
    val activeSilenceScope: StateFlow<NotificationSilenceScope?> =
        soberPreferences.notificationSilenceScope
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentSessions: StateFlow<List<FocusSession>> = focusSessionRepository.getRecentSessions(5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Countdown timer
    private val _remainingTimeText = MutableStateFlow("--:--")
    val remainingTimeText: StateFlow<String> = _remainingTimeText

    data class AppInfo(
        val name: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable?
    )

    /**
     * What the celebration card shows after a session runs its full length.
     * Blocked apps come from the session row itself, so the count survives the
     * active-session metadata being cleared on completion.
     */
    data class CelebrationUi(
        val label: String,
        val durationMinutes: Int,
        val blockedApps: List<String>,
        val streakDays: Int
    )

    private val _celebration = MutableStateFlow<CelebrationUi?>(null)
    val celebration: StateFlow<CelebrationUi?> = _celebration

    /** Set when a preset resolves to nothing on this device; cleared otherwise. */
    private val _presetNotice = MutableStateFlow<String?>(null)
    val presetNotice: StateFlow<String?> = _presetNotice

    private val mealBreakUntil: StateFlow<Long?> = manageMealBreakUseCase.until
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Remaining meal-break time as mm:ss, or null when no break is running. */
    private val _mealBreakRemainingText = MutableStateFlow<String?>(null)
    val mealBreakRemainingText: StateFlow<String?> = _mealBreakRemainingText

    private var lastClearedBreak: Long? = null

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps

    fun loadApps(pm: android.content.pm.PackageManager) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                    .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                    .map { appInfo ->
                        AppInfo(
                            name = pm.getApplicationLabel(appInfo).toString(),
                            packageName = appInfo.packageName,
                            icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
                        )
                    }
                    .sortedBy { it.name }
                _installedApps.value = apps
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    init {
        // Recovery path: a session can outlive the process (finished session
        // while the app was closed, engine killed by the system). Re-asserting
        // the engine here means opening the Focus tab is enough to make an
        // already-active session enforced again.
        //
        // The same collector watches for the active id disappearing, which is
        // how a finished session is detected regardless of whether the service
        // ticker or this screen's ticker completed it.
        viewModelScope.launch {
            var previousId: Int? = null
            activeSessionId.collect { id ->
                val finishedId = if (id == null) previousId else null
                if (id != null) {
                    _celebration.value = null
                    enforcementController.ensureRunning()
                }
                if (finishedId != null) showCelebrationFor(finishedId)
                previousId = id
            }
        }

        // Start a coroutine that ticks every second to update the countdown
        viewModelScope.launch {
            while (true) {
                val start = activeSessionStartTime.value
                val duration = activeSessionDuration.value
                val sessionId = activeSessionId.value

                if (start != null && duration != null && sessionId != null) {
                    if (duration <= 0L) {
                        // Open-ended (PRD 2.6): no window, so no completion can
                        // be derived from elapsed time.
                        _remainingTimeText.value = "∞"
                    } else {
                        val elapsed = System.currentTimeMillis() - start
                        // Clamped the same way the service does: a backwards clock
                        // change must not extend a session past its window.
                        val remaining = duration - elapsed.coerceIn(0L, duration)

                        if (remaining <= 0) {
                            // Session completed!
                            manageFocusSessionUseCase.completeSession(sessionId)
                            _remainingTimeText.value = "00:00"
                        } else {
                            val totalSeconds = remaining / 1000
                            val hours = totalSeconds / 3600
                            val minutes = (totalSeconds % 3600) / 60
                            val seconds = totalSeconds % 60

                            _remainingTimeText.value = if (hours > 0) {
                                String.format("%d:%02d:%02d", hours, minutes, seconds)
                            } else {
                                String.format("%02d:%02d", minutes, seconds)
                            }
                        }
                    }
                } else {
                    _remainingTimeText.value = "--:--"
                }

                updateMealBreakCountdown()
                delay(1000)
            }
        }
    }

    private suspend fun updateMealBreakCountdown() {
        val until = mealBreakUntil.value
        if (until == null) {
            _mealBreakRemainingText.value = null
            return
        }

        val remaining = until - System.currentTimeMillis()
        _mealBreakRemainingText.value = if (remaining > 0) formatClock(remaining) else null

        // Clear expired windows once so a backwards clock change cannot
        // resurrect an old break; the guard keeps the write to one per window.
        if (remaining <= 0 && lastClearedBreak != until) {
            lastClearedBreak = until
            manageMealBreakUseCase.endNow()
        }
    }

    private fun formatClock(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun startSession(
        label: String,
        durationMinutes: Int,
        blockedApps: List<String>,
        silenceScope: com.sober.core.domain.model.NotificationSilenceScope
    ) {
        viewModelScope.launch {
            manageFocusSessionUseCase.startSession(label, durationMinutes, blockedApps, silenceScope)
            // A session is only real if the engine that enforces it is running.
            enforcementController.ensureRunning()
        }
    }

    fun cancelSession(sessionId: Int) {
        viewModelScope.launch {
            manageFocusSessionUseCase.cancelSession(sessionId)
        }
    }

    /**
     * One tap starts a preset. The blocked list is resolved from what is
     * actually installed, so the session reflects the user's device rather
     * than a hardcoded package list that silently matches nothing.
     */
    fun startPreset(mode: PresetMode) {
        viewModelScope.launch {
            val plan = withContext(Dispatchers.IO) { PresetResolver.resolve(context, mode) }
            if (plan.blockedPackages.isEmpty()) {
                _presetNotice.value =
                    "No ${mode.label.lowercase()} apps found on this device — pick apps manually."
                return@launch
            }
            _presetNotice.value = null
            manageFocusSessionUseCase.startSession(
                label = "${mode.label} Mode",
                durationMinutes = plan.durationMinutes,
                blockedApps = plan.blockedPackages,
                silenceScope = silenceScopeFor(mode)
            )
            enforcementController.ensureRunning()
        }
    }

    fun startMealBreak() {
        viewModelScope.launch { manageMealBreakUseCase.start() }
    }

    fun endMealBreak() {
        viewModelScope.launch { manageMealBreakUseCase.endNow() }
    }

    private fun silenceScopeFor(mode: PresetMode): NotificationSilenceScope = when (mode) {
        // Sleep must still let calls through, and a gym session is the one
        // place people expect music notifications silenced entirely.
        PresetMode.SLEEP -> NotificationSilenceScope.ALL_EXCEPT_CALLS
        PresetMode.STUDY -> NotificationSilenceScope.SELECTED_APPS
        PresetMode.WORK, PresetMode.GYM -> NotificationSilenceScope.ALL
    }

    /**
     * The active id is cleared for cancellations too, so the finished session
     * must be re-read to tell the two cases apart. The time window keeps an
     * old completion from popping up when the app is reopened long after.
     */
    private suspend fun showCelebrationFor(sessionId: Int) {
        val session = focusSessionRepository.getSessionById(sessionId) ?: return
        if (session.status != SessionStatus.COMPLETED) return

        val endedAt = session.endedAt ?: return
        if (System.currentTimeMillis() - endedAt > CELEBRATION_WINDOW_MS) return

        val streakDays = runCatching { calculateStreakUseCase() }.getOrDefault(0)
        // A finished session is also a natural checkpoint for the streak
        // record, even when the user never returns to Home.
        runCatching { recordStreakUseCase() }
        _celebration.value = CelebrationUi(
            label = session.label,
            durationMinutes = session.durationMinutes,
            blockedApps = session.blockedApps,
            streakDays = streakDays
        )
    }

    fun dismissCelebration() {
        _celebration.value = null
    }

    /** "Go again": same label, duration and block list, no re-entry required. */
    fun repeatSession() {
        val session = _celebration.value ?: return
        viewModelScope.launch {
            val silenceScope = soberPreferences.notificationSilenceScope.first()
                ?: NotificationSilenceScope.SELECTED_APPS
            _celebration.value = null
            manageFocusSessionUseCase.startSession(
                label = session.label,
                durationMinutes = session.durationMinutes,
                blockedApps = session.blockedApps,
                silenceScope = silenceScope
            )
            enforcementController.ensureRunning()
        }
    }

    private companion object {
        /** Completions older than this are history, not news. */
        const val CELEBRATION_WINDOW_MS = 10 * 60_000L
    }
}
