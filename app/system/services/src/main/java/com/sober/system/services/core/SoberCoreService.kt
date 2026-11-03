package com.sober.system.services.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.repository.BlockableAppsRepository
import com.sober.core.domain.repository.PauseRulesRepository
import com.sober.core.domain.repository.ProtectionSettingsRepository
import com.sober.core.domain.usecase.BlockDecision
import com.sober.core.domain.usecase.BlockReason
import com.sober.core.domain.usecase.CheckBedtimeWarningUseCase
import com.sober.core.domain.usecase.CheckStreakNotificationsUseCase
import com.sober.core.domain.usecase.CheckUsageWarningsUseCase
import com.sober.core.domain.usecase.EvaluateBlockDecisionUseCase
import com.sober.core.domain.usecase.GetDailyScreenTimeUseCase
import com.sober.core.domain.usecase.ManageEmergencyOverrideUseCase
import com.sober.core.domain.usecase.ManageFocusSessionUseCase
import com.sober.core.domain.usecase.RecordAppOpenUseCase
import com.sober.core.domain.usecase.ResetDailyUsageUseCase
import com.sober.core.domain.usecase.SyncUsageSessionsUseCase
import com.sober.core.domain.repository.UsageStatsRepository
import com.sober.system.overlay.ui.BlockOverlayController
import com.sober.system.overlay.ui.PauseOverlayController
import com.sober.system.services.foreground.ForegroundSignal
import com.sober.system.services.widget.WidgetRefresh
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * The enforcement engine.
 *
 * Each poll cycle does four things, in order, and the order matters:
 *
 *  1. Roll the day over if the date changed, so a stale counter can never
 *     cause a false block at 00:01.
 *  2. Persist usage totals to `usage_sessions`, because limit checks read from
 *     that table and it was previously never written.
 *  3. Determine the real foreground app from usage-event transitions.
 *  4. Evaluate blocking policy and show or hide the overlay accordingly.
 *
 * The overlay is shown via [BlockOverlayController] (a WindowManager overlay),
 * not by launching an Activity — an Activity launch visibly switches away from
 * the app being blocked, which both looks wrong and loses the user's place.
 */
@AndroidEntryPoint
class SoberCoreService : Service() {

    @Inject
    lateinit var usageStatsRepository: UsageStatsRepository

    @Inject
    lateinit var soberPreferences: SoberPreferences

    @Inject
    lateinit var manageFocusSessionUseCase: ManageFocusSessionUseCase

    @Inject
    lateinit var syncUsageSessionsUseCase: SyncUsageSessionsUseCase

    @Inject
    lateinit var resetDailyUsageUseCase: ResetDailyUsageUseCase

    @Inject
    lateinit var evaluateBlockDecisionUseCase: EvaluateBlockDecisionUseCase

    @Inject
    lateinit var getDailyScreenTimeUseCase: GetDailyScreenTimeUseCase

    @Inject
    lateinit var checkUsageWarningsUseCase: CheckUsageWarningsUseCase

    @Inject
    lateinit var checkStreakNotificationsUseCase: CheckStreakNotificationsUseCase

    @Inject
    lateinit var checkBedtimeWarningUseCase: CheckBedtimeWarningUseCase

    @Inject
    lateinit var manageEmergencyOverrideUseCase: ManageEmergencyOverrideUseCase

    @Inject
    lateinit var recordAppOpenUseCase: RecordAppOpenUseCase

    @Inject
    lateinit var overlayController: BlockOverlayController

    @Inject
    lateinit var pauseOverlayController: PauseOverlayController

    @Inject
    lateinit var pauseRulesRepository: PauseRulesRepository

    @Inject
    lateinit var foregroundSignal: ForegroundSignal

    @Inject
    lateinit var blockableAppsRepository: BlockableAppsRepository

    @Inject
    lateinit var protectionSettingsRepository: ProtectionSettingsRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    /** Serialises policy application: the poll and window hints both write it. */
    private val policyMutex = Mutex()

    private val powerManager: PowerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    /** The default home app; never blocked, and it clears whatever is showing. */
    private val homePackage: String? by lazy {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
    }

    /** The app+reason currently on screen, as "package|reason", or null. */
    @Volatile
    private var currentlyBlocked: String? = null

    /** Last text posted to the daily-cap notification, to skip identical updates. */
    @Volatile
    private var lastDailyCapText: String? = null

    /**
     * The last app counted as an "open". Both the poll and window hints call
     * [applyPolicy], so without this an app open for a minute would be counted
     * four times by the poll alone.
     */
    @Volatile
    private var lastCountedPackage: String? = null

    /** Last day the open history was pruned, so it runs once per day, not per poll. */
    @Volatile
    private var lastOpenPruneDay: String? = null

    /**
     * The app whose pause-before-opening was already handled for the current
     * foreground run. A pause belongs to an open, not to every poll: without
     * this, a pass expiring while the user is still inside the app would throw
     * the pause screen back over it.
     */
    @Volatile
    private var pauseCheckedPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createDailyCapChannel()
        // Started here, not in onStartCommand: each start() call delivers another
        // onStartCommand to a running service, and launching the loops there
        // added a second poll, a second countdown ticker and a second window-hint
        // collector every time the app opened or a session was started or
        // stopped. onCreate runs once per service instance, so the loops have
        // exactly one owner.
        startEnforcementLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        when {
            // Android 15 caps dataSync at six hours per day, which a watchdog
            // that must run whenever the phone is in use would blow through.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            else -> startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    private fun startEnforcementLoop() {
        // Session countdown drives the notification text.
        scope.launch {
            soberPreferences.activeSessionId.collectLatest { sessionId ->
                if (sessionId != null) {
                    // Launched inside the collectLatest block so that a session
                    // change cancels the previous ticker. Launching into `scope`
                    // instead would leave orphaned tickers running and fighting
                    // over the notification.
                    launchCountdownTicker()
                } else {
                    updateNotification("Sober is active", "Protecting your focus...")
                }
            }
        }

        // Enforcement cycle.
        scope.launch {
            while (true) {
                try {
                    // With the screen off nothing can be in the foreground, so
                    // a full cycle would query UsageStats, write the rollup and
                    // refresh a notification for a state that cannot change.
                    // Checking cheaply instead keeps the wait after unlocking
                    // short without doing any work overnight.
                    if (powerManager.isInteractive) {
                        runEnforcementCycle()
                    }
                    delay(if (powerManager.isInteractive) POLL_INTERVAL_MS else SCREEN_OFF_RECHECK_MS)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // A single bad cycle must never kill the engine; log-free by
                    // design here, but the loop continues so enforcement recovers
                    // on the next tick.
                    currentlyBlocked = null
                    overlayController.hide()
                    delay(SCREEN_OFF_RECHECK_MS)
                }
            }
        }

        // Window hints from the accessibility service. The poll stays the path
        // that always works; this makes an app-level block land as soon as a
        // blocked app opens, instead of up to one poll later.
        scope.launch {
            foregroundSignal.packages.collect { packageName ->
                try {
                    policyMutex.withLock { applyPolicy(packageName) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    currentlyBlocked = null
                    overlayController.hide()
                }
            }
        }

        // Bedtime silence runs independently of the screen state: the window
        // usually opens while the phone is idle, and the enforcement loop
        // deliberately does no work then. One DataStore read a minute is far
        // cheaper than waking the full poll to change a single filter.
        scope.launch {
            while (true) {
                runCatching { syncBedtimeSilence() }
                delay(BEDTIME_SYNC_INTERVAL_MS)
            }
        }

        // The home-screen widget shows a point-in-time summary, so it only
        // needs a refresh when one of its inputs changes — not on every poll.
        scope.launch {
            combine(
                protectionSettingsRepository.pausedUntil,
                protectionSettingsRepository.lockedUntil,
                soberPreferences.activeSessionId,
                soberPreferences.activeSessionStartTime,
                soberPreferences.activeSessionDuration
            ) { pausedUntil, lockedUntil, sessionId, startTime, duration ->
                "$pausedUntil|$lockedUntil|$sessionId|$startTime|$duration"
            }
                .distinctUntilChanged()
                .collect { WidgetRefresh.request(this@SoberCoreService) }
        }
    }

    /**
     * Turns Do Not Disturb on for the bedtime window and puts the user's own
     * filter back when it closes.
     *
     * The previous filter is stored before the first change. Without it, ending
     * bedtime would force "all" and quietly cancel a Do Not Disturb the user had
     * chosen for themselves. Policy access is checked every time: a revoked
     * grant must leave the filter alone rather than throw, and the stored flag
     * stays set so the restore runs once access returns.
     */
    private suspend fun syncBedtimeSilence() {
        val bedtime = protectionSettingsRepository.bedtime.first()
        val active = bedtime.enabled && bedtime.isActive(minuteOfDay(System.currentTimeMillis()))
        val silenced = protectionSettingsRepository.bedtimeSilenced.first()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.isNotificationPolicyAccessGranted) return

        if (active && !silenced) {
            protectionSettingsRepository.setBedtimePreviousFilter(
                manager.currentInterruptionFilter
            )
            protectionSettingsRepository.setBedtimeSilenced(true)
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        } else if (!active && silenced) {
            val previous = protectionSettingsRepository.bedtimePreviousFilter.first()
            manager.setInterruptionFilter(
                if (previous in RESTORABLE_INTERRUPTION_FILTERS) {
                    previous
                } else {
                    // An unrecognised value means the write was interrupted or
                    // the user changed DND by hand; "all" is the safe answer.
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
            )
            protectionSettingsRepository.setBedtimePreviousFilter(-1)
            protectionSettingsRepository.setBedtimeSilenced(false)
        }
    }

    private fun minuteOfDay(now: Long): Int = java.util.Calendar.getInstance().apply {
        timeInMillis = now
    }.let { it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE) }

    private suspend fun runEnforcementCycle() {
        // 1. Daily rollover before anything reads counters.
        resetDailyUsageUseCase()

        // 2. Persist today's totals so limit checks have data to read.
        syncUsageSessionsUseCase()

        // 2b. Refresh the daily-cap notification from the same totals.
        updateDailyCapNotification()

        // 2c. Post the once-a-day 80% warnings, if any limit just crossed it.
        checkUsageWarningsUseCase()

        // 2c-bis. Evening streak note (22:00–midnight only) and the bedtime
        //        warning when a sleep schedule is about to open.
        checkStreakNotificationsUseCase()
        checkBedtimeWarningUseCase()

        // 2d. Drop override state that can no longer do anything, so an expired
        //     grant is not read back as if it were still current.
        manageEmergencyOverrideUseCase.clearExpired()

        // 2e. Keep the open history bounded. Once a day is enough for a table
        //     that only needs the current day.
        val day = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        if (day != lastOpenPruneDay) {
            lastOpenPruneDay = day
            runCatching { recordAppOpenUseCase.prune() }
        }

        // 3. Foreground app from event transitions (not lastTimeUsed).
        val foreground = usageStatsRepository.getForegroundPackage()

        // 4. Policy evaluation.
        policyMutex.withLock { applyPolicy(foreground) }
    }

    /**
     * Shows or hides the overlay for [foreground], the package on screen.
     *
     * The launcher and Sober itself are never blocked: the overall cap applies
     * to apps, and covering Home would leave the user with no way to navigate.
     * The poll already reports both as "unknown"; a window hint names them, so
     * they are mapped to the same answer here.
     *
     * A keyboard, the notification shade or a permission dialog can also come
     * to the foreground while an app is open. Those are not app switches, so
     * the current overlay state is held rather than cleared — otherwise typing
     * in a text field could lift an active block until the next poll.
     */
    private suspend fun applyPolicy(foreground: String?) {
        if (foreground != null && blockableAppsRepository.isTransientChrome(foreground)) return

        val target = foreground?.takeUnless {
            it == homePackage ||
                it == packageName ||
                !blockableAppsRepository.isBlockable(it)
        }

        if (target == null) {
            lastCountedPackage = null
            pauseCheckedPackage = null
            if (pauseOverlayController.isShowing) pauseOverlayController.hide()
            if (currentlyBlocked != null) {
                currentlyBlocked = null
                overlayController.hide()
            }
            return
        }

        // Evaluation happens before counting this entry: the opens budget is
        // "how many times may this open", so the current attempt must be
        // checked against the previous count, not against a count it just
        // incremented itself. Otherwise a budget of one blocks the first open.
        val decision = evaluateBlockDecisionUseCase(target)
        if (decision != null) {
            // A block outranks a pause: the app is not usable either way, and
            // the block screen is the truthful explanation.
            if (pauseOverlayController.isShowing) pauseOverlayController.hide()
            pauseCheckedPackage = target
            showBlock(decision)
            return
        }

        // Pause before opening applies once per foreground run.
        if (target != pauseCheckedPackage) {
            pauseCheckedPackage = target
            // A previous app's pause window must not survive the switch.
            if (pauseOverlayController.isShowing) pauseOverlayController.hide()

            val rule = runCatching { pauseRulesRepository.ruleFor(target) }.getOrNull()
            val pass = runCatching { pauseRulesRepository.passUntil(target) }.getOrNull()
            val now = System.currentTimeMillis()
            if (rule != null && rule.delaySeconds > 0 && (pass == null || pass <= now)) {
                if (currentlyBlocked != null) {
                    currentlyBlocked = null
                    overlayController.hide()
                }
                pauseOverlayController.show(target, rule.delaySeconds)
                // Not counted yet: backing out at the pause is not an open.
                return
            }
        }

        // Counting happens on the transition, not on every poll. Going to the
        // launcher clears the marker, so returning to the same app is a new
        // open; a keyboard or dialog never reaches here, so it cannot inflate
        // the count either. Blocked and abandoned entries are not counted, so
        // the budget measures use rather than attempts.
        if (target != lastCountedPackage) {
            lastCountedPackage = target
            runCatching { recordAppOpenUseCase(target) }
        }

        if (currentlyBlocked != null) {
            // Limit lifted (override, app removed, session ended, or user
            // switched to an allowed app) — clear the overlay.
            currentlyBlocked = null
            overlayController.hide()
        }
    }

    private fun showBlock(decision: BlockDecision) {
        // Re-render when either the app or the reason changes: the same app can
        // move from a focus-session block to a daily-limit block, and the
        // overlay copy has to follow.
        // `isShowing` matters: the overlay hides itself after granting an
        // emergency override, and a stale signature alone would then suppress
        // the re-show when the override expires.
        val signature = "${decision.packageName}|${decision.reason}"
        if (currentlyBlocked == signature && overlayController.isShowing) return
        currentlyBlocked = signature
        overlayController.show(decision)
    }

    private fun launchCountdownTicker() {
        scope.launch {
            var lastText: String? = null
            while (true) {
                val startTime = soberPreferences.activeSessionStartTime.first() ?: return@launch
                val durationMs = soberPreferences.activeSessionDuration.first() ?: return@launch
                val label = soberPreferences.activeSessionLabel.first() ?: "Focus Session"
                val sessionId = soberPreferences.activeSessionId.first() ?: return@launch

                // Duration 0 marks an open-ended session (PRD 2.6): there is no
                // window to count down, and "zero remaining" must never be read
                // as completion or the session would end on its first tick.
                val text = if (durationMs <= 0L) {
                    "Open-ended — runs until you stop"
                } else {
                    val elapsedMs = System.currentTimeMillis() - startTime
                    // Wall-clock time is what survives a reboot, but a clock
                    // change must never stretch a session: clamp elapsed into
                    // its window.
                    val remainingMs = durationMs - elapsedMs.coerceIn(0L, durationMs)

                    if (remainingMs <= 0L) {
                        updateNotification(label, "Session complete!")
                        manageFocusSessionUseCase.completeSession(sessionId)
                        // Session end may unblock whatever is on screen.
                        overlayController.hide()
                        currentlyBlocked = null
                        return@launch
                    }

                    val minutes = (remainingMs / 1000) / 60
                    val seconds = (remainingMs / 1000) % 60
                    String.format("%02d:%02d remaining", minutes, seconds)
                }

                if (text != lastText) {
                    lastText = text
                    updateNotification(label, text)
                }
                delay(1000)
            }
        }
    }

    private fun updateNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Posts the overall-cap countdown on its own channel and id so it never
     * overwrites the focus-session countdown — the two answer different
     * questions ("when does my session end" vs "how much phone time is left").
     *
     * Remaining time is recomputed from the same totals enforcement uses, and
     * the post is skipped when the rounded value has not changed, so a 15s
     * poll does not produce 240 notification updates an hour.
     */
    private suspend fun updateDailyCapNotification() {
        val limitMinutes = soberPreferences.dailyScreenTimeLimitMinutes.first()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (limitMinutes <= 0) {
            if (lastDailyCapText != null) {
                manager.cancel(DAILY_CAP_NOTIFICATION_ID)
                lastDailyCapText = null
            }
            return
        }

        val usedMinutes = getDailyScreenTimeUseCase()
        val remainingMinutes = (limitMinutes - usedMinutes).coerceAtLeast(0)

        val title: String
        val text: String
        if (remainingMinutes == 0) {
            title = "Daily limit reached"
            text = "Apps are blocked until midnight."
        } else {
            title = "Sober is active"
            text = "${formatDuration(remainingMinutes)} left of your ${formatDuration(limitMinutes)} daily cap"
        }

        val signature = "$title|$text"
        if (signature == lastDailyCapText) return
        lastDailyCapText = signature

        val notification = NotificationCompat.Builder(this, DAILY_CAP_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(DAILY_CAP_NOTIFICATION_ID, notification)
    }

    /** "1h 12m" / "42m", matching how the rest of the app writes durations. */
    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayController.hide()
        pauseOverlayController.hide()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(DAILY_CAP_NOTIFICATION_ID)
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sober Core Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs the core blocking and tracking engine for Sober."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createDailyCapChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DAILY_CAP_CHANNEL_ID,
                "Daily screen time",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows how much of your daily screen-time cap is left."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sober is active")
            .setContentText("Protecting your focus...")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "sober_core_channel"
        private const val NOTIFICATION_ID = 1001

        private const val DAILY_CAP_CHANNEL_ID = "sober_daily_cap_channel"
        private const val DAILY_CAP_NOTIFICATION_ID = 1002

        /**
         * 15s while the screen is on: the poll is the fallback for devices where
         * accessibility is off, and a longer window leaves a blocked app usable
         * for that much longer before the overlay lands. With accessibility on,
         * window hints make this a safety net rather than the main path.
         *
         * The battery saving is not a longer interval but skipping the cycle
         * entirely while the screen is off — see [SCREEN_OFF_RECHECK_MS].
         */
        private const val POLL_INTERVAL_MS = 15_000L

        /**
         * Screen off: no app is in the foreground, so only re-check "is the
         * screen on again" at this pace. Five seconds keeps the wait after
         * unlocking short while doing no UsageStats or database work overnight.
         */
        private const val SCREEN_OFF_RECHECK_MS = 5_000L

        /** How often the bedtime window is checked for DND changes. */
        private const val BEDTIME_SYNC_INTERVAL_MS = 60_000L

        /**
         * Filters worth putting back after bedtime. `UNKNOWN` (0) and the
         * sentinel -1 are not values Sober could have seen before its own
         * change, so restoring them would be worse than restoring "all".
         */
        private val RESTORABLE_INTERRUPTION_FILTERS = setOf(
            NotificationManager.INTERRUPTION_FILTER_ALL,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_ALARMS
        )

        fun start(context: Context) {
            val intent = Intent(context, SoberCoreService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SoberCoreService::class.java)
            context.stopService(intent)
        }
    }
}