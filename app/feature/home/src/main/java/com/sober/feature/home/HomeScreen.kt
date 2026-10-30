package com.sober.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.components.SoberHoldToCancelButton
import com.sober.core.ui.components.SoberOutlinedButton
import com.sober.core.ui.components.SoberProgressBar
import com.sober.core.ui.components.SoberSectionLabel
import com.sober.core.ui.theme.JetBrainsMono
import com.sober.core.ui.theme.SoberTheme
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
    val today = Date()

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
                    fontWeight = FontWeight.SemiBold
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
            border = androidx.compose.foundation.BorderStroke(1.dp, SoberTheme.colors.primary)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔥", fontSize = 16.sp)
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
            shape = RoundedCornerShape(14.dp),
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
                val usagePercent = if (uiState.dailyLimitMinutes > 0) {
                    (uiState.totalScreenTimeMinutes.toFloat() / uiState.dailyLimitMinutes).coerceAtMost(1f)
                } else 0f

                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = SoberTheme.colors.divider
                    val fillColor = if (usagePercent >= 1f) SoberTheme.colors.danger else SoberTheme.colors.primary
                    Canvas(modifier = Modifier.size(160.dp)) {
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
                        val hours = uiState.totalScreenTimeMinutes / 60
                        val mins = uiState.totalScreenTimeMinutes % 60
                        Text(
                            text = "${hours}h ${mins}m",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SoberTheme.colors.primary
                        )
                        Text(
                            text = "of ${limitLabel(uiState.dailyLimitMinutes)} limit",
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
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                Image(
                                    painter = rememberDrawablePainter(drawable = app.icon),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = app.appName,
                                fontSize = 14.sp,
                                color = SoberTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (app.isAtLimit) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Limit reached",
                                    tint = SoberTheme.colors.danger,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = "${app.usageMinutes}m",
                                fontSize = 14.sp,
                                color = if (app.isAtLimit) {
                                    SoberTheme.colors.danger
                                } else {
                                    SoberTheme.colors.textSecondary
                                },
                                fontWeight = FontWeight.Medium
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
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SoberTheme.colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SoberTheme.colors.divider),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SoberSectionLabel("Pickups")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.topApps.size}",
                        fontSize = 24.sp,
                        color = SoberTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "apps used",
                        fontSize = 12.sp,
                        color = SoberTheme.colors.textSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SoberTheme.colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SoberTheme.colors.divider),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SoberSectionLabel("Focus")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.sessionsToday}",
                        fontSize = 24.sp,
                        color = SoberTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "sessions today",
                        fontSize = 12.sp,
                        color = SoberTheme.colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // -- Session controls --
        // While a session runs, the buttons are replaced by the session widget:
        // starting a second session is not meaningful, and ending the current
        // one is the only action that makes sense from here.
        val session = activeSession
        if (session != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SoberTheme.colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SoberTheme.colors.primary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SoberSectionLabel("Active session")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = session.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoberTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.remainingText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMono,
                        color = SoberTheme.colors.primary
                    )
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
                    onClick = { viewModel.stopFocusMode() }
                )
            }
        } else {
            SoberButton(
                text = "Start Focus Session",
                onClick = onStartFocusSession
            )

            Spacer(modifier = Modifier.height(12.dp))

            SoberOutlinedButton(
                text = "Focus Mode",
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
