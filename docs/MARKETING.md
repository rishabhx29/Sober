# Marketing and Launch Playbook

Prepared 2026-11-03, at the 1.5 release. This is a channel map, not a promise:
each entry says what the audience is, what it accepts, and what the risk is.
Rules change; re-read the sidebar or wiki of any community before posting.

**Baseline when this was written:** 1 GitHub star, 0 forks, 0 downloads on
`Sober-1.5.apk` (released the same day), 2 on 1.3, no repository topics set.

---

## 1. What we are selling

**One line:** Sober is a free, open-source screen-time enforcer for Android that
cannot phone home — it declares no internet permission.

That last clause is the whole pitch. It is unusual, it is verifiable with one
command, and it is the thing no subscription app can copy without giving up its
business model.

| Proof point | How a stranger can check it |
|---|---|
| No `INTERNET` permission | `aapt2 dump permissions Sober-1.5.apk` |
| No account, no cloud, no sync server | The app has nowhere to send anything |
| Free, no ads, no in-app purchases | MIT licence, source on GitHub |
| Enforcement survives reboot | `RECEIVE_BOOT_COMPLETED` + watchdog service |
| Works without Accessibility (slower) | Poll fallback still enforces limits |
| Real tests | 527 JVM tests across 63 classes in the README |

**Differentiators to lead with**, in order of how much they matter to each
audience:

1. **Privacy crowd:** no network permission, no analytics, no account.
2. **Digital-minimalism crowd:** blocks Reels/Shorts/Explore sections and
   specific websites, not just whole apps.
3. **Self-control crowd:** opens-per-day limit, reflection before override,
   daily cap, focus sessions, lock mode.
4. **FOSS crowd:** MIT, Kotlin/Compose, small APK, no trackers, Obtainium.

**Say the limits first.** Trust is the product here.

- Android only; no iOS.
- Sideload only for now — not on Play Store, not yet on F-Droid.
- Needs Usage Access, and Accessibility for instant blocking.
- Some OEM battery managers will still kill background services.
- We do not claim bit-for-bit reproducible builds yet.

**Do not claim:** Play Store availability, F-Droid availability, iOS, "works on
every phone", "impossible to bypass", or "used by N people".

---

## 2. The launch kit (build this before posting anywhere)

Conversion happens on the GitHub release page. Every channel below sends people
there, so fix that page before spending reach on it.

| Asset | Why it matters | Status |
|---|---|---|
| Repository topics | GitHub search and topic pages are free discovery | Missing |
| Pinned Discussion | Where "how did you find us" and feedback live | Missing |
| 60-second demo video | The single highest-leverage asset | Not made |
| 10-second silent loop | README hero + X/Bluesky post | Not made |
| Install GIF (3 taps) | Removes the "sideloading is scary" objection | Not made |
| `Add to Obtainium` link | One tap for the FOSS crowd | Missing from README |
| Landing page or GitHub Pages site | Gives every bio/press mention one URL | Not made |
| OG image (1200×630) | Link previews on X, Reddit, Slack, Discord | Not made |
| `CONTRIBUTING.md` | "Stars" become "contributors" only with a path in | Missing |
| Feedback issue template | Turns drive-by users into bug reports | Missing |

Suggested topics: `android`, `screen-time`, `digital-wellbeing`,
`digital-minimalism`, `app-blocker`, `kotlin`, `jetpack-compose`, `foss`,
`privacy`, `offline`, `accessibility-service`, `self-control`.

Suggested pinned discussion: **"How did you find Sober?"** — with no analytics,
this is the only attribution you will get.

---

## 3. Distribution: the F-Droid ecosystem (highest intent)

This is the best-matched audience in existence for this app. Do it first.

| Channel | What it is | Action | Caveat |
|---|---|---|---|
| **IzzyOnDroid** | Largest third-party F-Droid-compatible repo, ~1,400 apps, hosts developer-signed APKs | Submit through the repository's "Adding apps" page at [izzyondroid.org](https://izzyondroid.org/); include metadata, licence, and a build the dev signed | No API 37 build-server wait, unlike F-Droid. The APK must be signed by the project key, which ours is |
| **F-Droid** | The main free-software store | Recipe is ready in `docs/fdroiddata/`; submit once the build server carries API 37 | Listing waits on their infrastructure, not on us |
| **Obtainium** | Updater that pulls APKs straight from GitHub releases | Add an `obtainium://add/https://github.com/rishabhx29/Sober` link to the README | Users need versionCode to rise every release — it does |
| **Neo Store / Droid-ify / F-Droid Classic** | F-Droid clients | Nothing to submit — they surface every repo you are in | Appears automatically once IzzyOnDroid or F-Droid lists you |
| **APKMirror / APKPure / Aptoide / Amazon Appstore** | General APK mirrors and stores | Optional, later | Update lag and trust dilution; a privacy app should not be re-signed or bundled by a third party. Skip until there is demand |

The FOSS funnel is slow but compounding: one IzzyOnDroid listing feeds every
F-Droid client, every "open source Android apps" video, and every
r/fossdroid recommendation thread for years.

---

## 4. Reddit (read the rules, one subreddit per day)

Reddit is where this audience actually is, and also where a careless launch gets
an account banned. The 90/10 norm applies: for every self-promotional post,
participate in nine threads without a link.

| Subreddit | Fit | What works | Rules and risk |
|---|---|---|---|
| [r/fossdroid](https://www.reddit.com/r/fossdroid/) | Very high | "I built an MIT-licensed screen-time blocker with no internet permission" + demo | Dev posts tolerated when you answer questions; huge overlap with IzzyOnDroid users |
| [r/opensource](https://www.reddit.com/r/opensource/) | High | Same post, more code/architecture detail | Low tolerance for marketing language; talk about the code |
| [r/androidapps](https://www.reddit.com/r/androidapps/) | High | The dedicated self-promotion/feedback thread | Current mod rule confines all self-promotion to that thread — a standalone post is removed |
| [r/SideProject](https://www.reddit.com/r/SideProject/) | High | Build story, numbers, what broke | Explicitly promo-friendly |
| [r/IMadeThis](https://www.reddit.com/r/IMadeThis/) | High | Same | Promo-friendly, small |
| [r/alphaandbetausers](https://www.reddit.com/r/alphaandbetausers/) | Medium | Install-and-feedback ask | Small but responsive |
| [r/digitalminimalism](https://www.reddit.com/r/digitalminimalism/) | Perfect audience, hostile to promo | The "Screen Time Apps to explore!" megathread and monthly threads only | Rule: no self-promotion of any kind. A standalone post will be removed and can get you banned — participate in the megathread and disclose you built it |
| [r/nosurf](https://www.reddit.com/r/nosurf/) | High | Answer "what actually works" threads with experience | Same promo sensitivity; check the pinned rules |
| [r/privacy](https://www.reddit.com/r/privacy/) | High | The no-`INTERNET`-permission proof | Skeptical and technical; bring the `aapt2` output, not adjectives |
| [r/degoogle](https://www.reddit.com/r/degoogle/) | High | "Nothing to degoogle because it cannot connect" | Same |
| [r/Android](https://www.reddit.com/r/Android/) | Massive, strict | Self-promo is normally removed | Skip unless a moderator invites a dev post |
| [r/IndiaTech](https://www.reddit.com/r/IndiaTech/) | Medium | India-built FOSS angle | Follow the same disclosure rules |

Post format that survives moderation: a title that states the problem, a body
that explains what you built and what it does **not** do, a link at the end, and
three hours of replies. Identical titles across subs trip spam filters — rewrite
each one.

---

## 5. Launch platforms

| Platform | Cost | Best for | How to use it |
|---|---|---|---|
| [Product Hunt](https://www.producthunt.com/) | Free | One-day spike, backlinks, press discovery | Launch Tue–Thu, 12:01 am PT. Gallery of 3 screenshots, 60-second video, first comment explaining the privacy angle. Reply all day. Never ask for upvotes — that is vote manipulation |
| [Hacker News — Show HN](https://news.ycombinator.com/submit) | Free | Technical credibility, GitHub stars | Title: `Show HN: Sober – an open-source Android screen-time blocker with no internet permission`. Post 8–11 am ET, Tue–Thu, and stay in the thread. No marketing language, no star requests. Expect "accessibility services are spyware" and answer it honestly |
| [Lobsters](https://lobste.rs/) | Free (invite) | FOSS/technical | Only if you have an invite; same tone as HN |
| [BetaList](https://betalist.com/) | Free tier | Early-adopter list | It is live, not pre-launch; still a valid directory entry |
| [Uneed](https://uneed.best/) | Free tier | Daily product discovery | Queue for a launch day |
| [MicroLaunch](https://microlaunch.net/) | Free | Small launch rounds | Low effort, low ceiling |
| [Fazier](https://fazier.com/) | Free | New-product directory | Same |
| [Peerlist Launchpad](https://peerlist.io/launchpad) | Free | Strong Indian dev audience | Good fit for a solo Indian developer story |
| [Indie Hackers](https://www.indiehackers.com/) | Free | Build-in-public narrative | Post the milestone, not the product: "shipped a no-internet Android app, here is what I learned" |

General startup directories are mostly link farms. Submit to a handful that
carry real traffic — Product Hunt, BetaList, Uneed, Peerlist — and skip the
"782 free directories" lists; the time is better spent replying to Reddit
comments. If you do a directory sweep, do it in one afternoon, not over a week.

### Software directories and alternative lists (SEO layer, do once)

These rank for "alternative to X" searches and send steady long-tail traffic.

| Site | Entry angle |
|---|---|
| [AlternativeTo](https://alternativeto.net/) | Alternative to Opal, One Sec, AppBlock, Forest, Digital Detox |
| [OpenAlternative](https://openalternative.co/) | Open-source alternative to screen-time subscriptions |
| [OpenSourceAlternative](https://www.opensourcealternative.to/) | Same |
| [Slant](https://www.slant.co/) | "Best screen-time blocker" questions |
| [SaaSHub](https://www.saashub.com/) | Comparison pages |
| [LibHunt](https://libhunt.com/) | Kotlin/Android OSS discovery |
| [StackShare](https://stackshare.io/) | Tech-stack profile; low consumer value |

### GitHub lists (PRs, not posts)

| Repo | Note |
|---|---|
| [offa/android-foss](https://github.com/offa/android-foss) | Curated FOSS-for-Android list; add under a screen-time/productivity section |
| [pcqpcq/open-source-android-apps](https://github.com/pcqpcq/open-source-android-apps) | Acceptance via PR |
| [fiedri/awesome-android-apps](https://github.com/fiedri/awesome-android-apps) | Open-source replacements for proprietary apps |
| [awesome-android topic](https://github.com/topics/awesome-android) | Find and contribute to active lists |

One accurate PR per list, following its contribution guide. Spraying the same
entry across twenty lists is how projects get blacklisted by maintainers.

---

## 6. Social

| Channel | Fit | Approach |
|---|---|---|
| **X** | High for build-in-public and tech reach | 6-post thread: hook → demo clip → the privacy proof (`aapt2` output as an image) → the failure modes → repo. Link in the last post or a reply, not the first. Reply to screen-time and "brain rot" discourse with substance, not links |
| **Mastodon / [fosstodon.org](https://fosstodon.org/)** | High for FOSS | Post the repo directly; this audience does not punish links. Hashtags: `#FOSS` `#Android` `#Privacy` `#DigitalMinimalism` |
| **Bluesky** | Medium-high | Mirror the X thread; smaller but friendlier |
| **Lemmy** ([lemmy.world](https://lemmy.world/)) | Medium | Post to the Android/FOSS/privacy communities; check each community's self-promo rule |
| **LinkedIn** | Low-medium | The "solo developer ships privacy-first app" story works; the product pitch does not |
| **YouTube Shorts / Reels / TikTok** | Highest ceiling for this niche | Vertical demo cut. This is where "POV: Instagram is locked until 6pm" reaches normal people, not developers |
| **Discord / Matrix** | Medium | FOSS Android and self-control communities; help first, mention the app only when asked |

### Content angles that fit this niche

1. "Your screen-time app has internet permission. Mine doesn't."
2. "I blocked Reels without blocking Instagram."
3. POV: tapping Instagram and the block screen appearing.
4. "24 minutes left today" — the daily-cap countdown.
5. "What will you do instead?" — the reflection gate.
6. Before/after of a week's stats.
7. "I rebuilt my own app's blocker because the existing ones stopped working."
8. How the Android foreground-detection bug that broke the block screen got fixed.
9. "No subscription, no account, no server — so where would your data go?"
10. A short teardown of what accessibility access can see, and what Sober actually reads.

---

## 7. Press and newsletters (pitch, do not spam)

| Outlet | What to send | Honest expectation |
|---|---|---|
| [Android Police](https://www.androidpolice.com/) | Tip: open-source blocker with no network permission | They run open-source roundups regularly |
| [XDA Developers](https://www.xda-developers.com/) | Same, plus the technical story (foreground detection) | Good fit for "how it works" pieces |
| [9to5Google](https://9to5google.com/) | App tip | Low hit rate |
| [Android Authority](https://www.androidauthority.com/) | App tip | Low hit rate |
| [It's FOSS](https://itsfoss.com/) | Open-source app submission | Strong fit for the licence and privacy angle |
| [FOSS Post](https://fosspost.org/) | Article pitch | Small, FOSS-focused |
| [MakeUseOf](https://www.makeuseof.com/) / [Lifehacker](https://lifehacker.com/) | Digital-detox angle | Usually paid placement; skip unless organic |
| [Hacker Newsletter](https://hackernewsletter.com/) | Submit the Show HN once it has traction | Curated from HN |
| [Android Weekly](https://androidweekly.net/) | Submit the project as an open-source app | Developer audience, not consumers |
| YourStory / Inc42 / The Better India | Human-interest: solo Indian developer, privacy-first, free | Only if there is a true story; do not fabricate a narrative |

The strongest press angle is not the feature list. It is the argument: **Play
Store economics push privacy-first apps toward sideloading, so here is one that
is verifiable instead of trusted.** That is a story a tech journalist can write.

### Creator outreach

Privacy and FOSS YouTube channels — [Techlore](https://www.youtube.com/@Techlore),
Side of Burritos, and smaller "open source Android apps" channels — cover apps
like this for free when the app is genuinely interesting. One short email each,
no payment, no follow-up spam, a demo clip attached. A 20-second clip they can
show is worth more than a press release.

---

## 8. The intro video

Make two cuts from one recording: 16:9 for YouTube/Product Hunt/X, 9:16 for
Shorts/Reels/TikTok. The first two seconds decide everything — show the block
screen appearing, not a logo.

| Time | Shot | On-screen text |
|---|---|---|
| 0–3s | Instagram opens, block screen slides over it | "I can't open Instagram. On purpose." |
| 3–10s | Home: daily cap and remaining time | "2 hours a day. 24 minutes left." |
| 10–20s | Apps list: limit an app, set opens, block Reels/Shorts sections | "Block the app. Or just the part that eats your day." |
| 20–28s | Website blocking while typing a URL in Chrome | "Also websites." |
| 28–36s | Focus session: notifications hidden, countdown | "Focus mode." |
| 36–44s | Override attempt → "What will you do instead?" | "It asks why first." |
| 44–52s | `aapt2 dump permissions` output, no `INTERNET` line | "No internet permission. It can't phone home." |
| 52–60s | Stats and streak, then the repo URL | "Free. Open source. No account. github.com/rishabhx29/Sober" |

Production notes:

- Record on the emulator at 1080×2400 in dark mode:
  `adb shell screenrecord --bit-rate 12M --size 1080x2400 /sdcard/demo.mp4`
- Burn in captions; most social views are muted.
- Music from a licensed library (YouTube Audio Library, Pixabay); no track that
  will get the post muted.
- Do not stage fake usage numbers or fake testimonials. The app is the proof.
- End card must show the URL as text, not a QR code — QR codes do not survive
  re-compression on social platforms.

---

## 9. Four-week cadence

| Week | Focus | Concrete output |
|---|---|---|
| 0 | Kit | Repo topics, pinned discussion, Obtainium link, landing page, demo recording |
| 1 | FOSS core | IzzyOnDroid submission, AlternativeTo/OpenAlternative entries, r/fossdroid post, X thread |
| 2 | Technical | Show HN, r/opensource, r/SideProject, Android Weekly submission |
| 3 | Consumers | Product Hunt launch, Shorts/Reels/TikTok cut, r/nosurf + digitalminimalism megathread participation |
| 4 | Press and retro | 6 outlet tips + 3 creator emails, then a DEV.to/Hashnode write-up of what worked. The write-up is itself distribution |

One channel per day. A launch is not a broadcast; it is a week of conversations.

---

## 10. Message bank

**Product Hunt tagline (60 chars):**
`Screen-time blocking that can't phone home. Free, open source.`

**Product Hunt first comment:** Start with why: every mainstream screen-time app
asks for network access and a subscription. Sober declares no internet permission
at all, is MIT licensed, and enforces limits on-device. Then list what it does,
then the honest limitations (sideload, Accessibility, Android only).

**Show HN title:** `Show HN: Sober – an open-source Android screen-time blocker
with no internet permission`

**Reddit title variants:**
- r/fossdroid: "MIT-licensed screen-time blocker for Android with no INTERNET permission (sideload, no account)"
- r/privacy: "I built a screen-time enforcer that literally cannot send data anywhere — it has no internet permission"
- r/SideProject: "Shipped a solo-dev Android app that blocks Reels and Shorts, and the hardest bug was detecting which app is actually on screen"

**X thread skeleton:** 1) the claim + demo clip, 2) what it blocks, 3) the
`aapt2` proof, 4) the two bugs that made 1.5 take a week, 5) what it does not do,
6) repo link.

**Email pitch (five sentences):** what it is, the one fact that makes it
unusual, who it is for, the demo link, and an offer to answer questions. No
attachment, no press kit demand.

### Objection handling

| Objection | Answer |
|---|---|
| "Accessibility services are spyware" | Correct to be suspicious. Sober reads the foreground package name and, for section rules, text labels in the active window. It cannot send anything — no internet permission. The service is optional; limits still enforce by polling |
| "Why sideload?" | Google Play policy makes this category difficult for small developers, and a privacy app should not need a store account. The APK is signed, hash-published, and the source is MIT |
| "Why not Play Store?" | Same, plus it would require an account and a signing relationship. GitHub releases keep the update path in the user's hands |
| "Will it block my bank app?" | Only apps you choose; system UI, keyboards and Settings are never blocked |
| "Battery?" | Foreground service with a 15-second cycle and a 2-second recheck only while a block is showing. No network, so no wakeups for sync |
| "iOS?" | No. Android exposes the foreground app; iOS does not in a way that allows this without Screen Time entitlements |

---

## 11. Rules and risks

- **Reddit:** 90/10 participation, one subreddit per day, disclose that you are
  the developer, no alt accounts, no DM campaigns, no vote requests. A removed
  post is a signal to stop, not to repost with a new title.
- **Product Hunt:** no upvote solicitation, no fake comments, no "support my
  launch" DMs. The platform detects rings.
- **Hacker News:** no marketing language, no star requests, no deleting critical
  comments. Answer the hard questions in public.
- **Everywhere:** never ask for stars in FOSS communities. Ask for feedback; the
  star follows the feedback.
- **Privacy consistency:** do not add analytics, UTM shorteners or a landing-page
  pixel that tracks users. That would make the central claim false.
- **Accuracy:** do not say F-Droid or Play availability before it is true, do not
  claim reproducible builds, do not claim a user count.

---

## 12. Measurement, given no analytics

The no-internet promise means no in-app attribution. That is a feature, and it
also means the funnel has to be measured from the outside.

| Signal | Where |
|---|---|
| APK downloads per release | `gh api repos/rishabhx29/Sober/releases/tags/v1.5` |
| Repository visitors and referring sites | GitHub → Insights → Traffic (the closest thing to channel attribution) |
| Stars, forks, watchers | GitHub API |
| Issues and discussions opened | GitHub notifications |
| "How did you find Sober?" discussion | Pinned discussion — qualitative attribution |
| Install → permission-complete drop-off | Only observable via user reports; ask in the discussion |
| IzzyOnDroid / F-Droid install counts | Their listing pages once accepted |

Targets worth setting for 30 and 90 days: stars, unique repository visitors from
non-GitHub referrers, and number of substantive issues. Ignore impressions.
