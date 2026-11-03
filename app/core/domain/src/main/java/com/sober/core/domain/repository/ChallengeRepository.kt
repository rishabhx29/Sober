package com.sober.core.domain.repository

import com.sober.core.domain.model.ActiveChallenge
import kotlinx.coroutines.flow.Flow

/**
 * The one active challenge and every challenge that was finished.
 *
 * Only the opt-in is stored; progress is derived from usage history on every
 * read, because a stored count would drift the moment the history is corrected.
 * Finished challenges are kept so the achievement and the completed count
 * survive reinstalling nothing but the app's own data.
 */
interface ChallengeRepository {
    fun getActive(): Flow<ActiveChallenge?>

    fun getCompleted(): Flow<List<ActiveChallenge>>

    suspend fun start(challenge: ActiveChallenge)

    suspend fun clearActive()

    /** Moves [challenge] to the completed set and clears the active slot. */
    suspend fun complete(challenge: ActiveChallenge)
}
