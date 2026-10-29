package com.sober.system.services.core

import com.sober.core.domain.usecase.BlockInForceReason
import com.sober.core.domain.usecase.IsBlockingInForceUseCase
import com.sober.system.overlay.ui.SectionBlockOverlayController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Covers Settings → Accessibility while a block is in force (PRD 4.6.2).
 *
 * The PRD asks Sober to "re-open the Accessibility settings toggle". Android
 * does not allow that: a service cannot switch itself back on, and reaching
 * into Settings to flip the switch would be the exact self-granting behaviour
 * both the platform and Play policy forbid. The guard does the honest version
 * instead — it holds the screen the user would toggle from, says why, and
 * leaves "go back" as the only way out.
 *
 * It is inert when [IsBlockingInForceUseCase] reports nothing blocked. A speed
 * bump with no block behind it would just obstruct someone turning on a screen
 * reader.
 *
 * Every limitation is real and must stay visible in the copy: if the
 * overlay-permission is revoked the window cannot be drawn at all, and the
 * guard cannot stop the user from toggling the switch via adb or an OEM
 * shortcut.
 */
@Singleton
class AccessibilitySettingsGuard @Inject constructor(
    private val overlayController: SectionBlockOverlayController,
    private val isBlockingInForceUseCase: IsBlockingInForceUseCase
) {
    private var showing = false

    suspend fun onAccessibilitySettingsShown(onBack: () -> Unit) {
        // The block query reads UsageStatsManager, which is a binder call; it
        // must not run on the main thread that just delivered the event.
        val reason = withContext(Dispatchers.Default) { isBlockingInForceUseCase() }
        if (reason == null) {
            hide()
            return
        }

        showing = true
        overlayController.showMessage(
            key = KEY,
            title = "Sober is still on.",
            subtitle = subtitleFor(reason),
            footnote = "Turning Accessibility off pauses section blocking and notification " +
                "muting until it is back on. Focus sessions and limits keep working.",
            onBack = onBack
        )
    }

    fun hide() {
        if (!showing) return
        showing = false
        overlayController.hide()
    }

    private fun subtitleFor(reason: BlockInForceReason): String = when (reason) {
        BlockInForceReason.FOCUS_SESSION ->
            "A focus session is running. Go back and finish it first."
        BlockInForceReason.DAILY_LIMIT ->
            "You are at a daily limit. Go back to Sober to change it."
        BlockInForceReason.SCHEDULE ->
            "A schedule is blocking apps right now. It lifts on its own."
        BlockInForceReason.CUSTOM_APP ->
            "An app you blocked by hand is on the list. Undo that in Sober first."
    }

    private companion object {
        const val KEY = "accessibility-settings-guard"
    }
}
