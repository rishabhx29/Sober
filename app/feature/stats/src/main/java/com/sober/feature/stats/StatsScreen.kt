package com.sober.feature.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.sober.core.domain.usecase.UsageHeatmap
import com.sober.core.domain.usecase.weekOverWeekLabel
import com.sober.core.ui.components.SoberProgressBar
import com.sober.core.ui.components.SoberSectionLabel
import com.sober.core.ui.components.SoberSegmentedControl
import com.sober.core.ui.theme.SoberTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoberTheme.colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Statistics",
            fontSize = 22.sp,
            color = SoberTheme.colors.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        SoberSegmentedControl(
            options = Timeframe.entries.toList(),
            selected = uiState.selectedTimeframe,
            onSelect = { viewModel.selectTimeframe(it) },
            label = {
                when (it) {
                    Timeframe.TODAY -> "Today"
                    Timeframe.WEEK -> "7 Days"
                    Timeframe.MONTH -> "30 Days"
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.overrideSummary.hasOverrides) {
            OverrideFlagCard(
                count = uiState.overrideSummary.count,
                minutesGranted = uiState.overrideSummary.totalMinutesGranted
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        ScreenTimeCard(
            totalMinutes = uiState.totalScreenTimeMinutes,
            averageMinutes = uiState.averageDailyMinutes,
            timeframe = uiState.selectedTimeframe,
            trendLabel = weekOverWeekLabel(uiState.highlights.weekOverWeekPercent),
            trendPercent = uiState.highlights.weekOverWeekPercent
        )

        if (uiState.dailyTotals.size > 1) {
            Spacer(modifier = Modifier.height(20.dp))
            SoberSectionLabel(text = "SCREEN TIME")
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
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

        Spacer(modifier = Modifier.height(20.dp))

        SoberSectionLabel(text = "TOP APPS")
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SoberTheme.colors.surface,
            border = BorderStroke(1.dp, SoberTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (uiState.topApps.isEmpty() && !uiState.isLoading) {
                    Text(
                        text = "No usage data yet. Use your phone and check back!",
                        fontSize = 14.sp,
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

                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                Image(
                                    painter = rememberDrawablePainter(drawable = app.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = app.appName,
                                fontSize = 15.sp,
                                color = SoberTheme.colors.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Text(
                                text = if (app.limitMinutes != null) {
                                    "${formatMinutes(app.usageMinutes)} / ${formatMinutes(app.limitMinutes)}"
                                } else {
                                    formatMinutes(app.usageMinutes)
                                },
                                fontSize = 14.sp,
                                color = if (atLimit) {
                                    SoberTheme.colors.danger
                                } else {
                                    SoberTheme.colors.textSecondary
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        SoberProgressBar(
                            progress = app.usageMinutes.toFloat() / peak,
                            isDanger = atLimit
                        )
                    }

                    if (index < uiState.topApps.lastIndex) {
                        HorizontalDivider(color = SoberTheme.colors.divider)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SoberSectionLabel(text = "PATTERNS")
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SoberTheme.colors.surface,
            border = BorderStroke(1.dp, SoberTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                HighlightRow(
                    label = "Most productive day",
                    value = uiState.highlights.mostProductiveWeekday ?: "—",
                    hint = "Lowest screen time"
                )
                Spacer(modifier = Modifier.height(12.dp))
                HighlightRow(
                    label = "Focus sessions",
                    value = "${uiState.highlights.focusCompleted} done · ${uiState.highlights.focusCancelled} cancelled",
                    hint = "In this period"
                )
                Spacer(modifier = Modifier.height(12.dp))
                HighlightRow(
                    label = "Days under your limit",
                    value = if (uiState.highlights.hasAdherence) {
                        "${uiState.highlights.daysUnderLimit} of ${uiState.highlights.daysMeasured}"
                    } else {
                        "—"
                    },
                    hint = "Set an overall cap to measure this"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SoberSectionLabel(text = "WHEN YOU USE YOUR PHONE")
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
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

        Spacer(modifier = Modifier.height(20.dp))

        SoberSectionLabel(text = "ACHIEVEMENTS")
        Spacer(modifier = Modifier.height(8.dp))

        uiState.achievements.chunked(2).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { achievement ->
                    AchievementCard(
                        emoji = achievement.id.emoji,
                        title = achievement.id.title,
                        description = achievement.id.description,
                        isUnlocked = achievement.isUnlocked,
                        modifier = Modifier.weight(1f)
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
 * A flag rather than a statistic: it is only rendered when an override happened
 * in the selected period, so its presence always means something.
 */
@Composable
private fun OverrideFlagCard(count: Int, minutesGranted: Int) {
    val colors = SoberTheme.colors

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.danger.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚑", fontSize = 16.sp, color = colors.danger)
                Spacer(modifier = Modifier.width(8.dp))
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
private fun ScreenTimeCard(
    totalMinutes: Int,
    averageMinutes: Int,
    timeframe: Timeframe,
    trendLabel: String,
    trendPercent: Int?
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SoberTheme.colors.surface,
        border = BorderStroke(1.dp, SoberTheme.colors.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TOTAL SCREEN TIME",
                fontSize = 11.sp,
                color = SoberTheme.colors.textMuted,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatMinutes(totalMinutes),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = SoberTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (timeframe) {
                    Timeframe.TODAY -> "today"
                    Timeframe.WEEK -> "${formatMinutes(averageMinutes)} daily average"
                    Timeframe.MONTH -> "${formatMinutes(averageMinutes)} daily average"
                },
                fontSize = 14.sp,
                color = SoberTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                // Down is good: the trend line is the one place red is used for
                // an increase without also being a limit breach.
                text = trendLabel,
                fontSize = 12.sp,
                color = when {
                    trendPercent == null -> SoberTheme.colors.textMuted
                    trendPercent > 0 -> SoberTheme.colors.danger
                    else -> SoberTheme.colors.textSecondary
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
                    .clickable { onSelect(index) },
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
        modifier = Modifier.fillMaxWidth(),
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
            fontWeight = FontWeight.Medium
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
    emoji: String,
    title: String,
    description: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        // Locked cards sink into the page rather than taking a fixed cream
        // tint, which would be a pale block in dark mode.
        color = if (isUnlocked) {
            SoberTheme.colors.surface
        } else {
            SoberTheme.colors.background
        },
        border = if (isUnlocked) BorderStroke(1.dp, SoberTheme.colors.divider) else null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isUnlocked) SoberTheme.colors.primary else SoberTheme.colors.textMuted,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = SoberTheme.colors.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
