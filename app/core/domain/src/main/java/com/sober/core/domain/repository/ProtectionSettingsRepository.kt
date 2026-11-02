package com.sober.core.domain.repository

import com.sober.core.domain.model.AllowOnlyMode
import com.sober.core.domain.model.AppCategory
import com.sober.core.domain.model.BedtimeSettings
import com.sober.core.domain.model.CategoryLimit
import kotlinx.coroutines.flow.Flow

/**
 * The user's standing protection policy, apart from per-app limits and
 * schedules. All of it is read by the decision engine on every poll, so the
 * implementations are plain DataStore reads with no side effects.
 */
interface ProtectionSettingsRepository {
    /** Epoch millis until which all enforcement is paused (Pause everything). */
    val pausedUntil: Flow<Long?>

    /** Epoch millis until which protection settings are locked, or null. */
    val lockedUntil: Flow<Long?>

    val allowOnly: Flow<AllowOnlyMode>

    val bedtime: Flow<BedtimeSettings>

    /**
     * True while Sober itself turned Do Not Disturb on for bedtime. It is what
     * lets the engine restore the user's own filter afterwards instead of
     * guessing.
     */
    val bedtimeSilenced: Flow<Boolean>

    val categoryLimits: Flow<List<CategoryLimit>>

    suspend fun setPausedUntil(until: Long?)

    suspend fun setLockedUntil(until: Long?)

    suspend fun setAllowOnly(mode: AllowOnlyMode)

    suspend fun setBedtime(settings: BedtimeSettings)

    suspend fun setBedtimeSilenced(silenced: Boolean)

    /** Writes [limit], or removes [category]'s budget when [limit] is null. */
    suspend fun setCategoryLimit(category: AppCategory, limit: CategoryLimit?)
}
