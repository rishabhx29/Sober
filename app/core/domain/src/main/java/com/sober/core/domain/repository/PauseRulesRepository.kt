package com.sober.core.domain.repository

import com.sober.core.domain.model.AppPauseRule
import kotlinx.coroutines.flow.Flow

/**
 * Per-app "pause before opening" rules, plus the short passes earned by sitting
 * through one.
 *
 * A pass is a package plus the instant it expires. It is stored in the same
 * preferences file as the rules because both are policy: losing a pass on
 * process death only costs the user one more countdown, while losing a rule
 * would silently disable a promise they made.
 */
interface PauseRulesRepository {
    val rules: Flow<List<AppPauseRule>>

    /** packageName -> epoch millis the earned pass expires. */
    val passes: Flow<Map<String, Long>>

    suspend fun ruleFor(packageName: String): AppPauseRule?

    /** Adds or updates the rule for [packageName]; a null [rule] removes it. */
    suspend fun setRule(packageName: String, rule: AppPauseRule?)

    /** Marks [packageName] as allowed until [until] without another countdown. */
    suspend fun grantPass(packageName: String, until: Long)

    /** The pass expiry for [packageName], or null when none is running. */
    suspend fun passUntil(packageName: String): Long?

    /** Drops expired passes so the stored set cannot grow forever. */
    suspend fun clearExpiredPasses(now: Long = System.currentTimeMillis())
}
