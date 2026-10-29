package com.sober.feature.settings

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.repository.PinRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.repository.AlertKind
import com.sober.core.domain.repository.NotificationPreferencesRepository
import com.sober.core.domain.repository.StreakPreferencesRepository
import com.sober.core.domain.repository.StreakRule
import com.sober.core.domain.repository.ThemeMode
import com.sober.core.domain.repository.ThemePreferencesRepository
import com.sober.core.domain.usecase.ExportUsageCsvUseCase
import com.sober.core.domain.usecase.SetPinUseCase
import com.sober.core.domain.usecase.VerifyPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class GateState(
    val hasPin: Boolean,
    val sessionRunning: Boolean
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soberPreferences: SoberPreferences,
    private val sessionStateRepository: SessionStateRepository,
    pinRepository: PinRepository,
    private val notificationPreferences: NotificationPreferencesRepository,
    private val streakPreferences: StreakPreferencesRepository,
    private val themePreferences: ThemePreferencesRepository,
    private val setPinUseCase: SetPinUseCase,
    private val verifyPinUseCase: VerifyPinUseCase,
    private val exportUsageCsvUseCase: ExportUsageCsvUseCase
) : ViewModel() {

    /** The overall daily screen-time cap, enforced by the core service. */
    val dailyScreenTimeLimitMinutes: StateFlow<Int> =
        soberPreferences.dailyScreenTimeLimitMinutes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SoberPreferences.DEFAULT_DAILY_SCREEN_TIME_LIMIT
        )

    /**
     * Everything the lock gate needs, or null while the stored state is still
     * loading.
     *
     * A pair of independent StateFlows would start at `false`/`false`, which
     * renders the settings body for a frame before the real values arrive — long
     * enough to tap a limit button while a session is supposed to be locked.
     * A single nullable state makes "not known yet" explicit and keeps the body
     * off screen until it is.
     */
    val gate: StateFlow<GateState?> = combine(
        pinRepository.hasPin,
        sessionStateRepository.activeSessionId
    ) { hasPin, activeSessionId ->
        GateState(
            hasPin = hasPin,
            sessionRunning = activeSessionId != null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun setDailyScreenTimeLimit(minutes: Int) {
        viewModelScope.launch {
            soberPreferences.setDailyScreenTimeLimitMinutes(minutes)
        }
    }

    /**
     * Which optional alerts may be posted. Every kind starts enabled, matching
     * the repository's unset default. Built from [AlertKind.entries] so a new
     * alert kind cannot silently go missing from this map.
     */
    val alerts: StateFlow<Map<AlertKind, Boolean>> = combine(
        AlertKind.entries.map { notificationPreferences.isEnabled(it) }
    ) { values ->
        AlertKind.entries.zip(values.toList()).toMap()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AlertKind.entries.associateWith { true }
    )

    fun setAlertEnabled(kind: AlertKind, enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setEnabled(kind, enabled)
        }
    }

    val streakRule: StateFlow<StreakRule> = streakPreferences.rule().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StreakRule.UNDER_LIMITS
    )

    fun setStreakRule(rule: StreakRule) {
        viewModelScope.launch {
            streakPreferences.setRule(rule)
        }
    }

    val themeMode: StateFlow<ThemeMode> = themePreferences.mode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setMode(mode)
        }
    }

    /**
     * Sets a PIN, or replaces the current one when [currentPin] is supplied.
     * The current PIN is re-verified here rather than in the composable so the
     * check cannot be bypassed by another caller of the same ViewModel.
     */
    fun savePin(currentPin: String?, newPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (currentPin != null && !verifyPinUseCase(currentPin)) {
                onResult(false)
                return@launch
            }
            onResult(setPinUseCase(newPin))
        }
    }

    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(verifyPinUseCase(pin))
        }
    }

    /**
     * Writes the CSV to cache and hands back a shareable URI.
     *
     * The file lives in `cacheDir/exports` rather than files/ or external
     * storage: it is a hand-off to another app, not something Sober should keep
     * or back up, and Android is free to reclaim it. Previous exports are
     * deleted first so repeated taps cannot pile up copies of the user's usage
     * data in a readable location.
     */
    fun exportUsageCsv(onResult: (Uri?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val csv = exportUsageCsvUseCase()
                val file = withContext(Dispatchers.IO) { writeExport(csv) }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                onResult(uri, null)
            } catch (e: Exception) {
                // File IO and FileProvider config can both fail on a real
                // device; the screen needs a message, not a crash.
                onResult(null, "Couldn't create the export file.")
            }
        }
    }

    private fun writeExport(csv: String): File {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return File(dir, "sober-usage-$day.csv").apply { writeText(csv) }
    }

    companion object {
        const val MIN_PIN_LENGTH = PinRepository.MIN_LENGTH
        const val MAX_PIN_LENGTH = PinRepository.MAX_LENGTH
        const val EXPORT_DIR = "exports"
    }
}
