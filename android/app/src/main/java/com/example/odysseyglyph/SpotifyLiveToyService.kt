package com.example.odysseyglyph

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.SystemClock
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy
import kotlin.concurrent.thread

class SpotifyLiveToyService : Service(), SpotifyPlaybackState.StateChangeListener {
    
    private var glyphManager: GlyphMatrixManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val messenger = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            // no-op, but needed for Toy
        }
    })
    
    private var isRegistered = false
    private var currentParsedLyrics: List<Pair<Long, String>> = emptyList()
    private var currentTrackId = ""
    private var fontStyle = GlyphFontEngine.FontStyle.SMOOTH
    
    private val glyphCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            glyphManager?.register(Glyph.DEVICE_23112)
            isRegistered = true
            SpotifyPlaybackState.addListener(this@SpotifyLiveToyService)
            
            val prefs = getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE)
            val fontStyleInt = prefs.getInt("live_font_style", 0)
            fontStyle = if (fontStyleInt == 1) GlyphFontEngine.FontStyle.BLOCK_BOLD else GlyphFontEngine.FontStyle.SMOOTH
            
            // Initial fetch if active
            if (SpotifyPlaybackState.hasActiveSession) {
                onMetadataChanged(SpotifyPlaybackState.trackTitle, SpotifyPlaybackState.artist)
            }
            
            mainHandler.post(playbackRunnable)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            isRegistered = false
            SpotifyPlaybackState.removeListener(this@SpotifyLiveToyService)
        }
    }
    
    private val playbackRunnable = object : Runnable {
        override fun run() {
            if (!isRegistered) return
            
            val prefs = getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("live_lyrics_enabled", false)
            
            val lyricsToUse = SpotifyPlaybackState.manualOverrideLyrics ?: currentParsedLyrics
            
            if (!isEnabled || !SpotifyPlaybackState.hasActiveSession || lyricsToUse.isEmpty()) {
                glyphManager?.turnOff()
                mainHandler.postDelayed(this, 100L) // Slow poll when inactive/unmatched
                return
            }
            
            val animStyle = prefs.getInt("live_anim_style", 0)
            
            var estimatedPositionMs = SpotifyPlaybackState.position
            if (SpotifyPlaybackState.isPlaying) {
                estimatedPositionMs += ((SystemClock.elapsedRealtime() - SpotifyPlaybackState.lastUpdateTime) * SpotifyPlaybackState.playbackSpeed).toLong()
            }
            
            val frameData = LrcUtils.getFrameTextAtTime(lyricsToUse, estimatedPositionMs, fontStyle, animStyle)
            
            if (frameData == null) {
                glyphManager?.turnOff()
            } else {
                val frameText = frameData.first
                val offsetX = frameData.second
                val frameBytes = GlyphFontEngine.renderTextFrame(frameText, fontStyle, offsetX, autoScale = false)
                glyphManager?.setMatrixFrame(toIntArray(frameBytes))
            }
            
            mainHandler.postDelayed(this, 30L)
        }
    }
    
    private fun toIntArray(bytes: ByteArray): IntArray {
        val ints = IntArray(bytes.size)
        for (i in bytes.indices) {
            // Scale 8-bit (0-255) to 12-bit (0-4095) for the Glyph SDK
            ints[i] = (bytes[i].toInt() and 0xFF) * 16
        }
        return ints
    }

    override fun onCreate() {
        super.onCreate()
        glyphManager = GlyphMatrixManager.getInstance(applicationContext)
        glyphManager?.init(glyphCallback)
    }

    override fun onDestroy() {
        isRegistered = false
        SpotifyPlaybackState.removeListener(this)
        mainHandler.removeCallbacks(playbackRunnable)
        glyphManager?.turnOff()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return messenger.binder
    }

    override fun onMetadataChanged(title: String, artist: String) {
        val trackId = "$title-$artist"
        if (currentTrackId != trackId && title.isNotEmpty()) {
            currentTrackId = trackId
            currentParsedLyrics = emptyList() // clear old
            
            thread {
                val query = LrcQueryCleaner.clean("$title $artist")
                val results = LRCLibClient.searchLyrics(query)
                val syncedMatch = results.firstOrNull { it.syncedLyrics != null }
                if (syncedMatch != null) {
                    val parsed = LrcUtils.parseLrc(syncedMatch.syncedLyrics!!)
                    mainHandler.post {
                        if (currentTrackId == trackId) {
                            currentParsedLyrics = parsed
                        }
                    }
                }
            }
        }
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        // State changes are smoothly handled by the polling loop reading from the global state
    }
}
