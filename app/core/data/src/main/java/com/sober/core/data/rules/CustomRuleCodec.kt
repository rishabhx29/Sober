package com.sober.core.data.rules

import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType

/**
 * Encodes a [CustomRule] into the single string DataStore's string set stores.
 *
 * Field order is `id|type|enabled|value`, and the value is last so that a
 * keyword containing the separator survives: splitting stops after four fields,
 * which puts everything else back into the value. The id is a UUID and the type
 * is an enum name, so neither can contain the separator.
 */
object CustomRuleCodec {

    private const val SEPARATOR = '|'
    private const val FIELD_COUNT = 4

    fun encode(rule: CustomRule): String = buildString {
        append(rule.id)
        append(SEPARATOR)
        append(rule.type.name)
        append(SEPARATOR)
        append(if (rule.enabled) '1' else '0')
        append(SEPARATOR)
        append(rule.value)
    }

    /**
     * Lenient on read: a record written by an older build (or a hand-edited
     * store) is dropped rather than crashing the whole list.
     */
    fun decode(raw: String): CustomRule? {
        val parts = raw.split(SEPARATOR, limit = FIELD_COUNT)
        if (parts.size != FIELD_COUNT) return null

        val id = parts[0]
        val type = CustomRuleType.entries.firstOrNull { it.name == parts[1] } ?: return null
        val enabled = when (parts[2]) {
            "1" -> true
            "0" -> false
            else -> return null
        }
        val value = parts[3]
        if (id.isBlank() || value.isBlank()) return null

        return CustomRule(id = id, type = type, value = value, enabled = enabled)
    }
}
