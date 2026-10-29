package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.repository.ThemeMode
import com.sober.core.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ThemePreferencesRepositoryImpl @Inject constructor(
    private val preferences: SoberPreferences
) : ThemePreferencesRepository {

    override fun mode(): Flow<ThemeMode> = preferences.themeMode

    override suspend fun setMode(mode: ThemeMode) {
        preferences.setThemeMode(mode)
    }
}
