package com.sober.feature.stats

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.model.Achievement
import com.sober.core.domain.model.AchievementId
import com.sober.core.domain.model.ChallengeId
import com.sober.core.domain.model.ChallengeState
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.FocusSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import com.sober.core.domain.usecase.ChallengeStart
import com.sober.core.domain.usecase.DailyUsageTotal
import com.sober.core.domain.usecase.ExtremeDay
import com.sober.core.domain.usecase.GetAchievementsUseCase
import com.sober.core.domain.usecase.GetChallengeStateUseCase
import com.sober.core.domain.usecase.GetOverrideSummaryUseCase
import com.sober.core.domain.usecase.GetUsageHeatmapUseCase
import com.sober.core.domain.usecase.GetUsageHistoryUseCase
import com.sober.core.domain.usecase.ManageChallengeUseCase
import com.sober.core.domain.usecase.OverrideSummary
import com.sober.core.domain.usecase.StatsHighlights
import com.sober.core.domain.usecase.UsageHeatmap
import com.sober.core.domain.usecase.bestAndWorstDays
import com.sober.core.domain.usecase.limitAdherence
import com.sober.core.domain.usecase.mostProductiveWeekday
import com.sober.core.domain.usecase.weekOverWeekChange
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AppUsageDisplay(
    val appName: String,
    val packageName: String,
    val usageMinutes: Int,
    /** This app's own limit, or null when it has none. */
    val limitMinutes: Int? = null,
    val icon: Drawable? = null
)

data class StatsUiState(
    val totalScreenTimeMinutes: Int = 0,
    val averageDailyMinutes: Int = 0,
    val topApps: List<AppUsageDisplay> = emptyList(),
    /** One entry per day in the selected range, oldest first. */
    val dailyTotals: List<Int> = emptyList(),
    /** ISO dates matching [dailyTotals], for the chart tooltip. */
    val dailyDates: List<String> = emptyList(),
    val dailyLimitMinutes: Int = SoberPreferences.DEFAULT_DAILY_SCREEN_TIME_LIMIT,
    val selectedTimeframe: Timeframe = Timeframe.TODAY,
    val heatmap: UsageHeatmap = UsageHeatmap.EMPTY,
    val highlights: StatsHighlights = StatsHighlights(),
    /** Emergency unlocks inside the selected range; shown as a flag when > 0. */
    val overrideSummary: OverrideSummary = OverrideSummary(),
    /** Keyguard dismissals over 7 days, or null when the platform cannot report it. */
    val screenUnlocks: Int? = null,
    val challenges: ChallengeState = ChallengeState(),
    val achievements: List<Achievement> = AchievementId.entries.map {
        Achievement(it, isUnlocked = false)
    },
    val isLoading: Boolean = true
)

/**
 * Everything the shareable card needs, snapshotted so the bitmap is rendered
 * from the same numbers the screen showed rather than from a second query.
 */
data class WeeklyReportData(
    val rangeLabel: String,
    val totalMinutes: Int,
    val averageMinutes: Int,
    val comparisonPercent: Int?,
    val bestDay: ExtremeDay?,
    val worstDay: ExtremeDay?,
    val daysUnderLimit: Int,
    val daysMeasured: Int,
    val focusCompleted: Int,
    val unlocks: Int?
)

enum class Timeframe(val days: Int) {
    TODAY(1),
    WEEK(7),
    MONTH(30)
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsRepository: UsageStatsRepository,
    private val getUsageHistoryUseCase: GetUsageHistoryUseCase,
    private val getUsageHeatmapUseCase: GetUsageHeatmapUseCase,
    private val getAchievementsUseCase: GetAchievementsUseCase,
    private val getOverrideSummaryUseCase: GetOverrideSummaryUseCase,
    private val appLimitRepository: AppLimitRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val soberPreferences: SoberPreferences,
    private val getChallengeStateUseCase: GetChallengeStateUseCase,
    private val manageChallengeUseCase: ManageChallengeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

    /** One-shot feedback for a refused or completed challenge tap. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun consumeMessage() {
        _message.value = null
    }

    /**
     * Sober itself and the launcher: both show up in usage stats, neither is an
     * app the user opened, and naming "Launcher" in a top-apps list is wrong.
     */
    private val nonAppPackages: Set<String> by lazy {
        val home = runCatching {
            context.packageManager.resolveActivity(
                android.content.Intent(android.content.Intent.ACTION_MAIN)
                    .addCategory(android.content.Intent.CATEGORY_HOME),
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )?.activityInfo?.packageName
        }.getOrNull()
        buildSet {
            add(context.packageName)
            home?.let(::add)
        }
    }

    init {
        loadStats(Timeframe.TODAY)
    }

    fun selectTimeframe(timeframe: Timeframe) {
        if (timeframe == _uiState.value.selectedTimeframe) return
        _uiState.value = _uiState.value.copy(selectedTimeframe = timeframe, isLoading = true)
        loadStats(timeframe)
    }

    fun refresh() = loadStats(_uiState.value.selectedTimeframe)

    fun startChallenge(id: ChallengeId) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { manageChallengeUseCase.start(id) }
                .getOrDefault(ChallengeStart.NothingToMeasure)
            when (result) {
                ChallengeStart.Started ->
                    loadStats(_uiState.value.selectedTimeframe)
                ChallengeStart.AlreadyRunning ->
                    _message.value = "Finish or leave your current challenge first."
                ChallengeStart.NothingToMeasure ->
                    _message.value = if (id.isSocial) {
                        "No social apps found on this device, so there is nothing to measure."
                    } else {
                        "Set a daily or per-app limit first, so a clean day means something."
                    }
            }
        }
    }

    /** Starts the same challenge again after a failed run. */
    fun restartChallenge(id: ChallengeId) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { manageChallengeUseCase.abandon() }
            startChallenge(id)
        }
    }

    fun finishChallenge() {
        viewModelScope.launch(Dispatchers.IO) {
            val finished = runCatching { manageChallengeUseCase.finish() }.getOrDefault(false)
            _message.value = if (finished) {
                "Challenge complete."
            } else {
                "That challenge is not finished yet."
            }
            loadStats(_uiState.value.selectedTimeframe)
        }
    }

    fun abandonChallenge() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { manageChallengeUseCase.abandon() }
            loadStats(_uiState.value.selectedTimeframe)
        }
    }

    /**
     * Renders the current 7-day view to a PNG and hands back a shareable URI.
     *
     * The card is drawn with Android canvas rather than by capturing the
     * composable: a capture depends on what is laid out on screen, and the
     * report has to fit on one image even when parts of the screen are scrolled
     * out of view.
     */
    fun shareWeeklyReport(onResult: (Uri?, String?) -> Unit) {
        val state = _uiState.value
        if (state.selectedTimeframe != Timeframe.WEEK || state.dailyTotals.isEmpty()) {
            onResult(null, "Switch to 7 days to share a weekly report.")
            return
        }

        viewModelScope.launch {
            try {
                val data = state.toWeeklyReportData()
                val file = withContext(Dispatchers.IO) { writeWeeklyReport(data) }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                onResult(uri, null)
            } catch (e: Exception) {
                onResult(null, "Couldn't create the weekly report.")
            }
        }
    }

    private fun StatsUiState.toWeeklyReportData(): WeeklyReportData {
        val (best, worst) = bestAndWorstDays(
            dailyDates.zip(dailyTotals) { date, minutes -> DailyUsageTotal(date, minutes) }
        )
        return WeeklyReportData(
            rangeLabel = weekRangeLabel(dailyDates),
            totalMinutes = totalScreenTimeMinutes,
            averageMinutes = averageDailyMinutes,
            comparisonPercent = highlights.weekOverWeekPercent,
            bestDay = best,
            worstDay = worst,
            daysUnderLimit = highlights.daysUnderLimit,
            daysMeasured = highlights.daysMeasured,
            focusCompleted = highlights.focusCompleted,
            unlocks = screenUnlocks
        )
    }

    private fun weekRangeLabel(dates: List<String>): String {
        fun parse(raw: String?): LocalDate? =
            raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val first = parse(dates.firstOrNull()) ?: return "This week"
        val last = parse(dates.lastOrNull()) ?: return "This week"
        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        return "${first.format(formatter)} – ${last.format(formatter)}"
    }

    private fun writeWeeklyReport(data: WeeklyReportData): File {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        // One report file at a time: the cache is a hand-off, not a gallery.
        dir.listFiles()?.filter { it.name.startsWith(WEEKLY_REPORT_PREFIX) }
            ?.forEach { it.delete() }
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return File(dir, "$WEEKLY_REPORT_PREFIX$day.png").apply {
            FileOutputStream(this).use { out ->
                renderWeeklyReport(context, data).compress(
                    android.graphics.Bitmap.CompressFormat.PNG,
                    100,
                    out
                )
            }
        }
    }

    /**
     * One 30-day read feeds every timeframe.
     *
     * The alternative — a query per timeframe — would make the today view slower
     * than the month view for no reason, and the week-over-week line needs 14
     * days even when the toggle says "Today".
     */
    private fun loadStats(timeframe: Timeframe) {
        viewModelScope.launch(Dispatchers.IO) {
            val limit = runCatching {
                soberPreferences.dailyScreenTimeLimitMinutes.first()
            }.getOrDefault(SoberPreferences.DEFAULT_DAILY_SCREEN_TIME_LIMIT)

            val history = runCatching { getUsageHistoryUseCase(30) }.getOrNull()
            val allTotals = history?.dailyTotals ?: emptyList()
            val selectedTotals = allTotals.takeLast(timeframe.days)
            val selectedDates = selectedTotals.map { it.date }.toSet()

            val perApp = history?.rows
                ?.filter { it.date in selectedDates }
                ?.groupBy { it.packageName }
                ?.mapValues { (_, rows) -> rows.sumOf { it.usedMinutes } }
                ?.toMutableMap()
                ?: mutableMapOf()

            // `usage_sessions` is written by the enforcement service every poll
            // but the live figure can be fresher for today; take the larger.
            val live = if (timeframe == Timeframe.TODAY) livePerApp() else emptyMap()
            live.forEach { (packageName, minutes) ->
                perApp[packageName] = maxOf(perApp[packageName] ?: 0, minutes)
            }

            val totals = if (timeframe == Timeframe.TODAY) {
                val liveTotal = live
                    .filterKeys { it !in nonAppPackages }
                    .values
                    .sum()
                listOf(maxOf(liveTotal, selectedTotals.lastOrNull()?.totalMinutes ?: 0))
            } else {
                selectedTotals.map { it.totalMinutes }
            }

            val limits = runCatching {
                appLimitRepository.getAllLimits().first()
            }.getOrDefault(emptyList())
                .filter { it.isActive && it.dailyLimitMinutes > 0 }
                .associate { it.packageName to it.dailyLimitMinutes }

            val now = System.currentTimeMillis()
            val rangeStart = GetUsageHeatmapUseCase.startOfDaysAgo(timeframe.days)
            val screenUnlocks = if (timeframe == Timeframe.WEEK) {
                runCatching {
                    usageStatsRepository.getScreenUnlockCount(rangeStart, now)
                }.getOrNull()
            } else {
                null
            }
            val completedSessions = runCatching {
                focusSessionRepository.countCompletedBetween(rangeStart, now)
            }.getOrDefault(0)
            val cancelledSessions = runCatching {
                focusSessionRepository.getCancelledBetween(rangeStart, now).size
            }.getOrDefault(0)

            val (underLimit, measured) = limitAdherence(selectedTotals, limit)

            val heatmap = runCatching {
                getUsageHeatmapUseCase(timeframe.days)
            }.getOrDefault(UsageHeatmap.EMPTY)

            val achievements = runCatching {
                getAchievementsUseCase()
            }.getOrElse { _uiState.value.achievements }

            val challenges = runCatching {
                getChallengeStateUseCase()
            }.getOrDefault(ChallengeState())

            val overrideSummary = runCatching {
                getOverrideSummaryUseCase(rangeStart, now)
            }.getOrDefault(OverrideSummary())

            val total = totals.sum()

            _uiState.value = StatsUiState(
                totalScreenTimeMinutes = total,
                averageDailyMinutes = if (totals.isEmpty()) 0 else total / totals.size,
                topApps = perApp.entries
                    .filter { (packageName, minutes) ->
                        minutes > 0 && packageName !in nonAppPackages
                    }
                    .sortedByDescending { it.value }
                    .take(10)
                    .map { (packageName, minutes) ->
                        AppUsageDisplay(
                            appName = appLabel(packageName),
                            packageName = packageName,
                            usageMinutes = minutes,
                            limitMinutes = limits[packageName],
                            icon = appIcon(packageName)
                        )
                    },
                dailyTotals = totals,
                dailyDates = selectedTotals.map { it.date },
                dailyLimitMinutes = limit,
                selectedTimeframe = timeframe,
                heatmap = heatmap,
                challenges = challenges,
                achievements = achievements,
                overrideSummary = overrideSummary,
                screenUnlocks = screenUnlocks,
                highlights = StatsHighlights(
                    focusCompleted = completedSessions,
                    focusCancelled = cancelledSessions,
                    mostProductiveWeekday = mostProductiveWeekday(selectedTotals),
                    weekOverWeekPercent = weekOverWeekChange(allTotals),
                    daysUnderLimit = underLimit,
                    daysMeasured = measured
                ),
                isLoading = false
            )
        }
    }

    private fun livePerApp(): Map<String, Int> = runCatching {
        usageStatsRepository.getUsageStatsForToday()
            .map { it.packageName to (it.totalTimeInForeground / 60_000L).toInt() }
            .toMap()
    }.getOrDefault(emptyMap())

    /**
     * The installed app's own label. A package name is not a name — the old
     * fallback turned "com.instagram.android" into "Android", which reads as a
     * different app entirely.
     */
    private fun appLabel(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrElse {
        packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    private fun appIcon(packageName: String): Drawable? = runCatching {
        context.packageManager.getApplicationIcon(packageName)
    }.getOrNull()

    companion object {
        /** Shared directory exposed through FileProvider; see res/xml/file_paths.xml. */
        const val EXPORT_DIR = "exports"
        const val WEEKLY_REPORT_PREFIX = "sober-week-"
    }
}
