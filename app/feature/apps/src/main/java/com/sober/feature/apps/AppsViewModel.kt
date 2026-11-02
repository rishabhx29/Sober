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
import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.WebsiteAddress
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.AppOpenRepository
import com.sober.core.domain.repository.CustomRuleRepository
import com.sober.core.domain.repository.DetectionRuleCatalog
import com.sober.core.domain.repository.DetectionRuleRepository
import com.sober.core.domain.repository.PauseRulesRepository
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
    val pauseDelaySeconds: Int? = null
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
    private val customRuleRepository: CustomRuleRepository,
    private val pauseRulesRepository: PauseRulesRepository,
    private val appOpenRepository: AppOpenRepository
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps

    /** Catalog sections, keyed by package, for the detail sheet. */
    private val _sectionRules = MutableStateFlow<List<DetectionRule>>(emptyList())
    val sectionRules: StateFlow<List<DetectionRule>> = _sectionRules

    private val _enabledSectionRules = MutableStateFlow<Set<String>>(emptySet())
    val enabledSectionRules: StateFlow<Set<String>> = _enabledSectionRules

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val catalog = runCatching { detectionRuleRepository.load() }
                .getOrDefault(DetectionRuleCatalog.EMPTY)
            _sectionRules.value = catalog.rules
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
            sectionRulePreferencesRepository.setEnabled(ruleId, enabled)
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
            customRuleRepository.add(CustomRuleType.BLOCKED_WEBSITE, domain)
        }
    }

    fun setWebsiteEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { customRuleRepository.setEnabled(id, enabled) }
    }

    fun removeWebsite(id: String) {
        viewModelScope.launch { customRuleRepository.remove(id) }
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
                        pauseDelaySeconds = pauseByPackage[appInfo.packageName]
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
     * an hour with no opens cap. A row with neither is deleted rather than kept
     * at zero, which enforcement would have to special-case forever. The pause
     * rule lives in its own store because it outlives the limit.
     */
    fun saveAppRules(
        packageName: String,
        appName: String,
        limitMinutes: Int,
        openLimit: Int,
        pauseDelaySeconds: Int?
    ) {
        viewModelScope.launch {
            val existing = appLimitRepository.getLimit(packageName)
            val hasBudget = limitMinutes > 0 || openLimit > 0
            when {
                !hasBudget && existing != null -> appLimitRepository.deleteLimit(existing)
                hasBudget && existing != null -> appLimitRepository.updateLimit(
                    existing.copy(
                        dailyLimitMinutes = limitMinutes,
                        dailyOpenLimit = openLimit,
                        isActive = true
                    )
                )
                hasBudget -> appLimitRepository.insertLimit(
                    AppLimit(
                        packageName = packageName,
                        appName = appName,
                        dailyLimitMinutes = limitMinutes,
                        dailyOpenLimit = openLimit
                    )
                )
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
                        pauseDelaySeconds = pauseDelaySeconds
                    )
                } else app
            }
        }
    }

    /** Removes every rule the sheet owns, including the pause, in one action. */
    fun removeAppRules(packageName: String) {
        viewModelScope.launch {
            val existing = appLimitRepository.getLimit(packageName)
            if (existing != null) {
                appLimitRepository.deleteLimit(existing)
            }
            pauseRulesRepository.setRule(packageName, null)
            _apps.value = _apps.value.map { app ->
                if (app.packageName == packageName) {
                    app.copy(
                        limitMinutes = null,
                        openLimit = 0,
                        isLimited = false,
                        pauseDelaySeconds = null
                    )
                } else app
            }
        }
    }
}
