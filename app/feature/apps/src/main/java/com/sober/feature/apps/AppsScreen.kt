package com.sober.feature.apps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sober.core.common.permissions.PermissionUtils
import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.DetectionType
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.components.SoberDrumPicker
import com.sober.core.ui.components.SoberMiniBarChart
import com.sober.core.ui.components.SoberProgressBar
import com.sober.core.ui.components.SoberSectionLabel
import com.sober.core.ui.components.SoberSegmentedControl
import com.sober.core.ui.components.SoberToggleSwitch
import com.sober.core.ui.theme.SoberTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val apps by viewModel.filteredApps.collectAsState()
    val appHistory by viewModel.appHistory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sectionRules by viewModel.sectionRules.collectAsState()
    val enabledSectionRules by viewModel.enabledSectionRules.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()

    // Bottom sheet state
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var sheetLimitHours by remember { mutableIntStateOf(1) }
    var sheetLimitMinutes by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        viewModel.loadApps(context.packageManager)
    }

    // The Accessibility grant lives in system Settings; coming back must not
    // leave a stale "access needed" notice on screen. The usage minutes on each
    // row are refreshed for the same reason: they were read once when the list
    // was built and would otherwise freeze for the life of the ViewModel.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAccessibilityStatus()
                viewModel.loadApps(context.packageManager)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Bottom Sheet
    if (selectedApp != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedApp = null },
            sheetState = sheetState,
            containerColor = SoberTheme.colors.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            val app = selectedApp!!

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                // App header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (app.icon != null) {
                        Image(
                            painter = rememberDrawablePainter(drawable = app.icon),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        Text(
                            text = app.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SoberTheme.colors.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoberTheme.colors.divider
                        ) {
                            Text(
                                text = "${app.usageMinutes} min today",
                                fontSize = 12.sp,
                                color = SoberTheme.colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Limit picker
                SoberSectionLabel("Daily limit")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Hours", fontSize = 12.sp, color = SoberTheme.colors.textMuted)
                        SoberDrumPicker(
                            value = sheetLimitHours,
                            range = 0..12,
                            onValueChange = { sheetLimitHours = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        ":",
                        fontSize = 32.sp,
                        color = SoberTheme.colors.textMuted,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Minutes", fontSize = 12.sp, color = SoberTheme.colors.textMuted)
                        SoberDrumPicker(
                            value = sheetLimitMinutes,
                            range = 0..59,
                            onValueChange = { sheetLimitMinutes = it },
                            modifier = Modifier.fillMaxWidth(),
                            formatter = { it.toString().padStart(2, '0') }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 7-day usage chart for this app.
                if (appHistory.isNotEmpty()) {
                    SoberSectionLabel("Last 7 days")
                    Spacer(modifier = Modifier.height(8.dp))
                    SoberMiniBarChart(
                        values = appHistory,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Peak ${appHistory.maxOrNull() ?: 0} min · ${appHistory.average().toInt()} min average",
                        fontSize = 12.sp,
                        color = SoberTheme.colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Only apps in the rules catalog get section toggles; every
                // other app has nothing to detect, so an empty list would be
                // a heading with no rows under it.
                val appSections = sectionRules.filter { it.packageName == app.packageName }
                if (appSections.isNotEmpty()) {
                    SoberSectionLabel("Block sections")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sections are blocked in the app, not the whole app. Feed, messages and search stay open.",
                        fontSize = 11.sp,
                        color = SoberTheme.colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isAccessibilityEnabled) {
                        Text(
                            text = "Section blocking needs Accessibility access. Until it is on, an app with a section turned on stays blocked entirely — Sober cannot tell which section is open.",
                            fontSize = 12.sp,
                            color = SoberTheme.colors.textSecondary
                        )
                        TextButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(PermissionUtils.getAccessibilityIntent())
                                }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Open Accessibility settings",
                                fontSize = 12.sp,
                                color = SoberTheme.colors.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    appSections.forEach { rule ->
                        SectionToggleRow(
                            rule = rule,
                            checked = rule.id in enabledSectionRules,
                            onCheckedChange = { viewModel.setSectionRuleEnabled(rule.id, it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                SoberButton(
                    text = "Save Limit",
                    onClick = {
                        val totalMinutes = sheetLimitHours * 60 + sheetLimitMinutes
                        viewModel.setAppLimit(app.packageName, app.name, totalMinutes)
                        selectedApp = null
                    },
                    enabled = sheetLimitHours * 60 + sheetLimitMinutes > 0
                )

                if (app.isLimited) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            viewModel.removeAppLimit(app.packageName)
                            selectedApp = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Remove Limit",
                            color = SoberTheme.colors.danger,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoberTheme.colors.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // -- Header --
        Text(
            text = "Apps",
            fontSize = 22.sp,
            color = SoberTheme.colors.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // -- Search Bar --
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = {
                Text("Search apps...", color = SoberTheme.colors.textMuted)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = SoberTheme.colors.textMuted
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoberTheme.colors.primary,
                unfocusedBorderColor = SoberTheme.colors.divider,
                focusedContainerColor = SoberTheme.colors.surface,
                unfocusedContainerColor = SoberTheme.colors.surface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // -- Filter Chips --
        SoberSegmentedControl(
            options = listOf(AppFilter.ALL, AppFilter.LIMITED, AppFilter.BLOCKED),
            selected = selectedFilter,
            onSelect = { viewModel.updateFilter(it) },
            label = { filter ->
                when (filter) {
                    AppFilter.ALL -> "All"
                    AppFilter.LIMITED -> "Limited"
                    AppFilter.BLOCKED -> "Blocked"
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // -- Section Label --
        SoberSectionLabel("Installed apps")

        Spacer(modifier = Modifier.height(8.dp))

        // -- App List --
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SoberTheme.colors.primary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        onClick = {
                            selectedApp = app
                            sheetLimitHours = (app.limitMinutes ?: 60) / 60
                            sheetLimitMinutes = (app.limitMinutes ?: 60) % 60
                            viewModel.loadHistory(app.packageName)
                        },
                        onLimitToggle = { enabled ->
                            viewModel.setLimitEnabled(app.packageName, enabled)
                        }
                    )
                }
            }
        }
    }
}

/**
 * One installed app. The switch is the quick control the PRD asks for: it
 * enables or disables an existing limit without opening the sheet, and the
 * configured minutes are kept so turning it back on needs no re-entry.
 *
 * The switch sits inside the row's clickable area but consumes its own taps,
 * so flipping it never opens the detail sheet.
 */
@Composable
internal fun AppRow(
    app: InstalledApp,
    onClick: () -> Unit,
    onLimitToggle: (Boolean) -> Unit
) {
    val colors = SoberTheme.colors
    val hasLimit = app.limitMinutes != null
    val atLimit = app.isLimited &&
        app.usageMinutes >= (app.limitMinutes ?: Int.MAX_VALUE)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app.icon != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = app.icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.divider, RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    fontSize = 15.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.Medium
                )
                if (app.usageMinutes > 0) {
                    Text(
                        text = "${app.usageMinutes}m today",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            if (hasLimit) {
                if (atLimit) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Blocked",
                        tint = colors.danger,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${app.limitMinutes}m",
                    fontSize = 13.sp,
                    color = when {
                        atLimit -> colors.danger
                        app.isLimited -> colors.textMuted
                        else -> colors.textMuted.copy(alpha = 0.6f)
                    },
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(10.dp))
                SoberToggleSwitch(
                    checked = app.isLimited,
                    onCheckedChange = onLimitToggle,
                    modifier = Modifier.semantics {
                        contentDescription = "Limit for ${app.name}"
                    }
                )
            }
        }

        // Progress bar for limited apps
        if (app.isLimited && app.limitMinutes != null && app.limitMinutes > 0) {
            val progress = (app.usageMinutes.toFloat() / app.limitMinutes).coerceAtMost(1f)
            SoberProgressBar(
                progress = progress,
                isDanger = progress >= 1f
            )
        }

        HorizontalDivider(color = colors.divider)
    }
}

@Composable
private fun SectionToggleRow(
    rule: DetectionRule,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule.sectionName,
                fontSize = 14.sp,
                color = SoberTheme.colors.primary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = sectionHint(rule),
                fontSize = 11.sp,
                color = SoberTheme.colors.textMuted
            )
        }
        SoberToggleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * The detection types are an implementation detail the user did not choose, so
 * the hint says what the rule looks at in plain words instead of naming it.
 */
private fun sectionHint(rule: DetectionRule): String = when (rule.detectionType) {
    DetectionType.SELECTED_TAB -> "When the ${rule.sectionName} tab is open"
    DetectionType.RESOURCE_ID -> "When the ${rule.sectionName} screen appears"
    DetectionType.ACTIVITY -> "When ${rule.sectionName} opens"
    DetectionType.TEXT -> "When the screen shows \"${rule.detectionValue}\""
}
