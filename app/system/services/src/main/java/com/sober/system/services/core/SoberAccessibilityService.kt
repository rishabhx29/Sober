package com.sober.system.services.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.sober.core.common.accessibility.AccessibilitySettingsDetector
import com.sober.core.common.runtime.ServiceLiveness
import com.sober.core.domain.model.BrowserCatalog
import com.sober.core.domain.model.CapturedSection
import com.sober.core.domain.model.CustomRuleType
import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.SectionCaptureRequest
import com.sober.core.domain.model.WebsiteAddress
import com.sober.core.domain.repository.CustomRuleRepository
import com.sober.core.domain.repository.CustomSectionRuleRepository
import com.sober.core.domain.repository.DetectionRuleCatalog
import com.sober.core.domain.repository.DetectionRuleRepository
import com.sober.core.domain.repository.SectionCaptureRepository
import com.sober.core.domain.repository.SectionRulePreferencesRepository
import com.sober.core.domain.usecase.DetectBlockedSectionUseCase
import com.sober.core.domain.usecase.DetectBlockedWebsiteUseCase
import com.sober.core.domain.usecase.SuggestSectionCandidatesUseCase
import com.sober.system.overlay.ui.SectionBlockOverlayController
import com.sober.system.services.foreground.ForegroundSignal
import com.sober.system.services.foreground.isSystemTaskSwitcher
import com.sober.system.services.sections.SectionCaptureReader
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
    lateinit var customSectionRuleRepository: CustomSectionRuleRepository

    @Inject
    lateinit var sectionCaptureRepository: SectionCaptureRepository

    @Inject
    lateinit var suggestSectionCandidatesUseCase: SuggestSectionCandidatesUseCase

    @Inject
    lateinit var detectBlockedSectionUseCase: DetectBlockedSectionUseCase

    @Inject
    lateinit var detectBlockedWebsiteUseCase: DetectBlockedWebsiteUseCase

    @Inject
    lateinit var customRuleRepository: CustomRuleRepository

    @Inject
    lateinit var sectionOverlayController: SectionBlockOverlayController

    @Inject
    lateinit var accessibilitySettingsGuard: AccessibilitySettingsGuard

    @Inject
    lateinit var foregroundSignal: ForegroundSignal

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var catalog: DetectionRuleCatalog = DetectionRuleCatalog.EMPTY
    private var customRules: List<DetectionRule> = emptyList()
    private var enabledRuleIds: Set<String> = emptySet()
    private var monitoredPackages: Set<String> = emptySet()
    private var lastActivityName: String? = null
    private var lastEvaluatedAtMillis = 0L
    private var lastWebsiteEvaluatedAtMillis = 0L
    private var currentRuleId: String? = null
    private var currentWebsiteDomain: String? = null
    private var blockedDomains: Set<String> = emptySet()
    private var captureRequest: SectionCaptureRequest? = null
    private var lastCaptureAtMillis = 0L
    private var lastCaptured: CapturedSection? = null

    /** Shipped rules plus anything the user recorded, in that order. */
    private val allRules: List<DetectionRule>
        get() = catalog.rules + customRules

    override fun onServiceConnected() {
        super.onServiceConnected()
        ServiceLiveness.accessibilityConnected()

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
                if (allRules.none { rule -> rule.id in enabledRuleIds && rule.id == currentRuleId }) {
                    clearBlock()
                }
            }
        }
        scope.launch {
            customSectionRuleRepository.rules().collect { rules ->
                customRules = rules
                refreshMonitoredPackages()
                // Removing a recorded rule while its section is on screen must
                // release the cover immediately, like switching one off does.
                if (currentRuleId != null &&
                    allRules.none { it.id == currentRuleId && it.id in enabledRuleIds }
                ) {
                    clearBlock()
                }
            }
        }
        scope.launch {
            sectionCaptureRepository.request().collect { request ->
                if (request?.packageName != captureRequest?.packageName) {
                    // A new recording starts clean: the previous result belongs
                    // to another app and must not be read as this one's screen.
                    lastCaptured = null
                    lastCaptureAtMillis = 0L
                }
                captureRequest = request
            }
        }
        scope.launch {
            customRuleRepository.rules().collect { rules ->
                blockedDomains = rules
                    .asSequence()
                    .filter { it.enabled && it.type == CustomRuleType.BLOCKED_WEBSITE }
                    .mapNotNull { WebsiteAddress.normalizeDomain(it.value) }
                    .toSet()
                // Removing the rule while its site is on screen must release
                // the cover immediately, like switching a section off does.
                if (currentWebsiteDomain != null && currentWebsiteDomain !in blockedDomains) {
                    clearBlock()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val target = event ?: return
        // Any event proves the system is still delivering to us. Recorded
        // before the filters below so a quiet screen still looks alive.
        ServiceLiveness.accessibilityEventSeen()
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
            val activityName = target.className?.toString()
            lastActivityName = activityName

            // Tell the enforcement engine immediately, so an app-level block
            // is applied on the screen the user just opened rather than on the
            // next poll. The engine decides what to do with the package.
            //
            // The task switcher is the exception: it is System UI chrome, and
            // reporting it as a package would hold the current block over the
            // switcher instead of clearing it as the user leaving the app.
            if (isSystemTaskSwitcher(packageName, activityName)) {
                foregroundSignal.reportTaskSwitcher()
            } else {
                foregroundSignal.report(packageName)
            }

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

        // A recording is passive: it reads the screen the user walked to and
        // never adds a block of its own. It runs before the package filters so
        // an app outside the shipped catalog can still be recorded.
        if (captureRequest?.packageName == packageName) {
            recordCapture(packageName, type)
        }

        // Browsers are not section-rule packages. They are evaluated on their
        // own signal — the host in the address bar — so the section matcher,
        // which reads tabs and resource ids, has nothing to do here.
        if (BrowserCatalog.isBrowser(packageName)) {
            evaluateWebsite(packageName, type)
            return
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
        val match = detectBlockedSectionUseCase(snapshot, allRules, enabledRuleIds)
        if (match == null) clearBlock() else showBlock(match)
    }

    override fun onInterrupt() {
        clearBlock()
        accessibilitySettingsGuard.hide()
    }

    override fun onDestroy() {
        clearBlock()
        accessibilitySettingsGuard.hide()
        ServiceLiveness.accessibilityDisconnected()
        scope.cancel()
        super.onDestroy()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        clearBlock()
        accessibilitySettingsGuard.hide()
        ServiceLiveness.accessibilityDisconnected()
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
        monitoredPackages = allRules
            .asSequence()
            .filter { it.id in enabledRuleIds }
            .map { it.packageName }
            .toSet()
    }

    /**
     * Saves the selected-tab signals of the screen the user walked to.
     *
     * Only a changed result is written: a scrolling feed fires content events
     * constantly, and rewriting the same record dozens of times a second would
     * cost battery for no new information.
     */
    private fun recordCapture(packageName: String, eventType: Int) {
        val request = captureRequest ?: return
        val now = System.currentTimeMillis()
        if (now - request.requestedAt > CAPTURE_MAX_AGE_MS) return

        val isStateChange = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        val elapsed = SystemClock.uptimeMillis()
        if (!isStateChange && elapsed - lastCaptureAtMillis < CONTENT_THROTTLE_MS) return
        lastCaptureAtMillis = elapsed

        val root = rootInActiveWindow?.takeIf { it.packageName == packageName } ?: return
        val captured = CapturedSection(
            packageName = packageName,
            activityName = lastActivityName,
            capturedAt = now,
            candidates = suggestSectionCandidatesUseCase(
                activityName = lastActivityName,
                selected = SectionCaptureReader.readSelected(root)
            )
        )

        val previous = lastCaptured
        if (previous != null &&
            previous.packageName == captured.packageName &&
            previous.activityName == captured.activityName &&
            previous.candidates == captured.candidates
        ) {
            return
        }
        lastCaptured = captured
        scope.launch { sectionCaptureRepository.record(captured) }
    }

    /**
     * Reads the current page host and covers the window when it is blocked.
     *
     * Content events matter here: navigating inside a browser changes the
     * address bar without a window transition, so a state-change-only check
     * would let the first blocked page through but not the next one. The same
     * throttle as section rules keeps a scrolling page from re-reading the
     * tree on every frame.
     */
    private fun evaluateWebsite(packageName: String, eventType: Int) {
        // No rules means nothing to compare against. Bailing out before the
        // tree walk keeps Chrome's scrolling cheap for users who never set one.
        if (blockedDomains.isEmpty()) {
            clearWebsiteBlock()
            return
        }

        val isStateChange = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        val now = SystemClock.uptimeMillis()
        if (!isStateChange && now - lastWebsiteEvaluatedAtMillis < CONTENT_THROTTLE_MS) return
        lastWebsiteEvaluatedAtMillis = now

        val addressBarId = BrowserCatalog.addressBarIdFor(packageName) ?: return
        val root = rootInActiveWindow?.takeIf { it.packageName == packageName } ?: return
        val snapshot = SectionSnapshotReader.read(packageName, lastActivityName, root)
        val domain = detectBlockedWebsiteUseCase(
            addressBarText = snapshot.textByResourceId[addressBarId],
            blockedDomains = blockedDomains
        )
        if (domain == null) clearWebsiteBlock() else showWebsiteBlock(domain)
    }

    private fun showWebsiteBlock(domain: String) {
        // The app hides the cover when Sober itself comes to the front, so the
        // same domain still has to re-attach on the way back to the browser.
        if (currentWebsiteDomain == domain && sectionOverlayController.isShowing) return
        currentWebsiteDomain = domain
        currentRuleId = null
        sectionOverlayController.showMessage(
            key = "website:$domain",
            title = "$domain is blocked",
            subtitle = "You asked Sober to keep this website closed.",
            footnote = "It stays covered until you remove it from Sober.",
            onBack = { performGlobalAction(GLOBAL_ACTION_BACK) }
        )
    }

    private fun clearWebsiteBlock() {
        if (currentWebsiteDomain == null) return
        currentWebsiteDomain = null
        // A section rule cannot be active for a browser package, but the
        // overlay slot is shared, so hide it unconditionally.
        sectionOverlayController.hide()
    }

    private fun showBlock(rule: DetectionRule) {
        if (currentRuleId == rule.id && sectionOverlayController.isShowing) return
        currentRuleId = rule.id
        currentWebsiteDomain = null
        sectionOverlayController.show(rule) {
            // Back is the only exit offered: navigating the app off the tab
            // proves the section is gone, and the next event hides the window.
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun clearBlock() {
        if (currentRuleId == null && currentWebsiteDomain == null) return
        currentRuleId = null
        currentWebsiteDomain = null
        sectionOverlayController.hide()
    }

    private companion object {
        /**
         * Content events arrive in bursts while a feed scrolls. 250 ms is fast
         * enough to catch a tab switch and cheap enough for a scrolling app.
         */
        const val CONTENT_THROTTLE_MS = 250L

        /**
         * A recording older than this is not the screen the user meant any
         * more. The screen ends it when they return; this stops a forgotten
         * recording from turning a much later screen into a rule.
         */
        const val CAPTURE_MAX_AGE_MS = 10 * 60 * 1000L
    }
}
