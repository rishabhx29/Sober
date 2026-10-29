package com.sober.core.common.permissions

import android.Manifest
import android.app.AppOpsManager
import android.app.Application
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The permission probes read system state rather than our own code, so these are
 * the cases a plain JVM fake would get wrong: a setting that was never written,
 * a service list that names another app, and the API-33 split for
 * POST_NOTIFICATIONS.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PermissionUtilsTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    private fun writeAccessibilitySetting(enabled: Boolean, services: String?) {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            if (enabled) 1 else 0
        )
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            services
        )
    }

    @Test
    fun `accessibility is off when the flag was never written`() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            null
        )

        assertFalse(PermissionUtils.hasAccessibilityPermission(context))
    }

    @Test
    fun `accessibility is on when this package is listed and enabled`() {
        writeAccessibilitySetting(
            enabled = true,
            services = "com.other.app/com.other.app.Service:" +
                "${context.packageName}/com.sober.system.services.SoberAccessibilityService"
        )

        assertTrue(PermissionUtils.hasAccessibilityPermission(context))
    }

    @Test
    fun `another app's accessibility service does not count`() {
        writeAccessibilitySetting(
            enabled = true,
            services = "com.other.app/com.other.app.Service"
        )

        assertFalse(PermissionUtils.hasAccessibilityPermission(context))
    }

    @Test
    fun `the enabled flag without a service list stays off`() {
        writeAccessibilitySetting(enabled = true, services = null)

        assertFalse(PermissionUtils.hasAccessibilityPermission(context))
    }

    @Test
    fun `the service list without the enabled flag stays off`() {
        writeAccessibilitySetting(
            enabled = false,
            services = "${context.packageName}/com.sober.system.services.SoberAccessibilityService"
        )

        assertFalse(PermissionUtils.hasAccessibilityPermission(context))
    }

    @Test
    fun `usage access follows the app ops mode`() {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        shadowOf(appOps).setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
            AppOpsManager.MODE_DEFAULT
        )
        assertFalse(PermissionUtils.hasUsageStatsPermission(context))

        shadowOf(appOps).setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
            AppOpsManager.MODE_ALLOWED
        )
        assertTrue(PermissionUtils.hasUsageStatsPermission(context))
    }

    @Test
    @Config(sdk = [32])
    fun `post notifications is implicitly granted before android 13`() {
        assertTrue(PermissionUtils.hasPostNotificationsPermission(context))
    }

    @Test
    fun `post notifications is denied until the user grants it`() {
        assertFalse(PermissionUtils.hasPostNotificationsPermission(context))

        shadowOf(context as Application)
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertTrue(PermissionUtils.hasPostNotificationsPermission(context))
    }

    @Test
    fun `overlay intent targets this package`() {
        val intent = PermissionUtils.getOverlayIntent(context)

        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, intent.action)
        assertEquals("package:${context.packageName}", intent.data.toString())
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `device admin intent carries the receiver component`() {
        val intent = PermissionUtils.getDeviceAdminIntent(context, DeviceAdminReceiver::class.java)

        assertEquals(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN, intent.action)
        assertEquals(
            ComponentName(context, DeviceAdminReceiver::class.java),
            intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN)
        )
        assertNotNull(intent.getStringExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION))
    }

    @Test
    fun `notification settings intent names this package`() {
        val intent = PermissionUtils.getAppNotificationSettingsIntent(context)

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(
            context.packageName,
            intent.getStringExtra(Settings.EXTRA_APP_PACKAGE)
        )
    }
}
