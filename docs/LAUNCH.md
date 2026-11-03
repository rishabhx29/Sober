# Sober launch kit

Everything needed to put Sober in front of people, in the order that does not get
the account banned. Copy is ready to paste; the sequence, the etiquette and the
answers to the awkward questions are here too.

**Status:** v1.6 is built and signed, the 25s film is rendered, repo metadata is
set. Nothing has been posted anywhere yet.

---

## 1. The facts you may repeat

Every claim below is true of the published APK. If a post needs a number that is
not here, it is not a claim we can make.

| Fact | Value |
|---|---|
| What it is | Android screen-time blocker: per-app limits, a phone-wide cap, focus sessions, section blocking, website blocking |
| Price | Free, MIT licence, no paid tier, no ads, no upsell |
| Network | The app declares **no internet permission**. It cannot send data anywhere |
| Data | No account, no cloud, no analytics, no third-party SDK |
| Size / requirements | 3.8 MB, Android 8.0 (API 26) or newer |
| Where to get it | GitHub Releases APK (not on Google Play) |
| Repository | https://github.com/rishabhx29/Sober |
| Download | https://github.com/rishabhx29/Sober/releases/latest |
| Film | `sober-promo.mp4` on the latest release, 25s, vertical |
| Code | Kotlin, Jetpack Compose, Clean Architecture, 527 JVM tests |
| Signing key | Unchanged since 1.0, so updates install over older versions |

**The one thing to be careful about:** Sober is not on Google Play and cannot be.
Apps that enforce limits need Accessibility and usage access, and Play restricts
that category. Say this plainly and early, before someone finds out and calls it
a red flag.

---

## 2. The pitch, in four lengths

**Eight words**
> Screen-time limits that hold when you don't.

**Twenty-five words**
> Sober gives you a daily limit per app and for the whole phone, blocks Reels and Shorts without blocking Instagram, and asks for no internet permission at all.

**Sixty words**
> Every screen-time app I tried could be dismissed in one tap, and most wanted an
> account and a cloud. Sober sets one number per app and per day, closes the app
> when the number runs out, and can block just the Reels or Shorts part of an app.
> It has no internet permission, so it cannot send anything anywhere. Free, MIT,
> 3.8 MB.

**One hundred and twenty words**
> I kept failing at screen time for a boring reason: every limit I set was a
> suggestion. A tap dismissed it, or an extra fifteen minutes for "just today",
> and the app was open again.
>
> Sober is the version that does not negotiate. You set a daily limit for an app,
> or one cap for the whole phone, and when the time runs out the app closes. It
> also blocks the part of an app that actually eats the hour — Reels, Shorts, the
> For You feed — without blocking the app itself. Focus sessions, lock mode and a
> bedtime window cover the rest.
>
> The part I am proudest of is what it does not do. No account, no cloud, no
> analytics, no internet permission at all. You can check that in the APK.
> It is free and open source, 3.8 MB, and works offline forever.

---

## 3. Channel plan

Post in this order. One community per day, never the same text twice.

| # | Where | What to post | Why it is worth it | Risk |
|---|---|---|---|---|
| 1 | GitHub release + README | v1.6 with the film attached | Gives every other post a link that works | none |
| 2 | X | One post with the film, then a thread | Fastest feedback, and the film travels | low |
| 3 | r/SideProject | Direct post with the film | Built for exactly this, no rule friction | low |
| 4 | r/fossdroid | Direct post, lead with "no internet permission" | The most sympathetic audience on Reddit | low |
| 5 | Hacker News (Show HN) | Text post, technical | Can send thousands of visitors in an hour, or zero | medium: read the Show HN rules first |
| 6 | r/androidapps | **Self-promotion megathread** first, not a standalone post | Highest-intent audience, strictest rules | high if posted wrong |
| 7 | r/digitalminimalism, r/nosurf | Story post, not an ad | The audience with the problem | medium: read the rules, some ban tool links |
| 8 | Product Hunt | Launch page | Listing that keeps working | low, needs a prepared first comment |
| 9 | AlternativeTo, Slant, LibHunt | Listings | Search traffic that compounds | low |
| 10 | dev.to / Hashnode | Build story with the film | Engineers share it; also ranks in search | low |
| 11 | Mastodon, Lemmy (FOSS instances) | Short post with the film | Small, friendly, no algorithm | low |
| 12 | F-Droid, Obtainium, awesome lists | Submission / PR | Distribution instead of a post | slow, weeks of review |

**Do not** post the same day to r/androidapps and r/Android and r/privacy and
r/degoogle. Four identical posts by the same account in one hour is what a spam
filter is built to catch, and subreddit moderators talk to each other.

---

## 4. Copy, ready to paste

### 4.1 GitHub release (v1.6)

Title:
```
v1.6 — the over-cap figure now matches the ring
```

Body:
```markdown
## What changed

One fix. The home screen showed "0m OVER CAP" under a ring that read
"4h 1m of 3h cap", because the tile clamped the remaining time to zero and
reused that clamped value for the over-cap figure. It now reports the surplus.

If you are not over your limit, nothing about your day looks different.

## Also in this release

The 25-second launch film is attached below (`sober-promo.mp4`, 1080x1920,
sound on). Nothing in it is a mockup: every screen is the app running on a
device.

## Install

1. Download `Sober-1.6.apk` below.
2. Tap it in your Downloads notification.
3. Allow installs from that source when Android asks, then Install.
4. If Play Protect says "unsafe app blocked", choose **More details → Install anyway**.
   It flags every sideloaded app it has not seen before.

Installs over 1.0-1.5; the signing key has not changed.

## Verify

SHA-256 (file):  ac0858e36996f95bf9cd1db0c2c77c064ef2db82615a525e41cc68d75f660f2d
SHA-256 (cert):  f9fc8e710485179fbad428689d72d949dea925129ff3ea5ddbef9fac63836c64
Signer:          CN=Sober, O=Sober, C=IN
```

### 4.2 X, main post

Attach `sober-promo.mp4`. Do not put the link in the post body; put it in the
first reply. Links in the body suppress reach.

```
I built a screen-time app that has no internet permission.

Not "we don't upload your data." The APK cannot reach the network at all.

Set a daily limit per app, or one cap for the phone. When the time is gone,
the app closes.

Free, MIT, 3.8 MB.
```

Then a reply within a minute:
```
Android 8+, sideload only (Play restricts accessibility-based blockers):
github.com/rishabhx29/Sober
```

### 4.3 X, thread (post the morning after the single post)

```
1/ Every screen-time app I tried had the same flaw: the limit was a suggestion.
   One tap, "15 more minutes", and the feed is back.

2/ Sober removes the suggestion. You set a number per app and per day, or one
   number for the whole phone. When it runs out, the app closes and stays closed
   until midnight.

3/ The better idea is section blocking. You do not have to block Instagram to
   stop Reels. Block the Reels tab, keep your messages. Same for Shorts and the
   Explore feed.

4/ Focus sessions, a bedtime window, lock mode behind a PIN, and a regret timer
   if you end a session early. All of it on the phone, none of it in a cloud.

5/ The proof: the app declares no internet permission. You can check it yourself:
   aapt dump permissions Sober-1.6.apk

6/ Free, MIT, 527 tests, 3.8 MB, Android 8+.
   github.com/rishabhx29/Sober
```

### 4.4 r/SideProject

Title:
```
I built a screen-time app that physically cannot phone home — 25s demo inside
```

Body:
```
I have tried most screen-time apps. The problem was never the tracking, it was
that every limit could be dismissed in one tap, and most of them wanted an
account to do it.

Sober is my version: a daily limit per app, a cap for the whole phone, and focus
sessions that hold when you are tired. It also blocks just the Reels or Shorts
part of an app instead of the whole app, which turned out to be the feature I
actually use.

The part I care about most is what it does not have. No account, no cloud, no
analytics, and no internet permission in the manifest. The APK cannot send your
usage anywhere because it has no network access at all. That is verifiable, not
a promise.

25 seconds, sound on: [video]
Repo, APK, screenshots: https://github.com/rishabhx29/Sober

It is free and MIT licensed. I am the author, so I will answer everything
honestly, including "why is this not on the Play Store" (short answer: Play
restricts accessibility-based blockers).

Happy to hear what is missing. Stats, iOS, and a smarter bedtime window are the
obvious gaps.
```

### 4.5 r/fossdroid

Title:
```
Sober: screen-time limits and section blocking, no internet permission, no trackers (MIT, 3.8 MB)
```

Body:
```
A screen-time blocker for Android that I have been building for myself.

What it does: per-app daily limits, one cap for the whole phone, timed focus
sessions, schedule and bedtime windows, and section blocking for Reels, Shorts
and For You feeds through the Accessibility API. Blocking a section does not
block the app.

Why it might interest this sub:
- No internet permission. Not "offline-first", physically no network access.
- No account, no cloud, no analytics, no crash reporting, no third-party SDK.
- MIT licensed, Kotlin + Compose, 527 JVM tests, builds from source with
  ./gradlew :app:assembleRelease.
- You can back up every rule to one JSON file and restore it on another phone.

Honest limits: it needs usage access and (for section blocking) Accessibility.
It is not on F-Droid yet; the recipe is in docs/DISTRIBUTION.md and a submission
is the next step. The APK is signed with a project key, so an F-Droid build
would not update over it.

Repo: https://github.com/rishabhx29/Sober
APK: https://github.com/rishabhx29/Sober/releases/latest

Feedback welcome, especially on the permission model and anything that smells
like it could leak.
```

### 4.6 Show HN

Title (keep it under 80 characters, no marketing words):
```
Show HN: Sober – Android screen-time blocker with no internet permission
```

URL field: `https://github.com/rishabhx29/Sober`

First comment, posted immediately:
```
Author here. I built this because every screen-time app I tried could be
dismissed in one tap, and the ones that worked wanted an account.

The enforcement is a foreground service plus the Accessibility API: per-app
limits come from UsageStats, section blocking comes from the node tree (it
closes Reels inside Instagram rather than closing Instagram). The interesting
constraint is that the app declares no INTERNET permission, so nothing can be
sent off the device even if a dependency wanted to. Usage never leaves the
phone because there is nowhere for it to go.

Two things are deliberately conservative. A day only counts toward a streak if
usage was actually observed and stayed under the cap, so an unobserved day is
never celebrated. And the block overlay re-reads the foreground every 2s while
it is up, because Android does not reliably tell you when the app underneath
comes back.

Known gaps: no root-based enforcement, in-app browsers are not readable (block
the host app instead), and no iOS version. 527 JVM tests, Kotlin + Compose,
MIT.

Happy to answer anything about the accessibility approach or the battery cost.
```

### 4.7 r/androidapps

That subreddit runs a recurring self-promotion thread. Post there first, even if
a standalone post is also allowed today. If you do post standalone, use its
required developer format and put the disclosure in the first line.

```
[DEV] Sober — per-app limits that hold, section blocking for Reels/Shorts, no internet permission

I am the developer. Free, open source, no ads, no paid tier, no account.

Most screen-time apps have the same hole: the limit is one tap away from being
ignored. Sober's limit closes the app and keeps it closed until midnight. You
can also block only the Reels or Shorts section instead of the whole app.

What is in it: per-app daily limits with a 7-day chart, a phone-wide cap, focus
sessions with an optional regret timer, lock mode behind a PIN, bedtime and
schedule windows, category budgets, website blocking inside the browser,
allow-only mode, backup/restore to one JSON file, a Quick Settings tile and a
home-screen widget.

Privacy: no internet permission, no account, no analytics, no third-party SDKs.

Requires Android 8.0+. Sideload from GitHub, because Play restricts
accessibility-based blockers:
https://github.com/rishabhx29/Sober/releases/latest

The first thing to know before installing: it is signed with my project key and
not published on Play, so Play Protect will ask you to confirm. That is normal
for any app it has not seen before.
```

### 4.8 r/digitalminimalism and r/nosurf

Do not post the same text to both. Neither is an app-launch subreddit, so lead
with the idea and mention the app once, at the end, as the thing you built out
of it.

Title:
```
The limit didn't fail. The fact that I could dismiss it did.
```

Body:
```
For two years I tried to cut down with every app timer I could find. What I
eventually noticed was that I was not losing to the app, I was losing to the
dismiss button. A limit you can overrule in a second is not a limit, it is a
reminder, and reminders are exactly what a tired brain is good at ignoring.

The version that finally worked for me had three properties:
- it closed the app instead of asking me to close it,
- it covered the specific thing I lost time to (Reels, Shorts) rather than the
  app I still need for messages,
- and it made changing the rule slow enough that the impulse passed.

I got tired of waiting for one, so I built it and put it on GitHub for free.
Link in a comment if you want it; no pressure either way, the three properties
are the useful part.
```

### 4.9 Product Hunt

Tagline (60 characters max):
```
Screen-time limits that hold when you don't
```

Description:
```
Sober sets a daily limit per app and one for the whole phone, then closes the app when the time runs out. Block Reels or Shorts without blocking Instagram. No account, no cloud, and no internet permission at all. Free, open source, Android 8+.
```

First comment:
```
Maker here. I built Sober after failing with every other screen-time app for the
same reason: the limit was a suggestion and I was very good at accepting it.

Three decisions shaped it:

1. The limit is structural, not advisory. The app closes, and the rule cannot be
   edited for a set number of hours unless you enter your PIN.
2. Section blocking. You can keep Instagram for messages and still close Reels.
   That is the feature that changed my own usage.
3. No internet permission. Not a privacy policy claim, a manifest fact. The APK
   cannot send anything anywhere.

It is free, MIT licensed, 3.8 MB, and needs Android 8 or newer. Not on Google
Play because Play restricts accessibility-based blockers, so it is distributed
as a signed APK on GitHub.

Ask me anything, including the uncomfortable questions about Accessibility.
```

### 4.10 Listings without a post

| Site | Field | Text |
|---|---|---|
| AlternativeTo | Short description | Free, open-source Android screen-time blocker with per-app limits, section blocking for Reels and Shorts, and no internet permission. |
| AlternativeTo | Alternatives to | Digital Wellbeing, Opal, one sec, Forest, AppBlock |
| Slant | Pros (3) | No internet permission; blocks sections not whole apps; free and open source |
| Slant | Cons (2) | Sideload only, no Play Store listing; Android only |
| LibHunt | Tagline | Private screen-time limits for Android, no internet permission |
| Awesome lists | One line | [Sober](https://github.com/rishabhx29/Sober) — On-device screen-time and focus enforcer with per-app limits and section blocking. No internet permission. |

### 4.11 dev.to / Hashnode build story

Title:
```
I shipped a network feature by not adding one
```

Angle: the two bugs that shaped the app — an Android foreground-detection gap
that left the block screen looking frozen, and a display bug where a clamped
number was reused for a second question. Both are documented in `GUIDE.md`, so
the post is real engineering, and the project link belongs at the bottom. This
is the post engineers share, and it keeps bringing people months later.

---

## 5. Etiquette, non-negotiable

1. **Disclose in the first line.** "I am the developer" or "I built this". Not at
   the bottom, not implied.
2. **One community per day.** Answer every comment for the first three hours.
   A post you abandon in an hour dies.
3. **Never ask for stars in a title.** State that it is open source and let
   people decide. Astroturfing reads instantly and costs the project its
   credibility permanently.
4. **Do not post the same body twice.** Rewrite the opening for each audience:
   privacy people get the permission model, minimalists get the dismissal
   problem, developers get the engineering.
5. **If a moderator removes it, reply once, politely, and stop.** Do not repost
   the same day. Ask what would make it acceptable.
6. **No vote requests, no alt accounts, no "upvote so others see it".**
7. **Answer criticism with the limitation, not a defence.** "Yes, it needs
   Accessibility for section blocking, and here is what that means" beats
   arguing.
8. **Do not claim a user number you do not have.**

---

## 6. Answers to the questions that will come

**"Accessibility is a keylogger permission. Why should I trust you?"**
Fair. Accessibility can read screen content, and that is exactly why section
blocking works. Sober reads the node tree only to find the tab you are on, and
that data stays in memory. The app has no internet permission, so there is no
path off the device. The whole source is MIT and published; the service code is
in `system/services`.

**"Why is it not on Play Store?"**
Play restricts apps that use Accessibility to block other apps. A listing would
be removed. It ships as a signed APK on GitHub instead, with a stable key so
updates install over the previous version.

**"Is this safe to sideload?"**
It is signed with a project key, the certificate hash is published in the
release, and you can rebuild the code from the tagged commit. Play Protect will
still warn because it has not seen the app before.

**"Why not just use Digital Wellbeing?"**
Digital Wellbeing can be turned off in Settings in about four seconds. Sober
puts rule changes behind a PIN and a timer. If the built-in tools work for you,
keep them.

**"Does it drain battery?"**
Limits come from UsageStats, which the system already collects. The overlay is
only in memory while something is blocked. The foreground service runs during
focus sessions.

**"iOS?"**
No. The enforcement model here depends on Android APIs (UsageStats, overlay
windows, Accessibility) that iOS does not expose to third-party apps.

**"Can I turn it off?"**
Yes, you are an adult. Everything is in Settings, including pausing all
enforcement, and uninstalling is uninstalling. The app is designed to be a
decision you made once, not a cage.

**"What happens to my data if I back up?"**
One JSON file, written wherever you choose. No server exists to receive it.

---

## 7. Ten-day sequence

| Day | Action |
|---|---|
| 0 | Publish v1.6 with the film attached. Verify the download link and the video link from a phone and from a browser. |
| 1 | X: one post with the film, link in the first reply. |
| 2 | r/SideProject, direct post. |
| 3 | r/fossdroid, direct post. |
| 4 | Show HN, plus the first comment immediately. Do not touch it again that day. |
| 5 | r/androidapps self-promotion thread. |
| 6 | r/digitalminimalism, story angle. |
| 7 | Product Hunt launch page. |
| 8 | r/nosurf with its own rewritten opening. |
| 9 | AlternativeTo, Slant, LibHunt listings; dev.to build story. |
| 10 | Read the feedback, ship one thing people asked for twice, and reply to every open thread with what changed. |

---

## 8. What to measure

- **GitHub traffic → views.** Repository insights shows referrers, which is the
  only free attribution available. Note which post produced which referrer.
- **Download count** on the release asset. `gh release view v1.6 --json assets`.
- **Stars per day**, not total stars. A launch post that adds 3 stars is not
  working; 40 in a day is.
- **Issue quality.** One real bug report is worth more than fifty stars.
- **Comments asking "how do I..."** tell you what the README is missing.

Do not measure impressions. There is nothing to learn from them.

---

## 9. Asset checklist

| Asset | Path | Use |
|---|---|---|
| Film | `promo/renders/sober-promo-vertical.mp4` (also on the release) | X, Reddit video posts, Product Hunt gallery |
| Cover still | `promo/stills/cover.png` | Post image, listing thumbnail |
| Block screen still | `promo/stills/block.png` | The single most convincing image |
| End card | `promo/stills/end-card.png` | Product Hunt gallery, release notes |
| Seven screens | `docs/assets/screens/` | README, AlternativeTo, listing gallery |
| Icon, banner | `promo/assets/icon.png`, `docs/assets/banner.png` | Already in use |

Reddit and X both prefer a native upload to a link. Upload the MP4 directly;
use the GitHub link as the download destination and nothing else.
