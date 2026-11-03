package com.sober.core.data.rules

import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.DetectionType

/**
 * Encodes a user-recorded [DetectionRule] into the string DataStore stores.
 *
 * Field order is fixed, and every field is percent-escaped first, so a section
 * name containing the separator (or a `%`) survives a round trip. The name is
 * free text the user typed, which is why this codec escapes where
 * [CustomRuleCodec] can rely on putting the value last.
 */
object SectionRuleCodec {

    private const val SEPARATOR = '|'
    private const val FIELD_COUNT = 10

    fun encode(rule: DetectionRule): String = listOf(
        rule.id,
        rule.packageName,
        rule.appName,
        rule.sectionId,
        rule.sectionName,
        rule.detectionType.name,
        rule.detectionValue,
        rule.overlayTitle,
        rule.overlaySubtitle,
        rule.activityValue.orEmpty()
    ).joinToString(SEPARATOR.toString(), transform = ::escape)

    /**
     * Lenient on read: a record written by an older build (or a hand-edited
     * store) is dropped rather than crashing the whole list.
     */
    fun decode(raw: String): DetectionRule? {
        val parts = raw.split(SEPARATOR, limit = FIELD_COUNT)
        if (parts.size != FIELD_COUNT) return null

        val values = parts.map { unescape(it) ?: return null }
        val id = values[0]
        val packageName = values[1]
        val sectionName = values[4]
        val type = DetectionType.entries.firstOrNull { it.name == values[5] } ?: return null
        val detectionValue = values[6]
        if (id.isBlank() || packageName.isBlank() ||
            sectionName.isBlank() || detectionValue.isBlank()
        ) {
            return null
        }

        return DetectionRule(
            id = id,
            packageName = packageName,
            appName = values[2].ifBlank { packageName },
            sectionId = values[3].ifBlank { id },
            sectionName = sectionName,
            detectionType = type,
            detectionValue = detectionValue,
            overlayTitle = values[7].ifBlank { "$sectionName is blocked" },
            overlaySubtitle = values[8].ifBlank { "You set this rule in Sober." },
            activityValue = values[9].ifBlank { null },
            // This store only ever holds rules the user recorded.
            isCustom = true
        )
    }

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '%' -> append("%25")
                SEPARATOR -> append("%7C")
                '\n' -> append("%0A")
                '\r' -> append("%0D")
                else -> append(char)
            }
        }
    }

    /** Null means the field is not a valid escape, not that it was blank. */
    private fun unescape(value: String): String? {
        if ('%' !in value) return value
        val out = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char != '%') {
                out.append(char)
                index++
                continue
            }
            if (index + 2 >= value.length) return null
            val code = value.substring(index + 1, index + 3).toIntOrNull(16) ?: return null
            out.append(code.toChar())
            index += 3
        }
        return out.toString()
    }
}
