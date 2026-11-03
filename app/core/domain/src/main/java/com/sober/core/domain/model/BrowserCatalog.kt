package com.sober.core.domain.model

/**
 * Browsers whose address bar is readable through the Accessibility framework.
 *
 * The address bar is the only place a URL appears as text, and it is read by
 * resource id. That makes coverage explicit rather than guessed:
 *
 * - Chromium and its forks keep the `url_bar` id in their own namespace.
 * - Firefox, Firefox Beta and Nightly (one codebase) share the Android
 *   Components toolbar id.
 * - Opera and Opera Mini use `url_field`, DuckDuckGo names its omnibox
 *   `omnibarTextInput`, and Samsung Internet ships its own location bar id.
 *
 * Resource ids are namespaced, so a browser that renames its toolbar simply
 * never matches: the feature fails open and never covers the wrong screen. A
 * browser missing from this list is not "allowed" — it is undetectable, and the
 * Apps screen tells the user so.
 *
 * Two surfaces are outside this catalogue by design. A Custom Tab opens with
 * the *host app's* package, and an in-app WebView never exposes a browsable
 * address bar, so neither can be read here; blocking the host app is the only
 * remedy for those.
 */
object BrowserCatalog {

    data class Browser(
        val packageName: String,
        val addressBarId: String
    )

    val browsers: List<Browser> = listOf(
        Browser("com.android.chrome", "com.android.chrome:id/url_bar"),
        Browser("org.chromium.chrome", "org.chromium.chrome:id/url_bar"),
        Browser("com.chrome.beta", "com.chrome.beta:id/url_bar"),
        Browser("com.chrome.dev", "com.chrome.dev:id/url_bar"),
        Browser("com.chrome.canary", "com.chrome.canary:id/url_bar"),
        Browser("com.brave.browser", "com.brave.browser:id/url_bar"),
        Browser("com.microsoft.emmx", "com.microsoft.emmx:id/url_bar"),
        Browser("com.vivaldi.browser", "com.vivaldi.browser:id/url_bar"),
        Browser("org.bromite.cromite", "org.bromite.cromite:id/url_bar"),
        Browser("us.spotco.mulch", "us.spotco.mulch:id/url_bar"),
        Browser("app.vanadium.browser", "app.vanadium.browser:id/url_bar"),
        Browser(
            "org.mozilla.firefox",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view"
        ),
        Browser(
            "org.mozilla.firefox_beta",
            "org.mozilla.firefox_beta:id/mozac_browser_toolbar_url_view"
        ),
        Browser(
            "org.mozilla.fenix.nightly",
            "org.mozilla.fenix.nightly:id/mozac_browser_toolbar_url_view"
        ),
        Browser("com.opera.browser", "com.opera.browser:id/url_field"),
        Browser("com.opera.mini.native", "com.opera.mini.native:id/url_field"),
        Browser(
            "com.duckduckgo.mobile.android",
            "com.duckduckgo.mobile.android:id/omnibarTextInput"
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
