# Where Sober can be listed

Written 2026-11-04. Every entry below is a place a person looking for a screen-time
app might actually land. The gate column is the honest reason it has not happened
yet: most of these want an account, and a captcha cannot be signed by an agent.

The copy is ready. Filling a form is a two-minute job, not a writing job.

---

## The one-liners

Submission forms ask for the same three lengths. Use these, not a rewrite.

**Tagline, 48 chars**
```
Screen-time limits that actually close the app
```

**Short, 158 chars**
```
Daily limits per app, one cap for the phone, focus sessions, and section blocking:
Reels closes while Instagram DMs stay open. No INTERNET permission. Free, MIT, 3.8 MB.
```

**Long, 78 words**
```
Sober is an Android screen-time blocker for people who keep overruling their own
limits. Set a number per app or one for the whole phone; when it runs out the app
closes and stays closed until midnight. Section blocking closes Reels while leaving
Instagram messages alone, and the same works for Shorts and the Explore feed. The
manifest has no INTERNET permission, so nothing can send usage anywhere. Free, MIT
licensed, 3.8 MB, Android 8 and up, signed APK from GitHub.
```

Everywhere it asks for a category: **Productivity > Habit tracking**, or
**Health & Fitness > Digital wellbeing**. Never pick "Self-improvement" and never
pick a paid tier.

---

## The list

| # | Where | Link | Gate | Status |
|---|---|---|---|---|
| 1 | Product Hunt | producthunt.com | account, and a scheduled 12:01am PT day | not started |
| 2 | AlternativeTo | alternativeto.net/manage/submit/ | account, free signup | not started |
| 3 | Slant | slant.co | account | not started |
| 4 | LibHunt | libhunt.com | GitHub OAuth | not started |
| 5 | OpenAlternative | openalternative.co/submit | form, no account needed | not started |
| 6 | IzzyOnDroid | codeberg.org/IzzyOnDroid/repo/issues | Codeberg account | not started |
| 7 | F-Droid (RFP) | gitlab.com/fdroid/rfp/-/issues | GitLab account | not started |
| 8 | offa/android-foss | github.com/offa/android-foss | GitHub PR | blocked: 6-month rule |
| 9 | Show HN | news.ycombinator.com/submit | HN account | not started |
| 10 | dev.to | dev.to/new | account | not started |
| 11 | Mastodon (fosstodon.org) | fosstodon.org | account | not started |
| 12 | Lemmy (!android@lemmy.ml) | lemmy.ml | account | not started |
| 13 | Reddit | see `LAUNCH.md` §3 | new account, karma gate | one post filtered |

### 1. Product Hunt

The only listing with a real one-day spike. Needs the gallery: `cover.png`,
`block.png`, `end-card.png` and `sober-promo.mp4` are all in `promo/`.

- Name: `Sober — screen-time limits that close the app`
- Tagline: the 48-char line above
- First comment, paste this:
```
I built this because every blocker I tried had the same flaw: the dismiss button.
"15 more minutes" is a suggestion, and at 11pm I accept suggestions.

Sober counts usage per app and closes the app when the number runs out. Section
blocking is the part I use daily: Reels closes, Instagram keeps working for
messages. The manifest has no INTERNET permission, so there is no server that
could ever receive your usage.

It is free and MIT licensed. Android 8 and up, sideload only, because Play restricts
apps that rely on the Accessibility API. Ask me anything about the block-screen
lifecycle bug that took two releases to fix.
```

### 2. AlternativeTo

The long tail. People search "alternative to Forest" or "alternative to
one sec" for years and this is where they land.

- Add as a new application, not as an alternative.
- Description: the 158-char line.
- License: MIT. Cost: free.
- Platforms: Android.
- Tags: `screen-time`, `digital-wellbeing`, `productivity`, `open-source`, `offline`.
- Then list it as an alternative to: Forest, one sec, AppBlock, StayFree,
  Digital Detox, ScreenZen. This is what makes it findable.

### 3. Slant

- "What is the best screen time app for Android?" is the question to answer under.
- Write the answer as a short comparison, not a listing: what it does, who it is
  wrong for.

### 4. LibHunt

- GitHub OAuth, then "Submit a project".
- It indexes the GitHub repo and shows stars, so it also feeds a second page of
  SEO. Pastes the 158-char line and the topics.

### 5. OpenAlternative

- No account, which makes it the cheapest listing on this page.
- Category: Productivity.
- Link: the GitHub repo, not the APK.

### 6. IzzyOnDroid

The fastest route to a real F-Droid-style repository, and the one that keeps
delivering installs without anyone posting anything. It accepts an APK the
developer signed themselves, so there is no build-server round trip.

- Request format, in the issue body:
```
App: Sober
Package: com.sober.app
Repo: https://github.com/rishabhx29/Sober
License: MIT
APK: https://github.com/rishabhx29/Sober/releases/download/v1.6/Sober-1.6.apk
Version: 1.6 (versionCode 7), minSdk 26
Description: Screen-time limits that close the app until midnight, with section
blocking (Reels closes, Instagram DMs stay open). No INTERNET permission.
```
- Report back: if they accept, the repo URL is
  `https://apt.izzysoft.de/fdroid/index/apk/com.sober.app`.

### 7. F-Droid RFP

Slower than IzzyOnDroid and stricter: F-Droid builds from source with its own
toolchain, so the Gradle build has to work from a clean checkout with no
proprietary dependency. Read their inclusion policy before filing, and file the
RFP only after IzzyOnDroid has accepted — a reviewer there will often point out
the same problems first.

### 8. offa/android-foss

11.6k stars and the list people actually read. Their criteria say **at least six
months of active development**, which this project cannot claim until roughly
2027-05. Add it to the queue and do not waste a PR before then.

When it is time, the entry goes in the Digital Wellbeing or Productivity section
in alphabetical order:
```markdown
* [**Sober**](https://github.com/rishabhx29/Sober) <sup>**[[IzzyOnDroid](https://apt.izzysoft.de/packages/com.sober.app)]**</sup>
```

### 9. Show HN

The single biggest shot at GitHub stars, and the one place where the
no-INTERNET-permission angle is the story rather than a footnote.

- Title: `Show HN: A screen-time blocker with no internet permission`
- First comment, posted by you immediately after:
```
The reason for no INTERNET permission is simple: I did not want a screen-time app
that could send usage anywhere, so I removed the ability instead of promising not to.

The bug that ate the most time was the block screen staying up. Closing Recents back
into an app emits a launcher pause with no matching resume, so the obvious "wait for
a resume" logic clears the cover and drops the user inside the blocked app.

Sideload only: Play restricts apps that use the Accessibility API to read the screen.
```

### 10-12. dev.to, Mastodon, Lemmy

Long tail, no spike, near-zero effort each. Post the same short write-up that
`LAUNCH.md` §4.7 uses, with the film embedded. On Mastodon and Lemmy, lead with
the no-INTERNET point; those two audiences care about it more than the features.

### 13. Reddit

Read `LAUNCH.md` §3 for the subreddits and §5 for the one-community-per-day rule.
The gate is account karma, not content, so expect the first two or three attempts
to be filtered. A post with no links survives more often than a post with two.

---

## What actually moves the download counter

In order, for this app:

1. **IzzyOnDroid** — a listing that keeps feeding installs for months with no
   further work from you.
2. **Show HN** — one morning, potentially hundreds of repo visits, and the story
   is genuinely unusual.
3. **Product Hunt** — one day, a real spike, needs a gallery and a comment to be
   ready in advance.
4. **Reddit** — the best audience fit, the worst gate for a new account.
5. **AlternativeTo** — slow, compounding, invisible on the day.

Everything else is a rounding error on top of those five.
