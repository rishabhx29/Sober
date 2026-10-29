package com.sober.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Light, dark, or whatever the system is doing.
 *
 * [SYSTEM] is the default because it is the only choice that keeps Sober in
 * step with the phone at sunset without the user having to think about it.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

interface ThemePreferencesRepository {
    fun mode(): Flow<ThemeMode>

    suspend fun setMode(mode: ThemeMode)
}
