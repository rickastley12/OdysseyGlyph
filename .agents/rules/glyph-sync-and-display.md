# Odyssey Glyph Sync and Display Guidelines

When working on playback synchronization or word display logic in the Odyssey Glyph project, adhere to these strict invariants:

1. **True Audio Clock Sync**: Always use the hardware audio clock (`mediaPlayer.currentPosition()`) to derive the target visual frame index. NEVER use `postDelayed` or timer ticks to manually advance frames, as Android scheduling jitter accumulates and causes massive sync drift over time.
2. **Readability Over Formatting**: Do NOT use `autoScale = true` when rendering text to the Glyph matrix, and do not group multiple words if it requires shrinking the text below `12f`. The 25x25 matrix resolution makes small text completely illegible.
3. **Pixel-Aware Time Chunking**: For wide words that exceed the matrix width, do not rely on raw character counts. Use `GlyphFontEngine.formatWordForDisplay()` with `autoScale = false` to split words based on exact pixel-width boundaries, and flash them sequentially (time-chunking) to maintain the large, readable `12f` font without clipping the edges.
