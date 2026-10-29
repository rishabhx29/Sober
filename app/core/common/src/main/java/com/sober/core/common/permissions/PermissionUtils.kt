package com.sober.core.common.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.app.admin.DevicePolicyManager
import android.content.ComponentName

object PermissionUtils {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        // unsafeCheckOpNoThrow only exists from API 29; minSdk is 26, and calling
        // it unconditionally is a NoSuchMethodError on the phones that need this
        // check the most. checkOpNoThrow carries the same meaning but throws
        // SecurityException for foreign packages, which cannot happen here.
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasAccessibilityPermission(context: Context): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            // Error handling
        }
        
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                return settingValue.contains(context.packageName)
            }
        }
        return false
    }

    fun hasNotificationListenerPermission(context: Context): Boolean {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledListeners.contains(context.packageName)
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * POST_NOTIFICATIONS only exists from API 33. Below that the permission is
     * granted at install time, so the check must report true rather than
     * blocking onboarding on a permission that cannot be requested.
     */
    fun hasPostNotificationsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasDeviceAdminPermission(context: Context, adminReceiver: Class<*>): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ComponentName(context, adminReceiver))
    }

    // Intents for launching settings
    
    fun getUsageStatsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    
    fun getAccessibilityIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    
    fun getNotificationListenerIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    
    fun getOverlayIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    
    fun getDeviceAdminIntent(context: Context, adminReceiver: Class<*>): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(context, adminReceiver))
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Device Admin is required to prevent circumvention of app blocks.")
        }
    }

    /**
     * The system's own deactivation screen. Sober never deactivates the admin
     * itself: the permission belongs to the user, and taking it away silently
     * would leave them unable to explain why their phone changed.
     *
     * The `ACTION_REMOVE_DEVICE_ADMIN` constant no longer exists in the current
     * platform stubs, so the documented action string is used directly. Callers
     * must be ready for it not to resolve and fall back to
     * [getDeviceAdminSettingsIntent].
     */
    fun getRemoveDeviceAdminIntent(context: Context, adminReceiver: Class<*>): Intent {
        return Intent(LEGACY_ACTION_REMOVE_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(context, adminReceiver))
        }
    }

    /** Security settings, where device administrators are listed on every OEM. */
    fun getDeviceAdminSettingsIntent(): Intent {
        return Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    private const val LEGACY_ACTION_REMOVE_DEVICE_ADMIN = "android.app.action.REMOVE_DEVICE_ADMIN"

    /**
     * App notification settings, so a user who denied the runtime prompt can
     * still turn notifications on — the system dialog will not reappear after
     * a permanent denial.
     */
    fun getAppNotificationSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
