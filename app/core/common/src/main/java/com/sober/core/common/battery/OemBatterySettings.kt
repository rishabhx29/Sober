package com.sober.core.common.battery

/**
 * A vendor settings screen where background limits live *in addition to*
 * Android's own battery-optimisation list.
 *
 * Xiaomi is the reason this exists: its "Autostart" switch is not the Doze
 * exemption, so a device can report `isIgnoringBatteryOptimizations == true`
 * and still kill a service after the screen turns off. Samsung's "Sleeping
 * apps" list behaves the same way. These are private, undocumented activities,
 * so they are only ever opened when the device actually resolves them.
 */
internal data class OemBatteryPanel(
    val packageName: String,
    val activityName: String
)

/**
 * Pure catalogue of known vendor panels. Kept free of Android types so the
 * matching can be unit-tested on the JVM; [BatteryOptimizationUtils] turns a
 * match into a `ComponentName`.
 */
internal object OemBatterySettings {

    /** Canonical manufacturer names used by [panelsFor]'s matching. */
    private val MULTI_BRAND_MANUFACTURERS = setOf("xiaomi", "redmi", "poco")

    fun panelsFor(manufacturer: String?): List<OemBatteryPanel> {
        val brand = manufacturer?.trim()?.lowercase() ?: return emptyList()

        if (brand in MULTI_BRAND_MANUFACTURERS) {
            return listOf(
                // MIUI/HyperOS: the only switch that survives a swipe-away.
                OemBatteryPanel(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                ),
                OemBatteryPanel(
                    "com.miui.securitycenter",
                    "com.miui.powercenter.PowerSettings"
                )
            )
        }

        return when (brand) {
            "samsung" -> listOf(
                OemBatteryPanel(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                ),
                OemBatteryPanel(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.battery.ui.BatteryActivity"
                )
            )
            "huawei", "honor" -> listOf(
                OemBatteryPanel(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            )
            "oppo", "realme", "oneplus" -> listOf(
                OemBatteryPanel(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                ),
                OemBatteryPanel(
                    "com.oplus.safecenter",
                    "com.oplus.safecenter.startupapp.StartupAppListActivity"
                )
            )
            "vivo", "iqoo" -> listOf(
                OemBatteryPanel(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            )
            else -> emptyList()
        }
    }
}
