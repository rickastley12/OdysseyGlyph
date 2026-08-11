package com.example.odysseyglyph

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlin.concurrent.thread

class LiveLyricsActivity : AppCompatActivity(), MusicPlaybackState.StateChangeListener {

    private lateinit var prefs: SharedPreferences
    
    private lateinit var switchMaster: MaterialSwitch
    private lateinit var tvStatus: TextView
    private lateinit var tvTrackInfo: TextView
    private lateinit var previewImage: ImageView
    
    private lateinit var permissionCard: LinearLayout
    private lateinit var btnGrantPermission: MaterialButton
    
    private lateinit var toggleAnimation: MaterialButtonToggleGroup
    private lateinit var toggleTypography: MaterialButtonToggleGroup
    private lateinit var btnOpenManager: MaterialButton
    
    private lateinit var optionsContainer: LinearLayout
    
    private var currentFontStyle = GlyphFontEngine.FontStyle.SMOOTH
    private var isNotificationAccessGranted = false
    
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_lyrics)
        
        prefs = getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        
        switchMaster = findViewById(R.id.switchMaster)
        tvStatus = findViewById(R.id.tvStatus)
        tvTrackInfo = findViewById(R.id.tvTrackInfo)
        previewImage = findViewById(R.id.previewImage)
        
        permissionCard = findViewById(R.id.permissionCard)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        
        toggleAnimation = findViewById(R.id.toggleAnimation)
        toggleTypography = findViewById(R.id.toggleTypography)
        btnOpenManager = findViewById(R.id.btnOpenManager)
        
        optionsContainer = findViewById(R.id.optionsContainer)
        
        switchMaster.isChecked = prefs.getBoolean("live_lyrics_enabled", false)
        optionsContainer.visibility = if (switchMaster.isChecked) View.VISIBLE else View.GONE
        
        switchMaster.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("live_lyrics_enabled", isChecked).apply()
            optionsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        val savedAnimStyle = prefs.getInt("live_anim_style", 0)
        when (savedAnimStyle) {
            1 -> toggleAnimation.check(R.id.btnAnimScroll)
            2 -> toggleAnimation.check(R.id.btnAnimHybrid)
            else -> toggleAnimation.check(R.id.btnAnimFlash)
        }
        
        toggleAnimation.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val style = when (checkedId) {
                    R.id.btnAnimScroll -> 1
                    R.id.btnAnimHybrid -> 2
                    else -> 0
                }
                prefs.edit().putInt("live_anim_style", style).apply()
            }
        }
        
        val tvSyncOffset: TextView = findViewById(R.id.tvSyncOffset)
        val sliderSync: com.google.android.material.slider.Slider = findViewById(R.id.sliderSync)
        
        val savedSyncOffset = prefs.getInt("live_sync_offset", 0)
        sliderSync.value = savedSyncOffset.toFloat()
        tvSyncOffset.text = "${savedSyncOffset}ms"
        
        sliderSync.addOnChangeListener { _, value, _ ->
            val offsetMs = value.toInt()
            tvSyncOffset.text = "${offsetMs}ms"
            prefs.edit().putInt("live_sync_offset", offsetMs).apply()
        }
        
        val savedFontStyle = prefs.getInt("live_font_style", 0)
        currentFontStyle = when (savedFontStyle) {
            1 -> GlyphFontEngine.FontStyle.BLOCK_BOLD
            2 -> GlyphFontEngine.FontStyle.PIXEL_TINY
            else -> GlyphFontEngine.FontStyle.SMOOTH
        }
        
        when (currentFontStyle) {
            GlyphFontEngine.FontStyle.BLOCK_BOLD -> toggleTypography.check(R.id.btnBlocky)
            GlyphFontEngine.FontStyle.PIXEL_TINY -> toggleTypography.check(R.id.btnPixel)
            else -> toggleTypography.check(R.id.btnSmooth)
        }
        
        toggleTypography.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentFontStyle = when (checkedId) {
                    R.id.btnBlocky -> GlyphFontEngine.FontStyle.BLOCK_BOLD
                    R.id.btnPixel -> GlyphFontEngine.FontStyle.PIXEL_TINY
                    else -> GlyphFontEngine.FontStyle.SMOOTH
                }
                
                val styleInt = when (currentFontStyle) {
                    GlyphFontEngine.FontStyle.BLOCK_BOLD -> 1
                    GlyphFontEngine.FontStyle.PIXEL_TINY -> 2
                    else -> 0
                }
                prefs.edit().putInt("live_font_style", styleInt).apply()
            }
        }
        
        btnGrantPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        
        btnOpenManager.setOnClickListener {
            try {
                val intent = Intent()
                intent.component = ComponentName("com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity")
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(findViewById(android.R.id.content), "Toys Manager not found.", Snackbar.LENGTH_LONG).show()
            }
        }
        
        // Search functionality removed
        
        if (prefs.getBoolean("first_run_live_v2", true)) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_nothing_onboarding, null)
            val dialog = AlertDialog.Builder(this).setView(dialogView).create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            dialogView.findViewById<TextView>(R.id.tvDialogTitle).text = "LIVE LYRICS"
            dialogView.findViewById<TextView>(R.id.tvDialogMessage).text = "Live Lyrics reads your phone's media playback (like Spotify or YouTube Music) and automatically searches for synchronized lyrics. If a match is found, it streams them directly to your Glyph matrix in real-time as the song plays!"
            dialogView.findViewById<MaterialButton>(R.id.btnDialogAction).setOnClickListener {
                prefs.edit().putBoolean("first_run_live_v2", false).apply()
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
        MusicPlaybackState.addListener(this)
        updateUIState()
        mainHandler.post(previewUpdater)
    }

    override fun onPause() {
        super.onPause()
        MusicPlaybackState.removeListener(this)
        mainHandler.removeCallbacks(previewUpdater)
    }

    private fun checkNotificationPermission() {
        val componentName = ComponentName(this, MediaSessionListenerService::class.java)
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        isNotificationAccessGranted = enabledListeners?.contains(componentName.flattenToString()) == true
        
        if (isNotificationAccessGranted) {
            permissionCard.visibility = View.GONE
        } else {
            permissionCard.visibility = View.VISIBLE
        }
    }

    private fun updateUIState() {
        if (!isNotificationAccessGranted) {
            tvStatus.text = "Missing Permission"
            tvTrackInfo.text = "Enable Notification Access to read Spotify playback."
            return
        }
        
        if (!MusicPlaybackState.hasActiveSession) {
            tvStatus.text = "Waiting for Music..."
            tvTrackInfo.text = "Play a track to begin."
        } else {
            val title = MusicPlaybackState.trackTitle
            val artist = MusicPlaybackState.artist
            
            if (MusicPlaybackState.manualOverrideLyrics != null) {
                tvStatus.text = "Live Syncing"
                tvTrackInfo.text = "$title — $artist"
            } else if (title.isNotEmpty()) {
                tvStatus.text = "Active Session"
                tvTrackInfo.text = "$title — $artist\n(Searching for lyrics...)"
            } else {
                tvStatus.text = "Active Session"
                tvTrackInfo.text = "Unknown track."
            }
        }
    }

    override fun onMetadataChanged(title: String, artist: String) {
        mainHandler.post { updateUIState() }
        
        if (title.isNotEmpty()) {
            thread {
                val results = LRCLibClient.searchLyrics("$title $artist")
                val syncedResult = results.firstOrNull { it.syncedLyrics != null }
                if (syncedResult != null) {
                    val parsed = LrcUtils.parseLrc(syncedResult.syncedLyrics)
                    MusicPlaybackState.manualOverrideLyrics = parsed
                    MusicPlaybackState.manualOverrideTrackName = syncedResult.trackName
                    mainHandler.post { updateUIState() }
                } else {
                    MusicPlaybackState.manualOverrideLyrics = null
                    mainHandler.post { 
                        if (MusicPlaybackState.trackTitle == title) {
                            tvTrackInfo.text = "$title — $artist\n(No synced lyrics found)"
                        }
                    }
                }
            }
        }
    }





    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        mainHandler.post { updateUIState() }
    }

    private val previewUpdater = object : Runnable {
        var sleepTick = 0f
        
        override fun run() {
            if (!switchMaster.isChecked || !MusicPlaybackState.hasActiveSession) {
                mainHandler.postDelayed(this, 100L)
                return
            }

            var textToRender = ""
            var scrollOffset = 0f
            
            if (MusicPlaybackState.isPlaying && MusicPlaybackState.manualOverrideLyrics != null) {
                val syncOffset = prefs.getInt("live_sync_offset", 0).toLong()
                val currentPos = MusicPlaybackState.position + 
                    (System.currentTimeMillis() - MusicPlaybackState.lastUpdateTime) * MusicPlaybackState.playbackSpeed.toLong() +
                    syncOffset
                
                val lyrics = MusicPlaybackState.manualOverrideLyrics!!
                val currentLine = lyrics.lastOrNull { it.first <= currentPos }
                if (currentLine != null) {
                    textToRender = currentLine.second
                }
            } else if (!MusicPlaybackState.isPlaying) {
                sleepTick += 0.2f
                val zCount = (sleepTick.toInt() % 4)
                textToRender = "Z".repeat(zCount)
            }
            
            if (textToRender.isNotEmpty()) {
                val rawMatrix = GlyphFontEngine.renderTextFrame(textToRender, currentFontStyle, scrollOffset, autoScale = true)
                val bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)
                val pixels = IntArray(625)
                for (i in 0 until 625) {
                    val bright = rawMatrix[i].toInt() and 0xFF
                    pixels[i] = Color.argb(255, bright, bright, bright)
                }
                bitmap.setPixels(pixels, 0, 25, 0, 0, 25, 25)
                val scaled = Bitmap.createScaledBitmap(bitmap, 250, 250, false)
                previewImage.setImageBitmap(scaled)
            } else {
                previewImage.setImageDrawable(null)
            }
            
            mainHandler.postDelayed(this, 100L)
        }
    }
}
