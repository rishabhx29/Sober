package com.sober.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.data.local.datastore.SoberPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-level startup state: whether onboarding has completed, which decides the
 * navigation start destination.
 */
@HiltViewModel
class SoberStartViewModel @Inject constructor(
    private val soberPreferences: SoberPreferences
) : ViewModel() {

    val isOnboardingComplete: Flow<Boolean> = soberPreferences.isOnboardingComplete

    fun setOnboardingComplete(value: Boolean) {
        viewModelScope.launch {
            soberPreferences.setOnboardingComplete(value)
        }
    }
}