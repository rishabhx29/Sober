package com.sober.feature.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sober.core.common.permissions.PermissionUtils
import com.sober.core.common.presets.PresetMode
import com.sober.core.domain.model.NotificationSilenceScope
import com.sober.core.domain.model.SessionStatus
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.components.SoberDrumPicker
import com.sober.core.ui.components.SoberSegmentedControl
import com.sober.core.ui.components.SoberToggleSwitch
import com.sober.core.ui.theme.SoberTheme
import com.sober.core.ui.components.SoberHoldToCancelButton
import com.sober.feature.focus.components.PresetCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusScreen(
    viewModel: FocusViewModel = hiltViewModel()
) {
    val recentSessions by viewModel.recentSessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val celebration by viewModel.celebration.collectAsState()
    val remainingTime by viewModel.remainingTimeText.collectAsState()
    val activeSilenceScope by viewModel.activeSilenceScope.collectAsState()
    val presetNotice by viewModel.presetNotice.collectAsState()
    val mealBreakRemaining by viewModel.mealBreakRemainingText.collectAsState()
    val completedSession = celebration

    var sessionLabel by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableIntStateOf(25) }
    var showCustomDuration by remember { mutableStateOf(false) }
    var customDurationMinutes by remember { mutableIntStateOf(0) }
    var openEnded by remember { mutableStateOf(false) }
    val isCustomDuration = customDurationMinutes > 0 && customDurationMinutes !in DURATION_PRESETS

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoberTheme.colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        if (activeSessionId != null) {
            // ============ ACTIVE SESSION VIEW ============
            Text(
                text = "ACTIVE SESSION",
                fontSize = 11.sp,
                color = SoberTheme.colors.danger,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(2.dp, SoberTheme.colors.primary),
                color = SoberTheme.colors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🧘",
                        fontSize = 40.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Focus Mode Active",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoberTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = remainingTime,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoberTheme.colors.primary,
                        fontFamily = com.sober.core.ui.theme.JetBrainsMono
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "remaining",
                        fontSize = 14.sp,
                        color = SoberTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    NotificationAccessNotice(silenceScope = activeSilenceScope)

                    SoberHoldToCancelButton(
                        onCancelConfirmed = {
                            viewModel.cancelSession(activeSessionId!!)
                        }
                    )
                }
            }
        } else if (completedSession != null) {
            // ============ SESSION COMPLETE VIEW ============
            SessionCompleteCard(
                celebration = completedSession,
                onGoAgain = { viewModel.repeatSession() },
                onDismiss = { viewModel.dismissCelebration() }
            )
        } else {
            // ============ SETUP SESSION VIEW ============
            Text(
                text = "START A SESSION",
                fontSize = 11.sp,
                color = SoberTheme.colors.textMuted,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SoberTheme.colors.divider),
                color = SoberTheme.colors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Session name
                    OutlinedTextField(
                        value = sessionLabel,
                        onValueChange = { sessionLabel = it },
                        placeholder = {
                            Text("Name this session...", color = SoberTheme.colors.textMuted)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoberTheme.colors.primary,
                            unfocusedBorderColor = SoberTheme.colors.divider,
                            focusedContainerColor = SoberTheme.colors.surface,
                            unfocusedContainerColor = SoberTheme.colors.surface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Duration selector: presets in two rows, plus a validated
                    // custom duration so the picker is not the old four-chip cap.
                    // Open-ended replaces the whole selector because a session
                    // with no end has nothing to pick.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open-ended",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = SoberTheme.colors.primary
                            )
                            Text(
                                text = "Runs until you turn it off",
                                fontSize = 12.sp,
                                color = SoberTheme.colors.textSecondary
                            )
                        }
                        SoberToggleSwitch(
                            checked = openEnded,
                            onCheckedChange = { openEnded = it }
                        )
                    }

                    if (!openEnded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SoberSegmentedControl(
                            options = DURATION_PRESETS.take(4),
                            selected = selectedDuration,
                            onSelect = { selectedDuration = it },
                            label = { durationLabel(it) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SoberSegmentedControl(
                            options = DURATION_PRESETS.drop(4),
                            selected = selectedDuration,
                            onSelect = { selectedDuration = it },
                            label = { durationLabel(it) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { showCustomDuration = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isCustomDuration) SoberTheme.colors.primary else SoberTheme.colors.divider
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SoberTheme.colors.primary
                            )
                        ) {
                            Text(
                                text = if (isCustomDuration) {
                                    "Custom · ${durationLabel(selectedDuration)}"
                                } else {
                                    "Custom duration"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (showCustomDuration) {
                            CustomDurationDialog(
                                initialMinutes = selectedDuration,
                                onDismiss = { showCustomDuration = false },
                                onConfirm = { minutes ->
                                    customDurationMinutes = minutes
                                    selectedDuration = minutes
                                    showCustomDuration = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    var silenceScope by remember {
                        mutableStateOf(NotificationSilenceScope.SELECTED_APPS)
                    }
                    val silenceEnabled = silenceScope != NotificationSilenceScope.NONE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Silence notifications",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = SoberTheme.colors.textPrimary
                            )
                            Text(
                                text = if (silenceEnabled) {
                                    scopeSummary(silenceScope)
                                } else {
                                    "Alerts come through as usual"
                                },
                                fontSize = 12.sp,
                                color = SoberTheme.colors.textSecondary
                            )
                        }
                        SoberToggleSwitch(
                            checked = silenceEnabled,
                            onCheckedChange = { enabled ->
                                silenceScope = if (enabled) {
                                    NotificationSilenceScope.SELECTED_APPS
                                } else {
                                    NotificationSilenceScope.NONE
                                }
                            }
                        )
                    }

                    if (silenceEnabled) {
                        SoberSegmentedControl(
                            options = SILENCE_SCOPES,
                            selected = silenceScope,
                            onSelect = { silenceScope = it },
                            label = { scopeLabel(it) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = scopeDetail(silenceScope),
                            fontSize = 12.sp,
                            color = SoberTheme.colors.textMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    var showAppSelection by remember { mutableStateOf(false) }
                    var selectedApps by remember { mutableStateOf(setOf<String>()) }
                    
                    OutlinedButton(
                        onClick = { showAppSelection = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SoberTheme.colors.primary
                        ),
                        border = BorderStroke(1.dp, SoberTheme.colors.primary)
                    ) {
                        Text(
                            text = if (selectedApps.isEmpty()) "Select apps to block" else "${selectedApps.size} apps blocked",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (showAppSelection) {
                        val installedApps by viewModel.installedApps.collectAsState()
                        val context = androidx.compose.ui.platform.LocalContext.current
                        LaunchedEffect(Unit) {
                            if (installedApps.isEmpty()) {
                                viewModel.loadApps(context.packageManager)
                            }
                        }
                        
                        com.sober.feature.focus.components.AppSelectionBottomSheet(
                            apps = installedApps,
                            selectedApps = selectedApps,
                            onAppSelected = { packageName, isSelected ->
                                selectedApps = if (isSelected) {
                                    selectedApps + packageName
                                } else {
                                    selectedApps - packageName
                                }
                            },
                            onDismiss = { showAppSelection = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Start button
                    SoberButton(
                        text = "Start Session",
                        onClick = {
                            val finalLabel = if (sessionLabel.isNotBlank()) sessionLabel else "Focus Session"
                            val duration = if (openEnded) 0 else selectedDuration
                            viewModel.startSession(finalLabel, duration, selectedApps.toList(), silenceScope)
                            sessionLabel = ""
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ============ QUICK MODES ============
        // Hidden while a session runs: starting a preset would orphan the
        // active session, so the only actions offered then are end or pause.
        if (activeSessionId == null) {
            Text(
                text = "QUICK MODES",
                fontSize = 11.sp,
                color = SoberTheme.colors.textMuted,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetMode.entries.forEach { mode ->
                    item {
                        PresetCard(mode.label, mode.description) {
                            viewModel.startPreset(mode)
                        }
                    }
                }
                item {
                    PresetCard("Meal break", "20m · Pause blocks") {
                        viewModel.startMealBreak()
                    }
                }
            }
        }

        presetNotice?.let { notice ->
            Text(
                text = notice,
                color = SoberTheme.colors.danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        mealBreakRemaining?.let { remaining ->
            MealBreakBanner(remaining = remaining, onEndNow = { viewModel.endMealBreak() })
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ============ RECENT SESSIONS ============
        Text(
            text = "RECENT SESSIONS",
            fontSize = 11.sp,
            color = SoberTheme.colors.textMuted,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (recentSessions.isEmpty()) {
            Text(
                text = "No sessions yet. Start your first one above!",
                fontSize = 14.sp,
                color = SoberTheme.colors.textMuted,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            recentSessions.forEach { session ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SoberTheme.colors.divider),
                    color = SoberTheme.colors.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = session.label,
                                fontWeight = FontWeight.SemiBold,
                                color = SoberTheme.colors.primary,
                                fontSize = 15.sp
                            )
                            val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            Text(
                                text = df.format(Date(session.startedAt)),
                                fontSize = 12.sp,
                                color = SoberTheme.colors.textSecondary
                            )
                            // PRD 4.2.4: a cancelled session is marked with a
                            // broken-streak indicator, not just a red label.
                            if (session.status == SessionStatus.CANCELLED) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = buildString {
                                        append("Streak broken")
                                        session.cancelledAtMinute?.let { append(" · cancelled at ${it}m") }
                                    },
                                    fontSize = 11.sp,
                                    color = SoberTheme.colors.danger
                                )
                            }
                        }

                        val statusText = session.status.name
                        val statusColor = when (statusText) {
                            "COMPLETED" -> SoberTheme.colors.primary
                            "CANCELLED" -> SoberTheme.colors.danger
                            else -> SoberTheme.colors.textMuted
                        }
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * TASK 2.7: what a session earns when it runs its full length. It replaces
 * the setup form instead of sitting beside it — "go again" is the only
 * sensible next action after finishing.
 */
@Composable
private fun SessionCompleteCard(
    celebration: FocusViewModel.CelebrationUi,
    onGoAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    Text(
        text = "SESSION COMPLETE",
        fontSize = 11.sp,
        color = SoberTheme.colors.primary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, SoberTheme.colors.primary),
        color = SoberTheme.colors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✓",
                fontSize = 40.sp,
                color = SoberTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = celebration.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SoberTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = durationLabel(celebration.durationMinutes),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = SoberTheme.colors.primary,
                fontFamily = com.sober.core.ui.theme.JetBrainsMono
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "focused",
                fontSize = 14.sp,
                color = SoberTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (celebration.blockedApps.isNotEmpty()) {
                    CelebrationStat(
                        value = celebration.blockedApps.size.toString(),
                        label = "apps avoided"
                    )
                }
                if (celebration.streakDays > 0) {
                    CelebrationStat(
                        value = celebration.streakDays.toString(),
                        label = "day streak"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SoberButton(text = "Go again", onClick = onGoAgain)
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Done",
                    color = SoberTheme.colors.textMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MealBreakBanner(remaining: String, onEndNow: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SoberTheme.colors.primary),
        color = SoberTheme.colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Meal break",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoberTheme.colors.primary
                )
                Text(
                    text = "All blocks paused · $remaining left",
                    fontSize = 12.sp,
                    color = SoberTheme.colors.textSecondary
                )
            }
            TextButton(onClick = onEndNow) {
                Text(
                    text = "End now",
                    color = SoberTheme.colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CelebrationStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = SoberTheme.colors.primary,
            fontFamily = com.sober.core.ui.theme.JetBrainsMono
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = SoberTheme.colors.textMuted
        )
    }
}

/** Preset durations from PRD 4.2.1. Custom durations sit outside this list. */
private val DURATION_PRESETS = listOf(15, 25, 30, 45, 60, 90, 120)

private const val MIN_CUSTOM_MINUTES = 5
private const val MAX_CUSTOM_MINUTES = 12 * 60

private fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

/**
 * Validated custom duration. The drums allow any minute value, but a session
 * under five minutes is not a focus session and over twelve hours is a lockout
 * risk, so the confirm button stays disabled outside that range.
 */
@Composable
private fun CustomDurationDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hours by remember { mutableIntStateOf(initialMinutes / 60) }
    var minutes by remember { mutableIntStateOf(initialMinutes % 60) }
    val total = hours * 60 + minutes
    val isValid = total in MIN_CUSTOM_MINUTES..MAX_CUSTOM_MINUTES

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoberTheme.colors.surface,
        title = {
            Text(
                text = "Custom duration",
                color = SoberTheme.colors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HOURS",
                            color = SoberTheme.colors.textMuted,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        SoberDrumPicker(
                            value = hours,
                            range = 0..12,
                            onValueChange = { hours = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MINUTES",
                            color = SoberTheme.colors.textMuted,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        SoberDrumPicker(
                            value = minutes,
                            range = 0..59,
                            onValueChange = { minutes = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (!isValid) {
                    Text(
                        text = "Choose between 5 minutes and 12 hours.",
                        color = SoberTheme.colors.danger,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(total) },
                enabled = isValid
            ) {
                Text(
                    text = "Set",
                    color = if (isValid) SoberTheme.colors.primary else SoberTheme.colors.textMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = SoberTheme.colors.textSecondary)
            }
        }
    )
}

/** Selectable notification scopes, in the order the PRD lists them. */
private val SILENCE_SCOPES = listOf(
    NotificationSilenceScope.ALL,
    NotificationSilenceScope.SELECTED_APPS,
    NotificationSilenceScope.ALL_EXCEPT_CALLS
)

private fun scopeLabel(scope: NotificationSilenceScope): String = when (scope) {
    NotificationSilenceScope.ALL -> "All"
    NotificationSilenceScope.SELECTED_APPS -> "My apps"
    NotificationSilenceScope.ALL_EXCEPT_CALLS -> "Except calls"
    NotificationSilenceScope.NONE -> "Off"
}

private fun scopeSummary(scope: NotificationSilenceScope): String = when (scope) {
    NotificationSilenceScope.ALL -> "Silencing every alert"
    NotificationSilenceScope.SELECTED_APPS -> "Silencing the apps you blocked"
    NotificationSilenceScope.ALL_EXCEPT_CALLS -> "Silencing everything but calls"
    NotificationSilenceScope.NONE -> "Alerts come through as usual"
}

private fun scopeDetail(scope: NotificationSilenceScope): String = when (scope) {
    NotificationSilenceScope.ALL -> "Every notification is hidden for the length of the session."
    NotificationSilenceScope.SELECTED_APPS -> "Only notifications from the apps you block are hidden."
    NotificationSilenceScope.ALL_EXCEPT_CALLS -> "Everything is hidden except incoming phone calls."
    NotificationSilenceScope.NONE -> "Notifications are not affected."
}

/**
 * TASK 2.3.6: notification access can be revoked mid-session (Android resets
 * listener access on some updates, or the user toggles it off). Silent
 * failure here is the worst outcome — the session promises quiet but alerts
 * keep arriving — so the active card states it plainly and links to the
 * toggle.
 *
 * The check is re-run on ON_RESUME rather than every tick: the user leaves for
 * Settings and comes back, and that is the only moment the answer can change.
 */
@Composable
private fun NotificationAccessNotice(silenceScope: NotificationSilenceScope?) {
    if (silenceScope == null || silenceScope == NotificationSilenceScope.NONE) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(PermissionUtils.hasNotificationListenerPermission(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = PermissionUtils.hasNotificationListenerPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (granted) return

    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SoberTheme.colors.danger),
        color = SoberTheme.colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Notification blocking is off",
                color = SoberTheme.colors.danger,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Sober lost notification access, so alerts are not being silenced. Your apps are still blocked.",
                color = SoberTheme.colors.textSecondary,
                fontSize = 12.sp
            )
            TextButton(
                onClick = { context.startActivity(PermissionUtils.getNotificationListenerIntent()) },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Re-enable notification access",
                    color = SoberTheme.colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

