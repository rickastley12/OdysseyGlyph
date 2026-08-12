```
GLYPH ODYSSEY
video → 25×25 LED matrix  ·  on-device  ·  MIT
```

This started because I wanted to watch the Odyssey trailer on the back of my Nothing Phone as a bit. I thought it would take a few hours. It took a weekend, three rewrites, and way more thinking about pixel averaging than any reasonable person should do for a joke.

Somewhere in that process, the pipeline stopped caring what video I gave it. So I shipped it.

---

> **If you're on a Nothing Phone 1, 2, 2a, or (4a) Pro — close this tab.**
>
> Those have zone-based LED strips or lower-density matrices (like the 137-LED array on the 4a Pro). They can light up in regions or rough shapes, not individual pixels. The 25×25 addressable matrix strictly requires the flagship **Nothing Phone (3)**. This app does nothing useful on other models.

---

## What it does

**Video → Glyph Matrix**  
Pick any video from your gallery. Pinch and pan to crop a region into the 25×25 overlay with live video playback behind the frame. Trim start and end. Hit process. The app extracts frames, runs the pipeline, and plays the result on your matrix. Entirely on-device, works offline, no account needed.

**Lyric Studio**  
Load an LRC lyric file, pick a font mode, and preview frame-by-frame. Sync to audio and export as a Glyph toy. The font engine renders directly to the 25px matrix — getting text to actually read at that resolution across three different typography sizes took more iteration than I'd like to admit.

**Live Lyrics**  
A background service that detects whatever's currently playing on your phone — any music app, not just Spotify — via Android's MediaSession. It fetches time-synced lyrics from LRCLib and scrolls them across the matrix in real time. No API key required, and it works in basically any language.

**Toy Manager**  
Browse, preview, and delete your saved animations. Nothing exciting, just necessary.

---

## How this is different from what already exists

If you are looking for a general-purpose Glyph animation tool, you should probably be using **Glyph Museum** (by pauwma) on the Play Store. It has 15,000+ active users, gallery/GIF import, live matrix preview, audio-reactive light shows, and a massive library. It is the gold standard for the community. Pauwma also maintains **GlyphMatrixEditor**, an excellent web app for deliberate, frame-by-frame animation work with onion skinning.

Glyph Odyssey is built to solve a much narrower set of problems:

**1. The video processing pipeline**  
Glyph Museum imports GIFs and images, but Odyssey is built specifically to crush real-world video footage (like faces and cinematic shots) into 25×25 pixels and have it remain legible. It does this via a custom pipeline: box-filter downsampling, S-curve contrast stretching, and unsharp masking, with live video playback running behind your gesture-crop frame.

**2. Live lyrics**  
Other apps do audio-reactive light shows (EQ bars bouncing to the beat). Odyssey does *Live Lyrics*. It reads MediaSession metadata from whatever music app you're using (Spotify, YouTube Music), fetches time-synced lyrics from LRCLib, and scrolls the actual text across your hardware matrix in real-time.

**3. Lyric studio**  
An LRC file-based authoring tool. It uses a custom Ndot57 font engine with dynamic per-word hyphenation and chunking to render legible text on a 25-pixel-wide constraint, so you can export hardcoded lyric animations as Glyph toys.

---

## The parts that were actually interesting to figure out

**Why faces are still recognizable at 25 pixels wide**  
The naive way to shrink a video frame to 25×25 is to take every Nth pixel and throw the rest away. You lose most of the information in the source frame and it looks terrible. This app averages every pixel in each output cell's corresponding source region instead — box filter. You can actually tell who's in the video at 25 pixels wide. That difference is the whole point.

**The contrast problem**  
A flat grayscale conversion on monochrome LEDs looks like grey murk. The fix: stretch each frame's actual tonal range to 0–255 first, then apply `t*t*(3-2*t)` as an S-curve — the smoothstep function from graphics shaders. Unsharp masking runs after. Subjects actually read on the hardware instead of washing out.

**Lyrics in any language**  
Android ships a built-in ICU Transliterator — `Any-Latin; Latin-ASCII` — that phonetically converts any writing system to Latin characters, on-device, no network call. So when you're playing a J-pop song, the matrix doesn't try to render kanji at 25 pixels wide. It renders what the song sounds like. `夜に駆ける` becomes `yoru ni kakeru`. It falls back silently if it fails.

**Timing words that have no timestamps**  
LRC files mark the start of each lyric line. They say nothing about individual words. To do word-by-word flash animation, the app has to estimate when each word appears. It weights each word by character count — longer words get more time — with extra hold after punctuation since those have natural spoken pauses. It's a heuristic. It works surprisingly well in practice.

**Why live lyrics drift if you don't do something about it**  
Spotify and YouTube Music fire a PlaybackState callback when something changes — play, pause, skip. They don't broadcast continuous position. So if you extrapolate position from the last known state, you slowly fall behind mid-song. Fix: poll `MediaController` every 500ms for a fresh position read. Boring solution, lyrics stay locked.

---

## Anticipated questions

**It's a debug APK**  
Yes. Signing infrastructure and a release pipeline for a personal project that uses hardware APIs that'd fail Play Store review anyway felt like the wrong use of a Saturday. The debug flag doesn't affect functionality. If that bothers you, build from source.

**Notification access is a scary permission**  
Fair. Live Lyrics needs it to read track metadata across all apps — title, artist, playback position. That's the full scope of what's read. Nothing is logged, stored, or sent anywhere except the LRCLib lyrics fetch which only gets song title and artist. If you don't trust that, skip Live Lyrics — the rest works fine without it.

**Does it drain battery**  
Video processing is CPU-heavy but finishes in seconds. The Live Lyrics service runs a MediaSession poll and a ~12fps canvas render loop. Roughly equivalent to a music visualizer.

**Will this get maintained**  
I use it myself so when something bothers me enough I'll fix it. No roadmap, no promises.

---

## Install

1. [Releases](https://github.com/rickastley12/OdysseyGlyph/releases) → download `app-debug.apk` to your phone
2. Tap the file — Android will ask you to allow installs from unknown sources, enable it
3. Open the app, grant media permissions
4. For Live Lyrics: enable Notification Access when the app prompts you

---

## Usage

1. Tap **Select Video** — pick any clip from your gallery
2. Pinch and pan to frame your subject in the 25×25 overlay
3. Trim sliders for start and end
4. **Process Video** — extracts frames, runs the pipeline, writes a `.bin`
5. Toggle the **Glyph Switch** — plays on the back of your phone

---

## Build from source

```bash
git clone https://github.com/rickastley12/OdysseyGlyph.git
```

Open in Android Studio → sync Gradle → run to a connected Nothing Phone. The `glyph-matrix-sdk-2.0.aar` is already in `android/app/libs/`.

---

MIT. Not affiliated with Nothing Technology Limited.
