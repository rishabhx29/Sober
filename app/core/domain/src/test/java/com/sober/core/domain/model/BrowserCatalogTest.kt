package com.sober.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A wrong address bar id never crashes and never blocks the wrong screen — it
 * just means the site is not covered. These tests cannot prove an id matches
 * the app on a device; they pin the namespace rule and catch a package that
 * loses its entry in an edit.
 */
class BrowserCatalogTest {

    @Test
    fun `every address bar id belongs to its own package`() {
        BrowserCatalog.browsers.forEach { browser ->
            assertTrue(
                "${browser.packageName} points at ${browser.addressBarId}",
                browser.addressBarId.startsWith("${browser.packageName}:id/")
            )
        }
    }

    @Test
    fun `no browser is listed twice`() {
        val packages = BrowserCatalog.browsers.map { it.packageName }

        assertEquals(packages.size, packages.toSet().size)
    }

    @Test
    fun `the common browsers stay covered`() {
        val expected = setOf(
            "com.android.chrome",
            "com.brave.browser",
            "com.microsoft.emmx",
            "org.mozilla.firefox",
            "com.sec.android.app.sbrowser",
            "com.opera.browser",
            "com.duckduckgo.mobile.android"
        )

        assertTrue(
            "missing: ${expected - BrowserCatalog.packageNames}",
            BrowserCatalog.packageNames.containsAll(expected)
        )
    }

    @Test
    fun `lookups agree with the list`() {
        BrowserCatalog.browsers.forEach { browser ->
            assertTrue(BrowserCatalog.isBrowser(browser.packageName))
            assertEquals(
                browser.addressBarId,
                BrowserCatalog.addressBarIdFor(browser.packageName)
            )
        }

        assertFalse(BrowserCatalog.isBrowser("com.example.notabrowser"))
        assertNull(BrowserCatalog.addressBarIdFor("com.example.notabrowser"))
    }
}
