package com.sober.core.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.inputmethod.InputMethod
import com.sober.core.domain.repository.BlockableApp
import com.sober.core.domain.repository.BlockableAppsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A package is blockable when it is a real app the user launches from the
 * launcher, not a piece of the device's own furniture.
 *
 * The chrome set is resolved, not hard-coded, because keyboards and Settings
 * are OEM packages: Gboard on Pixel, a vendor IME elsewhere. Querying the
 * platform means a phone Sober has never seen still gets the right answer. The
 * literals below are the AOSP names plus a fallback for the case where the
 * intent query is empty.
 */
@Singleton
class BlockableAppsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BlockableAppsRepository {

    private val packageManager = context.packageManager

    override fun isBlockable(packageName: String): Boolean =
        packageName !in chromePackages() && hasLauncherEntry(packageName)

    override fun isTransientChrome(packageName: String): Boolean {
        if (packageName == SYSTEM_UI || packageName.startsWith("$SYSTEM_UI.")) return true
        if (packageName in inputMethodPackages()) return true
        return packageName in permissionControllerPackages()
    }

    override fun blockableApps(): List<BlockableApp> {
        val chrome = chromePackages()
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName !in chrome && hasLauncherEntry(it.packageName) }
            .map {
                BlockableApp(
                    packageName = it.packageName,
                    label = packageManager.getApplicationLabel(it).toString()
                )
            }
            .sortedBy { it.label.uppercase() }
            .toList()
    }

    /**
     * The launcher decides what "an app" means for enforcement. Without this
     * check the cap would cover anything that reports a foreground window,
     * including keyboards and system dialogs.
     */
    private fun hasLauncherEntry(packageName: String): Boolean =
        runCatching { packageManager.getLaunchIntentForPackage(packageName) != null }
            .getOrDefault(false)

    /** Home, keyboards, Settings, permission dialogs and Sober itself. */
    private fun chromePackages(): Set<String> = buildSet {
        add(context.packageName)
        addAll(homePackages())
        addAll(inputMethodPackages())
        addAll(settingsPackages())
        addAll(permissionControllerPackages())
        add(SYSTEM_UI)
    }

    private fun homePackages(): Set<String> =
        queryActivityPackages(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))

    private fun settingsPackages(): Set<String> =
        queryActivityPackages(Intent(Settings.ACTION_SETTINGS)) +
            queryActivityPackages(Intent(Settings.ACTION_APPLICATION_SETTINGS)) +
            // Fallbacks if the platform will not resolve the action for us.
            "com.android.settings"

    private fun inputMethodPackages(): Set<String> =
        runCatching {
            packageManager.queryIntentServices(Intent(InputMethod.SERVICE_INTERFACE), 0)
                .mapNotNull { it.serviceInfo?.packageName }
                .toSet()
        }.getOrDefault(emptySet())

    /**
     * The AOSP and Google packages. Permission dialogs are system UI, not apps
     * the user launches, so they can never be the target of a block anyway;
     * naming them keeps the intent explicit.
     */
    private fun permissionControllerPackages(): Set<String> = setOf(
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller"
    )

    private fun queryActivityPackages(intent: Intent): Set<String> =
        runCatching {
            packageManager.queryIntentActivities(intent, 0)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        }.getOrDefault(emptySet())

    private companion object {
        const val SYSTEM_UI = "com.android.systemui"
    }
}
