#!/usr/bin/env python3
"""
video_to_glyph.py

Converts a video clip into a sequence of 25x25 monochrome frames suitable
for playback on the Nothing Phone (3) Glyph Matrix (489 addressable LEDs,
arranged in a 25x25 grid where only some cells are lit LEDs — the SDK
handles masking, you just supply a full 25x25 brightness array).

Output:
  1. <out>/frames.bin   - raw binary asset for the Android app.
                          Format: [uint32 frame_count][uint32 fps]
                          followed by frame_count * 625 bytes
                          (one brightness byte 0-255 per cell, row-major).
  2. <out>/preview.mp4  - upscaled preview so you can see what it'll look
                          like *before* pushing it to the phone.

Usage:
  python3 video_to_glyph.py input.mp4 --out build --fps 12 --duration 6
"""

import argparse
import struct
import sys
from pathlib import Path

import cv2
import numpy as np

MATRIX_SIZE = 25


def floyd_steinberg_dither(img: np.ndarray, levels: int = 16) -> np.ndarray:
    """Light dithering so gradients don't band too hard on a 25x25 grid."""
    img = img.astype(np.float32)
    h, w = img.shape
    step = 255.0 / (levels - 1)
    for y in range(h):
        for x in range(w):
            old = img[y, x]
            new = round(old / step) * step
            img[y, x] = new
            err = old - new
            if x + 1 < w:
                img[y, x + 1] += err * 7 / 16
            if y + 1 < h:
                if x - 1 >= 0:
                    img[y + 1, x - 1] += err * 3 / 16
                img[y + 1, x] += err * 5 / 16
                if x + 1 < w:
                    img[y + 1, x + 1] += err * 1 / 16
    return np.clip(img, 0, 255).astype(np.uint8)


def frame_to_matrix(frame_bgr: np.ndarray, bbox: tuple, gamma: float, dither: bool, invert: bool) -> np.ndarray:
    x, y, w_box, h_box = bbox
    frame_bgr = frame_bgr[y:y+h_box, x:x+w_box]
    
    h, w = frame_bgr.shape[:2]
    side = min(h, w)
    y0, x0 = (h - side) // 2, (w - side) // 2
    cropped = frame_bgr[y0:y0 + side, x0:x0 + side]

    gray = cv2.cvtColor(cropped, cv2.COLOR_BGR2GRAY)
    
    # Improve legibility by aggressively stretching contrast
    # (Values below 40 become pitch black, values above 200 become max brightness)
    gray = np.clip((gray.astype(np.float32) - 40) * (255.0 / (200 - 40)), 0, 255).astype(np.uint8)
    
    small = cv2.resize(gray, (MATRIX_SIZE, MATRIX_SIZE), interpolation=cv2.INTER_AREA)
    
    # Apply a circular mask so it explicitly acts like a circular cutout
    y, x = np.ogrid[:MATRIX_SIZE, :MATRIX_SIZE]
    center = (MATRIX_SIZE - 1) / 2.0
    mask = (x - center)**2 + (y - center)**2 <= (MATRIX_SIZE / 2.0)**2
    small = small * mask

    # gamma correction — LEDs read as "on" much more of the frame than
    # the eye expects unless midtones are pulled down a bit
    norm = small.astype(np.float32) / 255.0
    norm = np.power(norm, gamma)
    out = (norm * 255).astype(np.uint8)

    if invert:
        out = 255 - out

    if dither:
        out = floyd_steinberg_dither(out)

    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("input", help="path to source video clip")
    ap.add_argument("--out", default="build", help="output directory")
    ap.add_argument("--fps", type=float, default=12.0, help="playback fps on the matrix (8-15 recommended)")
    ap.add_argument("--duration", type=float, default=None, help="seconds to convert (default: whole clip)")
    ap.add_argument("--start", type=float, default=0.0, help="seconds into the clip to start")
    ap.add_argument("--gamma", type=float, default=0.6, help="<1 brightens midtones, >1 darkens")
    ap.add_argument("--invert", action="store_true", help="invert brightness (for light-background clips)")
    ap.add_argument("--no-dither", action="store_true", help="disable dithering")
    args = ap.parse_args()

    in_path = Path(args.input)
    if not in_path.exists():
        sys.exit(f"Input not found: {in_path}")

    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    cap = cv2.VideoCapture(str(in_path))
    if not cap.isOpened():
        sys.exit(f"Could not open video: {in_path}")

    src_fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    src_duration = total_frames / src_fps if src_fps else 0
    
    # Auto-detect letterbox cropping from the first frame
    ok, first_frame = cap.read()
    if ok:
        gray_first = cv2.cvtColor(first_frame, cv2.COLOR_BGR2GRAY)
        _, thresh = cv2.threshold(gray_first, 10, 255, cv2.THRESH_BINARY)
        coords = cv2.findNonZero(thresh)
        if coords is not None:
            bbox = cv2.boundingRect(coords)
        else:
            bbox = (0, 0, first_frame.shape[1], first_frame.shape[0])
    else:
        bbox = (0, 0, int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)), int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT)))

    start = max(0.0, args.start)
    duration = args.duration if args.duration is not None else max(0.0, src_duration - start)
    end = min(src_duration, start + duration)

    cap.set(cv2.CAP_PROP_POS_MSEC, start * 1000)

    frame_interval = 1.0 / args.fps
    matrices = []
    t = start
    while t < end:
        cap.set(cv2.CAP_PROP_POS_MSEC, t * 1000)
        ok, frame = cap.read()
        if not ok:
            break
        matrix = frame_to_matrix(
            frame,
            bbox=bbox,
            gamma=args.gamma,
            dither=not args.no_dither,
            invert=args.invert,
        )
        matrices.append(matrix)
        t += frame_interval

    cap.release()

    if not matrices:
        sys.exit("No frames extracted — check --start/--duration against the clip length.")

    # --- write binary asset for the Android app ---
    bin_path = out_dir / "frames.bin"
    with open(bin_path, "wb") as f:
        f.write(struct.pack("<II", len(matrices), int(round(args.fps))))
        for m in matrices:
            f.write(m.tobytes())  # 625 bytes, row-major, 0-255 per cell

    # --- write a human-viewable preview (upscaled, nearest-neighbor) ---
    preview_path = out_dir / "preview.mp4"
    scale = 20  # 25*20 = 500px preview
    writer = cv2.VideoWriter(
        str(preview_path),
        cv2.VideoWriter_fourcc(*"mp4v"),
        args.fps,
        (MATRIX_SIZE * scale, MATRIX_SIZE * scale),
    )
    for m in matrices:
        big = cv2.resize(m, (MATRIX_SIZE * scale, MATRIX_SIZE * scale), interpolation=cv2.INTER_NEAREST)
        big_bgr = cv2.cvtColor(big, cv2.COLOR_GRAY2BGR)
        writer.write(big_bgr)
    writer.release()

    print(f"Frames extracted : {len(matrices)}")
    print(f"Playback fps     : {args.fps}")
    print(f"Runtime          : {len(matrices) / args.fps:.1f}s")
    print(f"Binary asset     : {bin_path}  ({bin_path.stat().st_size} bytes)")
    print(f"Preview video    : {preview_path}")

    # Auto-copy asset into android project if asset folder exists
    assets_dir = Path("android/app/src/main/assets")
    if assets_dir.exists():
        import shutil
        target_asset = assets_dir / "frames.bin"
        shutil.copy(bin_path, target_asset)
        print(f"Auto-synced to   : {target_asset}")
    else:
        print()
        print("Copy frames.bin into your Android project at:")
        print("  app/src/main/assets/frames.bin")


if __name__ == "__main__":
    main()


