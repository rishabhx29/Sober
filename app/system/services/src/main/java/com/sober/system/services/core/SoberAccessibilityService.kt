package com.sober.system.services.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.sober.core.common.accessibility.AccessibilitySettingsDetector
import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.repository.DetectionRuleCatalog
import com.sober.core.domain.repository.DetectionRuleRepository
import com.sober.core.domain.repository.SectionRulePreferencesRepository
import com.sober.core.domain.usecase.DetectBlockedSectionUseCase
import com.sober.system.overlay.ui.SectionBlockOverlayController
import com.sober.system.services.foreground.ForegroundSignal
import com.sober.system.services.sections.SectionSnapshotReader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Detects blocked sections in the apps the user switched rules on for.
 *
 * This service only ever *adds* a section block. Whether an app is blocked
 * outright stays with the enforcement engine, which runs whether or not
 * Accessibility access has been granted — so losing this service degrades to
 * package-level limits rather than to no protection at all.
 *
 * It also watches for the Settings → Accessibility screen and hands it to
 * [AccessibilitySettingsGuard] while a block is in force.
 *
 * Every event ends in either a match or `clearBlock()`; nothing is persisted
 * here, so a service restart cannot leave a stale overlay rule behind.
 */
@AndroidEntryPoint
class SoberAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var detectionRuleRepository: DetectionRuleRepository

    @Inject
    lateinit var sectionRulePreferencesRepository: SectionRulePreferencesRepository

    @Inject
    lateinit var detectBlockedSectionUseCase: DetectBlockedSectionUseCase

    @Inject
    lateinit var sectionOverlayController: SectionBlockOverlayController

    @Inject
    lateinit var accessibilitySettingsGuard: AccessibilitySettingsGuard

    @Inject
    lateinit var foregroundSignal: ForegroundSignal

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var catalog: DetectionRuleCatalog = DetectionRuleCatalog.EMPTY
    private var enabledRuleIds: Set<String> = emptySet()
    private var monitoredPackages: Set<String> = emptySet()
    private var lastActivityName: String? = null
    private var lastEvaluatedAtMillis = 0L
    private var currentRuleId: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        // viewIdResourceName is null without FLAG_REPORT_VIEW_IDS, which would
        // make every RESOURCE_ID / SELECTED_TAB rule silently never match.
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }

        scope.launch {
            catalog = runCatching { detectionRuleRepository.load() }
                .getOrDefault(DetectionRuleCatalog.EMPTY)
            refreshMonitoredPackages()
        }
        scope.launch {
            sectionRulePreferencesRepository.enabledRuleIds().collect {
                enabledRuleIds = it
                refreshMonitoredPackages()
                // A rule switched off while its section is on screen must
                // release the overlay without waiting for the next app switch.
                if (catalog.rules.none { rule -> rule.id in enabledRuleIds && rule.id == currentRuleId }) {
                    clearBlock()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val target = event ?: return
        val type = target.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }
        val packageName = target.packageName?.toString() ?: return
        // The overlay window is ours; observing it would make this service
        // react to its own output.
        if (packageName == this.packageName) return

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastActivityName = target.className?.toString()

            // Tell the enforcement engine immediately, so an app-level block
            // is applied on the screen the user just opened rather than on the
            // next poll. The engine decides what to do with the package.
            foregroundSignal.report(packageName)

            // The guard is a screen-level decision, so it is evaluated on
            // transitions only: content events would re-run the block query
            // while the user scrolls the settings list.
            if (isAccessibilitySettings(target, packageName)) {
                clearBlock()
                scope.launch {
                    accessibilitySettingsGuard.onAccessibilitySettingsShown {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }
                return
            }
            accessibilitySettingsGuard.hide()
        }

        if (packageName !in monitoredPackages) {
            // Only a real app switch means "leave the blocked section".
            // Unrelated content events (keyboard, system UI) must not make the
            // overlay flicker off and on.
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) clearBlock()
            return
        }

        val now = SystemClock.uptimeMillis()
        val isStateChange = type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (!isStateChange && now - lastEvaluatedAtMillis < CONTENT_THROTTLE_MS) return
        lastEvaluatedAtMillis = now

        // rootInActiveWindow belongs to whichever window is active; while the
        // overlay is up that can be our own, so only evaluate roots of the app
        // being inspected.
        val root = rootInActiveWindow?.takeIf { it.packageName == packageName } ?: return

        val snapshot = SectionSnapshotReader.read(packageName, lastActivityName, root)
        val match = detectBlockedSectionUseCase(snapshot, catalog.rules, enabledRuleIds)
        if (match == null) clearBlock() else showBlock(match)
    }

    override fun onInterrupt() {
        clearBlock()
        accessibilitySettingsGuard.hide()
    }

    override fun onDestroy() {
        clearBlock()
        accessibilitySettingsGuard.hide()
        scope.cancel()
        super.onDestroy()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        clearBlock()
        accessibilitySettingsGuard.hide()
        return super.onUnbind(intent)
    }

    private fun isAccessibilitySettings(
        event: AccessibilityEvent,
        packageName: String
    ): Boolean {
        val texts = event.text.map { it.toString() } +
            listOfNotNull(event.contentDescription?.toString())
        return AccessibilitySettingsDetector.isAccessibilitySettings(
            packageName = packageName,
            activityName = event.className?.toString(),
            texts = texts
        )
    }

    private fun refreshMonitoredPackages() {
        monitoredPackages = catalog.rules
            .asSequence()
            .filter { it.id in enabledRuleIds }
            .map { it.packageName }
            .toSet()
    }

    private fun showBlock(rule: DetectionRule) {
        if (currentRuleId == rule.id) return
        currentRuleId = rule.id
        sectionOverlayController.show(rule) {
            // Back is the only exit offered: navigating the app off the tab
            // proves the section is gone, and the next event hides the window.
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun clearBlock() {
        if (currentRuleId == null) return
        currentRuleId = null
        sectionOverlayController.hide()
    }

    private companion object {
        /**
         * Content events arrive in bursts while a feed scrolls. 250 ms is fast
         * enough to catch a tab switch and cheap enough for a scrolling app.
         */
        const val CONTENT_THROTTLE_MS = 250L
    }
}
