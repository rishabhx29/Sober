package com.sober.feature.settings.customrules

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.repository.CustomRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A row as the list renders it: the rule plus what to call it. */
data class CustomRuleRow(
    val rule: CustomRule,
    val title: String,
    val subtitle: String
)

/** One launchable app, for the "block an app" picker. */
data class PickableApp(
    val name: String,
    val packageName: String
)

@HiltViewModel
class CustomRulesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val customRuleRepository: CustomRuleRepository
) : ViewModel() {

    /** Display names resolved once per process; the label rarely changes. */
    private val labelCache = mutableMapOf<String, String>()

    private val _apps = MutableStateFlow<List<PickableApp>>(emptyList())
    val apps: StateFlow<List<PickableApp>> = _apps

    private val _pickerQuery = MutableStateFlow("")
    val pickerQuery: StateFlow<String> = _pickerQuery

    val rows: StateFlow<List<CustomRuleRow>> =
        combine(customRuleRepository.rules(), _apps) { rules, apps ->
            val labels = apps.associate { it.packageName to it.name }
            rules.map { rule ->
                when (rule.type) {
                    CustomRuleType.BLOCKED_APP -> CustomRuleRow(
                        rule = rule,
                        title = labels[rule.value] ?: labelCache[rule.value] ?: rule.value,
                        subtitle = "Always blocked"
                    )
                    CustomRuleType.NOTIFICATION_KEYWORD -> CustomRuleRow(
                        rule = rule,
                        title = "\u201C${rule.value}\u201D",
                        subtitle = "Notifications containing this word are hidden"
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val installed = runCatching {
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                    .filter { it.packageName != context.packageName }
                    .map { info ->
                        val label = runCatching { pm.getApplicationLabel(info).toString() }
                            .getOrDefault(info.packageName)
                        labelCache[info.packageName] = label
                        PickableApp(name = label, packageName = info.packageName)
                    }
                    .sortedBy { it.name.lowercase() }
            }.getOrDefault(emptyList())

            // Assigning a StateFlow is thread-safe; no main hop needed.
            _apps.value = installed
        }
    }

    fun updatePickerQuery(query: String) {
        _pickerQuery.value = query
    }

    fun blockApp(packageName: String) {
        viewModelScope.launch {
            customRuleRepository.add(CustomRuleType.BLOCKED_APP, packageName)
        }
    }

    fun addKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            customRuleRepository.add(CustomRuleType.NOTIFICATION_KEYWORD, trimmed)
        }
    }

    /**
     * "Edit" means a different value: another package for an app rule, another
     * word for a keyword. The rule keeps its id, so any toggle state and its
     * place in the list stay put.
     */
    fun updateValue(id: String, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { customRuleRepository.updateValue(id, trimmed) }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { customRuleRepository.setEnabled(id, enabled) }
    }

    fun remove(id: String) {
        viewModelScope.launch { customRuleRepository.remove(id) }
    }
}
