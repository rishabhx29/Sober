package com.sober.system.services.tile

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.sober.core.domain.repository.ProtectionSettingsRepository
import com.sober.system.services.R
import com.sober.system.services.core.SoberCoreService
import com.sober.system.services.widget.WidgetRefresh
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * The Quick Settings tile: tap to pause everything for fifteen minutes, tap
 * again to resume.
 *
 * Pausing is refused while lock mode is on — a pause lifts every rule at once,
 * so it is the shortest path around a lock. Rather than leave a tile that does
 * nothing when tapped, the tile opens Sober so the user can see the end time.
 */
@AndroidEntryPoint
class SoberTileService : TileService() {

    @Inject
    lateinit var protectionSettingsRepository: ProtectionSettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listeningJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        listeningJob?.cancel()
        listeningJob = scope.launch {
            combine(
                protectionSettingsRepository.pausedUntil,
                protectionSettingsRepository.lockedUntil
            ) { pausedUntil, lockedUntil -> pausedUntil to lockedUntil }
                .catch { }
                .collect { (pausedUntil, lockedUntil) -> render(pausedUntil, lockedUntil) }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningJob?.cancel()
        listeningJob = null
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val lockedUntil = protectionSettingsRepository.lockedUntil.first()
                if (lockedUntil != null && lockedUntil > now) {
                    openApp()
                    return@runCatching
                }

                val pausedUntil = protectionSettingsRepository.pausedUntil.first()
                val paused = pausedUntil != null && pausedUntil > now
                protectionSettingsRepository.setPausedUntil(
                    if (paused) null else now + PAUSE_MINUTES * 60_000L
                )
                SoberCoreService.start(this@SoberTileService)
                WidgetRefresh.request(this@SoberTileService)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render(pausedUntil: Long?, lockedUntil: Long?) {
        val tile = qsTile ?: return
        val now = System.currentTimeMillis()
        val paused = pausedUntil != null && pausedUntil > now
        val locked = lockedUntil != null && lockedUntil > now

        tile.icon = Icon.createWithResource(this, R.drawable.ic_sober_tile)
        tile.label = getString(R.string.tile_label)
        tile.state = if (paused) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                locked -> getString(R.string.tile_locked)
                paused -> getString(R.string.tile_paused)
                else -> getString(R.string.tile_active)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val description = when {
                locked -> getString(R.string.tile_state_locked, formatTime(lockedUntil))
                paused -> getString(R.string.tile_state_paused, formatTime(pausedUntil))
                else -> getString(R.string.tile_state_on)
            }
            tile.contentDescription = description
            tile.stateDescription = description
        }
        tile.updateTile()
    }

    private fun openApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun formatTime(epochMillis: Long?): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis ?: 0L))

    private companion object {
        /** Long enough to step away, short enough that it is hard to forget. */
        const val PAUSE_MINUTES = 15
    }
}
