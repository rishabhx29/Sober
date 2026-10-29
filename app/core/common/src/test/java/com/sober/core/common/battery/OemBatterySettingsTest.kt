package com.sober.core.common.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OemBatterySettingsTest {

    @Test
    fun `xiaomi family resolves the autostart panel first`() {
        for (brand in listOf("Xiaomi", "redmi", "POCO")) {
            val panels = OemBatterySettings.panelsFor(brand)
            assertTrue("expected panels for $brand", panels.isNotEmpty())
            assertEquals("com.miui.securitycenter", panels.first().packageName)
            assertTrue(
                "autostart must come first so the user sees the switch that matters",
                panels.first().activityName.contains("autostart", ignoreCase = true)
            )
        }
    }

    @Test
    fun `samsung resolves its battery panel`() {
        val panels = OemBatterySettings.panelsFor("samsung")
        assertTrue(panels.any { it.packageName == "com.samsung.android.lool" })
        assertTrue(panels.isNotEmpty())
    }

    @Test
    fun `oppo family resolves a startup list`() {
        for (brand in listOf("oppo", "realme", "OnePlus")) {
            val panels = OemBatterySettings.panelsFor(brand)
            assertTrue("expected panels for $brand", panels.isNotEmpty())
            assertTrue(panels.any { it.activityName.contains("startup", ignoreCase = true) })
        }
    }

    @Test
    fun `huawei and vivo families have panels`() {
        assertTrue(OemBatterySettings.panelsFor("HUAWEI").isNotEmpty())
        assertTrue(OemBatterySettings.panelsFor("honor").isNotEmpty())
        assertTrue(OemBatterySettings.panelsFor("vivo").isNotEmpty())
    }

    @Test
    fun `unknown manufacturers get no guessed panel`() {
        assertTrue(OemBatterySettings.panelsFor("Google").isEmpty())
        assertTrue(OemBatterySettings.panelsFor(null).isEmpty())
        assertTrue(OemBatterySettings.panelsFor("").isEmpty())
        assertTrue(OemBatterySettings.panelsFor("   ").isEmpty())
    }

    @Test
    fun `matching ignores case and surrounding whitespace`() {
        val panels = OemBatterySettings.panelsFor("  SAMSUNG ")
        assertTrue(panels.isNotEmpty())
        assertEquals("com.samsung.android.lool", panels.first().packageName)
    }

    @Test
    fun `every panel names a package and an activity`() {
        for (brand in listOf("xiaomi", "samsung", "huawei", "oppo", "vivo")) {
            for (panel in OemBatterySettings.panelsFor(brand)) {
                assertTrue(panel.packageName.isNotBlank())
                assertTrue(panel.activityName.contains("."))
            }
        }
    }
}
