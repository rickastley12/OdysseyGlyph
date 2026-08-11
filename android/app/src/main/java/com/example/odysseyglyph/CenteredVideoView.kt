package com.example.odysseyglyph

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Surface
import com.otaliastudios.zoom.ZoomSurfaceView

class CenteredVideoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ZoomSurfaceView(context, attrs) {

    val mediaPlayer = MediaPlayer()
    
    var isVideoPrepared = false
    private var videoWidth = 0
    private var videoHeight = 0

    init {
        addCallback(object : ZoomSurfaceView.Callback {
            override fun onZoomSurfaceCreated(view: ZoomSurfaceView) {
                mediaPlayer.setSurface(view.surface)
            }
            override fun onZoomSurfaceDestroyed(view: ZoomSurfaceView) {
                mediaPlayer.setSurface(null)
            }
        })
        
        mediaPlayer.setOnVideoSizeChangedListener { _, width, height ->
            videoWidth = width
            videoHeight = height
            // ZoomSurfaceView natively handles centering, we just set the video size
            setContentSize(width.toFloat(), height.toFloat())
        }
        
        mediaPlayer.setOnPreparedListener {
            isVideoPrepared = true
            setContentSize(mediaPlayer.videoWidth.toFloat(), mediaPlayer.videoHeight.toFloat())
            onPreparedListener?.invoke(mediaPlayer)
        }
        
        // Configure standard zoom properties
        setMinZoom(0.2f)
        setMaxZoom(5.0f)
        setOverScrollHorizontal(true)
        setOverScrollVertical(true)
        setOverPinchable(true)
        setTransformation(
            com.otaliastudios.zoom.ZoomApi.TRANSFORMATION_CENTER_CROP,
            com.otaliastudios.zoom.ZoomApi.TRANSFORMATION_GRAVITY_AUTO
        )
    }

    private var onPreparedListener: ((MediaPlayer) -> Unit)? = null
    fun setOnPreparedListener(listener: (MediaPlayer) -> Unit) {
        onPreparedListener = listener
    }

    fun setVideoURI(uri: Uri) {
        isVideoPrepared = false
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun start() {
        if (isVideoPrepared) mediaPlayer.start()
    }

    fun pause() {
        if (isVideoPrepared) mediaPlayer.pause()
    }

    fun seekTo(msec: Int) {
        if (isVideoPrepared) mediaPlayer.seekTo(msec)
    }

    val currentPosition: Int
        get() = if (isVideoPrepared) mediaPlayer.currentPosition else 0

    val isPlaying: Boolean
        get() = if (isVideoPrepared) mediaPlayer.isPlaying else false

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (engine.isPanning || engine.isZooming) {
            parent?.requestDisallowInterceptTouchEvent(true)
        } else {
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        return super.onTouchEvent(event)
    }

    fun release() {
        mediaPlayer.release()
    }
}
