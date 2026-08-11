package com.example.odysseyglyph

object LrcUtils {

    fun parseLrc(lrcContent: String): List<Pair<Long, String>> {
        val parsedLines = mutableListOf<Pair<Long, String>>()
        val lines = lrcContent.split("\n")
        val regex = Regex("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)")
        
        for (line in lines) {
            val match = regex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msStr = match.groupValues[3]
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                
                val totalMs = min * 60 * 1000 + sec * 1000 + ms
                val text = match.groupValues[4].trim()
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
        val nextTimeMs = if (lyricIndex < parsedLines.size - 1) parsedLines[lyricIndex + 1].first else currentLyric.first + 5000L
        
        if (currentMs < currentLyric.first || currentMs > nextTimeMs) {
            return null // Blank frame
        }

        val text = currentLyric.second
        val timeSinceStart = currentMs - currentLyric.first
        val lineDuration = (nextTimeMs - currentLyric.first).coerceAtLeast(1L).toFloat()
        
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
                    var weight = word.length.toFloat()
                    if (word.endsWith("...")) {
                        weight += 12f
                    } else if (word.endsWith(".") || word.endsWith("!") || word.endsWith("?")) {
                        weight += 8f
                    } else if (word.endsWith(",")) {
                        weight += 5f
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
