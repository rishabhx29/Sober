# Sober launch kit

Everything needed to put Sober in front of people, in the order that does not get
the account banned. Copy is ready to paste; the sequence, the etiquette and the
answers to the awkward questions are here too.

**Status:** v1.6 is published at
https://github.com/rishabhx29/Sober/releases/tag/v1.6 with `Sober-1.6.apk` and
`sober-promo.mp4`. Repo topics, description and homepage are set, and the README
links the film.

Posted so far:
- X, from `@RishabhTri8805`: the 25 s film plus one reply with the GitHub link.
- r/SideProject: https://www.reddit.com/r/SideProject/comments/1xnfxe3/

Baseline when this section was written: 2 APK downloads on v1.6, 1 star,
98 unique clones in the last 14 days (almost all of them this machine).

The rest of the list is one community per day — a new account that posts four
subreddits in an hour gets filtered before anyone sees it.

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
> The manifest has no INTERNET permission. No account, no cloud, no analytics,
> and nothing that could leave the phone even if something tried. You can check
> that on the APK yourself. It is free and MIT licensed, 3.8 MB, Android 8+.

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
I shipped the screen-time app I wished existed.

It shuts Reels when you are done and leaves Instagram DMs open. Run out of time and the app stays closed until midnight.

No INTERNET permission in its manifest. Nothing to phone home to.

Free, MIT, 3.8 MB. Try it, link below.
```

Then a reply within a minute. The install link and the ask both live here, because
a link in the body costs reach and a bare fact does not get anyone to tap:
```
Install: github.com/rishabhx29/Sober

Android 8+, sideload only (Play restricts accessibility-based blockers). If it earns a spot on your phone, a star on the repo is the whole marketing budget.
```

### 4.3 X, thread (post the morning after the single post)

```
1/ I tried a lot of screen-time apps and every one of them lost to the same
   button: "15 more minutes". At 11pm I am very good at accepting suggestions.

2/ Sober sets a number per app per day, or one number for the phone. When it
   runs out the app closes and stays closed until midnight. Changing the rule
   takes a PIN.

3/ The part I use every day is section blocking. Reels gets closed, Instagram
   stays open for messages. Same for Shorts and the Explore feed. It reads the
   screen through the Accessibility API and closes the one screen you named.

4/ Focus sessions, a bedtime window, and a regret timer that waits before
   unlocking if you end a session early. All of it runs on the phone.

5/ No INTERNET permission in the manifest. You can check it on the APK:
   aapt dump permissions Sober-1.6.apk

6/ Free, MIT, 527 tests, 3.8 MB, Android 8+.
   github.com/rishabhx29/Sober
```

### 4.4 r/SideProject

Title:
```
I shipped a screen-time blocker that has no INTERNET permission — 25s demo
```

Body:
```
I have tried most of the screen-time apps. What killed each one was the dismiss
button. A limit you can overrule in one tap is a reminder, and I am bad at
reminders.

So I shipped my own. Daily limits per app, one cap for the whole phone, focus
sessions. The one I use every day is section blocking: Reels closes, Instagram
stays open for messages.

The manifest has no INTERNET permission. Whatever a dependency might want, the
APK has no route out, and you can check that on the release file.

25 seconds, sound on: [video]

Try it: https://github.com/rishabhx29/Sober — Android 8+, 3.8 MB, free, MIT.
If it earns a spot on your phone, star the repo. That is the whole marketing
budget.

I will answer everything here, including why there is no Play Store listing
(Play restricts accessibility-based blockers).

Known gaps: no iOS version, and in-app browsers cannot be read, so block the
host app instead.
```

### 4.5 r/fossdroid

Title:
```
Sober: screen-time limits and section blocking, no internet permission, no trackers (MIT, 3.8 MB)
```

Body:
```
A screen-time blocker for Android that I have been building for myself.

Per-app daily limits, one cap for the whole phone, focus sessions, schedule and
bedtime windows, and section blocking through the Accessibility API: Reels,
Shorts and the For You feed get closed while the rest of the app stays usable.

For this sub specifically, the manifest has no INTERNET permission. There is no
account, no analytics, no crash reporting and no third-party SDK. MIT, Kotlin and
Compose, 527 JVM tests, and ./gradlew :app:assembleRelease builds it from source.
Every rule exports to one JSON file and restores on another phone.

It needs usage access, and Accessibility if you want section blocking. It is not
on F-Droid yet; the recipe is in docs/DISTRIBUTION.md. One thing to know before
installing: this APK is signed with my own key, so an F-Droid build would not
update over it.

Repo: https://github.com/rishabhx29/Sober
APK: https://github.com/rishabhx29/Sober/releases/latest

Tell me what looks wrong, especially around the permission model.
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

Enforcement is a foreground service plus the Accessibility API. Per-app limits
come from UsageStats. Section blocking walks the node tree, which is how it
closes Reels inside Instagram rather than closing Instagram. The manifest has no
INTERNET permission, so usage has nowhere to go even if a dependency tried.

Two decisions worth mentioning. A day only counts toward a streak if usage was
actually observed and stayed under the cap, so an unobserved day is never
celebrated. And the block overlay re-reads the foreground every 2 seconds while
it is up, because closing Recents back into an app emits a launcher pause with no
resume, so the obvious "wait for a resume" logic clears the cover and leaves the
user inside the blocked app.

Known gaps: no root-based enforcement, in-app browsers are not readable (block
the host app instead), no iOS version. 527 JVM tests, Kotlin and Compose, MIT.

Ask me anything about the accessibility approach or what it costs in battery.
```

### 4.7 r/androidapps

That subreddit runs a recurring self-promotion thread. Post there first, even if
a standalone post is also allowed today. If you do post standalone, use its
required developer format and put the disclosure in the first line.

```
[DEV] Sober — per-app limits that hold, section blocking for Reels/Shorts, no internet permission

I am the developer. Free, open source, no ads, no paid tier, no account.

I kept ignoring my own screen-time limits, so this one closes the app and keeps
it closed until midnight. It can also close only the Reels or Shorts section and
leave the rest of Instagram usable, which is the part I actually wanted.

Inside: per-app daily limits with a 7-day chart, a phone-wide cap, focus
sessions with an optional regret timer, lock mode behind a PIN, bedtime and
schedule windows, category budgets, website blocking inside the browser,
allow-only mode, backup to one JSON file, a Quick Settings tile and a
home-screen widget.

Privacy: no INTERNET permission in the manifest, no account, no analytics, no
third-party SDKs.

Requires Android 8.0+. Sideload from GitHub, because Play restricts
accessibility-based blockers:
https://github.com/rishabhx29/Sober/releases/latest

Before you install: the APK is signed with my own key and is not on Play, so Play
Protect will ask you to confirm. That happens with anything it has not seen
before, and the source is public if you want to build it yourself.
```

### 4.8 r/digitalminimalism and r/nosurf

Do not post the same text to both. Neither is an app-launch subreddit, so lead
with the idea and mention the app once, at the end, as the thing you built out
of it.

Title:
```
I kept dismissing my own screen-time limits, so I built one I couldn't
```

Body:
```
For two years I tried to cut down with app timers. The pattern I eventually
noticed was that I never lost to the app. I lost to the dismiss button. A limit
you can overrule in a second is a reminder, and a tired brain is very good at
ignoring reminders.

What finally worked had three properties worth stealing whether or not you use
my app:
- it closed the app rather than asking me to close it,
- it covered the specific thing I lost time to (Reels, Shorts) and left the app
  I need for messages alone,
- changing the rule was slow enough that the impulse had usually passed by the
  time I could.

I got tired of waiting for one that worked this way, so I built it and put it on
GitHub for free. Link in a comment if you want it. The three properties are the
useful part either way.
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
same reason: the limit was a suggestion, and I was very good at accepting it.

Two things shaped it. The limit is structural, so the app closes and the rule
cannot be edited for a set number of hours without your PIN. And section
blocking, where Reels closes and Instagram stays open for messages. That second
one changed my own usage more than any limit ever did.

The manifest has no INTERNET permission. You can check that on the APK rather
than take my word for it in a policy.

Free, MIT licensed, 3.8 MB, Android 8 or newer. Not on Google Play because Play
restricts accessibility-based blockers, so it ships as a signed APK on GitHub.

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

### The commands

APK downloads per asset. This is the only install count that exists, because the
app has nowhere to report to:

```
gh release view v1.6 --json assets --jq '.assets[] | "\(.name) \(.download_count)"'
gh api repos/rishabhx29/Sober/releases --jq '.[] | "\(.tag_name) " + ([.assets[] | "\(.name)=\(.download_count)"] | join(" "))'
```

Stars, and the moment each one landed:

```
gh api repos/rishabhx29/Sober --jq '{stars: .stargazers_count, forks: .forks_count}'
gh api -H "Accept: application/vnd.github.star+json" repos/rishabhx29/Sober/stargazers --jq '.[] | "\(.starred_at) \(.user.login)"'
```

Referrers and views. GitHub keeps 14 days of this, so save it if you want a
trend:

```
gh api repos/rishabhx29/Sober/traffic/views --jq '{count, uniques}'
gh api repos/rishabhx29/Sober/traffic/popular/referrers --jq '.[] | "\(.referrer) \(.count)"'
gh api repos/rishabhx29/Sober/traffic/popular/paths --jq '.[] | "\(.path) \(.count)"'
```

Read download counts as a delta, not a total. One fetch is one download, so a
mirror or a bot counts the same as a person. Take the number before you post and
again a day later; the difference is what the post did.

### The two platform dashboards

- **X**: `x.com/i/account_analytics` gives impressions, profile clicks and link
  clicks per post. On an account with no followers almost every impression comes
  from the reply carrying the link, not from the post.
- **Reddit**: open your own post and use "View insights" for views and upvote
  rate. This is the one that tells you whether a subreddit is worth a second
  post.

### What the app itself reports

Nothing, and that is the point of it. With no INTERNET permission no analytics SDK
can run, so the install count lives only where the file was downloaded. If you
ever want funnel numbers, you will have to ask users, not measure them.

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
