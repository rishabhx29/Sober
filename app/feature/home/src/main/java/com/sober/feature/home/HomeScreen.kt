package com.sober.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.components.SoberHoldToCancelButton
import com.sober.core.ui.components.SoberIconBadge
import com.sober.core.ui.components.SoberOutlinedButton
import com.sober.core.ui.components.SoberProgressBar
import com.sober.core.ui.components.SoberSectionLabel
import com.sober.core.ui.components.SoberStatTile
import com.sober.core.ui.components.rememberBreathingScale
import com.sober.core.ui.format.spokenDuration
import com.sober.core.ui.format.spokenRemaining
import com.sober.core.ui.theme.JetBrainsMono
import com.sober.core.ui.theme.SoberTheme
import com.sober.core.ui.theme.TabularNumberStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit = {},
    onStartFocusSession: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val scrollState = rememberScrollState()

    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    // Re-read on resume as well: an app left open overnight would otherwise
    // keep yesterday's date in the header, since nothing else here recomposes.
    var today by remember { mutableStateOf(Date()) }

    // Usage is read once per ViewModel otherwise, so every figure on this screen
    // froze at the moment the app was opened — the ring could still say "42m
    // used" after an hour elsewhere. The back-stack entry goes RESUMED again on
    // returning to this tab or to the app, which is exactly when to re-read.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadUsageData()
                today = Date()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoberTheme.colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // -- Header --
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = dayFormat.format(today).uppercase(),
                    fontSize = 11.sp,
                    color = SoberTheme.colors.textMuted,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(today),
                    fontSize = 22.sp,
                    color = SoberTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() }
                )
            }

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = SoberTheme.colors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // -- Streak Pill --
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, SoberTheme.colors.primary),
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("Day ${uiState.streakDays} streak")
                    if (uiState.longestStreakDays > uiState.streakDays) {
                        append(". Best: ${uiState.longestStreakDays} days")
                    }
                }
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = SoberTheme.colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Day ${uiState.streakDays} Streak",
                        color = SoberTheme.colors.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    // Only worth a second line once there is a past best to
                    // be proud of; otherwise it repeats the streak above.
                    if (uiState.longestStreakDays > uiState.streakDays) {
                        Text(
                            text = "Best: ${uiState.longestStreakDays} days",
                            color = SoberTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // -- Main Usage Card --
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SoberTheme.colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SoberTheme.colors.divider),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Usage Ring
                val targetPercent = if (uiState.dailyLimitMinutes > 0) {
                    (uiState.totalScreenTimeMinutes.toFloat() / uiState.dailyLimitMinutes).coerceAtMost(1f)
                } else 0f
                val usagePercent by animateFloatAsState(
                    targetValue = targetPercent,
                    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    label = "usage-ring"
                )
                val usageHours = uiState.totalScreenTimeMinutes / 60
                val usageMins = uiState.totalScreenTimeMinutes % 60

                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = buildString {
                                append("${spokenDuration(uiState.totalScreenTimeMinutes)} used")
                                if (uiState.dailyLimitMinutes > 0) {
                                    append(" of ${spokenDuration(uiState.dailyLimitMinutes)} daily limit")
                                }
                                if (targetPercent >= 1f) append(", limit reached")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = SoberTheme.colors.divider
                    val fillColor = if (targetPercent >= 1f) SoberTheme.colors.danger else SoberTheme.colors.primary
                    Canvas(modifier = Modifier.size(168.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val topLeft = Offset(
                            (size.width - 2 * radius) / 2,
                            (size.height - 2 * radius) / 2
                        )

                        // Track
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Fill
                        drawArc(
                            color = fillColor,
                            startAngle = -90f,
                            sweepAngle = 360f * usagePercent,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Center text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${usageHours}h ${usageMins}m",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SoberTheme.colors.primary,
                            style = TabularNumberStyle
                        )
                        Text(
                            text = if (uiState.dailyLimitMinutes > 0) {
                                "of ${limitLabel(uiState.dailyLimitMinutes)} limit"
                            } else {
                                "today"
                            },
                            fontSize = 12.sp,
                            color = SoberTheme.colors.textMuted
                        )
                    }
                }

                // Top Apps
                if (uiState.topApps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = SoberTheme.colors.divider)
                    Spacer(modifier = Modifier.height(12.dp))

                    uiState.topApps.take(3).forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = buildString {
                                        append("${app.appName}, ${spokenDuration(app.usageMinutes)}")
                                        if (app.isAtLimit) append(", limit reached")
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SoberTheme.colors.primaryMuted),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = rememberDrawablePainter(drawable = app.icon),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(32.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = app.appName,
                                fontSize = 14.sp,
                                color = SoberTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (app.isAtLimit) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = SoberTheme.colors.danger,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "${app.usageMinutes}m",
                                fontSize = 14.sp,
                                color = if (app.isAtLimit) {
                                    SoberTheme.colors.danger
                                } else {
                                    SoberTheme.colors.textSecondary
                                },
                                fontWeight = FontWeight.Medium,
                                style = TabularNumberStyle
                            )
                        }

                        // No bar for an app without a limit: a progress bar needs
                        // a denominator, and using the daily cap would claim the
                        // app is close to a limit it does not have.
                        app.progress?.let { progress ->
                            SoberProgressBar(progress = progress, isDanger = app.isAtLimit)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // -- Quick Stats Row --
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Counts every app with foreground time today, not the three rows
            // the card shows.
            SoberStatTile(
                icon = Icons.Rounded.Apps,
                label = "Apps used",
                value = "${uiState.appsUsedToday}",
                caption = "today",
                modifier = Modifier.weight(1f)
            )
            SoberStatTile(
                icon = Icons.Rounded.Timer,
                label = "Focus",
                value = "${uiState.sessionsToday}",
                caption = "sessions today",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // -- Session controls --
        // While a session runs, the buttons are replaced by the session widget:
        // starting a second session is not meaningful, and ending the current
        // one is the only action that makes sense from here.
        val session = activeSession
        if (session != null) {
            val breathe = rememberBreathingScale(minScale = 1f, maxScale = 1.04f, periodMillis = 3200)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SoberTheme.colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SoberTheme.colors.primary),
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "Active session. ${session.label}. " +
                        "${spokenRemaining(session.remainingText)} remaining."
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SoberIconBadge(
                            imageVector = Icons.Rounded.SelfImprovement,
                            contentDescription = null,
                            size = 52.dp,
                            iconSize = 26.dp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = breathe
                                scaleY = breathe
                            }
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            SoberSectionLabel(text = "Active session", isHeading = false)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = session.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SoberTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = session.remainingText,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMono,
                                style = TabularNumberStyle,
                                color = SoberTheme.colors.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SoberHoldToCancelButton(
                        onCancelConfirmed = { viewModel.endSession() }
                    )
                }
            }
            // An open-ended session has no countdown to run out, so the control
            // that started it stays visible as the way to stop it (PRD 2.6).
            if (session.isOpenEnded) {
                Spacer(modifier = Modifier.height(12.dp))
                SoberButton(
                    text = "Focus Mode is on — tap to turn off",
                    icon = Icons.Rounded.SelfImprovement,
                    onClick = { viewModel.stopFocusMode() }
                )
            }
        } else {
            SoberButton(
                text = "Start Focus Session",
                icon = Icons.Rounded.PlayArrow,
                onClick = onStartFocusSession
            )

            Spacer(modifier = Modifier.height(12.dp))

            SoberOutlinedButton(
                text = "Focus Mode",
                icon = Icons.Rounded.SelfImprovement,
                onClick = { viewModel.startFocusMode() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Indefinite focus. Blocks distracting apps until you turn it off.",
                color = SoberTheme.colors.textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            uiState.focusModeNotice?.let { notice ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notice,
                    color = SoberTheme.colors.danger,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * The cap is adjustable in 30-minute steps, so whole hours cannot be assumed:
 * 90 minutes must read "1h 30m", not "1h".
 */
internal fun limitLabel(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}
