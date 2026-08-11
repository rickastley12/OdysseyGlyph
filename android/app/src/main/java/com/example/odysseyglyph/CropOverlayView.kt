package com.example.odysseyglyph

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CropOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var circleX = -1f
    var circleY = -1f
    var circleRadius = 200f

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#BB000000") // 73% black
        style = Paint.Style.FILL
    }
    
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF") // 20% white
        strokeWidth = 1f
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        // Pass touches through to the view below (the ZoomableVideoView)
        isClickable = false
        isFocusable = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Fixed in the exact center of the screen
        circleX = w / 2f
        circleY = h / 2f
        // Make the circle take up 80% of the screen width/height (whichever is smaller)
        circleRadius = minOf(w, h) * 0.4f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        
        // Clip to circle so grid only draws inside
        canvas.save()
        val path = Path()
        path.addCircle(circleX, circleY, circleRadius, Path.Direction.CW)
        canvas.clipPath(path)
        
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        
        // Draw 25x25 grid
        val gridSize = 25
        val startX = circleX - circleRadius
        val startY = circleY - circleRadius
        val step = (circleRadius * 2) / gridSize
        
        for (i in 1 until gridSize) {
            val x = startX + i * step
            canvas.drawLine(x, startY, x, startY + circleRadius * 2, gridPaint)
            
            val y = startY + i * step
            canvas.drawLine(startX, y, startX + circleRadius * 2, y, gridPaint)
        }
        
        canvas.restore()
        
        canvas.drawCircle(circleX, circleY, circleRadius, borderPaint)
    }
}
