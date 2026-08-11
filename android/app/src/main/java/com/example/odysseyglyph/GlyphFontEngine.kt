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
    
    /**
     * Renders a single frame of text onto the 25x25 matrix.
     * @param text The string to render.
     * @param style The typography style.
     * @param scrollOffsetX Horizontal pixel offset (for scrolling text).
     * @return 625-byte array of brightness values (0-255).
     */
    fun renderTextFrame(text: String, style: FontStyle, scrollOffsetX: Float): ByteArray {
        val bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        
        val paint = Paint()
        when (style) {
            FontStyle.PIXEL_TINY -> {
                paint.typeface = Typeface.MONOSPACE
                paint.textSize = 8f
                paint.isAntiAlias = false
                paint.color = Color.WHITE
            }
            FontStyle.BLOCK_BOLD -> {
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textSize = 10f
                paint.isAntiAlias = false
                paint.color = Color.WHITE
            }
            FontStyle.SMOOTH -> {
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 9f
                paint.isAntiAlias = true
                paint.color = Color.WHITE
            }
        }
        
        // Calculate vertical centering
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        val textOffset = textHeight / 2 - fontMetrics.bottom
        val y = 25f / 2f + textOffset
        
        canvas.drawText(text, scrollOffsetX, y, paint)
        
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
    fun measureTextWidth(text: String, style: FontStyle): Float {
        val paint = Paint()
        when (style) {
            FontStyle.PIXEL_TINY -> {
                paint.typeface = Typeface.MONOSPACE
                paint.textSize = 8f
            }
            FontStyle.BLOCK_BOLD -> {
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textSize = 10f
            }
            FontStyle.SMOOTH -> {
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 9f
            }
        }
        return paint.measureText(text)
    }
}
