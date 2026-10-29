package com.sober.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sober.core.domain.model.UsageSession

@Entity(tableName = "usage_sessions")
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
