package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.repository.AlertKind
import com.sober.core.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NotificationPreferencesRepositoryImpl @Inject constructor(
    private val preferences: SoberPreferences
) : NotificationPreferencesRepository {

    override fun isEnabled(kind: AlertKind): Flow<Boolean> = preferences.alertEnabled(kind)

    override suspend fun setEnabled(kind: AlertKind, enabled: Boolean) {
        preferences.setAlertEnabled(kind, enabled)
    }
}
