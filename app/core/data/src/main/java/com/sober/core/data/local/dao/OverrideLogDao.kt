package com.sober.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sober.core.data.local.entity.OverrideLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OverrideLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: OverrideLogEntity)

    @Query("SELECT * FROM override_logs ORDER BY overrideAt DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<OverrideLogEntity>>

    @Query("SELECT * FROM override_logs WHERE overrideAt >= :from AND overrideAt < :to ORDER BY overrideAt DESC")
    suspend fun getLogsBetween(from: Long, to: Long): List<OverrideLogEntity>
}
