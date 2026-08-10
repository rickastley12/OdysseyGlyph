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
                val drawable = ImageDecoder.decodeDrawable(source)
                setImageDrawable(drawable)
                if (drawable is AnimatedImageDrawable) {
                    drawable.start()
                }
            } else {
                setImageURI(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            setImageURI(uri)
        }
    }
}
