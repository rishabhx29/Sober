package com.sober.core.domain.repository

import com.sober.core.domain.model.DetectionRule
import kotlinx.coroutines.flow.Flow

/**
 * Section rules the user recorded themselves, kept apart from the shipped
 * catalog so an app update can replace the catalog without touching them.
 */
interface CustomSectionRuleRepository {

    fun rules(): Flow<List<DetectionRule>>

    /**
     * Stores [rule] switched on, assigning it a fresh id. A rule for the same
     * package, signal and type returns the existing record instead of adding a
     * second row the user cannot tell apart.
     */
    suspend fun add(rule: DetectionRule): DetectionRule

    /** Removes the rule and switches off its id in one step. */
    suspend fun remove(id: String)
}
