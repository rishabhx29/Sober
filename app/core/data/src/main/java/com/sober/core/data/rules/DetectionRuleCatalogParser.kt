package com.sober.core.data.rules

import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.DetectionType
import com.sober.core.domain.repository.DetectionRuleCatalog
import com.squareup.moshi.JsonReader
import okio.BufferedSource

/**
 * Parses the versioned detection-rules document.
 *
 * Streamed with [JsonReader] rather than reflective binding: the document ships
 * as an asset and is also cached as raw text, so the parser has to survive an
 * unknown field from a newer file without failing the whole catalog. A
 * structurally broken document throws; a single unusable section is skipped.
 */
object DetectionRuleCatalogParser {

    fun parse(source: BufferedSource): DetectionRuleCatalog {
        val reader = JsonReader.of(source)
        reader.isLenient = false

        var version = 0
        val rules = mutableListOf<DetectionRule>()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "version" -> version = reader.nextInt()
                "apps" -> readApps(reader, rules)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        if (version <= 0) throw IllegalArgumentException("rules document has no version")

        // A duplicate id would make "which rule is on" ambiguous in the toggles.
        val seen = mutableSetOf<String>()
        val unique = rules.filter { seen.add(it.id) }

        return DetectionRuleCatalog(version = version, rules = unique)
    }

    private fun readApps(reader: JsonReader, into: MutableList<DetectionRule>) {
        reader.beginArray()
        while (reader.hasNext()) {
            readApp(reader, into)
        }
        reader.endArray()
    }

    private fun readApp(reader: JsonReader, into: MutableList<DetectionRule>) {
        var packageName: String? = null
        var appName: String? = null
        val sections = mutableListOf<RawSection>()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "packageName" -> packageName = reader.nextString()
                "appName" -> appName = reader.nextString()
                "sections" -> readSections(reader, sections)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val pkg = packageName?.trim().orEmpty()
        if (!isValidPackage(pkg)) return
        val app = appName?.trim().takeUnless { it.isNullOrBlank() } ?: pkg.substringAfterLast('.')

        sections.mapNotNull { section -> section.toRule(pkg, app) }.forEach(into::add)
    }

    private fun readSections(reader: JsonReader, into: MutableList<RawSection>) {
        reader.beginArray()
        while (reader.hasNext()) {
            into += readSection(reader)
        }
        reader.endArray()
    }

    private fun readSection(reader: JsonReader): RawSection {
        var id: String? = null
        var name: String? = null
        var match: String? = null
        var value: String? = null
        var title: String? = null
        var subtitle: String? = null
        var activity: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "name" -> name = reader.nextString()
                "match" -> match = reader.nextString()
                "value" -> value = reader.nextString()
                "title" -> title = reader.nextString()
                "subtitle" -> subtitle = reader.nextString()
                "activity" -> activity = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return RawSection(id, name, match, value, title, subtitle, activity)
    }

    private data class RawSection(
        val id: String?,
        val name: String?,
        val match: String?,
        val value: String?,
        val title: String?,
        val subtitle: String?,
        val activity: String?
    ) {
        fun toRule(packageName: String, appName: String): DetectionRule? {
            val sectionId = id?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val sectionName = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val detectionValue = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val type = match?.trim()
                ?.let { raw -> runCatching { DetectionType.valueOf(raw) }.getOrNull() }
                ?: return null
            val overlayTitle = title?.trim()?.takeIf { it.isNotEmpty() }
                ?: "$sectionName is blocked"

            return DetectionRule(
                id = "$packageName:$sectionId",
                packageName = packageName,
                appName = appName,
                sectionId = sectionId,
                sectionName = sectionName,
                detectionType = type,
                detectionValue = detectionValue,
                activityValue = activity?.trim()?.takeIf { it.isNotEmpty() },
                overlayTitle = overlayTitle,
                overlaySubtitle = subtitle?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "You set this rule in Sober."
            )
        }
    }

    /** Reverse-DNS-ish, which every Android package name already is. */
    private val PACKAGE_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")

    private fun isValidPackage(packageName: String): Boolean =
        PACKAGE_PATTERN.matches(packageName)
}
