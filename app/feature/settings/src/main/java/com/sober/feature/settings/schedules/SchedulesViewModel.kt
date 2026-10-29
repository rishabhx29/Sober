package com.sober.feature.settings.schedules

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.common.presets.PresetMode
import com.sober.core.common.presets.PresetResolver
import com.sober.core.domain.model.ScheduleRule
import com.sober.core.domain.model.ScheduleSuggestion
import com.sober.core.domain.model.ScheduleSuggestionKind
import com.sober.core.domain.model.formatTimeOfDay
import com.sober.core.domain.repository.ScheduleRuleRepository
import com.sober.core.domain.repository.ScheduleSuggestionRepository
import com.sober.core.domain.usecase.GetScheduleSuggestionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** One launchable app, for the editor's block list. */
data class ScheduleApp(
    val name: String,
    val packageName: String
)

/**
 * The two schedule presets the PRD asks for. They exist as one-tap templates
 * rather than separate features: the rule they create is an ordinary rule the
 * user can then edit or delete.
 */
enum class ScheduleTemplate(
    val label: String,
    val description: String,
    val ruleName: String,
    val days: List<Int>,
    val startTime: String,
    val endTime: String,
    val preset: PresetMode
) {
    WORK(
        label = "Work Mode",
        description = "Mon–Fri, 9:00–18:00 · social and video apps",
        ruleName = "Work Mode",
        days = listOf(1, 2, 3, 4, 5),
        startTime = "09:00",
        endTime = "18:00",
        preset = PresetMode.WORK
    ),
    SLEEP(
        label = "Sleep Mode",
        description = "Every day, 22:00–08:00 · everything but calls and messages",
        ruleName = "Sleep Mode",
        days = emptyList(),
        startTime = "22:00",
        endTime = "08:00",
        preset = PresetMode.SLEEP
    )
}

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRuleRepository: ScheduleRuleRepository,
    private val scheduleSuggestionRepository: ScheduleSuggestionRepository,
    private val getScheduleSuggestionsUseCase: GetScheduleSuggestionsUseCase
) : ViewModel() {

    val rules: StateFlow<List<ScheduleRule>> = scheduleRuleRepository.getAllRules().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _apps = MutableStateFlow<List<ScheduleApp>>(emptyList())
    val apps: StateFlow<List<ScheduleApp>> = _apps

    private val _suggestions = MutableStateFlow<List<ScheduleSuggestion>>(emptyList())
    val suggestions: StateFlow<List<ScheduleSuggestion>> = _suggestions

    init {
        viewModelScope.launch {
            _apps.value = loadLaunchableApps()
        }
        loadSuggestions()
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            _suggestions.value = runCatching {
                withContext(Dispatchers.IO) { getScheduleSuggestionsUseCase() }
            }.getOrDefault(emptyList())
        }
    }

    /**
     * Launchable apps only, and never Sober itself: a schedule that blocks the
     * app you use to undo it would lock the user out of their own settings.
     */
    private suspend fun loadLaunchableApps(): List<ScheduleApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { it.packageName != context.packageName }
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { ScheduleApp(pm.getApplicationLabel(it).toString(), it.packageName) }
                .sortedBy { it.name.lowercase() }
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(rule: ScheduleRule) {
        viewModelScope.launch {
            if (rule.id == 0) {
                scheduleRuleRepository.insertRule(rule)
            } else {
                scheduleRuleRepository.updateRule(rule)
            }
            loadSuggestions()
        }
    }

    fun setActive(rule: ScheduleRule, active: Boolean) {
        viewModelScope.launch {
            scheduleRuleRepository.updateRule(rule.copy(isActive = active))
        }
    }

    fun delete(rule: ScheduleRule) {
        viewModelScope.launch {
            scheduleRuleRepository.deleteRule(rule)
            loadSuggestions()
        }
    }

    /**
     * Applying a suggestion is the same operation as applying its template:
     * the suggestion only carries the observation, not a second way to build a
     * rule.
     */
    fun applySuggestion(suggestion: ScheduleSuggestion, onResult: (Boolean) -> Unit) {
        val template = when (suggestion.kind) {
            ScheduleSuggestionKind.LATE_NIGHT -> ScheduleTemplate.SLEEP
            ScheduleSuggestionKind.WORK_HOURS -> ScheduleTemplate.WORK
        }
        applyTemplate(template) { applied ->
            if (applied) {
                _suggestions.value = _suggestions.value.filterNot { it.kind == suggestion.kind }
            }
            onResult(applied)
        }
    }

    fun dismissSuggestion(suggestion: ScheduleSuggestion) {
        viewModelScope.launch {
            scheduleSuggestionRepository.dismiss(suggestion.kind)
            _suggestions.value = _suggestions.value.filterNot { it.kind == suggestion.kind }
        }
    }

    /**
     * Creates a template's rule, or refreshes the one with the same name.
     *
     * Re-running a template must not stack a second identical window, but it
     * also must not silently overwrite a rule the user has since edited into
     * something else — matching on the name is the compromise: same name means
     * "this template", anything else is the user's own work.
     */
    fun applyTemplate(template: ScheduleTemplate, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val plan = withContext(Dispatchers.Default) {
                PresetResolver.resolve(context, template.preset)
            }
            if (plan.blockedPackages.isEmpty()) {
                onResult(false)
                return@launch
            }

            val existing = scheduleRuleRepository.getAllRules().first()
                .firstOrNull { it.ruleName == template.ruleName }
            val rule = ScheduleRule(
                id = existing?.id ?: 0,
                ruleName = template.ruleName,
                blockedPackages = plan.blockedPackages,
                daysOfWeek = template.days,
                startTime = template.startTime,
                endTime = template.endTime,
                isActive = true
            )
            if (existing == null) {
                scheduleRuleRepository.insertRule(rule)
            } else {
                scheduleRuleRepository.updateRule(rule)
            }
            loadSuggestions()
            onResult(true)
        }
    }

    companion object {
        fun timeLabel(rule: ScheduleRule): String = buildString {
            append(rule.startTime)
            append(" – ")
            append(rule.endTime)
            append(" (")
            append(daysLabel(rule.daysOfWeek))
            append(")")
        }

        /** "Mon, Tue, Wed", "Weekdays", "Every day". */
        fun daysLabel(days: List<Int>): String {
            if (days.isEmpty() || days.size == 7) return "Every day"
            val sorted = days.distinct().sorted()
            if (sorted == listOf(1, 2, 3, 4, 5)) return "Weekdays"
            if (sorted == listOf(6, 7)) return "Weekends"
            return sorted.joinToString(", ") { DAY_ABBREVIATIONS[it - 1] }
        }

        val DAY_ABBREVIATIONS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        /** "9:00 AM" for the editor's read-out under the drums. */
        fun displayTime(hour: Int, minute: Int): String {
            val suffix = if (hour < 12) "AM" else "PM"
            val hour12 = when (val h = hour % 12) {
                0 -> 12
                else -> h
            }
            return "$hour12:${formatTimeOfDay(hour, minute).substringAfter(':')} $suffix"
        }
    }
}
