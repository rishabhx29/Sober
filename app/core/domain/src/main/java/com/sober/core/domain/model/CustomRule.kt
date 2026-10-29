package com.sober.core.domain.model

/**
 * A rule the user wrote themselves, as opposed to the bundled section catalog
 * or a scheduled window.
 *
 * Two kinds exist because they are enforced in different places: a keyword is
 * read by the notification listener (where the text exists), an app is read by
 * the block policy (where the foreground package is known). Keeping them in one
 * list means the user manages one screen.
 */
enum class CustomRuleType {
    /** Notifications whose title or body contains [CustomRule.value] are hidden. */
    NOTIFICATION_KEYWORD,

    /** The app with package [CustomRule.value] is blocked outright, with no end. */
    BLOCKED_APP
}

data class CustomRule(
    /** Stable identity so a toggle cannot hit the wrong row after an edit. */
    val id: String,
    val type: CustomRuleType,
    /** The keyword, or the package name. Never blank. */
    val value: String,
    val enabled: Boolean = true
)
