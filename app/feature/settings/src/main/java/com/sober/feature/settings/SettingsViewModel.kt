package com.sober.feature.settings

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.model.AllowOnlyMode
import com.sober.core.domain.model.AppCategory
import com.sober.core.domain.model.BedtimeSettings
import com.sober.core.domain.model.CategoryLimit
import com.sober.core.domain.repository.AppCategoryRepository
import com.sober.core.domain.repository.BackupRepository
import com.sober.core.domain.repository.BlockableApp
import com.sober.core.domain.repository.BlockableAppsRepository
import com.sober.core.domain.repository.PinRepository
import com.sober.core.domain.repository.ProtectionSettingsRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.repository.AlertKind
import com.sober.core.domain.repository.NotificationPreferencesRepository
import com.sober.core.domain.repository.StreakPreferencesRepository
import com.sober.core.domain.repository.StreakRule
import com.sober.core.domain.repository.ThemeMode
import com.sober.core.domain.repository.ThemePreferencesRepository
import com.sober.core.domain.usecase.ExportUsageCsvUseCase
import com.sober.core.domain.usecase.SetPinUseCase
import com.sober.core.domain.usecase.VerifyPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class GateState(
    val hasPin: Boolean,
    val sessionRunning: Boolean,
    /** Non-null while lock mode is on, until this moment. */
    val lockedUntil: Long? = null
) {
    val lockActive: Boolean
        get() = lockedUntil != null && lockedUntil > System.currentTimeMillis()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soberPreferences: SoberPreferences,
    private val sessionStateRepository: SessionStateRepository,
    pinRepository: PinRepository,
    private val notificationPreferences: NotificationPreferencesRepository,
    private val streakPreferences: StreakPreferencesRepository,
    private val themePreferences: ThemePreferencesRepository,
    private val blockableAppsRepository: BlockableAppsRepository,
    private val appCategoryRepository: AppCategoryRepository,
    private val protectionSettingsRepository: ProtectionSettingsRepository,
    private val setPinUseCase: SetPinUseCase,
    private val verifyPinUseCase: VerifyPinUseCase,
    private val exportUsageCsvUseCase: ExportUsageCsvUseCase,
    private val backupRepository: BackupRepository
) : ViewModel() {

    /**
     * Apps whose launch counts as screen time. Loaded once per screen: the
     * installed set changes rarely and the query walks PackageManager.
     */
    private val _capApps = MutableStateFlow<List<BlockableApp>>(emptyList())
    val capApps: StateFlow<List<BlockableApp>> = _capApps

    /**
     * How many installed apps fall in each budgetable category. Resolved once
     * per screen like [capApps]; the counts only change when apps are installed.
     */
    private val _categoryCounts = MutableStateFlow<Map<AppCategory, Int>>(emptyMap())
    val categoryCounts: StateFlow<Map<AppCategory, Int>> = _categoryCounts

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _capApps.value = runCatching { blockableAppsRepository.blockableApps() }
                .getOrDefault(emptyList())
            _categoryCounts.value = AppCategory.entries.associateWith { category ->
                runCatching { appCategoryRepository.getPackagesInCategory(category).size }
                    .getOrDefault(0)
            }
        }
    }

    /** Apps the user removed from the overall cap. */
    val capExemptPackages: StateFlow<Set<String>> =
        sessionStateRepository.dailyCapExemptPackages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    fun setAppExemptFromCap(packageName: String, exempt: Boolean) {
        viewModelScope.launch {
            sessionStateRepository.setDailyCapExempt(packageName, exempt)
        }
    }

    /** The overall daily screen-time cap, enforced by the core service. */
    val dailyScreenTimeLimitMinutes: StateFlow<Int> =
        soberPreferences.dailyScreenTimeLimitMinutes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SoberPreferences.DEFAULT_DAILY_SCREEN_TIME_LIMIT
        )

    /**
     * Everything the lock gate needs, or null while the stored state is still
     * loading.
     *
     * A pair of independent StateFlows would start at `false`/`false`, which
     * renders the settings body for a frame before the real values arrive — long
     * enough to tap a limit button while a session is supposed to be locked.
     * A single nullable state makes "not known yet" explicit and keeps the body
     * off screen until it is.
     */
    val gate: StateFlow<GateState?> = combine(
        pinRepository.hasPin,
        sessionStateRepository.activeSessionId,
        protectionSettingsRepository.lockedUntil
    ) { hasPin, activeSessionId, lockedUntil ->
        GateState(
            hasPin = hasPin,
            sessionRunning = activeSessionId != null,
            lockedUntil = lockedUntil?.takeIf { it > System.currentTimeMillis() }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    /** Lock mode for [hours]; a part-hour is rounded up by the caller's choice. */
    fun lockFor(hours: Int) {
        viewModelScope.launch {
            protectionSettingsRepository.setLockedUntil(
                System.currentTimeMillis() + hours * 60L * 60_000L
            )
        }
    }

    /** Ends lock mode. Only reachable from the settings body, which needs the PIN first. */
    fun clearLock() {
        viewModelScope.launch { protectionSettingsRepository.setLockedUntil(null) }
    }

    // -- Stricter modes --

    val allowOnly: StateFlow<AllowOnlyMode> = protectionSettingsRepository.allowOnly.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AllowOnlyMode()
    )

    val bedtime: StateFlow<BedtimeSettings> = protectionSettingsRepository.bedtime.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BedtimeSettings()
    )

    /**
     * Category budgets as a lookup. A stored budget of zero would mean "no time
     * allowed", which the engine treats as no budget at all for categories, so
     * zero is removed at the source instead of shown as an active rule.
     */
    val categoryLimitMinutes: StateFlow<Map<AppCategory, Int>> =
        protectionSettingsRepository.categoryLimits
            .map { limits ->
                limits.filter { it.isActive && it.dailyLimitMinutes > 0 }
                    .associate { it.category to it.dailyLimitMinutes }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )

    /** Passing zero removes the category's budget. */
    fun setCategoryLimit(category: AppCategory, minutes: Int) {
        viewModelScope.launch {
            protectionSettingsRepository.setCategoryLimit(
                category,
                if (minutes <= 0) null else CategoryLimit(category, minutes)
            )
        }
    }

    fun setAllowOnlyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = protectionSettingsRepository.allowOnly.first()
            protectionSettingsRepository.setAllowOnly(current.copy(enabled = enabled))
        }
    }

    fun setAllowOnlyPackages(packages: Set<String>) {
        viewModelScope.launch {
            val current = protectionSettingsRepository.allowOnly.first()
            protectionSettingsRepository.setAllowOnly(
                current.copy(allowedPackages = packages)
            )
        }
    }

    fun setBedtimeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = protectionSettingsRepository.bedtime.first()
            protectionSettingsRepository.setBedtime(current.copy(enabled = enabled))
        }
    }

    fun setBedtimeWindow(startMinuteOfDay: Int, endMinuteOfDay: Int) {
        viewModelScope.launch {
            val current = protectionSettingsRepository.bedtime.first()
            protectionSettingsRepository.setBedtime(
                current.copy(
                    startMinuteOfDay = startMinuteOfDay,
                    endMinuteOfDay = endMinuteOfDay
                )
            )
        }
    }

    fun setBedtimeAllowedPackages(packages: Set<String>) {
        viewModelScope.launch {
            val current = protectionSettingsRepository.bedtime.first()
            protectionSettingsRepository.setBedtime(
                current.copy(allowedPackages = packages)
            )
        }
    }

    fun setDailyScreenTimeLimit(minutes: Int) {
        viewModelScope.launch {
            soberPreferences.setDailyScreenTimeLimitMinutes(minutes)
        }
    }

    /**
     * Which optional alerts may be posted. Every kind starts enabled, matching
     * the repository's unset default. Built from [AlertKind.entries] so a new
     * alert kind cannot silently go missing from this map.
     */
    val alerts: StateFlow<Map<AlertKind, Boolean>> = combine(
        AlertKind.entries.map { notificationPreferences.isEnabled(it) }
    ) { values ->
        AlertKind.entries.zip(values.toList()).toMap()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AlertKind.entries.associateWith { true }
    )

    fun setAlertEnabled(kind: AlertKind, enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setEnabled(kind, enabled)
        }
    }

    val streakRule: StateFlow<StreakRule> = streakPreferences.rule().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StreakRule.UNDER_LIMITS
    )

    fun setStreakRule(rule: StreakRule) {
        viewModelScope.launch {
            streakPreferences.setRule(rule)
        }
    }

    val themeMode: StateFlow<ThemeMode> = themePreferences.mode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setMode(mode)
        }
    }

    /** The line the user wrote for the block screen; empty until they add one. */
    val yourReason: StateFlow<String> = protectionSettingsRepository.yourReason.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )

    fun setYourReason(reason: String) {
        viewModelScope.launch {
            protectionSettingsRepository.setYourReason(reason)
        }
    }

    /**
     * Sets a PIN, or replaces the current one when [currentPin] is supplied.
     * The current PIN is re-verified here rather than in the composable so the
     * check cannot be bypassed by another caller of the same ViewModel.
     */
    fun savePin(currentPin: String?, newPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (currentPin != null && !verifyPinUseCase(currentPin)) {
                onResult(false)
                return@launch
            }
            onResult(setPinUseCase(newPin))
        }
    }

    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(verifyPinUseCase(pin))
        }
    }

    /**
     * Writes the CSV to cache and hands back a shareable URI.
     *
     * The file lives in `cacheDir/exports` rather than files/ or external
     * storage: it is a hand-off to another app, not something Sober should keep
     * or back up, and Android is free to reclaim it. Previous exports are
     * deleted first so repeated taps cannot pile up copies of the user's usage
     * data in a readable location.
     */
    fun exportUsageCsv(onResult: (Uri?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val csv = exportUsageCsvUseCase()
                val file = withContext(Dispatchers.IO) { writeExport(csv) }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                onResult(uri, null)
            } catch (e: Exception) {
                // File IO and FileProvider config can both fail on a real
                // device; the screen needs a message, not a crash.
                onResult(null, "Couldn't create the export file.")
            }
        }
    }

    private fun writeExport(csv: String): File {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return File(dir, "sober-usage-$day.csv").apply { writeText(csv) }
    }

    /**
     * Writes the rules to the file the user picked in the system picker.
     *
     * SAF rather than a share sheet: a backup is a file the user keeps, so it
     * goes wherever they choose instead of being handed to another app.
     */
    fun exportBackup(target: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val json = backupRepository.export()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(target)?.use { stream ->
                        stream.write(json.toByteArray())
                    } ?: error("No output stream for $target")
                }
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, "Couldn't write the backup file.")
            }
        }
    }

    /**
     * Reads the picked file and replaces the device's rules with it.
     *
     * The size cap exists because this reads an arbitrary file the user chose:
     * a backup is a few kilobytes, and buffering a much larger one would only
     * waste memory before the format check rejected it anyway.
     */
    fun restoreBackup(source: Uri, onResult: (String?, String?) -> Unit) {
        viewModelScope.launch {
            val json = try {
                withContext(Dispatchers.IO) { readBackup(source) }
            } catch (e: Exception) {
                onResult(null, "Couldn't read that file.")
                return@launch
            }
            if (json == null) {
                onResult(null, "That file is too large to be a Sober backup.")
                return@launch
            }

            try {
                val summary = backupRepository.restore(json)
                onResult(
                    if (summary.totalRules == 0) {
                        "Restored. The backup had no rules in it."
                    } else {
                        "Restored ${summary.totalRules} " +
                            (if (summary.totalRules == 1) "rule." else "rules.")
                    },
                    null
                )
            } catch (e: IllegalArgumentException) {
                onResult(null, e.message ?: "That file isn't a Sober backup.")
            } catch (e: Exception) {
                onResult(null, "Couldn't restore that file.")
            }
        }
    }

    /** Null when the file exceeds [MAX_BACKUP_BYTES]. */
    private fun readBackup(source: Uri): String? {
        context.contentResolver.openInputStream(source)?.use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(chunk)
                if (read < 0) break
                buffer.write(chunk, 0, read)
                if (buffer.size() > MAX_BACKUP_BYTES) return null
            }
            return buffer.toByteArray().toString(Charsets.UTF_8)
        }
        error("No input stream for $source")
    }

    companion object {
        const val MIN_PIN_LENGTH = PinRepository.MIN_LENGTH
        const val MAX_PIN_LENGTH = PinRepository.MAX_LENGTH
        const val EXPORT_DIR = "exports"
        private const val MAX_BACKUP_BYTES = 4 * 1024 * 1024
    }
}
