package com.sober.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sober.core.data.local.entity.UsageSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSession(session: UsageSessionEntity)

    @Query("SELECT * FROM usage_sessions WHERE packageName = :packageName AND date = :date LIMIT 1")
    suspend fun getUsageForDate(packageName: String, date: String): UsageSessionEntity?

    @Query("SELECT * FROM usage_sessions WHERE date = :date")
    fun getAllUsageForDate(date: String): Flow<List<UsageSessionEntity>>

    @Query("SELECT * FROM usage_sessions WHERE date IN (:dates)")
    suspend fun getUsageForDates(dates: List<String>): List<UsageSessionEntity>

    @Query("UPDATE usage_sessions SET usedMinutes = 0, lastUpdated = :timestamp WHERE date = :date")
    suspend fun resetDailyUsage(date: String, timestamp: Long)
}
