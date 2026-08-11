package com.example.odysseyglyph

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

object GlyphFontEngine {
    
    enum class FontStyle {
        PIXEL_TINY,   // ~3x5 logical size using a tiny font size
        BLOCK_BOLD,   // ~5x5 bold
        SMOOTH        // Anti-aliased smooth font
    }
    
    private fun getConfiguredPaint(text: String, style: FontStyle, autoScale: Boolean): Paint {
        val paint = Paint()
        paint.color = Color.WHITE
        when (style) {
            FontStyle.PIXEL_TINY -> {
                paint.typeface = Typeface.MONOSPACE
                paint.textSize = 9f
                paint.isAntiAlias = false
            }
            FontStyle.BLOCK_BOLD -> {
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textSize = 12f
                paint.isAntiAlias = false
            }
            FontStyle.SMOOTH -> {
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                paint.textSize = 12f
                paint.isAntiAlias = true
            }
        }
        
        if (autoScale) {
            val lines = text.split("\n")
            val isMultiline = lines.size > 1
            
            if (isMultiline && paint.textSize > 7.5f) {
                paint.textSize = 7.5f
            }
            
            val maxWidth = if (isMultiline) 17f else 22f
            
            while (lines.any { paint.measureText(it) > maxWidth } && paint.textSize > 5f) {
                paint.textSize -= 0.5f
            }
        }
        return paint
    }

    /**
     * Renders a single frame of text onto the 25x25 matrix.
     * @param text The string to render.
     * @param style The typography style.
     * @param scrollOffsetX Horizontal pixel offset (for scrolling text).
     * @param autoScale Whether to shrink text to fit the 25px width.
     * @return 625-byte array of brightness values (0-255).
     */
    fun renderTextFrame(text: String, style: FontStyle, scrollOffsetX: Float, autoScale: Boolean = false): ByteArray {
        val bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        
        val paint = getConfiguredPaint(text, style, autoScale)
        
        val lines = text.split("\n")
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        
        if (lines.size == 1) {
            val textOffset = textHeight / 2 - fontMetrics.bottom
            val y = 25f / 2f + textOffset
            canvas.drawText(lines[0], scrollOffsetX, y, paint)
        } else {
            // Compress line spacing so words don't hit the narrow top/bottom edges of the circular matrix
            val lineHeight = textHeight * 0.8f
            val totalHeight = lineHeight * lines.size
            var startY = (25f - totalHeight) / 2f - fontMetrics.top
            for (line in lines) {
                // Ignore scrollOffsetX for multiline since it's only used in Flash mode, which calculates its own line X
                val lineWidth = paint.measureText(line)
                val lineX = (25f - lineWidth) / 2f
                canvas.drawText(line, lineX, startY, paint)
                startY += lineHeight
            }
        }
        
        // Extract to ByteArray
        val pixels = IntArray(625)
        bitmap.getPixels(pixels, 0, 25, 0, 0, 25, 25)
        
        val output = ByteArray(625)
        val center = 12f
        val radiusSq = 12f * 12f
        
        for (i in 0 until 625) {
            val px = i % 25
            val py = i / 25
            
            // Circular mask
            val distSq = (px - center) * (px - center) + (py - center) * (py - center)
            if (distSq <= radiusSq) {
                val color = pixels[i]
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                output[i] = gray.toByte()
            } else {
                output[i] = 0
            }
        }
        
        bitmap.recycle()
        return output
    }
    
    /**
     * Helper to measure text width for scrolling calculations.
     */
    fun measureTextWidth(text: String, style: FontStyle, autoScale: Boolean = false): Float {
        val paint = getConfiguredPaint(text, style, autoScale)
        val lines = text.split("\n")
        return lines.maxOfOrNull { paint.measureText(it) } ?: 0f
    }
}
