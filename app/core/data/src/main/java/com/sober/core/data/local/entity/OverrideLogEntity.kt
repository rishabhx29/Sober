package com.sober.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sober.core.domain.model.OverrideLog

@Entity(tableName = "override_logs")
data class OverrideLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val overrideAt: Long = System.currentTimeMillis(),
    val durationGrantedMinutes: Int,
    val sessionId: Int? = null // if it was during a focus session
) {
    fun toDomainModel() = OverrideLog(
        id = id,
        packageName = packageName,
        overrideAt = overrideAt,
        durationGrantedMinutes = durationGrantedMinutes,
        sessionId = sessionId
    )
}

fun OverrideLog.toEntity() = OverrideLogEntity(
    id = id,
    packageName = packageName,
    overrideAt = overrideAt,
    durationGrantedMinutes = durationGrantedMinutes,
    sessionId = sessionId
)
