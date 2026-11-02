package com.sober.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sober.core.data.local.entity.AppOpenEntity
import kotlinx.coroutines.flow.Flow

data class PackageOpenCount(val packageName: String, val openCount: Int)

@Dao
interface AppOpenDao {
    @Insert
    suspend fun insert(open: AppOpenEntity)

    @Query("SELECT COUNT(*) FROM app_opens WHERE packageName = :packageName AND openedAt >= :since")
    suspend fun countSince(packageName: String, since: Long): Int

    @Query(
        "SELECT packageName, COUNT(*) AS openCount FROM app_opens " +
            "WHERE openedAt >= :since GROUP BY packageName"
    )
    fun countsSince(since: Long): Flow<List<PackageOpenCount>>

    @Query("DELETE FROM app_opens WHERE openedAt < :cutoff")
    suspend fun deleteBefore(cutoff: Long)
}
