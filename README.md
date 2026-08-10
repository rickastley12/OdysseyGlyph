# Odyssey Glyph

**The rapid, on-device alternative to GlyphMatrixEditor for instantly turning gallery videos into Glyph toys.**

Odyssey Glyph is a native Android application for the Nothing Phone (3) and (4a) Pro that lets you grab any video from your gallery, dynamically crop and trim it using intuitive pinch/pan gestures, and instantly render it to the back of your phone as a 25x25 Glyph animation. No computer, no web browsers, no JSON files. Just spontaneity. 

## Features
- **Native On-Device Processing:** Works entirely offline on your phone.
- **Gesture Cropping:** Pinch, zoom, and pan directly over the video to frame the exact portion you want on the matrix.
- **True Box-Filter Downsampling:** Mathematically averages high-res pixels instead of dropping them, retaining crisp facial details on the 25x25 matrix.
- **Adaptive S-Curve Contrast:** Per-frame local contrast stretching combined with Unsharp Masking ensures faces and shapes punch through the background.

## Installation
Since Odyssey Glyph relies on specific native Android hardware capabilities and isn't available on the Play Store, you can download it directly from GitHub:
1. Go to the [Releases](https://github.com/rickastley12/OdysseyGlyph/releases) page.
2. Download the latest `app-debug.apk`.
3. Tap the downloaded file to install (you may need to allow "Install from Unknown Sources" in your Android settings).

## Usage
1. Open the app and grant the necessary media permissions.
2. Tap **Select Video** and pick a clip from your gallery.
3. **Pinch and Pan** the video to perfectly frame your subject inside the 25x25 grid overlay.
4. Use the **Trim Sliders** to select the exact start and end time of your animation.
5. Tap **Process Video**. The app will extract the frames, apply the localized contrast filters, and compile them into a `frames.bin` sequence.
6. Toggle the **Glyph Switch** to see your creation play instantly on the back of your device!

## Building from Source
If you want to build the app yourself using Android Studio:
1. Clone the repository: `git clone https://github.com/rickastley12/OdysseyGlyph.git`
2. Open the project in Android Studio.
3. Sync Gradle and click **Run** to deploy it to your connected Nothing Phone.

## Device Compatibility
Odyssey Glyph requires the full 25x25 addressable matrix found on the **Nothing Phone (3)** and **Nothing Phone (4a) Pro**. Older models (Phone 1, 2, 2a) only feature zone-based LED strips and are not supported.

## License
MIT License. See `LICENSE` for details.
