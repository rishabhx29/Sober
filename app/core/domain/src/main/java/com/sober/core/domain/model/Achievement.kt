package com.sober.core.domain.model

/**
 * The six achievements from the PRD.
 *
 * The display strings live on the enum so the Stats grid, the future detail
 * sheet and any notification all describe an achievement the same way. [emoji]
 * is deliberately not translated: it is the visual identity of the badge.
 */
enum class AchievementId(
    val emoji: String,
    val title: String,
    val description: String
) {
    SEVEN_DAY_STREAK(
        emoji = "🔥",
        title = "7-Day Streak",
        description = "Stay under your limits 7 days in a row"
    ),
    FOCUS_MASTER(
        emoji = "🧘",
        title = "Focus Master",
        description = "Complete 10 focus sessions"
    ),
    SOCIAL_DETOX(
        emoji = "📵",
        title = "Social Detox",
        description = "A full day without social media"
    ),
    NIGHT_OWL(
        emoji = "🌙",
        title = "Night Owl No More",
        description = "No phone after 10 PM for 5 nights"
    ),
    SPEED_DEMON(
        emoji = "⚡",
        title = "Speed Demon",
        description = "A Pomodoro 5 days in a row"
    ),
    SOBER_PRO(
        emoji = "🏆",
        title = "Sober Pro",
        description = "30 days under every limit"
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
