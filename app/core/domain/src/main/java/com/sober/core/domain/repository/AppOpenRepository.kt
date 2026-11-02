package com.sober.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * How many times each app was opened today.
 *
 * An "open" is one transition of the app to the foreground, not one minute of
 * use: the number answers "how often did I reach for this?", which screen time
 * cannot. Stored in Room rather than in memory because a count that resets when
 * Android kills the service would not limit anything.
 */
interface AppOpenRepository {
    suspend fun recordOpen(packageName: String, at: Long = System.currentTimeMillis())

    suspend fun countToday(packageName: String, now: Long = System.currentTimeMillis()): Int

    /** Every package opened today, mapped to its count. */
    fun observeTodayCounts(now: Long = System.currentTimeMillis()): Flow<Map<String, Int>>

    /** Deletes rows older than [cutoff] so the table stays bounded. */
    suspend fun deleteBefore(cutoff: Long)
}
