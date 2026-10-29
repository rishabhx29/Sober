package com.sober.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.domain.repository.ThemeMode
import com.sober.core.domain.repository.ThemePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Supplies the chosen theme to the root composable, before any screen loads. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    repository: ThemePreferencesRepository
) : ViewModel() {

    val mode: StateFlow<ThemeMode> = repository.mode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM
    )
}
