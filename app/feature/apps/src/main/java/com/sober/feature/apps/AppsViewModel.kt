package com.sober.feature.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.common.permissions.PermissionUtils
import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.model.AppUsage
import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.WebsiteAddress
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.CustomRuleRepository
import com.sober.core.domain.repository.DetectionRuleCatalog
import com.sober.core.domain.repository.DetectionRuleRepository
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
    val isLimited: Boolean = false
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
    private val customRuleRepository: CustomRuleRepository
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
                        limitMinutes = limit?.dailyLimitMinutes,
                        isLimited = limit != null && limit.isActive
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

    fun setAppLimit(packageName: String, appName: String, limitMinutes: Int) {
        viewModelScope.launch {
            val existing = appLimitRepository.getLimit(packageName)
            if (existing != null) {
                appLimitRepository.updateLimit(existing.copy(dailyLimitMinutes = limitMinutes, isActive = true))
            } else {
                appLimitRepository.insertLimit(
                    AppLimit(
                        packageName = packageName,
                        appName = appName,
                        dailyLimitMinutes = limitMinutes
                    )
                )
            }
            // Refresh the app list
            _apps.value = _apps.value.map { app ->
                if (app.packageName == packageName) {
                    app.copy(limitMinutes = limitMinutes, isLimited = true)
                } else app
            }
        }
    }

    fun removeAppLimit(packageName: String) {
        viewModelScope.launch {
            val existing = appLimitRepository.getLimit(packageName)
            if (existing != null) {
                appLimitRepository.deleteLimit(existing)
            }
            _apps.value = _apps.value.map { app ->
                if (app.packageName == packageName) {
                    app.copy(limitMinutes = null, isLimited = false)
                } else app
            }
        }
    }
}
