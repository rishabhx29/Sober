package com.sober.core.domain.model

data class OverrideLog(
    val id: Int = 0,
    val packageName: String,
    val overrideAt: Long = System.currentTimeMillis(),
    val durationGrantedMinutes: Int,
    val sessionId: Int? = null // if it was during a focus session
)
