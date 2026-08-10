# Odyssey on the Glyph Matrix

Plays a video clip back on the Nothing Phone (3)'s rear Glyph Matrix
(25x25 monochrome LED grid) as a custom Glyph Toy.

Two parts:

1. `video_to_glyph.py` — converts your MP4 into `frames.bin`, a compact
   binary sequence of 25x25 brightness frames.
2. `android/` — an Android Studio project with a Glyph Toy service that
   plays `frames.bin` back on the matrix, looping.

## Current Setup Status

- [x] **Clip Processed**: `clip.mp4` has been converted into matrix frames (`663` frames @ `12` FPS, `55.2`s duration).
- [x] **Preview Generated**: `build/preview.mp4` (500x500 upscaled preview video). Watch this to see how it looks!
- [x] **Assets Synced**: `frames.bin` has been copied into `android/app/src/main/assets/frames.bin`.
- [x] **Glyph Matrix SDK Installed**: `glyph-matrix-sdk-2.0.aar` downloaded into `android/app/libs/`.
- [x] **Toy Preview & Icons Generated**: Preview graphic created at `android/app/src/main/res/drawable/img_toy_preview.png` and launcher icons generated.

## 1. Customizing or Re-converting the Clip (Optional)

If you want to tweak contrast, duration, or start timestamp for the meme reel:

```bash
python video_to_glyph.py clip.mp4 --fps 12 --gamma 0.6
```
*(Passing arguments like `--start 10 --duration 15` will convert a specific segment. Running the script automatically syncs `frames.bin` to the Android assets).*

## 2. Build and Install on Nothing Phone (3)

1. Open the `android/` directory in **Android Studio**.
2. Let Gradle sync automatically (all dependencies and SDK `.aar` are already in place).
3. Connect your **Nothing Phone (3)** via USB (make sure Developer Options & USB Debugging are enabled).
4. Click **Run** in Android Studio to install `Odyssey Glyph`.

## 3. Activate the Toy on the Phone

1. Open the **Odyssey Glyph** app on your phone.
2. Tap **"Open Glyph Toys Manager"** (or go to Settings -> Glyph Interface -> Glyph Toys).
3. Add **"Odyssey"** to your active Glyph Toy carousel.
4. Flip your phone over and short-press the **Glyph Button** on the back to cycle to Odyssey!
5. Long-press the Glyph Button to pause/resume playback.

## Notes

- The matrix is monochrome, so color is discarded — clips with strong
  contrast and simple silhouettes (a face close-up, a bright object
  against dark, etc.) read far better than busy wide shots.
- `--fps` on the matrix is independent of the source clip's frame rate;
  the script re-samples to whatever you pass.
- If playback looks choppy on-device, that's usually a `frames.bin` +
  loop-timing issue, not the phone struggling — the whole sequence is
  pre-baked, so playback should be smooth at whatever fps you chose.
