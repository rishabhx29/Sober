package com.sober.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sober.core.domain.model.UsageSession

/**
 * One row per app per day. The unique index is what makes that true: the
 * enforcement poll and a UI-triggered sync can reach the same upsert at once,
 * and without it both see "no row" and insert.
 */
@Entity(
    tableName = "usage_sessions",
    indices = [
        Index(value = ["packageName", "date"], unique = true),
        Index(value = ["date"])
    ]
)
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val date: String, // YYYY-MM-DD
    val usedMinutes: Int,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toDomainModel() = UsageSession(
        id = id,
        packageName = packageName,
        date = date,
        usedMinutes = usedMinutes,
        lastUpdated = lastUpdated
    )
}

fun UsageSession.toEntity() = UsageSessionEntity(
    id = id,
    packageName = packageName,
    date = date,
    usedMinutes = usedMinutes,
    lastUpdated = lastUpdated
)
