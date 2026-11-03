package com.sober.core.domain.repository

/**
 * What a restore actually changed, so the screen can say more than "done".
 */
data class BackupSummary(
    val appLimits: Int = 0,
    val schedules: Int = 0,
    val customRules: Int = 0,
    val sectionRules: Int = 0,
    val pauseRules: Int = 0,
    val categoryLimits: Int = 0
) {
    val totalRules: Int
        get() = appLimits + schedules + customRules + sectionRules + pauseRules + categoryLimits
}

/**
 * Protection rules as one portable document.
 *
 * A backup deliberately excludes the PIN, lock and pause state, overrides,
 * streaks, achievements and usage history. Moving rules to a new phone is the
 * job; cloning an identity or carrying an unlocked lock across devices is not.
 */
interface BackupRepository {
    /** Serialises every protection rule and budget on this device. */
    suspend fun export(): String

    /**
     * Replaces every rule on this device with the document's.
     *
     * @throws IllegalArgumentException when the file is not a Sober backup or
     * was written by a newer format version. Validation happens before any
     * write, so a rejected document cannot leave the device half-restored.
     */
    suspend fun restore(json: String): BackupSummary
}
