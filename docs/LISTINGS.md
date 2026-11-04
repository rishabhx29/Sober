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
| 1 | Product Hunt | producthunt.com | account, and a scheduled 12:01am PT day | product page public at `/products/sober-3`; launch scheduled 2026-11-05 |
| 2 | AlternativeTo | alternativeto.net — the form lives at `/manage-item/` (`/manage/submit/` 404s) | account, free signup | submitted 2026-11-04 as "Sober Screen Time"; waiting for review, private until then |
| 3 | Slant | slant.co | account | not started |
| 4 | LibHunt | libhunt.com | GitHub OAuth | not started |
| 5 | OpenAlternative | openalternative.co/submit | login required; free listings go live one per weekday | not started |
| 6 | IzzyOnDroid | codeberg.org/IzzyOnDroid/repo/issues | Codeberg account | not started |
| 7 | F-Droid (RFP) | gitlab.com/fdroid/rfp/-/issues | GitLab account | not started |
| 8 | offa/android-foss | github.com/offa/android-foss | GitHub PR | blocked: 6-month rule |
| 9 | Show HN | news.ycombinator.com/submit | HN account | blocked by HN's Show HN restriction for new accounts |
| 10 | dev.to | dev.to/new | account | not started |
| 11 | Mastodon (fosstodon.org) | fosstodon.org | account | not started |
| 12 | Lemmy (!android@lemmy.ml) | lemmy.ml | account | not started |
| 13 | Reddit | see `LAUNCH.md` §3 | new account, karma gate | one post filtered |
| 14 | awesome-digital-wellbeing | pull/14 | none, PR | open: https://github.com/jcanfield/awesome-digital-wellbeing/pull/14 |
| 15 | awesome-android-apps | pull/22 | none, PR | open: https://github.com/fiedri/awesome-android-apps/pull/22 |
| 16 | Uneed | uneed.best/submit-a-tool | account | submitted 2026-11-04, queued for 2027-06-13; shareable preview link exists |
| 17 | Indie Hackers | indiehackers.com/products/new | account; publishing gated to established members | product page live, posting blocked |
| 18 | X | x.com | account | posted 2026-11-04, live |

Two of these need nothing but a GitHub session, so they are already in front of
a maintainer. Everything above them needs an account this machine does not hold.

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

Filled 2026-11-04 and scheduled for 2026-11-05. The gallery needs landscape
frames, and every existing still is 1080×1920, so the first image is a board built
for the purpose: `promo/ph/gallery.html` renders to `promo/ph/gallery-1270x760.png`
at exactly the size PH asks for, with the two phone captures inside it. The other
two gallery slots are `promo/stills/block.png` and `promo/stills/end-card.png`.

### 2. AlternativeTo

The long tail. People search "alternative to Forest" or "alternative to
one sec" for years and this is where they land.

Submitted 2026-11-04. The form is at `/manage-item/`; the old `/manage/submit/`
path 404s.

- The name had to be qualified: an unrelated Roblox client already owns
  `/software/sober/`, and the form refuses a second "Sober". "Sober Screen Time"
  is the form the site itself suggests ("add some other text to it like company
  name or similar").
- Description, license, platforms, tags, icon and two screenshots as below.
- The listing is at `/software/sober-screen-time/` and says "Your submission is
  waiting to be reviewed … Only you can see this app right now, so please do not
  share the link to it yet."
- Five real alternatives were attached, which is what the empty-listing warning
  asks for and what puts the app on other apps' pages: Mindful, StayFree, One
  Sec, Opal Focus, AppBlock. Among those, Mindful is the closest match — free,
  open source, Android, screen-time quotas.
- The form then offers a one-time $15 priority review, which moves it to the
  front of a queue of "several thousand apps"; it was declined, so the listing
  sits in the normal queue and may not be looked at for a long time. Revisit only
  if the free queue is still silent in a few months.
- Not done: the "Company / Author" field, which the form marks required but
  accepts empty — revisit if a reviewer asks.

### 3. Slant

- "What is the best screen time app for Android?" is the question to answer under.
- Write the answer as a short comparison, not a listing: what it does, who it is
  wrong for.

### 4. LibHunt

- GitHub OAuth, then "Submit a project".
- It indexes the GitHub repo and shows stars, so it also feeds a second page of
  SEO. Pastes the 158-char line and the topics.

### 5. OpenAlternative

- Login required. Free listings are reviewed and go live one per weekday, so file
  it and wait rather than re-submitting.
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

**Blocked by HN on 2026-11-04.** `u/rishabhjtripath` (1 karma, created that day) filled
the submit form with the title below and the repo URL; the button returned
`/showlim`, which reads:

> We're temporarily restricting Show HNs because of a massive influx, mostly by
> users who aren't yet familiar with the site or its culture.

`/submitted?id=rishabhjtripath` stayed empty, so nothing was posted and nothing was
lost. There is no workaround worth having: submitting the same link under a title
that does not start with `Show HN` is the exact dodge the restriction exists to
stop, and a 1-karma account's self-link tends to die in /new anyway.

The way in is the one HN names — read, comment, be useful for a few weeks, then
post. Practically: comment on threads about Android battery, accessibility
services, or digital wellbeing without mentioning Sober; the account needs history
before it can carry a self-post.

Once the account can post:

- Title: `Show HN: Sober – Android screen-time blocker with no internet permission`
  (72 chars, tested against the field)
- URL: `https://github.com/rishabhx29/Sober`
- Text field: empty. Post the first comment immediately after.
- First comment:
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

**The r/SideProject appeal was sent on 2026-11-04** through the new composer at
https://www.reddit.com/message/compose?to=/r/SideProject, with the text below.

The reCAPTCHA is not actually the blocker it looked like: `old.reddit.com` redirects
modmail to the new composer, the composer carries a hidden
`textarea[name="g-recaptcha-response"]` that fills itself with an enterprise token,
and a click on Send goes through. What misleads is the feedback — the form clears
and no toast appears, so it reads as a failed submit. The proof is in the network
log: the POST to `/svc/shreddit/graphql` returns

```json
{"data":{"sendMessageToSubreddit":{"errors":null,"ok":true}},"errors":[]}
```

`/message/sent.json` stays empty after a successful modmail, because modmail is a
separate system from private messages, so do not use it to check this. Until the
toast-vs-network question is settled for another site, confirm writes by reading
the GraphQL response rather than the UI.

```text
Subject: Removed by the spam filter, not by a mod — request to restore

Hi mods,

I posted "I built a screen-time app that has no INTERNET permission" and it was
removed within about five minutes with no mod message, so I believe it was the
automated spam filter: the account is days old and had no karma, and the body
carried a GitHub link.

I am the developer of the app (Sober, an MIT-licensed Android screen-time
blocker). If the removal was a rule I broke, I would rather know which one than
repost blind. If it was the filter, I would be grateful if the post could be
looked at: https://www.reddit.com/r/SideProject/comments/1xnfxe3/

Either way, thank you.
```

Two earlier clicks may also have delivered the same text before the network log
was in place; both cleared the form identically, and there is no sent copy to
count. If the mods see duplicates, that is why.

### 16. Uneed

Submitted 2026-11-04. Scraped from the repo, then filled in: name, slug `sober`,
repo URL, category Personal Life, pricing Free, tags Happiness / Smartphones /
Android, open source on, tagline "Screen-time limits that actually close the app",
rich description from the long blurb, logo.

Uneed's queue is the whole story, and it is the reason this is not a launch lever:

| Option | Cost | When |
|---|---|---|
| Join the free line | $0 | 2027-06-13 (~221 days) |
| Fast-track, Uneed picks the day | $14.99 | ~14 days after payment |
| Pick the exact day | $39.99 | as soon as this week |
| Auto-Submit to 100+ directories | $249 | n/a |

Free was chosen. On the free line the listing also needs an upvote score of 10 on
launch day to stay published, and 20 to keep a do-follow backlink — on a queue
seven months long neither number is reachable.

Practical read: the entry costs nothing and may eventually produce a backlink, but
it will not produce users. If a paid option is ever worth it, Fast-track at $14.99
is the only sane one, and it should be spent on a week when there is something else
running — not on its own.

Until the queue comes up, `/tool/sober` returns 404. The dashboard's "Share a
preview" panel produces a public page that needs no launch date —
https://www.uneed.best/preview/n0Q2UkKu_IKDtQpRkWGZNc6u — rendered on 2026-11-04 as
"a draft shared by its maker", not listed on Uneed. That is the only Uneed URL worth
sending to anyone before 2027.

### 17. Indie Hackers

The product page at https://www.indiehackers.com/product/sober-2 is public: name,
tagline ("Screen-time limits that actually close the app", 46/60), logo, website and
founder. On 2026-11-04 the **Motivation** field was filled. It is marked `*Required`
yet the form had saved without it, which is why the page rendered with no About
section:

> Every blocker I tried had a dismiss button, and the ones that worked wanted an
> account and a server. I wanted limits that actually close the app, with no
> network permission at all. Free and MIT.

`@RishabhTri8805` was added as the Twitter handle. That text is 194 of the 220
characters.

Publishing is the gate. The page carries a standing notice — "Publishing is open to
established members. Join in with a few thoughtful comments on posts you find
interesting and we'll open it up for you — or subscribe to IH+ to publish right away" —
and "Write a new post" does nothing on this account. A product page with zero posts
never appears in an IH feed, so the page is a backlink and a landing page today, not
a distribution channel. The only real route is a few genuine comments over the next
week, then the first post. Buying IH+ to unlock posting is not worth it for one
launch.

### 18. X

Two posts were live on 2026-11-04 from `@RishabhTri8805`:

- https://x.com/RishabhTri8805/status/2117845866484752498 — the no-permission post:
  31 views, 1 like, 1 bookmark at audit time
- https://x.com/RishabhTri8805/status/2117850680132342184 — the CTA reply: put the
  limit on the app you open without deciding to; a star on the repo is the whole
  marketing budget: 2 views, 1 like

Engagement is small because the account has no audience, not because the posts are
hidden. Nothing to fix there; X needs followers before it produces installs.

### 19. Public-state audit, 2026-11-04

What an anonymous visitor can actually see, site by site:

| Site | Public URL | State |
|---|---|---|
| Product Hunt | `/products/sober-3` | **live** — logo, tagline, summary, 3 gallery images, maker comment, "Launched in 2026"; the launch event fires 2026-11-05 12:01am PT |
| X | two posts | **live**, low reach |
| Indie Hackers | `/product/sober-2` | **live** page with About filled; zero posts, posting gated |
| GitHub | repo + 2 PRs | **live**; jcanfield#14 and fiedri#22 both OPEN; 1 star; description and 12 topics set |
| Uneed | `/tool/sober` | **404** — queued for 2027-06-13; the preview link renders and is marked "draft shared by its maker" |
| Reddit | `/r/SideProject/comments/1xnfxe3` | **hidden** — author view reads "Sorry, this post was removed by Reddit's filters"; no mod reply in the inbox |
| Hacker News | `/submitted?id=rishabhjtripath` | **empty** — zero submissions, 1 karma |
| AlternativeTo | https://alternativeto.net/software/sober-screen-time/ | **submitted 2026-11-04** — "waiting to be reviewed", private to the submitter until then; five alternatives attached; $15 priority review declined |

That table is the answer to "why can't I see the launches": only one of the eight has a
scheduled public moment, and it is tomorrow. The rest are queued (Uneed 2027),
filtered (Reddit), refused (HN), gated (IH), unconfirmed (AlternativeTo), or live but
audienceless (X).

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
