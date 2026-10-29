package com.sober.core.domain.model

data class AppUsage(
    val packageName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long
)
