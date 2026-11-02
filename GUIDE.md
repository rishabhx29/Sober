# Sober — Development Guide

> **Maintained by**: Guide Agent  
> **Last Updated**: 2026-08-19  
> **Purpose**: Living document. Step-by-step instructions for every task — from environment setup to feature coding. This file is **append-only** for new learnings; existing sections are updated in-place when corrections are needed.

---

## Table of Contents

1. [How This Guide Works](#1-how-this-guide-works)
2. [Environment Setup](#2-environment-setup)
3. [Project Initialization](#3-project-initialization)
4. [Architecture Overview](#4-architecture-overview)
5. [Design System Setup](#5-design-system-setup)
6. [Data Layer Setup](#6-data-layer-setup)
7. [System Services](#7-system-services)
8. [Feature Development Workflow](#8-feature-development-workflow)
9. [Testing Strategy](#9-testing-strategy)
10. [Agent Workflow](#10-agent-workflow)
11. [Lessons Learned](#11-lessons-learned)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. How This Guide Works

This guide follows a **progressive disclosure** pattern — each section builds on the previous one. When you're working on a task from `TASKS.md`, find the corresponding section here for step-by-step instructions.

**Guide Rules**:
- **New learnings** (gotchas, workarounds, best practices discovered during development) are appended to Section 11 "Lessons Learned" by the Guide Agent.
- **Corrections** to existing instructions are edited in-place with a `[UPDATED: date]` marker.
- **New features** get their own sub-section appended to the relevant section.

---

## 2. Environment Setup

### 2.1 Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Latest Stable (Ladybug+) | [developer.android.com](https://developer.android.com/studio) |
| JDK | 17+ | Bundled with Android Studio |
| Android SDK | API 35 (Android 15) | SDK Manager in Android Studio |
| Git | Any recent | [git-scm.com](https://git-scm.com) |
| Emulator | Pixel 7, API 35 | AVD Manager |

### 2.2 Android Studio Setup

1. Install Android Studio — use the default installation wizard
2. Open **SDK Manager** → SDK Platforms → install **Android 15 (API 35)**
3. SDK Tools → ensure these are installed:
   - Android SDK Build-Tools 35.0.0
   - Android SDK Command-line Tools
   - Android Emulator
   - Android SDK Platform-Tools
   - Google USB Driver (if using Windows)
4. Create an emulator: **Pixel 7**, **API 35**, **x86_64**
5. (Optional) Enable **Developer Options** and **USB Debugging** on your physical Android device to test locally.

### 2.3 Environment Variables (Windows)

```powershell
# Verify ANDROID_HOME is set
[System.Environment]::GetEnvironmentVariable("ANDROID_HOME", "User")
# If not set:
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")
```

### 2.4 Git Setup

```powershell
cd c:\Rishabh\Sober
git init
git add .
git commit -m "Initial project structure"
```

---

## 3. Project Initialization (Phase 0 Detailed Guide)

Phase 0 sets up the foundational environment and architecture for Sober. It involves manual project creation, dependency definition, and critical module scaffolding.

### 3.1 Initial Scaffold
1. Open Android Studio → **New Project** → **Empty Activity (Compose)**.
   - **Name**: Sober
   - **Package**: `com.sober.app`
   - **Min SDK**: API 26 (Android 8.0) — *Chosen to support modern background service requirements without legacy bloat.*
   - **Build Configuration**: Kotlin DSL (`build.gradle.kts`)
2. Once the project generates, clean out the default `MainActivity` and `ui/theme` code to start fresh.

### 3.2 Modular Architecture Setup
To maintain Clean Architecture, we created 12 Gradle modules. 
1. In Android Studio, go to **File** → **New** → **New Module** → **Android Library**.
2. We created the following modules:
   - **Domain & Data**: `:core:domain`, `:core:data`
   - **UI & Common**: `:core:ui`, `:core:common`
   - **Features**: `:feature:home`, `:feature:apps`, `:feature:focus`, `:feature:stats`, `:feature:settings`
   - **System Integration**: `:system:services`, `:system:overlay`, `:system:receivers`
3. We then verified that all of these are correctly registered in `settings.gradle.kts`.

### 3.3 The Version Catalog (`libs.versions.toml`)
Instead of hardcoding dependency versions across 12 different modules, we use a single `libs.versions.toml` file.
1. We populated `gradle/libs.versions.toml` with the core libraries we need:
   - **Hilt** (Dependency Injection)
   - **Room** (Local Database)
   - **Compose** (Material 3, Navigation, Animation)
   - **DataStore** (Preferences), **Moshi** (JSON), **WorkManager** (Background tasks), **Vico** (Charts), **Coil** (Images), **Accompanist** (Permissions), **Lottie** (Animations).

### 3.4 App Module Configuration
In `app/build.gradle.kts`, we applied the dependencies and plugins.
*Key Learning:* We had to apply the Kotlin Android plugin (`org.jetbrains.kotlin.android`) explicitly because the Compose wizard sometimes omits it, which breaks Hilt's Android injection binding.

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.plugin)
    alias(libs.plugins.kotlin.kapt)
}
```

### 3.5 Resolving the KSP / AGP 9.3 Conflict
During Phase 0, we hit a build error: `Plugin [id: 'com.google.devtools.ksp'] was not found`.
**Why it happened**: KSP is strictly bound to the exact Kotlin compiler version. AGP 9.3+ bundles its own Kotlin compiler, making it very difficult to find an exact matching KSP plugin version.
**The Fix**: 
1. We removed KSP entirely.
2. We switched Room to use **KAPT** (`kapt(libs.room.compiler)`), which is built directly into Kotlin and avoids plugin version mismatches.
3. Upgraded Hilt to 2.60.1 and migrated `kapt` to use `com.android.legacy-kapt`.

### 3.6 Hilt Application Setup
Hilt needs a root `Application` class to generate the dependency graph.
1. Created `SoberApplication.kt` annotated with `@HiltAndroidApp`.
2. Registered it in `AndroidManifest.xml` via `android:name=".SoberApplication"`.

### 3.7 Adding Custom Fonts
1. Downloaded **Inter** and **JetBrains Mono** variable `.ttf` files from Google Fonts.
2. Created the `app/src/main/res/font/` directory.
3. Placed `inter.ttf` and `jetbrains_mono.ttf` inside. (Android requires all lowercase with underscores).

### 3.8 Phase 0 Verification
We clicked **Sync Project with Gradle Files** and ran the app on a physical Android device. A successful build with zero errors confirms Phase 0 is complete and our environment is perfectly stable.

---

## 4. Architecture Overview

### 4.1 Clean Architecture Layers

```
┌─────────────────────────────────────────┐
│              Presentation               │
│  (Compose UI, ViewModels, Navigation)   │
│  Modules: :feature:*, :system:overlay   │
├─────────────────────────────────────────┤
│                Domain                   │
│  (Use Cases, Models, Repo Interfaces)   │
│  Module: :core:domain                   │
├─────────────────────────────────────────┤
│                 Data                    │
│  (Room DB, DataStore, Repo Impls,       │
│   UsageStatsManager wrapper)            │
│  Module: :core:data                     │
└─────────────────────────────────────────┘
```

### 4.2 Data Flow

```
User Action → ViewModel → Use Case → Repository Interface
                                          ↓
                              Repository Implementation
                                    ↓          ↓
                              Room DB    UsageStatsManager
```

### 4.3 Module Dependency Rules

- `:feature:*` modules can depend on `:core:domain` and `:core:ui` — **never** on `:core:data`
- `:core:data` implements interfaces defined in `:core:domain`
- `:system:*` modules can depend on `:core:domain` and `:core:data`
- `:core:ui` is a leaf module — depends only on `:core:common`
- No circular dependencies ever

---

## 5. Design System Setup

### 5.1 Color Palette (`core:ui`)

Create `com/sober/core/ui/theme/Color.kt`:

```kotlin
package com.sober.core.ui.theme

import androidx.compose.ui.graphics.Color

// Manuscript Palette
val SoberBackground = Color(0xFFF7F4EF)      // Warm cream
val SoberSurface = Color(0xFFFFFFFF)          // Pure white (cards)
val SoberCardBorder = Color(0xFFE8E0D5)       // Warm border
val SoberPrimary = Color(0xFF1A1A2E)          // Ink Navy
val SoberPrimaryMuted = Color(0xFFEDEAE4)     // Cream tint
val SoberDanger = Color(0xFFC0392B)           // Signal Red
val SoberDangerLight = Color(0xFFF9ECEB)      // Red tint bg
val SoberWarning = Color(0xFF8B6F47)          // Warm Brown
val SoberWarningLight = Color(0xFFF5EFE6)     // Warning bg
val SoberTextPrimary = Color(0xFF1A1A1A)      // Body copy
val SoberTextSecondary = Color(0xFF6B6560)    // Supporting text
val SoberTextMuted = Color(0xFF9C9490)        // Placeholders
val SoberDivider = Color(0xFFE8E0D5)          // Dividers
```

### 5.2 Typography (`core:ui`)

Create `com/sober/core/ui/theme/Type.kt`:

```kotlin
package com.sober.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
)

val SoberTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        color = SoberTextPrimary,
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        color = SoberTextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        color = SoberTextPrimary,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = SoberTextPrimary,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = SoberTextSecondary,
    ),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = SoberPrimary,
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = SoberTextMuted,
        letterSpacing = 1.sp,
    ),
)
```

### 5.3 Adding Font Files

1. Download [Inter](https://fonts.google.com/specimen/Inter) and [JetBrains Mono](https://fonts.google.com/specimen/JetBrains+Mono) from Google Fonts
2. Place `.ttf` files in `core/ui/src/main/res/font/`:
   - `inter_regular.ttf`
   - `inter_medium.ttf`
   - `inter_semibold.ttf`
   - `jetbrains_mono_regular.ttf`

---

## 6. Data Layer Setup

### 6.1 Room Database

Create `SoberDatabase.kt` in `:core:data`:

```kotlin
@Database(
    entities = [
        AppLimitEntity::class,
        UsageSessionEntity::class,
        FocusSessionEntity::class,
        ScheduleRuleEntity::class,
        OverrideLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SoberDatabase : RoomDatabase() {
    abstract fun appLimitDao(): AppLimitDao
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao
    abstract fun overrideLogDao(): OverrideLogDao
}
```

### 6.2 Type Converters

For JSON columns (blockedSections, blockedApps, etc.), use Moshi:

```kotlin
class Converters {
    private val moshi = Moshi.Builder().build()
    private val listAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        listAdapter.toJson(value ?: emptyList())

    @TypeConverter
    fun toStringList(value: String): List<String> =
        listAdapter.fromJson(value) ?: emptyList()
}
```

### 6.3 Entity Examples

```kotlin
@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val isActive: Boolean = true,
    val blockedSections: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)
```

### 6.4 DataStore Setup

```kotlin
val Context.soberDataStore by preferencesDataStore(name = "sober_preferences")

object PreferenceKeys {
    val OVERALL_DAILY_LIMIT = intPreferencesKey("overall_daily_limit")
    val PIN_HASH = stringPreferencesKey("pin_hash")
    val THEME = stringPreferencesKey("theme")
    val STREAK_CURRENT = intPreferencesKey("streak_current")
    val STREAK_LONGEST = intPreferencesKey("streak_longest")
    val STREAK_LAST_SUCCESS = longPreferencesKey("streak_last_success")
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
}
```

---

## 7. System Services

### 7.1 Foreground Service

The `SoberCoreService` is the heart of the app. Key implementation notes:

1. **Notification Channel** — create a dedicated channel for the persistent notification
2. **Polling Interval** — poll UsageStats every 30 seconds (balance between accuracy and battery)
3. **Midnight Reset** — schedule via AlarmManager for exact midnight trigger
4. **START_STICKY** — return this from `onStartCommand` so the system restarts the service if killed

```kotlin
class SoberCoreService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        startUsagePolling()
        return START_STICKY
    }
}
```

### 7.2 Accessibility Service

The `SoberAccessibilityService` is critical for section blocking (Phase 3). Configuration in `res/xml/accessibility_service_config.xml`:

```xml
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canRetrieveWindowContent="true"
    android:accessibilityFlags="flagReportViewIds|flagIncludeNotImportantViews"
    android:settingsActivity="com.sober.app.ui.settings.SettingsActivity"
    android:description="@string/accessibility_description" />
```

### 7.3 Notification Listener

`SoberNotificationService` intercepts notifications during Focus Sessions:

```kotlin
class SoberNotificationService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isInFocusSession && sbn.packageName in blockedApps) {
            cancelNotification(sbn.key)
        }
    }
}
```

### 7.4 Manifest Declarations

All services must be declared in `AndroidManifest.xml`:

```xml
<!-- Foreground Service -->
<service
    android:name=".system.services.SoberCoreService"
    android:foregroundServiceType="specialUse"
    android:exported="false" />

<!-- Accessibility Service -->
<service
    android:name=".system.services.SoberAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>

<!-- Notification Listener -->
<service
    android:name=".system.services.SoberNotificationService"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>

<!-- Boot Receiver -->
<receiver
    android:name=".system.receivers.SoberBootReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

---

## 8. Feature Development Workflow

### 8.1 Standard Workflow for Any Feature

Every feature follows this exact workflow:

```
1. Check TASKS.md → find the task
2. Read this GUIDE for step-by-step instructions
3. Create/edit files in the correct module
4. Write unit tests (JUnit 5 + Mockk)
5. Run tests locally
6. Build the project (./gradlew assembleDebug)
7. Test on emulator
8. QA Agent verifies
9. Task marked [x] in TASKS.md
```

### 8.2 Adding a New Screen

1. Create the Screen composable in the feature module (e.g., `feature:home`)
2. Create the ViewModel in the same module
3. Define any new use cases in `:core:domain`
4. Implement the data access in `:core:data`
5. Add navigation route in `:app` navigation graph
6. Wire up Hilt injection

### 8.3 Adding a New Entity

1. Define the domain model in `:core:domain`
2. Create the Room Entity in `:core:data`
3. Create the DAO in `:core:data`
4. Add the entity to `SoberDatabase`
5. Create the repository interface in `:core:domain`
6. Implement the repository in `:core:data`
7. Create use cases in `:core:domain`
8. Increment database version + write migration

---

## 9. Testing Strategy

### 9.1 Unit Tests

```kotlin
// Example: Testing a use case
@ExtendWith(MockKExtension::class)
class CheckLimitReachedUseCaseTest {
    @MockK lateinit var usageRepo: UsageRepository
    @MockK lateinit var limitRepo: AppLimitRepository

    private lateinit var useCase: CheckLimitReachedUseCase

    @BeforeEach
    fun setup() {
        useCase = CheckLimitReachedUseCase(usageRepo, limitRepo)
    }

    @Test
    fun `returns true when usage exceeds limit`() = runTest {
        coEvery { usageRepo.getUsageForToday("com.instagram.android") } returns 31
        coEvery { limitRepo.getLimit("com.instagram.android") } returns AppLimit(dailyLimitMinutes = 30)

        val result = useCase("com.instagram.android")

        assertTrue(result)
    }
}
```

### 9.2 Running Tests

```powershell
# Unit tests
.\gradlew test

# Compose UI tests (requires emulator or connected physical device)
.\gradlew connectedAndroidTest

# Specific module
.\gradlew :core:domain:test
```

### 9.3 Running on a Physical Device

Since we are actively testing on real devices during development instead of relying solely on the emulator:

1. Connect your Android phone via USB (ensure USB Debugging is on).
2. Authorize the PC connection on your phone's screen.
3. Build and install directly to the device:
```powershell
.\gradlew installDebug
```
4. If you prefer to generate an APK to share via GitHub instead of direct install:
```powershell
.\gradlew assembleDebug
# The APK will be generated at app/build/outputs/apk/debug/app-debug.apk
```

---

## 10. Agent Workflow

### 10.1 Agent Roles

See `AGENTS.md` for full agent definitions. Summary:

| Agent | Role |
|-------|------|
| Orchestrator | Decomposes tasks, delegates, verifies |
| Implementation Agent | Writes code for assigned tasks |
| QA Agent | Reviews code, runs tests, verifies quality |
| Guide Agent | Updates this guide with new learnings |
| Task Tracker Agent | Maintains TASKS.md status |

### 10.2 Workflow Sequence

```
1. Orchestrator picks next task from TASKS.md
2. Implementation Agent writes code
3. QA Agent reviews + runs tests
4. Guide Agent updates this document if new patterns learned
5. Task Tracker Agent marks task [x]
6. Orchestrator picks next task
```

---

## 11. Lessons Learned

> This section is **append-only**. Every time we discover a gotcha, workaround, or best practice, the Guide Agent adds it here.

### 11.1 AGP 9.3, Kotlin 2.2, and Room Metadata

**Date**: 2026-08-20  
**Context**: Initializing Phase 0 dependencies and configuring `:core:data`.  
**Learning**: AGP 9.3+ bundles its own Kotlin compiler, and when paired with `compose-bom:2026.02.01`, the project requires Kotlin version `2.2.10`. However, earlier versions of Room (like `2.6.1`) use an older `kotlinx-metadata-jvm` library that only supports up to Kotlin 2.0 metadata, which causes `kapt` to crash with `Provided Metadata instance has version 2.2.0, while maximum supported version is 2.0.0`.
**Solution/Pattern**: 
1. Use Room version `2.8.4` (or newer) which is compatible with Kotlin 2.2 metadata.
2. Maintain `kotlin = "2.2.10"` in `libs.versions.toml` to support the newer Compose BOM.
3. Remove redundant `kotlin-android` plugin applications and use `com.android.legacy-kapt` for `kapt` support.

---

### 11.2 Foreground Service Limits and Boot Restrictions

**Date**: 2026-10-16  
**Context**: QA review of `SoberCoreService` and the Android manifest.  
**Learning**: For apps targeting Android 15 (API 35) or later, Android limits an app's `dataSync` foreground services to six hours total in a 24-hour period, and prevents `BOOT_COMPLETED` receivers from starting several foreground service types, including `dataSync`. These policies exist because foreground services are intended for user-noticeable work with a bounded purpose; a perpetual `dataSync` watchdog can exhaust its runtime allowance and fail to restart at boot. Declaring `specialUse` does not bypass the policy: it needs a meaningful subtype rationale in the service manifest, and Google Play reviews the declared use case.  
**Solution/Pattern**: Do not use `dataSync` as a perpetual watchdog. Start a foreground service only for qualifying user-visible work and stop it when that work ends; use an API appropriate to the actual background work. Use `specialUse` only when no other foreground service type fits, and provide a truthful, specific manifest rationale that can withstand Play review. See [Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15#changes-to-foreground-services) and [foreground service types: special use](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use).

[UPDATED: 2026-10-29] `SoberCoreService` now starts with `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` on API 34+ and keeps `dataSync` only for API 29–33. The manifest declares both types plus a `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` rationale, so the six-hour daily cap no longer applies on modern devices. `FOREGROUND_SERVICE_SPECIAL_USE` was added to the permissions. This still requires a Play Store justification at submission time; it is not a way to avoid the review.

### 11.3 UsageStats Timestamps Do Not Identify the Current Foreground App

**Date**: 2026-10-16  
**Context**: QA review of app foreground detection and usage polling.  
**Learning**: `UsageStats.lastTimeUsed` reports when an app activity was last used; it is historical usage data, so the most recent timestamp does not prove that the app is still foreground now. Polling that value can therefore leave Sober acting on a stale app after the user has switched away.  
**Solution/Pattern**: Use recent `UsageEvents` activity transitions (`ACTIVITY_RESUMED` and `ACTIVITY_PAUSED`) to infer foreground changes from event history, or use accessibility window-state events when the user has enabled the accessibility service and that event stream is appropriate. Treat polling and event delivery as state signals with lifecycle and stale-data handling, rather than treating `lastTimeUsed` as authoritative live state. See [UsageStats](https://developer.android.com/reference/android/app/usage/UsageStats#getLastTimeUsed()), [UsageEvents.Event](https://developer.android.com/reference/android/app/usage/UsageEvents.Event#ACTIVITY_RESUMED), and [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService).

### 11.4 Cold Gradle Builds Can Look Idle During Artifact Resolution

**Date**: 2026-10-16  
**Context**: Phase 0 cold `assembleDebug test` build.  
**Learning**: The first cold build spent about 18 minutes downloading and transforming Gradle, AGP, Kotlin, and lint artifacts, while non-verbose output appeared silent at task-graph calculation. `help --info` succeeded; the full build then passed with `--no-configuration-cache --info`, and the default warm-cache build completed in about 20 seconds. This points to cold artifact resolution rather than a persistent configuration-cache hang.  
**Solution/Pattern**: Use `--info` and inspect file or log progress to distinguish active downloads/transforms from an idle build. Let the first cold run finish, then retry the normal command with the populated cache; do not disable configuration cache based on this symptom alone.

### 11.5 Section Detection Depends on Two Service Flags and One Good Signal

**Date**: 2026-10-30  
**Context**: Phase 3 — blocking Reels/Shorts while leaving the rest of an app usable.  
**Learning**: Three things must line up before `AccessibilityNodeInfo.viewIdResourceName` is worth anything. First, the service needs `FLAG_REPORT_VIEW_IDS`; without it every id is null and resource-id rules silently never match. Second, the config XML has to declare the event types the code relies on (`typeWindowStateChanged` and `typeWindowContentChanged`). Third, "the tab's icon exists" is not the same as "the tab is open" — most apps keep every bottom-nav label in the tree at all times, so presence-based matching blocks the whole app. The reliable signal is `node.isSelected` on the tab, which is why the catalog gives `SELECTED_TAB` its own detection type; presence, text and activity rules exist only for screens without a selected-state tab.  
**Solution/Pattern**: Enable `FLAG_REPORT_VIEW_IDS`, set event types in `onServiceConnected`, throttle content events (they arrive in bursts while a feed scrolls), always re-check `rootInActiveWindow?.packageName` before reading a tree, and prefer selected-state over presence. Keep the matcher a pure function over a flattened snapshot so rules stay testable without a device; the tree walk is then the only untestable part.

### 11.6 A Blocker App Cannot Fetch Its Own Rules

**Date**: 2026-10-30  
**Context**: Phase 3.2 asked for remote, versioned detection rules.  
**Learning**: The app ships without `INTERNET` and its privacy policy promises nothing leaves the device, so a remote rules fetch is not a small addition — it reverses a product promise. Shipping the catalog as a versioned JSON asset keeps parsing identical to a future fetch (same parser, same schema, same version gate) while the last-good copy in DataStore covers a damaged asset. The version field is what makes the remote path additive later rather than a rewrite.  
**Solution/Pattern**: Model updateable data as a versioned document even when it ships locally. If a fetch is ever added, only the source changes; parsing, validation, fallback and the version gate stay as they are.

### 11.7 A Service Cannot Re-Enable Itself

**Date**: 2026-10-30  
**Context**: Phase 5.2 asked Sober to re-open the Accessibility toggle when the user goes to Settings to disable it.  
**Learning**: An `AccessibilityService` can observe other windows and post an overlay, but it cannot grant itself the accessibility permission: enabling a service is a user action the platform reserves for the Settings UI. Driving the switch with `performAction(ACTION_CLICK)` on a Settings node is the "self-granting" pattern that Play policy treats as abuse. What is implementable is a speed bump — cover the Accessibility screen while a block is in force and explain the consequence, which is what `AccessibilitySettingsGuard` does. Two facts ride on top of that: an overlay is only drawable while "Display over other apps" is granted, and the settings screen must be recognised, which is why detection trusts the non-localised activity class name first and only falls back to text.  
**Solution/Pattern**: When a PRD requirement conflicts with a platform rule, implement the closest honest behaviour and record the divergence in `TASKS.md` next to the checkbox. Gate the behaviour on a real state read (`IsBlockingInForceUseCase`), never on "the service is running", so the speed bump disappears the moment nothing is blocked.

### 11.8 Battery Exemption Is Not Autostart

**Date**: 2026-10-30  
**Context**: Phase 5.6 asked for a battery-optimisation whitelist prompt in onboarding, including Xiaomi and Samsung fixes.  
**Learning**: There are at least three separate switches behind "the app keeps getting killed". Doze/battery optimisation (`PowerManager.isIgnoringBatteryOptimizations`) is the one the public API exposes and it is readable. Vendor *autostart* (MIUI/HyperOS) and *sleeping apps* (Samsung) are private activities: they cannot be read, only opened, and their class names change between builds. That is why `BatteryOptimizationUtils` resolves a candidate list with `getActivityInfo` and returns null instead of guessing — opening the wrong settings screen is worse than hiding the link. It also does not request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: the list screen needs no permission at all, and the direct dialog needs a restricted permission Play grants only when exemption is core to the app.  
**Solution/Pattern**: For vendor-specific settings, keep the catalogue of package/activity pairs as a pure data object (unit-testable on the JVM) and do the Android resolution in one thin wrapper. Show the state you can read, offer the screens you can open, and let the user proceed when the outcome cannot be verified.

### 11.9 Release Builds Are a Different App

**Date**: 2026-10-30  
**Context**: Phase 5.7 turned on R8 for the first time; every build so far had been debug.  
**Learning**: A release build is not "the debug build, smaller". R8 renames classes, strips members it believes are unused, and rewrites patterns such as enums into integer constants. Sober stores enum names as strings in DataStore and reads them back with `valueOf`, so a renamed constant silently resets a setting — a failure that only appears in a build users run, never in debug or in JVM tests. Two rules prevent it: keep `SourceFile`/`LineNumberTable` for readable crashes, and keep the constant fields of `com.sober.**` enums. Keeping the fields also switches off enum unboxing, which is the rewrite that turns name lookups into ordinals. Everything else is already covered by consumer rules shipped inside Room, Hilt, Compose, WorkManager, Lottie and Coil — adding blanket `-keep` rules for them only bloats the APK. On AGP 9 the switch is `optimization { enable = true; keepRules { includeDefault = true; files.add(...) } }`; `isMinifyEnabled` still exists but is no longer the documented path. Signing belongs in environment variables (`SOBER_KEYSTORE_PATH`/`_PASSWORD`, `SOBER_KEY_ALIAS`, `SOBER_KEY_PASSWORD`), not in Gradle files: a checkout without them must still build an unsigned artifact, and a keystore must never be committed.  
**Solution/Pattern**: After enabling shrinking, verify the *mapping file*, not just `BUILD SUCCESSFUL`: grep for the classes the manifest declares and for persisted enum constants. Sign the AAB/APK with a throwaway key once to prove the wiring, then delete it — the production key stays with the owner.

### 11.10 Compose Tests Do Not Need a Device

**Date**: 2026-10-30  
**Context**: Verification & Delivery asked for Compose UI tests, Espresso tests and Robolectric tests while no emulator exists.  
**Learning**: Robolectric runs the real Android framework classes on the JVM, so `Settings.Secure`, `AppOpsManager` and runtime-permission state behave like Android rather than like a stub. Compose rides on top: `createComposeRule()` works under `RobolectricTestRunner` when the module sets `testOptions { unitTests { isIncludeAndroidResources = true } }` and puts `ui-test-junit4` **and** `ui-test-manifest` on `testImplementation` (the manifest AAR is what declares the host activity). Pin the test to a released API with `@Config(sdk = [35])` instead of inheriting `compileSdk = 37`: Robolectric ships shadowed runtimes only for released Android versions, and an unsupported default is a startup error, not a silent fallback. Espresso is the wrong tool here — with one Activity and no View hierarchy it has nothing to query, while the Compose rule sees every semantics node. One trap matters for this app: a screen with `LaunchedEffect { while (true) { delay(1_000) } }` never reaches idle. Setting `composeRule.mainClock.autoAdvance = false` before `setContent` parks the ticker at the first frame, so the test asserts copy instead of waiting forever.  
**Solution/Pattern**: For each module that needs UI or framework tests, add Robolectric + Compose test dependencies and the resources flag once, then write tests that assert user-visible text and semantics rather than implementation details. When a screen owns an infinite timer, freeze the clock.

### 11.11 A Switch Inside a Clickable Row Survives Merging

**Date**: 2026-10-30  
**Context**: 1.10 needed a per-row "enable/disable limit" switch inside a row that already opened the detail sheet on tap.  
**Learning**: `Modifier.clickable` sets `mergeDescendants = true`, so the row and its texts collapse into one semantics node; the Material `Switch`'s own `toggleable` is a second boundary. Compose hit-testing is positional, so the switch consumes its own tap and the row does not fire — that part is free. What is not free is *finding* the switch in a test: without a distinguishing label, `onNodeWithText("60m").performClick()` may land on the merged row. Passing `Modifier.semantics { contentDescription = "Limit for $name" }` into the switch gives it a stable, unique handle and reads correctly to TalkBack. The tests then assert both halves of the contract: flipping the switch reports the new value with zero row clicks, and tapping the name counts one row click.  
**Solution/Pattern**: When two tap targets share a line, give the inner control its own content description and test both directions. Do not put a raw `Switch` inside a `clickable` row without one.

### 11.12 Test Rules Against the Asset That Ships

**Date**: 2026-10-30  
**Context**: Phase 6 rules were already JSON; unit tests only exercised hand-built snapshots, so a typo in `detection_rules.json` could still ship.  
**Learning**: A JVM unit test can read `src/main/assets/...` with a plain `File(...)` because Gradle runs tests with the module directory as the working directory. Parsing that real file and feeding each `DetectionRule` through the production matcher proves the document and the schema agree — the failure mode a fixture never catches. Assert three things per rule: it matches its own package, it matches no other package, and a visible-but-unselected tab does not match. Uniqueness of rule ids and non-blank overlay copy are cheap invariants that pay off because ids are persisted as preference keys.  
**Solution/Pattern**: Have at least one test per shipped data file that loads the file itself, not a copy. Treat rule ids as persisted API — renaming one disables a user's existing choice.

### 11.13 GitHub Pages Is Not a Free Host for a Project Site Here

**Date**: 2026-10-30  
**Context**: The privacy policy needed a public URL (ticket 5.8). Pages was enabled for the `Sober` repo and served a 404.  
**Learning**: A project site inherits the user site's custom domain. `rishabhx29.github.io` is configured with `rishabhx29.me`, whose DNS points at a different host, so every `rishabhx29.github.io/Sober/...` request 301s to `www.rishabhx29.me/Sober/...` and dies. Raw-file proxies do not rescue it either: jsDelivr and raw.githack serve `.html` as `text/plain` with `nosniff`. What actually works is the file GitHub renders itself — `docs/PRIVACY.md` at `github.com/.../blob/main/...` — which returns 200 and stays in the repo history.  
**Solution/Pattern**: For a policy page, point at the Markdown blob URL until a domain is chosen deliberately; only treat Pages as live after `curl -L` shows 200 on the final host.

### 11.14 `onStartCommand` Runs Again on Every `startService` Call

**Date**: 2026-11-01  
**Context**: Auditing `SoberCoreService` (ticket 1.7) after the release upload.  
**Learning**: Starting an already-running service with `Context.startForegroundService`/`startService` does not call `onCreate` again — it delivers another `onStartCommand` to the same instance. `SoberCoreService.start()` is called from the boot receiver, from `SoberNavHost` on every app launch and from `ensureRunning()` on every session start/stop, so launching the engine loops from `onStartCommand` added a second 15-second poll, a second session countdown ticker and a second window-hint collector each time. Nothing crashes: the poll is mutex-guarded and completion is idempotent, which is exactly why it stays invisible until you look at battery and Database writes.  
**Solution/Pattern**: Put anything that must have one owner per service instance (poll loops, tickers, flow collectors) in `onCreate`, which runs once per instance, and keep `onStartCommand` for `startForeground` and the return value. `START_STICKY` recreations still get a fresh `onCreate`.

### 11.15 A ViewModel Outlives the Tab, So `init` Is Not a Refresh

**Date**: 2026-11-01  
**Context**: Auditing Home, Stats and Apps (tickets 1.9, 4.3, 1.10) after the release upload.  
**Learning**: All three read their data once in the ViewModel's `init`. Navigation Compose keeps the destination's ViewModel across tab switches (`saveState`/`restoreState`), so returning to Home or Stats showed the numbers from when the app was opened — the ring, the app rows, the "today" chart and the per-app minutes all froze. Nothing looks broken, which is why it survives review: the screen renders fine, it just renders the past. A composition-local `Date()` has the same shape of problem: no recomposition, no new date, so an app left open overnight still says yesterday.  
**Solution/Pattern**: Anything that describes "now" (usage totals, today's date, permission state read from system Settings) belongs behind an `ON_RESUME` observer on `LocalLifecycleOwner`, which fires again when the back-stack entry returns to the foreground. Keep the `init` load for first paint, make the refresh non-destructive (no loading spinner if data already exists), and hold the date in state if the header depends on it.

### 11.16 Usage Events Report Changes, Not State

**Date**: 2026-11-01  
**Context**: Auditing foreground detection while picking up the enforcement path (tickets 1.5, 1.7).  
**Learning**: `UsageStatsManager.queryEvents` only contains transitions — a resume when an app comes forward, a pause when it leaves. The engine replayed a fixed 10-minute window and took the last transition as "the app on screen". That is correct for ten minutes and wrong after: a user watching a video emits no events at all, so the resume falls outside the window, the replay finds nothing, and the engine reports "unknown". The damage is worse than a missed block, because the poll treats an unknown screen as "the user switched to something allowed" and tears down an overlay that was already covering the blocked app. Fixed-window polling of an event stream has to start from a known state.  
**Solution/Pattern**: Keep the last answer and apply only new events to it; re-establish that state once when the process starts (here, one 24-hour replay) rather than widening the window on every poll. An empty event list means "nothing changed", not "nothing is there". When writing the fold, keep it a pure function of `(events, previousAnswer)` so the steady-state case — the one that breaks — is directly testable without a device.

### 11.17 An Enforcer Counts Its Own Foreground Service as Screen Time

**Date**: 2026-11-02  
**Context**: Emulator QA. Launching Sober showed its own "you have used 82 min of your 30 min daily cap" overlay, locking the settings screen that would fix it.  
**Learning**: `UsageStatsManager.queryUsageStats` reports `totalTimeInForeground` for every package, including the app asking and any foreground service. Sober's enforcement service runs in Sober's process, so its own runtime accumulated as usage; the launcher's home-screen time accumulates the same way. One day the app had counted 76 minutes for itself. Any cap check that sums all rows therefore eventually blocks the enforcer itself, and Home/Stats show a number that disagrees with both each other and the cap.  
**Solution/Pattern**: Keep one `excludedPackages` set (own package + resolved HOME activity) on the usage repository and apply it at every consumer: enforcement totals, Home ring, Stats live total, stored history reads and session sync. Filtering only at the query source would leave already-synced rows inflating history, so read-side filtering is required too. A `resolveActivity(ACTION_MAIN/CATEGORY_HOME)` lookup is the reliable way to find the launcher; hard-coding `com.android.launcher3` misses OEM launchers. Pin the rule with a test that mixes excluded and included packages in both live and stored data.

### 11.18 Emulator QA Without a Physical Device

**Date**: 2026-11-02  
**Context**: Standing up the `SoberQA` AVD to verify the release build.  
**Learning/Pattern**: A headless AVD plus `adb` covers most UI and enforcement checks, but only if the session is scripted. Useful commands:

```bash
emulator -avd SoberQA -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect &
adb wait-for-device
adb shell getprop sys.boot_completed        # 1 means booted, not ready to use
adb install -r app-release.apk
adb shell am start -n com.sober.app/.MainActivity
adb exec-out screencap -p > home.png
adb shell dumpsys notification --noredact  # what was actually posted
adb shell dumpsys usagestats                # foreground history
```

The AVD's clock and usage database reset to the host's notion of "today" on a cold boot, so generate usage in the current session (`am start` an app, wait a minute) before capturing a screenshot. Read a screenshot at 900×2000 while the device is 1080×2400 and multiply tap coordinates by 1.2. If `adb devices` goes empty mid-run, the emulator process died; restart it rather than re-issuing adb commands.

### 11.19 The Device's Own Chrome Is Not an App You Can Block

**Date**: 2026-11-02  
**Context**: On the realme phone, once the overall cap fired it started covering System UI and Gboard, and during an install it covered the OEM installer, leaving no way to tap Install.  
**Learning**: An app-level blocker sees whatever package is in front, and keyboards, System UI panels and OEM system activities are packages too. A cap that says "block the foreground app" therefore blocks the tools needed to fix the problem — including the uninstall/install dialogs. Two more facts matter: (1) overlay windows are added by `WindowManager`, so a covered system dialog is invisible in `uiautomator dump` (use `dumpsys window windows` to see the `com.sober.app` window or take a screenshot); (2) "has a launcher icon" is a good blockability test because it excludes transient system UI and OEM installer activities without hard-coding vendor packages.  
**Solution/Pattern**: Put blockability in one place (`BlockableAppsRepository`) and consult it first in the policy engine. Exclude own package, HOME activities, all IME services (`android.view.InputMethod`), the resolved Settings activity plus `com.android.settings`, permission controllers and System UI. Only packages with `getLaunchIntentForPackage != null` may be blocked. In the foreground service, treat System UI, IMEs and permission dialogs as *transient chrome*: if one comes forward, keep the current overlay instead of hiding it. Package visibility on API 30+ needs `<queries>` entries for `android.view.InputMethod`, `android.settings.SETTINGS` and `android.settings.APPLICATION_SETTINGS`, otherwise the lookups return empty and everything looks blockable. User choice belongs separately: the cap's exemption set is a preference, not a classifier, so an exempt app still obeys its own limits and focus sessions.

### 11.20 The OEM Installer Is a Foreground App Too

**Date**: 2026-11-02  
**Context**: `adb install -r` of v1.2 on the realme RMX5030 failed with `INSTALL_FAILED_VERIFICATION_FAILURE`, and the retry surfaced `com.oplus.stdsp/.extappdetail.model.scan.ui.InstallGuideActivity` — which v1.1's cap then covered with the block screen.  
**Learning**: realme/ColorOS routes sideloads through its own "StdSP Core" scanner. It is a normal foreground activity, and `am force-stop`/`am stop-service` do not reliably keep the enforcement service down, so the confirm button can be permanently hidden behind the overlay. Changing verifier settings (`package_verifier_enable`, `verifier_verify_adb_installs`) is one option but alters device security state.  
**Solution/Pattern**: Disable the blocker briefly with `pm disable-user --user 0 com.sober.app`, complete the install through the UI (`uiautomator dump` locates the Install button; its bounds are in raw pixels), verify with `dumpsys package com.sober.app | grep versionName`, then `pm enable com.sober.app` and relaunch. With v1.2's blockability guard this is no longer necessary, because the OEM installer has no launcher activity — but the sequence is the fallback on older builds. Reinstalling also clears the Accessibility binding (`enabled_accessibility_services` becomes `null`); restore it with `settings put secure enabled_accessibility_services com.sober.app/com.sober.system.services.core.SoberAccessibilityService` and confirm `dumpsys accessibility` lists it under `Bound services`.

### 11.21 Emoji Are Text, Not Icons

**Date**: 2026-11-02  
**Context**: The achievement badges and streak chip used emoji glyphs (flame, lotus, check marks). They rendered in each OEM's emoji font, at that font's weight and colour, and TalkBack read them as their Unicode name ("fire").  
**Learning**: An emoji in a `Text` cannot be recoloured, sized independently of the font, or kept visually consistent across devices, and it disappears under some accessibility font settings. Material icons are vectors drawn by Compose, so they take `tint`, match stroke weight, and are silent to TalkBack when `contentDescription = null` because the surrounding row carries the meaning. Real application icons are the exception: those should stay as drawables so users recognise the app.  
**Solution/Pattern**: Map domain ids to vectors in the UI layer (`AchievementId.badgeIcon()`), never store the glyph on the enum. Grep twice after a sweep: one pass for the literal characters, and one for the Unicode emoji ranges (`[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}]`) because many emoji hide in strings that look like plain punctuation.

### 11.22 `Text` Has No `fontFeatureSettings` Parameter

**Date**: 2026-11-02  
**Context**: Ticking minute counts shifted their own line width, so the usage ring's number jittered each second. Adding `fontFeatureSettings = "tnum"` to `Text` failed to compile with "none of the following candidates is applicable".  
**Learning**: Compose's `Text` overloads accept colour, size, weight and style, but not `fontFeatureSettings`; that lives on `TextStyle`. The compiler error names the call, not the missing parameter, which makes it look like an overload-resolution problem.  
**Solution/Pattern**: Keep one `TabularNumberStyle = TextStyle(fontFeatureSettings = "tnum")` in the theme and pass it as `style =`; explicit `fontSize`/`color` arguments still merge over it. Any value that updates on a timer — countdowns, screen-time totals — needs this, or it reflows on every tick.

### 11.23 Equal Cards Need an Intrinsic Row Height

**Date**: 2026-11-02  
**Context**: In the achievements grid, a two-line description made its card taller than the one beside it; the pair looked ragged even though both used the same 18dp radius and padding.  
**Learning**: `Row` measures children independently, and `weight(1f)` only equalises *width*. Equal height needs the row to first measure the tallest child's intrinsic height, then stretch every child to it. Without that, `fillMaxHeight()` has no bounded height to fill and does nothing.  
**Solution/Pattern**: `Row(Modifier.height(IntrinsicSize.Max))` plus `Modifier.weight(1f).fillMaxHeight()` on each card. This is cheap for two-column grids of short content; avoid it for long scrollable children, where intrinsic measurement costs more.

### 11.24 `queryUsageStats` Rounds the Range Out to a Whole Bucket

**Date**: 2026-11-02  
**Context**: The tracker listed Photos without it being opened, and credited Sober with 482 minutes by 06:38.  
**Learning**: `queryUsageStats(INTERVAL_DAILY, startOfToday, now)` does not clip its answer to the requested range. The platform expands both ends to the nearest whole interval period, so a 06:38 request returns the day's bucket — including time recorded when the emulator's clock sat on a different date. `totalTimeInForeground` can therefore exceed the minutes elapsed since midnight, and any limit check built on it can block early. `queryAndAggregateUsageStats` is documented as the same bucket-merged view, so it is not a way out. Usage events are the only signal that carries exact timestamps.  
**Solution/Pattern**: Fold `queryEvents` into per-package intervals with the same resume/pause pairing used for foreground detection, clip each interval to `[from, to]`, and close an open interval at `to` so the app on screen right now counts. Keep the fold pure and test the range edge cases. Accept one deliberate hole: an app already on screen at midnight has no resume inside the range, so it starts counting at its next resume — under-counting is the safe direction, because an inflated total enforces a limit that was never reached.

### 11.25 A Browser's Address Bar Is the Only Page Signal Accessibility Gets for Free

**Date**: 2026-11-02  
**Context**: Adding website blocking without a VPN or the `INTERNET` permission.  
**Learning**: Website blocking is usually sold as DNS filtering, which needs a `VpnService` that forwards every other query upstream — the exact transmission the app promises not to make. Accessibility offers a narrower path: Chrome exposes its omnibox node (`com.android.chrome:id/url_bar`) with the current host as text, so the service can read where the user is without reading page content. The limits follow from that source. The id is per-browser and per-build, so the catalogue is best-effort beyond Chrome; the bar can be scrolled away, delaying detection; custom tabs run inside the host app's package; and a redirect can render for a frame before the address updates.  
**Solution/Pattern**: Normalise both the user's input and the address-bar text through one function (strip scheme, `www.`, path, query and port; reject whitespace, `@`, `chrome:`, `about:`, `file:` and bare words), match host-equals-or-ends-with-dot-domain, and pick the most specific matching rule. Use `textByResourceId` rather than the flat text set, or an unrelated label containing the URL can trigger a block. Cover the page with the existing section overlay so the exit path (`GLOBAL_ACTION_BACK`) is the same one users already know. Document the browser catalogue as verified-for-Chrome-only and keep the address bar out of anything stored.

### 11.26 An Overlay Service That Ignores Its Own Package Can Trap the User

**Date**: 2026-11-02  
**Context**: The website cover stayed on screen after switching back to Sober, hiding the screen where the rule is removed.  
**Learning**: `SoberAccessibilityService` returns early for `packageName == this.packageName` so the overlay window it adds cannot feed its own events back into detection. The same rule means the foreground transition to Sober is never processed, and the cover — which exists to block *another* app — remains on top of the app that manages the rule. The user can still press the overlay's Go back, but the footnote promises removal in Sober while Sober itself is covered.  
**Solution/Pattern**: Hide the cover when the app's own UI becomes visible (`onResume` on the single activity), rather than trying to tell the overlay window apart from the activity in the event stream. Because hiding happens outside the service, the service's "same block already shown" guard must also require `isShowing`; otherwise the retained rule id suppresses the re-show when the user returns to the blocked page. This also keeps the app-level block overlay behaviour intact, since its engine already maps Sober and the launcher to "no target".

### 11.27 A Lazy Share Read Through `.value` Never Collects

**Date**: 2026-11-03  
**Context**: The Focus countdown showed `--:--` for the entire session, while the active-session card still knew the session existed.  
**Learning**: `stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)` only starts collecting upstream while someone is subscribed. The countdown loop read `.value` once per second — reading a `StateFlow` is not subscribing — so the upstream `DataStore` flow never started, the shared state stayed at its `null` initial value, and the formatter rendered the placeholder. Nothing crashed and no test failed, because both sides are individually correct. The same bug was waiting in the meal-break until value.  
**Solution/Pattern**: When a value is consumed by polling or by a non-Compose caller, share it with `SharingStarted.Eagerly`. Reserve `WhileSubscribed` for flows that are collected by UI, where the subscription is guaranteed. The tell is a `StateFlow` whose initial value can still be observed minutes later.

### 11.28 The Emulator Can Repaint the Status Bar With the Wrong Icon Colour

**Date**: 2026-11-03  
**Context**: In light mode the status bar showed white icons on the cream background, making the clock and battery unreadable.  
**Learning**: `dumpsys window` reported `vsysui=LIGHT_STATUS_BAR LIGHT_NAVIGATION_BAR` for Sober, and the system Settings app showed the same white icons on its light background. The app was asking for the correct appearance; Android's SystemUI had kept the dark-mode icon tint after a theme toggle. This is an emulator/SystemUI artifact, not a Compose bug — but an app cannot rely on the system being right.  
**Solution/Pattern**: Verify a suspected status-bar defect against a system app before changing app code. To reset a stuck SystemUI on the emulator, `adb root` then `pkill -f com.android.systemui` (it restarts on its own) and re-apply the night mode. Independently, set both `isAppearanceLightStatusBars` and `isAppearanceLightNavigationBars` from the app's own `darkTheme` value in a `SideEffect`, because `enableEdgeToEdge` can only follow the system theme while the app may be forcing the other one.

### 11.29 An Editorial System Is a Spacing Contract, Not a Theme

**Date**: 2026-11-03  
**Context**: The refactor moved every screen from nested bordered cards to hairline-separated rows.  
**Learning**: Restyling eleven screens one file at a time produced near-duplicates: the same 13dp row padding, the same mono index treatment, the same pill chip and the same quiet text action, each written locally. A screen that drifts by 2dp is invisible in review and obvious in a screenshot walk. Colour tokens alone do not prevent that, because the defect is layout rather than palette.  
**Solution/Pattern**: Put the repeated structure in one file — `SoberEditorial.kt` — and let screens supply content, not spacing: `SoberScreenHeader`, `SoberSectionHeader`, `SoberListRow`, `SoberFilterChips`, `SoberQuietButton`, `SoberInlineMetric`. Keep cards for things that are genuinely a single object (the active session, the completion card); use rows plus dividers for lists. Two consequences to expect: copy-dependent tests need updating when a wordmark changes casing (`Sober` → `SOBER`), and a mid-session or post-session screen must derive its section indices instead of hard-coding them.

### 11.30 Fixed Heights and Fixed Indices Break When Sections Are Conditional

**Date**: 2026-11-03  
**Context**: Three layout defects surfaced in one pass: a focus session sheet hid its Save button, the Study preset clipped its second description line, and section numbering started at `02` or `03` when sections above were hidden.  
**Learning**: Each value was correct when written and wrong once the content around it changed. `ModalBottomSheet` opens at half height by default, and the drum picker plus Save button are taller than that half. A `80.dp` preset card fitted one description line until the copy gained a second. A hard-coded `"03"` index was right in the setup form and wrong in the active and completed states, where the two sections above it are gone. Unit tests do not see any of it; the layout is only wrong on screen.  
**Solution/Pattern**: Open a sheet that owns a confirm button with `skipPartiallyExpanded = true` or size it beyond its own content. Size text containers for the copy they actually hold (the preset card is `92.dp` with an explicit `lineHeight` and `maxLines = 2`). Treat section numbers as data derived from the state that decides visibility (see `sectionIndex` in Schedules and `setupVisible` in Focus), never as literals beside a conditional header.

### 11.31 A Counter Must Be Incremented After the Decision, Not Before

**Date**: 2026-11-03  
**Context**: F3 added an opens-per-day budget. The first implementation recorded the open at the top of `applyPolicy`, then evaluated the block.  
**Learning**: A budget of one blocked the first open of the day: the attempt incremented the count before the check read it. Worse, a blocked app or an abandoned pause attempt consumed budget, so the counter measured attempts rather than usable opens. A zero-minute limit had the mirror-image bug — an `AppLimit(0)` row represents an opens-only rule, but every reader compared `usedMinutes >= 0`, which is always true.  
**Solution/Pattern**: Evaluate policy first, then record the transition only on the path that actually lets the app through. Decide what the counter counts ("times this open was allowed") and enforce that at the one place that knows it. When a field is reused to mean "unset" (0 minutes), guard every reader with `> 0`, not just the writer.

### 11.32 Restore the State You Replaced, Not a Default

**Date**: 2026-11-03  
**Context**: Bedtime mode turns Do Not Disturb on for an overnight window through `NotificationManager.setInterruptionFilter`.  
**Learning**: Restoring `INTERRUPTION_FILTER_ALL` when the window closes would silently cancel a Do Not Disturb the user had chosen themselves. The system offers no "undo" for a filter change, and a sentinel like `false` in a `bedtimeSilenced` flag says only *that* Sober changed something, not what was there before.  
**Solution/Pattern**: Read `currentInterruptionFilter` before the first change, persist it, and restore exactly that value when the window ends; only fall back to `ALL` when the stored value is unrecognised (`UNKNOWN`/`-1`). Check `isNotificationPolicyAccessGranted` on every pass so a revoked grant skips the write instead of throwing, and leave the flag set so the restore retries once access returns.

### 11.33 A Lock Is Only as Strong as the Paths Around It

**Date**: 2026-11-03  
**Context**: F2 added lock mode, then audit found three working escapes: the Settings PIN gate only covered focus sessions, `cancelSession` ended a session freely, and `grant` could mature an override requested before the lock.  
**Learning**: A UI that hides controls is not policy. Anything reachable from another screen, a stale request or a timer still runs, and a lock that any one path ignores is not a lock — it is a suggestion with extra steps.  
**Solution/Pattern**: Put the refusal in the domain use case that mutates state (`ManageFocusSessionUseCase.cancelSession`, `ManageEmergencyOverrideUseCase.request/grant`), return a Boolean the caller can surface, and re-check at both request *and* grant time because a wait can span the lock boundary. Then apply the same rule in every surface (Settings body, Apps edits, Home pause, overlay affordance) so the copy and the enforcement agree.

### 11.34 Draw the Share Card, Don't Capture the Screen

**Date**: 2026-11-03  
**Context**: F6 shares a weekly report. The obvious implementation — screenshot the Stats screen — would crop to the visible scroll viewport and bake in whichever light/dark theme was active.  
**Learning**: A "share what you see" capture is only correct when the thing being shared is exactly what is on screen. A report is a document with its own fixed layout, so it must be rendered independently of the view hierarchy.  
**Solution/Pattern**: Build a `WeeklyReportData` snapshot from the already-loaded UI state, then draw it with `android.graphics.Canvas` at a fixed 1080×1350. Put the PNG in `cacheDir/exports`, which the existing `FileProvider` already exposes, and grant `FLAG_GRANT_READ_URI_PERMISSION` on the `ACTION_SEND` intent. Render with local `Paint` copies (not shared painters) so a fitted-down value in one column cannot shrink the next one.

## 12. Troubleshooting

### 12.1 Common Build Issues

| Issue | Solution |
|-------|----------|
| `Could not find com.google.dagger:hilt-android` | Run `./gradlew --refresh-dependencies` |
| KSP annotation processing failures | Ensure KSP version matches Kotlin version |
| Module not found in navigation | Check `settings.gradle.kts` includes the module |
| `AAPT: resource not found` for fonts | Ensure font files are in `core/ui/src/main/res/font/` |
| `Could not connect to the Gradle daemon` / `BindException: Cannot assign requested address: getsockopt` on Windows | Host IPv6 loopback is blocked; run Gradle with `JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"` and `GRADLE_OPTS="-Djava.net.preferIPv4Stack=true"` |

### 12.2 Runtime Issues

| Issue | Solution |
|-------|----------|
| Foreground service killed by OEM | Add battery optimization whitelist prompt |
| Overlay not showing | Check `Settings.canDrawOverlays()` returns true |
| UsageStats returns empty | Verify `PACKAGE_USAGE_STATS` is granted in Settings |
| Accessibility service not running | Check if it's listed in enabled accessibility services |
| Sideloaded APK blocked by Play Protect | "More details" → "Install anyway"; if absent, pause Play Protect scanning, install, re-enable. On Android 13+, grant "Allow restricted settings" in the app's info page before enabling Accessibility |
| Sober blocks its own settings | The cap totalled Sober's foreground service as usage; confirm `excludedPackages` includes the app and launcher in every total, including stored history |
| Cap blocks the keyboard, System UI or Settings | Before v1.2 the cap had no block-target classifier. Confirm `BlockableAppsRepository.isBlockable` is consulted first in `EvaluateBlockDecisionUseCase`, and that the manifest declares the `android.view.InputMethod` / Settings `<queries>`. Users can also leave an app out of the cap in Settings → Daily screen time → Apps covered → **Choose apps** |

### 12.3 Device-Specific Issues

| Device | Issue | Solution |
|--------|-------|----------|
| Xiaomi | Aggressive battery optimization kills services | Add to "Autostart" list + battery whitelist |
| Samsung | Recent apps cleaner kills foreground service | Enable "Don't optimize" for Sober |
| OnePlus | Deep sleep kills background work | Disable "Adaptive Battery" for Sober |
| realme / OPPO | `adb install` fails with `INSTALL_FAILED_VERIFICATION_FAILURE`; the StdSP installer is covered by the block overlay | Install through the UI after briefly running `pm disable-user --user 0 com.sober.app`, then `pm enable`. On v1.2+ the installer has no launcher activity, so the cap cannot cover it |
| realme / OPPO | Accessibility binding resets after reinstall | `settings put secure enabled_accessibility_services com.sober.app/com.sober.system.services.core.SoberAccessibilityService`, then verify via `dumpsys accessibility` |
| Emulator | `INSTALL_FAILED_UPDATE_INCOMPATIBLE` when installing a debug APK | The AVD already holds the release-signed package. Install a release build signed with the same key (same `versionCode` works with `adb install -r`), or uninstall first — which deletes the app's setup state |

---

> **End of Guide v1.0** — This document grows with the project. New sections will be added by the Guide Agent as development progresses.
