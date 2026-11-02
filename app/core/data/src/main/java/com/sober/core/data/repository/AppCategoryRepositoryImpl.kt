package com.sober.core.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.sober.core.common.presets.PresetResolver
import com.sober.core.domain.model.AppCategory
import com.sober.core.domain.repository.AppCategoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Categories come from the platform, not from a hard-coded package list.
 *
 * `ApplicationInfo.category` is set by the app itself (Play requires it for
 * most new listings), so it stays correct across OEMs and apps Sober has never
 * seen. The bundled social list covers the common case where a popular app
 * declares CATEGORY_UNDEFINED.
 */
class AppCategoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppCategoryRepository {

    private val packageManager = context.packageManager

    override fun getSocialPackages(): Set<String> =
        PresetResolver.socialPackages(context) + getPackagesInCategory(AppCategory.SOCIAL)

    override fun getCategory(packageName: String): AppCategory {
        val platform = runCatching {
            packageManager.getApplicationInfo(packageName, 0).category
        }.getOrDefault(ApplicationInfo.CATEGORY_UNDEFINED)

        return when (platform) {
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
            else -> if (packageName in PresetResolver.socialPackages(context)) {
                AppCategory.SOCIAL
            } else {
                AppCategory.OTHER
            }
        }
    }

    override fun getPackagesInCategory(category: AppCategory): Set<String> =
        runCatching {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
                .filter { platformCategoryOf(it) == category }
                .map { it.packageName }
                .toSet()
        }.getOrDefault(emptySet())

    private fun platformCategoryOf(info: ApplicationInfo): AppCategory = when (info.category) {
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
        ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
        ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
        ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
        else -> if (info.packageName in PresetResolver.socialPackages(context)) {
            AppCategory.SOCIAL
        } else {
            AppCategory.OTHER
        }
    }
}
