package com.sober.core.data.backup

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import okio.Buffer

/**
 * The on-disk shape of a Sober backup.
 *
 * Field names are spelled out in JSON, so a backup file stays inspectable and
 * can be hand-edited without this class: the reader treats unknown enums as
 * absent and unknown keys as ignorable.
 */
data class BackupDocument(
    val format: String = FORMAT,
    val version: Int = CURRENT_VERSION,
    val createdAt: Long = 0L,
    val appLimits: List<BackupAppLimit> = emptyList(),
    val scheduleRules: List<BackupScheduleRule> = emptyList(),
    val customRules: List<BackupCustomRule> = emptyList(),
    val sectionRules: List<String> = emptyList(),
    val pauseRules: List<BackupPauseRule> = emptyList(),
    val categoryLimits: List<BackupCategoryLimit> = emptyList(),
    val allowOnly: BackupAllowOnly? = null,
    val bedtime: BackupBedtime? = null,
    val dailyCap: BackupDailyCap? = null
) {
    companion object {
        const val FORMAT = "sober-backup"
        const val CURRENT_VERSION = 1
    }
}

data class BackupAppLimit(
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val isActive: Boolean = true,
    val blockedSections: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val dailyOpenLimit: Int = 0
)

data class BackupScheduleRule(
    val ruleName: String,
    val blockedPackages: List<String> = emptyList(),
    val daysOfWeek: List<Int> = emptyList(),
    val startTime: String,
    val endTime: String,
    val isActive: Boolean = true
)

data class BackupCustomRule(
    val type: String,
    val value: String,
    val enabled: Boolean = true
)

data class BackupPauseRule(
    val packageName: String,
    val delaySeconds: Int,
    val isActive: Boolean = true
)

data class BackupCategoryLimit(
    val category: String,
    val dailyLimitMinutes: Int
)

data class BackupAllowOnly(
    val enabled: Boolean,
    val allowedPackages: List<String> = emptyList()
)

data class BackupBedtime(
    val enabled: Boolean,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val allowedPackages: List<String> = emptyList()
)

data class BackupDailyCap(
    val minutes: Int,
    val exemptPackages: List<String> = emptyList()
)

/**
 * JSON encoding for [BackupDocument], with the format check restore depends on.
 *
 * The reader and writer are written by hand rather than generated or reflected:
 * release builds shrink and obfuscate, and a codec that finds fields by name at
 * runtime is the kind of thing that only breaks in the build users install.
 * Hand-writing it also means an unknown key can be skipped instead of rejected,
 * so a backup from a later minor version still restores what it understands.
 */
class BackupCodec {

    fun encode(document: BackupDocument): String {
        val buffer = Buffer()
        val writer = JsonWriter.of(buffer)
        writer.setIndent("  ")
        writeDocument(writer, document)
        writer.flush()
        return buffer.readUtf8()
    }

    /**
     * A wrong file is a user mistake, not a crash: every rejection is an
     * [IllegalArgumentException] whose message can be shown as-is.
     */
    fun decode(json: String): BackupDocument {
        val document = try {
            readDocument(JsonReader.of(Buffer().writeUtf8(json)))
        } catch (e: Exception) {
            throw IllegalArgumentException("That file isn't a Sober backup.")
        }

        if (document.format != BackupDocument.FORMAT) {
            throw IllegalArgumentException("That file isn't a Sober backup.")
        }
        if (document.version !in 1..BackupDocument.CURRENT_VERSION) {
            throw IllegalArgumentException("This backup was made by a newer version of Sober.")
        }
        return document
    }

    private fun writeDocument(writer: JsonWriter, document: BackupDocument) {
        writer.beginObject()
        writer.name("format").value(document.format)
        writer.name("version").value(document.version)
        writer.name("createdAt").value(document.createdAt)

        writer.name("appLimits")
        writer.beginArray()
        document.appLimits.forEach { writeAppLimit(writer, it) }
        writer.endArray()

        writer.name("scheduleRules")
        writer.beginArray()
        document.scheduleRules.forEach { writeScheduleRule(writer, it) }
        writer.endArray()

        writer.name("customRules")
        writer.beginArray()
        document.customRules.forEach { writeCustomRule(writer, it) }
        writer.endArray()

        writer.name("sectionRules")
        writeStringList(writer, document.sectionRules)

        writer.name("pauseRules")
        writer.beginArray()
        document.pauseRules.forEach { writePauseRule(writer, it) }
        writer.endArray()

        writer.name("categoryLimits")
        writer.beginArray()
        document.categoryLimits.forEach { writeCategoryLimit(writer, it) }
        writer.endArray()

        writer.name("allowOnly")
        document.allowOnly?.let { writeAllowOnly(writer, it) } ?: writer.nullValue()

        writer.name("bedtime")
        document.bedtime?.let { writeBedtime(writer, it) } ?: writer.nullValue()

        writer.name("dailyCap")
        document.dailyCap?.let { writeDailyCap(writer, it) } ?: writer.nullValue()

        writer.endObject()
    }

    private fun writeAppLimit(writer: JsonWriter, value: BackupAppLimit) {
        writer.beginObject()
        writer.name("packageName").value(value.packageName)
        writer.name("appName").value(value.appName)
        writer.name("dailyLimitMinutes").value(value.dailyLimitMinutes)
        writer.name("isActive").value(value.isActive)
        writer.name("blockedSections")
        writeStringList(writer, value.blockedSections)
        writer.name("createdAt").value(value.createdAt)
        writer.name("dailyOpenLimit").value(value.dailyOpenLimit)
        writer.endObject()
    }

    private fun writeScheduleRule(writer: JsonWriter, value: BackupScheduleRule) {
        writer.beginObject()
        writer.name("ruleName").value(value.ruleName)
        writer.name("blockedPackages")
        writeStringList(writer, value.blockedPackages)
        writer.name("daysOfWeek")
        writer.beginArray()
        value.daysOfWeek.forEach { writer.value(it) }
        writer.endArray()
        writer.name("startTime").value(value.startTime)
        writer.name("endTime").value(value.endTime)
        writer.name("isActive").value(value.isActive)
        writer.endObject()
    }

    private fun writeCustomRule(writer: JsonWriter, value: BackupCustomRule) {
        writer.beginObject()
        writer.name("type").value(value.type)
        writer.name("value").value(value.value)
        writer.name("enabled").value(value.enabled)
        writer.endObject()
    }

    private fun writePauseRule(writer: JsonWriter, value: BackupPauseRule) {
        writer.beginObject()
        writer.name("packageName").value(value.packageName)
        writer.name("delaySeconds").value(value.delaySeconds)
        writer.name("isActive").value(value.isActive)
        writer.endObject()
    }

    private fun writeCategoryLimit(writer: JsonWriter, value: BackupCategoryLimit) {
        writer.beginObject()
        writer.name("category").value(value.category)
        writer.name("dailyLimitMinutes").value(value.dailyLimitMinutes)
        writer.endObject()
    }

    private fun writeAllowOnly(writer: JsonWriter, value: BackupAllowOnly) {
        writer.beginObject()
        writer.name("enabled").value(value.enabled)
        writer.name("allowedPackages")
        writeStringList(writer, value.allowedPackages)
        writer.endObject()
    }

    private fun writeBedtime(writer: JsonWriter, value: BackupBedtime) {
        writer.beginObject()
        writer.name("enabled").value(value.enabled)
        writer.name("startMinuteOfDay").value(value.startMinuteOfDay)
        writer.name("endMinuteOfDay").value(value.endMinuteOfDay)
        writer.name("allowedPackages")
        writeStringList(writer, value.allowedPackages)
        writer.endObject()
    }

    private fun writeDailyCap(writer: JsonWriter, value: BackupDailyCap) {
        writer.beginObject()
        writer.name("minutes").value(value.minutes)
        writer.name("exemptPackages")
        writeStringList(writer, value.exemptPackages)
        writer.endObject()
    }

    private fun writeStringList(writer: JsonWriter, values: List<String>) {
        writer.beginArray()
        values.forEach { writer.value(it) }
        writer.endArray()
    }

    private fun readDocument(reader: JsonReader): BackupDocument {
        var format = ""
        var version = 1
        var createdAt = 0L
        var appLimits: List<BackupAppLimit> = emptyList()
        var scheduleRules: List<BackupScheduleRule> = emptyList()
        var customRules: List<BackupCustomRule> = emptyList()
        var sectionRules: List<String> = emptyList()
        var pauseRules: List<BackupPauseRule> = emptyList()
        var categoryLimits: List<BackupCategoryLimit> = emptyList()
        var allowOnly: BackupAllowOnly? = null
        var bedtime: BackupBedtime? = null
        var dailyCap: BackupDailyCap? = null

        readObject(reader) { key ->
            when (key) {
                "format" -> format = reader.nextString()
                "version" -> version = reader.nextInt()
                "createdAt" -> createdAt = reader.nextLong()
                "appLimits" -> appLimits = readArray(reader) { readAppLimit(reader) }
                "scheduleRules" -> scheduleRules = readArray(reader) { readScheduleRule(reader) }
                "customRules" -> customRules = readArray(reader) { readCustomRule(reader) }
                "sectionRules" -> sectionRules = readStringList(reader)
                "pauseRules" -> pauseRules = readArray(reader) { readPauseRule(reader) }
                "categoryLimits" -> categoryLimits = readArray(reader) { readCategoryLimit(reader) }
                "allowOnly" -> allowOnly = readNullable(reader) { readAllowOnly(reader) }
                "bedtime" -> bedtime = readNullable(reader) { readBedtime(reader) }
                "dailyCap" -> dailyCap = readNullable(reader) { readDailyCap(reader) }
                else -> return@readObject false
            }
            true
        }

        return BackupDocument(
            format = format,
            version = version,
            createdAt = createdAt,
            appLimits = appLimits,
            scheduleRules = scheduleRules,
            customRules = customRules,
            sectionRules = sectionRules,
            pauseRules = pauseRules,
            categoryLimits = categoryLimits,
            allowOnly = allowOnly,
            bedtime = bedtime,
            dailyCap = dailyCap
        )
    }

    private fun readAppLimit(reader: JsonReader): BackupAppLimit {
        var packageName = ""
        var appName = ""
        var dailyLimitMinutes = 0
        var isActive = true
        var blockedSections: List<String> = emptyList()
        var createdAt = 0L
        var dailyOpenLimit = 0

        readObject(reader) { key ->
            when (key) {
                "packageName" -> packageName = reader.nextString()
                "appName" -> appName = reader.nextString()
                "dailyLimitMinutes" -> dailyLimitMinutes = reader.nextInt()
                "isActive" -> isActive = reader.nextBoolean()
                "blockedSections" -> blockedSections = readStringList(reader)
                "createdAt" -> createdAt = reader.nextLong()
                "dailyOpenLimit" -> dailyOpenLimit = reader.nextInt()
                else -> return@readObject false
            }
            true
        }

        return BackupAppLimit(
            packageName = packageName,
            appName = appName,
            dailyLimitMinutes = dailyLimitMinutes,
            isActive = isActive,
            blockedSections = blockedSections,
            createdAt = createdAt,
            dailyOpenLimit = dailyOpenLimit
        )
    }

    private fun readScheduleRule(reader: JsonReader): BackupScheduleRule {
        var ruleName = ""
        var blockedPackages: List<String> = emptyList()
        var daysOfWeek: List<Int> = emptyList()
        var startTime = ""
        var endTime = ""
        var isActive = true

        readObject(reader) { key ->
            when (key) {
                "ruleName" -> ruleName = reader.nextString()
                "blockedPackages" -> blockedPackages = readStringList(reader)
                "daysOfWeek" -> daysOfWeek = readIntList(reader)
                "startTime" -> startTime = reader.nextString()
                "endTime" -> endTime = reader.nextString()
                "isActive" -> isActive = reader.nextBoolean()
                else -> return@readObject false
            }
            true
        }

        return BackupScheduleRule(
            ruleName = ruleName,
            blockedPackages = blockedPackages,
            daysOfWeek = daysOfWeek,
            startTime = startTime,
            endTime = endTime,
            isActive = isActive
        )
    }

    private fun readCustomRule(reader: JsonReader): BackupCustomRule {
        var type = ""
        var value = ""
        var enabled = true

        readObject(reader) { key ->
            when (key) {
                "type" -> type = reader.nextString()
                "value" -> value = reader.nextString()
                "enabled" -> enabled = reader.nextBoolean()
                else -> return@readObject false
            }
            true
        }

        return BackupCustomRule(type = type, value = value, enabled = enabled)
    }

    private fun readPauseRule(reader: JsonReader): BackupPauseRule {
        var packageName = ""
        var delaySeconds = 0
        var isActive = true

        readObject(reader) { key ->
            when (key) {
                "packageName" -> packageName = reader.nextString()
                "delaySeconds" -> delaySeconds = reader.nextInt()
                "isActive" -> isActive = reader.nextBoolean()
                else -> return@readObject false
            }
            true
        }

        return BackupPauseRule(packageName = packageName, delaySeconds = delaySeconds, isActive = isActive)
    }

    private fun readCategoryLimit(reader: JsonReader): BackupCategoryLimit {
        var category = ""
        var dailyLimitMinutes = 0

        readObject(reader) { key ->
            when (key) {
                "category" -> category = reader.nextString()
                "dailyLimitMinutes" -> dailyLimitMinutes = reader.nextInt()
                else -> return@readObject false
            }
            true
        }

        return BackupCategoryLimit(category = category, dailyLimitMinutes = dailyLimitMinutes)
    }

    private fun readAllowOnly(reader: JsonReader): BackupAllowOnly {
        var enabled = false
        var allowedPackages: List<String> = emptyList()

        readObject(reader) { key ->
            when (key) {
                "enabled" -> enabled = reader.nextBoolean()
                "allowedPackages" -> allowedPackages = readStringList(reader)
                else -> return@readObject false
            }
            true
        }

        return BackupAllowOnly(enabled = enabled, allowedPackages = allowedPackages)
    }

    private fun readBedtime(reader: JsonReader): BackupBedtime {
        var enabled = false
        var startMinuteOfDay = 0
        var endMinuteOfDay = 0
        var allowedPackages: List<String> = emptyList()

        readObject(reader) { key ->
            when (key) {
                "enabled" -> enabled = reader.nextBoolean()
                "startMinuteOfDay" -> startMinuteOfDay = reader.nextInt()
                "endMinuteOfDay" -> endMinuteOfDay = reader.nextInt()
                "allowedPackages" -> allowedPackages = readStringList(reader)
                else -> return@readObject false
            }
            true
        }

        return BackupBedtime(
            enabled = enabled,
            startMinuteOfDay = startMinuteOfDay,
            endMinuteOfDay = endMinuteOfDay,
            allowedPackages = allowedPackages
        )
    }

    private fun readDailyCap(reader: JsonReader): BackupDailyCap {
        var minutes = 0
        var exemptPackages: List<String> = emptyList()

        readObject(reader) { key ->
            when (key) {
                "minutes" -> minutes = reader.nextInt()
                "exemptPackages" -> exemptPackages = readStringList(reader)
                else -> return@readObject false
            }
            true
        }

        return BackupDailyCap(minutes = minutes, exemptPackages = exemptPackages)
    }

    private fun readObject(reader: JsonReader, onField: (String) -> Boolean) {
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (!onField(name)) {
                reader.skipValue()
            }
        }
        reader.endObject()
    }

    private fun readStringList(reader: JsonReader): List<String> = readArray(reader) { reader.nextString() }

    private fun readIntList(reader: JsonReader): List<Int> = readArray(reader) { reader.nextInt() }

    private inline fun <T> readArray(reader: JsonReader, element: () -> T): List<T> {
        val values = mutableListOf<T>()
        reader.beginArray()
        while (reader.hasNext()) {
            values.add(element())
        }
        reader.endArray()
        return values
    }

    private fun <T> readNullable(reader: JsonReader, block: () -> T): T? {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Any>()
            return null
        }
        return block()
    }
}
