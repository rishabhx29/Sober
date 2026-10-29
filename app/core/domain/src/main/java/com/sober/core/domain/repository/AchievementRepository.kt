package com.sober.core.domain.repository

import com.sober.core.domain.model.AchievementId
import kotlinx.coroutines.flow.Flow

/**
 * Permanent achievement unlocks, keyed by the moment they were first earned.
 *
 * Unlocks are stored rather than derived on every read because the conditions
 * are streaks and single days: deriving them means an achievement would
 * silently disappear the moment the user has a bad day.
 */
interface AchievementRepository {
    fun getUnlocked(): Flow<Map<AchievementId, Long>>

    /** Records the first unlock of [id]. Later calls for the same id are no-ops. */
    suspend fun unlock(id: AchievementId, atMillis: Long)
}
