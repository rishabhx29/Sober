package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.model.AchievementId
import com.sober.core.domain.repository.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Stores unlocks in the shared DataStore as `ID:epochMillis;ID:epochMillis`.
 *
 * The format is flat on purpose: achievements are a small, append-mostly set,
 * and a flat string survives DataStore migrations without a schema.
 */
class AchievementRepositoryImpl @Inject constructor(
    private val preferences: SoberPreferences
) : AchievementRepository {

    override fun getUnlocked(): Flow<Map<AchievementId, Long>> =
        preferences.achievementsRaw.map { decode(it) }

    override suspend fun unlock(id: AchievementId, atMillis: Long) {
        val current = decode(preferences.achievementsRaw.first())
        if (current.containsKey(id)) return
        preferences.setAchievementsRaw(encode(current + (id to atMillis)))
    }

    /** Unknown ids (a downgrade) are dropped rather than failing the whole map. */
    private fun decode(raw: String?): Map<AchievementId, Long> = raw
        ?.split(';')
        ?.mapNotNull { entry ->
            val parts = entry.split(':')
            if (parts.size != 2) return@mapNotNull null
            val id = runCatching { AchievementId.valueOf(parts[0]) }.getOrNull()
                ?: return@mapNotNull null
            val at = parts[1].toLongOrNull() ?: return@mapNotNull null
            id to at
        }
        ?.toMap()
        .orEmpty()

    private fun encode(unlocked: Map<AchievementId, Long>): String =
        unlocked.entries
            .sortedBy { it.key.ordinal }
            .joinToString(";") { (id, at) -> "${id.name}:$at" }
}
