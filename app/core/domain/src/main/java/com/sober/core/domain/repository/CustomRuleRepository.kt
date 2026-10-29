package com.sober.core.domain.repository

import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import kotlinx.coroutines.flow.Flow

/**
 * The user's own rules, newest last.
 *
 * Writes are suspend because they land in DataStore; reads are a Flow so the
 * settings list and the enforcement services all see the same list without a
 * refresh call.
 */
interface CustomRuleRepository {
    fun rules(): Flow<List<CustomRule>>

    /**
     * Adds a rule unless an equivalent one already exists, in which case the
     * existing rule is returned untouched. Re-adding the same app or keyword is
     * a no-op rather than a duplicate row the user then has to clean up.
     */
    suspend fun add(type: CustomRuleType, value: String): CustomRule

    /** Replaces [id]'s value, keeping its enabled state and position. */
    suspend fun updateValue(id: String, value: String)

    suspend fun setEnabled(id: String, enabled: Boolean)

    suspend fun remove(id: String)
}
