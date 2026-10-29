# Sober — Privacy Policy

**Effective 30 October 2026.** Applies to the Sober Android app.

> **Sober works entirely on your device.** It has no internet permission, no
> analytics, no advertising and no third-party SDK that reports anything. The
> only way data leaves your phone is when you export it yourself.

## What Sober stores

All of this stays inside the app's private storage on your device:

- Daily usage totals per app, measured from Android's usage-access API.
- The daily limits, schedules, blocked sections and custom rules you set.
- Focus-session history and override history, so the app can show your streaks
  and the reasons a block was lifted.
- Your preferences: theme, notifications, onboarding state.
- Your optional PIN, stored as a salted PBKDF2 hash. Sober cannot read the PIN
  back, and there is no server that could.

## What leaves your device

**Sober itself sends nothing anywhere.** It declares no internet permission, so
it could not upload data even if it wanted to. There is no analytics, no crash
reporting, no advertising and no third-party SDK that collects data.

Two things can still move data off the phone, and both are your choices:

- **Export.** "Export usage data" writes a CSV file and opens Android's share
  sheet. You choose the destination; Sober does not know or keep a record of it.
  Exported files sit in the app cache until you share them or Android clears them.
- **Android system backup.** If you have backup enabled in your Google account
  settings, Android may copy Sober's preferences and usage records to *your own*
  backup. That is the operating system acting on your account, not Sober sending
  data to us — we never receive it. You can turn it off in Android Settings, or
  turn off backup for Sober specifically.

## Permissions and what each one is for

**Usage access.** Measures how long each app has been in the foreground, which
is how limits are counted. Sober reads durations, not screen contents.

**Display over other apps.** Shows the block screen on top of an app whose limit
you have reached.

**Notifications.** Reports session countdowns, limit warnings and streak
reminders.

**Notification access (optional).** Compares notification titles and text
against the keywords you configure, and can silence matching notifications while
a focus session is running. Matching happens in memory; notification content is
never stored, logged or sent. Turn it off in Android Settings at any time.

**Accessibility (optional, off by default).** Reads which app is in the
foreground and the structure of the current screen, so Sober can close a specific
section — Reels, Shorts, a home feed — instead of blocking the whole app. It does
**not** read what you type, does not read text fields, does not capture
screenshots and does not perform actions on your behalf outside of showing the
block screen. Visible on-screen text is compared in memory against the section
names and keywords you enabled; it is not stored. Everything degrades to
whole-app blocking when this permission is off.

**Device admin (optional).** Only makes it harder to uninstall Sober by accident
while blocking is on. It is never used to lock, wipe or remotely control the
device.

**Run at startup and foreground service.** Keeps enforcement running after a
reboot and while you use other apps. The foreground service shows a permanent
notification; Android requires it.

**Battery-optimisation exemption (optional).** Asks Android not to suspend
enforcement while the screen is off. You grant or deny this in system settings.

## Data sharing and selling

Sober does not sell, rent, share or transfer your data to anyone. There is no
account, no server and no cloud sync.

## Retention and deletion

Everything Sober keeps is in its private app storage. Uninstalling the app, or
clearing its data in Android Settings, deletes all of it. There is no
server-side copy to request, because no copy exists. Exported CSV files are
yours to delete wherever you sent them.

## Children

Sober is not directed at children under 13 and does not knowingly collect
information from them — it collects nothing at all.

## Changes to this policy

If the app's data practices change, this page is updated before the change
ships, and the effective date above changes with it. The same policy is
available inside the app under Settings → Privacy Policy.

## Contact

Questions about this policy or the app's data handling can be raised at
<https://github.com/rishabhx29/Sober/issues>.
