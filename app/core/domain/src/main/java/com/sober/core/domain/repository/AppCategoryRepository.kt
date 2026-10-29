package com.sober.core.domain.repository

/**
 * Classifies installed apps into the coarse categories the product talks about.
 *
 * This is an interface so achievement rules stay in the domain and JVM-testable;
 * the Android implementation resolves categories through `PackageManager`.
 */
interface AppCategoryRepository {
    /**
     * Packages the platform (or a known-package list) labels as social media.
     * Empty when nothing social is installed.
     */
    fun getSocialPackages(): Set<String>
}
