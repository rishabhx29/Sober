package com.sober.core.common.battery

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Background-reliability helpers.
 *
 * A foreground service is still a process the system may stop: Doze on stock
 * Android, and much more aggressively on some vendor builds, can kill the
 * enforcement engine. The only durable fix is user-granted exemptions, so this
 * object reports the state and opens the right screen — it never asks for the
 * restricted `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission, which Play only
 * approves when exemption is essential to a core feature. Opening the settings
 * list keeps the decision with the user and needs no manifest permission.
 */
object BatteryOptimizationUtils {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** The system battery-optimisation list, common to every OEM. */
    fun getBatteryOptimizationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * The vendor's own background panel, resolved against what is actually
     * installed. Returns null on stock Android and on vendor builds whose
     * activity names differ — guessing would either crash or open the wrong
     * screen, so the caller hides the link instead.
     */
    fun getOemBatterySettingsIntent(context: Context): Intent? {
        for (panel in OemBatterySettings.panelsFor(Build.MANUFACTURER)) {
            val component = ComponentName(panel.packageName, panel.activityName)
            val exists = try {
                context.packageManager.getActivityInfo(component, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
            if (exists) {
                return Intent().setComponent(component).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        }
        return null
    }

    /** Display name for the vendor panel link, e.g. "Xiaomi". */
    fun manufacturerLabel(): String? {
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
        if (manufacturer.isEmpty() || manufacturer.equals("unknown", ignoreCase = true)) return null
        return manufacturer.replaceFirstChar { it.uppercase() }
    }
}
