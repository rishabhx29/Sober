package com.sober.feature.settings.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sober.core.domain.model.ScheduleRule
import com.sober.core.domain.model.ScheduleSuggestion
import com.sober.core.domain.model.formatTimeOfDay
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.components.SoberCard
import com.sober.core.ui.components.SoberDivider
import com.sober.core.ui.components.SoberDrumPicker
import com.sober.core.ui.components.SoberQuietButton
import com.sober.core.ui.components.SoberScreenHeader
import com.sober.core.ui.components.SoberSectionHeader
import com.sober.core.ui.components.SoberToggleSwitch
import com.sober.core.ui.theme.SoberTheme

/**
 * Schedule editor state. Kept out of the ViewModel deliberately: it is
 * unsaved scratch input, and a ViewModel-held draft would have to be cleared
 * on cancel and would survive a back-then-forward navigation.
 */
private data class ScheduleDraft(
    val id: Int = 0,
    val name: String = "",
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 18,
    val endMinute: Int = 0,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5),
    val packages: Set<String> = emptySet()
) {
    val startTime: String get() = formatTimeOfDay(startHour, startMinute)
    val endTime: String get() = formatTimeOfDay(endHour, endMinute)
    val timesValid: Boolean get() = startTime != endTime
    val canSave: Boolean
        get() = name.isNotBlank() && packages.isNotEmpty() && timesValid

    fun toRule() = ScheduleRule(
        id = id,
        ruleName = name.trim(),
        blockedPackages = packages.sorted(),
        daysOfWeek = days.sorted(),
        startTime = startTime,
        endTime = endTime,
        isActive = true
    )
}

private fun ScheduleRule.toDraft() = ScheduleDraft(
    id = id,
    name = ruleName,
    startHour = startTime.substringBefore(':').toIntOrNull() ?: 9,
    startMinute = startTime.substringAfter(':').toIntOrNull() ?: 0,
    endHour = endTime.substringBefore(':').toIntOrNull() ?: 18,
    endMinute = endTime.substringAfter(':').toIntOrNull() ?: 0,
    days = daysOfWeek.toSet(),
    packages = blockedPackages.toSet()
)

/**
 * Recurring block windows.
 *
 * Enforcement is not scheduled from here: the core service already re-evaluates
 * blocking policy every poll, so a window opening or closing takes effect at the
 * next tick without an alarm. That also means a window that crosses midnight, or
 * one shifted by a clock or time-zone change, needs no special handling — each
 * poll asks "is this rule active *now*" with the current wall clock.
 */
@Composable
fun SchedulesScreen(
    onBack: () -> Unit,
    viewModel: SchedulesViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    var draft by remember { mutableStateOf<ScheduleDraft?>(null) }
    var templateError by remember { mutableStateOf<String?>(null) }

    val colors = SoberTheme.colors
    val editing = draft

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = if (editing == null) "Back" else "Cancel editing",
                tint = colors.primary,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { if (editing == null) onBack() else draft = null }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SoberScreenHeader(
                title = if (editing == null) {
                    "Schedules"
                } else if (editing.id == 0) {
                    "New schedule"
                } else {
                    "Edit schedule"
                },
                eyebrow = if (editing == null) "Automate" else "Schedule",
                subtitle = if (editing == null) {
                    "Block apps automatically during set times, every week."
                } else {
                    null
                }
            )
        }

        if (editing == null) {
            ScheduleList(
                rules = rules,
                suggestions = suggestions,
                templateError = templateError,
                onToggle = { rule, active -> viewModel.setActive(rule, active) },
                onEdit = { draft = it.toDraft() },
                onDelete = { viewModel.delete(it) },
                onNew = { draft = ScheduleDraft() },
                onTemplate = { template ->
                    templateError = null
                    viewModel.applyTemplate(template) { applied ->
                        if (!applied) {
                            templateError = "No matching apps on this device for ${template.label}."
                        }
                    }
                },
                onApplySuggestion = { suggestion ->
                    templateError = null
                    viewModel.applySuggestion(suggestion) { applied ->
                        if (!applied) {
                            templateError = "No matching apps on this device for ${suggestion.title}."
                        }
                    }
                },
                onDismissSuggestion = { viewModel.dismissSuggestion(it) }
            )
        } else {
            ScheduleEditor(
                draft = editing,
                apps = apps,
                onDraftChange = { draft = it },
                onSave = {
                    viewModel.save(editing.toRule())
                    draft = null
                }
            )
        }
    }
}

@Composable
private fun ScheduleList(
    rules: List<ScheduleRule>,
    suggestions: List<ScheduleSuggestion>,
    templateError: String?,
    onToggle: (ScheduleRule, Boolean) -> Unit,
    onEdit: (ScheduleRule) -> Unit,
    onDelete: (ScheduleRule) -> Unit,
    onNew: () -> Unit,
    onTemplate: (ScheduleTemplate) -> Unit,
    onApplySuggestion: (ScheduleSuggestion) -> Unit,
    onDismissSuggestion: (ScheduleSuggestion) -> Unit
) {
    val colors = SoberTheme.colors

    // Suggestions only appear once usage exists, so Templates must not be
    // "02" on a fresh install where there is no "01" above it.
    val hasSuggestions = suggestions.isNotEmpty()
    fun sectionIndex(base: Int): String =
        (if (hasSuggestions) base else base - 1).toString().padStart(2, '0')

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        if (hasSuggestions) {
            SoberSectionHeader(title = "Suggestions", index = "01")
            Spacer(modifier = Modifier.height(4.dp))

            suggestions.forEach { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = suggestion.title,
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = suggestion.message,
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    SoberQuietButton(
                        text = "Apply",
                        onClick = { onApplySuggestion(suggestion) }
                    )
                    SoberQuietButton(
                        text = "Dismiss",
                        onClick = { onDismissSuggestion(suggestion) }
                    )
                }
                SoberDivider()
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SoberSectionHeader(title = "Templates", index = sectionIndex(2))
        Spacer(modifier = Modifier.height(4.dp))

        ScheduleTemplate.entries.forEach { template ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.label,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = template.description,
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                SoberQuietButton(
                    text = "Use",
                    onClick = { onTemplate(template) }
                )
            }
            SoberDivider()
        }

        if (templateError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = templateError,
                color = colors.danger,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Your schedules", index = sectionIndex(3))
        Spacer(modifier = Modifier.height(4.dp))

        if (rules.isEmpty()) {
            Text(
                text = "No schedules yet. Without one, blocking follows your app limits " +
                    "and any focus session.",
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            rules.forEach { rule ->
                ScheduleRuleRow(
                    rule = rule,
                    onToggle = { onToggle(rule, it) },
                    onEdit = { onEdit(rule) },
                    onDelete = { onDelete(rule) }
                )
                SoberDivider()
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SoberButton(text = "New schedule", onClick = onNew)
    }
}

@Composable
private fun ScheduleRuleRow(
    rule: ScheduleRule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = SoberTheme.colors

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.ruleName,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = SchedulesViewModel.timeLabel(rule) + " · " +
                        if (rule.blockedPackages.isEmpty()) {
                            "No apps selected"
                        } else {
                            "${rule.blockedPackages.size} app${if (rule.blockedPackages.size == 1) "" else "s"}"
                        },
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            SoberToggleSwitch(
                checked = rule.isActive,
                onCheckedChange = onToggle
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SoberQuietButton(text = "Edit", onClick = onEdit)
            TextButton(onClick = onDelete) {
                Text("Delete", color = colors.danger, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ScheduleEditor(
    draft: ScheduleDraft,
    apps: List<ScheduleApp>,
    onDraftChange: (ScheduleDraft) -> Unit,
    onSave: () -> Unit
) {
    val colors = SoberTheme.colors
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        SoberSectionHeader(title = "Name", index = "01")

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it.take(40))) },
            placeholder = { Text("Work Mode", fontSize = 15.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.cardBorder,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.primary,
                focusedPlaceholderColor = colors.textMuted,
                unfocusedPlaceholderColor = colors.textMuted
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Days", index = "02")

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SchedulesViewModel.DAY_ABBREVIATIONS.forEachIndexed { index, label ->
                val day = index + 1
                val selected = day in draft.days
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) colors.primary else colors.background)
                        .clickable {
                            val next = if (selected) draft.days - day else draft.days + day
                            onDraftChange(draft.copy(days = next))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) colors.onPrimary else colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (draft.days.isEmpty()) {
                "Every day"
            } else {
                SchedulesViewModel.daysLabel(draft.days.toList())
            },
            color = colors.textMuted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Window", index = "03")

        Spacer(modifier = Modifier.height(2.dp))

        TimeRow(
            label = "From",
            hour = draft.startHour,
            minute = draft.startMinute,
            onChange = { h, m -> onDraftChange(draft.copy(startHour = h, startMinute = m)) }
        )
        SoberDivider()
        TimeRow(
            label = "To",
            hour = draft.endHour,
            minute = draft.endMinute,
            onChange = { h, m -> onDraftChange(draft.copy(endHour = h, endMinute = m)) }
        )

        if (!draft.timesValid) {
            Text(
                text = "Start and end can't be the same time.",
                color = colors.danger,
                fontSize = 12.sp
            )
        } else if (draft.endTime < draft.startTime) {
            Text(
                text = "Crosses midnight — the window ends the next morning.",
                color = colors.textMuted,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(
            title = "Apps to block",
            index = "04",
            action = {
                Text(
                    text = "${draft.packages.size} selected",
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps", fontSize = 14.sp) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.cardBorder,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.primary,
                focusedPlaceholderColor = colors.textMuted,
                unfocusedPlaceholderColor = colors.textMuted
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        val visibleApps = apps.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true)
        }

        // Bounded height so this list scrolls on its own inside the page
        // instead of being measured with infinite height, which Compose cannot
        // lay out lazily.
        SoberCard(modifier = Modifier.fillMaxWidth()) {
            if (visibleApps.isEmpty()) {
                Text(
                    text = if (apps.isEmpty()) "Loading apps…" else "No apps match.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        AppCheckRow(
                            app = app,
                            checked = app.packageName in draft.packages,
                            onCheckedChange = { checked ->
                                val next = if (checked) {
                                    draft.packages + app.packageName
                                } else {
                                    draft.packages - app.packageName
                                }
                                onDraftChange(draft.copy(packages = next))
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoberButton(
            text = "Save schedule",
            onClick = onSave,
            enabled = draft.canSave
        )

        if (!draft.canSave) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    draft.name.isBlank() -> "Give the schedule a name."
                    draft.packages.isEmpty() -> "Pick at least one app to block."
                    else -> "Fix the window above to save."
                },
                color = colors.textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TimeRow(
    label: String,
    hour: Int,
    minute: Int,
    onChange: (Int, Int) -> Unit
) {
    val colors = SoberTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(0.9f)) {
            Text(
                text = label,
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = SchedulesViewModel.displayTime(hour, minute),
                color = colors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.weight(1.4f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SoberDrumPicker(
                value = hour,
                range = 0..23,
                onValueChange = { onChange(it, minute) },
                formatter = { "%02d".format(it) },
                modifier = Modifier.weight(1f)
            )
            Text(":", color = colors.textMuted, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            SoberDrumPicker(
                value = minute,
                range = 0..59,
                onValueChange = { onChange(hour, it) },
                formatter = { "%02d".format(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AppCheckRow(
    app: ScheduleApp,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = SoberTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = colors.primary,
                checkmarkColor = colors.onPrimary,
                uncheckedColor = colors.textMuted
            )
        )
        Text(
            text = app.name,
            color = colors.textPrimary,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
