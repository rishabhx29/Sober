package com.sober.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * What makes a day count towards the streak.
 *
 * Both rules require the user to have set at least one limit: a streak of days
 * spent "under" a rule that does not exist is not an achievement.
 */
enum class StreakRule {
    /** The day stayed under the overall cap and every active app limit. */
    UNDER_LIMITS,

    /** [UNDER_LIMITS], and no focus session was cancelled that day. */
    LIMITS_AND_NO_CANCELLATIONS
}

interface StreakPreferencesRepository {
    fun rule(): Flow<StreakRule>

    suspend fun setRule(rule: StreakRule)
}
