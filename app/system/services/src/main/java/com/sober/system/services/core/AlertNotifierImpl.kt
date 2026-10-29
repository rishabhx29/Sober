package com.sober.system.services.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.sober.core.domain.repository.AlertNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Posts one-shot behavioural alerts on a dedicated channel, so they can be
 * muted independently of the ongoing engine and daily-cap notifications.
 *
 * Each alert type gets its own notification id: sharing one would make the
 * second alert silently replace the first while the user is away.
 */
class AlertNotifierImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AlertNotifier {

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun notifyStreakWarning(cancelledToday: Int) {
        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Streak at risk")
            .setContentText("You've cancelled $cancelledToday focus sessions today.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(STREAK_WARNING_ID, notification)
    }

    override fun notifyStreakAtRisk(streakDays: Int) {
        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Your $streakDays-day streak ends tonight")
            .setContentText("A daily limit is used up, so today will not count. The streak resets at midnight.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "A daily limit is used up, so today will not count. The streak resets at midnight."
                )
            )
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(STREAK_AT_RISK_ID, notification)
    }

    override fun notifyDailyStreak(streakDays: Int) {
        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Day $streakDays streak")
            .setContentText("You're under every limit today. Keep it up until midnight.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        manager.notify(DAILY_STREAK_ID, notification)
    }

    override fun notifyBedtimeWarning(ruleName: String, minutesUntil: Int) {
        ensureChannel()

        val minutes = if (minutesUntil == 1) "1 minute" else "$minutesUntil minutes"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Bedtime in $minutes")
            .setContentText("$ruleName starts soon and blocks its apps.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(BEDTIME_WARNING_ID, notification)
    }

    override fun notifyUsageWarning(packageName: String?, usedMinutes: Int, limitMinutes: Int) {        ensureChannel()

        val remaining = (limitMinutes - usedMinutes).coerceAtLeast(0)
        val title = if (packageName == null) {
            "80% of your daily screen time"
        } else {
            "${appLabel(packageName)} is at ${percentOf(usedMinutes, limitMinutes)}%"
        }
        val text = if (packageName == null) {
            "${formatMinutes(usedMinutes)} of ${formatMinutes(limitMinutes)} used. ${formatMinutes(remaining)} left today."
        } else {
            "${formatMinutes(usedMinutes)} of ${formatMinutes(limitMinutes)} used. ${formatMinutes(remaining)} before it blocks."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // A distinct id per app so two apps can be warned about at once. The
        // masked hash is stable for a package, so re-firing replaces the old
        // alert instead of stacking a duplicate.
        val id = if (packageName == null) {
            OVERALL_WARNING_ID
        } else {
            APP_WARNING_ID_BASE + (packageName.hashCode() and 0x0FFF)
        }
        manager.notify(id, notification)
    }

    override fun notifySessionComplete(label: String, durationMinutes: Int) {
        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Session complete")
            .setContentText("$label · ${formatMinutes(durationMinutes)} of focus.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(SESSION_COMPLETE_ID, notification)
    }

    override fun notifyNotificationAccessRevoked() {
        ensureChannel()

        // A tap must lead somewhere actionable: the listener list is where the
        // user turns Sober's notification access back on.
        val settingsIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            settingsIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Notification blocking is off")
            .setContentText("Sober lost notification access. Tap to turn it back on.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ACCESS_ID, notification)
    }

    private fun appLabel(packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    private fun percentOf(used: Int, limit: Int): Int =
        if (limit <= 0) 100 else (used * 100 / limit).coerceIn(0, 100)

    private fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sober alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "One-off warnings, such as limits approaching or a finished focus session."
        }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "sober_alerts"

        const val STREAK_WARNING_ID = 1003
        const val OVERALL_WARNING_ID = 1004
        const val SESSION_COMPLETE_ID = 1005
        const val NOTIFICATION_ACCESS_ID = 1006
        const val STREAK_AT_RISK_ID = 1007
        const val DAILY_STREAK_ID = 1008
        const val BEDTIME_WARNING_ID = 1009

        /** False positives here would mean colliding alerts, so keep the range small. */
        const val APP_WARNING_ID_BASE = 2000
    }
}
