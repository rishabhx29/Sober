package com.sober.core.domain.model

/**
 * How aggressively a focus session silences notifications.
 *
 * Stored as a string in DataStore so an unrecognised value from a future
 * version degrades to [NONE] rather than throwing.
 */
enum class NotificationSilenceScope {
    /** Let everything through. */
    NONE,

    /** Silence every notification except phone calls. */
    ALL_EXCEPT_CALLS,

    /** Silence only notifications from apps in the session's block list. */
    SELECTED_APPS,

    /** Silence everything. */
    ALL
}