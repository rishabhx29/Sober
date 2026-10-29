plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.plugin)
    alias(libs.plugins.legacy.kapt)
}

android {
    namespace = "com.sober.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.sober.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Signing material comes from the release environment so no keystore or
    // password is ever committed. Without it the build still succeeds and
    // produces an unsigned APK, so a clean checkout can verify shrinking.
    val releaseKeystore = providers.environmentVariable("SOBER_KEYSTORE_PATH").orNull
    val releaseStorePassword = providers.environmentVariable("SOBER_KEYSTORE_PASSWORD").orNull
    val releaseKeyAlias = providers.environmentVariable("SOBER_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.environmentVariable("SOBER_KEY_PASSWORD").orNull
    val hasReleaseSigning = listOf(
        releaseKeystore,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword
    ).all { !it.isNullOrBlank() }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystore!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true
                keepRules {
                    includeDefault = true
                    files.add(file("proguard-rules.pro"))
                }
            }
            isShrinkResources = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Compose Navigation & Animation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.animation)

    // Hilt DI
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // DataStore (Preferences)
    implementation(libs.datastore.preferences)

    // Moshi (JSON Serialization)
    implementation(libs.moshi.kotlin)
    kapt(libs.moshi.kotlin.codegen)

    // WorkManager (Background Tasks)
    implementation(libs.work.runtime.ktx)

    // Vico (Charts)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // Coil (Image Loading)
    implementation(libs.coil.compose)

    // Accompanist (Permissions)
    implementation(libs.accompanist.permissions)

    // Lottie (Animations)
    implementation(libs.lottie.compose)
    
    // Project Modules
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:home"))
    implementation(project(":feature:apps"))
    implementation(project(":feature:focus"))
    implementation(project(":feature:stats"))
    implementation(project(":system:services"))
    implementation(project(":system:overlay"))
    implementation(project(":system:receivers"))

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
