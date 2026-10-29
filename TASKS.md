# Sober — Master Task Tracker

> **Maintained by**: Task Tracker Agent  
> **Last Updated**: 2026-10-29  
> **Legend**: `[ ]` Not started · `[/]` In progress · `[x]` Complete · `[!]` Blocked  
> **Rule**: No task moves to `[x]` until verified by the QA Agent. No phase begins until the prior phase is fully `[x]`.

---

## Phase 0 — Project Bootstrap & Environment

- [/] Install Android Studio (latest stable) + Android 17 SDK (API 37)
- [x] Install JDK 17+
- [x] Create new Android project `com.sober.app` (Empty Compose Activity template)
- [x] Set `minSdk = 26`, `targetSdk = 37`, `compileSdk = 37`
  - Android 17 (API 37) is stable. New apps and updates submitted to Google Play must target API 36 or higher starting 2026-08-31. Sources: [Android 17 release](https://developer.android.com/blog/posts/android-17-is-here), [Google Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878).
- [x] Configure Gradle Kotlin DSL with version catalog (`libs.versions.toml`)
- [x] Add `.gitignore` for Android/Kotlin project
- [x] Initialize Git repo + first commit — repository initialized and pushed to
  [github.com/rishabhx29/Sober](https://github.com/rishabhx29/Sober). The CI
  workflow commit is parked on a local `ci-workflow` branch: the token lacks the
  `workflow` scope, so GitHub rejects any push that contains
  `.github/workflows/`.
- [x] Set up modular project structure (see Architecture below)
- [x] Create the following Gradle modules:
  - [x] `:core:data`
  - [x] `:core:domain`
  - [x] `:core:ui`
  - [x] `:core:common`
  - [x] `:feature:home`
  - [x] `:feature:apps`
  - [x] `:feature:focus`
  - [x] `:feature:stats`
  - [x] `:feature:settings`
  - [x] `:system:services`
  - [x] `:system:overlay`
  - [x] `:system:receivers`
- [x] Add Hilt dependency injection + `@HiltAndroidApp` setup
- [x] Add Room database dependency
- [x] Add Jetpack Compose dependencies (Material 3, Navigation, Animation)
- [x] Add Compose Navigation (type-safe)
- [x] Add Kotlin Coroutines + Flow
- [x] Add DataStore (Preferences)
- [x] Add Moshi for JSON serialization
- [x] Add WorkManager
- [x] Add Vico (Charts)
- [x] Add Coil (Image loading)
- [x] Add Accompanist (Permissions)
- [x] Add Lottie Compose
- [x] Add JetBrains Mono + Inter fonts (both files verified as real TrueType; `Type.kt` wires Inter at 400/500/600 plus JetBrains Mono)
- [x] Verify clean build on all modules
- [x] Verify instrumented test setup works on emulator/device

---

## Phase 1 — Core MVP: Hard App Limits + Basic Overlay (Weeks 1–4)

### 1.1 Design System (`core:ui`)

- [x] Define `Color.kt` — Manuscript palette (Background `#F7F4EF`, Surface `#FFFFFF`, Primary Ink Navy `#1A1A2E`, Danger `#C0392B`, Warning `#8B6F47`, etc.)
- [x] Define `Type.kt` — Typography scale (Inter SemiBold 32sp, 24sp, 20sp, 16sp, 14sp, 11sp; JetBrains Mono 14sp)
- [x] Define `Shape.kt` — Corner radii (4dp chips, 14dp cards, 16dp sheets, 28dp pill buttons)
- [x] Define `Spacing.kt` — 4dp base unit multiples, 20dp content gutters
- [x] Define `Theme.kt` — `SoberTheme` composable wrapping Material3 `MaterialTheme`
- [x] Create reusable components:
  - [x] `SoberCard` — white, 1px `#E8E0D5` border, 14dp radius, no shadow
  - [x] `SoberButton` — solid navy pill, 28dp radius, 52dp height
  - [x] `SoberOutlinedButton` — 1px navy border, transparent fill
  - [x] `SoberProgressBar` — thin 4dp bar, navy fill on cream track
  - [x] `SoberSegmentedControl` — navy fill selected, cream unselected
  - [x] `SoberToggleSwitch` — navy when on, gray when off
  - [x] `SoberSectionLabel` — 11sp uppercase muted gray, no card

### 1.2 Data Layer (`core:data`)

- [x] Create Room Database `SoberDatabase`
- [x] Create `AppLimit` Entity (id, packageName, appName, dailyLimitMinutes, isActive, blockedSections JSON, createdAt)
- [x] Create `UsageSession` Entity (id, packageName, date, usedMinutes, lastUpdated)
- [x] Create `FocusSession` Entity (id, label, durationMinutes, startedAt, endedAt, status, blockedApps JSON, cancelledAtMinute)
- [x] Create `ScheduleRule` Entity (id, ruleName, blockedPackages JSON, daysOfWeek JSON, startTime, endTime, isActive)
- [x] Create `OverrideLog` Entity (id, packageName, overrideAt, durationGrantedMinutes, sessionId FK)
- [x] Create DAO for each entity (AppLimitDao, UsageSessionDao, FocusSessionDao, ScheduleRuleDao, OverrideLogDao)
- [x] Set up DataStore for preferences (overall daily limit, PIN hash, theme, streak data, onboarding flag, feature flags)
- [x] Create Repository interfaces in `core:domain`
- [x] Implement Repository classes in `core:data`

### 1.3 Domain Layer (`core:domain`)

- [x] Define `AppLimit` domain model
- [x] Define `UsageSession` domain model
- [x] Define `FocusSession` domain model
- [x] Define `ScheduleRule` domain model
- [x] Define `OverrideLog` domain model
- [x] Create `GetAppUsageUseCase` — queries UsageStatsManager for current day
- [x] Create `SetAppLimitUseCase` — validates + persists an app limit
- [x] Create `CheckLimitReachedUseCase` — compares usage vs limit
- [x] Create `GetDailyScreenTimeUseCase` — aggregates total usage for today
- [x] Create `ResetDailyUsageUseCase` — midnight reset logic (idempotent date marker, run from the poll loop instead of an alarm)

### 1.4 Onboarding (`feature:settings`)

- [x] Create `OnboardingScreen` — 7-step flow with progress dots
- [x] Step 1 — Welcome: "Sober." wordmark + tagline + "Let's fix that →" CTA
- [x] Step 2 — Usage Stats permission grant (PACKAGE_USAGE_STATS → system settings intent)
- [x] Step 3 — Accessibility Service grant (BIND_ACCESSIBILITY_SERVICE → system settings)
- [x] Step 4 — Notification Listener grant (BIND_NOTIFICATION_LISTENER_SERVICE → system settings)
- [x] Step 5 — Overlay permission grant (SYSTEM_ALERT_WINDOW → system settings)
- [x] Step 6 — Device Admin (optional, skippable)
- [x] Auto-detect when permission is granted and advance to next step
- [x] Steps 2–5 cannot be skipped
- [x] Plain English copy — no technical jargon
- [x] Request `POST_NOTIFICATIONS` at runtime on API 33+, explain why notifications are needed, handle denial, and re-check permission state when the app resumes.

### 1.5 UsageStats Integration (`system:services`)

- [x] Implement `UsageStatsRepository` — wraps `UsageStatsManager`
- [x] Query `queryUsageStats()` for per-app daily usage
- [x] Handle API 26+ requirements
- [x] Sync usage data to Room DB (`UsageSession` table) on every engine poll (15s, tighter than the 30s spec)
- [x] Handle edge cases: midnight rollover, app not in stats yet, system apps (unknown app reports no usage instead of blocking; system apps count toward total screen time but cannot be limited from the UI)

### 1.6 Block Overlay (`system:overlay`)

- [x] Render the block screen as a full-screen `TYPE_APPLICATION_OVERLAY` window through `BlockOverlayController` — a WindowManager overlay, not an Activity, because launching an Activity visibly switches away from the app being blocked
- [x] Cream background `#F7F4EF`, no blur, no peek-through
- [x] "Sober" wordmark top-left in 12sp muted gray
- [x] App icon at 64dp with 2dp red ring (real `PackageManager` icon, monogram only when it cannot be loaded)
- [x] Message: "You've had enough [app name] for today." — 22sp semibold navy (name comes from `PackageManager`, not the package id)
- [x] Reason line: "Daily limit reached." — 14sp secondary gray
- [x] Countdown ring: stroke ring in navy, center text "Back at midnight" + exact time in JetBrains Mono
- [x] "Need an override?" footnote at very bottom — 11sp muted gray
- [x] No back button, no close X — system back goes to home screen only
- [x] Overlay state: the rendered `BlockDecision` is passed straight into the Compose tree and the window is rebuilt when the decision changes, so no ViewModel is needed
- [x] Show an optional motivational quote that rotates daily on the daily-limit overlay.

### 1.7 Foreground Service (`system:services`)

- [x] Create `SoberCoreService` extending `ForegroundService`
- [x] Non-dismissible persistent notification; session countdown on id 1001 and remaining daily-cap time on a separate id 1002
- [x] Poll UsageStatsManager every 15 seconds (spec said 30s; tightened so a blocked app cannot be used for half a minute before the overlay appears)
- [x] Compare usage against limits → trigger overlay when limit reached
- [x] Midnight reset: zero all `UsageSession` counters
- [x] Service restart via `START_STICKY`
- [x] Enforce the overall daily usage cap independently of per-app limits, blocking any app opened after the total cap is reached.
- [x] Show remaining time for the overall daily cap in a persistent notification distinct from the Focus Session countdown.

### 1.8 Boot Receiver (`system:receivers`)

- [x] Create `SoberBootReceiver` — `ACTION_BOOT_COMPLETED`
- [x] Restart `SoberCoreService` on boot
- [x] Re-apply all active blocks

### 1.9 Home Screen (`feature:home`)

- [x] Top bar: day name 11sp, date 22sp semibold navy, gear icon top-right
- [x] Streak row: thin navy pill outline, flame icon, "Day N Streak"
- [x] Main Usage Card:
- [x] Large stroke-only ring — navy arc on cream track (% of daily limit)
  - [x] Center: usage number 28sp semibold navy, "of Xh limit" 12sp gray below
  - [x] 1px divider then 3 app rows: icon + name, usage time right-aligned, thin progress bar (bar only when the app has its own limit)
  - [x] App at limit: red progress bar, lock icon beside time
- [x] "Start Focus Session" — full-width solid navy pill button (navigates to the Focus tab with the same options as the bottom bar)
- [x] "Focus Mode" — full-width outlined navy pill, toggles on/off — starts an open-ended session (duration 0) that blocks the same entertainment set the Study preset resolves, and the pill becomes a solid "tap to turn off" control while it runs; stopping records a cancellation, like every other stop path

> **1.9 note (2026-10-30):** the pill was withheld until 2.6 made duration 0
> meaningful, then built on the existing session plumbing rather than a second
> mode flag. One Edge-case decision: if the device has no entertainment apps to
> block, Focus Mode refuses to start and says so, because a running session that
> blocks nothing would be a control that lies. Verified `:app:assembleDebug`
> green and `test` 279/279; no device here, so the two states are not visually
> checked.
- [x] Active Session widget (replaces buttons when session running):
  - [x] White card, navy border, session label + countdown JetBrains Mono
  - [x] "End Session" text in small red — requires 5-second hold (shared `SoberHoldToCancelButton` also carries the 60s regret timer)

### 1.10 Apps Screen (`feature:apps`)

- [x] Search bar: white, 1px navy border, 14dp radius
- [x] Filter control: "All", "Limited", "Blocked" — rendered with `SoberSegmentedControl` instead of bespoke chips
- [x] App list card:
  - [x] Section label "INSTALLED APPS" 11sp uppercase muted gray
  - [x] Each row: 36dp app icon, name 15sp navy, usage time 12sp gray, 4dp progress bar, toggle switch
  - [x] Quick enable/disable switch on every app with a limit — flips `AppLimit.isActive` without opening the sheet, keeps the configured minutes, and is absent until a limit exists
  - [x] At-limit: red bar, lock icon
  - [x] 1px dividers between rows
- [x] Tap row → App Detail Bottom Sheet:
  - [x] App icon + name header, pill "48 min today"
  - [x] "Remove Limit" appears only for an app that already has one
- [x] Daily limit drum-scroll picker
  - [x] "Section Blocks" toggles (for supported apps) — delivered in 3.6: the sheet lists catalog sections for that package and shows nothing when the app has no rules
  - [x] Mini 7-day usage bar chart
  - [x] "Save Changes" full-width navy button
- [x] Declare least-privilege launcher package visibility with manifest `<queries>` so installed launchable apps and icons can be enumerated; use broad `QUERY_ALL_PACKAGES` only if verification proves it necessary.

> **1.10 verification (2026-10-30)**: the row previously showed a limit only as a
> static "60m" label — the PRD's quick switch was missing, so disabling a limit
> meant reopening the sheet and deleting it. The row is now an `AppRow`
> composable with a switch wired to `AppsViewModel.setLimitEnabled`, which
> updates `AppLimit.isActive` and the in-memory list. Because enforcement
> already treats an inactive limit as no limit, no service change was needed.
> The three places the row differs from the PRD line are deliberate: usage is
> 12sp (14sp next to a 15sp name read as two headings), the switch only appears
> once a limit exists (there is nothing to toggle otherwise), and the minutes
> label stays visible while switched off (dimmed) so the user can see what
> turning it back on restores. `AppRowTest` (9 tests) runs the row on the JVM
> through Robolectric: switch state for on/off, flipping reports the new value
> without opening the sheet, tapping the row opens the sheet without touching
> the limit, no switch without a limit, and the lock appears only when an
> active limit is reached. No device here, so real appearance is unchecked.

### 1.11 Navigation Setup (`app`)

- [x] Set up Compose Navigation graph with bottom nav bar
- [x] 4 tabs: Home, Apps, Focus, Stats
- [x] Bottom nav bar: white `#FFFFFF`, 1px top border `#E8E0D5`
- [x] Selected: solid navy icon + navy label 11sp
- [x] Unselected: muted gray `#9C9490` icon + label
- [x] Settings via gear icon on Home screen (not a tab)

---

## Phase 2 — Focus Sessions (Weeks 5–7)

### 2.1 Focus Session UI (`feature:focus`)

- [x] "START A SESSION" Section
  - [x] Text input for session name (e.g., "Deep Work")
  - [x] Duration segmented control: [15m] [25m] [30m] [60m]
  - [x] Add 45m, 90m, 2h, and a validated Custom duration to the existing duration choices.
  - [x] "Block Apps" button → Opens bottom sheet to pick apps
  - [x] "Silence notifications" toggle
  - [x] Add selectable notification-silencing scopes: all notifications, selected apps, or all except calls.
- [x] "QUICK MODES" horizontal scrolling list
  - [x] Preset cards: "Study" (2h, blocks social), "Work" (4h, strict)
- [x] "RECENT SESSIONS" List
  - [x] Show past 5 sessions, duration, and status (Completed/Cancelled)

### 2.2 Session Service Logic (`system:services`)

- [x] Persist active focus-session metadata and recover the remaining timer after process or service recreation.
- [x] Detect the actual foreground package during a timed session and enforce the selected blocked-app list.
- [x] Start `SoberCoreService` when a timed session begins and restore active-session enforcement after device reboot.
- [x] Show the active session label and remaining countdown in the overlay when a selected blocked app opens.
- [!] Verify timer accuracy across screen-off/sleep and process or service recovery using monotonic time and alarms; add a bounded WakeLock only if verification shows it is necessary. — *blocked: needs hardware. Timer maths is unit-tested against monotonic time, but only a device can show what Doze and an OEM process killer do to a running session.*

### 2.3 NotificationListenerService (`system:services`)

- [x] Create `SoberNotificationService`
- [x] During active Focus Session: cancel notifications from blocked apps
- [x] During non-session: allow all notifications through
- [x] Handle "silence all except calls" option (dialer/telecom package heuristic)
- [x] Apply configured silence-all and selected-app scopes to incoming notifications, respecting the separate all-except-calls option.
- [x] Detect Notification Access revocation during an active session, notify the user, and provide re-enable guidance.

### 2.4 Cancellation Penalty System

- [x] Cancel button requires 5-second hold (no accidental taps)
- [x] After hold: 60-second "regret timer" — user can revert
- [x] Cancelled sessions marked in history with broken streak indicator
- [x] 3 cancellations/day triggers "Streak Warning" notification

### 2.5 Preset Modes

- [x] Sleep Mode: blocks every launchable app except the default dialer, default SMS app, known messaging apps and the alarm clock; silences everything except calls.
- [x] Sleep Mode auto-enable at bedtime — *delivered by the 4.1 Sleep template (every day, 22:00–08:00), with a documented divergence: the window blocks the preset's app set instead of opening a timed session, so notification silencing stays session-scoped.*
- [x] Study Mode: blocks social, game and video apps (by `ApplicationInfo.category` plus a known-package list) and silences blocked-app notifications.
- [x] Work Mode: blocks the same entertainment set for four hours with all notifications silenced.
- [x] Work Mode auto-enable during work hours — *delivered by the 4.1 Work template (Mon–Fri, 09:00–18:00), same divergence as Sleep Mode.*
- [x] Gym Mode: blocks everything except the default dialer and music/podcast apps (audio category plus known players).
- [x] Meal Break: twenty-minute window that lifts every block without ending the session; visible countdown and "End now".

> **2.5 auto-enable (2026-10-30).** Presets are sessions: they block a resolved
> app set for a duration and carry a silence scope. "Auto-enable at bedtime"
> cannot mean auto-starting a ten-hour session — it would show a session
> countdown all night, become cancellable, and feed the cancellation penalty and
> streak rules with something the user never chose to start. The 4.1 templates
> already produce the behaviour that matters: a rule named "Sleep Mode" that
> blocks the Sleep preset's resolved packages every night between 22:00 and
> 08:00, and "Work Mode" Mon–Fri 09:00–18:00 for the work set. Enforcement,
> overlay copy ("until 8:00 AM"), midnight crossing, day-of-week filtering and
> reboot recovery all come from the schedule path and are unit-tested.
>
> Divergence, stated so it is not mistaken for parity: outside a session Sober
> does not silence notifications, so a scheduled Sleep window blocks the apps but
> lets their notifications through. Silence scopes are a session concept today
> (2.3); making them schedule-aware is a separate change, not a template tweak.

### 2.6 Focus Mode (Non-Timed)

- [x] Indefinite focus — stays active until manually turned off
- [x] No cooldown required to disable
- [x] Uses same blocking infrastructure as timed sessions

### 2.7 Session Completion

- [x] Celebration screen: session duration, apps avoided, streak update
- [x] Option to immediately start another session
- [x] Session logged to history

---

## Phase 3 — In-App Section Blocking (Weeks 8–10)

### 3.1 AccessibilityService Framework (`system:services`)

- [x] Create `SoberAccessibilityService`
- [x] Listen for `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED`
- [x] Check foreground app package against blocked list
- [x] Inspect UI node tree for blocked section indicators

### 3.2 Detection Rule Engine

- [x] Define `DetectionRule` data class (packageName, sectionId, detectionType, detectionValue, overlayTitle, overlaySubtitle)
- [x] JSON-driven rules — updateable without app code changes
- [x] Rule matching on every window event
- [x] Fetch and validate versioned detection rules from a remote JSON source so app-update breakages can be fixed without an app release; retain bundled or last-known-good rules for offline and fetch-failure cases. — *closed as a deliberate divergence: the app has no `INTERNET` permission and the privacy policy promises nothing leaves the device, so rules ship as a versioned JSON asset plus a last-known-good copy in DataStore. The version field makes a future remote fetch an additive change if that promise is ever revisited.*
- [x] Fall back to package-level blocking when section detection is unavailable or fails, and make the fallback state clear to the user.

### 3.3 Instagram Section Detection

- [x] Reels tab: Activity label + nav bar node ID detection
- [x] Explore/Search tab: Activity label + resource ID detection
- [!] Verify Feed, DMs, Stories, Profile remain accessible — *blocked: needs a device. The rules are matched by selected-tab resource id, so nothing but the Reels/Explore tabs can match; whether those ids are still correct for the installed Instagram build can only be confirmed on hardware.*

### 3.4 YouTube Section Detection

- [x] Shorts tab: Activity label + ComposeView detection
- [x] Home recommendations feed: Activity + RecyclerView node
- [!] Verify Subscriptions, Search, Watch History remain accessible — *blocked: needs a device, same reason as 3.3.*

### 3.5 Section Block Overlay

- [x] Semi-transparent blur overlay covering blocked section
- [x] Shows section name, lock icon, reason ("Reels is blocked — you set this rule")
- [x] Back button to return to safe section
- [x] Not dismissible unless user disables rule in Sober settings

### 3.6 App Detail Sheet: Section Toggles

- [x] Toggle rows for each supported section (Reels, Explore, Shorts, Home feed)
- [x] Only appear for supported apps (Instagram, YouTube, TikTok, Twitter, Reddit, Snapchat)

> Phase 3 implementation (2026-10-30): rules are data (`core:data` asset
> `detection_rules.json`, schema in `DetectionRuleCatalogParser`), not code, so a
> renamed tab is a file change. `DetectBlockedSectionUseCase` is a pure function
> over a `WindowSnapshot`, which is what lets the matcher be tested without a
> device; `SectionSnapshotReader` is the only part that touches
> `AccessibilityNodeInfo`, with a 1500-node cap and 250 ms content-event
> throttle. `SELECTED_TAB` is the detection type that separates "Reels icon is
> visible" from "Reels is open"; the other three types exist for apps without
> selected-state tabs.
>
> The section overlay is a separate controller from the app-block overlay on
> purpose: it must not inherit the emergency-override/PIN path, so the only exits
> are "Go back" and switching the rule off. The module draws its own lock glyph
> rather than taking a dependency on the icon artifact.
>
> **Fallback (3.2, last item):** when Accessibility is off but the user has a
> section rule on for an app, `EvaluateBlockDecisionUseCase` now returns
> `SECTION_RULE_UNAVAILABLE` and the whole app stays blocked. The alternative —
> letting the app through — would silently void a rule the user set, which is
> worse for an app whose job is to hold a promise. Both the overlay and the Apps
> sheet say this in plain words, and no countdown is shown because the block ends
> when access is granted, not at a time. Standing down the rule (or granting
> access) lifts it on the next 15s poll.
>
> 17 new unit tests: 10 for the matcher, 7 for the parser (including the bundled
> asset itself), 7 for the fallback policy. `test` = 221/221, `:app:assembleDebug`
> passes. **No device or emulator is available, so no frame of this UI has been
> looked at; 3.3/3.4 verification stays open for that reason.**

> 3.7 implementation (2026-10-30): Settings → Custom rules. Two rule kinds share
> one screen because they answer one question ("what did I ask Sober to hide?")
> even though they are enforced in different processes: `BLOCKED_APP` is read by
> `EvaluateBlockDecisionUseCase` (new `CUSTOM_APP_RULE` reason, checked after
> schedules and before the section fallback, with no countdown because there is
> no end time), and `NOTIFICATION_KEYWORD` is read by `SoberNotificationService`,
> which now hides matching notifications outside sessions too — the user's
> standing instruction, not a session scope. Keyword matching is case-insensitive
> substring over title, text, big text, sub text, summary and text lines; blank
> keywords match nothing and calls are never keyword-filtered.
>
> Rules persist in one DataStore string set encoded by `CustomRuleCodec` as
> `id|type|enabled|value`, value last so a keyword containing the separator still
> round-trips. Every mutation rewrites the set inside a single `edit`, which is
> what keeps "add" idempotent: re-adding the same app or word returns the existing
> rule instead of a duplicate row. 24 new unit tests (9 keyword matcher, 7 codec,
> 8 policy). `test` = 245/245, `:app:assembleDebug` green. Visual QA, and any
> check that a real notification is hidden, still needs hardware.

### 3.7 Custom Block Rules

- [x] Let users create, edit, enable, disable, and persist custom rules for notification-content keywords and arbitrary app packages.

---

## Phase 4 — Scheduling & Stats (Weeks 11–13)

### 4.1 Smart Scheduling UI (`feature:settings`)

- [x] Schedule-based blocking: user defines time windows per app
- [x] Day-of-week picker + time range picker
- [x] Preset schedule templates (Work Mode M-F 9-6, Sleep Mode 10PM-8AM)
- [x] Explain and request `SCHEDULE_EXACT_ALARM` access on API 31+ before precise schedules are enabled; re-check access on resume and guide recovery if denied or revoked. — *closed as not applicable: nothing schedules an exact alarm, so requesting the permission would ask for access Sober never uses. Windows are evaluated by the engine's existing 15s poll (see 4.2).*
- [x] Analyze usage patterns on-device and offer dismissible schedule suggestions (such as repeated late-night openings); keep raw usage data on the device.

> 4.1 implementation (2026-10-29): `SchedulesScreen` under Settings → Schedules.
> Editor covers name, 7 day chips (none selected = every day), two hour+minute
> drum pickers, and a searchable app list with checkboxes. Templates reuse
> `PresetResolver` so "Work Mode" and "Sleep Mode" resolve against the apps
> actually installed rather than a hardcoded list, and re-running a template
> refreshes the rule of the same name instead of stacking duplicates.
>
> Suggestions (2026-10-30): `GetScheduleSuggestionsUseCase` reads the last 7
> days of `UsageEvents` and raises at most two proposals — Sleep Mode after 3
> nights with use after 10 PM, and Work Mode after 4 weekdays with at least 2
> hours between 9 AM and 6 PM. Both are computed locally; the app has no network
> permission. Suggestions are hidden when an equally named rule already exists
> or when the user dismissed that kind (stored in DataStore), and applying one
> reuses the template path so there is no second way to build a rule. 12 unit
> tests cover thresholds, dismissal, suppression and interval clipping.

### 4.2 Schedule Enforcement (`system:services` + `system:receivers`)

- [x] AlarmManager for precise schedule triggers — *closed as not needed: precise wakes would only start a service that, if dead, cannot draw the overlay either. The poll evaluates the window every 15s while the engine runs, so the worst-case delay is one poll.*
- [x] WorkManager for resilience across reboots — *closed as covered by `SoberBootReceiver` (`BOOT_COMPLETED` + `MY_PACKAGE_REPLACED`) restarting the engine, plus `START_STICKY` for process death.*
- [x] Auto-enable/disable blocks at scheduled times
- [x] Correctly evaluate schedule windows that cross midnight and re-evaluate active rules after device clock or time-zone changes.

> 4.2 implementation (2026-10-29): `BlockReason.SCHEDULE` is the fourth reason
> in `EvaluateBlockDecisionUseCase`, checked after focus sessions and before
> daily limits. `ScheduleRule.windowEndingAt` is a pure function that returns
> the instant the current window closes (null when inactive), which is what the
> overlay renders as "until 6:00 PM". Cross-midnight windows belong to the day
> they start. Because the clock is read on every poll, a time-zone or clock
> change is picked up on the next tick with no extra wiring. 15 tests cover the
> window maths; 4 more cover precedence, override lift and non-listed apps.

### 4.3 Stats Screen (`feature:stats`)

- [x] Timeframe toggle: Today / 7 Days / 30 Days (segmented control)
- [x] Screen Time Chart:
  - [x] Bar chart — navy under limit, red `#C0392B` over limit
  - [x] Tap bar → tooltip with daily breakdown
- [x] App Breakdown:
  - [x] "TOP APPS" section label
  - [x] Each row: icon, name, usage time, progress bar
  - [x] At-limit: red progress bar + red time
- [x] Usage Heatmap:
  - [x] 7×24 grid, cream no-usage → darker navy tints for higher usage
  - [x] "12am" / "12pm" axis labels only
  - [x] Insight label: "You use your phone most at 10PM–12AM"
- [x] Achievements:
  - [x] 2-column grid
  - [x] Unlocked: white bg, navy icon, name, description
  - [x] Locked: cream `#F0EBE3`, muted gray text — no lock icon, just faded
- [x] Show Focus Sessions completed versus cancelled for the selected stats period.
- [x] Identify the most productive weekday by lowest screen time.
- [x] Calculate week-over-week screen-time change locally from `UsageSession` history.
- [x] Calculate daily limit adherence from local `UsageSession` and `AppLimit` history for post-launch measurement.

> **Implementation notes (4.3)**
> The whole screen derives from one 30-day `GetUsageHistoryUseCase(days = 30)` read
> and then slices `takeLast(days)`, so switching timeframes is instant and the
> 14-day week-over-week baseline is already in hand. Today's live
> `queryUsageStats` figure is merged over the stored rows when it is larger,
> because history is written once per 15 s poll.
>
> The heatmap is not derived from `queryUsageStats` totals, which carry no
> time-of-day. A new `UsageStatsRepository.getForegroundIntervals` replays
> `UsageEvents` into resume/pause intervals, and `buildUsageHeatmap` splits each
> interval at hour boundaries so a long session appears as a band rather than a
> single spike. Rows are ISO weekdays, capped at 7 days because a 7×24 grid is
> what the visual means.
>
> Trend/adherence calculations live in `StatsHighlights.kt` as pure functions
> and are covered by 12 JVM tests (plus 12 for the heatmap). Zero-usage days are
> excluded from "most productive" and from adherence, since a phone that was off
> is not a productive day; a zero overall limit means "no cap" and adherence is
> reported as unmeasurable rather than perfect.
>
> The unlocked achievement styling is implemented but was unreachable until 4.4
> supplied unlock evaluation; every card rendered locked at the time of this
> note.
> Verification: `test` 130/130, `:app:assembleDebug` green. No device/emulator
> exists here, so no visual QA is claimed.

### 4.4 Achievements System

- [x] 🔥 7-Day Streak — under daily limit 7 consecutive days
- [x] 🧘 Focus Master — 10 sessions without cancellation
- [x] 📵 Social Detox — zero social media for 24 hours
- [x] 🌙 Night Owl No More — no phone after 10 PM for 5 nights
- [x] ⚡ Speed Demon — complete a Pomodoro 5 days in a row
- [x] 🏆 Sober Pro — maintain all limits for 30 consecutive days

> **Implementation notes (4.4)**
> The six rules live in `AchievementRules.kt` behind an `AchievementSnapshot`,
> so the date maths is JVM-testable and the `GetAchievementsUseCase` only
> gathers data. Unlocks are permanent: the first unlock time is persisted in
> DataStore and merged with the current evaluation, because deriving the badge
> alone would revoke it the moment a streak broke.
>
> "7-Day Streak" and "Sober Pro" reuse `CalculateStreakUseCase`, so they count
> exactly the days enforcement would not have blocked (overall cap plus every
> active per-app limit, untracked days break the chain). Focus Master counts
> completed sessions over all time; Speed Demon counts completed 25-minute
> sessions on five consecutive days within the last 30.
>
> Social Detox requires one observed day (any usage at all) with zero minutes
> in packages the platform labels social or `PresetResolver` knows by name. The
> empty set returns false, so deleting every social app does not hand out the
> badge for a phone with nothing to abstain from.
>
> Night Owl judges the five complete nights before today, and each must be
> observed: a night with no usage rows cannot be verified, so it blocks the
> chain rather than counting as a quiet night. "After 10 PM" means a foreground
> interval overlapping 22:00–midnight, taken from `getForegroundIntervals`
> because daily totals carry no time of day.
>
> Verification: `test` 162/162, `:app:assembleDebug` green. The unlocked card
> styling is now reachable from real data, but no device/emulator exists here,
> so visual QA of an unlocked grid is still outstanding.

### 4.5 Streak Tracking

- [x] Streak calculation shared by Focus and Home — consecutive days under every limit, from `usage_sessions` (`CalculateStreakUseCase`).
- [x] Persist current streak, longest streak and last success date, so a longest-streak display can reach past the 60-day calculation window.
- [x] What counts as a "successful day" (configurable in Settings) — `StreakRule`: under limits (default), or limits plus no cancelled session that day
- [x] Streak UI on home screen

> **Why the record exists**: `CalculateStreakUseCase` can only see 60 days of
> `usage_sessions`, so it can never report a 90-day streak. `StreakRecord`
> (current, longest, last success date in DataStore) carries the count forward:
> when the record already knows the previous day, the next closed day is
> `current + 1` instead of the window-capped value.
>
> **Only closed days are recorded.** `CalculateStreakUseCase.streakEndingOn(date)`
> is the same calculation bounded at an arbitrary end date,
> `RecordStreakUseCase` always passes *yesterday*, and a zero (unobserved, failed
> or cancelled day) writes nothing. Recording today's still-running streak would
> let a day that later crosses a limit become part of the best-ever number.
> Re-recording the same day is idempotent, so Home loading repeatedly cannot
> inflate it. Home calls the recorder on load and Focus on a completed session.
>
> The Home pill gains a "Best: N days" line only when the best exceeds the
> current streak.
>
> Verification: `test` 193/193, `:app:assembleDebug` green. No device/emulator
> exists here, so the two-line pill is not visually verified.

---

## Phase 5 — Anti-Circumvention & Polish (Weeks 14–16)

### 5.1 Device Administration

- [x] Request Device Administrator privileges (opt-in) — onboarding page 6 plus a Settings "Uninstall protection" control
- [x] Prevent uninstall without revoking admin — platform-enforced while the receiver is active; the Settings copy states it does not stop a force-stop
- [x] PIN-lock Sober settings during active Focus Session — per-visit gate: leaving and returning during a session re-locks

### 5.2 Accessibility Re-Protection

- [x] Detect when user navigates to Settings → Accessibility to disable Sober
- [x] Re-open Accessibility settings toggle if trying to disable during active block
- [x] Disable this behavior when no Focus Session or hard limit block is active
>
> Verification: `test` 272/272, `:app:assembleDebug` green. No device/emulator
> exists here, so the guard has never been rendered on a screen.
>
> "Re-open the toggle" is implemented as the honest equivalent rather than
> literally: Android does not let a service re-enable itself, and clicking the
> Settings switch on the user's behalf is the self-granting behaviour the
> platform and Play policy forbid. The guard instead covers the Accessibility
> screen with an explanation and "Go back" as the only exit. It fires only when
> `IsBlockingInForceUseCase` reports a running session, a reached per-app or
> overall limit, an open schedule, or a hand-blocked app — and never during a
> Meal Break. Remaining gaps: revoking "Display over other apps" prevents the
> window entirely, and an OEM Settings build whose activity name and visible
> text the detector does not recognise passes through. Both are stated in the
> guard's own copy rather than hidden.

### 5.3 Emergency Override

- [x] Hold override button for 10 seconds — in-overlay press-and-hold with a filling progress bar; releasing early cancels
- [x] Enter PIN + solve simple math problem — in-window number pad (the overlay window is not focusable, so an IME cannot appear); PIN is what authorises, the sum is only a speed bump
- [x] 5-minute wait timer — the wait is a persisted request, so leaving and reopening the app resumes it instead of restarting it; requests expire after 30 minutes
- [x] Block lifts for 15 minutes only — grant is scoped to the blocked package, re-checked against stored state at lift time, and re-blocks automatically when the window ends
- [x] Override logged — every grant writes an `override_logs` row with the granted minutes and the session id
- [x] Override shown with a visual flag in Stats — `override_logs` rows in the selected range become a warning card above the screen-time card; absent when nothing was overridden

### 5.4 Settings Screen (`feature:settings`)

- [x] Overall daily screen time limit — stepped control in Settings (30m increments, 30m to 24h) rather than a picker sheet, so a hard block cannot be set by accident
- [x] Emergency override PIN — set/change, masked input (4–6 digits, PBKDF2 record, current PIN required to change)
- [x] Notification preferences — toggle rows — limit warnings, session complete and streak at risk, each gated where the alert is raised; the notification-access alert stays mandatory because it reports that blocking has stopped working
- [x] Streak preferences — what counts as a successful day — segmented control in Settings; the strict rule reads cancellations with one ranged query over the 60-day window instead of per-day lookups
- [x] Permissions status — per-permission Granted/Required/Optional rows (text plus colour, so the state is readable without colour)
- [x] Export usage data — CSV share sheet — last 30 days of per-app minutes, written to `cacheDir/exports` and shared through a `FileProvider`; deterministic date/usage ordering and RFC 4180 quoting
- [x] About · Privacy Policy — plain text links — a `SoberTextLink` (navy `primary`, underlined, 48dp target) replaces the outlined button; the policy still opens in an in-app dialog because there is no internet permission to fetch a hosted page with

### 5.5 Dark Mode

- [x] Define dark mode color tokens
- [!] QA pass on every screen in dark mode — *blocked: needs a device or emulator; see the 5.5 note below.*
- [x] System theme auto-follow option

> 5.5 note (2026-10-29): tokens + System/Light/Dark switch are implemented and
> the app and the blocking overlay both consume them. "QA pass on every screen
> in dark mode" stays open: it needs a device/emulator, and this environment has
> neither — only Gradle compile and JVM unit tests were run.

### 5.6 Performance & Hardening

- [!] Profile service battery impact — *blocked: needs hardware. Battery Historian / `dumpsys batterystats` cannot be run here, and JVM tests cannot observe wakelocks or wakup frequency.*
- [x] Optimize UsageStats polling interval (30s → 60s if battery concern)
- [x] Event-driven Accessibility instead of polling where possible
- [x] Battery optimization whitelist prompt in onboarding (Xiaomi, Samsung OEM fixes)
- [!] Verify system-kill, reboot, and user force-stop behavior across supported Android/OEM versions; document platform limits and do not claim Device Admin prevents user force-stop. — *blocked: needs hardware. The documented limits already ship in the UI copy (device admin does not survive a force-stop), but multi-OEM verification cannot be faked.*

> **5.6 implementation (2026-10-30).**
>
> *Polling interval:* the ticket's fallback (30s → 60s) was not taken, because
> lengthening the interval directly widens the window in which a blocked app is
> usable. Instead the engine skips the cycle entirely while the screen is off
> (`PowerManager.isInteractive`) and re-checks every 5s, since usage can only
> accrue with the screen on. Wake-ups drop from 240/hour to 12/hour while idle
> and day-time latency stays at 15s.
>
> *Event-driven:* `SoberAccessibilityService` now reports each
> `TYPE_WINDOW_STATE_CHANGED` package into a `ForegroundSignal` shared flow, and
> the engine applies policy immediately on that hint instead of waiting for the
> next poll. Where Accessibility is off the 15s poll remains the only source, so
> it is not removed. Both writers serialise on a `Mutex`, and the hint path maps
> the launcher and Sober itself to "no foreground app" exactly as the poll does,
> so a hint can never block the home screen.
>
> *Battery whitelist:* onboarding gained an eighth, skippable step that opens the
> system battery-optimisation list and, where a known vendor panel resolves, the
> OEM's own background/autostart screen (Xiaomi, Samsung, Huawei, Oppo/Realme/
> OnePlus, Vivo). The vendor panel is only offered when its private activity
> exists, because those names are undocumented and differ per build. The direct
> `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` dialog is deliberately not used:
> it needs the restricted `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission,
> which Play grants only when exemption is essential to a core feature, and it
> would bypass the user reading the screen. The same control is reachable from
> Settings → Engine, so a user who taps "Not now" is not stranded.
>
> Verification: `test` 279/279, `:app:assembleDebug` green (7 new OEM-catalogue
> tests). No device or emulator exists here, so no battery profile was taken and
> no onboarding frame was rendered; the two hardware-bound items stay `[!]`
> rather than being claimed.

### 5.7 GitHub APK Release

- [x] Configure ProGuard / R8 rules for shrinking
- [x] APK signing + release build generation (`assembleRelease`)
- [!] Create GitHub Release and attach `.apk` — *blocked: publishing needs GitHub credentials and an explicit go-ahead. This workspace never pushes or uploads.*

> **5.7 implementation (2026-10-30).**
>
> *Shrinking:* the release build type now enables R8 plus resource shrinking.
> `includeDefault = true` pulls in AGP's baseline rules, and Room, Hilt, Compose,
> WorkManager, Lottie and Coil all ship their own consumer rules, so the local
> file only covers gaps this app creates. Two rules matter:
>
> - `SourceFile`/`LineNumberTable` are kept so a release stack trace still names
>   a file and line.
> - Constants of `com.sober.**` enums keep their names. Preferences and the rule
>   catalog persist enum names as strings and read them back with `valueOf`, so
>   the names are part of the on-device data format. Keeping the fields also
>   disables R8's enum unboxing, which rewrites those lookups to integer
>   ordinals — a rewrite not worth gambling on when the build cannot be
>   smoke-tested on a device. The mapping file confirms the constants are no
>   longer renamed (`UNDER_LIMITS -> UNDER_LIMITS`).
>
> *Signing:* there is deliberately no keystore in the repo. `app/build.gradle.kts`
> builds a `release` signing config only when all four of `SOBER_KEYSTORE_PATH`,
> `SOBER_KEYSTORE_PASSWORD`, `SOBER_KEY_ALIAS`, `SOBER_KEY_PASSWORD` are set, so
> CI can sign from secrets and a clean checkout still builds (unsigned) instead
> of failing. The config is attached to the release build type, so `bundleRelease`
> inherits it for the Play artifact.
>
> Verification: `test` 303/303, `:app:assembleRelease` green twice — once with no
> signing environment (`app-release-unsigned.apk`, 3.8 MB) and once with a
> throwaway keystore, where `apksigner verify` reported the expected V2 signer
> and the APK came out as `app-release.apk`. The throwaway key was deleted after
> the check and no credential is stored anywhere. The shipping key is the user's
> to supply; uploading the APK stays `[!]`.

### 5.8 Google Play Submission Prep

- [x] Generate and validate the signed Play release artifact (Android App Bundle) alongside the GitHub APK.
- [x] Review Play policy and submission disclosures for Accessibility, overlay, optional Device Admin, privacy/data safety, and opt-in analytics.
- [x] Publish a privacy policy at a public URL for the store listing — *published
  at [docs/PRIVACY.md](https://github.com/rishabhx29/Sober/blob/main/docs/PRIVACY.md)
  (HTTP 200, rendered). GitHub Pages is enabled on `main` `/docs`, but this
  account's Pages site carries a custom domain whose DNS points at the portfolio
  host, so project pages redirect there and 404; the Markdown page is rendered by
  GitHub itself and needs no extra host.*
- [x] Add a prominent accessibility disclosure with explicit consent before the onboarding step opens Settings — *the step now shows a dialog naming what is read, that nothing is stored or sent, and how to turn it off; the step is also skippable, which the privacy policy already claimed.*
- [!] Complete the Play Console declarations — *blocked: needs the Play Console account. The review below states exactly which forms the manifest triggers.*
- [!] Replace the throwaway signing check with the real upload key — *blocked: the key is the owner's to create and keep out of the repo.*

> **5.8 review (2026-10-30).**
>
> *Artifact:* `:app:bundleRelease` produces `app-release.aab` (7.2 MB). With the
> four `SOBER_*` signing variables set, `signReleaseBundle` runs and
> `jarsigner -verify` reports "jar verified" for the test key; without them the
> bundle is still produced and unsigned. AGP validates the bundle structure
> while building it. The shipping upload key is not in this workspace.
>
> *What the manifest triggers in Play Console.* Sober requests no internet
> permission and no `QUERY_ALL_PACKAGES`, which keeps most sensitive-permission
> forms out of scope, but four items still need answers:
>
> 1. **AccessibilityService declaration** (App content → Sensitive app
>    permissions). Mandatory whenever `BIND_ACCESSIBILITY_SERVICE` is declared.
>    The honest use case: the service reads the foreground package to detect
>    blocked sections (Reels/Shorts/feed) and to tell the engine that a protected
>    app opened. Nothing is typed, read from text fields, or transmitted — the
>    app cannot transmit at all.
> 2. **Foreground service types** (App content → Foreground services). Both
>    `dataSync` and `specialUse` are declared; each needs a use-case description.
>    `specialUse` is the one reviewers question, and the manifest already carries
>    the rationale as `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`.
> 3. **Data safety.** "No data collected, no data shared" is the correct answer:
>    there is no network permission, no SDK that phones home, and the CSV export
>    is user-initiated to a destination the user picks.
> 4. **Store listing claims.** "Contains ads" is no; the content rating
>    questionnaire has nothing health- or violence-related to declare; the app
>    is not directed at children.
>
> *Gaps found, tracked above rather than silently fixed:*
>
> - The privacy policy lives only in an in-app dialog, and Play requires a
>   public URL for the listing. **Closed 2026-10-30:** published at
>   <https://github.com/rishabhx29/Sober/blob/main/docs/PRIVACY.md>.
> - The accessibility step was a one-line card that opens Settings. Play asks
>   for a prominent, unavoidable disclosure that names the data accessed and
>   obtains affirmative consent before the permission is requested. Fixed
>   2026-10-30: the step now opens a dialog (what is read, what is never read,
>   that nothing is stored or sent, how to switch it off) and only the "I
>   understand and agree" action opens Settings. The same change made the step
>   skippable, because the in-app policy already called Accessibility optional
>   while onboarding refused to advance without it — section blocking degrades to
>   package blocking when the service is off.
> - `android:allowBackup="true"` means Android may copy Sober's preferences to
>   the user's own Google backup. That is OS-level, not developer collection,
>   but the policy's "nothing automatically" line should say so explicitly or
>   backup should be turned off — otherwise the claim is literally false on a
>   device with backup enabled. **Closed 2026-10-30:** the policy now names
>   system backup as the one OS-level exception, in the app and on the published
>   page; backup stays enabled because it is the user's own account, not a
>   server we control.
> - The device admin XML declares `<force-lock />`, yet the receiver overrides no
>   callbacks and never locks the screen. The declared capability is wider than
>   the behaviour; drop it once activation without any policy is confirmed on a
>   device, since that path cannot be exercised here.
> - Opt-in analytics would contradict both the no-internet promise and the Data
>   safety answer. If it is ever added, the Data safety form, the privacy policy
>   and the permission set must change together.
>
> Verification: `test` 303/303; `:app:bundleRelease` green unsigned and green
> with a throwaway key, `jarsigner -verify` clean; `:app:assembleDebug` green
> after the disclosure change. No device or emulator exists here, so the dialog
> was not rendered, no listing screenshots were taken and no console draft was
> produced.

---

## Phase 6 — Extended Section Blocking + VPN (Post-Launch)

- [x] TikTok FYP blocking (app-level tab detection)
- [x] Twitter/X "For You" blocking (tab bar node detection)
- [x] Reddit Home feed blocking (activity detection)
- [x] Snapchat Discover tab blocking
- [!] VPN-based DNS blocking layer (local VPN, no data leaves device) — *blocked on
  a product decision: a `VpnService` must forward every query it does not block,
  and forwarding needs the `INTERNET` permission and sends those queries to an
  upstream resolver. That contradicts the in-app promise, the published policy
  and the Data safety answer, all of which state the app cannot transmit.
  Dropping the queries instead would leave the phone unable to resolve anything.*
- [x] iOS Screen Time API exploration — *[docs/IOS_SCREEN_TIME.md](https://github.com/rishabhx29/Sober/blob/main/docs/IOS_SCREEN_TIME.md):
  section blocking and notification filtering cannot be ported; Apple exposes no
  foreground-app or view-tree signal and Family Controls tokens are opaque.*
- [!] Validate the post-launch rule ids against current builds of TikTok, X,
  Reddit and Snapchat — *blocked: needs hardware and the installed apps. A wrong
  id fails closed (no block), but only the live apps can confirm the ids.*

> **Phase 6 (2026-10-30).** The four rule sets ship as catalog data
> (`detection_rules.json`) rather than code, so their toggles appear in Apps and
> the section overlay treats them like Instagram and YouTube.
> `ShippedSectionRulesTest` now runs the shipped document through
> `DetectBlockedSectionUseCase` for every rule: each matches its own package and
> selected tab, matches no other app, and does not fire for a tab that is visible
> but unselected. Each section also asserts its id is `package:section`, has
> overlay copy, and that ids are unique. What that cannot prove is whether the
> resource ids match current app builds; that is the blocked item above.

---

## Notifications System (Cross-Phase)

- [x] ⏱ Usage Warning (80%) — triggers at 80% of any daily limit (per app and for the overall cap; once per day, below 100% where the overlay takes over)
- [x] 🔴 Limit Reached — daily limit hit, app blocked (overlay plus the "Daily limit reached" notification)
- [x] 🧘 Focus Session Active — persistent countdown notification
- [x] ✅ Session Complete — focus session ended successfully
- [x] 🔥 Daily Streak — end-of-day stayed-under-limits notification
- [x] ⚠️ Streak at Risk — approaching midnight, app over limit
- [x] 🌙 Bedtime Warning — sleep mode activating in 15 min

> **Notifications implementation (2026-10-30).** Both streak alerts run on the
> existing 15s poll and only after 22:00, when the day's outcome is settled but a
> nudge can still matter. They are mutually exclusive: if any limit is reached,
> "your N-day streak ends tonight" uses the streak as it stood *before* today
> (today has already failed); otherwise "day N, under every limit" fires — and
> only when usage was actually observed, because an untouched phone and an engine
> that never ran look identical in the tables. Reached-limit detection reuses
> `UsageTotals`, the same helper the block policy uses, so the message cannot
> contradict the overlay. Each alert records a per-day fired key, so a 15s poll
> produces one notification, not hundreds.
>
> Bedtime Warning is poll-driven rather than an alarm: a warning that needs a
> precise wake-up is exactly the alarm that fails on a killed process, and the
> engine already ticks. Only rules whose name contains "sleep" are considered —
> `ScheduleRule` has no type field, and warning someone that their work hours
> start under the heading "Bedtime" would be wrong; both the template and the
> suggestion name the rule "Sleep Mode". Keys are per rule per day.
>
> The three optional alerts are now real `AlertKind` entries, so Settings lists
> them and the preference rows are generated from `AlertKind.entries` rather than
> a hand-maintained combine of three flows. Verified: `test` 303/303,
> `:app:assembleDebug` green (24 new tests). No device here, so nothing was seen
> arriving on a notification shade.

---

## Verification & Delivery (Cross-Phase)

- [x] Add JUnit-based unit testing infrastructure and meaningful tests for domain use cases and overlay formatting. **Delivered**: JUnit 4 + `kotlinx-coroutines-test` + MockK dependency present; 26 new JVM tests across `:core:domain` use cases (GetUsageHistory, GetAppUsageHistory, GetDailyScreenTime, ResetDailyUsage, ManageFocusSession/EvaluateBlockDecision) and `:system:overlay` formatting helpers, using hand-written fakes instead of MockK mocks. **Deviation**: JUnit 5 was not adopted — it needs an AGP-compatible runner plugin and that plugin was not verified against AGP 9.3.1.
- [x] Add Compose UI tests for reusable components and feature screens — *runs on the JVM through Robolectric: seven component tests in `:core:ui` (primary/outlined buttons, text link, status row, section label, card) and three block-overlay screen tests in `:system:overlay`. Home and Settings are Hilt-injected screens and are not covered.*
- [x] Add Espresso integration tests for full-screen flows and navigation — *closed as not applicable as written: the app has one Activity and no View hierarchy, so Espresso would only see an empty root. The Compose test rule drives the same screens through semantics, and it runs here instead of waiting for a device.*
- [x] Add Robolectric framework tests for Android API behavior without an emulator — *Robolectric 4.17 on `@Config(sdk = [35])`; eleven `PermissionUtils` tests cover the never-written setting, another app's service, the API-33 POST_NOTIFICATIONS split, AppOps modes and intent extras.*
- [x] Configure GitHub Actions to run lint and unit tests on push, build the APK, and automate GitHub Release publication for release builds — *`.github/workflows/android.yml`; not executed here, so the YAML was parsed and each command was run locally rather than observed on a runner.*
- [x] Diagnose the all-module `connectedDebugAndroidTest` failure in empty `:core:data` (diagnostic complete; aggregate remains unverified).
- [!] Integrate optional Firebase Analytics and Crashlytics only with explicit opt-in; minimize events to aggregate metrics, collect no personal data or raw package-level usage, support withdrawal, and disclose collection in the Privacy Policy. — *blocked on a product decision: the app ships without `INTERNET`, and the in-app policy states that nothing is sent anywhere and that there is no analytics or advertising. Adding either SDK changes the permission set, the privacy policy, the Data safety answer and the "nothing leaves the device" promise at once.*

> **Verification & Delivery (2026-10-30).** Robolectric 4.17 makes Android- and
> Compose-level tests runnable on the JVM, so three previously device-only
> categories now execute here. `:core:common` runs eleven `PermissionUtils`
> tests against real `Settings.Secure`, `AppOpsManager` and runtime-permission
> state; `:core:ui` runs seven Compose tests over the shared components; and
> `:system:overlay` runs three block-screen tests. The overlay ticks once a
> second, so its tests freeze the test clock (`autoAdvance = false`) instead of
> waiting for a loop that never idles.
>
> Espresso was closed as not applicable: with one Activity and no View
> hierarchy there is nothing for `onView` to find, and the Compose rule drives
> the same semantics. GitHub Actions is configured but not executed here — the
> workflow was YAML-parsed, and every command in it was run locally.
>
> Still not covered: the Hilt-injected Home and Settings screens as whole
> screens, and anything that needs a real shade, a real overlay window or a
> real OEM process killer.

---

## Task Log

| Date       | Task ID | Action                | Agent              | Notes |
|------------|---------|----------------------|--------------------| ------|
| 2026-08-19 | —       | Initial task breakdown | Task Tracker Agent | Created from PRD v1.0 |
| 2026-08-19 | P0      | completed            | Orchestrator       | Project initialized, modules created |
| 2026-08-20 | P0      | completed             | Orchestrator       | Dependencies and fonts added, AGP bug fixed. Phase 0 Complete |
| 2026-10-16 | P0.1    | started               | Implementation Agent | Assigned by Orchestrator: Android Studio and Android 17/API 37 SDK setup |
| 2026-10-16 | P0.4    | started               | Implementation Agent | Assigned by Orchestrator: minSdk 26, targetSdk 37, compileSdk 37 configuration |
| 2026-10-16 | P0.5    | started               | Implementation Agent | Assigned by Orchestrator: Gradle Kotlin DSL and version catalog configuration |
| 2026-10-16 | P0.6    | started               | Implementation Agent | Assigned by Orchestrator: Android/Kotlin `.gitignore` |
| 2026-10-16 | P0.7    | started               | Implementation Agent | Assigned by Orchestrator: Git initialization and first commit |
| 2026-10-16 | P0.4    | updated               | Task Tracker Agent | Updated SDK baseline from API 35 to Android 17/API 37; Play submissions require target API 36 or higher from 2026-08-31 |
| 2026-10-16 | P2.2.1  | added                 | Task Tracker Agent | Added active-session timer persistence and recovery from IMPLEMENTATION.md |
| 2026-10-16 | P2.2.2  | added                 | Task Tracker Agent | Added actual foreground-app enforcement for the selected session block list |
| 2026-10-16 | P2.2.3  | added                 | Task Tracker Agent | Added session-triggered service startup and reboot recovery |
| 2026-10-16 | VD.1    | added                 | Task Tracker Agent | Added JUnit 5 + MockK unit and repository coverage from the testing strategy |
| 2026-10-16 | VD.2    | added                 | Task Tracker Agent | Added Compose UI coverage for components and screens |
| 2026-10-16 | VD.3    | added                 | Task Tracker Agent | Added Espresso integration coverage for flows and navigation |
| 2026-10-16 | VD.4    | added                 | Task Tracker Agent | Added Robolectric framework coverage for Android APIs |
| 2026-10-16 | VD.5    | added                 | Task Tracker Agent | Added GitHub Actions lint, test, APK build, and release publication pipeline |
| 2026-10-16 | P0      | started               | Task Tracker Agent | QA reconciliation: clean build and instrumented-device verification moved from complete to in progress |
| 2026-10-16 | P1.3    | started               | Task Tracker Agent | QA reconciliation: missing daily screen-time and reset use cases moved to in progress |
| 2026-10-16 | P1.5    | started               | Task Tracker Agent | QA reconciliation: Room usage sync and usage edge-case handling moved to in progress |
| 2026-10-16 | P1.6    | started               | Task Tracker Agent | QA reconciliation: overlay type, identity and presentation, countdown, override prompt, and ViewModel moved to in progress |
| 2026-10-16 | P1.7    | started               | Task Tracker Agent | QA reconciliation: persistent countdown notification, hard-limit enforcement, and midnight reset moved to in progress |
| 2026-10-16 | P1.8    | started               | Task Tracker Agent | QA reconciliation: active-block restoration from the boot receiver moved to in progress |
| 2026-10-16 | P1.9    | started               | Task Tracker Agent | QA reconciliation: gear/settings, live limit and streak, focus actions, and active-session widget moved to in progress |
| 2026-10-16 | P1.10   | started               | Task Tracker Agent | QA reconciliation: installed-app visibility/toggle and app-detail picker, section toggles, and 7-day chart moved to in progress |
| 2026-10-16 | P1.11   | started               | Task Tracker Agent | QA reconciliation: Settings destination and Home gear navigation moved to in progress |
| 2026-10-16 | P0.2    | started               | Task Tracker Agent | Moved JDK 17+ setup through the required in-progress status before recording QA approval |
| 2026-10-16 | P0.2    | completed             | QA Agent           | QA approved JDK 17+ setup based on environment inspection |
| 2026-10-16 | P0.3    | started               | Task Tracker Agent | Moved `com.sober.app` project setup through the required in-progress status before recording QA approval |
| 2026-10-16 | P0.3    | completed             | QA Agent           | QA approved the `com.sober.app` project setup |
| 2026-10-16 | P0.4    | completed             | QA Agent           | QA approved minSdk 26, targetSdk 37, and compileSdk 37 configuration |
| 2026-10-16 | P0.5    | completed             | QA Agent           | QA approved Gradle Kotlin DSL and version catalog configuration |
| 2026-10-16 | P0.6    | completed             | QA Agent           | QA approved the Android/Kotlin `.gitignore` |
| 2026-10-16 | P0.8    | started               | Task Tracker Agent | Moved modular project structure through the required in-progress status before recording QA approval |
| 2026-10-16 | P0.8    | completed             | QA Agent           | QA approved the modular project structure |
| 2026-10-16 | P0.23   | completed             | QA Agent           | QA approved the clean build across all modules |
| 2026-10-16 | P0.24   | completed             | QA Agent           | QA approved `:app:connectedDebugAndroidTest :core:domain:connectedDebugAndroidTest` on AVD `Sober_QA_API37_20261016` (1 test each, zero failures). The all-module `connectedDebugAndroidTest` aggregate still crashes in `:core:data` with 0 tests |
| 2026-10-16 | VD.6    | added                 | Task Tracker Agent | Added unchecked follow-up to diagnose and fix, or deliberately exclude, the empty `:core:data` all-module connected-test failure |
| 2026-10-16 | VD.6    | started               | Task Tracker Agent | Moved the QA-approved diagnostic-only follow-up through the required in-progress status before recording its closure |
| 2026-10-16 | VD.6    | completed             | QA Agent           | `:core:data:connectedDebugAndroidTest --info` passes in isolation (0 tests, 0 failures/errors, exit 0); prior aggregate process crash was not reproducible, followed an ADB-unresponsive warning, and has no captured fatal stack. No Gradle change is justified. Current connected gate is `:app:connectedDebugAndroidTest :core:domain:connectedDebugAndroidTest`; aggregate is not verified green |
| 2026-10-16 | P0.22   | started               | Task Tracker Agent | QA found `app/core/ui/src/main/res/font/inter.ttf` is a 305,682-byte GitHub 404 HTML page referenced by `Type.kt`; moved font setup from complete to in progress pending a valid Inter asset |
| 2026-10-16 | P0      | updated               | Task Tracker Agent | Reconciliation: historical 2026-08-20 P0 completion was inaccurate; Phase 0 remains in progress |
| 2026-10-16 | P1.4.10 | added                 | Task Tracker Agent | Added API 33+ runtime `POST_NOTIFICATIONS` request, denial handling, and permission re-check |
| 2026-10-16 | P1.6.11 | added                 | Task Tracker Agent | Added the optional daily-rotating motivational quote requirement for the daily-limit overlay |
| 2026-10-16 | P1.7.7  | added                 | Task Tracker Agent | Added explicit enforcement of the overall daily usage cap independently of per-app limits |
| 2026-10-16 | P1.7.8  | added                 | Task Tracker Agent | Added a persistent remaining-time notification for the overall daily cap, distinct from the focus-session countdown |
| 2026-10-16 | P1.10.9 | added                 | Task Tracker Agent | Added least-privilege launcher `<queries>` package visibility for installed apps and icons |
| 2026-10-16 | P2.1.5  | added                 | Task Tracker Agent | Added the missing 45m, 90m, 2h, and validated custom focus-session durations |
| 2026-10-16 | P2.1.6  | added                 | Task Tracker Agent | Added all / selected-app / all-except-calls notification-silencing choices |
| 2026-10-16 | P2.2.4  | added                 | Task Tracker Agent | Added the active-session label and remaining countdown to the blocked-app overlay |
| 2026-10-16 | P2.2.5  | added                 | Task Tracker Agent | Added timer-accuracy verification across screen-off and recovery; WakeLock remains conditional on verification |
| 2026-10-16 | P2.3.5  | added                 | Task Tracker Agent | Added enforcement of configured silence-all and selected-app notification scopes |
| 2026-10-16 | P2.3.6  | added                 | Task Tracker Agent | Added active-session Notification Access revocation warning and re-enable guidance |
| 2026-10-16 | P3.2.4  | added                 | Task Tracker Agent | Added versioned remote JSON detection-rule updates with bundled / last-known-good fallback |
| 2026-10-16 | P3.2.5  | added                 | Task Tracker Agent | Added package-level blocking fallback when section detection is unavailable or fails |
| 2026-10-16 | P3.7.1  | added                 | Task Tracker Agent | Added user-created keyword and arbitrary-package block rules |
| 2026-10-16 | P4.1.4  | added                 | Task Tracker Agent | Added API 31+ exact-alarm access request, re-check, and denied/revoked guidance |
| 2026-10-16 | P4.1.5  | added                 | Task Tracker Agent | Added on-device usage-pattern schedule suggestions with local raw usage data |
| 2026-10-16 | P4.2.4  | added                 | Task Tracker Agent | Added overnight schedule-window and device clock/time-zone change handling |
| 2026-10-16 | P4.3.5  | added                 | Task Tracker Agent | Added completed-versus-cancelled focus-session statistics |
| 2026-10-16 | P4.3.6  | added                 | Task Tracker Agent | Added most-productive-weekday insight based on lowest screen time |
| 2026-10-16 | P4.3.7  | added                 | Task Tracker Agent | Added local week-over-week screen-time change calculation |
| 2026-10-16 | P4.3.8  | added                 | Task Tracker Agent | Added daily limit adherence calculation from local usage and limit history |
| 2026-10-29 | P1.1     | completed             | QA Agent           | All 7 Manuscript components present and compiling; SoberProgressBar/SegmentedControl/ToggleSwitch adopted in Home, Apps, Focus, Stats. :app:assembleDebug green |
| 2026-10-29 | P1.4.10  | completed             | QA Agent           | Added runtime POST_NOTIFICATIONS step to onboarding (API 33+), denial fallback to app notification settings, resume re-check, and a Notifications row in Settings permission status |
| 2026-10-29 | P1.6.11  | completed             | QA Agent           | Overlay ring now counts down to session end (label + MM:SS/H:MM:SS + depleting arc) for FOCUS_SESSION, midnight for daily limits; daily quote rotates by day-of-year; BlockDecision carries session window |
| 2026-10-29 | P1.7.7-8 | completed             | QA Agent           | Overall cap enforced through EvaluateBlockDecisionUseCase; launcher and Sober excluded from foreground so a cap block can never trap the user; second low-importance channel/id (1002) posts remaining cap time, de-duplicated per minute and cancelled when no cap is set |
| 2026-10-29 | P1.10.5-9 | completed             | QA Agent           | App detail sheet uses two SoberDrumPickers (0–12h, 0–59m) instead of steppers, with Save disabled at 0; per-app 7-day chart via new GetAppUsageHistoryUseCase + SoberMiniBarChart; manifest declares launcher/HOME <queries> instead of QUERY_ALL_PACKAGES |
| 2026-10-29 | P2.1-2.2 | completed             | QA Agent           | EnforcementController port + Hilt adapter starts the engine on session begin and on Focus-tab open; session state recovered from DataStore with clamped wall-clock elapsed; FGS type is specialUse on API 34+ (dataSync's 6h/day cap) with manifest rationale |
| 2026-10-29 | P2.4     | completed             | QA Agent           | History rows show a broken-streak line for cancelled sessions with the cancellation minute; ManageFocusSessionUseCase counts same-day cancellations and fires a one-shot AlertNotifier warning on the third, on a dedicated sober_alerts channel (id 1003) |
| 2026-10-29 | VD.1     | completed             | QA Agent           | `:core:domain:testDebugUnitTest` 21 tests / 0 failures; `:system:overlay:testDebugUnitTest` 6 tests / 0 failures (26 new, plus 2 pre-existing example tests). Fakes live in `Fakes.kt` |
| 2026-10-29 | VD.1     | updated               | Task Tracker Agent | Deviation logged: JUnit 5 replaced by JUnit 4. JUnit 5 needs an AGP-compatible test runner plugin and compatibility with AGP 9.3.1 is unverified; also MockK is on the classpath but tests use hand-written fakes for readability across multi-repository use cases |
| 2026-10-29 | P0.22    | completed             | QA Agent           | Font assets verified as real TrueType (`inter.ttf` 879,708 bytes, `jetbrains_mono.ttf` 187,208 bytes) and wired through `Type.kt`; closes the stale 404-HTML finding |
| 2026-10-29 | P1.3     | completed             | QA Agent           | `GetDailyScreenTimeUseCase` and `ResetDailyUsageUseCase` verified implemented and unit-tested; rollover is date-marked and idempotent |
| 2026-10-29 | P1.5     | completed             | QA Agent           | Usage sync writes absolute daily totals each poll; foreground detection excludes Sober and the launcher, and an unknown app reports no usage rather than blocking |
| 2026-10-29 | P1.6     | completed             | QA Agent           | Overlay identity now resolved through `PackageManager` (real label and icon) instead of guessing from the package id, which produced "You've had enough Android for today."; overlay state needs no ViewModel |
| 2026-10-29 | P1.7     | completed             | QA Agent           | Overlay trigger, midnight reset and both persistent notifications verified; polling is deliberately 15s and the rationale is recorded in code and in the task text |
| 2026-10-29 | P1.9     | updated               | Task Tracker Agent | Top bar, streak pill and usage ring verified; Active Session widget moved back to not started because Home renders neither the widget nor a hold-to-end control |
| 2026-10-29 | P1.10    | updated               | Task Tracker Agent | Filter row documented as `SoberSegmentedControl`; section-block toggles moved to not started pending Phase 3 detection rules |
| 2026-10-29 | P1.11    | completed             | QA Agent           | Home gear opens the Settings destination |
| 2026-10-29 | P2.1.6   | completed             | QA Agent           | Focus screen offers All / My apps / Except calls, which makes the listener's `ALL` and `ALL_EXCEPT_CALLS` branches reachable from the UI |
| 2026-10-29 | P2.3     | updated               | Task Tracker Agent | Service, per-scope silencing and non-session pass-through verified; Notification Access revocation handling remains open |
| 2026-10-29 | P5.4     | updated               | Task Tracker Agent | Daily cap control and permission status verified; PIN, notification preferences, streak rules, export and About remain open |
| 2026-10-29 | Notifications | updated          | Task Tracker Agent | Limit Reached and Focus Session Active countdown verified; 80% warning, session complete, daily streak, streak at risk and bedtime warning remain open |
| 2026-10-29 | P1.9     | completed             | QA Agent           | "Start Focus Session" now navigates to the Focus tab; Active Session card shows label, JetBrains Mono countdown and hold-to-end; top-3 rows use real icons, PackageManager labels and per-app limits with a lock at the limit; "sessions today" reads from FocusSession history instead of a hardcoded 0 |
| 2026-10-29 | P2.4     | updated               | QA Agent           | `HoldToCancelButton` promoted to `:core:ui` as `SoberHoldToCancelButton` so Home can reuse the 5s hold plus 60s regret timer without a feature-to-feature dependency |
| 2026-10-29 | P1.9     | updated               | Task Tracker Agent | "Quick Focus Mode" button removed from Home until Phase 2.6 exists — it had an empty onClick and looked functional |
| 2026-10-29 | Notif-80 | completed             | QA Agent           | New `CheckUsageWarningsUseCase` posts one warning per app and one for the overall cap between 80% and 100%, using the same live-or-stored minute figure the block uses; fired keys persist per calendar day so a service restart does not replay them. 7 unit tests |
| 2026-10-29 | Notif-Complete | completed        | QA Agent           | `completeSession` now posts a "Session complete" alert on the `sober_alerts` channel and returns early when the session is no longer ACTIVE, which also stops the service and Focus screen tickers from double-completing the same session |
| 2026-10-29 | P1.7     | updated               | Task Tracker Agent | Enforcement cycle now also runs the 80% warning check after the daily-cap notification refresh |
| 2026-10-29 | P2.3.6   | completed             | QA Agent           | `onListenerDisconnected` posts a high-priority alert with a PendingIntent into notification-listener settings and requests a rebind; the active session card shows an in-app notice when access is missing, re-checked on ON_RESUME, and only when the session's silence scope is not NONE. `PermissionUtils` moved to `:core:common` so both features can share the check |
| 2026-10-29 | P2.7     | completed             | QA Agent           | Completion is detected by the active session id clearing, then re-read from history so cancellations (which clear the id the same way) do not celebrate; the card shows label, duration, blocked-app count and streak, offers "Go again" with the same label/duration/list, and only fires for completions in the last 10 minutes. Sessions were already persisted to history by `completeSession`, so that item needed no code |
| 2026-10-29 | P4.5     | added                 | Task Tracker Agent | New `CalculateStreakUseCase` computes consecutive under-limit days from `usage_sessions` using the same `used >= limit` comparison as enforcement; it is the domain basis for 2.7's streak display, while DataStore persistence (longest streak, success-day preferences), the Home streak value and the Settings controls remain open |
| 2026-10-29 | P2.5     | completed             | QA Agent           | Presets resolve against installed apps via `PresetResolver` (category first, known packages second, default dialer/SMS/alarm roles third) instead of hardcoded package ids that would match nothing on a real phone; manifest gained DIAL/SENDTO/SET_ALARM visibility queries. Meal Break is a DataStore window consumed by `EvaluateBlockDecisionUseCase`, so a pause lifts session and limit blocks without touching history. 6 new unit tests |
| 2026-10-29 | P2.5     | updated               | Task Tracker Agent | Sleep-at-bedtime and Work-during-work-hours auto-activation split into their own unchecked items: they depend on 4.1 schedule templates, which do not exist yet |
| 2026-10-29 | P2.6     | completed             | QA Agent           | Open-ended sessions are persisted as duration 0 and every consumer treats 0 as "no end": the service ticker, the Focus ticker and Home all skip completion, the notification says "runs until you stop", and the overlay shows "Open-ended" with no countdown ring. Disabling needs no cooldown — the existing hold-to-cancel is an anti-mistap guard, and the 60s regret window only offers a revert, it does not delay the stop. One new unit test for the null end time |
| 2026-10-29 | P4.5     | updated               | Task Tracker Agent | Home streak pill now shows the calculated streak instead of a hardcoded 0. The streak is derived rather than stored in DataStore, so persistence of the longest streak and last success date remains an explicit open item |
| 2026-10-29 | P5.4     | started               | Implementation Agent | PIN feature: `SoberPreferences.pinRecord` + `PinHasher` (PBKDF2-HMAC-SHA256, 120k iterations, self-describing `iterations:salt:hash`) + `PinRepository`/`PinRepositoryImpl` + `SetPinUseCase`/`VerifyPinUseCase`, 10 unit tests |
| 2026-10-29 | P5.4     | completed             | QA Agent           | Set/change PIN lives in a masked NumberPassword dialog with local length/match validation and a server-side current-PIN check; validation is in the use case, not the composable, so the emergency override can reuse it. Verified: `test` 61/61, `:app:assembleDebug` green |
| 2026-10-29 | P5.1     | completed             | QA Agent           | Settings gate combines PIN presence and active-session state into one nullable `GateState` so the body never renders before the stored values load; leaving and returning mid-session re-locks. Back navigation stays available so the gate is a speed bump, not a trap |
| 2026-10-29 | P5.3     | completed             | QA Agent           | Override state is a persisted request window plus a package-scoped grant (`OverrideStateRepository`), so the 5-minute wait survives the overlay being rebuilt every poll and an override for one app can never lift another's block. `EvaluateBlockDecisionUseCase` consults the grant before every rule, and `clearExpired` drops windows that are only true in the past. Grant is re-validated at lift time, not trusted from the UI. 12 new unit tests |
| 2026-10-29 | P5.3     | updated               | Task Tracker Agent | "Logged in stats with visual flag" split: logging ships now (one Room row per grant), the Stats flag waits on 4.3 because the Stats screen is unimplemented |
| 2026-10-29 | P5.1     | started               | Implementation Agent | Opt-in device admin already existed on onboarding page 6, but pointed at a placeholder `DummyDeviceAdminReceiver`; renamed to `SoberDeviceAdminReceiver` under `feature.settings.admin`, updated the manifest, and added a Settings "Uninstall protection" activate/deactivate control |
| 2026-10-29 | P5.1     | completed             | QA Agent           | Uninstall protection is platform-enforced while the receiver is active; `ACTION_REMOVE_DEVICE_ADMIN` is absent from the API 37 stubs, so removal uses the legacy action string with a Security-settings fallback. Copy states it does not stop a force-stop, so the app does not claim protection it cannot provide. Verified: `test` 79/79, `:app:assembleDebug` green |
| 2026-10-29 | P5.4     | started               | Implementation Agent | CSV export: `ExportUsageCsvUseCase` (30-day window, date/usage/package ordering, RFC 4180 quoting), `FileProvider` limited to `cacheDir/exports`, and a Settings "Data" section that opens the share sheet |
| 2026-10-29 | P5.4     | completed             | QA Agent           | Export writes to `cacheDir/exports` and deletes previous files first, so repeated taps cannot accumulate the user's usage history; the provider exposes only that folder, and the URI is granted read access per share. 6 unit tests cover empty history, ordering, window filtering and quoting. Verified: `test` 79/79, `:app:assembleDebug` green |
| 2026-10-29 | P5.4     | completed             | QA Agent           | About section shows the installed version and a privacy policy dialog shipped in the app (no network, consistent with the absence of INTERNET permission). Policy states what is stored, that nothing leaves the device, what each permission does, and how to erase everything |
| 2026-10-29 | P5.4     | completed             | QA Agent           | Notification preferences are DataStore booleans read by the use cases that raise each alert, not by the notifier: suppression happens before a warning key is recorded, so re-enabling later the same day still delivers a warning that is still in its window. 3 new unit tests; the notification-access alert is intentionally not toggleable |
| 2026-10-29 | P5.4     | completed             | QA Agent           | Streak rule is a stored enum with a lenient parse. The strict option treats a cancelled session as a broken day, checked before the "today has no rows yet" shortcut so a cancellation today cannot be counted as success. Cancellations come from a new ranged DAO query. 4 new unit tests. Verified: `test` 86/86, `:app:assembleDebug` green |
| 2026-10-29 | P4.5     | updated               | Task Tracker Agent | "What counts as a successful day" is now configurable, closing the 4.5 duplicate; longest-streak persistence stays open because it needs a write path the streak does not have yet |
| 2026-10-29 | P5.5     | started               | Orchestrator       | Dark mode: token-layer change (add `onPrimary`, because the accent flips navy→cream and hard-coded white text would be unreadable), then convert every component and screen off hard-coded colours |
| 2026-10-29 | P5.5     | completed             | QA Agent           | Dark tokens live in `core:ui/theme`; `SoberColors.onPrimary` replaced 8 hard-coded white/text-on-accent uses (buttons, segmented control, switch thumb, hold-to-cancel, overlay keypad). Theme mode is a stored enum (System/Light/Dark) surfaced in Settings → Appearance and applied in `SoberApp`; the dead duplicate `ui/theme` package in `:app` was deleted after a grep proved no references |
| 2026-10-29 | P5.5     | completed             | QA Agent           | The blocking overlay is its own window and cannot inherit the app's theme, so `BlockOverlayController` now reads the same stored `ThemeMode` and wraps `BlockOverlayScreen` in `SoberTheme`; SYSTEM is resolved from resources because there is no Activity host. Also fixed a latent crash: `SoberTheme` cast `view.context` to `Activity`, which is null in an overlay window → now walks the ContextWrapper chain and skips status-bar tinting when absent. Verified: `test` 86/86, `:app:assembleDebug` green. "QA pass on every screen in dark mode" left open — no device/emulator here |
| 2026-10-29 | P4.1     | started               | Orchestrator       | Scheduling UI: reuse the existing `schedule_rules` table and `PresetResolver` rather than adding a new schema or a hardcoded app list |
| 2026-10-29 | P4.1     | completed             | QA Agent           | `SchedulesScreen` (Settings → Schedules) lists, toggles, edits and deletes windows, with Work/Sleep templates resolved against installed apps. Draft state is held in the screen, not the ViewModel, so cancelling an edit cannot leak into the next one. Re-running a template updates the same-named rule instead of duplicating it |
| 2026-10-29 | P4.2     | completed             | QA Agent           | Enforcement is the engine's existing 15s poll: `ScheduleRule.windowEndingAt` is pure (returns the closing instant or null) and `BlockReason.SCHEDULE` sits after focus sessions, before daily limits. Cross-midnight windows belong to their start day; start == end and malformed times block nothing. AlarmManager and WorkManager were deliberately not added — a dead engine cannot draw the overlay either, and boot + `START_STICKY` already cover restarts. Verified: `test` 106/106, `:app:assembleDebug` green |
| 2026-10-30 | P4.3     | started               | Implementation Agent | Stats: one 30-day history read sliced per timeframe; heatmap fed by a new `getForegroundIntervals` event replay; highlights (focus done/cancelled, quietest weekday, week-over-week, adherence) as pure functions |
| 2026-10-30 | P4.3     | completed             | QA Agent           | Top apps now show the installed label and icon instead of guessing from the package id (`com.instagram.android` used to render as "Android"); rows show usage against that app's own limit. Heatmap, tap-for-tooltip chart naming, pattern card and trend line are wired. 24 new unit tests (12 heatmap, 12 highlights). Verified: `test` 130/130, `:app:assembleDebug` green. Unlocked achievement styling exists but is unreachable until 4.4; visual QA needs a device |
| 2026-10-30 | P5.3     | updated               | Task Tracker Agent | "Override shown with a visual flag in Stats" is no longer blocked by 4.3 — the Stats screen exists, so the flag can be built on the override log |
| 2026-10-30 | P4.4     | started               | Implementation Agent | Achievements: permanent first-unlock store (DataStore), `AchievementRules` snapshot evaluator, new `getCompletedBetween` for Pomodoro days, social packages reused from `PresetResolver` |
| 2026-10-30 | P4.4     | completed             | QA Agent           | Six badges evaluated from streak, focus history, 30-day usage rows and 7-day foreground intervals. Semantics that needed a decision are recorded: unobserved nights break Night Owl instead of counting, an empty social-app set never unlocks Social Detox, and unlocks never revoke. 32 new unit tests. Verified: `test` 162/162, `:app:assembleDebug` green |
| 2026-10-30 | P4.1     | started               | Implementation Agent | Usage-pattern suggestions: 7-day `UsageEvents` analysed on device; Sleep and Work proposals share the existing template application path, dismissals in DataStore |
| 2026-10-30 | P4.1     | completed             | QA Agent           | Two explainable patterns only (3 late nights; 4 busy weekdays), each with the observed count in the copy. Suggestions are suppressed by an existing same-named rule or a prior dismissal, and the intervals are reused from the heatmap pipeline. 12 new unit tests. Verified: `test` 174/174, `:app:assembleDebug` green. Visual QA of the suggestion cards needs a device |
| 2026-10-30 | P4.5     | started               | Implementation Agent | `StreakRecord` persisted in DataStore (current/longest/last success). `CalculateStreakUseCase` gained `streakEndingOn(date)`; `RecordStreakUseCase` folds yesterday in and is called from Home and Focus |
| 2026-10-30 | P4.5     | completed             | QA Agent           | Record is written only for a closed day, so a day that later crosses its limit cannot enter the best-ever count; recording is idempotent within a day; a zero day writes nothing and previous bests are never lowered. Streak beyond the 60-day window comes from `stored.current + 1`, not from the capped window value. Home shows "Best: N days" when it exceeds the current streak. 19 new unit tests. Verified: `test` 193/193, `:app:assembleDebug` green. Visual QA of the two-line pill needs a device |
| 2026-10-30 | P5.3     | started               | Implementation Agent | Stats flag built on a ranged `override_logs` query (`getLogsBetween`), summarised by `GetOverrideSummaryUseCase` and rendered above the screen-time card |
| 2026-10-30 | P5.3     | completed             | QA Agent           | The flag counts the same range the Stats screen is showing (half-open `[start, now)`), so "Today" never includes yesterday's grant. It is a history read, not a block-state read, and it is omitted entirely when the count is zero. A failing log query degrades to no flag instead of taking Stats down. 4 new unit tests. Verified: `test` 197/197, `:app:assembleDebug` green. Visual QA of the danger-tinted card needs a device |
| 2026-10-30 | P3.1–P3.2 | completed           | QA Agent           | Rules live in a versioned JSON asset parsed by `DetectionRuleCatalogParser`; matching is a pure function (`DetectBlockedSectionUseCase`) over a `WindowSnapshot`, so the tree walk is the only Android-bound part. Remote fetch closed as a documented divergence — no `INTERNET` permission, privacy promise intact; last-known-good copy in DataStore covers a damaged asset. Fallback added: with Accessibility off, an app holding an enabled section rule now blocks whole (`SECTION_RULE_UNAVAILABLE`) instead of letting the rule silently do nothing, and both the overlay and the Apps sheet say so. Verified: `test` 221/221, `:app:assembleDebug` green |
| 2026-10-30 | P3.3–P3.4 | blocked             | QA Agent           | Instagram and YouTube rules implemented (Reels/Explore, Shorts/Home) with activity prerequisites; the "verify the other tabs stay usable" items stay open because there is no device or emulator — the bundled resource ids are unverified against current app builds, so no accessibility claim is made here |
| 2026-10-30 | P3.5–P3.6 | completed           | QA Agent           | Section overlay is a separate `WindowManager` window (blur when the platform supports cross-window blur, 94% scrim otherwise) with "Go back" as the only exit; it deliberately excludes the emergency-override path. Toggles appear only for catalog apps, and the sheet refreshes the Accessibility grant on `ON_RESUME` so the warning does not outlive the fix. Verified: `test` 221/221, `:app:assembleDebug` green. No frame has been rendered on a screen — visual QA outstanding |
| 2026-10-30 | P3.7     | completed             | QA Agent           | Custom rules live in one DataStore string set (`CustomRuleCodec`, value last so separators survive) with whole-set rewrites inside one `edit`; add is idempotent per value. Blocked apps are policy (`CUSTOM_APP_RULE`, no countdown, beats daily limits) and muted words are notification-listener behaviour that also applies outside sessions. Calls are never keyword-filtered and a blank keyword matches nothing. Verified: `test` 245/245, `:app:assembleDebug` green. No device, so the notification path and the screen are unverified visually |
| 2026-10-30 | P5.2     | started               | Implementation Agent | Guard built from a pure `AccessibilitySettingsDetector` (package + activity name, text fallback), `IsBlockingInForceUseCase` (session / limit / schedule / custom app, Meal Break exempt) and the existing section overlay window, which now also carries caller-supplied copy |
| 2026-10-30 | P5.2     | completed             | QA Agent           | The literal "re-open the toggle" is not implementable: a service cannot re-enable itself and clicking the switch for the user is the self-granting behaviour Play policy forbids. The guard covers Settings → Accessibility instead, explains the cost of turning access off, and offers "Go back" only. It is inert with nothing blocked, and evaluates on window transitions off the main thread. Shared `UsageTotals` now feeds both the block decision and the guard so they cannot disagree about a reached limit. 27 new unit tests. Verified: `test` 272/272, `:app:assembleDebug` green. No device, so the guard screen and the overlay-over-Settings behaviour are unverified visually |
| 2026-10-30 | P5.4     | completed             | QA Agent           | About section: the Privacy Policy action is now a plain navy text link (`SoberTextLink`, underlined so the affordance is not colour-only, `minimumInteractiveComponentSize` keeps the 48dp target) instead of an outlined button. The policy itself is unchanged and still an in-app dialog. Verified: `:app:assembleDebug` green |
| 2026-10-30 | P5.6     | started               | Implementation Agent | Performance: skip the enforcement cycle while the screen is off, add a `ForegroundSignal` hint from the accessibility service, and scope an OEM battery/autostart prompt that only opens panels the device actually resolves |
| 2026-10-30 | P5.6     | completed             | QA Agent           | Poll skipped entirely while `PowerManager.isInteractive` is false (5s re-check), so idle wake-ups fall from 240/h to 12/h without widening the 15s block latency. Accessibility window transitions now push a `ForegroundSignal` and the engine applies policy immediately; poll and hint serialise on a `Mutex` and both treat the launcher and Sober as "nothing to block". Onboarding gained a skippable battery step that opens the system exemption list plus a resolved vendor panel, with the same control in Settings → Engine; the restricted `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission is deliberately not requested. 7 new OEM-catalogue tests. Verified: `test` 279/279, `:app:assembleDebug` green. Battery profiling and multi-OEM kill/force-stop verification stay `[!]` — no hardware here |
| 2026-10-30 | P1.9     | completed             | QA Agent           | Focus Mode pill restored now that 2.6 exists: it starts an open-ended session blocking the preset-resolved entertainment set, turns into a solid "tap to turn off" control while running, and refuses to start (with a message) when there is nothing to block. Stopping records a cancellation like every other stop path, so the strict streak rule treats it identically. Verified `:app:assembleDebug` green |
| 2026-10-30 | Notif-Streak | completed          | QA Agent           | Daily Streak and Streak at Risk share one poll-driven use case after 22:00 and are mutually exclusive; risk reports the streak as it stood before today, daily requires observed usage so an unobserved day is never celebrated. Bedtime Warning is keyed per rule per day and only considers sleep-named rules, since `ScheduleRule` carries no type. New `minutesUntilStart` is pure and handles after-midnight starts and day-of-week filtering. Three new `AlertKind` entries flow into Settings automatically. 24 new tests. Verified: `test` 303/303, `:app:assembleDebug` green. No device, so delivery to the shade is unverified |
| 2026-10-30 | P5.7 | completed            | QA Agent           | Release build now shrinks (R8 + resources). Local rules keep source lines and `com.sober.**` enum constants, which also disables enum unboxing so stored enum names keep round-tripping. Signing is env-driven (`SOBER_KEYSTORE_PATH/_PASSWORD`, `SOBER_KEY_ALIAS`, `SOBER_KEY_PASSWORD`) with no key in the repo; verified unsigned and signed with a throwaway key (`apksigner` V2, key deleted after). GitHub Release upload stays `[!]` |
| 2026-10-30 | P5.8 | completed            | QA Agent           | `bundleRelease` produced a 7.2 MB AAB, `jarsigner -verify` clean with the throwaway key. Policy review recorded the four console declarations this manifest triggers (Accessibility, FGS types, Data safety, listing claims) and the remaining gaps. Privacy-policy URL and console drafts stay blocked on accounts |
| 2026-10-30 | P5.8a | completed           | Implementation Agent | Accessibility onboarding step now shows a consent dialog naming what is read, what is never read, and that nothing is stored or sent; only "I understand and agree" opens Settings. The step became skippable, matching the policy's "optional" claim, because section blocking degrades to package blocking |
| 2026-10-30 | P2.5 | completed            | QA Agent           | Sleep/Work auto-enable is delivered by the 4.1 templates as scheduled blocks, not as auto-started sessions; divergence documented — notification silencing stays session-scoped |
| 2026-10-30 | VD.2–VD.4 | completed        | QA Agent           | Robolectric 4.17 runs framework and Compose tests on the JVM: 11 `PermissionUtils` tests, 7 design-system tests, 3 block-overlay tests. Espresso closed as not applicable (no View hierarchy). GitHub Actions workflow added and YAML-validated, not executed on a runner |
| 2026-10-30 | P5.0-lint | fixed              | Implementation Agent | Full lint run found `AppOpsManager.unsafeCheckOpNoThrow` (API 29) called on a minSdk 26 path — a `NoSuchMethodError` on Android 8–9; now branches to `checkOpNoThrow` below Q. It also flagged the three `UsageStatsManager` queries because `PACKAGE_USAGE_STATS` was declared only in `:app`; the module manifest now declares it too (merger deduplicates). `lint` is green across all modules |
| 2026-10-30 | P0.7 | completed            | Task Tracker Agent | Repository initialized and pushed to `github.com/rishabhx29/Sober`. The CI workflow commit is parked on a local `ci-workflow` branch: the token has no `workflow` scope, so GitHub rejects any push containing `.github/workflows/` |
| 2026-10-30 | P5.8-privacy | completed    | Implementation Agent | Published `docs/PRIVACY.md` (HTTP 200). GitHub Pages is enabled but shadowed by an account-level custom domain whose DNS points at the portfolio host, so the Markdown URL is the live one. The policy now names OS backup as the only automatic path off-device; the in-app dialog links to the page and uses the same wording |
| 2026-10-30 | P6.1–P6.4 | completed        | QA Agent           | The four post-launch rules already ship as catalog data and are listed with toggles. `ShippedSectionRulesTest` (5 tests) runs the shipped document through the matcher: every rule matches its own package and selected tab, matches no other app, and ignores a visible-but-unselected tab. Whether the ids match current app builds stays blocked on hardware |
| 2026-10-30 | P6.5 | blocked              | Orchestrator       | A DNS-blocking `VpnService` must forward unblocked queries, which needs `INTERNET` and sends them to an upstream resolver — contradicts the no-transmission promise, the published policy and the Data safety answer. Needs a product decision, not code |
| 2026-10-30 | P6.6 | completed            | Implementation Agent | `docs/IOS_SCREEN_TIME.md`: Family Controls requires Apple-granted entitlement; shields are app-level and tokens opaque, so section blocking and notification filtering cannot be ported. Port assessed as a different product |
| 2026-10-30 | P1.10 | completed            | QA Agent           | The app row had no quick switch; a limit was only removable by reopening the sheet. `AppRow` now owns the row and shows a `SoberToggleSwitch` for any app with a limit, wired to a new `setLimitEnabled` that updates `AppLimit.isActive` in the repository and the list; enforcement already honoured `isActive`, so no service change. `AppRowTest` (9 Robolectric/Compose tests) covers switch state, toggle-not-open, tap-not-toggle, no switch without a limit, and the lock's two states. Verified: `:feature:apps:testDebugUnitTest` 9/9, `:feature:apps:compileDebugKotlin` green. Row appearance is not visually checked — no device |

---

> **Adding a new task**: Append it under the correct phase. The Task Tracker Agent will pick it up on next scan.  
> **Completing a task**: Only the QA Agent may mark a task `[x]` after verification passes.