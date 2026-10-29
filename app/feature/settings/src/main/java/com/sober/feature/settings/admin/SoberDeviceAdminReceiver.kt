package com.sober.feature.settings.admin

import android.app.admin.DeviceAdminReceiver

/**
 * The device administrator Sober asks the user to activate.
 *
 * No callbacks are overridden on purpose: the admin exists for one manifest
 * consequence — an active administrator cannot be uninstalled until it is
 * deactivated — not to lock the screen or wipe data. Android gives no hook to
 * veto an uninstall, so the protection is "the system refuses while active",
 * and the user can always deactivate from Settings.
 *
 * Named without a "Dummy" prefix because it is the real component the manifest
 * declares; a placeholder name invites someone to replace it and break the
 * declared receiver.
 */
class SoberDeviceAdminReceiver : DeviceAdminReceiver()
