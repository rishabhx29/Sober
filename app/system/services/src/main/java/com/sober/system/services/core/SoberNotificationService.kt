package com.sober.system.services.core

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sober.core.domain.model.NotificationSilenceScope
import com.sober.core.domain.repository.AlertNotifier
import com.sober.core.domain.repository.CustomRuleRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.usecase.MatchNotificationKeywordUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hides notifications in two cases, and only two.
 *
 * A focus session silences the apps it blocks, according to its scope setting.
 * Separately — and outside any session — the user's own keyword rules hide
 * notifications whose text contains a word they chose.
 *
 * Nothing else is touched: without a session and without a rule this service
 * lets every notification through, so it can never become a silent general
 * filter the user did not ask for. Calls are never keyword-filtered, because
 * hiding a call is not a preference, it is a missed call.
 */
@AndroidEntryPoint
class SoberNotificationService : NotificationListenerService() {

    @Inject
    lateinit var sessionStateRepository: SessionStateRepository

    @Inject
    lateinit var alertNotifier: AlertNotifier

    @Inject
    lateinit var customRuleRepository: CustomRuleRepository

    @Inject
    lateinit var matchNotificationKeywordUseCase: MatchNotificationKeywordUseCase

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val key = sbn.key

        scope.launch {
            if (isHiddenByKeyword(sbn)) {
                cancelNotification(key)
                return@launch
            }

            val activeSessionId = sessionStateRepository.activeSessionId.first() ?: return@launch
            val scopeSetting = sessionStateRepository.notificationSilenceScope.first() ?: return@launch

            if (scopeSetting == NotificationSilenceScope.NONE) return@launch

            val isBlockedApp = sessionStateRepository.activeSessionBlockedApps.first()
                ?.split(',')
                ?.filter { it.isNotBlank() }
                ?.contains(packageName)
                ?: false

            val shouldSilence = when (scopeSetting) {
                NotificationSilenceScope.ALL -> true
                // "All except calls" is a phone call; on Android these arrive
                // via the dialer/telecom packages, which we match by role.
                NotificationSilenceScope.ALL_EXCEPT_CALLS ->
                    !isLikelyCallNotification(packageName)
                NotificationSilenceScope.SELECTED_APPS -> isBlockedApp
                NotificationSilenceScope.NONE -> false
            }

            // Never silence Sober's own foreground notification.
            if (shouldSilence && packageName != applicationContext.packageName) {
                cancelNotification(key)
            }
        }
    }

    /**
     * Keyword rules apply whether or not a session is running: they are the
     * user's standing instruction, not part of the session's scope.
     */
    private suspend fun isHiddenByKeyword(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName
        if (packageName == applicationContext.packageName) return false
        if (isLikelyCallNotification(packageName)) return false

        val rules = runCatching { customRuleRepository.rules().first() }
            .getOrDefault(emptyList())
        if (rules.isEmpty()) return false

        return matchNotificationKeywordUseCase(notificationTexts(sbn), rules) != null
    }

    /**
     * Every text field the user can actually read. A title-only match would
     * miss the common "App name in title, the message in the body" shape, and
     * messaging apps put long messages in the big-text field.
     */
    private fun notificationTexts(sbn: StatusBarNotification): List<String?> {
        val extras = sbn.notification?.extras ?: return emptyList()
        return buildList {
            add(extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
            add(extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
            add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString())
            add(extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString())
            add(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString())
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.forEach { add(it?.toString()) }
        }
    }

    private fun isLikelyCallNotification(packageName: String): Boolean {
        // Heuristic, deliberately narrow: only the in-call UI and dialer
        // packages count as "calls" for the all-except-calls option. Anything
        // ambiguous is treated as not-a-call, so the option errs toward
        // silencing rather than letting entertainment notifications through.
        return packageName == "com.android.dialer" ||
            packageName == "com.google.android.dialer" ||
            packageName == "com.android.incallui" ||
            packageName == "com.android.server.telecom"
    }

    /**
     * Notification Access can be revoked at any time — the user toggles it off,
     * or an OEM battery sweep unbinds the service. During a session that means
     * the promise to silence notifications is quietly broken, so say so and
     * offer the setting as a tap target.
     */
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()

        scope.launch {
            val sessionId = sessionStateRepository.activeSessionId.first() ?: return@launch
            val scopeSetting = sessionStateRepository.notificationSilenceScope.first()
                ?: NotificationSilenceScope.NONE
            if (scopeSetting == NotificationSilenceScope.NONE) return@launch

            alertNotifier.notifyNotificationAccessRevoked()
        }

        // Re-binding is best-effort: the system may refuse until the user
        // re-grants access, in which case the alert above is the durable nudge.
        try {
            requestRebind(ComponentName(this, SoberNotificationService::class.java))
        } catch (e: Exception) {
            // Never let a rebind failure take down the service.
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}