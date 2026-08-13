```
GLYPH ODYSSEY
video → 25×25 LED matrix  ·  on-device  ·  MIT
```

**[🌐 Visit the Live Website](https://odyssey-glyph.vercel.app/)**

This started because I wanted to watch the Odyssey trailer on the back of my Nothing Phone as a bit. I thought it would take a few hours. It took a weekend, three rewrites, and way more thinking about pixel averaging than any reasonable person should do for a joke.

Somewhere in that process, the pipeline stopped caring what video I gave it. So I shipped it.

---

> **If you're on a Nothing Phone 1, 2, 2a, or (4a) Pro — close this tab.**
>
> Those have zone-based LED strips or lower-density matrices. The 25×25 addressable matrix is exclusive to the **Nothing Phone (3)**. This app does nothing useful on other models.

---

## ⚠️ Install currently requires ADB — here's why

Google Play Protect is hard-blocking the APK at install time. Not because it's malicious — **0 of 66 security vendors on VirusTotal flag it** — but because of an automatic policy: any sideloaded app that declares the `NOTIFICATION_LISTENER` permission gets blocked, no exceptions, regardless of what it actually does with it. Spyware and banking trojans abuse that permission heavily, so Google treats it as an automatic hard block for anything not on the Play Store. Our app needs it for Live Lyrics (reading current track metadata, nothing else), and that's enough to trigger it.

A Play Protect appeal has been submitted with the VirusTotal report. If it gets approved, normal sideload install will work again. Until then, **ADB is the only way in**.

**Install via ADB (one command):**

Plug your phone into your computer with USB debugging enabled, then:

```bash
adb install app-release.apk
```

That's it. ADB bypasses Play Protect entirely. If you haven't used ADB before:
1. Enable **Developer Options** on your phone: Settings → About Phone → tap Build Number 7 times
2. Enable **USB Debugging** inside Developer Options
3. Install [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) on your computer
4. Download `app-release.apk` from the [latest release](https://github.com/rickastley12/OdysseyGlyph/releases)
5. Run `adb install app-release.apk`

---

## What it does

**Video → Glyph Matrix**
Pick any video from your gallery. Pinch and pan to crop a region into the 25×25 overlay with live video playback behind the frame. Trim start and end. Hit process. The app extracts frames, runs the pipeline, and plays the result on your matrix. Entirely on-device, works offline, no account needed.

**Lyric Studio**
Load an LRC lyric file, pick a font mode, and preview frame-by-frame. Sync to audio and export as a Glyph toy. The font engine renders directly to the 25px matrix — getting text to actually read at that resolution across three different typography sizes took more iteration than I'd like to admit.

**Live Lyrics**
A background service that detects whatever's currently playing on your phone — any music app, not just Spotify — via Android's MediaSession. It fetches time-synced lyrics from LRCLib and scrolls them across the matrix in real time. No API key required, works in basically any language.

**Toy Manager**
Browse, preview, and delete your saved animations. Nothing exciting, just necessary.

---

## How this is different from what already exists

If you're looking for a general-purpose Glyph animation tool, you should probably be using **Glyph Museum** (by pauwma) on the Play Store. It has 15,000+ active users, gallery/GIF import, live matrix preview, audio-reactive light shows, and a massive community library. Pauwma also maintains **GlyphMatrixEditor**, an excellent web-based frame-by-frame animation editor with onion skinning.

Glyph Odyssey is built to solve a narrower set of problems:

**1. The video processing pipeline**
Glyph Museum imports GIFs and images. Odyssey is built specifically to crush real-world video footage — faces, cinematic shots, motion — into 25×25 pixels and have it remain legible. Box-filter downsampling, S-curve contrast stretching, unsharp masking, with live video playback running behind your gesture-crop frame.

**2. Live lyrics**
Other apps do audio-reactive light shows (EQ bars bouncing to the beat). Odyssey does *Live Lyrics* — it reads MediaSession metadata from whatever music app you're using, fetches time-synced lyrics from LRCLib, and scrolls the actual text across the matrix in real time.

**3. Lyric studio**
An LRC file-based authoring tool with a custom Ndot57 font engine. Dynamic per-word hyphenation and chunking to render legible text on a 25-pixel-wide constraint.

---

## The parts that were actually interesting to figure out

**Why faces are still recognizable at 25 pixels wide**
The naive way to shrink a video frame to 25×25 is to take every Nth pixel and throw the rest away. You lose most of the source frame and it looks terrible. This app averages every pixel in each output cell's corresponding source region instead — box filter. You can actually tell who's in the video at 25 pixels wide. That difference is the whole point.

**The contrast problem**
A flat grayscale conversion on monochrome LEDs looks like grey murk. The fix: stretch each frame's actual tonal range to 0–255 first, then apply `t*t*(3-2*t)` as an S-curve — the smoothstep function from graphics shaders. Unsharp masking runs after. Subjects actually read on the hardware instead of washing out.

**Lyrics in any language**
Android ships a built-in ICU Transliterator — `Any-Latin; Latin-ASCII` — that phonetically converts any writing system to Latin characters, on-device, no network call. So when you're playing a J-pop song, the matrix doesn't try to render kanji at 25 pixels wide. It renders what the song sounds like. `夜に駆ける` becomes `yoru ni kakeru`. Falls back silently if it fails.

**Timing words that have no timestamps**
LRC files mark the start of each lyric line. They say nothing about individual words. The app estimates per-word timing by weighting each word by character count — longer words get more time — with extra hold after punctuation since those have natural spoken pauses. It's a heuristic. Works surprisingly well in practice.

**Why live lyrics drift if you don't do something about it**
Spotify and YouTube Music fire a PlaybackState callback when something changes — play, pause, skip. They don't broadcast continuous position. So if you extrapolate position from the last known state, you slowly fall behind mid-song. Fix: poll `MediaController` every 500ms for a fresh position read. Boring solution, lyrics stay locked.

---

## Security & Trust

**Why Play Protect blocks it**
`NOTIFICATION_LISTENER` + sideloaded = automatic block, by Google's own policy, regardless of what the app actually does. Our VirusTotal scan (0/66 vendors) confirms it's clean. A Play Protect appeal is in progress — if approved, normal sideload will work again.

**How do I know the APK is safe?**
Every release is built transparently by [GitHub Actions](https://github.com/rickastley12/OdysseyGlyph/actions) from the open-source code in this repo — not on a personal machine. Build logs are public. Every release includes a SHA-256 hash and a direct VirusTotal link in the release notes.

**Notification access is a scary permission**
Fair. Live Lyrics needs it to read track metadata — title, artist, playback position. That's it. Nothing is logged, stored, or transmitted except the LRCLib lyrics fetch which only sends song title and artist. If you'd rather not grant it, skip Live Lyrics — everything else works without it.

---

## FAQ

**Why not just put it on the Play Store?**
The appeal is more effort than this project warrants right now. It's a hobby tool for a niche hardware feature on one phone model. If the Play Protect appeal goes through, sideload installs become painless and that's good enough.

**Does it drain battery?**
Video processing is CPU-heavy but finishes in seconds. The Live Lyrics service runs a MediaSession poll and a ~12fps render loop. Roughly equivalent to a music visualizer widget.

**Will this get maintained?**
I use it myself so when something bothers me enough I'll fix it. No roadmap, no promises.

---

## Build from source

```bash
git clone https://github.com/rickastley12/OdysseyGlyph.git
```

Open in Android Studio → sync Gradle → run to a connected Nothing Phone. The `glyph-matrix-sdk-2.0.aar` is already in `android/app/libs/`.

---

MIT. Not affiliated with Nothing Technology Limited.
