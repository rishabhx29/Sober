package com.sober.core.data.rules

import com.sober.core.domain.model.CapturedCandidate
import com.sober.core.domain.model.CapturedSection
import com.sober.core.domain.model.DetectionType
import com.sober.core.domain.model.SectionCaptureRequest

/**
 * Encodes the one in-flight recording and its result for DataStore.
 *
 * Unlike a rule, a candidate label can be arbitrary text the user saw on
 * screen, so every field is percent-escaped before the three separators are
 * applied. That keeps a label containing `|`, `;` or `,` from splitting a
 * record into fields that do not exist.
 */
object SectionCaptureCodec {

    private const val FIELD = '|'
    private const val CANDIDATE = ';'
    private const val VALUE = ','

    fun encodeRequest(request: SectionCaptureRequest): String =
        escape(request.packageName) + FIELD + request.requestedAt

    fun decodeRequest(raw: String?): SectionCaptureRequest? {
        val parts = raw?.split(FIELD, limit = 2) ?: return null
        if (parts.size != 2) return null
        val packageName = unescape(parts[0]) ?: return null
        val requestedAt = parts[1].toLongOrNull() ?: return null
        if (packageName.isBlank() || requestedAt <= 0L) return null
        return SectionCaptureRequest(packageName = packageName, requestedAt = requestedAt)
    }

    fun encodeSection(section: CapturedSection): String = listOf(
        escape(section.packageName),
        escape(section.activityName.orEmpty()),
        section.capturedAt.toString(),
        section.candidates.joinToString(CANDIDATE.toString()) { candidate ->
            listOf(
                escape(candidate.type.name),
                escape(candidate.value),
                escape(candidate.label),
                if (candidate.isBroad) "1" else "0"
            ).joinToString(VALUE.toString())
        }
    ).joinToString(FIELD.toString())

    fun decodeSection(raw: String?): CapturedSection? {
        val parts = raw?.split(FIELD, limit = 4) ?: return null
        if (parts.size != 4) return null
        val packageName = unescape(parts[0]) ?: return null
        val activityName = unescape(parts[1]) ?: return null
        val capturedAt = parts[2].toLongOrNull() ?: return null
        if (packageName.isBlank()) return null

        val candidates = if (parts[3].isEmpty()) {
            emptyList()
        } else {
            parts[3].split(CANDIDATE).mapNotNull(::decodeCandidate)
        }
        return CapturedSection(
            packageName = packageName,
            activityName = activityName.ifBlank { null },
            capturedAt = capturedAt,
            candidates = candidates
        )
    }

    private fun decodeCandidate(raw: String): CapturedCandidate? {
        val parts = raw.split(VALUE, limit = 4)
        if (parts.size != 4) return null
        val typeName = unescape(parts[0]) ?: return null
        val type = DetectionType.entries.firstOrNull { it.name == typeName } ?: return null
        val value = unescape(parts[1]) ?: return null
        val label = unescape(parts[2]) ?: return null
        val broad = when (parts[3]) {
            "1" -> true
            "0" -> false
            else -> return null
        }
        if (value.isBlank()) return null
        return CapturedCandidate(
            type = type,
            value = value,
            label = label.ifBlank { value },
            isBroad = broad
        )
    }

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when {
                char == '%' -> append("%25")
                char == FIELD -> append("%7C")
                char == CANDIDATE -> append("%3B")
                char == VALUE -> append("%2C")
                char == '\n' -> append("%0A")
                char == '\r' -> append("%0D")
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
