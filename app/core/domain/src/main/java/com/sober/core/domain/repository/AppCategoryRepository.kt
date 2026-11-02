package com.sober.core.domain.repository

import com.sober.core.domain.model.AppCategory

/**
 * Classifies installed apps into the coarse categories the product talks about.
 *
 * This is an interface so achievement and policy rules stay in the domain and
 * JVM-testable; the Android implementation resolves categories through
 * `PackageManager`, falling back to the bundled social package list when an
 * app declares no category at all.
 */
interface AppCategoryRepository {
    /**
     * Packages the platform (or a known-package list) labels as social media.
     * Empty when nothing social is installed.
     */
    fun getSocialPackages(): Set<String>

    /** The bucket [packageName] belongs to; OTHER when the platform is silent. */
    fun getCategory(packageName: String): AppCategory

    /** Every installed, user-launchable package in [category]. */
    fun getPackagesInCategory(category: AppCategory): Set<String>
}
