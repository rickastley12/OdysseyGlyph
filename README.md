```
GLYPH ODYSSEY
video → 25×25 LED matrix  ·  on-device  ·  MIT
```

this started because i wanted to watch the Odyssey trailer on the back of my Nothing Phone as a bit. i thought it would take a few hours. it took a weekend, three rewrites, and way more thinking about pixel averaging than any reasonable person should do for a joke.

somewhere in that process the pipeline stopped caring what video i gave it. so i shipped it.

---

> **if you're on a Nothing Phone 1, 2, or 2a — close this tab.**
>
> those have zone-based LED strips. they can light up in regions, not individual pixels. the 25×25 addressable matrix only exists on the **Nothing Phone (3)** and **Nothing Phone (4a) Pro**. this app does nothing useful on older models.

---

## how this is different from what already exists

if you are looking for a general-purpose Glyph animation tool, you should probably be using **Glyph Museum** (by pauwma) on the Play Store. it has 15,000+ active users, gallery/GIF import, live matrix preview, audio-reactive light shows, and a massive library. it is the gold standard for the community. pauwma also maintains **GlyphMatrixEditor**, an excellent web app for deliberate, frame-by-frame animation work with onion skinning.

Glyph Odyssey is built to solve a much narrower set of problems:

**1. the video processing pipeline**  
Glyph Museum imports GIFs and images, but Odyssey is built specifically to crush real-world video footage (like faces and cinematic shots) into 25×25 pixels and have it remain legible. it does this via a custom pipeline: box-filter downsampling, S-curve contrast stretching, and unsharp masking, with live video playback running behind your gesture-crop frame.

**2. live lyrics**  
other apps do audio-reactive light shows (EQ bars bouncing to the beat). Odyssey does *Live Lyrics*. it reads MediaSession metadata from whatever music app you're using (Spotify, YouTube Music), fetches time-synced lyrics from LRCLib, and scrolls the actual text across your hardware matrix in real-time.

**3. lyric studio**  
an LRC file-based authoring tool. it uses a custom Ndot57 font engine with dynamic per-word hyphenation and chunking to render legible text on a 25-pixel-wide constraint, so you can export hardcoded lyric animations as Glyph toys.

---

## what it does

**video → glyph matrix**  
pick any video from your gallery. pinch and pan to crop a region into the 25×25 overlay with live video playback behind the frame. trim start and end. hit process. the app extracts frames, runs the pipeline, plays the result on your matrix. entirely on-device, works offline, no account needed.

**lyric studio**  
load an LRC lyric file, pick a font mode, preview frame-by-frame. sync to audio, export as a Glyph toy. the font engine renders directly to the 25px matrix — getting text to actually read at that resolution across three different typography sizes took more iteration than i'd like to admit.

**live lyrics**  
a background service that detects whatever's currently playing on your phone — any music app, not just Spotify — via Android's MediaSession. fetches time-synced lyrics from LRCLib and scrolls them across the matrix in real time. no API key. works in basically any language.

**toy manager**  
browse, preview, delete your saved animations. nothing exciting, just necessary.

---

## the parts that were actually interesting to figure out

**why faces are still recognizable at 25 pixels wide**  
the naive way to shrink a video frame to 25×25 is to take every Nth pixel and throw the rest away. you lose most of the information in the source frame and it looks terrible. this app averages every pixel in each output cell's corresponding source region instead — box filter. you can actually tell who's in the video at 25 pixels wide. that difference is the whole point.

**the contrast problem**  
a flat grayscale conversion on monochrome LEDs looks like grey murk. the fix: stretch each frame's actual tonal range to 0–255 first, then apply `t*t*(3-2*t)` as an S-curve — the smoothstep function from graphics shaders. unsharp masking runs after. subjects actually read on the hardware instead of washing out.

**lyrics in any language**  
Android ships a built-in ICU Transliterator — `Any-Latin; Latin-ASCII` — that phonetically converts any writing system to Latin characters, on-device, no network call. so when you're playing a J-pop song, the matrix doesn't try to render kanji at 25 pixels wide. it renders what the song sounds like. `夜に駆ける` becomes `yoru ni kakeru`. falls back silently if it fails.

**timing words that have no timestamps**  
LRC files mark the start of each lyric line. they say nothing about individual words. to do word-by-word flash animation, the app has to estimate when each word appears. it weights each word by character count — longer words get more time — with extra hold after punctuation since those have natural spoken pauses. it's a heuristic. it works surprisingly well in practice.

**why live lyrics drift if you don't do something about it**  
Spotify and YouTube Music fire a PlaybackState callback when something changes — play, pause, skip. they don't broadcast continuous position. so if you extrapolate position from the last known state, you slowly fall behind mid-song. fix: poll `MediaController` every 500ms for a fresh position read. boring solution, lyrics stay locked.

---

## anticipated questions

**it's a debug APK**  
yes. signing infrastructure and a release pipeline for a personal project that uses hardware APIs that'd fail Play Store review anyway felt like the wrong use of a Saturday. the debug flag doesn't affect functionality. if that bothers you, build from source.

**notification access is a scary permission**  
fair. Live Lyrics needs it to read track metadata across all apps — title, artist, playback position. that's the full scope of what's read. nothing is logged, stored, or sent anywhere except the LRCLib lyrics fetch which only gets song title and artist. if you don't trust that, skip Live Lyrics — the rest works fine without it.

**does it drain battery**  
video processing is CPU-heavy but finishes in seconds. the Live Lyrics service runs a MediaSession poll and a ~12fps canvas render loop. roughly equivalent to a music visualizer.

**will this get maintained**  
i use it myself so when something bothers me enough i'll fix it. no roadmap, no promises.

---

## install

1. [Releases](https://github.com/rickastley12/OdysseyGlyph/releases) → download `app-debug.apk` to your phone
2. tap the file — Android will ask you to allow installs from unknown sources, enable it
3. open the app, grant media permissions
4. for Live Lyrics: enable Notification Access when the app prompts you

---

## usage

1. tap **Select Video** — pick any clip from your gallery
2. pinch and pan to frame your subject in the 25×25 overlay
3. trim sliders for start and end
4. **Process Video** — extracts frames, runs the pipeline, writes a `.bin`
5. toggle the **Glyph Switch** — plays on the back of your phone

---

## build from source

```bash
git clone https://github.com/rickastley12/OdysseyGlyph.git
```

open in Android Studio → sync Gradle → run to a connected Nothing Phone. the `glyph-matrix-sdk-2.0.aar` is already in `android/app/libs/`.

---

MIT. not affiliated with Nothing Technology Limited.
