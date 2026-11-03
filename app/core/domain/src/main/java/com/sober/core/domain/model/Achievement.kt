package com.sober.core.domain.model

/**
 * The six PRD achievements, plus two added with challenges in Phase 8.
 *
 * The display strings live on the enum so the Stats grid and any notification
 * describe an achievement the same way. The badge glyph is chosen in the UI
 * layer: emoji rendered differently on every OEM and ignored the theme, so the
 * visual identity is a Material icon keyed by [AchievementId].
 */
enum class AchievementId(
    val title: String,
    val description: String
) {
    SEVEN_DAY_STREAK(
        title = "7-Day Streak",
        description = "Stay under your limits 7 days in a row"
    ),
    FOCUS_MASTER(
        title = "Focus Master",
        description = "Complete 10 focus sessions"
    ),
    SOCIAL_DETOX(
        title = "Social Detox",
        description = "A full day without social media"
    ),
    NIGHT_OWL(
        title = "Night Owl No More",
        description = "No phone after 10 PM for 5 nights"
    ),
    SPEED_DEMON(
        title = "Speed Demon",
        description = "A Pomodoro 5 days in a row"
    ),
    SOBER_PRO(
        title = "Sober Pro",
        description = "30 days under every limit"
    ),
    DEEP_WORK(
        title = "Deep Work",
        description = "Complete a 90-minute focus session"
    ),
    CHALLENGE_CHAMPION(
        title = "Challenge Champion",
        description = "Finish a challenge you started"
    )
}

/**
 * [unlockedAtMillis] is the first time the condition was met, not the latest:
 * achievements are permanent. A seven-day streak that is later broken must not
 * take the badge away.
 */
data class Achievement(
    val id: AchievementId,
    val isUnlocked: Boolean,
    val unlockedAtMillis: Long? = null
)
