package com.sober.feature.focus

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.sober.core.ui.components.SoberDivider
import com.sober.core.ui.components.SoberDrumPicker
import com.sober.core.ui.components.SoberFilterChips
import com.sober.core.ui.components.SoberIconBadge
import com.sober.core.ui.components.SoberListRow
import com.sober.core.ui.components.SoberOutlinedButton
import com.sober.core.ui.components.SoberScreenHeader
import com.sober.core.ui.components.SoberSectionHeader
import com.sober.core.ui.components.SoberToggleSwitch
import com.sober.core.ui.components.rememberBreathingScale
import com.sober.core.ui.format.spokenRemaining
import com.sober.core.ui.theme.JetBrainsMono
import com.sober.core.ui.theme.SoberTheme
import com.sober.core.ui.theme.TabularNumberStyle
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
    val focusSoundEnabled by viewModel.focusSoundEnabled.collectAsState()
    val presetNotice by viewModel.presetNotice.collectAsState()
    val mealBreakRemaining by viewModel.mealBreakRemainingText.collectAsState()
    val completedSession = celebration

    var sessionLabel by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableIntStateOf(25) }
    var showCustomDuration by remember { mutableStateOf(false) }
    var openEnded by remember { mutableStateOf(false) }
    // Derived, not stored: keeping a separate "custom minutes" value let the
    // Custom button keep advertising 50m after the user tapped a 25m preset.
    val isCustomDuration = !isPresetDuration(selectedDuration)

    val scrollState = rememberScrollState()

    // The setup form occupies sections 01 (Length) and 02 (Session). Once a
    // session is running or has just finished those disappear, so the sections
    // below renumber from 01 rather than starting at 03 with nothing above.
    val setupVisible = activeSessionId == null && completedSession == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoberTheme.colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        if (activeSessionId != null) {
            // ============ ACTIVE SESSION VIEW ============
            SoberScreenHeader(
                title = "Focus",
                eyebrow = "Active session"
            )

            Spacer(modifier = Modifier.height(20.dp))

            val breathe = rememberBreathingScale(
                minScale = 1f,
                maxScale = 1.05f,
                periodMillis = 3200
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SoberTheme.colors.cardBorder),
                color = SoberTheme.colors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Focus session active. " +
                            "${spokenRemaining(remainingTime)} remaining."
                    }
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(SoberTheme.colors.primary)
                    )
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    SoberIconBadge(
                        imageVector = Icons.Rounded.SelfImprovement,
                        contentDescription = null,
                        size = 72.dp,
                        iconSize = 34.dp,
                        containerColor = SoberTheme.colors.primaryMuted,
                        modifier = Modifier.graphicsLayer {
                            scaleX = breathe
                            scaleY = breathe
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Focus Mode Active",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoberTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = remainingTime,
                        fontSize = 44.sp,
                        lineHeight = 48.sp,
                        letterSpacing = (-1).sp,
                        fontWeight = FontWeight.Bold,
                        color = SoberTheme.colors.textPrimary,
                        fontFamily = JetBrainsMono,
                        style = TabularNumberStyle
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "remaining",
                        fontSize = 13.sp,
                        color = SoberTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    NotificationAccessNotice(silenceScope = activeSilenceScope)

                    // The engine reads the preference live, so turning the
                    // sound off mid-session works; without this row the only
                    // way to stop it would be ending the session.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = focusSoundEnabled,
                                role = Role.Switch,
                                onValueChange = viewModel::setFocusSound
                            )
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Brown noise",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = SoberTheme.colors.textPrimary
                            )
                            Text(
                                text = if (focusSoundEnabled) {
                                    "Playing · generated on device"
                                } else {
                                    "Off"
                                },
                                fontSize = 11.sp,
                                color = SoberTheme.colors.textSecondary
                            )
                        }
                        SoberToggleSwitch(
                            checked = focusSoundEnabled,
                            onCheckedChange = null
                        )
                    }

                    SoberHoldToCancelButton(
                        onCancelConfirmed = {
                            // The session can end between composition and the
                            // tap (the service ticker completes it), which
                            // would turn `!!` into a crash on a finished session.
                            val id = activeSessionId
                            if (id != null) viewModel.cancelSession(id)
                        }
                    )
                    }
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
            SoberScreenHeader(
                title = "Focus",
                eyebrow = "Do not disturb",
                subtitle = "One intention, one session. Choose how long it lasts."
            )

            Spacer(modifier = Modifier.height(28.dp))

            SoberSectionHeader(title = "Length", index = "01")
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = if (openEnded) {
                        "Session length: open-ended"
                    } else {
                        "Session length: $selectedDuration minutes"
                    }
                },
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (openEnded) "∞" else "$selectedDuration",
                    fontSize = 58.sp,
                    lineHeight = 62.sp,
                    letterSpacing = (-2).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoberTheme.colors.textPrimary,
                    fontFamily = JetBrainsMono,
                    style = TabularNumberStyle
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (openEnded) "minutes · until you stop it" else "minutes",
                    fontSize = 13.sp,
                    color = SoberTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 9.dp)
                )
            }

            if (!openEnded) {
                Spacer(modifier = Modifier.height(14.dp))
                SoberFilterChips(
                    options = DURATION_PRESETS,
                    selected = selectedDuration,
                    onSelect = { selectedDuration = it },
                    label = { durationLabel(it) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                SoberOutlinedButton(
                    text = if (isCustomDuration) {
                        "Custom · ${durationLabel(selectedDuration)}"
                    } else {
                        "Custom duration"
                    },
                    onClick = { showCustomDuration = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = openEnded,
                        role = Role.Switch,
                        onValueChange = { openEnded = it }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Open-ended",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = SoberTheme.colors.textPrimary
                    )
                    Text(
                        text = "Runs until you turn it off",
                        fontSize = 12.sp,
                        color = SoberTheme.colors.textSecondary
                    )
                }
                SoberToggleSwitch(
                    checked = openEnded,
                    onCheckedChange = null
                )
            }

            if (showCustomDuration) {
                CustomDurationDialog(
                    initialMinutes = selectedDuration,
                    onDismiss = { showCustomDuration = false },
                    onConfirm = { minutes ->
                        selectedDuration = minutes
                        showCustomDuration = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            SoberSectionHeader(title = "Session", index = "02")
            Spacer(modifier = Modifier.height(6.dp))

            TextField(
                value = sessionLabel,
                onValueChange = { sessionLabel = it },
                placeholder = {
                    Text(
                        text = "Name this session…",
                        color = SoberTheme.colors.textMuted,
                        fontSize = 15.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = SoberTheme.colors.primary,
                    unfocusedIndicatorColor = SoberTheme.colors.divider,
                    cursorColor = SoberTheme.colors.primary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            var silenceScope by remember {
                mutableStateOf(NotificationSilenceScope.SELECTED_APPS)
            }
            val silenceEnabled = silenceScope != NotificationSilenceScope.NONE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = silenceEnabled,
                        role = Role.Switch,
                        onValueChange = { enabled ->
                            silenceScope = if (enabled) {
                                NotificationSilenceScope.SELECTED_APPS
                            } else {
                                NotificationSilenceScope.NONE
                            }
                        }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Silence notifications",
                        fontSize = 15.sp,
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
                    onCheckedChange = null
                )
            }

            if (silenceEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                SoberFilterChips(
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

            Spacer(modifier = Modifier.height(10.dp))

            // A listening habit rather than a per-session choice: the value is
            // stored globally, and the engine reads it live, so turning it on
            // mid-session works too.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = focusSoundEnabled,
                        role = Role.Switch,
                        onValueChange = viewModel::setFocusSound
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Brown noise",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = SoberTheme.colors.textPrimary
                    )
                    Text(
                        text = if (focusSoundEnabled) {
                            "Generated on device · follows your media volume"
                        } else {
                            "A steady background while the session runs"
                        },
                        fontSize = 12.sp,
                        color = SoberTheme.colors.textSecondary
                    )
                }
                SoberToggleSwitch(
                    checked = focusSoundEnabled,
                    onCheckedChange = null
                )
            }

            var showAppSelection by remember { mutableStateOf(false) }
            var selectedApps by remember { mutableStateOf(setOf<String>()) }

            SoberDivider()
            SoberListRow(
                title = "Apps to block",
                subtitle = when (selectedApps.size) {
                    0 -> "None selected"
                    1 -> "1 app blocked"
                    else -> "${selectedApps.size} apps blocked"
                },
                showChevron = true,
                onClick = { showAppSelection = true }
            )
            SoberDivider()

            if (showAppSelection) {
                val installedApps by viewModel.installedApps.collectAsState()
                val context = LocalContext.current
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

            SoberButton(
                text = "Start Session",
                icon = Icons.Rounded.PlayArrow,
                onClick = {
                    val finalLabel = if (sessionLabel.isNotBlank()) sessionLabel else "Focus Session"
                    val duration = if (openEnded) 0 else selectedDuration
                    viewModel.startSession(finalLabel, duration, selectedApps.toList(), silenceScope)
                    sessionLabel = ""
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ============ QUICK MODES ============
        // Hidden while a session runs: starting a preset would orphan the
        // active session, so the only actions offered then are end or pause.
        if (activeSessionId == null) {
            SoberSectionHeader(
                title = "Quick modes",
                index = if (setupVisible) "03" else "01"
            )

            Spacer(modifier = Modifier.height(10.dp))

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
        // Quick modes is hidden mid-session, so the last section shifts up
        // instead of leaving a gap in the sequence.
        SoberSectionHeader(
            title = "Recent sessions",
            index = when {
                setupVisible -> "04"
                completedSession != null -> "02"
                else -> "01"
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (recentSessions.isEmpty()) {
            Text(
                text = "No sessions yet. Start your first one above!",
                fontSize = 14.sp,
                color = SoberTheme.colors.textMuted,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            val df = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            recentSessions.forEachIndexed { index, session ->
                val dateLabel = df.format(Date(session.startedAt))
                val cancelledDetail = buildString {
                    append("Streak broken")
                    session.cancelledAtMinute?.let { append(" · cancelled at ${it}m") }
                }
                if (index > 0) SoberDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = buildString {
                                append("${session.label}, ")
                                append("${sessionLengthLabel(session.durationMinutes)}, ")
                                append("$dateLabel, ")
                                append(session.status.name.lowercase().replaceFirstChar { it.uppercase() })
                                if (session.status == SessionStatus.CANCELLED) {
                                    append(". $cancelledDetail")
                                }
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.label,
                            fontWeight = FontWeight.SemiBold,
                            color = SoberTheme.colors.textPrimary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${sessionLengthLabel(session.durationMinutes)} · $dateLabel",
                            fontSize = 12.sp,
                            color = SoberTheme.colors.textSecondary
                        )
                        // PRD 4.2.4: a cancelled session is marked with a
                        // broken-streak indicator, not just a red label.
                        if (session.status == SessionStatus.CANCELLED) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = cancelledDetail,
                                fontSize = 11.sp,
                                color = SoberTheme.colors.danger
                            )
                        }
                    }

                    val statusColor = when (session.status) {
                        SessionStatus.COMPLETED -> SoberTheme.colors.textSecondary
                        SessionStatus.CANCELLED -> SoberTheme.colors.danger
                        else -> SoberTheme.colors.textMuted
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (session.status == SessionStatus.COMPLETED) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.Cancel
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = session.status.name
                                .lowercase()
                                .replaceFirstChar { it.uppercase() },
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
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val badgeScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "complete-badge-scale"
    )
    val badgeAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "complete-badge-alpha"
    )

    SoberScreenHeader(
        title = "Well done",
        eyebrow = "Session complete"
    )

    Spacer(modifier = Modifier.height(20.dp))

    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SoberTheme.colors.cardBorder),
        color = SoberTheme.colors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(SoberTheme.colors.primary)
            )
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            SoberIconBadge(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                size = 64.dp,
                iconSize = 32.dp,
                containerColor = SoberTheme.colors.primaryMuted,
                modifier = Modifier.graphicsLayer {
                    scaleX = badgeScale
                    scaleY = badgeScale
                    alpha = badgeAlpha
                }
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
                fontSize = 44.sp,
                lineHeight = 48.sp,
                letterSpacing = (-1).sp,
                fontWeight = FontWeight.Bold,
                color = SoberTheme.colors.textPrimary,
                fontFamily = JetBrainsMono,
                style = TabularNumberStyle
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "focused",
                fontSize = 13.sp,
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
            SoberButton(
                text = "Go again",
                icon = Icons.Rounded.Replay,
                onClick = onGoAgain
            )
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
            fontFamily = JetBrainsMono,
            style = TabularNumberStyle
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

/** A chip is selected exactly when the duration is one of the preset values. */
internal fun isPresetDuration(minutes: Int): Boolean = minutes in DURATION_PRESETS

private const val MIN_CUSTOM_MINUTES = 5
private const val MAX_CUSTOM_MINUTES = 12 * 60

private fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

/** History rows need to tell an open-ended session apart from a zero-length one. */
internal fun sessionLengthLabel(minutes: Int): String =
    if (minutes <= 0) "Open-ended" else durationLabel(minutes)

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
                onClick = {
                    PermissionUtils.openSettings(
                        context,
                        PermissionUtils.getNotificationListenerIntent()
                    )
                },
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

