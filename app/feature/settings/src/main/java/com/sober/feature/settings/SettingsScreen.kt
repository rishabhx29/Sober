package com.sober.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sober.core.domain.model.YourReason
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.components.SoberCard
import com.sober.core.ui.components.SoberDivider
import com.sober.core.ui.components.SoberFilterChips
import com.sober.core.ui.components.SoberListRow
import com.sober.core.ui.components.SoberOutlinedButton
import com.sober.core.ui.components.SoberQuietButton
import com.sober.core.ui.components.SoberScreenHeader
import com.sober.core.ui.components.SoberSectionHeader
import com.sober.core.ui.components.SoberStatusRow
import com.sober.core.ui.components.SoberToggleSwitch
import com.sober.core.ui.theme.JetBrainsMono
import com.sober.core.ui.theme.SoberTheme
import com.sober.core.ui.theme.TabularNumberStyle
import com.sober.core.common.battery.BatteryOptimizationUtils
import com.sober.core.common.permissions.PermissionUtils
import com.sober.core.domain.model.AppCategory
import com.sober.core.domain.repository.AlertKind
import com.sober.core.domain.repository.BlockableApp
import com.sober.core.domain.repository.StreakRule
import com.sober.core.domain.repository.ThemeMode
import com.sober.feature.settings.admin.SoberDeviceAdminReceiver
import java.time.LocalDate

/**
 * Settings.
 *
 * Scope note: this screen exposes only what is wired to real enforcement — the
 * overall daily cap, the override PIN, alert toggles the notifier reads, the
 * streak rule, uninstall protection and permission status. The usage export is
 * included because it reads the same history Stats charts.
 *
 * The PIN serves two purposes: it authorises emergency overrides, and while a
 * focus session runs it gates this screen. The gate is per-visit — leaving and
 * returning during a session re-locks, so a half-finished edit cannot be resumed
 * by someone who did not enter the PIN.
 */
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)?,
    onOpenSchedules: () -> Unit,
    onOpenCustomRules: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val dailyLimitMinutes by viewModel.dailyScreenTimeLimitMinutes.collectAsState()
    val capApps by viewModel.capApps.collectAsState()
    val capExemptPackages by viewModel.capExemptPackages.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val streakRule by viewModel.streakRule.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val gate by viewModel.gate.collectAsState()
    val allowOnly by viewModel.allowOnly.collectAsState()
    val bedtime by viewModel.bedtime.collectAsState()
    val categoryLimitMinutes by viewModel.categoryLimitMinutes.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val yourReason by viewModel.yourReason.collectAsState()
    var unlocked by remember { mutableStateOf(false) }

    val lockState = gate
    if (lockState == null) {
        // Stored state has not loaded yet. Render the background only, so the
        // controls cannot be tapped before we know whether they are locked.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SoberTheme.colors.background)
        ) {}
        return
    }

    val hasPin = lockState.hasPin
    if ((lockState.sessionRunning || lockState.lockActive) && hasPin && !unlocked) {
        SettingsLockScreen(
            onBack = onBack,
            lockedUntil = lockState.lockedUntil.takeIf { lockState.lockActive },
            sessionRunning = lockState.sessionRunning,
            onUnlock = { pin, onResult -> viewModel.verifyPin(pin, onResult) },
            onUnlocked = { unlocked = true }
        )
        return
    }

    val scrollState = rememberScrollState()
    val colors = SoberTheme.colors
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var showYourReasonDialog by remember { mutableStateOf(false) }
    var showCapApps by remember { mutableStateOf(false) }
    var showAllowOnlyApps by remember { mutableStateOf(false) }
    var showBedtimeApps by remember { mutableStateOf(false) }
    var editingBedtimeTime by remember { mutableStateOf<BedtimeTimeTarget?>(null) }
    var showPrivacy by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var backupBusy by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backupMessageIsError by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var showRestorePin by remember { mutableStateOf(false) }

    fun startRestore(uri: Uri) {
        backupBusy = true
        backupMessage = null
        viewModel.restoreBackup(uri) { message, error ->
            backupBusy = false
            backupMessageIsError = error != null
            backupMessage = error ?: message
        }
    }

    val backupCreateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        backupBusy = true
        backupMessage = null
        viewModel.exportBackup(uri) { ok, error ->
            backupBusy = false
            backupMessageIsError = !ok
            backupMessage = if (ok) "Backup saved." else error
        }
    }

    val backupOpenLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // The file is only read after the PIN check: importing rules is a
        // settings change, and the PIN is what protects settings.
        if (hasPin) {
            pendingRestore = uri
            showRestorePin = true
        } else {
            startRestore(uri)
        }
    }

    // Device-admin and battery state only change on a system screen, so
    // re-reading them on resume is both sufficient and cheaper than polling.
    var adminActive by remember {
        mutableStateOf(PermissionUtils.hasDeviceAdminPermission(context, SOBER_ADMIN))
    }
    var batteryExempt by remember {
        mutableStateOf(BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context))
    }
    var dndAccess by remember { mutableStateOf(PermissionUtils.hasDndAccess(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                adminActive =
                    PermissionUtils.hasDeviceAdminPermission(context, SOBER_ADMIN)
                batteryExempt =
                    BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context)
                dndAccess = PermissionUtils.hasDndAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
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
            }
        }

        SoberScreenHeader(
            title = "Settings",
            eyebrow = "Preferences",
            subtitle = "The rules, the permissions, and how Sober behaves."
        )

        Spacer(modifier = Modifier.height(28.dp))

        SoberSectionHeader(title = "Daily screen time", index = "01")

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = formatMinutes(dailyLimitMinutes),
            color = colors.textPrimary,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            fontFamily = JetBrainsMono,
            style = TabularNumberStyle
        )
        Text(
            text = "total per day",
            color = colors.textSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Applies across every app, on top of your individual app limits.",
            color = colors.textSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stepped control rather than a free picker: this value governs a
        // hard block, and a scroll picker makes it easy to land on an
        // unintended number by accident.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SoberOutlinedButton(
                text = "− 30m",
                onClick = {
                    viewModel.setDailyScreenTimeLimit(
                        (dailyLimitMinutes - 30).coerceAtLeast(30)
                    )
                },
                modifier = Modifier.weight(1f)
            )
            SoberOutlinedButton(
                text = "+ 30m",
                onClick = {
                    viewModel.setDailyScreenTimeLimit(
                        (dailyLimitMinutes + 30).coerceAtMost(24 * 60)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        SoberDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Apps covered",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (capExemptPackages.size) {
                        0 -> "All of them"
                        1 -> "1 app left out"
                        else -> "${capExemptPackages.size} apps left out"
                    },
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            SoberQuietButton(text = "Choose", onClick = { showCapApps = true })
        }
        Text(
            text = "Apps left out still count as screen time, but the cap never blocks them. " +
                "Home, Settings, System UI and your keyboard are never blocked.",
            color = colors.textMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Category budgets", index = "02")

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "One shared daily budget for every app Android puts in a category. " +
                "It stacks on top of each app's own limit.",
            color = colors.textSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        CATEGORY_BUDGET_ROWS.forEachIndexed { index, (category, label) ->
            val minutes = categoryLimitMinutes[category] ?: 0
            val appCount = categoryCounts[category] ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when {
                            appCount == 0 -> "No installed apps in this category"
                            appCount == 1 -> "1 app · budget shared"
                            else -> "$appCount apps · budget shared"
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = if (minutes == 0) "Off" else formatMinutes(minutes),
                    color = if (minutes == 0) colors.textMuted else colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    style = TabularNumberStyle
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SoberFilterChips(
                options = CATEGORY_BUDGET_OPTIONS,
                selected = minutes,
                onSelect = { viewModel.setCategoryLimit(category, it) },
                label = { option -> if (option == 0) "Off" else formatMinutes(option) }
            )
            if (index != CATEGORY_BUDGET_ROWS.lastIndex) {
                SoberDivider(modifier = Modifier.padding(vertical = 10.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Schedules", index = "03")

        SoberListRow(
            title = "Time-based blocking",
            subtitle = "Block chosen apps during set hours, on set days.",
            showChevron = true,
            onClick = onOpenSchedules
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Custom rules", index = "04")

        SoberListRow(
            title = "Your own rules",
            subtitle = "Keep an app or website closed, or mute notifications containing a word you choose.",
            showChevron = true,
            onClick = onOpenCustomRules
        )

        Spacer(modifier = Modifier.height(24.dp))

        // -- Stricter modes: allow-only and bedtime --

        SoberSectionHeader(title = "Stricter modes", index = "05")

        SoberListRow(
            title = "Allow-only mode",
            subtitle = when {
                !allowOnly.enabled ->
                    "Only the apps you choose can be opened. Everything else is held shut."
                allowOnly.allowedPackages.isEmpty() ->
                    "On, but no apps are allowed yet — everything except system screens is blocked."
                allowOnly.allowedPackages.size == 1 ->
                    "On. 1 app can open; everything else is held shut."
                else ->
                    "On. ${allowOnly.allowedPackages.size} apps can open; everything else is held shut."
            },
            trailing = {
                SoberToggleSwitch(
                    checked = allowOnly.enabled,
                    onCheckedChange = { enabled ->
                        viewModel.setAllowOnlyEnabled(enabled)
                        // An allow-list that allows nothing is a trap unless the
                        // user meant it, so the picker opens on the same tap.
                        if (enabled && allowOnly.allowedPackages.isEmpty()) {
                            showAllowOnlyApps = true
                        }
                    }
                )
            }
        )
        if (allowOnly.enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Allowed apps",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                SoberQuietButton(
                    text = "Choose",
                    onClick = { showAllowOnlyApps = true }
                )
            }
            Text(
                text = "Calls, alarms and the launcher keep working. Sober never blocks its own " +
                    "settings, System UI or your keyboard.",
                color = colors.textMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        SoberDivider(modifier = Modifier.padding(vertical = 8.dp))

        SoberListRow(
            title = "Bedtime mode",
            subtitle = when {
                !bedtime.enabled ->
                    "A nightly window that keeps chosen apps open and everything else shut."
                bedtime.allowedPackages.isEmpty() ->
                    "Every night ${minuteOfDayText(bedtime.startMinuteOfDay)}–" +
                        "${minuteOfDayText(bedtime.endMinuteOfDay)}. No apps allowed yet."
                else ->
                    "Every night ${minuteOfDayText(bedtime.startMinuteOfDay)}–" +
                        "${minuteOfDayText(bedtime.endMinuteOfDay)}, " +
                        "${bedtime.allowedPackages.size} apps allowed."
            },
            trailing = {
                SoberToggleSwitch(
                    checked = bedtime.enabled,
                    onCheckedChange = { viewModel.setBedtimeEnabled(it) }
                )
            }
        )
        if (bedtime.enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SoberOutlinedButton(
                    text = "Starts ${minuteOfDayText(bedtime.startMinuteOfDay)}",
                    onClick = { editingBedtimeTime = BedtimeTimeTarget.START },
                    modifier = Modifier.weight(1f)
                )
                SoberOutlinedButton(
                    text = "Ends ${minuteOfDayText(bedtime.endMinuteOfDay)}",
                    onClick = { editingBedtimeTime = BedtimeTimeTarget.END },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (bedtime.allowedPackages.isEmpty()) {
                        "Allowed apps — none yet"
                    } else {
                        "${bedtime.allowedPackages.size} apps allowed"
                    },
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                SoberQuietButton(
                    text = "Choose",
                    onClick = { showBedtimeApps = true }
                )
            }
            if (dndAccess) {
                Text(
                    text = "Do Not Disturb turns on for the window and back off when it ends. " +
                        "Softer interruptions still get through.",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else {
                Text(
                    text = "Apps are held shut, but the phone still rings and buzzes. " +
                        "Grant Do Not Disturb access to silence it overnight.",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                SoberQuietButton(
                    text = "Grant Do Not Disturb access",
                    onClick = {
                        runCatching { context.startActivity(PermissionUtils.getDndAccessIntent()) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Notifications", index = "06")

        Column(modifier = Modifier.padding(top = 6.dp)) {
            val rows = listOf(
                Triple(
                    AlertKind.USAGE_WARNING,
                    "Limit warnings",
                    "One heads-up a day when an app or your total crosses 80%."
                ),
                Triple(
                    AlertKind.SESSION_COMPLETE,
                    "Session complete",
                    "An alert when a timed focus session finishes."
                ),
                Triple(
                    AlertKind.STREAK_WARNING,
                    "Streak at risk",
                    "After a third cancelled session, or when a limit is crossed late in the day."
                ),
                Triple(
                    AlertKind.DAILY_STREAK,
                    "Daily streak",
                    "One evening note when every limit still holds."
                ),
                Triple(
                    AlertKind.BEDTIME_WARNING,
                    "Bedtime warning",
                    "15 minutes before a sleep schedule starts."
                )
            )
            rows.forEachIndexed { index, (kind, title, description) ->
                AlertToggleRow(
                    title = title,
                    description = description,
                    checked = alerts[kind] ?: true,
                    onCheckedChange = { viewModel.setAlertEnabled(kind, it) }
                )
                if (index != rows.lastIndex) {
                    SoberDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Streaks", index = "07")

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "What counts as a successful day",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (streakRule == StreakRule.UNDER_LIMITS) {
                "A day counts when you stay under your overall cap and every app limit."
            } else {
                "A day also fails if you cancel a focus session that day."
            },
            color = colors.textSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        SoberFilterChips(
            options = listOf(
                StreakRule.UNDER_LIMITS,
                StreakRule.LIMITS_AND_NO_CANCELLATIONS
            ),
            selected = streakRule,
            onSelect = { viewModel.setStreakRule(it) },
            label = {
                when (it) {
                    StreakRule.UNDER_LIMITS -> "Under limits"
                    StreakRule.LIMITS_AND_NO_CANCELLATIONS -> "Limits + sessions"
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Protection", index = "08")

        SoberListRow(
            title = if (hasPin) "PIN is set" else "No PIN set",
            subtitle = if (hasPin) {
                "Required to change limits while a focus session is running, and for emergency overrides."
            } else {
                "Set a 4–6 digit PIN to lock these settings during focus sessions and to authorise emergency overrides."
            },
            trailing = {
                SoberQuietButton(
                    text = if (hasPin) "Change" else "Set",
                    onClick = { showPinDialog = true }
                )
            }
        )
        SoberDivider()
        SoberListRow(
            title = "Your reason",
            subtitle = yourReason.ifBlank {
                "One line you will see when a block appears. It replaces the daily quote."
            },
            trailing = {
                SoberQuietButton(
                    text = if (yourReason.isBlank()) "Add" else "Edit",
                    onClick = { showYourReasonDialog = true }
                )
            }
        )
        SoberDivider()
        SoberListRow(
            title = "Lock mode",
            subtitle = when {
                lockState.lockActive ->
                    "On until ${clockText(lockState.lockedUntil!!)}. " +
                        "Limits, sessions and overrides cannot be changed until then."
                hasPin ->
                    "Make your current rules non-negotiable for a while. " +
                        "Only the PIN can end it early."
                else ->
                    "Set a PIN first — lock mode is unlocked with it."
            },
            trailing = {
                SoberQuietButton(
                    text = when {
                        lockState.lockActive -> "Turn off"
                        hasPin -> "Start"
                        else -> "Set PIN"
                    },
                    onClick = {
                        when {
                            // Reaching "Turn off" means the PIN gate above was
                            // already passed, so this is the authorised path.
                            lockState.lockActive -> viewModel.clearLock()
                            hasPin -> Unit
                            else -> showPinDialog = true
                        }
                    }
                )
            }
        )
        if (hasPin && !lockState.lockActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LOCK_LENGTHS.forEach { (hours, label) ->
                    SoberQuietButton(
                        text = label,
                        onClick = { viewModel.lockFor(hours) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        SoberDivider()
        SoberListRow(
            title = "Uninstall protection",
            subtitle = if (adminActive) {
                "Active. Android will not uninstall Sober until you deactivate it. " +
                    "It does not stop a force-stop from the app info screen."
            } else {
                "Off. Sober can be uninstalled at any time, including mid-session."
            },
            trailing = {
                SoberQuietButton(
                    text = if (adminActive) "Deactivate" else "Activate",
                    onClick = {
                        if (adminActive) {
                            // The system's removal dialog is not part of the public
                            // SDK surface any more; fall back to Security settings
                            // rather than crashing if it does not resolve.
                            runCatching {
                                context.startActivity(
                                    PermissionUtils.getRemoveDeviceAdminIntent(context, SOBER_ADMIN)
                                )
                            }.onFailure {
                                context.startActivity(PermissionUtils.getDeviceAdminSettingsIntent())
                            }
                        } else {
                            context.startActivity(
                                PermissionUtils.getDeviceAdminIntent(context, SOBER_ADMIN)
                            )
                        }
                    }
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Permission status", index = "09")

        val permissions = listOf(
            Triple("Usage access", PermissionUtils.hasUsageStatsPermission(context), true),
            Triple("Display over other apps", PermissionUtils.hasOverlayPermission(context), true),
            Triple(
                "Notifications",
                PermissionUtils.hasPostNotificationsPermission(context),
                true
            ),
            Triple("Do Not Disturb access", dndAccess, false),
            Triple(
                "Notification access",
                PermissionUtils.hasNotificationListenerPermission(context),
                false
            ),
            Triple("Accessibility", PermissionUtils.hasAccessibilityPermission(context), false)
        )

        Column(modifier = Modifier.padding(top = 4.dp)) {
            permissions.forEachIndexed { index, (label, granted, required) ->
                SoberStatusRow(
                    label = label,
                    statusText = if (granted) "Granted" else if (required) "Required" else "Optional",
                    isPositive = granted
                )
                if (index != permissions.lastIndex) {
                    SoberDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Engine", index = "10")

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Sober checks your usage every 15 seconds and blocks any app that has hit a limit.",
            color = colors.textSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (batteryExempt) {
                "Sober is exempt from battery optimisation, so the engine keeps running after the screen turns off."
            } else {
                "Phones stop background apps to save power. If Sober is stopped, your limits stop too — allow it to run in the background."
            },
            color = colors.textSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Stacked rather than side by side: the vendor label can be long
        // ("Open Xiaomi background settings"), and two buttons sharing one line
        // would squeeze it into a ragged multi-line link.
        Column {
            SoberQuietButton(
                text = "Battery settings",
                icon = Icons.Rounded.OpenInNew,
                onClick = {
                    // The list screen needs no permission, unlike the direct
                    // exemption dialog, which is restricted on Play.
                    context.startActivity(
                        BatteryOptimizationUtils.getBatteryOptimizationSettingsIntent()
                    )
                }
            )
            BatteryOptimizationUtils.getOemBatterySettingsIntent(context)?.let { intent ->
                SoberQuietButton(
                    text = "Open ${BatteryOptimizationUtils.manufacturerLabel() ?: "vendor"} background settings",
                    icon = Icons.Rounded.OpenInNew,
                    onClick = { runCatching { context.startActivity(intent) } }
                )
            }
        }
        Text(
            text = "Some phones need a second switch (autostart or 'never sleeping apps') before blocking survives.",
            color = colors.textMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Data", index = "11")

        SoberListRow(
            title = "Back up rules",
            subtitle = "Limits, schedules, custom rules, category budgets and stricter " +
                "modes as one JSON file. Usage history and your PIN are never included.",
            trailing = {
                SoberQuietButton(
                    text = if (backupBusy) "Working…" else "Back up",
                    onClick = {
                        if (backupBusy) return@SoberQuietButton
                        backupCreateLauncher.launch("sober-backup-${LocalDate.now()}.json")
                    }
                )
            }
        )

        SoberDivider()

        SoberListRow(
            title = "Restore rules",
            subtitle = if (hasPin) {
                "Replaces the rules on this device with the file's. Your PIN is required."
            } else {
                "Replaces the rules on this device with the file's."
            },
            trailing = {
                SoberQuietButton(
                    text = if (backupBusy) "Working…" else "Restore",
                    onClick = {
                        if (backupBusy) return@SoberQuietButton
                        backupOpenLauncher.launch(
                            arrayOf("application/json", "text/plain", "application/octet-stream")
                        )
                    }
                )
            }
        )

        backupMessage?.let {
            Text(
                text = it,
                color = if (backupMessageIsError) colors.danger else colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        SoberDivider()

        SoberListRow(
            title = "Export usage data",
            subtitle = "The last 30 days of per-app usage as a CSV. Nothing leaves the " +
                "device unless you pick an app in the share sheet.",
            trailing = {
                SoberQuietButton(
                    text = if (exporting) "Preparing…" else "Export",
                    onClick = {
                        // Ignore repeat taps while the file is being written; the
                        // button has no disabled state to do this for us.
                        if (exporting) return@SoberQuietButton
                        exporting = true
                        exportError = null
                        viewModel.exportUsageCsv { uri, error ->
                            exporting = false
                            if (uri == null) {
                                exportError = error
                                return@exportUsageCsv
                            }
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Sober usage export")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(send, "Export usage data"))
                        }
                    }
                )
            }
        )

        exportError?.let {
            Text(
                text = it,
                color = colors.danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Appearance", index = "12")

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Theme",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (themeMode == ThemeMode.SYSTEM) {
                "Following the system setting."
            } else {
                "Sober ignores the system setting while this is chosen."
            },
            color = colors.textSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        SoberFilterChips(
            options = ThemeMode.entries,
            selected = themeMode,
            onSelect = { viewModel.setThemeMode(it) },
            label = {
                when (it) {
                    ThemeMode.SYSTEM -> "System"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "About", index = "13")

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Sober ${appVersion(context)}",
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Sober has no internet permission: it cannot send anything anywhere, " +
                "even by accident.",
            color = colors.textSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        SoberListRow(
            title = "Privacy Policy",
            showChevron = true,
            onClick = { showPrivacy = true }
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (onBack != null) {
            SoberButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPinDialog) {
        PinDialog(
            hasPin = hasPin,
            onDismiss = { showPinDialog = false },
            onSubmit = { currentPin, newPin, onResult ->
                viewModel.savePin(currentPin, newPin, onResult)
            }
        )
    }

    if (showYourReasonDialog) {
        YourReasonDialog(
            initial = yourReason,
            onSave = viewModel::setYourReason,
            onDismiss = { showYourReasonDialog = false }
        )
    }

    if (showRestorePin) {
        RestorePinDialog(
            onDismiss = {
                showRestorePin = false
                pendingRestore = null
            },
            onVerify = { pin, onResult -> viewModel.verifyPin(pin, onResult) },
            onVerified = {
                showRestorePin = false
                pendingRestore?.let { startRestore(it) }
                pendingRestore = null
            }
        )
    }

    if (showCapApps) {
        CapAppsDialog(
            apps = capApps,
            exempt = capExemptPackages,
            onToggle = viewModel::setAppExemptFromCap,
            onDismiss = { showCapApps = false }
        )
    }

    if (showAllowOnlyApps) {
        AllowedAppsDialog(
            title = "Apps allowed in allow-only mode",
            description = "Everything not checked here is held shut, whatever its limits say. " +
                "Calls, alarms and the launcher keep working.",
            apps = capApps,
            selected = allowOnly.allowedPackages,
            onToggle = { packageName, allowed ->
                viewModel.setAllowOnlyPackages(
                    if (allowed) {
                        allowOnly.allowedPackages + packageName
                    } else {
                        allowOnly.allowedPackages - packageName
                    }
                )
            },
            onDismiss = { showAllowOnlyApps = false }
        )
    }

    if (showBedtimeApps) {
        AllowedAppsDialog(
            title = "Apps allowed at bedtime",
            description = "Everything not checked here is held shut during the window. " +
                "Most people allow calls, maps and the alarm clock.",
            apps = capApps,
            selected = bedtime.allowedPackages,
            onToggle = { packageName, allowed ->
                viewModel.setBedtimeAllowedPackages(
                    if (allowed) {
                        bedtime.allowedPackages + packageName
                    } else {
                        bedtime.allowedPackages - packageName
                    }
                )
            },
            onDismiss = { showBedtimeApps = false }
        )
    }

    editingBedtimeTime?.let { target ->
        val current = bedtime
        MinuteOfDayPickerDialog(
            title = if (target == BedtimeTimeTarget.START) "Bedtime starts" else "Bedtime ends",
            initialMinuteOfDay = if (target == BedtimeTimeTarget.START) {
                current.startMinuteOfDay
            } else {
                current.endMinuteOfDay
            },
            onDismiss = { editingBedtimeTime = null },
            onConfirm = { minuteOfDay ->
                if (target == BedtimeTimeTarget.START) {
                    viewModel.setBedtimeWindow(minuteOfDay, current.endMinuteOfDay)
                } else {
                    viewModel.setBedtimeWindow(current.startMinuteOfDay, minuteOfDay)
                }
                editingBedtimeTime = null
            }
        )
    }

    if (showPrivacy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
    }
}

/**
 * The user's choice of apps the overall cap covers. Checked means covered, so
 * the default state — everything on — reads as "the cap applies to my apps"
 * rather than making the user pick from an empty list.
 */
@Composable
private fun CapAppsDialog(
    apps: List<BlockableApp>,
    exempt: Set<String>,
    onToggle: (packageName: String, exempt: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SoberTheme.colors
    var query by remember { mutableStateOf("") }
    val coveredCount = apps.count { it.packageName !in exempt }
    val filtered = remember(apps, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.label.contains(trimmed, ignoreCase = true) ||
                    it.packageName.contains(trimmed, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Apps covered by your cap",
                color = colors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "Unchecked apps still count as screen time, but the cap " +
                        "never blocks them. Their own limits and schedules still apply.",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$coveredCount of ${apps.size} apps covered",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (apps.isEmpty()) {
                    Text(
                        text = "No apps found.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search apps") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = colors.textMuted
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear search",
                                        tint = colors.textMuted
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.divider,
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (filtered.isEmpty()) {
                        Text(
                            text = "No apps match \"${query.trim()}\".",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                            items(filtered, key = { it.packageName }) { app ->
                                val covered = app.packageName !in exempt
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .toggleable(
                                            value = covered,
                                            role = Role.Switch,
                                            onValueChange = { next ->
                                                onToggle(app.packageName, !next)
                                            }
                                        )
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = app.label,
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    SoberToggleSwitch(
                                        checked = covered,
                                        onCheckedChange = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done", color = colors.primary, fontWeight = FontWeight.Medium)
            }
        }
    )
}

/**
 * An allow-list picker shared by allow-only mode and bedtime. Unlike the cap
 * picker, nothing is selected by default: an allow-list is a promise about what
 * may run, and pre-ticking apps the user did not choose would make the promise
 * mean something else.
 */
@Composable
private fun AllowedAppsDialog(
    title: String,
    description: String,
    apps: List<BlockableApp>,
    selected: Set<String>,
    onToggle: (packageName: String, allowed: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SoberTheme.colors
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.label.contains(trimmed, ignoreCase = true) ||
                    it.packageName.contains(trimmed, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = title,
                color = colors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = description,
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${selected.size} of ${apps.size} apps allowed",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (apps.isEmpty()) {
                    Text(
                        text = "No apps found.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search apps") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = colors.textMuted
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear search",
                                        tint = colors.textMuted
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.divider,
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (filtered.isEmpty()) {
                        Text(
                            text = "No apps match \"${query.trim()}\".",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                            items(filtered, key = { it.packageName }) { app ->
                                val allowed = app.packageName in selected
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .toggleable(
                                            value = allowed,
                                            role = Role.Switch,
                                            onValueChange = { onToggle(app.packageName, it) }
                                        )
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = app.label,
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    SoberToggleSwitch(
                                        checked = allowed,
                                        onCheckedChange = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done", color = colors.primary, fontWeight = FontWeight.Medium)
            }
        }
    )
}

/**
 * A 24-hour wheel for the bedtime window. Material's dial is used rather than a
 * list of preset windows so an unusual schedule (00:30–06:00) is still possible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinuteOfDayPickerDialog(
    title: String,
    initialMinuteOfDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (minuteOfDay: Int) -> Unit
) {
    val colors = SoberTheme.colors
    val state = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = title,
                color = colors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(text = "Set", color = colors.primary, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = colors.textSecondary)
            }
        }
    )
}

/**
 * One line the user writes for the block screen. The field caps input at the
 * stored limit so the counter and the value can never disagree; the repository
 * normalises again before writing.
 */
@Composable
private fun YourReasonDialog(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SoberTheme.colors
    var text by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Your reason",
                color = colors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "Shown when a block appears, in place of the daily quote. " +
                        "Keep it short and specific — it is read at a glance.",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        if (input.length <= YourReason.MAX_LENGTH) text = input
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "e.g. Be present with my kids",
                            fontSize = 14.sp,
                            color = colors.textMuted
                        )
                    },
                    maxLines = 3,
                    supportingText = {
                        Text(
                            text = "${text.length}/${YourReason.MAX_LENGTH}",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.divider,
                        cursorColor = colors.primary,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text); onDismiss() }) {
                Text(text = "Save", color = colors.primary, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = colors.textSecondary)
            }
        }
    )
}

/**
 * The policy is shipped in the app rather than opened over the network: there
 * is no internet permission to fetch a hosted page with, and a policy the user
 * can only read while online is a policy they may not be able to read at all.
 */
@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val colors = SoberTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Privacy Policy",
                color = colors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PrivacySection(
                    heading = "What Sober stores",
                    body = "On this device only: per-app daily usage totals, the limits you " +
                        "set, focus-session history, override history, and your preferences. " +
                        "Your PIN is stored as a salted PBKDF2 hash — Sober cannot read it back."
                )
                PrivacySection(
                    heading = "What leaves the device",
                    body = "Sober sends nothing anywhere: it has no internet permission, no " +
                        "analytics and no advertising. Two things can still move data, and " +
                        "both are your choice. \"Export usage data\" writes a CSV and opens " +
                        "the share sheet, so you pick the destination. And if backup is on " +
                        "in your Google account, Android may copy Sober's data to your own " +
                        "backup — the operating system acting on your account, not Sober " +
                        "sending anything to us."
                )
                PrivacySection(
                    heading = "Why each permission is used",
                    body = "Usage access measures time per app. Display over other apps shows " +
                        "the block screen. Notifications report sessions and limits. " +
                        "Notification access is optional and only silences notifications " +
                        "during a session — their content is never stored or sent. " +
                        "Accessibility is optional and off by default: it reads which app is " +
                        "open and the structure of the screen so a single section can be " +
                        "closed, and the browser's address bar when you block a website. It " +
                        "does not read page content or what you type in other fields, and " +
                        "nothing on screen is stored. Device admin is optional and only prevents " +
                        "uninstallation; it is never used to lock or wipe the device."
                )
                PrivacySection(
                    heading = "Deleting your data",
                    body = "Everything Sober keeps is inside its private app storage. " +
                        "Uninstalling the app, or clearing its data in Android settings, " +
                        "removes all of it. Exported CSV files in the app cache are replaced " +
                        "on the next export and can be cleared by Android at any time."
                )
                PrivacySection(
                    heading = "Full policy",
                    body = "The complete policy, including how to contact us, is published " +
                        "at github.com/rishabhx29/Sober/blob/main/docs/PRIVACY.md"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = colors.primary, fontWeight = FontWeight.Medium)
            }
        }
    )
}

@Composable
private fun PrivacySection(heading: String, body: String) {
    Text(
        text = heading,
        color = SoberTheme.colors.textPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = body,
        color = SoberTheme.colors.textSecondary,
        fontSize = 13.sp
    )
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun AlertToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = SoberTheme.colors
    // The whole row is the target: a 48dp-wide switch alone is a small hit area,
    // and screen readers get one "title, description, on" stop instead of three.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
        SoberToggleSwitch(checked = checked, onCheckedChange = null)
    }
}

/** The installed version name, or an empty string if the system cannot report it. */
@Suppress("DEPRECATION")
private fun appVersion(context: Context): String =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

/**
 * Shown instead of the settings body when a focus session is running and a PIN
 * exists. The back arrow and button stay available on purpose: locking someone
 * out of leaving the app entirely would make the gate a trap rather than a
 * speed bump.
 */
@Composable
private fun SettingsLockScreen(
    onBack: (() -> Unit)?,
    lockedUntil: Long?,
    sessionRunning: Boolean,
    onUnlock: (String, (Boolean) -> Unit) -> Unit,
    onUnlocked: () -> Unit
) {
    val colors = SoberTheme.colors
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
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
            }
        }

        SoberScreenHeader(
            title = "Settings",
            eyebrow = "Locked",
            subtitle = when {
                lockedUntil != null -> "Lock mode is on until ${clockText(lockedUntil)}."
                sessionRunning -> "A focus session is running."
                else -> "Locked."
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        SoberCard {
            Text(
                text = "Settings are locked",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (lockedUntil != null) {
                    "Enter your PIN to change rules before the lock ends."
                } else {
                    "Enter your PIN to change limits while the session runs."
                },
                color = colors.textSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            PinField(
                value = pin,
                label = "PIN",
                onValueChange = {
                    pin = it
                    error = null
                },
                isError = error != null
            )

            error?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    color = colors.danger,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SoberButton(
                text = "Unlock",
                enabled = pin.isNotEmpty(),
                onClick = {
                    onUnlock(pin) { ok ->
                        if (ok) onUnlocked() else error = "Incorrect PIN."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (onBack != null) {
            Spacer(modifier = Modifier.height(16.dp))

            SoberButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Lock lengths offered under the Lock mode row. */
private val LOCK_LENGTHS = listOf(1 to "1 hour", 3 to "3 hours", 8 to "8 hours")

private enum class BedtimeTimeTarget { START, END }

/**
 * Category budgets are stepped in half-hours for the same reason the overall
 * cap is: this is a hard block, and a free picker invites accidental values.
 */
private val CATEGORY_BUDGET_OPTIONS = listOf(0, 30, 60, 120, 180)

private val CATEGORY_BUDGET_ROWS = listOf(
    AppCategory.SOCIAL to "Social",
    AppCategory.VIDEO to "Video",
    AppCategory.GAMES to "Games",
    AppCategory.NEWS to "News",
    AppCategory.PRODUCTIVITY to "Productivity"
)

/** "10:00 PM" from minutes since midnight, wrapping a 24+ hour value. */
private fun minuteOfDayText(minuteOfDay: Int): String {
    val normalized = ((minuteOfDay % 1440) + 1440) % 1440
    val hour24 = normalized / 60
    val hour12 = when (val h = hour24 % 12) {
        0 -> 12
        else -> h
    }
    val suffix = if (hour24 < 12) "AM" else "PM"
    return String.format("%d:%02d %s", hour12, normalized % 60, suffix)
}

/** "9:40 PM" — the wall-clock time a lock lifts. */
private fun clockText(epochMillis: Long): String {
    val at = java.util.Calendar.getInstance().apply { timeInMillis = epochMillis }
    val hour24 = at.get(java.util.Calendar.HOUR_OF_DAY)
    val hour12 = when (val h = hour24 % 12) {
        0 -> 12
        else -> h
    }
    val suffix = if (hour24 < 12) "AM" else "PM"
    return String.format("%d:%02d %s", hour12, at.get(java.util.Calendar.MINUTE), suffix)
}

@Composable
private fun PinDialog(
    hasPin: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (currentPin: String?, newPin: String, onResult: (Boolean) -> Unit) -> Unit
) {
    val colors = SoberTheme.colors
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val localError = when {
        newPin.isNotEmpty() && newPin.length !in
            SettingsViewModel.MIN_PIN_LENGTH..SettingsViewModel.MAX_PIN_LENGTH ->
            "PIN must be ${SettingsViewModel.MIN_PIN_LENGTH}–${SettingsViewModel.MAX_PIN_LENGTH} digits."
        confirmPin.isNotEmpty() && confirmPin != newPin -> "PINs don't match."
        else -> null
    }
    val canSubmit = newPin.length >= SettingsViewModel.MIN_PIN_LENGTH &&
        newPin == confirmPin &&
        localError == null &&
        (!hasPin || currentPin.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = if (hasPin) "Change PIN" else "Set PIN",
                color = colors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                if (hasPin) {
                    PinField(
                        value = currentPin,
                        label = "Current PIN",
                        onValueChange = {
                            currentPin = it
                            error = null
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                PinField(
                    value = newPin,
                    label = "New PIN",
                    onValueChange = {
                        newPin = it
                        error = null
                    },
                    isError = localError != null
                )
                Spacer(modifier = Modifier.height(12.dp))
                PinField(
                    value = confirmPin,
                    label = "Confirm new PIN",
                    onValueChange = {
                        confirmPin = it
                        error = null
                    },
                    isError = localError != null
                )

                val message = error ?: localError
                if (message != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = colors.danger,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = {
                    onSubmit(currentPin.ifEmpty { null }, newPin) { ok ->
                        if (ok) onDismiss() else error = "Current PIN is incorrect."
                    }
                }
            ) {
                Text(
                    text = "Save",
                    color = if (canSubmit) colors.primary else colors.textMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = colors.textSecondary)
            }
        }
    )
}

/**
 * The PIN prompt for a restore, separate from [PinDialog] because this one
 * authorises an action rather than setting a new code — and it says what the
 * action will do before asking.
 */
@Composable
private fun RestorePinDialog(
    onDismiss: () -> Unit,
    onVerify: (String, (Boolean) -> Unit) -> Unit,
    onVerified: () -> Unit
) {
    val colors = SoberTheme.colors
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Restore these rules?",
                color = colors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "The file replaces this device's limits, schedules and " +
                        "custom rules. Enter your PIN to continue.",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                PinField(
                    value = pin,
                    label = "PIN",
                    onValueChange = {
                        pin = it
                        error = null
                    },
                    isError = error != null
                )
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = colors.danger, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = pin.isNotEmpty(),
                onClick = {
                    onVerify(pin) { ok ->
                        if (ok) onVerified() else error = "Incorrect PIN."
                    }
                }
            ) {
                Text(
                    text = "Restore",
                    color = if (pin.isNotEmpty()) colors.primary else colors.textMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = colors.textSecondary)
            }
        }
    )
}

/** Masked, digits-only entry shared by the lock gate and the set/change dialog. */
@Composable
private fun PinField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Filter here as well as in the use case: a wrong keypress should
            // never appear in the field as a character that is not counted.
            if (input.length <= SettingsViewModel.MAX_PIN_LENGTH && input.all { it.isDigit() }) {
                onValueChange(input)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = label, fontSize = 13.sp) },
        singleLine = true,
        isError = isError,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SoberTheme.colors.textPrimary,
            unfocusedTextColor = SoberTheme.colors.textPrimary,
            focusedBorderColor = SoberTheme.colors.primary,
            unfocusedBorderColor = SoberTheme.colors.divider,
            focusedLabelColor = SoberTheme.colors.primary,
            unfocusedLabelColor = SoberTheme.colors.textMuted,
            cursorColor = SoberTheme.colors.primary,
            focusedContainerColor = SoberTheme.colors.surface,
            unfocusedContainerColor = SoberTheme.colors.surface
        )
    )
}

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

private val SOBER_ADMIN = SoberDeviceAdminReceiver::class.java
