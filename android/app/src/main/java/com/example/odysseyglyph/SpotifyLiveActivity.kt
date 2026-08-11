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
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlin.concurrent.thread

class SpotifyLiveActivity : AppCompatActivity(), SpotifyPlaybackState.StateChangeListener {

    private lateinit var prefs: SharedPreferences
    
    private lateinit var switchMaster: MaterialSwitch
    private lateinit var tvStatus: TextView
    private lateinit var tvTrackInfo: TextView
    private lateinit var previewImage: ImageView
    
    private lateinit var permissionCard: LinearLayout
    private lateinit var btnGrantPermission: MaterialButton
    
    private lateinit var toggleTypography: MaterialButtonToggleGroup
    private lateinit var btnOpenManager: MaterialButton
    
    private lateinit var searchContainer: LinearLayout
    private lateinit var etSearch: TextInputEditText
    private lateinit var btnSearch: MaterialButton
    private lateinit var resultsContainer: LinearLayout
    
    private var currentFontStyle = GlyphFontEngine.FontStyle.SMOOTH
    private var isNotificationAccessGranted = false
    
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotify_live)
        
        prefs = getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        
        switchMaster = findViewById(R.id.switchMaster)
        tvStatus = findViewById(R.id.tvStatus)
        tvTrackInfo = findViewById(R.id.tvTrackInfo)
        previewImage = findViewById(R.id.previewImage)
        
        permissionCard = findViewById(R.id.permissionCard)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        
        toggleTypography = findViewById(R.id.toggleTypography)
        btnOpenManager = findViewById(R.id.btnOpenManager)
        
        searchContainer = findViewById(R.id.searchContainer)
        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        resultsContainer = findViewById(R.id.resultsContainer)
        
        switchMaster.isChecked = prefs.getBoolean("live_lyrics_enabled", false)
        switchMaster.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("live_lyrics_enabled", isChecked).apply()
        }
        
        val savedFontStyle = prefs.getInt("live_font_style", 0)
        currentFontStyle = if (savedFontStyle == 1) GlyphFontEngine.FontStyle.BLOCKY else GlyphFontEngine.FontStyle.SMOOTH
        if (currentFontStyle == GlyphFontEngine.FontStyle.BLOCKY) {
            toggleTypography.check(R.id.btnBlocky)
        } else {
            toggleTypography.check(R.id.btnSmooth)
        }
        
        toggleTypography.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentFontStyle = if (checkedId == R.id.btnBlocky) {
                    GlyphFontEngine.FontStyle.BLOCKY
                } else {
                    GlyphFontEngine.FontStyle.SMOOTH
                }
                prefs.edit().putInt("live_font_style", if (currentFontStyle == GlyphFontEngine.FontStyle.BLOCKY) 1 else 0).apply()
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
        
        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
        SpotifyPlaybackState.addListener(this)
        updateUIState()
        mainHandler.post(previewUpdater)
    }

    override fun onPause() {
        super.onPause()
        SpotifyPlaybackState.removeListener(this)
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
        
        if (!SpotifyPlaybackState.hasActiveSession) {
            tvStatus.text = "Waiting for Spotify..."
            tvTrackInfo.text = "Play a track on Spotify to begin."
            searchContainer.visibility = View.GONE
        } else {
            val title = SpotifyPlaybackState.trackTitle
            val artist = SpotifyPlaybackState.artist
            
            if (title.isNotEmpty()) {
                tvStatus.text = "Active Session"
                tvTrackInfo.text = "$title — $artist"
                
                // Show search container for manual fallback
                searchContainer.visibility = View.VISIBLE
                if (etSearch.text.isNullOrEmpty()) {
                    etSearch.setText("$title $artist")
                }
            } else {
                tvStatus.text = "Active Session"
                tvTrackInfo.text = "Unknown track."
            }
        }
    }

    private fun performSearch(query: String) {
        resultsContainer.removeAllViews()
        
        val loadingText = TextView(this).apply {
            text = "Searching..."
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }
        resultsContainer.addView(loadingText)
        
        thread {
            val results = LRCLibClient.searchLyrics(query)
            
            mainHandler.post {
                resultsContainer.removeAllViews()
                if (results.isEmpty()) {
                    resultsContainer.addView(TextView(this).apply {
                        text = "No matches found."
                        setTextColor(ContextCompat.getColor(this@SpotifyLiveActivity, R.color.colorError))
                        gravity = android.view.Gravity.CENTER
                    })
                    return@post
                }
                
                val syncedResults = results.filter { it.syncedLyrics != null }
                if (syncedResults.isEmpty()) {
                    resultsContainer.addView(TextView(this).apply {
                        text = "Matches found, but none have synchronized lyrics."
                        setTextColor(ContextCompat.getColor(this@SpotifyLiveActivity, R.color.colorWarning))
                        gravity = android.view.Gravity.CENTER
                    })
                    return@post
                }
                
                for (track in syncedResults) {
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        background = ContextCompat.getDrawable(this@SpotifyLiveActivity, R.drawable.bg_search_result)
                        setPadding(32, 32, 32, 32)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 16)
                        }
                        
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            // Normally we would parse and apply this, but the toy service handles it automatically 
                            // via track metadata. We can't easily inject manual lyrics into the toy service 
                            // without a more complex IPC or shared DB. For this beta, we just show if it exists.
                            Snackbar.make(findViewById(android.R.id.content), "Selected ${track.trackName}", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    
                    val tvTrack = TextView(this).apply {
                        text = track.trackName
                        setTextColor(Color.WHITE)
                        textSize = 16f
                    }
                    val tvArtist = TextView(this).apply {
                        text = track.artistName + (if (track.albumName.isNotEmpty()) " • ${track.albumName}" else "")
                        setTextColor(Color.parseColor("#888888"))
                        textSize = 14f
                    }
                    
                    card.addView(tvTrack)
                    card.addView(tvArtist)
                    resultsContainer.addView(card)
                }
            }
        }
    }

    override fun onMetadataChanged(title: String, artist: String) {
        mainHandler.post { updateUIState() }
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        mainHandler.post { updateUIState() }
    }

    private val previewUpdater = object : Runnable {
        override fun run() {
            // Very rudimentary live preview. The Toy Service handles real rendering,
            // this just gives a visual indicator in the activity.
            if (switchMaster.isChecked && SpotifyPlaybackState.hasActiveSession) {
                // Here we would ideally duplicate the Toy Service parsing logic to show a live preview.
                // But for the sake of simplicity, we just leave the previewImage blank unless we implement
                // shared memory parsing.
            }
            mainHandler.postDelayed(this, 100L)
        }
    }
}
