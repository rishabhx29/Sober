pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Sober"
include(":app")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":core:common")
include(":feature:home")
include(":feature:apps")
include(":feature:focus")
include(":feature:stats")
include(":feature:settings")
include(":system:services")
include(":system:overlay")
include(":system:receivers")
