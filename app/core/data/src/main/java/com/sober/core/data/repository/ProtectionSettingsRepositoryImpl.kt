package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.model.AllowOnlyMode
import com.sober.core.domain.model.AppCategory
import com.sober.core.domain.model.BedtimeSettings
import com.sober.core.domain.model.CategoryLimit
import com.sober.core.domain.repository.ProtectionSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtectionSettingsRepositoryImpl @Inject constructor(
    private val soberPreferences: SoberPreferences
) : ProtectionSettingsRepository {

    override val pausedUntil: Flow<Long?> = soberPreferences.pausedUntil
    override val lockedUntil: Flow<Long?> = soberPreferences.lockedUntil
    override val allowOnly: Flow<AllowOnlyMode> = soberPreferences.allowOnly
    override val bedtime: Flow<BedtimeSettings> = soberPreferences.bedtime
    override val bedtimeSilenced: Flow<Boolean> = soberPreferences.bedtimeSilenced
    override val categoryLimits: Flow<List<CategoryLimit>> = soberPreferences.categoryLimits

    override suspend fun setPausedUntil(until: Long?) =
        soberPreferences.setPausedUntil(until)

    override suspend fun setLockedUntil(until: Long?) =
        soberPreferences.setLockedUntil(until)

    override suspend fun setAllowOnly(mode: AllowOnlyMode) =
        soberPreferences.setAllowOnly(mode)

    override suspend fun setBedtime(settings: BedtimeSettings) =
        soberPreferences.setBedtime(settings)

    override suspend fun setBedtimeSilenced(silenced: Boolean) =
        soberPreferences.setBedtimeSilenced(silenced)

    override suspend fun setCategoryLimit(category: AppCategory, limit: CategoryLimit?) =
        soberPreferences.setCategoryLimit(category, limit)
}
