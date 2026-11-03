package com.sober.feature.apps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sober.core.common.permissions.PermissionUtils
import com.sober.core.domain.model.BrowserCatalog
import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.DetectionType
import com.sober.core.domain.model.WebsiteAddress
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.components.SoberDrumPicker
import com.sober.core.ui.components.SoberFilterChips
import com.sober.core.ui.components.SoberMiniBarChart
import com.sober.core.ui.components.SoberOutlinedButton
import com.sober.core.ui.components.SoberProgressBar
import com.sober.core.ui.components.SoberScreenHeader
import com.sober.core.ui.components.SoberSectionHeader
import com.sober.core.ui.components.SoberSectionLabel
import com.sober.core.ui.components.SoberSegmentedControl
import com.sober.core.ui.components.SoberToggleSwitch
import com.sober.core.ui.theme.JetBrainsMono
import com.sober.core.ui.theme.SoberTheme
import com.sober.core.ui.theme.TabularNumberStyle

/** Pause lengths offered in the sheet: long enough to interrupt, short enough to wait. */
private val PAUSE_DELAYS = listOf(5, 10, 30, 60)

/** "9:40 PM" — the wall-clock time a lock lifts. */
private fun clockText(epochMillis: Long): String =
    java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        .format(java.util.Date(epochMillis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val apps by viewModel.filteredApps.collectAsState()
    val allApps by viewModel.apps.collectAsState()
    val appHistory by viewModel.appHistory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sectionRules by viewModel.sectionRules.collectAsState()
    val enabledSectionRules by viewModel.enabledSectionRules.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val websiteRules by viewModel.websiteRules.collectAsState()
    val lockedUntil by viewModel.lockedUntil.collectAsState()
    val rulesLocked = lockedUntil != null

    // Bottom sheet state
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var sheetLimitHours by remember { mutableIntStateOf(1) }
    var sheetLimitMinutes by remember { mutableIntStateOf(0) }
    var sheetOpenLimit by remember { mutableIntStateOf(0) }
    var sheetPauseEnabled by remember { mutableStateOf(false) }
    var sheetPauseDelay by remember { mutableIntStateOf(10) }
    // Opens fully: the drum picker and the Save button together are taller
    // than the half-expanded height, which would hide the only way to confirm.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showWebsiteDialog by remember { mutableStateOf(false) }
    var websiteDraft by remember { mutableStateOf("") }

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
                                text = when {
                                    app.usageMinutes > 0 -> "${app.usageMinutes} min today"
                                    app.usedToday -> "Under a minute today"
                                    else -> "Not used today"
                                },
                                fontSize = 12.sp,
                                color = SoberTheme.colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (rulesLocked) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SoberTheme.colors.primaryMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Locked until ${clockText(lockedUntil!!)}. " +
                                "Rules cannot change until then.",
                            fontSize = 12.sp,
                            color = SoberTheme.colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

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

                // Opens budget. Time and opens limit different habits: a short
                // check-in five times a day fits inside any minutes cap, so the
                // count needs its own control.
                SoberSectionLabel("Opens per day")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (app.openCount > 0) {
                        "Opened ${app.openCount} times today. 0 turns the budget off."
                    } else {
                        "How many separate times this app can open. 0 turns the budget off."
                    },
                    fontSize = 11.sp,
                    color = SoberTheme.colors.textMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { sheetOpenLimit = (sheetOpenLimit - 1).coerceAtLeast(0) },
                        enabled = sheetOpenLimit > 0
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "Fewer opens",
                            tint = if (sheetOpenLimit > 0) SoberTheme.colors.textPrimary
                            else SoberTheme.colors.textMuted
                        )
                    }
                    Text(
                        text = if (sheetOpenLimit == 0) "Off" else "$sheetOpenLimit",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = JetBrainsMono,
                        style = TabularNumberStyle,
                        color = SoberTheme.colors.textPrimary,
                        modifier = Modifier
                            .widthIn(min = 64.dp)
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = { sheetOpenLimit = (sheetOpenLimit + 1).coerceAtMost(50) },
                        enabled = sheetOpenLimit < 50
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "More opens",
                            tint = SoberTheme.colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Pause before opening. The delay is the point, so it is chosen
                // here and enforced by the service on first foreground entry.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        SoberSectionLabel("Pause before opening")
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "A short breather first, so opening it is a choice.",
                            fontSize = 11.sp,
                            color = SoberTheme.colors.textMuted
                        )
                    }
                    SoberToggleSwitch(
                        checked = sheetPauseEnabled,
                        onCheckedChange = { sheetPauseEnabled = it }
                    )
                }
                if (sheetPauseEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SoberFilterChips(
                        options = PAUSE_DELAYS,
                        selected = sheetPauseDelay,
                        onSelect = { sheetPauseDelay = it },
                        label = { "$it seconds" }
                    )
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

                // Browsers have no section rules, so this is the one place a
                // website can be added from the app it applies to.
                if (BrowserCatalog.isBrowser(app.packageName)) {
                    SoberSectionLabel("Block websites")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The site and its subdomains are covered while the address bar shows them. " +
                            "Matches the address bar, so a redirect can appear for a moment before the cover. " +
                            "Works in the common browsers; in-app browsers and Custom Tabs cannot be read, so block the app itself for those.",
                        fontSize = 11.sp,
                        color = SoberTheme.colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isAccessibilityEnabled) {
                        Text(
                            text = "Website blocking needs Accessibility access. Without it, websites are not covered.",
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

                    if (websiteRules.isEmpty()) {
                        Text(
                            text = "No websites blocked.",
                            fontSize = 12.sp,
                            color = SoberTheme.colors.textMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        websiteRules.forEach { rule ->
                            WebsiteRuleRow(
                                rule = rule,
                                onToggle = { enabled ->
                                    viewModel.setWebsiteEnabled(rule.id, enabled)
                                },
                                onRemove = { viewModel.removeWebsite(rule.id) }
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            websiteDraft = ""
                            showWebsiteDialog = true
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = SoberTheme.colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Add website",
                            fontSize = 13.sp,
                            color = SoberTheme.colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                SoberButton(
                    text = "Save rules",
                    onClick = {
                        viewModel.saveAppRules(
                            packageName = app.packageName,
                            appName = app.name,
                            limitMinutes = sheetLimitHours * 60 + sheetLimitMinutes,
                            openLimit = sheetOpenLimit,
                            pauseDelaySeconds = if (sheetPauseEnabled) sheetPauseDelay else null
                        )
                        selectedApp = null
                    },
                    enabled = !rulesLocked &&
                        (sheetLimitHours * 60 + sheetLimitMinutes > 0 ||
                            sheetOpenLimit > 0 ||
                            sheetPauseEnabled)
                )

                if ((app.isLimited || app.pauseDelaySeconds != null) && !rulesLocked) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            viewModel.removeAppRules(app.packageName)
                            selectedApp = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Remove all rules",
                            color = SoberTheme.colors.danger,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showWebsiteDialog) {
        WebsiteDialog(
            draft = websiteDraft,
            onDraftChange = { websiteDraft = it },
            onDismiss = { showWebsiteDialog = false },
            onConfirm = {
                viewModel.addWebsite(websiteDraft)
                showWebsiteDialog = false
                websiteDraft = ""
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoberTheme.colors.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // -- Header --
        SoberScreenHeader(
            title = "Apps",
            eyebrow = "Your library",
            subtitle = "What you used today, and what each app is allowed."
        )

        Spacer(modifier = Modifier.height(20.dp))

        // -- Search --
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = {
                Text(
                    text = "Search apps",
                    color = SoberTheme.colors.textMuted,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = SoberTheme.colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = SoberTheme.colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = SoberTheme.colors.cardBorder,
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SoberTheme.colors.surface,
                unfocusedContainerColor = SoberTheme.colors.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = SoberTheme.colors.primary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // -- Filters --
        SoberFilterChips(
            options = listOf(AppFilter.USED, AppFilter.LIMITED, AppFilter.BLOCKED, AppFilter.ALL),
            selected = selectedFilter,
            onSelect = { viewModel.updateFilter(it) },
            label = { filter ->
                when (filter) {
                    AppFilter.USED -> "Used"
                    AppFilter.LIMITED -> "Limited"
                    AppFilter.BLOCKED -> "Blocked"
                    AppFilter.ALL -> "All"
                }
            },
            count = { filter ->
                when (filter) {
                    AppFilter.USED -> allApps.count { it.usedToday }
                    AppFilter.LIMITED -> allApps.count { it.isLimited }
                    AppFilter.BLOCKED -> allApps.count {
                        it.isLimited && it.usageMinutes >= (it.limitMinutes ?: Int.MAX_VALUE)
                    }
                    AppFilter.ALL -> allApps.size
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // -- Section Label --
        SoberSectionHeader(
            title = when (selectedFilter) {
                AppFilter.USED -> "Used today"
                AppFilter.LIMITED -> "Limited apps"
                AppFilter.BLOCKED -> "At their limit"
                AppFilter.ALL -> "All installed apps"
            },
            index = "01"
        )

        Spacer(modifier = Modifier.height(4.dp))

        // -- App List --
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SoberTheme.colors.primary)
            }
        } else if (apps.isEmpty()) {
            EmptyAppsState(
                filter = selectedFilter,
                query = searchQuery,
                onClearSearch = { viewModel.updateSearchQuery("") },
                onShowAll = { viewModel.updateFilter(AppFilter.ALL) }
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        onClick = {
                            selectedApp = app
                            sheetLimitHours = (app.limitMinutes ?: 60) / 60
                            sheetLimitMinutes = (app.limitMinutes ?: 60) % 60
                            sheetOpenLimit = app.openLimit
                            sheetPauseEnabled = app.pauseDelaySeconds != null
                            sheetPauseDelay = app.pauseDelaySeconds ?: 10
                            viewModel.loadHistory(app.packageName)
                        },
                        onLimitToggle = { enabled ->
                            viewModel.setLimitEnabled(app.packageName, enabled)
                        },
                        locked = rulesLocked
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
    onLimitToggle: (Boolean) -> Unit,
    locked: Boolean = false
) {
    val colors = SoberTheme.colors
    // A pause rule on its own has nothing to switch off, so it does not earn
    // the row's switch — but it does change what "Set limit" means.
    val hasLimit = app.limitMinutes != null || app.openLimit > 0
    val atMinuteLimit = app.isLimited && app.limitMinutes != null &&
        app.usageMinutes >= app.limitMinutes
    val atOpenLimit = app.isLimited && app.openLimit > 0 && app.openCount >= app.openLimit
    val atLimit = atMinuteLimit || atOpenLimit

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Open limit settings",
                    onClick = onClick
                )
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app.icon != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.primaryMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberDrawablePainter(drawable = app.icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(colors.divider, RoundedCornerShape(13.dp))
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    fontSize = 15.sp,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                if (app.usedToday) {
                    Text(
                        text = if (app.usageMinutes > 0) {
                            "${app.usageMinutes}m today"
                        } else {
                            "Used today, under a minute"
                        },
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
                    text = app.limitMinutes?.let { limitText(it) }
                        ?: "${app.openLimit} opens/day",
                    fontSize = 13.sp,
                    color = when {
                        atLimit -> colors.danger
                        app.isLimited -> colors.textMuted
                        else -> colors.textMuted.copy(alpha = 0.6f)
                    },
                    fontWeight = FontWeight.Medium,
                    style = TabularNumberStyle
                )
                Spacer(modifier = Modifier.width(10.dp))
                SoberToggleSwitch(
                    checked = app.isLimited,
                    onCheckedChange = onLimitToggle,
                    enabled = !locked,
                    modifier = Modifier.semantics {
                        contentDescription = "Limit for ${app.name}"
                    }
                )
            } else {
                // Without a limit there is no switch to show; the row still
                // opens the sheet, so this is the affordance that says so.
                Text(
                    text = "Set limit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted
                )
            }
        }

        // Progress bar for limited apps
        if (app.isLimited && app.limitMinutes != null && app.limitMinutes > 0) {
            val progress = (app.usageMinutes.toFloat() / app.limitMinutes).coerceAtMost(1f)
            SoberProgressBar(
                progress = progress,
                isDanger = progress >= 1f,
                modifier = Modifier.padding(start = 58.dp)
            )
        }

        HorizontalDivider(color = colors.divider)
    }
}

/** Whole hours stay whole: "1h 30m" rather than "90m" in a tight row. */
private fun limitText(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

/**
 * The list is empty for four different reasons, and "nothing here" would only
 * be true for one of them. Each copy names the next action instead.
 */
@Composable
private fun EmptyAppsState(
    filter: AppFilter,
    query: String,
    onClearSearch: () -> Unit,
    onShowAll: () -> Unit
) {
    val colors = SoberTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                query.isNotBlank() -> "No app matches \u201C$query\u201D."
                filter == AppFilter.USED -> "No apps used yet today."
                filter == AppFilter.LIMITED -> "No app limits yet."
                filter == AppFilter.BLOCKED -> "No app is at its limit right now."
                else -> "No launchable apps found."
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                query.isNotBlank() -> "Try a shorter name, or clear the search."
                filter == AppFilter.USED -> "Apps appear here once you open them. Switch to All to set a limit before you do."
                filter == AppFilter.LIMITED -> "Open an app in All and save a daily limit."
                else -> "There is nothing to limit on this device."
            },
            fontSize = 13.sp,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (query.isNotBlank()) {
            SoberOutlinedButton(text = "Clear search", onClick = onClearSearch)
        } else if (filter == AppFilter.USED || filter == AppFilter.LIMITED) {
            SoberOutlinedButton(text = "Show all apps", onClick = onShowAll)
        }
    }
}

@Composable
private fun WebsiteRuleRow(
    rule: CustomRule,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    val colors = SoberTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .toggleable(
                    value = rule.enabled,
                    role = Role.Switch,
                    onValueChange = onToggle
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rule.value,
                fontSize = 14.sp,
                color = if (rule.enabled) colors.textPrimary else colors.textMuted,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            SoberToggleSwitch(checked = rule.enabled, onCheckedChange = null)
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove ${rule.value}",
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun WebsiteDialog(
    draft: String,
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = SoberTheme.colors
    val normalized = WebsiteAddress.normalizeDomain(draft)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = "Block a website",
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
                    value = draft,
                    onValueChange = onDraftChange,
                    placeholder = { Text("example.com", color = colors.textMuted) },
                    singleLine = true,
                    isError = draft.isNotBlank() && normalized == null,
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
                if (draft.isNotBlank() && normalized == null) {
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
            TextButton(onClick = onConfirm, enabled = normalized != null) {
                Text(
                    text = "Block",
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

@Composable
private fun SectionToggleRow(
    rule: DetectionRule,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
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
            onCheckedChange = null
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
