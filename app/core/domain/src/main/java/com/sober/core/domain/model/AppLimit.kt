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
     * the app rests for [sessionRestMinutes] once the session ends. The row can
     * exist with this flag alone: no minutes and no opens budget.
     */
    val sessionTimerEnabled: Boolean = false,
    /**
     * How long the app stays closed after a session timer session ends. The
     * session carries this value, so editing it never moves a rest that is
     * already running.
     */
    val sessionRestMinutes: Int = DEFAULT_SESSION_REST_MINUTES
) {
    companion object {
        /** Long enough to break the pull back in, short enough to be a rest. */
        const val DEFAULT_SESSION_REST_MINUTES = 5

        const val MIN_SESSION_REST_MINUTES = 1
        const val MAX_SESSION_REST_MINUTES = 180
    }
}
