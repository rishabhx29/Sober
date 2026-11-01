package com.sober.feature.stats

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.model.Achievement
import com.sober.core.domain.model.AchievementId
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.FocusSessionRepository
import com.sober.core.domain.repository.UsageStatsRepository
import com.sober.core.domain.usecase.GetAchievementsUseCase
import com.sober.core.domain.usecase.GetOverrideSummaryUseCase
import com.sober.core.domain.usecase.GetUsageHeatmapUseCase
import com.sober.core.domain.usecase.GetUsageHistoryUseCase
import com.sober.core.domain.usecase.OverrideSummary
import com.sober.core.domain.usecase.StatsHighlights
import com.sober.core.domain.usecase.UsageHeatmap
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
    val achievements: List<Achievement> = AchievementId.entries.map {
        Achievement(it, isUnlocked = false)
    },
    val isLoading: Boolean = true
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
    private val soberPreferences: SoberPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

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
                val liveTotal = live.values.sum()
                listOf(maxOf(liveTotal, selectedTotals.lastOrNull()?.totalMinutes ?: 0))
            } else {
                selectedTotals.map { it.totalMinutes }
            }

            val limits = runCatching {
                appLimitRepository.getAllLimits().first()
            }.getOrDefault(emptyList())
                .filter { it.isActive }
                .associate { it.packageName to it.dailyLimitMinutes }

            val now = System.currentTimeMillis()
            val rangeStart = GetUsageHeatmapUseCase.startOfDaysAgo(timeframe.days)
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
                achievements = achievements,
                overrideSummary = overrideSummary,
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
}
