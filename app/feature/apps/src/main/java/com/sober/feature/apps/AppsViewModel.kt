package com.sober.feature.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.common.permissions.PermissionUtils
import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.model.AppPauseRule
import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.CapturedCandidate
import com.sober.core.domain.model.CapturedSection
import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.DetectionType
import com.sober.core.domain.model.SectionCaptureRequest
import com.sober.core.domain.model.WebsiteAddress
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.AppOpenRepository
import com.sober.core.domain.repository.AppSessionRepository
import com.sober.core.domain.repository.CustomRuleRepository
import com.sober.core.domain.repository.CustomSectionRuleRepository
import com.sober.core.domain.repository.DetectionRuleCatalog
import com.sober.core.domain.repository.DetectionRuleRepository
import com.sober.core.domain.repository.PauseRulesRepository
import com.sober.core.domain.repository.ProtectionSettingsRepository
import com.sober.core.domain.repository.SectionCaptureRepository
import com.sober.core.domain.repository.SectionRulePreferencesRepository
import com.sober.core.domain.repository.UsageStatsRepository
import com.sober.core.domain.usecase.GetAppUsageHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable?,
    val usageMinutes: Int = 0,
    /** True only when the app actually held the foreground today. */
    val usedToday: Boolean = false,
    val limitMinutes: Int? = null,
    val isLimited: Boolean = false,
    /** 0 means the opens budget is off. */
    val openLimit: Int = 0,
    val openCount: Int = 0,
    /** Null when no pause-before-opening rule is active for this app. */
    val pauseDelaySeconds: Int? = null,
    /** True when every entry starts with the length picker. */
    val sessionTimerEnabled: Boolean = false
)

enum class AppFilter {
    /** Apps the user opened today, which is what makes this screen a tracker. */
    USED,
    LIMITED,
    BLOCKED,
    ALL
}

@HiltViewModel
class AppsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLimitRepository: AppLimitRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val getAppUsageHistoryUseCase: GetAppUsageHistoryUseCase,
    private val detectionRuleRepository: DetectionRuleRepository,
    private val sectionRulePreferencesRepository: SectionRulePreferencesRepository,
    private val customSectionRuleRepository: CustomSectionRuleRepository,
    private val sectionCaptureRepository: SectionCaptureRepository,
    private val customRuleRepository: CustomRuleRepository,
    private val pauseRulesRepository: PauseRulesRepository,
    private val appOpenRepository: AppOpenRepository,
    private val appSessionRepository: AppSessionRepository,
    private val protectionSettingsRepository: ProtectionSettingsRepository
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps

    /** Catalog sections, keyed by package, for the detail sheet. */
    private val _sectionRules = MutableStateFlow<List<DetectionRule>>(emptyList())
    val sectionRules: StateFlow<List<DetectionRule>> = _sectionRules

    // Kept apart so either source can update without dropping the other: the
    // shipped catalog loads once, the user's rules arrive whenever they save.
    private var shippedSectionRules: List<DetectionRule> = emptyList()
    private var customSectionRules: List<DetectionRule> = emptyList()

    private val _enabledSectionRules = MutableStateFlow<Set<String>>(emptySet())
    val enabledSectionRules: StateFlow<Set<String>> = _enabledSectionRules

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled

    /** Non-null while lock mode is on; every rule edit is refused until then. */
    val lockedUntil: StateFlow<Long?> = protectionSettingsRepository.lockedUntil
        .map { until -> until?.takeIf { it > System.currentTimeMillis() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private suspend fun isLocked(): Boolean =
        protectionSettingsRepository.lockedUntil.first()?.let {
            it > System.currentTimeMillis()
        } ?: false

    init {
        viewModelScope.launch {
            customSectionRuleRepository.rules().collect { rules ->
                customSectionRules = rules
                _sectionRules.value = shippedSectionRules + customSectionRules
            }
        }
        viewModelScope.launch {
            val catalog = runCatching { detectionRuleRepository.load() }
                .getOrDefault(DetectionRuleCatalog.EMPTY)
            shippedSectionRules = catalog.rules
            _sectionRules.value = shippedSectionRules + customSectionRules
        }
        viewModelScope.launch {
            sectionRulePreferencesRepository.enabledRuleIds().collect {
                _enabledSectionRules.value = it
            }
        }
        // Pause rules are edited here but enforced in the service; keeping them
        // as a flow means the sheet's toggle reflects a change made elsewhere.
        viewModelScope.launch {
            pauseRulesRepository.rules.collect { rules ->
                val activeByPackage = rules
                    .filter { it.isActive }
                    .associate { it.packageName to it.delaySeconds }
                _apps.value = _apps.value.map { app ->
                    app.copy(pauseDelaySeconds = activeByPackage[app.packageName])
                }
            }
        }
        refreshAccessibilityStatus()
    }

    /**
     * Re-read on resume: the user leaves for system Settings to grant access
     * and comes straight back.
     */
    fun refreshAccessibilityStatus() {
        _isAccessibilityEnabled.value =
            runCatching { PermissionUtils.hasAccessibilityPermission(context) }.getOrDefault(false)
    }

    fun setSectionRuleEnabled(ruleId: String, enabled: Boolean) {
        viewModelScope.launch {
            if (isLocked()) return@launch
            sectionRulePreferencesRepository.setEnabled(ruleId, enabled)
        }
    }

    /** Non-null while a recording is armed; the service fills in the result. */
    val captureRequest: StateFlow<SectionCaptureRequest?> = sectionCaptureRepository.request()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The last screen the armed recording saw, kept for review. */
    val capturedSection: StateFlow<CapturedSection?> = sectionCaptureRepository.capturedSection()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Arms a recording for [packageName]. The screen offers to open the app as
     * a second, explicit step so the request is written before the service can
     * see the target app's first screen.
     */
    fun startSectionCapture(packageName: String) {
        viewModelScope.launch {
            if (isLocked()) return@launch
            sectionCaptureRepository.start(packageName, System.currentTimeMillis())
        }
    }

    /**
     * Called on resume. Only a recording that actually saw the target app ends
     * here: a rotation, or a trip to system Settings, must not stop a recording
     * that has not reached its screen yet.
     */
    fun finishSectionCapture() {
        val request = captureRequest.value ?: return
        if (capturedSection.value?.packageName != request.packageName) return
        viewModelScope.launch { sectionCaptureRepository.finish() }
    }

    fun cancelSectionCapture() {
        viewModelScope.launch { sectionCaptureRepository.clear() }
    }

    /**
     * Turns a reviewed candidate into a rule.
     *
     * The activity the capture saw scopes a tab rule: the same resource id can
     * exist on another screen, and blocking the wrong screen is worse than not
     * blocking. An activity candidate is the match itself, so it is not scoped.
     */
    fun saveCapturedSection(
        appName: String,
        packageName: String,
        candidate: CapturedCandidate,
        sectionName: String
    ) {
        val name = sectionName.trim().take(MAX_SECTION_NAME_LENGTH)
        if (name.isEmpty() || candidate.value.isBlank()) return
        viewModelScope.launch {
            if (isLocked()) return@launch
            val capture = capturedSection.value?.takeIf { it.packageName == packageName }
            customSectionRuleRepository.add(
                DetectionRule(
                    id = "",
                    packageName = packageName,
                    appName = appName,
                    sectionId = "",
                    sectionName = name,
                    detectionType = candidate.type,
                    detectionValue = candidate.value,
                    overlayTitle = "$name is blocked",
                    overlaySubtitle = "You recorded this section in Sober. " +
                        "The rest of $appName still works.",
                    activityValue = when (candidate.type) {
                        DetectionType.ACTIVITY -> null
                        else -> capture?.activityName
                    }
                )
            )
            sectionCaptureRepository.clear()
        }
    }

    fun removeCustomSectionRule(ruleId: String) {
        viewModelScope.launch {
            if (isLocked()) return@launch
            customSectionRuleRepository.remove(ruleId)
        }
    }

    /**
     * Websites the user asked Sober to cover inside a browser.
     *
     * Stored as custom rules rather than a second store: they are the same
     * promise as "block this app", just enforced on the address bar instead of
     * the package, and one list means one place where dedupe and removal live.
     */
    val websiteRules: StateFlow<List<CustomRule>> = customRuleRepository.rules()
        .map { rules -> rules.filter { it.type == CustomRuleType.BLOCKED_WEBSITE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addWebsite(raw: String) {
        val domain = WebsiteAddress.normalizeDomain(raw) ?: return
        viewModelScope.launch {
            if (isLocked()) return@launch
            customRuleRepository.add(CustomRuleType.BLOCKED_WEBSITE, domain)
        }
    }

    fun setWebsiteEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            if (isLocked()) return@launch
            customRuleRepository.setEnabled(id, enabled)
        }
    }

    fun removeWebsite(id: String) {
        viewModelScope.launch {
            if (isLocked()) return@launch
            customRuleRepository.remove(id)
        }
    }

    /** Last 7 days of usage for the app whose detail sheet is open, oldest first. */
    private val _appHistory = MutableStateFlow<List<Int>>(emptyList())
    val appHistory: StateFlow<List<Int>> = _appHistory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFilter = MutableStateFlow(AppFilter.USED)
    val selectedFilter: StateFlow<AppFilter> = _selectedFilter

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Shown in the header, so it counts every used app, not only visible rows. */
    val usedTodayCount: StateFlow<Int> = _apps
        .map { apps -> apps.count { it.usedToday } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val filteredApps: StateFlow<List<InstalledApp>> = combine(
        _apps, _searchQuery, _selectedFilter
    ) { apps, query, filter ->
        apps.filter { app ->
            val matchesQuery = query.isBlank() || app.name.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                AppFilter.USED -> app.usedToday
                AppFilter.LIMITED -> app.isLimited
                AppFilter.BLOCKED -> app.isLimited && app.usageMinutes >= (app.limitMinutes ?: Int.MAX_VALUE)
                AppFilter.ALL -> true
            }
            matchesQuery && matchesFilter
        }.sortedWith(
            // Used apps come first and longest-first everywhere: the list is a
            // usage tracker before it is a settings index. Ties fall back to the
            // name so the order does not shuffle between refreshes.
            compareByDescending<InstalledApp> { it.usedToday }
                .thenByDescending { it.usageMinutes }
                .thenBy { it.name.lowercase() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: AppFilter) {
        _selectedFilter.value = filter
    }

    /** Loads the 7-day chart for the app whose detail sheet just opened. */
    fun loadHistory(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _appHistory.value = getAppUsageHistoryUseCase(packageName, days = 7)
        }
    }

    fun loadApps(pm: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val usageList = usageStatsRepository.getUsageStatsForToday()
                val usageMap = usageList.associateBy { it.packageName }
                val limits = appLimitRepository.getAllLimits().first()
                val limitMap = limits.associateBy { it.packageName }
                // One grouped query for the whole list rather than one per app:
                // the counts only feed a row label, so a snapshot is enough and
                // loadApps already re-runs whenever the screen resumes.
                val openCounts = try {
                    appOpenRepository.observeTodayCounts(System.currentTimeMillis()).first()
                } catch (e: Exception) {
                    emptyMap()
                }
                val pauseByPackage = pauseRulesRepository.rules.first()
                    .filter { it.isActive }
                    .associate { it.packageName to it.delaySeconds }

                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val appList = installedApps.filter { appInfo ->
                    // Sober cannot block itself, so a row for it would offer a
                    // switch that does nothing — and it is always "used today".
                    appInfo.packageName != context.packageName &&
                        pm.getLaunchIntentForPackage(appInfo.packageName) != null
                }.map { appInfo ->
                    val usage = usageMap[appInfo.packageName]
                    val usageMillis = usage?.totalTimeInForeground ?: 0L
                    val limit = limitMap[appInfo.packageName]
                    InstalledApp(
                        name = pm.getApplicationLabel(appInfo).toString(),
                        packageName = appInfo.packageName,
                        icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null },
                        // Photos and other system apps appear in queryUsageStats
                        // with zero foreground time. Listing them in the used
                        // view would claim a visit that never happened.
                        usageMinutes = (usageMillis / 60000).toInt(),
                        usedToday = usageMillis > 0,
                        limitMinutes = limit?.dailyLimitMinutes?.takeIf { it > 0 },
                        isLimited = limit != null && limit.isActive,
                        openLimit = limit?.dailyOpenLimit ?: 0,
                        openCount = openCounts[appInfo.packageName] ?: 0,
                        pauseDelaySeconds = pauseByPackage[appInfo.packageName],
                        sessionTimerEnabled = limit?.sessionTimerEnabled == true
                    )
                }.sortedBy { it.name }

                _apps.value = appList
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    /**
     * Enables or disables an existing limit. The minutes are kept, so turning a
     * limit back on does not ask the user to re-enter it, and the enforcement
     * layer treats an inactive limit as no limit at all.
     *
     * A package with no limit is a no-op: the row only offers this switch when
     * there is something to switch.
     */
    fun setLimitEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            if (isLocked()) return@launch
            val existing = appLimitRepository.getLimit(packageName) ?: return@launch
            if (existing.isActive == enabled) return@launch
            appLimitRepository.updateLimit(existing.copy(isActive = enabled))
            _apps.value = _apps.value.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isLimited = enabled)
                } else app
            }
        }
    }

    /**
     * Saves every rule the detail sheet owns in one pass.
     *
     * Minutes and opens are separate budgets but one row, because a user may
     * want either alone: ten opens with no time cap is a valid rule, and so is
     * an hour with no opens cap. The session timer is a third, independent
     * rule on the same row: it needs no minutes or opens of its own, so the
     * "has any rule" test includes it. A row with nothing at all is deleted
     * rather than kept at zero, which enforcement would have to special-case
     * forever. The pause rule lives in its own store because it outlives the
     * limit.
     */
    fun saveAppRules(
        packageName: String,
        appName: String,
        limitMinutes: Int,
        openLimit: Int,
        pauseDelaySeconds: Int?,
        sessionTimerEnabled: Boolean
    ) {
        viewModelScope.launch {
            if (isLocked()) return@launch
            val existing = appLimitRepository.getLimit(packageName)
            val hasBudget = limitMinutes > 0 || openLimit > 0 || sessionTimerEnabled
            when {
                !hasBudget && existing != null -> appLimitRepository.deleteLimit(existing)
                hasBudget && existing != null -> appLimitRepository.updateLimit(
                    existing.copy(
                        dailyLimitMinutes = limitMinutes,
                        dailyOpenLimit = openLimit,
                        isActive = true,
                        sessionTimerEnabled = sessionTimerEnabled
                    )
                )
                hasBudget -> appLimitRepository.insertLimit(
                    AppLimit(
                        packageName = packageName,
                        appName = appName,
                        dailyLimitMinutes = limitMinutes,
                        dailyOpenLimit = openLimit,
                        sessionTimerEnabled = sessionTimerEnabled
                    )
                )
            }

            // Switching the timer off must also end the current session: a
            // stored record would otherwise restart the rule the moment it is
            // switched back on, at a length the user never picked for today.
            if (!sessionTimerEnabled) {
                appSessionRepository.clear(packageName)
            }

            pauseRulesRepository.setRule(
                packageName,
                pauseDelaySeconds?.let { AppPauseRule(packageName = packageName, delaySeconds = it) }
            )

            _apps.value = _apps.value.map { app ->
                if (app.packageName == packageName) {
                    app.copy(
                        limitMinutes = limitMinutes.takeIf { it > 0 },
                        openLimit = openLimit,
                        isLimited = hasBudget,
                        pauseDelaySeconds = pauseDelaySeconds,
                        sessionTimerEnabled = sessionTimerEnabled
                    )
                } else app
            }
        }
    }

    /** Removes every rule the sheet owns, including the pause, in one action. */
    fun removeAppRules(packageName: String) {
        viewModelScope.launch {
            if (isLocked()) return@launch
            val existing = appLimitRepository.getLimit(packageName)
            if (existing != null) {
                appLimitRepository.deleteLimit(existing)
            }
            pauseRulesRepository.setRule(packageName, null)
            appSessionRepository.clear(packageName)
            _apps.value = _apps.value.map { app ->
                if (app.packageName == packageName) {
                    app.copy(
                        limitMinutes = null,
                        openLimit = 0,
                        isLimited = false,
                        pauseDelaySeconds = null,
                        sessionTimerEnabled = false
                    )
                } else app
            }
        }
    }

    private companion object {
        /** Long enough for a real section name, short enough for the overlay. */
        const val MAX_SECTION_NAME_LENGTH = 40
    }
}
