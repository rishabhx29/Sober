# iOS Port: Screen Time API Assessment

**Status:** exploration only. No iOS code exists and none is planned before the
Android release ships.

## What Apple provides

Third-party apps reach Screen Time through four frameworks, all requiring the
**Family Controls entitlement**, which Apple grants per app on request:

| Framework | What it does |
|---|---|
| `FamilyControls` | Presents the system picker and returns opaque tokens for the apps, categories and web domains the user chose. |
| `ManagedSettings` | Applies shields to those tokens — the system draws the block screen instead of the app. |
| `DeviceActivity` | Schedules monitoring windows and fires threshold events; work runs in an extension. |
| `ShieldConfiguration` / `ShieldAction` | Customises the shield's appearance and handles its buttons. |

WWDC 2026 did not add APIs for third-party blockers: device-activity
reliability, readable shield state, event-based unlock triggers and readable
usage reports were all left as they were.

## What maps cleanly to Sober

- **Daily limits and app-level blocking.** The user picks apps in the system
  picker; `ManagedSettings` shields them. Sober never learns which apps those
  tokens point to, which is fine for enforcement and good for privacy.
- **Schedules and sleep mode.** `DeviceActivitySchedule` covers the time-window
  rules Sober already has.
- **Focus sessions.** Shield a set of tokens for the session's duration; end it
  through `ManagedSettings`.
- **Block-screen copy.** `ShieldConfiguration` can carry Sober's typography and
  the reason for the block, though the layout is constrained by the system.

## What cannot be ported

These are the parts that make Sober different from a generic app blocker:

- **Section blocking (Reels, Shorts, feeds).** iOS exposes no foreground-app
  signal and no view hierarchy to a third-party process. `FamilyControls`
  tokens are opaque, so even identifying the running app is impossible, let
  alone the selected tab. The Android implementation rests on
  `AccessibilityService`, which has no iOS equivalent for third-party apps.
- **Notification keyword filtering.** There is no notification-listener
  equivalent. A Focus mode can silence notifications system-wide, but its
  contents are never available to the app.
- **Per-section reasoning in the overlay.** Without a foreground signal there is
  nothing to put in a countdown that depends on which screen is open.
- **Custom rules over screen text.** Screen contents are unreadable by design.

## Distribution constraints

- The Family Controls entitlement is granted manually by Apple. Apps that
  request it are reviewed as parental-controls or self-control tools, and a
  request that does not fit that framing is rejected.
- The app cannot enumerate installed apps or read per-app usage directly; the
  system picker and `DeviceActivityReport` return tokens and aggregates.
- TestFlight builds need the entitlement too, so the review gate applies before
  any external testing.

## Assessment

A faithful port is not possible. An iOS Sober would be a **different product**:
app-level shields, schedules and focus sessions, with no section blocking and no
notification filtering. That is a legitimate app, but it is not this one, and
it would ship under Apple's entitlement approval rather than a Play declaration.

Recommendation: do not port before the Android launch. Revisit only if Apple
opens an event-based API for foreground app or screen state — at which point
the domain layer (`DetectBlockedSectionUseCase`, schedules, sessions) would be
reusable, and only the platform adapters would change.

## Sources

- [What's new in Screen Time API — WWDC22](https://developer.apple.com/videos/play/wwdc2022/110336/)
- [WWDC 2026: what changed for third-party blockers](https://habitdoom.com/blog/wwdc-2026-screen-time-wishlist)
