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
     * Reuses the Flash Word-by-Word punctuation-aware character proportional timing logic.
     */
    fun getFrameTextAtTime(
        parsedLines: List<Pair<Long, String>>,
        currentMs: Long,
        fontStyle: GlyphFontEngine.FontStyle,
        animationStyle: Int
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
            currentLyric.first + 4000L // default 4 seconds for last line
        }

        val text = currentLyric.second
        val timeSinceStart = currentMs - currentLyric.first
        
        // Cap the line duration so long instrumental gaps don't cause slow-motion text.
        // However, we MUST guarantee a minimum of 5 seconds so short, held-out vocal notes (e.g. "Ohhhh") 
        // aren't forcefully chopped off!
        val maxDurationMs = Math.max(text.length * 300L, 5000L)
        val gapDuration = (nextTimeMs - currentLyric.first).coerceAtLeast(1L)
        val lineDuration = Math.min(gapDuration, maxDurationMs).toFloat()

        if (timeSinceStart < 0 || timeSinceStart > lineDuration) {
            return null // Blank frame
        }

        var frameText = text
        var offsetX = 0f

        if (animationStyle == 1) {
            // Scroll Left
            val textWidth = GlyphFontEngine.measureTextWidth(text, fontStyle, autoScale = false)
            val progress = timeSinceStart.toFloat() / lineDuration
            offsetX = 25f - (progress * (textWidth + 25f))
        } else {
            // Flash Word-by-Word
            val words = text.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            if (words.isNotEmpty()) {
                val wordWeights = words.map { word ->
                    // Base weight on word length to approximate syllables, with a minimum weight
                    // so short words like "I" don't flash too fast.
                    var weight = Math.max(3f, word.length.toFloat())
                    // Small bump for punctuation pauses, but NOT massive multipliers
                    if (word.endsWith(",") || word.endsWith(".") || word.endsWith("?") || word.endsWith("!")) {
                        weight += 2f
                    }
                    weight
                }
                val totalWeight = wordWeights.sum()
                
                var accumulatedWeight = 0f
                var targetWordIndex = words.size - 1
                
                for ((wIdx, word) in words.withIndex()) {
                    val wordWeight = wordWeights[wIdx]
                    val wordStartWeight = accumulatedWeight
                    val wordEndWeight = accumulatedWeight + wordWeight
                    
                    val wordStartTime = (wordStartWeight / totalWeight) * lineDuration
                    val wordEndTime = (wordEndWeight / totalWeight) * lineDuration
                    
                    if (timeSinceStart >= wordStartTime && timeSinceStart < wordEndTime) {
                        targetWordIndex = wIdx
                        
                        val wordDuration = wordEndTime - wordStartTime
                        val timeInWord = timeSinceStart - wordStartTime
                        val chunkProgress = timeInWord / wordDuration
                        
                        frameText = GlyphFontEngine.formatWordForDisplay(word, fontStyle, chunkProgress)
                        break
                    }
                    accumulatedWeight += wordWeight
                }
            }
            val textWidth = GlyphFontEngine.measureTextWidth(frameText, fontStyle, autoScale = false)
            offsetX = (25f - textWidth) / 2f
        }
        
        return Pair(frameText, offsetX)
    }
}
