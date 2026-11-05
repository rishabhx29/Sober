package com.sober.core.domain.model

data class AppLimit(
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val isActive: Boolean = true,
    val blockedSections: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * How many times the app may be opened today. Zero means no open budget:
     * the limit is time-only, which is the older and more common setup.
     */
    val dailyOpenLimit: Int = 0,
    /**
     * When true, every entry into the app starts with a duration picker, and
     * the app rests for as long as the chosen session once it ends. The row can
     * exist with this flag alone: no minutes and no opens budget.
     */
    val sessionTimerEnabled: Boolean = false
)
