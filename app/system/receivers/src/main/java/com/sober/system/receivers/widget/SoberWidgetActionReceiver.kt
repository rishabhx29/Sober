package com.sober.system.receivers.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sober.core.domain.repository.ProtectionSettingsRepository
import com.sober.system.services.core.SoberCoreService
import com.sober.system.services.widget.WidgetRefresh
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Services the widget's button and the refresh broadcasts the tile and engine
 * send. Not exported: only Sober itself, or a PendingIntent it minted, can
 * reach it.
 */
@AndroidEntryPoint
class SoberWidgetActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var protectionSettingsRepository: ProtectionSettingsRepository

    @Inject
    lateinit var renderer: SoberWidgetRenderer

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    WidgetActions.PAUSE -> pause(context)
                    WidgetActions.RESUME -> resume(context)
                }
                renderer.render(context)
            } catch (e: Exception) {
                // A failed tap leaves the widget as it was; it must not crash
                // the process.
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun pause(context: Context) {
        val now = System.currentTimeMillis()
        // A pause lifts every rule at once, so it is refused while locked —
        // the same rule the Home screen and the tile apply.
        val lockedUntil = protectionSettingsRepository.lockedUntil.first()
        if (lockedUntil != null && lockedUntil > now) return
        protectionSettingsRepository.setPausedUntil(now + PAUSE_MINUTES * 60_000L)
        SoberCoreService.start(context)
    }

    private suspend fun resume(context: Context) {
        protectionSettingsRepository.setPausedUntil(null)
        SoberCoreService.start(context)
    }

    private companion object {
        const val PAUSE_MINUTES = 15
    }
}
