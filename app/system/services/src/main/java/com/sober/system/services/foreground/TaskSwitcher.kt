package com.sober.system.services.foreground

/**
 * True when a window transition belongs to the system task switcher.
 *
 * Recents is drawn by System UI on devices that do not hand it to the
 * launcher. It is chrome rather than an app switch, so the engine would
 * otherwise hold the current block over the switcher and the user would see
 * the block screen where Recents should be. Launcher-hosted switchers need no
 * special case: a launcher window already means "nothing to block".
 *
 * The package check is deliberately narrow. An ordinary app can legitimately
 * name an activity "RecentFilesActivity"; treating that as the switcher would
 * lift its block, so only System UI's own windows qualify.
 */
internal fun isSystemTaskSwitcher(packageName: String, className: String?): Boolean {
    if (className == null) return false
    if (packageName != SYSTEM_UI && !packageName.startsWith("$SYSTEM_UI.")) return false
    val name = className.lowercase()
    return "recents" in name || "overview" in name
}

private const val SYSTEM_UI = "com.android.systemui"