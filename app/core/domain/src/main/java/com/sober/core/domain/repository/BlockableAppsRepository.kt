package com.sober.core.domain.repository

/** An app the user can open and that Sober is allowed to cover. */
data class BlockableApp(
    val packageName: String,
    val label: String
)

/**
 * Decides which packages enforcement may cover.
 *
 * A daily cap applies to apps, not to the phone itself. Covering the keyboard,
 * the notification shade or Android Settings would make the device unusable and
 * could lock the user out of turning Sober off, so those packages are excluded
 * no matter what limit or rule names them.
 */
interface BlockableAppsRepository {

    /** True when [packageName] is a user-launchable app that is not device chrome. */
    fun isBlockable(packageName: String): Boolean

    /**
     * True for windows that appear on top of the current app without the user
     * leaving it: keyboards, system bars and permission dialogs. Enforcement
     * holds its current state for these instead of treating them as a switch.
     */
    fun isTransientChrome(packageName: String): Boolean

    /** Every blockable app, sorted by label. Used by the cap app picker. */
    fun blockableApps(): List<BlockableApp>
}
