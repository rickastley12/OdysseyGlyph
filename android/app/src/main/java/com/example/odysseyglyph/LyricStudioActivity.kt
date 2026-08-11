package com.example.odysseyglyph

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.RangeSlider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class LyricStudioActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    
    // UI Elements
    private lateinit var toolbar: MaterialToolbar
    private lateinit var etSearch: TextInputEditText
    private lateinit var resultsContainer: LinearLayout
    private lateinit var previewCard: MaterialCardView
    private lateinit var previewImage: CenteredImageView
    
    private lateinit var editorPanel: LinearLayout
    private lateinit var toggleTypography: MaterialButtonToggleGroup
    private lateinit var toggleAnimation: MaterialButtonToggleGroup
    
    private lateinit var tvTrimTimes: TextView
    private lateinit var tvLyricPreview: TextView
    private lateinit var rangeSlider: RangeSlider
    private lateinit var slotSpinner: AutoCompleteTextView
    
    private lateinit var audioCard: MaterialCardView
    private lateinit var tvAudioStatus: TextView
    private lateinit var btnAttachAudio: MaterialButton
    private lateinit var btnDrmInfo: ImageButton
    
    private lateinit var btnRender: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressBar: GlyphProgressView
    private lateinit var btnOpenManager: MaterialButton

    // State
    private var selectedAudioUri: Uri? = null
    private var selectedTrack: LrcTrack? = null
    private var currentFontStyle = GlyphFontEngine.FontStyle.SMOOTH
    private var currentAnimStyle = 0 // 0=Flash, 1=Scroll
    private var isProgrammaticTextChange = false
    private var searchJobId = 0
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRendering = AtomicBoolean(false)
    private var parsedLyricsCache = mutableListOf<Pair<Long, String>>()

    private val selectAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            selectedAudioUri = uri
            updateAudioStatus("Attached: Manual Selection", true)
        }
    }

    private val requestAudioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            selectedTrack?.let { attemptAutoMatchAudio(it) }
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Audio permission denied. Cannot auto-match local audio.", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_lyric_studio)
        prefs = getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE)

        bindViews()
        setupListeners()
        syncSlotState()
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbar)
        etSearch = findViewById(R.id.etSearch)
        resultsContainer = findViewById(R.id.resultsContainer)
        previewCard = findViewById(R.id.previewCard)
        previewImage = findViewById(R.id.previewImage)
        
        editorPanel = findViewById(R.id.editorPanel)
        toggleTypography = findViewById(R.id.toggleTypography)
        toggleAnimation = findViewById(R.id.toggleAnimation)
        
        tvTrimTimes = findViewById(R.id.tvTrimTimes)
        tvLyricPreview = findViewById(R.id.tvLyricPreview)
        rangeSlider = findViewById(R.id.rangeSlider)
        slotSpinner = findViewById(R.id.slotSpinner)
        
        audioCard = findViewById(R.id.audioCard)
        tvAudioStatus = findViewById(R.id.tvAudioStatus)
        btnAttachAudio = findViewById(R.id.btnAttachAudio)
        
        btnRender = findViewById(R.id.btnRender)
        btnCancel = findViewById(R.id.btnCancel)
        progressBar = findViewById(R.id.progressBar)
        btnOpenManager = findViewById(R.id.btnOpenManager)
    }

    private fun setupListeners() {
        toolbar.setNavigationOnClickListener { finish() }

        // Search Debounce & Re-entrancy
        etSearch.addTextChangedListener(object : TextWatcher {
            private val searchRunnable = Runnable {
                val query = etSearch.text.toString()
                if (query.length > 2) performSearch(query)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticTextChange) return
                mainHandler.removeCallbacks(searchRunnable)
                mainHandler.postDelayed(searchRunnable, 400) // 400ms debounce
            }
        })

        // Toggles
        toggleTypography.check(R.id.btnTypeSmooth)
        toggleTypography.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentFontStyle = when (checkedId) {
                    R.id.btnTypePixel -> GlyphFontEngine.FontStyle.PIXEL_TINY
                    R.id.btnTypeBold -> GlyphFontEngine.FontStyle.BLOCK_BOLD
                    else -> GlyphFontEngine.FontStyle.SMOOTH
                }
                updateWysiwygPreview()
            }
        }

        toggleAnimation.check(R.id.btnAnimFlash)
        toggleAnimation.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentAnimStyle = if (checkedId == R.id.btnAnimScroll) 1 else 0
                updateWysiwygPreview()
            }
        }

        // Sliders
        rangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            tvTrimTimes.text = String.format("%.1fs - %.1fs", values[0], values[1])
            updateLyricPreviewLine(values[0])
        }

        // Audio
        btnAttachAudio.setOnClickListener { selectAudioLauncher.launch(arrayOf("audio/*")) }

        // Rendering
        btnRender.setOnClickListener { renderLyrics() }
        btnCancel.setOnClickListener { cancelProcessing() }

        // Toys Manager
        btnOpenManager.setOnClickListener {
            try {
                val intent = Intent()
                intent.component = ComponentName("com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity")
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(findViewById(android.R.id.content), "Nothing OS Toys Manager not found.", Snackbar.LENGTH_LONG).show()
            }
        }
        
        slotSpinner.setOnItemClickListener { _, _, position, _ ->
            prefs.edit().putInt("selected_slot", position + 1).apply()
        }
    }

    private fun syncSlotState() {
        val slot = prefs.getInt("selected_slot", 1)
        slotSpinner.setText("Slot $slot", false)
    }

    private fun updateAudioStatus(text: String, attached: Boolean) {
        tvAudioStatus.text = text
        if (attached) {
            tvAudioStatus.setTextColor(ContextCompat.getColor(this, R.color.colorSuccess))
            btnAttachAudio.text = "CHANGE AUDIO"
        } else {
            tvAudioStatus.setTextColor(ContextCompat.getColor(this, R.color.colorOnSurface))
            btnAttachAudio.text = "SELECT LOCAL AUDIO"
        }
    }

    private fun performSearch(query: String) {
        resultsContainer.removeAllViews()
        val jobId = ++searchJobId
        
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
                if (jobId != searchJobId) return@post // Stale response guard
                
                resultsContainer.removeAllViews()
                if (results.isEmpty()) {
                    resultsContainer.addView(TextView(this).apply {
                        text = "No matches found for \"$query\"."
                        setTextColor(ContextCompat.getColor(this@LyricStudioActivity, R.color.colorError))
                        gravity = android.view.Gravity.CENTER
                    })
                    return@post
                }
                
                val syncedResults = results.filter { it.syncedLyrics != null }
                if (syncedResults.isEmpty()) {
                    resultsContainer.addView(TextView(this).apply {
                        text = "Matches found, but none have synchronized lyrics."
                        setTextColor(ContextCompat.getColor(this@LyricStudioActivity, R.color.colorError))
                        gravity = android.view.Gravity.CENTER
                    })
                    return@post
                }
                
                for (track in syncedResults) {
                    val card = MaterialCardView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, 16)
                        }
                        setCardBackgroundColor(ContextCompat.getColor(this@LyricStudioActivity, R.color.colorSurfaceVariant))
                        radius = 24f
                        
                        val inner = LinearLayout(this@LyricStudioActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(32, 32, 32, 32)
                        }
                        
                        inner.addView(TextView(this@LyricStudioActivity).apply {
                            text = track.trackName
                            setTextColor(Color.WHITE)
                            textSize = 16f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                        })
                        
                        inner.addView(TextView(this@LyricStudioActivity).apply {
                            text = "${track.artistName} • ${track.albumName}"
                            setTextColor(ContextCompat.getColor(this@LyricStudioActivity, R.color.colorOnSurfaceVariant))
                            textSize = 12f
                        })
                        
                        addView(inner)
                        
                        setOnClickListener {
                            selectTrack(track)
                        }
                    }
                    resultsContainer.addView(card)
                }
            }
        }
    }

    private fun selectTrack(track: LrcTrack) {
        selectedTrack = track
        
        isProgrammaticTextChange = true
        etSearch.setText(track.trackName)
        isProgrammaticTextChange = false
        
        resultsContainer.removeAllViews()
        editorPanel.visibility = View.VISIBLE
        previewCard.visibility = View.VISIBLE
        btnOpenManager.visibility = View.GONE
        
        parseLyricsCache(track.syncedLyrics ?: "")
        updateWysiwygPreview()
        checkAndRequestAudioMatch(track)
    }

    private fun parseLyricsCache(syncedLyrics: String) {
        parsedLyricsCache.clear()
        val lines = syncedLyrics.split("\n")
        val regex = Regex("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)")
        for (line in lines) {
            val match = regex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msStr = match.groupValues[3]
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                val totalMs = min * 60 * 1000 + sec * 1000 + ms
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    parsedLyricsCache.add(Pair(totalMs, text))
                }
            }
        }
        
        if (parsedLyricsCache.isNotEmpty()) {
            val lastLyricMs = parsedLyricsCache.last().first
            val maxSecs = Math.max(0.1f, (lastLyricMs + 5000L) / 1000f)
            val defaultEnd = minOf(20f, maxSecs)
            
            if (maxSecs > rangeSlider.valueTo) {
                rangeSlider.valueTo = maxSecs
                rangeSlider.values = listOf(0f, defaultEnd)
            } else {
                rangeSlider.values = listOf(0f, defaultEnd)
                rangeSlider.valueTo = maxSecs
            }
            tvTrimTimes.text = String.format("%.1fs - %.1fs", 0f, defaultEnd)
            updateLyricPreviewLine(0f)
        }
    }

    private fun updateLyricPreviewLine(timeSecs: Float) {
        val targetMs = (timeSecs * 1000).toLong()
        var currentText = "..."
        for (i in parsedLyricsCache.indices) {
            if (targetMs >= parsedLyricsCache[i].first) {
                currentText = parsedLyricsCache[i].second
            } else {
                break
            }
        }
        tvLyricPreview.text = "Preview: \"$currentText\""
    }

    private fun updateWysiwygPreview() {
        val textToPreview = parsedLyricsCache.firstOrNull()?.second ?: "Odyssey"
        val rawBytes = GlyphFontEngine.renderTextFrame(textToPreview, currentFontStyle, 0f)
        
        val bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(625)
        for (i in 0 until 625) {
            val v = rawBytes[i].toInt() and 0xFF
            pixels[i] = Color.rgb(v, v, v)
        }
        bitmap.setPixels(pixels, 0, 25, 0, 0, 25, 25)
        
        previewImage.setImageBitmap(bitmap)
    }

    private fun checkAndRequestAudioMatch(track: LrcTrack) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestAudioPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                return
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestAudioPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                return
            }
        }
        attemptAutoMatchAudio(track)
    }

    private fun attemptAutoMatchAudio(track: LrcTrack) {
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST)
        
        // Simple heuristic: search for track name in TITLE
        val selection = "${MediaStore.Audio.Media.TITLE} LIKE ?"
        val selectionArgs = arrayOf("%${track.trackName}%")
        
        var foundUri: Uri? = null
        var foundName = ""
        
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val id = cursor.getLong(idIndex)
                foundName = cursor.getString(titleIndex)
                foundUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
            }
        }
        
        if (foundUri != null) {
            selectedAudioUri = foundUri
            updateAudioStatus("Found Local: $foundName", true)
        } else {
            updateAudioStatus("Audio: Unattached", false)
        }
    }

    private fun cancelProcessing() {
        isRendering.set(true)
        btnRender.visibility = View.VISIBLE
        btnCancel.visibility = View.GONE
        progressBar.visibility = View.GONE
        progressBar.setProgress(0)
        Snackbar.make(findViewById(android.R.id.content), "Render cancelled.", Snackbar.LENGTH_SHORT).show()
    }

    private fun renderLyrics() {
        val lyrics = selectedTrack?.syncedLyrics ?: return
        
        isRendering.set(false)
        progressBar.visibility = View.VISIBLE
        progressBar.setProgress(0)
        btnRender.visibility = View.GONE
        btnCancel.visibility = View.VISIBLE
        btnOpenManager.visibility = View.GONE
        
        val slot = prefs.getInt("selected_slot", 1)
        val startTimeMs = (rangeSlider.values[0] * 1000).toLong()
        val endTimeMs = (rangeSlider.values[1] * 1000).toLong()
        
        VideoProcessor.processLyrics(
            context = this,
            syncedLyrics = lyrics,
            audioUri = selectedAudioUri,
            fontStyle = currentFontStyle,
            animationStyle = currentAnimStyle,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            targetFps = 12,
            slotIndex = slot,
            isCancelled = isRendering,
            onProgress = { prog ->
                progressBar.setProgress(prog)
            },
            onComplete = { success, error ->
                btnRender.visibility = View.VISIBLE
                btnCancel.visibility = View.GONE
                progressBar.visibility = View.GONE
                
                if (success) {
                    Snackbar.make(findViewById(android.R.id.content), "Success! Saved to Slot $slot.", Snackbar.LENGTH_LONG).show()
                    btnOpenManager.visibility = View.VISIBLE
                    sendBroadcast(Intent("com.example.odysseyglyph.RELOAD_FRAMES"))
                } else {
                    if (error != "Cancelled by user.") {
                        Snackbar.make(findViewById(android.R.id.content), "Error: $error", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}
