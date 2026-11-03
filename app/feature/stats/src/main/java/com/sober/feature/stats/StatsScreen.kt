package com.sober.feature.stats

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.sober.core.domain.model.AchievementId
import com.sober.core.domain.model.ChallengeId
import com.sober.core.domain.model.ChallengeProgress
import com.sober.core.domain.model.ChallengeState
import com.sober.core.domain.usecase.DailyUsageTotal
import com.sober.core.domain.usecase.ExtremeDay
import com.sober.core.domain.usecase.UsageHeatmap
import com.sober.core.domain.usecase.bestAndWorstDays
import com.sober.core.domain.usecase.weekOverWeekLabel
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.components.SoberDivider
import com.sober.core.ui.components.SoberFilterChips
import com.sober.core.ui.components.SoberIconBadge
import com.sober.core.ui.components.SoberProgressBar
import com.sober.core.ui.components.SoberScreenHeader
import com.sober.core.ui.components.SoberSectionHeader
import com.sober.core.ui.components.SoberTextLink
import com.sober.core.ui.format.spokenDuration
import com.sober.core.ui.theme.JetBrainsMono
import com.sober.core.ui.theme.SoberTheme
import com.sober.core.ui.theme.TabularNumberStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // A refused or finished challenge happens away from the card, so the
    // outcome needs to be spoken somewhere the next scroll cannot hide.
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    // Stats are computed once per ViewModel, which outlives the tab, so the
    // numbers would still describe the moment the app opened. Re-reading on
    // resume keeps "Today" honest after time spent in other apps.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Reset when the range changes: an index into the previous range would
    // point at a different day.
    var selectedDay by remember(uiState.selectedTimeframe, uiState.dailyTotals.size) {
        mutableStateOf<Int?>(null)
    }

    // Section numbers follow the sections actually rendered: the chart is
    // absent on "Today" and the weekly report only on 7 days, so fixed
    // numbers would skip or repeat.
    val hasChart = uiState.dailyTotals.size > 1
    val hasWeeklyReport = uiState.selectedTimeframe == Timeframe.WEEK
    val sectionKeys = buildList {
        if (hasChart) add("day")
        if (hasWeeklyReport) add("week")
        add("apps")
        add("patterns")
        add("heatmap")
        add("challenges")
        add("achievements")
    }
    fun sectionIndex(key: String): String =
        (sectionKeys.indexOf(key) + 1).toString().padStart(2, '0')

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoberTheme.colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SoberScreenHeader(
            title = "Statistics",
            eyebrow = "The record",
            subtitle = "No scores, no judgement. Just what actually happened."
        )

        Spacer(modifier = Modifier.height(20.dp))

        SoberFilterChips(
            options = Timeframe.entries.toList(),
            selected = uiState.selectedTimeframe,
            onSelect = { viewModel.selectTimeframe(it) },
            label = {
                when (it) {
                    Timeframe.TODAY -> "Today"
                    Timeframe.WEEK -> "7 days"
                    Timeframe.MONTH -> "30 days"
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.overrideSummary.hasOverrides) {
            OverrideFlagCard(
                count = uiState.overrideSummary.count,
                minutesGranted = uiState.overrideSummary.totalMinutesGranted
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        ScreenTimeHero(
            totalMinutes = uiState.totalScreenTimeMinutes,
            averageMinutes = uiState.averageDailyMinutes,
            timeframe = uiState.selectedTimeframe,
            trendLabel = weekOverWeekLabel(uiState.highlights.weekOverWeekPercent),
            trendPercent = uiState.highlights.weekOverWeekPercent
        )

        if (hasChart) {
            Spacer(modifier = Modifier.height(24.dp))
            SoberSectionHeader(title = "By day", index = sectionIndex("day"))
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SoberTheme.colors.surface,
                border = BorderStroke(1.dp, SoberTheme.colors.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DailyBarChart(
                        values = uiState.dailyTotals,
                        limitMinutes = uiState.dailyLimitMinutes,
                        selectedIndex = selectedDay,
                        onSelect = { index ->
                            selectedDay = if (selectedDay == index) null else index
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val pickedIndex = selectedDay
                    val pickedDate = pickedIndex?.let { uiState.dailyDates.getOrNull(it) }
                    Text(
                        text = if (pickedIndex == null || pickedDate == null) {
                            "Tap a bar for that day. Red bars are over your ${uiState.dailyLimitMinutes} min limit."
                        } else {
                            "${formatDate(pickedDate)} · ${
                                formatMinutes(uiState.dailyTotals.getOrElse(pickedIndex) { 0 })
                            }"
                        },
                        fontSize = 11.sp,
                        color = SoberTheme.colors.textMuted
                    )
                }
            }
        }

        if (hasWeeklyReport) {
            Spacer(modifier = Modifier.height(24.dp))
            SoberSectionHeader(title = "Weekly report", index = sectionIndex("week"))
            Spacer(modifier = Modifier.height(6.dp))
            WeeklyReportCard(uiState = uiState, viewModel = viewModel)
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Top apps", index = sectionIndex("apps"))
        Spacer(modifier = Modifier.height(2.dp))

        if (uiState.topApps.isEmpty() && !uiState.isLoading) {
            Text(
                text = "No usage data yet. Use your phone and check back.",
                fontSize = 13.sp,
                color = SoberTheme.colors.textMuted,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        val peak = uiState.topApps.maxOfOrNull { it.usageMinutes } ?: 1

        uiState.topApps.forEachIndexed { index, app ->
            // Red means "this app's own limit", not the overall cap: an
            // app without a limit can never be at it.
            val atLimit = app.limitMinutes != null &&
                app.usageMinutes >= app.limitMinutes

            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (app.icon != null) {
                        Image(
                            painter = rememberDrawablePainter(drawable = app.icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            fontSize = 15.sp,
                            color = SoberTheme.colors.textPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Text(
                            text = if (app.limitMinutes != null) {
                                "Limit ${formatMinutes(app.limitMinutes)}"
                            } else {
                                "No limit set"
                            },
                            fontSize = 11.sp,
                            color = SoberTheme.colors.textMuted
                        )
                    }
                    Text(
                        text = formatMinutes(app.usageMinutes),
                        fontSize = 14.sp,
                        color = if (atLimit) {
                            SoberTheme.colors.danger
                        } else {
                            SoberTheme.colors.textSecondary
                        },
                        fontWeight = FontWeight.Medium,
                        fontFamily = JetBrainsMono,
                        style = TabularNumberStyle
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                SoberProgressBar(
                    progress = app.usageMinutes.toFloat() / peak,
                    isDanger = atLimit,
                    modifier = Modifier.padding(start = 46.dp)
                )
            }

            if (index < uiState.topApps.lastIndex) {
                SoberDivider()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Patterns", index = sectionIndex("patterns"))
        Spacer(modifier = Modifier.height(2.dp))

        HighlightRow(
            label = "Most productive day",
            value = uiState.highlights.mostProductiveWeekday ?: "—",
            hint = "Lowest screen time"
        )
        SoberDivider()
        HighlightRow(
            label = "Focus sessions",
            value = "${uiState.highlights.focusCompleted} done · ${uiState.highlights.focusCancelled} cancelled",
            hint = "In this period"
        )
        SoberDivider()
        HighlightRow(
            label = "Days under your limit",
            value = if (uiState.highlights.hasAdherence) {
                "${uiState.highlights.daysUnderLimit} of ${uiState.highlights.daysMeasured}"
            } else {
                "—"
            },
            // Only an invitation while there is nothing to measure;
            // once a cap exists the row is showing a real count, and
            // telling the user to set one contradicts the number.
            hint = if (uiState.highlights.hasAdherence) {
                "Days measured"
            } else {
                "Set an overall cap to measure this"
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "When you use your phone", index = sectionIndex("heatmap"))
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SoberTheme.colors.surface,
            border = BorderStroke(1.dp, SoberTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                UsageHeatmapGrid(heatmap = uiState.heatmap)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = uiState.heatmap.busiestWindowLabel?.let {
                        "You use your phone most at $it."
                    } ?: "Not enough hourly data yet.",
                    fontSize = 12.sp,
                    color = SoberTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(
            title = "Challenges",
            index = sectionIndex("challenges"),
            action = {
                if (uiState.challenges.completedCount > 0) {
                    Text(
                        text = "${uiState.challenges.completedCount} finished",
                        fontSize = 11.sp,
                        color = SoberTheme.colors.textMuted
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(6.dp))

        ChallengesSection(
            state = uiState.challenges,
            onStart = viewModel::startChallenge,
            onRestart = viewModel::restartChallenge,
            onFinish = viewModel::finishChallenge,
            onAbandon = viewModel::abandonChallenge
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoberSectionHeader(title = "Achievements", index = sectionIndex("achievements"))
        Spacer(modifier = Modifier.height(6.dp))

        uiState.achievements.chunked(2).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(modifier = Modifier.height(12.dp))
            Row(
                // Cards in one row share the taller description's height;
                // otherwise a two-line hint leaves its neighbour visibly short.
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { achievement ->
                    AchievementCard(
                        id = achievement.id,
                        title = achievement.id.title,
                        description = achievement.id.description,
                        isUnlocked = achievement.isUnlocked,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                // Keeps an odd last card the same width as the ones above it.
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * The 7-day view condensed into something worth sending to a friend.
 *
 * The bitmap is drawn off-screen by the ViewModel, so what is shared does not
 * depend on what part of this scrollable screen happens to be visible.
 */
@Composable
private fun WeeklyReportCard(
    uiState: StatsUiState,
    viewModel: StatsViewModel
) {
    val colors = SoberTheme.colors
    val context = LocalContext.current
    val (best, worst) = remember(uiState.dailyDates, uiState.dailyTotals) {
        bestAndWorstDays(
            uiState.dailyDates.zip(uiState.dailyTotals) { date, minutes ->
                DailyUsageTotal(date, minutes)
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "THIS WEEK, ON ONE CARD",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.4.sp,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                WeeklyMetric(
                    label = "Quietest day",
                    value = best.shortDescription(),
                    modifier = Modifier.weight(1f)
                )
                WeeklyMetric(
                    label = "Heaviest day",
                    value = worst.shortDescription(),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                WeeklyMetric(
                    label = "Phone unlocks",
                    value = uiState.screenUnlocks?.toString() ?: "Not reported",
                    modifier = Modifier.weight(1f)
                )
                WeeklyMetric(
                    label = "Days under limit",
                    value = if (uiState.highlights.hasAdherence) {
                        "${uiState.highlights.daysUnderLimit} of ${uiState.highlights.daysMeasured}"
                    } else {
                        "No cap set"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            SoberButton(
                text = "Share this week",
                icon = Icons.Rounded.Share,
                onClick = {
                    viewModel.shareWeeklyReport { uri, error ->
                        if (uri != null) {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(send, "Share your week"))
                        } else {
                            Toast.makeText(
                                context,
                                error ?: "Couldn't create the report.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Drawn on this device. It only leaves when you share it.",
                fontSize = 11.sp,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun WeeklyMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = SoberTheme.colors
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(Locale.US),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp,
            color = colors.textMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )
    }
}

private fun ExtremeDay?.shortDescription(): String {
    if (this == null) return "No usage"
    val day = runCatching { LocalDate.parse(date) }.getOrNull()
    val label = day?.format(DateTimeFormatter.ofPattern("EEE", Locale.US)) ?: date
    return "$label · ${formatMinutes(minutes)}"
}

/**
 * A flag rather than a statistic: it is only rendered when an override happened
 * in the selected period, so its presence always means something.
 */
@Composable
private fun OverrideFlagCard(count: Int, minutesGranted: Int) {
    val colors = SoberTheme.colors

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.dangerMuted,
        border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.35f)),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = buildString {
                append("Emergency override used. ")
                append(if (count == 1) "1 unlock" else "$count unlocks")
                append(", ")
                append(spokenDuration(minutesGranted))
                append(" granted.")
            }
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoberIconBadge(
                    imageVector = Icons.Rounded.Flag,
                    contentDescription = null,
                    size = 28.dp,
                    iconSize = 15.dp,
                    containerColor = colors.surface,
                    contentColor = colors.danger,
                    shape = RoundedCornerShape(9.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Emergency override used",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.danger
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildString {
                    append(count)
                    append(if (count == 1) " unlock" else " unlocks")
                    append(" · ")
                    append(minutesGranted)
                    append(" min granted")
                },
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun ScreenTimeHero(
    totalMinutes: Int,
    averageMinutes: Int,
    timeframe: Timeframe,
    trendLabel: String,
    trendPercent: Int?
) {
    val colors = SoberTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "TOTAL SCREEN TIME",
            fontSize = 10.sp,
            color = colors.textMuted,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.4.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatMinutes(totalMinutes),
            fontSize = 52.sp,
            lineHeight = 56.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.5).sp,
            color = colors.textPrimary,
            fontFamily = JetBrainsMono,
            style = TabularNumberStyle
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (timeframe) {
                    Timeframe.TODAY -> "today"
                    Timeframe.WEEK, Timeframe.MONTH ->
                        "${formatMinutes(averageMinutes)} daily average"
                },
                fontSize = 13.sp,
                color = colors.textSecondary
            )
            Text(
                text = "  ·  ",
                fontSize = 13.sp,
                color = colors.textMuted
            )
            Text(
                // Down is good: the trend line is the one place red is used for
                // an increase without also being a limit breach.
                text = trendLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    trendPercent == null -> colors.textMuted
                    trendPercent > 0 -> colors.danger
                    else -> colors.textSecondary
                }
            )
        }
    }
}

/**
 * One bar per day. Bars are scaled against the larger of the peak day and the
 * daily limit, so a quiet week still shows how much headroom is left and a red
 * bar always means the day crossed the limit. Tapping a bar selects it; the
 * caller renders the selected day's exact figures underneath.
 */
@Composable
private fun DailyBarChart(
    values: List<Int>,
    limitMinutes: Int,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    if (values.isEmpty()) return
    val colors = SoberTheme.colors
    val peak = maxOf(values.maxOrNull() ?: 0, limitMinutes, 1)
    val gap = if (values.size > 10) 2.dp else 6.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        values.forEachIndexed { index, minutes ->
            val overLimit = limitMinutes > 0 && minutes > limitMinutes
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        role = androidx.compose.ui.semantics.Role.Button,
                        onClickLabel = "Show this day",
                        onClick = { onSelect(index) }
                    )
                    .semantics {
                        selected = isSelected
                        contentDescription = spokenDuration(minutes)
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((minutes.toFloat() / peak).coerceIn(0.02f, 1f))
                        .heightIn(min = 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                overLimit -> colors.danger
                                isSelected -> colors.primary
                                else -> colors.primary.copy(alpha = 0.55f)
                            }
                        )
                )
            }
        }
    }
}

/**
 * 7 rows (Mon..Sun) by 24 columns (hours). Intensity is the accent alpha, so
 * the low end stays close to the card and the high end reads at full contrast
 * in both light and dark themes.
 */
@Composable
private fun UsageHeatmapGrid(heatmap: UsageHeatmap) {
    val colors = SoberTheme.colors
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val peak = heatmap.cells.maxOfOrNull { row -> row.maxOfOrNull { it } ?: 0 } ?: 0

    Column(modifier = Modifier.fillMaxWidth()) {
        heatmap.cells.forEachIndexed { dayIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = days[dayIndex],
                    fontSize = 9.sp,
                    color = colors.textMuted,
                    modifier = Modifier.width(26.dp)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    row.forEach { minutes ->
                        val fraction = if (peak <= 0) 0f else minutes.toFloat() / peak
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(9.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    if (minutes <= 0) {
                                        colors.divider.copy(alpha = 0.5f)
                                    } else {
                                        colors.primary.copy(alpha = 0.15f + 0.85f * fraction)
                                    }
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(26.dp))
            Text("12am", fontSize = 9.sp, color = colors.textMuted)
            Spacer(modifier = Modifier.weight(1f))
            Text("12pm", fontSize = 9.sp, color = colors.textMuted)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HighlightRow(label: String, value: String, hint: String) {
    Row(
        // Matches SoberListRow, so a pattern row and a settings row breathe the
        // same amount around their hairline.
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = SoberTheme.colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = hint,
                fontSize = 11.sp,
                color = SoberTheme.colors.textMuted
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            color = SoberTheme.colors.textSecondary,
            fontWeight = FontWeight.Medium,
            style = TabularNumberStyle
        )
    }
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/** "Tue, Oct 28" for the chart tooltip. */
private fun formatDate(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US))
}.getOrDefault(isoDate)

@Composable
private fun AchievementCard(
    id: AchievementId,
    title: String,
    description: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = SoberTheme.colors

    Surface(
        shape = RoundedCornerShape(18.dp),
        // Locked cards sink into the page rather than taking a fixed cream
        // tint, which would be a pale block in dark mode.
        color = if (isUnlocked) colors.surface else colors.background,
        border = BorderStroke(
            1.dp,
            if (isUnlocked) colors.divider else colors.divider.copy(alpha = 0.7f)
        ),
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = if (isUnlocked) {
                "$title, unlocked. $description"
            } else {
                "$title, locked. $description"
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                SoberIconBadge(
                    imageVector = id.badgeIcon(),
                    contentDescription = null,
                    size = 54.dp,
                    iconSize = 26.dp,
                    containerColor = if (isUnlocked) colors.primaryMuted else colors.surface,
                    contentColor = if (isUnlocked) colors.primary else colors.textMuted,
                    shape = RoundedCornerShape(17.dp),
                    border = BorderStroke(1.dp, colors.divider)
                )
                if (!isUnlocked) {
                    SoberIconBadge(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        size = 20.dp,
                        iconSize = 10.dp,
                        containerColor = colors.surface,
                        contentColor = colors.textMuted,
                        border = BorderStroke(1.dp, colors.divider),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isUnlocked) colors.primary else colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = colors.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * One icon vocabulary for badges. The glyph is chosen here rather than stored
 * on the domain enum so the UI can change it without touching persisted data.
 */
private fun AchievementId.badgeIcon(): ImageVector = when (this) {
    AchievementId.SEVEN_DAY_STREAK -> Icons.Rounded.LocalFireDepartment
    AchievementId.FOCUS_MASTER -> Icons.Rounded.SelfImprovement
    AchievementId.SOCIAL_DETOX -> Icons.Rounded.PhoneDisabled
    AchievementId.NIGHT_OWL -> Icons.Rounded.Bedtime
    AchievementId.SPEED_DEMON -> Icons.Rounded.Bolt
    AchievementId.SOBER_PRO -> Icons.Rounded.WorkspacePremium
    AchievementId.DEEP_WORK -> Icons.Rounded.Timer
    AchievementId.CHALLENGE_CHAMPION -> Icons.Rounded.EmojiEvents
}

@Composable
private fun ChallengesSection(
    state: ChallengeState,
    onStart: (ChallengeId) -> Unit,
    onRestart: (ChallengeId) -> Unit,
    onFinish: () -> Unit,
    onAbandon: () -> Unit
) {
    val active = state.active
    if (active != null) {
        ActiveChallengeCard(
            progress = active,
            onFinish = onFinish,
            onRestart = { onRestart(active.id) },
            onAbandon = onAbandon
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "One at a time. The next challenge opens when this one is finished or left.",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = SoberTheme.colors.textMuted
        )
        return
    }

    ChallengeId.entries.forEachIndexed { index, id ->
        if (index > 0) Spacer(modifier = Modifier.height(10.dp))
        ChallengeOfferCard(id = id, onStart = { onStart(id) })
    }
}

@Composable
private fun ChallengeOfferCard(
    id: ChallengeId,
    onStart: () -> Unit
) {
    val colors = SoberTheme.colors

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = id.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = id.description,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = offerMeta(id),
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = colors.textMuted
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            SoberTextLink(
                text = "Start",
                onClick = onStart,
                contentDescription = "Start ${id.title}"
            )
        }
    }
}

@Composable
private fun ActiveChallengeCard(
    progress: ChallengeProgress,
    onFinish: () -> Unit,
    onRestart: () -> Unit,
    onAbandon: () -> Unit
) {
    val colors = SoberTheme.colors
    var confirmLeave by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val firstTarget = progress.targetDates.firstOrNull()

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Leave this challenge?",
                    color = colors.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "The days so far will not be recorded. You can start it again later.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLeave = false
                        onAbandon()
                    }
                ) {
                    Text(text = "Leave", color = colors.danger, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) {
                    Text(text = "Keep going", color = colors.textSecondary)
                }
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.id.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = progress.id.description,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = challengeStatusLabel(progress, today, firstTarget),
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = if (progress.isFailed) colors.textMuted else colors.accent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            SoberProgressBar(
                progress = progress.fraction,
                contentDescription = "${progress.daysDone} of ${progress.daysTarget} days done",
                height = 6.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = challengeBodyLabel(progress, today),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                when {
                    progress.isComplete ->
                        SoberTextLink(text = "Collect the badge", onClick = onFinish)
                    progress.isFailed -> {
                        SoberTextLink(text = "Start again", onClick = onRestart)
                        SoberTextLink(text = "Leave it", onClick = { confirmLeave = true })
                    }
                    else ->
                        SoberTextLink(text = "Leave challenge", onClick = { confirmLeave = true })
                }
            }
        }
    }
}
