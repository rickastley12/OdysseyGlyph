package com.example.odysseyglyph

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.util.AttributeSet
import com.otaliastudios.zoom.ZoomImageView

class CenteredImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ZoomImageView(context, attrs) {

    init {
        setMinZoom(0.2f)
        setMaxZoom(5.0f)
        setOverScrollHorizontal(true)
        setOverScrollVertical(true)
        setOverPinchable(true)
    }

    fun setImageURIWithAnim(uri: Uri) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val drawable = ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                    // Prevent GPU texture size limit crashes or OutOfMemoryErrors on 50MP images
                    var sampleSize = 1
                    while (info.size.width / sampleSize > 2000 || info.size.height / sampleSize > 2000) {
                        sampleSize *= 2
                    }
                    if (sampleSize > 1) {
                        decoder.setTargetSampleSize(sampleSize)
                    }
                }
                setImageDrawable(drawable)
                if (drawable is AnimatedImageDrawable) {
                    drawable.start()
                }
            } else {
                setImageURI(uri)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            // Fallback
            try {
                setImageURI(uri)
            } catch (fallbackEx: Throwable) {
                fallbackEx.printStackTrace()
            }
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        return super.onTouchEvent(event)
    }
}
