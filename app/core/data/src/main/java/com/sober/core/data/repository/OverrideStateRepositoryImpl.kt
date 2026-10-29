package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.repository.OverrideStateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverrideStateRepositoryImpl @Inject constructor(
    private val soberPreferences: SoberPreferences
) : OverrideStateRepository {

    override val requestPackage: Flow<String?> = soberPreferences.overrideRequestPackage
    override val requestedAt: Flow<Long?> = soberPreferences.overrideRequestedAt
    override val overridePackage: Flow<String?> = soberPreferences.overridePackage
    override val overrideUntil: Flow<Long?> = soberPreferences.overrideUntil

    override suspend fun setRequest(packageName: String, requestedAt: Long) {
        soberPreferences.setOverrideRequest(packageName, requestedAt)
    }

    override suspend fun clearRequest() {
        soberPreferences.clearOverrideRequest()
    }

    override suspend fun setOverride(packageName: String, until: Long) {
        soberPreferences.setOverride(packageName, until)
    }

    override suspend fun clearOverride() {
        soberPreferences.clearOverride()
    }
}
