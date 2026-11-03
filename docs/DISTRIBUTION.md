# Distribution

Sober is distributed as an APK built from this repository. There is no Play
Store listing, because Google Play restricts the Accessibility-based blocking
category the app depends on.

| Channel | Who signs the APK | Updates | Status |
|---|---|---|---|
| [GitHub Releases](https://github.com/rishabhx29/Sober/releases) | The project key | Manual download | Current |
| [Obtainium](https://github.com/ImranR98/Obtainium) | The project key | Automatic, from GitHub Releases | Supported today |
| F-Droid | F-Droid's key, built from source | F-Droid client | Recipe ready, not submitted |
| Building from source | You | You | Supported |

The GitHub APK and an F-Droid APK are **not interchangeable** as updates: they
are signed with different keys, so Android refuses to install one over the
other. Moving between them means uninstalling, which deletes local state
(limits, sessions, streaks). Settings → Data → **Back up** writes a JSON file
first; restore it after reinstalling.

---

## Obtainium

Obtainium tracks GitHub Releases directly, so a new release appears as an app
update without any store.

1. Install Obtainium from its own
   [releases page](https://github.com/ImranR98/Obtainium/releases).
2. Tap **Add App** and paste `https://github.com/rishabhx29/Sober`.
3. Keep the default "Releases" source and confirm. Obtainium picks
   `Sober-<version>.apk` from each release and offers it as an update.

This works with the existing releases; nothing extra is published for it. The
APK remains signed by the project key, so updates install over the previous
version.

---

## F-Droid

F-Droid builds from source on its own infrastructure and signs the result with
its own key. The listing itself is not live yet; this section is the submission
path.

### What is already in place

- MIT licence (`LICENSE`), public source, public issue tracker.
- No proprietary dependencies: AndroidX, Compose, Hilt, Room, DataStore,
  Moshi, Coil, Lottie and Vico are all free software.
- **No `INTERNET` permission.** The permissions are usage access, notifications,
  NFC, display-over-other-apps, boot-completed and the foreground-service types,
  plus the Accessibility, notification-listener and Quick Settings tile bindings
  the system grants explicitly. Nothing is sent off the device.
- No binary assets that need a `NonFreeAssets` flag.
- Fastlane metadata under `fastlane/metadata/android/en-US/`: title, summary,
  full description, screenshots and a changelog for version code 6.

### Submitting

1. Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
2. Copy `docs/fdroiddata/com.sober.app.yml` to `metadata/com.sober.app.yml`.
3. Run `fdroid rewritemeta com.sober.app` and `fdroid lint com.sober.app`.
4. Open a merge request. The review checks the build recipe, the licence, the
   permission list and that the app can be built without network access to
   anything but the dependency mirrors.

The recipe pins the `v1.5` tag (version code 6). A new release needs a new
`Builds:` entry, not an edit to the old one; F-Droid keeps the old entry so it
can reproduce earlier versions.

**Build-server note.** The project compiles against API 37 (Android 17). F-Droid
can only build the recipe once its build server carries that platform; if the
merge request is blocked on it, the listing waits rather than shipping a
different SDK level.

---

## Reproducible builds

The intent is that anyone can rebuild the APK from a tag and compare it with the
published one. What is true today:

- The build is fully source-based and needs no secrets. Without the signing
  environment variables, `:app:assembleRelease` still succeeds and produces
  `app-release-unsigned.apk`, so a clean checkout can verify shrinking and
  resources.
- The project key is **not** in this repository. It lives outside the tree; a
  rebuild cannot produce the same signature, and signed APKs are not
  byte-identical across JDK and SDK patch levels.
- We therefore do **not** claim bit-for-bit reproducibility yet. F-Droid's own
  build is authoritative for F-Droid users; Obtainium and direct downloads use
  the signed GitHub APK.

To rebuild the code that shipped as 1.5:

```bash
git clone https://github.com/rishabhx29/Sober.git
cd Sober && git checkout v1.5
cd app
./gradlew :app:assembleRelease
# app/app/build/outputs/apk/release/app-release-unsigned.apk
```

Toolchain: JDK 17 or newer and Android SDK platform 37. The Gradle wrapper pins
the Gradle version. To inspect a published APK's signature:

```bash
apksigner verify --print-certs Sober-1.5.apk
```

---

## Permissions, for reviewers

| Permission | Why it is there |
|---|---|
| `PACKAGE_USAGE_STATS` | Reads per-app foreground time; the only source of usage data Android offers to a normal app. |
| `POST_NOTIFICATIONS` | The ongoing status, the daily-cap countdown and the optional warnings. |
| `SYSTEM_ALERT_WINDOW` (display over other apps) | Draws the block screen over the blocked app instead of launching an Activity and switching away. |
| `NFC` | Reads only the tag the user deliberately taps; normal permission, no prompt. |
| `RECEIVE_BOOT_COMPLETED` | Restarts the enforcement service after a reboot, so a limit cannot be escaped by rebooting. |
| `FOREGROUND_SERVICE` + `_DATA_SYNC` + `_SPECIAL_USE` | Runs the watchdog continuously. The `specialUse` subtype is declared with the reason "screen-time enforcement". |
| `BIND_ACCESSIBILITY_SERVICE` (declared by the service) | Section blocking, website address-bar reading and instant app-level blocking. Optional: without it the poll still enforces limits, one interval later. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` (declared by the service) | Hides notifications during a focus session. Optional. |
| `BIND_QUICK_SETTINGS_TILE` (declared by the tile service) | Lets the system bind the pause tile. |

No `INTERNET` permission is declared, so even a compromised build could not send
usage data anywhere.

---

## Versioning

- `versionName` is `X.Y`, `versionCode` increases by one per published release.
- Every release is tagged `vX.Y` and gets a changelog at
  `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.
- The F-Droid recipe's `CurrentVersion`/`CurrentVersionCode` must match the
  newest tag; `AutoUpdateMode: Version` + `UpdateCheckMode: Tags` lets F-Droid
  find it.
