package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.data.rules.CustomRuleCodec
import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.repository.CustomRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom rules live in DataStore as an encoded string set.
 *
 * Every write rewrites the whole set. The list is a handful of rows edited by
 * hand, so a per-row store would add migration surface for no gain; the whole
 * set rewrite also keeps "dedupe by value" in one place instead of spread over
 * four mutations.
 */
@Singleton
class CustomRuleRepositoryImpl @Inject constructor(
    private val preferences: SoberPreferences
) : CustomRuleRepository {

    override fun rules(): Flow<List<CustomRule>> = preferences.customRulesRaw.map(::decodeAll)

    override suspend fun add(type: CustomRuleType, value: String): CustomRule {
        val trimmed = value.trim()
        require(trimmed.isNotEmpty()) { "A custom rule needs a value" }

        var stored: CustomRule? = null
        preferences.updateCustomRules { current ->
            val existing = decodeAll(current).firstOrNull { it.matches(type, trimmed) }
            if (existing != null) {
                stored = existing
                current
            } else {
                val added = CustomRule(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    value = trimmed
                )
                stored = added
                encodeAll(decodeAll(current) + added)
            }
        }
        return requireNotNull(stored)
    }

    override suspend fun updateValue(id: String, value: String) {
        val trimmed = value.trim()
        require(trimmed.isNotEmpty()) { "A custom rule needs a value" }

        preferences.updateCustomRules { current ->
            val edited = decodeAll(current).map {
                if (it.id == id) it.copy(value = trimmed) else it
            }
            encodeAll(edited)
        }
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        preferences.updateCustomRules { current ->
            encodeAll(
                decodeAll(current).map {
                    if (it.id == id) it.copy(enabled = enabled) else it
                }
            )
        }
    }

    override suspend fun remove(id: String) {
        preferences.updateCustomRules { current ->
            encodeAll(decodeAll(current).filterNot { it.id == id })
        }
    }

    /**
     * Sorted so the list does not reshuffle between reads: DataStore's string
     * set has no order of its own. Blocked apps come first because that is the
     * rule with the bigger consequence.
     */
    private fun decodeAll(raw: Set<String>): List<CustomRule> =
        raw.mapNotNull(CustomRuleCodec::decode)
            .distinctBy { it.type to it.value.lowercase() }
            .sortedWith(compareBy({ it.type.ordinal }, { it.value.lowercase() }))

    private fun encodeAll(rules: List<CustomRule>): Set<String> =
        rules.map(CustomRuleCodec::encode).toSet()

    private fun CustomRule.matches(type: CustomRuleType, value: String): Boolean =
        this.type == type && this.value.equals(value, ignoreCase = true)
}
