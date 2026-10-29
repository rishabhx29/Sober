package com.sober.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sober.core.domain.model.FocusSession
import com.sober.core.domain.model.SessionStatus

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val durationMinutes: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val status: SessionStatus,
    val blockedApps: List<String> = emptyList(),
    val cancelledAtMinute: Int? = null
) {
    fun toDomainModel() = FocusSession(
        id = id,
        label = label,
        durationMinutes = durationMinutes,
        startedAt = startedAt,
        endedAt = endedAt,
        status = status,
        blockedApps = blockedApps,
        cancelledAtMinute = cancelledAtMinute
    )
}

fun FocusSession.toEntity() = FocusSessionEntity(
    id = id,
    label = label,
    durationMinutes = durationMinutes,
    startedAt = startedAt,
    endedAt = endedAt,
    status = status,
    blockedApps = blockedApps,
    cancelledAtMinute = cancelledAtMinute
)
