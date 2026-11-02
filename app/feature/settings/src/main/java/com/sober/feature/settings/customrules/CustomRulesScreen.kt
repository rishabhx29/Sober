package com.sober.feature.settings.customrules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.model.WebsiteAddress
import com.sober.core.ui.components.SoberDivider
import com.sober.core.ui.components.SoberQuietButton
import com.sober.core.ui.components.SoberScreenHeader
import com.sober.core.ui.components.SoberSectionHeader
import com.sober.core.ui.components.SoberToggleSwitch
import com.sober.core.ui.theme.SoberTheme

/**
 * The user's own rules: apps they want shut, websites they want covered, and
 * words they want out of their notifications.
 *
 * All three kinds live on one screen because they answer the same question
 * ("what did I ask Sober to hide?"), even though they are enforced in different
 * places — the block policy, the accessibility service and the notification
 * listener respectively.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRulesScreen(
    onBack: () -> Unit,
    viewModel: CustomRulesViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val query by viewModel.pickerQuery.collectAsState()

    val colors = SoberTheme.colors
    val appRows = rows.filter { it.rule.type == CustomRuleType.BLOCKED_APP }
    val keywordRows = rows.filter { it.rule.type == CustomRuleType.NOTIFICATION_KEYWORD }
    val websiteRows = rows.filter { it.rule.type == CustomRuleType.BLOCKED_WEBSITE }

    // null = closed. For the picker an empty string means "add" and a rule id
    // means "change this one"; for the editors, the id is null on add.
    var pickerFor by remember { mutableStateOf<String?>(null) }
    var keywordEditorFor by remember { mutableStateOf<String?>(null) }
    var keywordEditingId by remember { mutableStateOf<String?>(null) }
    var websiteEditorFor by remember { mutableStateOf<String?>(null) }
    var websiteEditingId by remember { mutableStateOf<String?>(null) }
    var actionsFor by remember { mutableStateOf<CustomRule?>(null) }
    val sheetState = rememberModalBottomSheetState()

    if (actionsFor != null) {
        val rule = actionsFor!!
        AlertDialog(
            onDismissRequest = { actionsFor = null },
            containerColor = colors.surface,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = {
                Text(
                    text = when (rule.type) {
                        CustomRuleType.BLOCKED_APP -> "Blocked app"
                        CustomRuleType.NOTIFICATION_KEYWORD -> "Muted word"
                        CustomRuleType.BLOCKED_WEBSITE -> "Blocked website"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = when (rule.type) {
                        CustomRuleType.BLOCKED_APP ->
                            "This app stays closed until you remove it here."
                        CustomRuleType.NOTIFICATION_KEYWORD ->
                            "Notifications containing \u201C${rule.value}\u201D are hidden as they arrive."
                        CustomRuleType.BLOCKED_WEBSITE ->
                            "Pages on ${rule.value} and its subdomains are covered inside the browser."
                    },
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    actionsFor = null
                    when (rule.type) {
                        CustomRuleType.BLOCKED_APP -> pickerFor = rule.id
                        CustomRuleType.NOTIFICATION_KEYWORD -> {
                            keywordEditorFor = rule.value
                            keywordEditingId = rule.id
                        }
                        CustomRuleType.BLOCKED_WEBSITE -> {
                            websiteEditorFor = rule.value
                            websiteEditingId = rule.id
                        }
                    }
                }) {
                    Text("Edit", color = colors.primary, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.remove(rule.id)
                    actionsFor = null
                }) {
                    Text("Remove", color = colors.danger, fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    if (keywordEditorFor != null) {
        KeywordDialog(
            initial = keywordEditorFor.orEmpty(),
            onDismiss = {
                keywordEditorFor = null
                keywordEditingId = null
            },
            onConfirm = { text ->
                val id = keywordEditingId
                if (id == null) viewModel.addKeyword(text) else viewModel.updateValue(id, text)
                keywordEditorFor = null
                keywordEditingId = null
            }
        )
    }

    if (websiteEditorFor != null) {
        WebsiteDialog(
            initial = websiteEditorFor.orEmpty(),
            onDismiss = {
                websiteEditorFor = null
                websiteEditingId = null
            },
            onConfirm = { text ->
                val id = websiteEditingId
                if (id == null) viewModel.addWebsite(text) else viewModel.updateWebsite(id, text)
                websiteEditorFor = null
                websiteEditingId = null
            }
        )
    }

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
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = colors.primary,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SoberScreenHeader(
                title = "Custom rules",
                eyebrow = "Written by you",
                subtitle = "They stay in force until you switch them off."
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            SoberSectionHeader(title = "Blocked apps", index = "01")
            Spacer(modifier = Modifier.height(2.dp))

            if (appRows.isEmpty()) {
                Text(
                    text = "No apps blocked. An app added here stays closed all day, with no end time.",
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                appRows.forEachIndexed { index, row ->
                    RuleRow(
                        title = row.title,
                        subtitle = row.subtitle,
                        checked = row.rule.enabled,
                        onCheckedChange = { viewModel.setEnabled(row.rule.id, it) },
                        onOpen = { actionsFor = row.rule }
                    )
                    if (index != appRows.lastIndex) SoberDivider()
                }
            }
            SoberQuietButton(
                text = "Block an app",
                icon = Icons.Rounded.Add,
                onClick = {
                    viewModel.updatePickerQuery("")
                    pickerFor = ""
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SoberSectionHeader(title = "Blocked websites", index = "02")
            Spacer(modifier = Modifier.height(2.dp))

            if (websiteRows.isEmpty()) {
                Text(
                    text = "No websites blocked. A site added here is covered in the browser, including its subdomains.",
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                websiteRows.forEachIndexed { index, row ->
                    RuleRow(
                        title = row.title,
                        subtitle = row.subtitle,
                        checked = row.rule.enabled,
                        onCheckedChange = { viewModel.setEnabled(row.rule.id, it) },
                        onOpen = { actionsFor = row.rule }
                    )
                    if (index != websiteRows.lastIndex) SoberDivider()
                }
            }
            SoberQuietButton(
                text = "Block a website",
                icon = Icons.Rounded.Add,
                onClick = {
                    websiteEditingId = null
                    websiteEditorFor = ""
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SoberSectionHeader(title = "Muted words", index = "03")
            Spacer(modifier = Modifier.height(2.dp))

            if (keywordRows.isEmpty()) {
                Text(
                    text = "No muted words. Notifications containing a word you add are hidden, whatever app sends them.",
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                keywordRows.forEachIndexed { index, row ->
                    RuleRow(
                        title = row.title,
                        subtitle = row.subtitle,
                        checked = row.rule.enabled,
                        onCheckedChange = { viewModel.setEnabled(row.rule.id, it) },
                        onOpen = { actionsFor = row.rule }
                    )
                    if (index != keywordRows.lastIndex) SoberDivider()
                }
            }
            SoberQuietButton(
                text = "Mute a word",
                icon = Icons.Rounded.Add,
                onClick = {
                    keywordEditingId = null
                    keywordEditorFor = ""
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Muted words need Notification access; blocked apps need nothing extra. " +
                    "Blocked websites need Accessibility access. Calls are never hidden by a keyword.",
                color = colors.textMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }

    val pickerTarget = pickerFor
    if (pickerTarget != null) {
        val editingId = pickerTarget.ifEmpty { null }
        // Already-blocked apps are left out when adding: re-adding one would be
        // a no-op, so offering it would look like the tap failed.
        val blocked = appRows.map { it.rule.value }.toSet()
        val visible = apps.filter { app ->
            (editingId != null || app.packageName !in blocked) &&
                (query.isBlank() || app.name.contains(query, ignoreCase = true))
        }

        ModalBottomSheet(
            onDismissRequest = { pickerFor = null },
            sheetState = sheetState,
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = if (editingId == null) "Block an app" else "Change app",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::updatePickerQuery,
                    placeholder = { Text("Search apps...", color = colors.textMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = colors.textMuted
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.divider,
                        focusedContainerColor = colors.background,
                        unfocusedContainerColor = colors.background
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (visible.isEmpty()) {
                    Text(
                        text = if (apps.isEmpty()) "Loading apps..." else "No matching app.",
                        color = colors.textMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(visible, key = { it.packageName }) { app ->
                            Text(
                                text = app.name,
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (editingId == null) {
                                            viewModel.blockApp(app.packageName)
                                        } else {
                                            viewModel.updateValue(editingId, app.packageName)
                                        }
                                        pickerFor = null
                                    }
                                    .padding(vertical = 14.dp)
                            )
                            SoberDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpen: () -> Unit
) {
    val colors = SoberTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onOpen() }
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = title,
                color = if (checked) colors.textPrimary else colors.textMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 11.sp
            )
        }
        SoberToggleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun KeywordDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = SoberTheme.colors
    var text by remember(initial) { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = if (initial.isBlank()) "Mute a word" else "Edit word",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "Any notification whose title or text contains this word is hidden.",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("e.g. sale", color = colors.textMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.divider,
                        focusedContainerColor = colors.background,
                        unfocusedContainerColor = colors.background,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text(
                    text = "Save",
                    color = if (text.isNotBlank()) colors.primary else colors.textMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
private fun WebsiteDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = SoberTheme.colors
    var text by remember(initial) { mutableStateOf(initial) }
    val normalized = WebsiteAddress.normalizeDomain(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = if (initial.isBlank()) "Block a website" else "Edit website",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a site like example.com. The domain and every subdomain are covered in the browser.",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("example.com", color = colors.textMuted) },
                    singleLine = true,
                    isError = text.isNotBlank() && normalized == null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.divider,
                        focusedContainerColor = colors.background,
                        unfocusedContainerColor = colors.background,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                if (text.isNotBlank() && normalized == null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter a full domain, like example.com.",
                        fontSize = 12.sp,
                        color = colors.danger
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = normalized != null) {
                Text(
                    text = "Save",
                    color = if (normalized != null) colors.primary else colors.textMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
