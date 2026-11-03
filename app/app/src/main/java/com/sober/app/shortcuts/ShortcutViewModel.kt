package com.sober.app.shortcuts

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.common.presets.PresetResolver
import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.usecase.ManageFocusSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Starts a preset session when the app is opened by a shortcut or an
 * automation intent, then asks the UI to show the Focus tab.
 *
 * Refusals are deliberate rather than errors:
 *  - before onboarding, the app has no access, so a session would look active
 *    while enforcing nothing;
 *  - while a session already runs, starting another would overwrite the
 *    DataStore pointer and leave the first row ACTIVE in history forever.
 *
 * In both cases the app still opens, which is what a launcher shortcut should
 * do; it simply does not start a second thing.
 */
@HiltViewModel
class ShortcutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soberPreferences: SoberPreferences,
    private val sessionStateRepository: SessionStateRepository,
    private val manageFocusSessionUseCase: ManageFocusSessionUseCase
) : ViewModel() {

    private val _openFocus = MutableStateFlow(false)
    val openFocus: StateFlow<Boolean> = _openFocus

    /** Guards against a double-delivered intent racing two starts. */
    private var handling = false

    fun handleIntent(intent: Intent?) {
        val preset = FocusShortcut.presetFor(
            action = intent?.action,
            rawPreset = intent?.getStringExtra(FocusShortcut.EXTRA_PRESET)
        ) ?: return
        if (handling) return

        handling = true
        viewModelScope.launch {
            try {
                if (!soberPreferences.isOnboardingComplete.first()) return@launch
                if (sessionStateRepository.activeSessionId.first() != null) {
                    _openFocus.value = true
                    return@launch
                }

                // Resolving a preset walks PackageManager for every installed
                // app, so it is not main-thread work.
                val plan = withContext(Dispatchers.IO) {
                    PresetResolver.resolve(context, preset)
                }
                manageFocusSessionUseCase.startSession(
                    label = plan.mode.label,
                    durationMinutes = plan.durationMinutes,
                    blockedApps = plan.blockedPackages
                )
                _openFocus.value = true
            } finally {
                handling = false
            }
        }
    }

    fun consumeOpenFocus() {
        _openFocus.value = false
    }
}
