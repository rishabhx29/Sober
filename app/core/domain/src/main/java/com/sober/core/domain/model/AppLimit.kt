package com.sober.core.domain.model

data class AppLimit(
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val isActive: Boolean = true,
    val blockedSections: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
