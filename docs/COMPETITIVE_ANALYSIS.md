# Competitive analysis — digital wellbeing and app blockers

**Date:** 2026-11-03
**Scope:** consumer screen-time / app-blocking / focus products that a Sober user might
otherwise install, plus the OS features that come free with the phone.
**Method:** official product sites, pricing pages, Google Play and App Store listings,
vendor help centres, and review threads. Price and feature claims are dated; they change.

**Confidence markers**

| Mark | Meaning |
|---|---|
| ✓ | Verified on an official product, pricing or help page |
| ◐ | Reported by a credible review or listing, not confirmed on the vendor's own page |
| ? | Could not verify; treat as unknown |
| – | Explicitly absent (vendor says so, or the product does not exist on that platform) |

---

## 1. Where Sober stands today

### 1.1 What ships in v1.3 (code-verified)

| Area | Capability |
|---|---|
| Limits | Per-app daily time limit with a 7-day chart and a one-tap pause; overall daily cap with a per-app "Choose apps" exemption list |
| Blocking | App block overlay; section blocking for Instagram Reels/Explore and YouTube Shorts (accessibility); website blocking by browser address bar (Chrome verified, Brave/Edge/Firefox/Samsung Internet best-effort) |
| Rules | Custom rules: always-block an app, always-block a website, mute notifications containing a chosen word |
| Schedules | Time windows on chosen days, with suggested schedules from real usage |
| Focus | 15 m / 25 m / 30 m / 45 m / 1 h / 1.5 h / custom / open-ended; chosen block list; optional notification silencing; hold-5 s end with a 57 s regret window; meal break |
| Feedback | Usage warning at 80 %, session complete, streak warning, daily streak confirmation, bedtime warning; achievements; day streak; adherence ("observed day") metric |
| Insight | Today / 7 days / 30 days totals, top apps, patterns, hour heatmap, focus history, emergency-unlock count |
| Safety | PIN gate on Settings; Device Admin so the app cannot be uninstalled while active; accessibility service watchdog; boot recovery; foreground-service session |
| Privacy | **No `INTERNET` permission at all** — no account, no cloud, no analytics, no VPN, on-device only |
| Distribution | Signed APK on GitHub Releases, MIT licence, sideload with documented warnings |

### 1.2 What we already do better than most of the market

1. **No network permission.** Every major competitor either needs the network (accounts,
   sync, desktop, ad SDKs) or a VPN. Sober's APK cannot send usage anywhere even if it
   wanted to. ScreenZen's Play data-safety label, for example, says app activity is shared
   with third parties ◐, and AppBlock's free tier carries ads ✓.
2. **One app, four block types.** Section rules *and* website rules *and* per-app caps *and*
   a whole-phone cap with exemptions. Opal explicitly does not block sections ✓; Freedom has
   no time limits at all ✓; Brick has no per-app limits ✓; Apple Screen Time has no
   intra-app blocking ✓.
3. **Honest measurement.** A day only counts as "under your limit" when usage was actually
   observed. No competitor markets this; several count a day you did not use the phone.
4. **Uninstall protection without a subscription.** Device Admin is already wired ✓; Opal,
   one sec, StayFree and ScreenZen rely on a revocable permission or paid strict mode.
5. **No VPN battery tax, no VPN conflicts.** Freedom and most "block websites" apps run a
   local VPN ◐; Sober never does.
6. **Free, open source, reproducible.** Opal is $99.99/yr or $399 lifetime ✓; Jomo $29.99/yr ✓;
   one sec €14.99/yr ✓; Brick is $59 of hardware ✓.
7. **Copy that does not shame.** Cancellations are counted, not scolded; no red badges.

### 1.3 The structural constraints we must design around

- **Android only.** iOS exposes no foreground view hierarchy to third parties, so section
  blocking and keyword muting cannot be ported (`docs/IOS_SCREEN_TIME.md`).
- **No network permission.** No cloud sync, no remote dashboard, no crash reporting, no
  in-app update check, no email reports, no social features. Offline substitutes exist:
  files, QR codes, NFC, share sheets.
- **Sideload distribution.** Play Protect warnings and the Android 13 "restricted setting"
  step are real adoption friction that every Play Store competitor does not have.
- **Accessibility service.** OEM battery managers kill it; users can disable it; Play policy
  restricts the category; it is also the only way to do sections, keywords and address bars.

---

## 2. Market map

| Category | Products | What they sell |
|---|---|---|
| Premium friction + coaching | **Opal**, **one sec**, **Jomo**, **Clearspace** | Design, behavioural science, pause/reason before opening, scores, social proof |
| Hard blockers / cross-device | **Freedom**, **Cold Turkey**, **AppBlock**, **StayFree** | Toughness, lock modes, schedules, desktop + phone coverage, reporting |
| Physical barrier | **Brick**, **Unpluq**, **ScreenZen halo** | A puck/tag/beacon you must physically leave behind |
| Gamified focus | **Forest** (and Focus Plant, Flora) | A tree dies if you leave; real trees planted |
| OS built-ins | **Apple Screen Time**, **Google Digital Wellbeing**, **Samsung Digital Wellbeing**, **Google Family Link** | Free, unremovable, OS-level, family plumbing |
| Feed strippers | **AppBlock / StayFree in-app blocking**, **ScrollGuard**, **NoScroll**, **Dull** | Remove Reels/Shorts/Explore instead of the whole app |
| Minimalist launchers | **Minimalist Phone**, **lessphone**, **Olauncher** (open source) | Make the home screen boring |
| Parental controls (adjacent) | **Family Link**, **Qustodio**, **Net Nanny**, **Bark** | Supervise someone else's device |

Sober sits in **hard blockers** with two feet in **feed strippers**, and is the only product
in either group that combines them with an overall cap and no network permission.

---

## 3. Product profiles

### 3.1 Opal — the design and brand leader

- **Platforms:** iOS 18+, iPad, macOS, Android (launched Aug 2025, still behind iOS) ✓◐.
- **Price:** free = 1 rule; Pro $19.99/mo, $99.99/yr, **$399 lifetime**; 3–7 day trial ✓.
- **Mechanism:** Apple Screen Time/FamilyControls on iOS; Android uses an
  AccessibilityService, no VPN ✓.
- **Beats Sober with:** Deep Focus ("cannot bypass or cancel") and Hard Mode; Emergency
  Pass; timed "Breaks"; Focus Score and Opal Score; Focus Report with benchmarks;
  milestones and Focus Gems; friend leaderboard; Vacation Mode; widgets; Live
  Activities/Dynamic Island; soundscapes; Shortcuts automations; rule schedules tied to
  calendar/work/sleep routines; allow-only lists.
- **Sober equivalent:** regret timer (weaker lock), meal break (narrower breaks), day
  streak + achievements (less game-like), stats (no score), focus sessions (no sounds).
- **Weakness:** price and renewal complaints; free tier is a demo; no section blocking;
  Android lags; accessibility is revocable.

### 3.2 one sec — the science and friction specialist

- **Platforms:** iOS, Android, macOS, Windows, Linux, ChromeOS, browser extensions ✓.
- **Price:** free = 1 app; €3.99/mo, €14.99/yr, **€99.99 lifetime**; US listing $2.99/mo,
  $19.99/yr ✓.
- **Mechanism:** Screen Time API (iOS) or AccessibilityService (Android), no VPN ✓.
- **Beats Sober with:** the whole *pre-open* intervention set — 4-7-8/box breathing, mirror,
  rotate the phone, maths, follow-the-dot, retype text; randomised delays; "re-intervention"
  when a time cap hits; intentional app switching; journaling and emotion tracking; healthy
  alternatives; peer-reviewed evidence (Max Planck/Heidelberg) and a US patent; desktop and
  browser coverage; Live Activities.
- **Sober equivalent:** the block overlay (after the limit, not before opening).
- **Weakness:** described even by fans as "a speed bump, not a wall"; intervention fatigue;
  free tier is one app; Android loses website blocking and strict sessions ✓.

### 3.3 Jomo — the ritual and accountability app

- **Platforms:** iPhone, iPad, Mac, Vision Pro. **No Android** ✓.
- **Price:** free = 1 session/1 limit/1 budget; Plus $5.99/mo, $29.99/yr, **$99.99 lifetime**;
  Family $11.99/mo, $59.99/yr ✓.
- **Mechanism:** Apple Screen Time/FamilyControls; NFC tags; AI photo verification ✓◐.
- **Beats Sober with:** Strict Mode (rules locked for N days); unlock rituals (mirror,
  retype a personal message, AI photo proof of a real-world action); NFC-tag pairing;
  routines that earn screen time; opens-per-day caps; Squads (accountability groups);
  custom block-screen labels and tips; Jomo days and best streak; Apple Health integration.
- **Sober equivalent:** session label, hold-to-end, achievements; no NDEF/NFC hooks.
- **Weakness:** iOS-only; Apple framework limits on reporting; privacy label includes
  contacts/health/purchases ◐.

### 3.4 Clearspace — friction plus a human witness

- **Platforms:** iOS, iPad, Vision Pro, Android (v0.5, ~5 k installs), Chrome ✓◐.
- **Price:** free core; Premium $6.99/mo, $44.99/yr; Family $79.99/yr; students free ✓.
- **Mechanism:** Screen Time API on iOS; Android mechanism unverified ?.
- **Beats Sober with:** push-up/squat challenge via motion sensor to unlock extra time;
  accountability teammates notified when you exceed a budget; screen-time challenges;
  ADHD/anxiety-friendly positioning; Chrome extension; family plan.
- **Sober equivalent:** none of the exercise/social mechanics.
- **Weakness:** accountability is visibility, not a gate; best features paywalled; Play label
  declares third-party ads and linked data ◐; Android is an early build.

### 3.5 Freedom — cross-device sessions

- **Platforms:** iOS, Android, macOS, Windows, Chromebook, Linux, Chrome extension ✓.
- **Price:** free = 2 h sessions, no schedules/lock; Premium $8.99/mo or $39.99/yr; lifetime
  $99.50 ✓.
- **Mechanism:** local VPN on mobile ◐ plus accessibility/device admin; desktop app +
  extension ✓.
- **Beats Sober with:** one session across phone + laptop + browser; Locked Mode (cannot end
  early); recurring schedules; unlimited preset/custom blocklists; whole-internet blocks and
  allow-only website exceptions; focus sounds; Teams/Schools plans.
- **Sober equivalent:** per-device schedules and focus sessions only.
- **Weakness:** no per-app time limits, no stats at all; VPN battery and conflicts; sessions
  die if the user turns the VPN off ◐.

### 3.6 Cold Turkey — the desktop fortress

- **Platforms:** Windows + macOS only. **No Android/iOS** ✓.
- **Price:** free website blocking; Blocker Pro one-time ~$39 ◐, lifetime updates, 30-day
  refund ✓.
- **Mechanism:** OS-level process and browser blocking, no VPN; blocks task manager, date
  settings and uninstaller ✓.
- **Beats Sober with:** Frozen Turkey (lock/log off/shut down the machine); timer, random
  text, delay (up to 40 days), schedule and password lock types; blocks executables, folders,
  store apps and window titles; YouTube-channel allow/block; wildcard URLs; incognito
  history stats that survive history wipes; JSON export; "Pause for a Cause" (a break in
  exchange for a donation).
- **Sober equivalent:** hold-to-end + regret (much softer); no wildcard engine on desktop.
- **Weakness:** no phone coverage; a 30–60 s window after login before blocks start ◐;
  accidental locks are painful; needs admin rights.

### 3.7 AppBlock — the Android feature heavyweight

- **Platforms:** Android (10 M+ installs), iOS, watchOS, Chrome/Edge/Brave ✓.
- **Price:** free tier with caps; premium monthly/yearly/lifetime (site loads prices
  dynamically; 2026 reviews report ~$4.99/mo, $29.99/yr, $89.99 lifetime) ◐.
- **Mechanism:** Accessibility + usage access for detection and overlay; PIN-locked strict
  mode ✓.
- **Beats Sober with:** Strict Mode with cooldown or third-party approval; profiles triggered
  by time, day, **location or Wi-Fi**; launch-count limits; intervals; "allowlist mode"
  (dumbphone setup); auto-block newly installed apps; adult-content filtering; block in-app
  purchases; section blocking (Reels, Shorts, Stories, Spotlight) ✓; reports, backup,
  templates and courses; 15 languages.
- **Sober equivalent:** schedules (no location/Wi-Fi triggers), no launch counts, no
  allow-only, no categories, no adult filter, English only.
- **Weakness:** ads and limits on free; complex setup; location profiles cost battery ◐;
  iOS build is weaker.

### 3.8 StayFree — the reporting and in-app stripping app

- **Platforms:** Android, iOS, Windows, macOS, Linux, browser extensions, Wear OS ✓.
- **Price:** free install with premium gating; official prices not published on fetchable
  pages ?; 2026 review reports ~$10/mo, ~$50/yr ◐.
- **Mechanism:** Usage Access + Accessibility for website detection; no VPN claimed ✓.
- **Beats Sober with:** in-app blocking (hide Shorts, Reels, Stories, Explore, comments,
  recommendations) ✓; unlock counts and comparison against averages; session limits; sleep
  mode; overuse reminders; CSV export; widgets; cross-device stats and limit sync; Wear OS;
  a "Wellbeing Coach"; 15 languages.
- **Sober equivalent:** section blocking exists (smaller catalogue); patterns/heatmap exist;
  no export, no pickups/notifications counts, no Wear OS, no coach.
- **Weakness:** iOS reliability is poor (2.4/5 on the App Store at time of research) ◐;
  permission breadth and data-sharing concerns ◐; premium unclear.

### 3.9 Brick — the physical puck

- **Platforms:** iOS 18+, Android 12+ ✓.
- **Price:** Brick $59, Brick Night $129, bundle $169; cases $39; **no subscription** ✓.
- **Mechanism:** NFC puck + app; Android uses Accessibility over package names; iOS Screen
  Time ✓.
- **Beats Sober with:** a barrier you cannot swipe past; Brick Night Bluetooth proximity
  beacon; 10 custom modes; 5 Emergency Unbricks (+5 on request); Strict Mode that blocks
  deleting the app or changing settings; usage history; HSA/FSA eligibility.
- **Sober equivalent:** nothing physical; Device Admin already prevents uninstall.
- **Weakness:** $59 for a magnet; Android reliability complaints ◐; no website blocking on
  Android; no per-app time limits, categories, widgets or streaks documented ✓.

### 3.10 Unpluq — the key-tag barrier

- **Platforms:** iOS 16+, Android with NFC ✓.
- **Price:** tag $26.50; 3/6/12-month plans with tag $29/$46/$80; family plans $79.95–$99.95
  (period unverified ?); free tier = 2 apps, 1 schedule ✓.
- **Mechanism:** Screen Time (iOS) / Accessibility + **Device Admin** (Android) plus NFC
  barrier; other barriers include QR code, shake, tap pattern, scroll and walking ✓.
- **Beats Sober with:** physical unlock ritual; up to 49 apps per schedule; multiple barrier
  types; contact whitelist; lost-tag mode and 5-minute emergency mode; family app.
- **Sober equivalent:** none physical; hold-to-end ritual only.
- **Weakness:** the tag is useless without a paid plan ◐; no Android website blocking ✓; no
  per-app caps.

### 3.11 ScreenZen — the free friction benchmark

- **Platforms:** iOS, Android (1 M+ installs) ✓.
- **Price:** **free, no subscription**; optional tips; halo beacon $49 ✓.
- **Mechanism:** Screen Time API (iOS), Accessibility (Android), Bluetooth geofence halo ✓.
- **Beats Sober with:** escalating delays before opening apps; check-in windows; daily goals
  with block-on-limit; opens-based and time-based strict blocks; "Is this important?"
  prompts; breathing and alternative-activity lists; reflection before unlock; limited
  unlocks per day and cooldowns up to 2 h; settings lock with an accountability partner;
  Minimal modes that strip Reels/Shorts/feeds ✓; halo that prevents uninstalling or
  disabling.
- **Sober equivalent:** block overlay, not pre-open friction; no cooldowns, no opens limits,
  no reflections.
- **Weakness:** Android bypass by changing the system clock ◐; weak website picker, no
  keyword URL blocking; occasional "doesn't block" days ◐; data-safety label says activity
  shared with third parties ◐.

### 3.12 Forest — the gamified focus classic

- **Platforms:** iOS, Android (ad-supported), Chrome/Firefox extensions ✓.
- **Price:** free core; Forest Plus $5.99/mo or $35.99/yr; coin purchases $0.99–$21.99 ✓.
- **Mechanism:** focus timer; Android Time Guard uses Accessibility; allow-list blocking on
  iOS 16+ ✓.
- **Beats Sober with:** a tree that dies if you leave (loss aversion); Plant Together group
  rooms where one failure kills everyone's tree; leaderboards; 100+ species; soundscapes;
  coins and achievements; real trees planted via Trees for the Future (2 M+); Timeline and
  Apple Health sync; 5-minute pause with Plus.
- **Sober equivalent:** day streak and achievements; no group focus, no loss aversion, no
  forest/history visualisation.
- **Weakness:** subscription pivot backlash ◐; ads on Android; allow-list only on iOS; no
  anti-uninstall; weak time limits.

### 3.13 Apple Screen Time

- **Platforms:** iOS/iPadOS/macOS/watchOS/visionOS, free, built in ✓.
- **Mechanism:** OS-level FamilyControls/DeviceActivity/ManagedSettings ✓.
- **Beats Sober with:** *Time Allowances* (daily budget by category) and per-app limits;
  named schedules per weekday/weekend with "Match Screen Time Schedule"; **Always Allowed**
  apps and contacts; Allowed/Blocked website lists, ratings and **Ask to Browse**; Screen
  Distance; pickup and notification counts in reports; Share Across Devices; family approval
  requests; passcode that a child cannot change without the parent device.
- **Sober equivalent:** per-app limits + schedules + website allow/block (ours is per-domain
  via address bar); no categories, no contacts allow-list, no family, no pickups.
- **Weakness:** passcode recovery path documented ◐; app-declared web content only partly
  covered; no intra-app blocking; settings buried.

### 3.14 Google Digital Wellbeing

- **Platforms:** Android 9+ (stock/AOSP; OEM variants), free ✓.
- **Beats Sober with:** dashboard of screen time, **times opened, unlocks and notifications
  received**; app timers that dim the icon and reset at midnight; Chrome site timers with
  per-site time; Bedtime mode (schedule, DND, grayscale, dim wallpaper, dark theme, off at
  next alarm); Focus mode with quick-settings tile and scheduled windows; home-screen
  widget.
- **Sober equivalent:** usage time and top apps; no pickups/unlocks/notifications; bedtime
  is a warning only; no grayscale/DND automation; no widget/tile.
- **Weakness:** no content filtering, no allow-lists, no passcode by default; help page
  explains how to delete a timer to regain access ✓.

### 3.15 Samsung Digital Wellbeing / One UI

- **Platforms:** Galaxy devices, free, cannot be disabled ✓.
- **Beats Sober with:** app timers by app **or category** with per-weekday "Add schedule";
  Sleep mode inside Modes and Routines (DND, grayscale, dark mode, AOD, power saving);
  Driving screen time; volume monitor; Samsung Kids sandbox; family group parental PIN.
- **Sober equivalent:** per-app limits and schedules; no category caps, no grayscale/DND,
  no parental PIN (we have a Settings PIN for ourselves).
- **Weakness:** timers are self-editable without a PIN; blocking power sits in Modes and
  Routines and varies by One UI version.

### 3.16 Google Family Link (parental reference)

- **Platforms:** parent app Android/iOS/Chromebook/web; child Android/Chromebook only ✓.
- **Beats Sober with:** daily limit with per-weekday schedule; bonus time; remote lock;
  downtime and school time with unlimited-time apps; per-app limits; block/allow apps; Play
  content restrictions; purchase approvals; location; offline parent access code; the device
  date/time toggle is off by default so the clock cannot be moved.
- **Sober equivalent:** none of the remote/family plumbing (deliberately: no network, no
  account).
- **Weakness:** per-device limits (not pooled); system apps exempt; changes need internet
  (~5 min); no content filtering outside Play/Chrome.

### 3.17 Feed strippers and minimalist launchers (brief)

- **ScrollGuard / NoScroll / Dull:** remove short-form feeds inside Instagram, YouTube,
  TikTok and the web-app versions ✓◐. Single-purpose, no caps, no overall budget.
- **AppBlock and StayFree** already ship the same idea inside a full blocker ✓.
- **Minimalist Phone / lessphone / Olauncher:** replace the launcher with a text list; no
  enforcement, but removes the visual trigger. Olauncher is open source and on F-Droid.

---

## 4. Feature matrix

Legend: ✓ yes · ~ partial, opt-in, or platform-limited · – no · ? unverified.

### 4.1 Blocking and limits

| Capability | Sober | Opal | one sec | AppBlock | StayFree | ScreenZen | Brick | Freedom | Apple ST | Google DW |
|---|---|---|---|---|---|---|---|---|---|---|
| Per-app daily time limit | ✓ | ✓ | ~ | ✓ | ✓ | ✓ | – | – | ✓ | ✓ |
| Whole-phone daily budget | ✓ | ~ | – | – | ✓ | ✓ | – | – | ✓ | – |
| App categories / category limits | – | ✓ | ✓ | ~ | ✓ | – | – | – | ✓ | – |
| Per-weekday schedules | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ~ |
| Website / domain blocking | ✓ | ✓ | ~ | ✓ | ✓ | ~ | – | ✓ | ✓ | ~ |
| Keyword or wildcard URL rules | ✓ | – | – | ✓ | – | – | – | ~ | – | – |
| Section blocking (Reels/Shorts/tabs) | ✓ | – | ? | ✓ | ✓ | ~ | – | – | – | – |
| Notification keyword muting | ✓ | – | – | – | – | – | – | – | – | – |
| Allow-only / whitelist mode | – | ~ | – | ✓ | – | – | ~ | ~ | ✓ | – |
| Open-ended session | ✓ | ✓ | ~ | ~ | ~ | ~ | ~ | ✓ | ~ | – |
| Strict lock (cannot cancel) | ~ | ✓ | ~ | ✓ | – | ~ | ✓ | ✓ | ~ | – |
| Uninstall protection | ✓ | ~ | – | ~ | – | ~ | ✓ | – | n/a | n/a |
| Location / Wi-Fi triggered profile | – | – | – | ✓ | – | – | – | – | – | – |

### 4.2 Friction and behaviour design

| Capability | Sober | Opal | one sec | AppBlock | StayFree | ScreenZen | Brick | Freedom | Apple ST | Google DW |
|---|---|---|---|---|---|---|---|---|---|---|
| Pause/delay before an app opens | – | ~ | ✓ | – | – | ✓ | – | – | – | – |
| Breathing / exercise / mirror ritual | – | ~ | ✓ | – | – | ✓ | – | – | – | – |
| Opens-per-day limit | – | – | – | ✓ | ~ | ✓ | – | – | – | – |
| Intention prompt / journal | – | ~ | ✓ | – | – | ✓ | – | – | – | – |
| Streaks | ✓ | ✓ | – | – | – | ✓ | – | – | – | – |
| Achievements / gamification | ✓ | ✓ | – | – | – | – | – | – | – | – |
| Social / accountability partner | – | ✓ | – | – | – | ~ | – | – | – | – |
| Custom block-screen message | – | ~ | ~ | ~ | – | ~ | – | – | ~ | – |
| Emergency bypass with friction | ✓ | ✓ | ~ | ✓ | – | ~ | ✓ | – | ~ | – |
| Focus sounds | – | ✓ | – | – | – | – | – | ✓ | – | – |
| Bedtime / DND automation | ~ | ~ | – | ~ | ✓ | ~ | – | – | ✓ | ✓ |
| Timed break | ~ | ✓ | – | – | – | ~ | – | – | ~ | ✓ |

### 4.3 Insight and reporting

| Capability | Sober | Opal | one sec | AppBlock | StayFree | ScreenZen | Brick | Freedom | Apple ST | Google DW |
|---|---|---|---|---|---|---|---|---|---|---|
| Screen-time totals | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ~ | – | ✓ | ✓ |
| Per-app history charts | ✓ | ✓ | ✓ | ✓ | ✓ | ~ | ~ | – | ✓ | ✓ |
| Hour-of-day heatmap | ✓ | – | – | – | – | – | – | – | – | – |
| Pickups / unlocks / notifications | ~ | – | ~ | – | ✓ | – | – | – | ✓ | ✓ |
| Week-over-week comparison | ~ | ✓ | – | ✓ | ✓ | – | – | – | ✓ | ✓ |
| Focus score / index | – | ✓ | – | – | ~ | – | – | – | – | – |
| CSV / JSON export | – | – | – | ~ | ✓ | – | – | – | – | – |
| Shareable report card | – | ✓ | – | – | ~ | – | – | – | ~ | – |
| "Observed day" honesty | ✓ | – | – | – | – | – | – | – | – | – |

### 4.4 Surfaces, sync and platforms

| Capability | Sober | Opal | one sec | AppBlock | StayFree | ScreenZen | Brick | Freedom | Apple ST | Google DW |
|---|---|---|---|---|---|---|---|---|---|---|
| Home-screen widget | – | ✓ | – | ✓ | ✓ | ✓ | – | – | ✓ | ✓ |
| Quick Settings tile | – | – | ~ | – | – | ~ | – | – | ✓ | ✓ |
| Live activity / ongoing timer | ~ | ✓ | ✓ | – | – | – | – | – | ✓ | – |
| Wearable app | – | – | – | ✓ | ✓ | – | – | – | ✓ | – |
| Desktop / browser coverage | – | ~ | ✓ | ✓ | ✓ | – | – | ✓ | ~ | – |
| Cross-device sync | – | ✓ | ~ | – | ✓ | – | – | ✓ | ✓ | – |
| Shortcuts / automation hooks | – | ✓ | ✓ | – | – | ~ | – | – | ✓ | – |
| NFC / hardware barrier | – | – | – | – | – | ✓ | ✓ | – | – | – |
| Family / parental controls | – | – | – | – | – | – | – | – | ✓ | – |

### 4.5 Privacy, price and distribution

| Property | Sober | Opal | one sec | AppBlock | StayFree | ScreenZen | Brick | Freedom | Apple ST | Google DW |
|---|---|---|---|---|---|---|---|---|---|---|
| No `INTERNET` permission | **✓** | – | – | – | – | – | – | – | n/a | n/a |
| No account required | ✓ | – | ~ | ~ | – | ✓ | ✓ | ✓ | ✓ | ✓ |
| No VPN | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | – | n/a | n/a |
| On-device data only (vendor claim) | ✓ | ~ | ✓ | ~ | – | ~ | ~ | – | ✓ | ✓ |
| Open source | **✓** | – | – | – | – | – | – | – | – | – |
| Store install | – | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | n/a | n/a |
| Free tier | ✓ (all) | ~ (1 rule) | ~ (1 app) | ~ | ~ | ✓ | ✓ (hardware) | ~ (2 h) | ✓ | ✓ |
| Paid ceiling | $0 | $399 lifetime | €99.99 lifetime | ~$90 lifetime ◐ | ? | $0 | $169 bundle | $99.50 lifetime | $0 | $0 |
| Languages | 1 | ? | ? | 15 | 15 | ? | ? | ? | 40+ | 80+ |

---

## 5. How competitors beat Sober today (ranked by user impact)

| # | Gap | Who does it | Why it matters for us |
|---|---|---|---|
| 1 | **Store install and instant updates** | everyone except Brick-hardware-only users | Sideloading asks a user to click past Play Protect and Android 13's "restricted setting", then update by hand. This is the single largest adoption barrier, larger than any missing feature. |
| 2 | **Pre-open friction** | one sec, ScreenZen, Jomo, Clearspace | Sober only intervenes *after* a limit is spent. With generous limits, the moment of impulse is untouched. A 5–10 s pause plus one question is the cheapest behavioural change in the market. |
| 3 | **True lock modes** | Opal Deep Focus/Hard Mode, Freedom Locked Mode, Cold Turkey, AppBlock, Jomo | Our hold-5 s + 57 s regret can be beaten by disabling accessibility or changing the clock; theirs are backed by cooldowns, approvals or OS-level passcodes. |
| 4 | **Glanceable surfaces** | Opal, AppBlock, StayFree, ScreenZen, Apple, Google | A widget or QS tile is how these products stay present without opening the app. We currently have an ongoing notification only. |
| 5 | **Cross-device coverage** | Freedom, Cold Turkey, one sec, StayFree, Opal macOS | Phone-only means laptop scrolling is unmanaged. Deliberate for privacy, but it is a real loss. |
| 6 | **Accountability and social** | Jomo Squads, Clearspace teammates, Opal leaderboard, Forest Plant Together | Social pressure outperforms willpower for many users; we have nothing. |
| 7 | **Reporting depth** | StayFree, Cold Turkey, Apple, Google | We lack pickups/unlocks/notification counts, week-over-week detail and export. Our heatmap is actually ahead of most. |
| 8 | **Section-catalogue breadth** | AppBlock, StayFree, ScrollGuard, Dull | We verified Instagram and YouTube; they ship TikTok, Facebook, X, Snapchat and web-app variants. |
| 9 | **Category limits and allow-only setups** | Apple, AppBlock, Samsung | "Social media = 30 m total" is faster to configure than seven per-app limits; allow-only mode turns the phone into a dumb phone for a day. |
| 10 | **Polish, templates, localisation** | Opal, AppBlock (15 languages, templates/courses), Apple/Google (40–80+) | We are English-only with no onboarding templates. |
| 11 | **Physical friction** | Brick, Unpluq, ScreenZen halo | A puck/tag beats any software toggle for a determined user. We could support cheap NFC tags without selling hardware. |
| 12 | **Free built-ins are "good enough"** | Apple Screen Time, Google/Samsung Digital Wellbeing, Family Link | Every feature we add competes with an OS feature that is free, unremovable and already installed. |

---

## 6. Where Sober leads

1. **Provable privacy.** The manifest requests no `INTERNET` permission. No competitor in
   this list can make that statement; several cannot even claim on-device-only data.
2. **Breadth per byte.** Section blocking + website blocking + app caps + whole-phone cap +
   notification keyword mute + schedules + focus sessions in a 4 MB offline APK.
3. **A whole-phone budget with exemptions.** Apple has category allowances; AppBlock has
   allow-only; nobody else combines a global ceiling with "these apps don't count as blocked".
4. **Honest adherence.** "Observed day" is a unique metric and a defensible editorial stance.
5. **Device Admin protection at no cost.** Uninstall protection is not paywalled.
6. **No VPN.** No battery tax, no conflict with a real VPN, no traffic interception.
7. **Restraint.** No red badges, no guilt copy, cancellations counted rather than shamed.
8. **Open source (MIT) and reproducible builds.** Trust comes from inspectability, not a
   marketing page.

---

## 7. Improvement backlog

Effort: **S** ≈ 1–2 days, **M** ≈ 3–5 days, **L** ≈ 1–2 weeks (one developer, with tests
and an emulator pass).

### 7.1 P0 — the next release (1.4)

| ID | Feature | Why | Effort | Reference | Implementation sketch |
|---|---|---|---|---|---|
| F1 | **Pause before opening** — per-app delay (5–30 s) with an intention question and optional breathing ring | The strongest evidence-backed feature we lack; works even when no limit is set | M | one sec, ScreenZen, Jomo | Accessibility foreground event → full-screen Compose overlay in `:system:overlay`; per-app config in DataStore; skip inside the same app for N minutes after a pass; reuse the editorial block-screen layout |
| F2 | **Lock mode** — PIN-protected rule edits, optional "cannot end this session/schedule for N hours" | Closes the "turn it off in a weak moment" hole that Deep Focus/Locked Mode sell | M | Opal, Freedom, AppBlock | Extend the existing Settings PIN; store a `lockedUntil` timestamp; hold-to-end refuses and explains; Device Admin already blocks uninstall |
| F3 | **Opens-per-day limit** per app | Time limits miss 60 one-minute visits; opens limits are cheap and effective | S | AppBlock, ScreenZen, Jomo | Count `TYPE_WINDOW_STATE_CHANGED` transitions with a debounce in the accessibility service; reuse the limit/decision engine |
| F4 | **Widget + Quick Settings tile** — usage ring, remaining cap, start Focus | Daily presence; competitors all have one | M | Opal, AppBlock, Apple, Google | Glance app widget in `:app`; `TileService` toggling Focus; read the same repositories through Hilt entry points |
| F5 | **Category limits** | Fewer taps than per-app limits; matches Apple/Samsung | M | Apple, Samsung, StayFree | Map packages to categories with `ApplicationInfo.category`; add a category filter and a category budget rule |
| F6 | **Weekly report** — week over week, pickups/unlocks, best/worst day, shareable card | Makes progress legible; the card is free marketing | S | Opal Focus Report, Apple | New Stats range (7 d already exists — add comparison + card rendering to a bitmap via `Picture`/Compose) |
| F7 | **Backup and restore** — JSON file export/import, plus QR transfer | Offline alternative to cloud sync; recovery after reinstall | S | Freedom, StayFree sync | Serialise DataStore rules with Kotlin serialization; `ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`; QR via zxing-core (offline) |
| F8 | **Bedtime mode** — schedule that enables DND, blocks chosen apps and dims the screen | Sleep is the highest-value window | M | Apple, Google, Samsung | `NotificationManager.setInterruptionFilter` (needs notification-policy access), schedule rule, dim overlay; grayscale is not writable without `WRITE_SECURE_SETTINGS` |
| F9 | **Pause everything for a while** — vacation/grace period with an explicit expiry | Prevents "delete the app" responses to a bad day | S | Opal Vacation Mode, AppBlock pause | Global `pausedUntil` preference honoured by the decision engine |
| F10 | **Wider section and browser coverage** — TikTok, Facebook, X, Snapchat, more browsers, in-app WebViews where detectable | Section blocking is our differentiator; the catalogue is thin | L | AppBlock, StayFree, ScrollGuard | Extend `BrowserCatalog`; add per-app section node rules with a user-assisted picker; document unsupported surfaces |

### 7.2 P1 — the release after

| ID | Feature | Why | Effort | Reference |
|---|---|---|---|---|
| F11 | Reflection before unlock + "what will you do instead?" suggestions | Converts a block into a decision | S | one sec, Jomo, ScreenZen |
| F12 | Custom block-screen message and saved "why" | Personal copy outperforms generic copy | S | Jomo Recopy, Opal quotes |
| F13 | Challenges (7-day, weekend, no-Reels week) and richer achievements | Reuses the existing streak engine | S | Opal, ScreenZen, Forest |
| F14 | Localisation (top 8 languages) + RTL pass | 15 languages is table stakes for the category | M | AppBlock, StayFree, Apple |
| F15 | F-Droid + Obtainium distribution, reproducible-build docs | Store-like trust and updates without giving up the no-network design | M | Olauncher, F-Droid ecosystem |
| F16 | Health-check screen — permissions, accessibility alive, OEM battery guidance, test block | Reduces "it stopped working" reports | S | AppBlock Academy, vendor help centres |
| F17 | CSV export of usage and focus history | Power users and researchers | S | StayFree, Cold Turkey |
| F18 | NFC tag support — tap a cheap tag to start/end Focus or unlock | Physical friction without selling hardware | M | Jomo, Brick, Unpluq |
| F19 | Exported shortcuts/intents (Tasker, automation) | Lets users build their own triggers | S | one sec, Opal |
| F20 | Local focus sounds / white noise | One less reason to open another app | S | Freedom, Opal, Forest |

### 7.3 P2 — deliberate maybe, with trade-offs

| ID | Feature | Trade-off |
|---|---|---|
| F21 | Cloud sync / account | Directly contradicts "no internet permission". Offer QR/file transfer instead (F7). |
| F22 | Parental mode (second PIN, child profile) | Different product, different policy surface, and an accountability product is not the same as a self-control product. |
| F23 | Adult-content filtering | Needs URL/DNS coverage and a judging stance the app currently avoids; also a Play policy minefield. |
| F24 | Location/Wi-Fi profiles | Requires location permission, which breaks the privacy story. |
| F25 | iOS port | No section blocking, no keyword muting, no address bar — it would be a generic blocker competing with Opal on Opal's terms (`docs/IOS_SCREEN_TIME.md`). |
| F26 | Desktop / browser extension | A separate codebase and sync problem; high effort, weak fit with the no-network promise. |
| F27 | Hardware | Capital, logistics and support; NFC tags (F18) get most of the benefit for zero inventory. |

---

## 8. Small details worth borrowing

- **Opal:** block-screen copy with personality; a named "score"; a vacation mode; timed
  breaks; Live Activity showing the countdown; annual pricing anchored against lifetime.
- **one sec:** a public science page and a measured percentage claim; randomised
  interventions so the friction does not become invisible; "re-intervention" when a time cap
  is reached.
- **Jomo:** retyping a personal message to unlock; "rules locked for N days"; a template
  gallery; a family plan that shares a lifetime licence.
- **ScreenZen:** escalating delays; a fixed number of unlocks per day with a cooldown; an
  accountability partner who can authorise settings changes; a "halo" room beacon.
- **Brick:** 5 pre-granted emergency unbricks — honest, bounded exceptions rather than a
  hidden bypass; HSA/FSA eligibility (a US tax detail competitors miss).
- **AppBlock:** templates for students/ADHD/parents; 15 languages; allowlist "dumbphone"
  mode; profiles tied to a place.
- **StayFree:** unlock counts and comparison with an average; in-app stripping as a feature
  toggle; Wear OS.
- **Freedom:** Locked Mode; recurring schedules; focus audio.
- **Cold Turkey:** "Pause for a Cause" — a break purchased with a donation; a delay lock of
  up to 40 days; statistics that survive history wipes.
- **Forest:** group focus where one person's failure affects everyone; real-world tree
  planting as a CSR loop.
- **Apple/Google:** Always Allowed lists; per-weekday schedules; pickup/unlock counts;
  bedtime grayscale (not implementable without secure settings on third-party Android).

---

## 9. Positioning and pricing implications

- **Keep the free, offline, open-source promise.** It is the only claim no competitor in
  this list can copy without rebuilding their product. Do not trade it for sync or social.
- **Publish the comparison.** A README table that says "Opal: $99/yr, VPN/cloud; Sober: $0,
  no internet permission" is stronger than any feature list.
- **Distribution is the growth lever, not features.** F-Droid/Obtainium (F15) plus a
  one-screen install page will reach more users than F1–F10 combined. A Play Store listing
  is possible only with the accessibility-use declaration and a policy-compliant use case
  (already flagged in TASKS.md for website blocking).
- **Price:** free now; if funding is needed later, a one-time "supporter" purchase or
  donations fit the open-source story better than a subscription wall on blocking.
- **Do not chase Opal's score/gamification at the expense of honesty.** Our differentiator is
  a firm limit that tells the truth, not a dopamine loop about productivity.

---

## 10. Sources

Product and pricing pages were read on 2026-11-03.

- Opal — https://opalapp.com, https://opalapp.com/pricing, App Store id1497465230,
  Google Play `com.withopal.opal`
- one sec — https://one-sec.app, https://tutorials.one-sec.app, App Store id1532875441,
  Google Play `wtf.riedel.onesec`, https://www.blok.so/resources/one-sec-app-review-does-adding-friction-actually-reduce-screen-time
- Jomo — https://jomo.so, https://jomo.so/pricing, App Store id1609960918
- Clearspace — https://getclearspace.com, App Store id1572515807, Google Play `com.clearspace.app`
- Freedom — https://freedom.to, https://freedom.to/premium, Google Play `to.freedom.android2`
- Cold Turkey — https://getcoldturkey.com, https://getcoldturkey.com/features
- AppBlock — https://appblock.app, https://appblock.app/block-reels-shorts-stories-and-spotlight-on-android-with-appblock/, Google Play listing
- StayFree — https://stayfreeapps.com, Chrome Web Store listing, App Store id1631132311
- Brick — https://getbrick.com, https://getbrick.com/pages/faq, Google Play `com.brickllc.brick`
- Unpluq — https://unpluq.com, https://unpluq.com/pages/faq, Google Play `com.unpluq.beta`
- ScreenZen — https://screenzen.co, Google Play `com.screenzen`, App Store id1541027222
- Forest — https://forestapp.cc, Google Play `cc.forestapp`, App Store id866450515
- Apple Screen Time — https://support.apple.com/en-us/108806, /105121, /102677,
  set-up-screen-time-for-yourself-iphbfa595995
- Google Digital Wellbeing — https://support.google.com/android/answer/9346420
- Samsung — https://www.samsung.com/us/support/answer/ANS10001357, ANS10002400
- Google Family Link — https://support.google.com/families/answer/7101025, 7103340,
  15701527, 15938652, 15957417, 7103413, 7307262, 9116646
- Feed strippers — https://play.google.com/store/apps/details?id=com.newswarajya.noswipe.reelshortblocker,
  https://getdull.app

**Unverified items** are marked ? or ◐ in the tables: AppBlock and StayFree premium prices
(sites render prices dynamically), Clearspace's Android blocking mechanism, one sec's
section-blocking claims on Android, Unpluq family-plan billing period, and any figure
attributed to a review rather than a vendor page.
