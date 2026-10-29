package com.sober.core.domain.model

data class UsageSession(
    val id: Int = 0,
    val packageName: String,
    val date: String, // YYYY-MM-DD
    val usedMinutes: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)
