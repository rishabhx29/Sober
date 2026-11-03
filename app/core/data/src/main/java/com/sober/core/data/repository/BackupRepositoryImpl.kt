package com.sober.core.data.repository

import com.sober.core.data.backup.BackupAllowOnly
import com.sober.core.data.backup.BackupAppLimit
import com.sober.core.data.backup.BackupBedtime
import com.sober.core.data.backup.BackupCategoryLimit
import com.sober.core.data.backup.BackupCodec
import com.sober.core.data.backup.BackupCustomRule
import com.sober.core.data.backup.BackupDailyCap
import com.sober.core.data.backup.BackupDocument
import com.sober.core.data.backup.BackupPauseRule
import com.sober.core.data.backup.BackupScheduleRule
import com.sober.core.data.backup.BackupSectionRule
import com.sober.core.data.local.dao.AppLimitDao
import com.sober.core.data.local.dao.ScheduleRuleDao
import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.data.local.entity.toEntity
import com.sober.core.data.rules.CustomRuleCodec
import com.sober.core.data.rules.SectionRuleCodec
import com.sober.core.domain.model.AllowOnlyMode
import com.sober.core.domain.model.AppCategory
import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.model.AppPauseRule
import com.sober.core.domain.model.BedtimeSettings
import com.sober.core.domain.model.CategoryLimit
import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.DetectionType
import com.sober.core.domain.model.ScheduleRule
import com.sober.core.domain.repository.BackupRepository
import com.sober.core.domain.repository.BackupSummary
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and writes every protection rule, so a backup is one file rather than
 * a list of per-feature exports.
 *
 * Restore parses and validates the whole document before touching anything:
 * a wrong file is rejected with nothing written, and a valid one replaces the
 * rules rather than merging, which is the only predictable outcome when a
 * limit exists on both devices.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val appLimitDao: AppLimitDao,
    private val scheduleRuleDao: ScheduleRuleDao,
    private val preferences: SoberPreferences
) : BackupRepository {

    private val codec = BackupCodec()

    override suspend fun export(): String {
        val limits = appLimitDao.getAllLimits().first()
        val schedules = scheduleRuleDao.getAllRules().first()
        val customRules = preferences.customRulesRaw.first()
            .mapNotNull(CustomRuleCodec::decode)
        val sectionIds = preferences.enabledSectionRules.first()
        val customSectionRules = preferences.customSectionRulesRaw.first()
            .mapNotNull(SectionRuleCodec::decode)
            .distinctBy { it.id }
        val allowOnly = preferences.allowOnly.first()
        val bedtime = preferences.bedtime.first()

        val document = BackupDocument(
            createdAt = System.currentTimeMillis(),
            appLimits = limits.map {
                BackupAppLimit(
                    packageName = it.packageName,
                    appName = it.appName,
                    dailyLimitMinutes = it.dailyLimitMinutes,
                    isActive = it.isActive,
                    blockedSections = it.blockedSections,
                    createdAt = it.createdAt,
                    dailyOpenLimit = it.dailyOpenLimit
                )
            },
            scheduleRules = schedules.map {
                BackupScheduleRule(
                    ruleName = it.ruleName,
                    blockedPackages = it.blockedPackages,
                    daysOfWeek = it.daysOfWeek,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    isActive = it.isActive
                )
            },
            customRules = customRules.map {
                BackupCustomRule(type = it.type.name, value = it.value, enabled = it.enabled)
            },
            sectionRules = sectionIds
                .filterNot { it.startsWith(CustomSectionRuleRepositoryImpl.ID_PREFIX) }
                .sorted(),
            customSectionRules = customSectionRules.map {
                BackupSectionRule(
                    packageName = it.packageName,
                    appName = it.appName,
                    sectionName = it.sectionName,
                    detectionType = it.detectionType.name,
                    detectionValue = it.detectionValue,
                    overlayTitle = it.overlayTitle,
                    overlaySubtitle = it.overlaySubtitle,
                    activityValue = it.activityValue,
                    enabled = it.id in sectionIds
                )
            },
            pauseRules = preferences.appPauseRules.first().map {
                BackupPauseRule(
                    packageName = it.packageName,
                    delaySeconds = it.delaySeconds,
                    isActive = it.isActive
                )
            },
            categoryLimits = preferences.categoryLimits.first().map {
                BackupCategoryLimit(
                    category = it.category.name,
                    dailyLimitMinutes = it.dailyLimitMinutes
                )
            },
            allowOnly = BackupAllowOnly(
                enabled = allowOnly.enabled,
                allowedPackages = allowOnly.allowedPackages.sorted()
            ),
            bedtime = BackupBedtime(
                enabled = bedtime.enabled,
                startMinuteOfDay = bedtime.startMinuteOfDay,
                endMinuteOfDay = bedtime.endMinuteOfDay,
                allowedPackages = bedtime.allowedPackages.sorted()
            ),
            dailyCap = BackupDailyCap(
                minutes = preferences.dailyScreenTimeLimitMinutes.first(),
                exemptPackages = preferences.dailyCapExemptPackages.first().sorted()
            )
        )

        return codec.encode(document)
    }

    override suspend fun restore(json: String): BackupSummary {
        val document = codec.decode(json)

        // Build first. Every list below is validated and mapped before the
        // first write, so a malformed entry cannot leave a half-restored set.
        val limits = document.appLimits
            .filter { it.packageName.isNotBlank() }
            .map {
                AppLimit(
                    packageName = it.packageName,
                    appName = it.appName.ifBlank { it.packageName },
                    dailyLimitMinutes = it.dailyLimitMinutes.coerceAtLeast(0),
                    isActive = it.isActive,
                    blockedSections = it.blockedSections,
                    createdAt = it.createdAt,
                    dailyOpenLimit = it.dailyOpenLimit.coerceAtLeast(0)
                )
            }

        val schedules = document.scheduleRules
            .filter { it.ruleName.isNotBlank() && it.startTime.isNotBlank() && it.endTime.isNotBlank() }
            .map {
                ScheduleRule(
                    ruleName = it.ruleName,
                    blockedPackages = it.blockedPackages,
                    daysOfWeek = it.daysOfWeek.filter { day -> day in 1..7 },
                    startTime = it.startTime,
                    endTime = it.endTime,
                    isActive = it.isActive
                )
            }

        val customRules = document.customRules.mapNotNull { entry ->
            val type = CustomRuleType.entries.firstOrNull { it.name == entry.type }
                ?: return@mapNotNull null
            if (entry.value.isBlank()) return@mapNotNull null
            CustomRule(
                // The backup does not carry ids: replacing the set makes them
                // meaningless, and a fresh id cannot collide with anything.
                id = UUID.randomUUID().toString(),
                type = type,
                value = entry.value,
                enabled = entry.enabled
            )
        }

        val categoryLimits = document.categoryLimits.mapNotNull { entry ->
            val category = AppCategory.entries.firstOrNull { it.name == entry.category }
                ?: return@mapNotNull null
            CategoryLimit(
                category = category,
                dailyLimitMinutes = entry.dailyLimitMinutes.coerceAtLeast(0)
            )
        }

        val pauseRules = document.pauseRules
            .filter { it.packageName.isNotBlank() }
            .map { AppPauseRule(it.packageName, it.delaySeconds.coerceIn(0, 300), it.isActive) }

        // A recorded rule names the section it blocks, not a row in this
        // device's store, so restore mints a fresh id the same way saving does.
        // Duplicates collapse because two identical rows could not be told
        // apart when the user removes one.
        val customSectionRules = document.customSectionRules
            .distinctBy { listOf(it.packageName, it.detectionType, it.detectionValue) }
            .mapNotNull { entry ->
                val type = DetectionType.entries.firstOrNull { it.name == entry.detectionType }
                    ?: return@mapNotNull null
                if (entry.packageName.isBlank() || entry.detectionValue.isBlank()) {
                    return@mapNotNull null
                }
                val id = CustomSectionRuleRepositoryImpl.ID_PREFIX + UUID.randomUUID().toString()
                val sectionName = entry.sectionName.trim().take(40).ifBlank {
                    entry.detectionValue.substringAfterLast('.').ifBlank { entry.detectionValue }
                }
                val appName = entry.appName.ifBlank { entry.packageName }
                DetectionRule(
                    id = id,
                    packageName = entry.packageName,
                    appName = appName,
                    sectionId = id,
                    sectionName = sectionName,
                    detectionType = type,
                    detectionValue = entry.detectionValue,
                    overlayTitle = entry.overlayTitle.ifBlank { "$sectionName is blocked" },
                    overlaySubtitle = entry.overlaySubtitle.ifBlank {
                        "You recorded this section in Sober. The rest of $appName still works."
                    },
                    activityValue = entry.activityValue
                        ?.takeIf { it.isNotBlank() && type != DetectionType.ACTIVITY },
                    isCustom = true
                ) to entry.enabled
            }

        val sectionRules = document.sectionRules
            .filter { it.isNotBlank() && !it.startsWith(CustomSectionRuleRepositoryImpl.ID_PREFIX) }
            .toSet()
        val enabledCustomSectionIds = customSectionRules.filter { it.second }.map { it.first.id }.toSet()

        // ---- writes ----
        appLimitDao.deleteAll()
        limits.forEach { appLimitDao.insertLimit(it.toEntity()) }

        scheduleRuleDao.deleteAll()
        schedules.forEach { scheduleRuleDao.insertRule(it.toEntity()) }

        preferences.updateCustomRules { customRules.map(CustomRuleCodec::encode).toSet() }
        preferences.setCustomSectionRules(customSectionRules.map { SectionRuleCodec.encode(it.first) }.toSet())
        preferences.setSectionRules(sectionRules + enabledCustomSectionIds)
        preferences.setAppPauseRules(pauseRules)
        preferences.setCategoryLimits(categoryLimits)

        // An absent block means "the default state", not "leave whatever was
        // here": a restore that kept a stale allow-only mode could lock every
        // app on a device whose backup never had one.
        val allowOnly = document.allowOnly
        preferences.setAllowOnly(
            AllowOnlyMode(
                enabled = allowOnly?.enabled ?: false,
                allowedPackages = allowOnly?.allowedPackages.orEmpty().filter { it.isNotBlank() }.toSet()
            )
        )

        val bedtime = document.bedtime
        preferences.setBedtime(
            if (bedtime == null) {
                BedtimeSettings()
            } else {
                BedtimeSettings(
                    enabled = bedtime.enabled,
                    startMinuteOfDay = bedtime.startMinuteOfDay.coerceIn(0, 1439),
                    endMinuteOfDay = bedtime.endMinuteOfDay.coerceIn(0, 1439),
                    allowedPackages = bedtime.allowedPackages.filter { it.isNotBlank() }.toSet()
                )
            }
        )

        val dailyCap = document.dailyCap
        preferences.setDailyScreenTimeLimitMinutes(
            (dailyCap?.minutes ?: SoberPreferences.DEFAULT_DAILY_SCREEN_TIME_LIMIT)
                .coerceAtLeast(0)
        )
        preferences.setDailyCapExemptPackages(
            dailyCap?.exemptPackages.orEmpty().filter { it.isNotBlank() }.toSet()
        )

        return BackupSummary(
            appLimits = limits.size,
            schedules = schedules.size,
            customRules = customRules.size,
            sectionRules = sectionRules.size + customSectionRules.size,
            pauseRules = pauseRules.size,
            categoryLimits = categoryLimits.size
        )
    }
}
