package com.sober.core.domain.model

data class FocusSession(
    val id: Int = 0,
    val label: String,
    val durationMinutes: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val status: SessionStatus,
    val blockedApps: List<String> = emptyList(),
    val cancelledAtMinute: Int? = null
)

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}
