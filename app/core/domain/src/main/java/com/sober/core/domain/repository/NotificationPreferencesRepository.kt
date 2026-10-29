package com.sober.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Optional alerts the user can silence from Settings.
 *
 * The notification-access alert is deliberately not in this list: it fires
 * because blocking has stopped working, and muting a broken guard is the one
 * notification the app must not let someone do to themselves by accident.
 */
enum class AlertKind {
    /** "80% of your limit" warnings for apps and the overall cap. */
    USAGE_WARNING,

    /** "Session complete" when a timed focus session reaches its end. */
    SESSION_COMPLETE,

    /** "Streak at risk" after the third cancellation in a day, or a limit crossed late in the day. */
    STREAK_WARNING,

    /** Evening confirmation that today is still under every limit. */
    DAILY_STREAK,

    /** 15 minutes before a sleep schedule starts. */
    BEDTIME_WARNING
}

/**
 * Which alerts Sober may post. Defaults to enabled for every kind: these
 * warnings are the product's feedback loop, so an unset preference must mean
 * "on", not "off".
 */
interface NotificationPreferencesRepository {
    fun isEnabled(kind: AlertKind): Flow<Boolean>

    suspend fun setEnabled(kind: AlertKind, enabled: Boolean)
}
