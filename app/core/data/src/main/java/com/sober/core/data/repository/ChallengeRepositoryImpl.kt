package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.model.ActiveChallenge
import com.sober.core.domain.model.ChallengeId
import com.sober.core.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Stores challenges as `ID|yyyy-MM-dd`, one active and a set of finished ones.
 *
 * Two flat records rather than a database table: there are at most a handful of
 * challenges, they are edited a few times a year, and the format survives a
 * DataStore migration without a schema. A record that cannot be parsed is
 * dropped — a downgrade that removed an id must not take the whole feature
 * down with it.
 */
class ChallengeRepositoryImpl @Inject constructor(
    private val preferences: SoberPreferences
) : ChallengeRepository {

    override fun getActive(): Flow<ActiveChallenge?> =
        preferences.activeChallengeRaw.map { it?.let(ChallengeCodec::decode) }

    override fun getCompleted(): Flow<List<ActiveChallenge>> =
        preferences.completedChallengesRaw.map { records ->
            records.mapNotNull(ChallengeCodec::decode).sortedBy { it.startedOn }
        }

    override suspend fun start(challenge: ActiveChallenge) {
        preferences.setActiveChallengeRaw(ChallengeCodec.encode(challenge))
    }

    override suspend fun clearActive() {
        preferences.setActiveChallengeRaw(null)
    }

    override suspend fun complete(challenge: ActiveChallenge) {
        preferences.recordCompletedChallengeRaw(ChallengeCodec.encode(challenge))
    }
}

/**
 * `ID|yyyy-MM-dd`, flat enough for a DataStore string set.
 *
 * An unparseable record decodes to null rather than throwing: a downgrade that
 * removed an id must drop that record, not take the whole screen down.
 */
internal object ChallengeCodec {
    fun encode(challenge: ActiveChallenge): String =
        "${challenge.id.name}|${challenge.startedOn}"

    fun decode(raw: String): ActiveChallenge? {
        val separator = raw.indexOf('|')
        if (separator <= 0 || separator == raw.lastIndex) return null

        val id = runCatching { ChallengeId.valueOf(raw.substring(0, separator)) }.getOrNull()
            ?: return null
        val startedOn = runCatching { LocalDate.parse(raw.substring(separator + 1)) }.getOrNull()
            ?: return null
        return ActiveChallenge(id = id, startedOn = startedOn)
    }
}
