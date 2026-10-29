package com.sober.feature.settings.onboarding

import androidx.lifecycle.ViewModel
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.data.local.datastore.SoberPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val soberPreferences: SoberPreferences,
    private val sessionStateRepository: SessionStateRepository
) : ViewModel() {

    /** Drives the navigation start destination: onboarding only runs once. */
    val isOnboardingComplete: Flow<Boolean> = soberPreferences.isOnboardingComplete

    /**
     * Persists completion. Deliberately independent of the service start, so a
     * failure to start the engine cannot trap the user in onboarding forever.
     */
    suspend fun markOnboardingComplete() {
        soberPreferences.setOnboardingComplete(true)
        // Seed the rollover day so the very first cycle does not treat a missing
        // marker as a day change and zero fresh usage.
        if (sessionStateRepository.lastUsageRolloverDay.first() == null) {
            val today = java.text.SimpleDateFormat(
                "yyyy-MM-dd",
                java.util.Locale.US
            ).format(java.util.Date())
            sessionStateRepository.setLastUsageRolloverDay(today)
        }
    }
}