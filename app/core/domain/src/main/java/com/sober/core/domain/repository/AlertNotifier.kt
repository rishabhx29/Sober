package com.sober.core.domain.repository

/**
 * One-shot alerts the user must see even when no Sober screen is open.
 *
 * The domain layer decides *when* a warning is warranted; the Android layer
 * owns channels and posting. Kept separate from [EnforcementController] so the
 * enforcement engine is not the only way to reach the notification system.
 */
interface AlertNotifier {
    /** Fired the third time a focus session is cancelled in one day. */
    fun notifyStreakWarning(cancelledToday: Int)

    /**
     * Fired late in the day when a limit has been crossed, so the current
     * streak will reset at midnight. [streakDays] is the streak as it stood
     * before today, because today has already failed.
     */
    fun notifyStreakAtRisk(streakDays: Int)

    /** Fired in the evening when every limit still holds and a streak is live. */
    fun notifyDailyStreak(streakDays: Int)

    /** Fired 15 minutes before a sleep schedule window opens. */
    fun notifyBedtimeWarning(ruleName: String, minutesUntil: Int)

    /**
     * Fired once per day when an app or the overall cap crosses 80% of its
     * limit. [packageName] is null for the device-wide cap.
     */
    fun notifyUsageWarning(packageName: String?, usedMinutes: Int, limitMinutes: Int)

    /** Fired when a timed focus session reaches its end. */
    fun notifySessionComplete(label: String, durationMinutes: Int)

    /**
     * Fired when Notification Access is lost while a session is silencing
     * notifications. The alert must offer a way back to the system setting.
     */
    fun notifyNotificationAccessRevoked()
}
