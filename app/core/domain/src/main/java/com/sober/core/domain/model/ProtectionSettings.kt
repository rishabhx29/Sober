package com.sober.core.domain.model

/**
 * Coarse buckets the user can budget separately from per-app limits.
 *
 * These mirror the platform's `ApplicationInfo.CATEGORY_*` values rather than
 * being invented here: the data layer resolves each installed app through
 * PackageManager, so a user gets sensible buckets without Sober shipping a
 * hard-coded list of package names that goes stale.
 */
enum class AppCategory {
    SOCIAL,
    VIDEO,
    GAMES,
    PRODUCTIVITY,
    NEWS,
    OTHER
}

/** A daily screen-time budget shared by every app in [category]. */
data class CategoryLimit(
    val category: AppCategory,
    val dailyLimitMinutes: Int,
    val isActive: Boolean = true
)

/**
 * The strictest mode Sober offers: when enabled only [allowedPackages] may be
 * used at all. Everything else is held shut regardless of limits or schedules.
 */
data class AllowOnlyMode(
    val enabled: Boolean = false,
    val allowedPackages: Set<String> = emptySet()
)

/**
 * An overnight window during which only [allowedPackages] may run, typically
 * calls, maps and the alarm clock. The window is stored as minutes since
 * midnight so an overnight window (22:00 → 07:00) is one row, not two.
 */
data class BedtimeSettings(
    val enabled: Boolean = false,
    val startMinuteOfDay: Int = 22 * 60,
    val endMinuteOfDay: Int = 7 * 60,
    val allowedPackages: Set<String> = emptySet()
) {
    /** True when [nowMinuteOfDay] falls inside the window, overnight included. */
    fun isActive(nowMinuteOfDay: Int): Boolean {
        if (!enabled) return false
        return if (startMinuteOfDay <= endMinuteOfDay) {
            nowMinuteOfDay >= startMinuteOfDay && nowMinuteOfDay < endMinuteOfDay
        } else {
            nowMinuteOfDay >= startMinuteOfDay || nowMinuteOfDay < endMinuteOfDay
        }
    }

    /** Epoch millis at which the window that contains [nowMillis] closes. */
    fun endFor(nowMillis: Long): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, endMinuteOfDay / 60)
            set(java.util.Calendar.MINUTE, endMinuteOfDay % 60)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= nowMillis) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}

/**
 * "Take a breath before opening": a short delay shown before [packageName]
 * opens, so the decision to open it is made deliberately rather than by habit.
 */
data class AppPauseRule(
    val packageName: String,
    val delaySeconds: Int = 10,
    val isActive: Boolean = true
)
