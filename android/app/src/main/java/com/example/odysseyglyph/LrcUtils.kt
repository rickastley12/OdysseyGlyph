package com.example.odysseyglyph

object LrcUtils {

    fun parseLrc(lrcContent: String): List<Pair<Long, String>> {
        val parsedLines = mutableListOf<Pair<Long, String>>()
        val lines = lrcContent.split("\n")
        val regex = Regex("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)")
        
        // Use ICU Transliterator to convert Hindi (Devanagari) and other scripts to Latin-ASCII.
        // Falls back safely if transliteration fails.
        val transliterator = try {
            android.icu.text.Transliterator.getInstance("Any-Latin; Latin-ASCII")
        } catch (e: Exception) {
            null
        }
        
        for (line in lines) {
            val match = regex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msStr = match.groupValues[3]
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                
                val totalMs = min * 60 * 1000 + sec * 1000 + ms
                var text = match.groupValues[4].trim()
                
                if (transliterator != null && text.isNotEmpty()) {
                    text = transliterator.transliterate(text)
                }
                
                if (text.isNotEmpty()) {
                    parsedLines.add(Pair(totalMs, text))
                }
            }
        }
        return parsedLines
    }

    /**
     * Finds the lyric line and computes the formatted frame text (with offset) for a given time.
     * Uses character-length proportional weighting to distribute word timing within a line.
     */
    fun getFrameTextAtTime(
        parsedLines: List<Pair<Long, String>>,
        currentMs: Long,
        fontStyle: GlyphFontEngine.FontStyle,
        animationStyle: Int,
        matrixSize: Float = 25f
    ): Pair<String, Float>? {
        if (parsedLines.isEmpty()) return null

        var lyricIndex = 0
        while (lyricIndex < parsedLines.size - 1 && currentMs >= parsedLines[lyricIndex + 1].first) {
            lyricIndex++
        }
        
        val currentLyric = parsedLines[lyricIndex]
        val nextTimeMs = if (lyricIndex < parsedLines.size - 1) {
            parsedLines[lyricIndex + 1].first
        } else {
            currentLyric.first + 4000L
        }

        val text = currentLyric.second
        val timeSinceStart = currentMs - currentLyric.first
        
        // Trust the LRC timestamps directly
        val lineDuration = (nextTimeMs - currentLyric.first).coerceAtLeast(1L).toFloat()
        
        // Blank during long instrumental gaps (>10s) or if we've passed the line
        if (timeSinceStart < 0 || timeSinceStart > lineDuration || lineDuration > 10000f) {
            return null
        }

        var frameText = ""
        var offsetX = 0f

        if (animationStyle == 1) {
            // Scroll Left
            frameText = text
            val textWidth = GlyphFontEngine.measureTextWidth(text, fontStyle, autoScale = false)
            val progress = timeSinceStart.toFloat() / lineDuration
            offsetX = matrixSize - (progress * (textWidth + matrixSize))
        } else {
            // Flash Word-by-Word with character-length proportional timing
            val words = text.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            if (words.isNotEmpty()) {
                val wordWeights = words.map { word ->
                    var weight = Math.max(3f, word.length.toFloat())
                    if (word.endsWith(",") || word.endsWith(".") || word.endsWith("?") || word.endsWith("!")) {
                        weight += 2f
                    }
                    weight
                }
                val totalWeight = wordWeights.sum()
                
                var accumulatedWeight = 0f
                
                for ((wIdx, word) in words.withIndex()) {
                    val wordWeight = wordWeights[wIdx]
                    val wordStartWeight = accumulatedWeight
                    val wordEndWeight = accumulatedWeight + wordWeight
                    
                    val wordStartTime = (wordStartWeight / totalWeight) * lineDuration
                    val wordEndTime = (wordEndWeight / totalWeight) * lineDuration
                    
                    if (timeSinceStart >= wordStartTime && timeSinceStart < wordEndTime) {
                        val wordDuration = wordEndTime - wordStartTime
                        val timeInWord = timeSinceStart - wordStartTime
                        val chunkProgress = timeInWord / wordDuration
                        
                        // Create a dynamic blank gap at the end of every word to create a discrete strobe effect.
                        // 50ms is too fast for the eye. We use 25% of the word duration, capped at 200ms.
                        val gapDuration = Math.min(200f, wordDuration * 0.25f)
                        if (wordDuration > 100f && timeInWord > wordDuration - gapDuration) {
                            frameText = ""
                            break
                        }
                        
                        // Strip trailing punctuation so it doesn't artificially widen the word and ruin centering
                        val cleanWord = word.trimEnd { it == ',' || it == '.' || it == '?' || it == '!' || it == ';' || it == ':' || it == ')' || it == '"' || it == '\'' }
                        if (cleanWord.isEmpty()) {
                            frameText = ""
                            break
                        }
                        
                        if (animationStyle == 2) {
                            // HYBRID: Flash words, but if a word is too long, scroll it leftwards during its time slice.
                            frameText = cleanWord
                            val textWidth = GlyphFontEngine.measureTextWidth(cleanWord, fontStyle, autoScale = false)
                            if (textWidth > matrixSize) {
                                offsetX = matrixSize - (chunkProgress * (textWidth + matrixSize))
                            } else {
                                offsetX = (matrixSize - textWidth) / 2f
                            }
                        } else {
                            // FLASH: Multi-line or strobe chunks for long words
                            frameText = GlyphFontEngine.formatWordForDisplay(cleanWord, fontStyle, chunkProgress, matrixSize.toInt())
                            val textWidth = GlyphFontEngine.measureTextWidth(frameText, fontStyle, autoScale = false)
                            offsetX = (matrixSize - textWidth) / 2f
                        }
                        break
                    }
                    accumulatedWeight += wordWeight
                }
            }
        }
        
        return Pair(frameText, offsetX)
    }
}
