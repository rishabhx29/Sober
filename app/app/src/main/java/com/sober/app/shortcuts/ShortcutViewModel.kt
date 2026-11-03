package com.sober.app.shortcuts

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.common.presets.PresetMode
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
 * Starts a preset session when the app is opened by a shortcut, an automation
 * intent or an NFC tag, then asks the UI to show the Focus tab.
 *
 * An NFC tag only ever starts: ending or lifting a block stays behind the
 * in-app hold, so a sticker someone else can tap cannot shorten a session.
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
        ) ?: tagPreset(intent) ?: return
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

    /**
     * Only a real NDEF dispatch counts. Any exported-activity intent can carry
     * a `data` URI, so parsing the payload regardless of the action would let
     * an unrelated intent pretend to be a tag.
     */
    private fun tagPreset(intent: Intent?): PresetMode? {
        if (intent?.action != NfcAdapter.ACTION_NDEF_DISCOVERED) return null
        return FocusTag.presetFor(intent.dataString)
    }

    fun consumeOpenFocus() {
        _openFocus.value = false
    }
}
