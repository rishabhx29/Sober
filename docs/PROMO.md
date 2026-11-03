# Sober promo film — production brief

Status: **toolchain ready, awaiting go-ahead.** No film has been produced yet.
The project is scaffolded at `promo/` and the end card below is the only frame rendered so far.

## Toolchain (installed and verified)

| Piece | What it gives us | Verified |
|---|---|---|
| HyperFrames CLI 0.9.9 | HTML/CSS/GSAP composition → MP4, Chrome capture + ffmpeg | `doctor` green; 10s portrait smoke render passed |
| `hyperframes` + 8 domain skills | Composition contract, animation, keyframes, audio, registry, studio, captions | Installed in `~/.agents/skills` |
| `motion-doctrine`, `cut-the-curve`, `seam-craft`, `captions-overlay`, `oversized-cursor`, `changelog-video` | House motion rules: seam law, zoom-through cuts, caption overlays, cursor craft | Installed; loaded by the agent |
| `media-use` + Kokoro-82M TTS | Local voiceover, 12 voices, 9 languages | 4.3s WAV test passed |
| 21 bundled SFX (Pixabay licence) | whooshes, pops, clicks, riser, impact | Available in `media-use/audio` |
| `ffmpeg` | Mixing, loudness, conform | On PATH |
| Manuscript tokens + Inter/JetBrains Mono | Exact app colours and type | Copied into `promo/brand.css` and `promo/assets/fonts` |
| App screens + icon | 7 dark-mode screens, 1024px icon | Copied into `promo/assets` |

There is no official HyperFrames MCP server. HyperFrames integrates through its CLI and skills, and that is what is configured here.

## Deliverables

| Cut | Size | Use |
|---|---|---|
| Vertical (primary) | 1080×1920, 24s | Shorts, Reels, TikTok |
| Landscape | 1920×1080, 24s | YouTube, X, GitHub README |
| Square (optional) | 1080×1080 | Feed posts |

Silent-autoplay safe: every claim is burned in as type or a caption. Voiceover is optional and would only be added to the landscape cut.

## Recommended concept — "The app that lets you leave"

Quiet and confident, not alarmist. One continuous camera move through real screens, no idle wobble.
The differentiator lands last: **a screen-time enforcer that declares no internet permission.**

| Time | Beat | On screen | Sound |
|---|---|---|---|
| 0.0–2.5 | Hook | Full-bleed glowing phone in the dark. Type: "You have 96 apps." → "None of them help you close one." | Low room tone, one soft impact |
| 2.5–6.5 | Turn | Phone settles into a device frame; Sober home screen. "This one does." Session starts, ring begins counting. | Whoosh, one tactile click |
| 6.5–10.5 | Set the line | Apps tab. Instagram limit set to 30m; Shorts rule toggles on. "Set the line once." | Click, pop |
| 10.5–14.5 | It holds | Instagram opens, dims, Sober block screen arrives. "And the line holds." | Muted thud, then quiet |
| 14.5–17.5 | Payoff | Stats screen, weekly bars stepping down, streak. "Less screen. More life." | Riser into a clean chime |
| 17.5–20.5 | Proof | Airplane mode toggles on; Sober still works. "No account. No cloud. No internet permission." | Airplane-mode click, silence |
| 20.5–24.0 | End card | Candle icon, wordmark, "Less screen. More life.", free/open-source line, repo URL. | Final chime, tail |

Alternate angles if this one feels too soft:

- **"Willpower is not a feature."** — opens on a failed app timer, then Sober's hard stop. Punchier, more confrontational.
- **"The screen-time app that can't watch you."** — leads with the no-internet proof. Strongest for privacy audiences, weakest for mass social.

## Reference libraries

Study these before the build. Steal structure, not visuals.

| Source | What to take |
|---|---|
| [Apple App Previews](https://developer.apple.com/app-store/app-previews/) | The 15–30s rule, first-frame-as-thumbnail discipline, capture-from-the-app honesty |
| [Matte — app preview examples](https://matte.app/examples/app-store-preview-videos/) | Teardowns of what actually holds attention in the first 2s |
| [Incipia — 10 preview examples](https://incipia.co/post/app-marketing/10-examples-of-app-store-preview-videos/) | Feature-to-benefit phrasing |
| [AppLaunchFlow — 15 examples + script](https://www.applaunchflow.com/blog/mobile-app-promo-video-examples-guide) | Turning 5 screenshots into store, social and teaser cuts |
| [Dribbble — app preview video](https://dribbble.com/search/app-preview-video) | Device staging, type placement, colour blocking |
| [60fps.design](https://60fps.design) | Interaction detail worth recreating (tap, sheet, toggle) |
| [Screensdesign](https://screensdesign.com) / [Appllama](https://appllama.io) | Real screen flows and store-listing conventions |
| [Reelfolio](https://reelfolio.io) | The screens-to-showreel grammar we are reproducing by hand |
| Motionographer / Behance "app promo" | Camera moves, sound design, end-card timing |
| Product Hunt launch videos | How indie apps present proof and a call to action in under 30s |

## Assets and gaps

- Screens in `promo/assets/screens` are 540×1200 — fine for look-dev, **soft at 1080 wide**. Recapture at 1080×2400 from the emulator/device before the final render, plus three short screen recordings: starting a session, setting a limit, and the block screen appearing.
- No device is currently attached (`adb devices` empty), so capture is the first production step.
- Music: no credentialed HeyGen catalogue access. Options are a CC0/CC-BY instrumental track, or local generation. Default tempo 90–110 BPM, ducked under SFX.
- App icon and `docs/assets/banner.png` are ready.

## Decisions locked by default (change any of them)

- Aspect: vertical first, landscape second.
- Voice: none on the vertical cut; kinetic type only. Voiceover only if we make a landscape/YouTube cut.
- Tone: restrained. No red alarms, no shaming, no stock footage of hands.
- Length: 24s, inside the requested 20–25s.
- End card: the frame already rendered in `promo/` — logo, wordmark, tagline, proof line, repo.

## When approved

1. Capture 1080p screens and three interaction recordings.
2. Build the seven scenes in `promo/` as sub-compositions over one camera move.
3. Add SFX, then optional TTS, then mix to −14 LUFS for social.
4. `npm run check`, inspect snapshots at every beat boundary, then render vertical and landscape.
5. Export cover frame + end-card still for the README and launch posts.
