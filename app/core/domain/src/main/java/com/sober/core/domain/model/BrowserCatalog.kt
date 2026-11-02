package com.sober.core.domain.model

/**
 * Browsers whose address bar is readable through the Accessibility framework.
 *
 * The id is the only place the URL is exposed as text, so a browser missing
 * from this list is not "allowed" — it is simply not detectable. Chrome's id is
 * verified; the Chromium forks and Firefox use the ids their builds ship with
 * and are included so a blocked domain does not silently work in one browser
 * and not another. A custom tab opens with the *host app's* package, so those
 * pages are outside this catalogue.
 */
object BrowserCatalog {

    data class Browser(
        val packageName: String,
        val addressBarId: String
    )

    val browsers: List<Browser> = listOf(
        Browser("com.android.chrome", "com.android.chrome:id/url_bar"),
        Browser("com.chrome.beta", "com.chrome.beta:id/url_bar"),
        Browser("com.chrome.dev", "com.chrome.dev:id/url_bar"),
        Browser("com.chrome.canary", "com.chrome.canary:id/url_bar"),
        Browser("com.brave.browser", "com.brave.browser:id/url_bar"),
        Browser("com.microsoft.emmx", "com.microsoft.emmx:id/url_bar"),
        Browser(
            "org.mozilla.firefox",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view"
        ),
        Browser(
            "com.sec.android.app.sbrowser",
            "com.sec.android.app.sbrowser:id/location_bar_edit_text"
        )
    )

    private val byPackage: Map<String, String> =
        browsers.associate { it.packageName to it.addressBarId }

    val packageNames: Set<String> get() = byPackage.keys

    fun isBrowser(packageName: String): Boolean = packageName in byPackage

    fun addressBarIdFor(packageName: String): String? = byPackage[packageName]
}
