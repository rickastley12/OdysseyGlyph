package com.example.odysseyglyph

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GlyphProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridCols
        get() = MatrixConfig.getMatrixSize(context)
    private var progress = 0

    private val paintInactive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
        style = Paint.Style.FILL
    }
    
    private val paintActive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EA3323") // Nothing Red
        style = Paint.Style.FILL
    }

    fun setProgress(prog: Int) {
        progress = Math.max(0, Math.min(100, prog))
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        
        val dotRadius = Math.min(w / gridCols, h) / 2f * 0.8f
        val dotSpacingX = w / gridCols
        val cy = h / 2f
        
        val activeDots = (progress / 100f * gridCols).toInt()

        for (i in 0 until gridCols) {
            val cx = i * dotSpacingX + dotSpacingX / 2f
            val paint = if (i < activeDots) paintActive else paintInactive
            canvas.drawCircle(cx, cy, dotRadius, paint)
        }
    }
}
