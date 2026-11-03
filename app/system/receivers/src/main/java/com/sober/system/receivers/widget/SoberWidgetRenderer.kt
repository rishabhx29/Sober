package com.sober.system.receivers.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.sober.core.domain.repository.ProtectionSettingsRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.system.receivers.R
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the home-screen widget from the same stored state the engine reads.
 *
 * The copy is deliberately point-in-time ("Paused until 3:42 PM") instead of a
 * countdown: a widget does not tick, so a minute value would go stale between
 * refreshes, while an end time stays true until the next update.
 */
@Singleton
class SoberWidgetRenderer @Inject constructor(
    private val protectionSettingsRepository: ProtectionSettingsRepository,
    private val sessionStateRepository: SessionStateRepository
) {

    suspend fun render(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, SoberStatusWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, buildViews(context))
    }

    private suspend fun buildViews(context: Context): RemoteViews {
        val now = System.currentTimeMillis()
        val lockedUntil = protectionSettingsRepository.lockedUntil.first()
        val pausedUntil = protectionSettingsRepository.pausedUntil.first()
        val sessionId = sessionStateRepository.activeSessionId.first()

        val locked = lockedUntil != null && lockedUntil > now
        val paused = pausedUntil != null && pausedUntil > now

        val title: String
        val detail: String
        val action: String?
        when {
            // Lock outranks the other states: while it is on, neither the app
            // nor this button can lift protection, so the copy says so.
            locked -> {
                title = context.getString(R.string.widget_locked)
                detail = context.getString(R.string.widget_until, formatTime(lockedUntil))
                action = null
            }
            paused -> {
                title = context.getString(R.string.widget_paused)
                detail = context.getString(R.string.widget_until, formatTime(pausedUntil))
                action = context.getString(R.string.widget_resume)
            }
            sessionId != null -> {
                title = context.getString(R.string.widget_focus)
                detail = sessionDetail(context)
                action = context.getString(R.string.widget_pause)
            }
            else -> {
                title = context.getString(R.string.widget_on)
                detail = context.getString(R.string.widget_on_detail)
                action = context.getString(R.string.widget_pause)
            }
        }

        val views = RemoteViews(context.packageName, R.layout.widget_sober_status)
        views.setTextViewText(R.id.widget_status, title)
        views.setTextViewText(R.id.widget_detail, detail)

        if (action == null) {
            views.setViewVisibility(R.id.widget_action, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_action, View.VISIBLE)
            views.setTextViewText(R.id.widget_action, action)
            views.setContentDescription(R.id.widget_action, action)
            views.setOnClickPendingIntent(
                R.id.widget_action,
                actionPendingIntent(context, if (paused) WidgetActions.RESUME else WidgetActions.PAUSE)
            )
        }
        views.setContentDescription(R.id.widget_root, "$title. $detail. ${action.orEmpty()}")

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launch != null) {
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE)
            )
        }
        return views
    }

    private suspend fun sessionDetail(context: Context): String {
        val label = sessionStateRepository.activeSessionLabel.first()
        val start = sessionStateRepository.activeSessionStartTime.first()
        val duration = sessionStateRepository.activeSessionDuration.first()
        val name = label ?: context.getString(R.string.widget_focus_default)
        return if (start == null || duration == null || duration <= 0L) {
            context.getString(R.string.widget_focus_open, name)
        } else {
            context.getString(R.string.widget_focus_until, name, formatTime(start + duration))
        }
    }

    private fun actionPendingIntent(context: Context, action: String): PendingIntent {
        val intent = android.content.Intent(context, SoberWidgetActionReceiver::class.java)
            .setAction(action)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatTime(epochMillis: Long?): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis ?: 0L))
}
