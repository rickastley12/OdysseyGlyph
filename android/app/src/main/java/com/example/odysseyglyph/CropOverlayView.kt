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
        canvas.drawCircle(circleX, circleY, circleRadius, clearPaint)
        canvas.drawCircle(circleX, circleY, circleRadius, borderPaint)
    }
}
