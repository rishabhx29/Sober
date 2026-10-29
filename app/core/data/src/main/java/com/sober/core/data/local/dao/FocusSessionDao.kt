package com.sober.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sober.core.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Int): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSession(): FocusSessionEntity?

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'CANCELLED' AND endedAt >= :since")
    suspend fun countCancelledSince(since: Long): Int

    @Query(
        "SELECT * FROM focus_sessions WHERE status = 'CANCELLED' " +
            "AND endedAt >= :from AND endedAt < :to ORDER BY endedAt ASC"
    )
    suspend fun getCancelledBetween(from: Long, to: Long): List<FocusSessionEntity>

    @Query(
        "SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED' " +
            "AND endedAt >= :from AND endedAt < :to"
    )
    suspend fun countCompletedBetween(from: Long, to: Long): Int

    @Query(
        "SELECT * FROM focus_sessions WHERE status = 'COMPLETED' " +
            "AND endedAt >= :from AND endedAt < :to ORDER BY endedAt ASC"
    )
    suspend fun getCompletedBetween(from: Long, to: Long): List<FocusSessionEntity>
}
