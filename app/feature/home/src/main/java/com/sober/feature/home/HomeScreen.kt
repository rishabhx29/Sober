package com.sober.feature.home

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
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
import com.sober.core.ui.components.SoberDivider
import com.sober.core.ui.components.SoberHoldToCancelButton
import com.sober.core.ui.components.SoberIconBadge
import com.sober.core.ui.components.SoberInlineMetric
import com.sober.core.ui.components.SoberOutlinedButton
import com.sober.core.ui.components.SoberProgressBar
import com.sober.core.ui.components.SoberScreenHeader
import com.sober.core.ui.components.SoberSectionHeader
import com.sober.core.ui.components.SoberSectionLabel
import com.sober.core.ui.components.rememberBreathingScale
import com.sober.core.ui.format.spokenDuration
import com.sober.core.ui.format.spokenRemaining
import com.sober.core.ui.theme.JetBrainsMono
import com.sober.core.ui.theme.SoberTheme
import com.sober.core.ui.theme.TabularNumberStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
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
        SoberScreenHeader(
            eyebrow = dayFormat.format(today),
            title = dateFormat.format(today),
            trailing = {
                StreakBadge(
                    streakDays = uiState.streakDays,
                    longestStreakDays = uiState.longestStreakDays
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // -- Usage ring --
        UsageRing(
            totalMinutes = uiState.totalScreenTimeMinutes,
            limitMinutes = uiState.dailyLimitMinutes
        )

        Spacer(modifier = Modifier.height(20.dp))

        // -- Inline stats --
        // Three plain figures replace two stat cards: the same information in
        // less height, and the eye reaches the session controls sooner.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SoberInlineMetric(
                value = "${uiState.appsUsedToday}",
                label = "APPS USED",
                modifier = Modifier.weight(1f)
            )
            MetricDivider()
            SoberInlineMetric(
                value = "${uiState.sessionsToday}",
                label = "FOCUS SESSIONS",
                modifier = Modifier.weight(1f)
            )
            MetricDivider()
            val remaining = (uiState.dailyLimitMinutes - uiState.totalScreenTimeMinutes)
                .coerceAtLeast(0)
            val overBy = (uiState.totalScreenTimeMinutes - uiState.dailyLimitMinutes)
                .coerceAtLeast(0)
            val overLimit = uiState.totalScreenTimeMinutes >= uiState.dailyLimitMinutes &&
                uiState.dailyLimitMinutes > 0
            SoberInlineMetric(
                value = if (uiState.dailyLimitMinutes > 0) {
                    limitLabel(if (overLimit) overBy else remaining)
                } else "—",
                label = if (overLimit) "OVER CAP" else "LEFT TODAY",
                modifier = Modifier.weight(1f),
                isEmphasised = overLimit
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // -- Top apps --
        SoberSectionHeader(title = "Top apps today", index = "01")
        Spacer(modifier = Modifier.height(4.dp))

        if (uiState.topApps.isEmpty()) {
            Text(
                text = if (uiState.isLoading) "Reading today's usage…" else
                    "Nothing used yet today. Your first app will show up here.",
                color = SoberTheme.colors.textMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            uiState.topApps.take(3).forEachIndexed { index, app ->
                if (index > 0) SoberDivider()
                TopAppRow(
                    appName = app.appName,
                    usageMinutes = app.usageMinutes,
                    limitMinutes = app.limitMinutes,
                    progress = app.progress,
                    isAtLimit = app.isAtLimit,
                    icon = app.icon
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SoberSectionHeader(title = "Focus", index = "02")
        Spacer(modifier = Modifier.height(16.dp))

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
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(SoberTheme.colors.primary)
                    )
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
                        uiState.lockNotice?.let { notice ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = notice,
                                color = SoberTheme.colors.danger,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
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

        Spacer(modifier = Modifier.height(28.dp))

        // -- Pause everything --
        SoberSectionHeader(title = "Pause", index = "03")
        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isPaused) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SoberTheme.colors.surface,
                border = BorderStroke(1.dp, SoberTheme.colors.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription =
                            "Protection paused. ${uiState.pauseRemainingText}."
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SoberIconBadge(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = null,
                        size = 44.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Protection paused",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SoberTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uiState.pauseRemainingText.orEmpty(),
                            fontSize = 12.sp,
                            color = SoberTheme.colors.textMuted,
                            style = TabularNumberStyle
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    SoberOutlinedButton(
                        text = "Resume",
                        onClick = { viewModel.resumeProtection() },
                        modifier = Modifier.width(128.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15 to "15 min", 60 to "1 hour", 180 to "3 hours").forEach { (minutes, label) ->
                    SoberOutlinedButton(
                        text = label,
                        onClick = { viewModel.pauseProtection(minutes) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lifts every block and limit for a while, then protection returns on its own.",
                color = SoberTheme.colors.textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        uiState.pauseNotice?.let { notice ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = notice,
                color = SoberTheme.colors.danger,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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

@Composable
private fun StreakBadge(streakDays: Int, longestStreakDays: Int) {
    val colors = SoberTheme.colors

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = buildString {
                append(if (streakDays == 1) "1 day streak" else "$streakDays day streak")
                if (longestStreakDays > streakDays) {
                    append(". Best: $longestStreakDays days")
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$streakDays",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    style = TabularNumberStyle
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "DAY STREAK",
                fontFamily = JetBrainsMono,
                fontSize = 8.sp,
                letterSpacing = 0.8.sp,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun UsageRing(totalMinutes: Int, limitMinutes: Int) {
    val colors = SoberTheme.colors
    val targetPercent = if (limitMinutes > 0) {
        (totalMinutes.toFloat() / limitMinutes).coerceAtMost(1f)
    } else {
        0f
    }
    val usagePercent by animateFloatAsState(
        targetValue = targetPercent,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "usage-ring"
    )
    val atLimit = limitMinutes > 0 && totalMinutes >= limitMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    Box(
        modifier = Modifier
            .size(196.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("${spokenDuration(totalMinutes)} used")
                    if (limitMinutes > 0) {
                        append(" of ${spokenDuration(limitMinutes)} daily cap")
                    }
                    if (atLimit) append(", cap reached")
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val trackColor = colors.divider
        val fillColor = if (atLimit) colors.danger else colors.primary
        Canvas(modifier = Modifier.size(196.dp)) {
            val strokeWidth = 12.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val topLeft = Offset(
                (size.width - 2 * radius) / 2,
                (size.height - 2 * radius) / 2
            )

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${hours}h ${minutes}m",
                fontSize = 38.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-1).sp,
                color = if (atLimit) colors.danger else colors.textPrimary,
                style = TabularNumberStyle
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (limitMinutes > 0) {
                    "of ${limitLabel(limitMinutes)} cap"
                } else {
                    "today"
                },
                fontSize = 12.sp,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(SoberTheme.colors.divider)
    )
}

@Composable
private fun TopAppRow(
    appName: String,
    usageMinutes: Int,
    limitMinutes: Int?,
    progress: Float?,
    isAtLimit: Boolean,
    icon: Drawable?
) {
    val colors = SoberTheme.colors
    val subtitle = when {
        isAtLimit -> "Limit reached"
        limitMinutes != null && progress != null ->
            "${(progress * 100).roundToInt()}% of ${limitLabel(limitMinutes)} limit"
        else -> "No limit set"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("$appName, ${spokenDuration(usageMinutes)}")
                    if (isAtLimit) append(", limit reached")
                }
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.primaryMuted),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(
                        painter = rememberDrawablePainter(drawable = icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(27.dp)
                            .clip(RoundedCornerShape(7.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (isAtLimit) colors.danger else colors.textMuted
                )
            }
            if (isAtLimit) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = colors.danger,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = "${usageMinutes}m",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isAtLimit) colors.danger else colors.textSecondary,
                style = TabularNumberStyle
            )
        }

        // No bar for an app without a limit: a progress bar needs a
        // denominator, and using the daily cap would claim the app is close
        // to a limit it does not have.
        if (progress != null) {
            Spacer(modifier = Modifier.height(9.dp))
            SoberProgressBar(progress = progress, isDanger = isAtLimit)
        }
    }
}
