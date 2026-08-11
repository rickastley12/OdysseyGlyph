package com.example.odysseyglyph

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlin.concurrent.thread

class LyricStudioActivity : ComponentActivity() {

    private lateinit var etSearch: EditText
    private lateinit var resultsContainer: LinearLayout
    private lateinit var previewText: TextView
    
    private lateinit var btnStylePixel: MaterialButton
    private lateinit var btnStyleBold: MaterialButton
    private lateinit var btnStyleSmooth: MaterialButton
    
    private lateinit var btnAnimFlash: MaterialButton
    private lateinit var btnAnimScroll: MaterialButton
    
    private lateinit var btnAttachAudio: MaterialButton
    private lateinit var btnRender: MaterialButton
    
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    
    private var selectedAudioUri: Uri? = null
    private var selectedTrack: LrcTrack? = null
    private var currentFontStyle = GlyphFontEngine.FontStyle.SMOOTH
    private var currentAnimStyle = 0 // 0=Flash, 1=Scroll
    
    private val selectAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            selectedAudioUri = uri
            btnAttachAudio.text = "Audio Attached ✓"
            btnAttachAudio.setBackgroundColor(Color.parseColor("#4CAF50"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Instagram-style gradient background
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#1E1528"), Color.parseColor("#0F0C1B"))
        )
        window.decorView.background = gradient

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        // Header
        rootLayout.addView(TextView(this).apply {
            text = "Lyric Studio"
            textSize = 32f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        })
        rootLayout.addView(TextView(this).apply {
            text = "Create hologram visuals perfectly synced to music."
            setTextColor(Color.parseColor("#BBBBBB"))
            textSize = 14f
            setPadding(0, 0, 0, 32)
        })

        // Search Bar
        etSearch = EditText(this).apply {
            hint = "Search for a song..."
            setHintTextColor(Color.parseColor("#777777"))
            setTextColor(Color.WHITE)
            setPadding(48, 32, 48, 32)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2A2235"))
                cornerRadius = 48f
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s.toString().length > 2) {
                        performSearch(s.toString())
                    }
                }
            })
        }
        rootLayout.addView(etSearch)
        
        // Results Area
        val scrollResults = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = 32
            }
        }
        resultsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollResults.addView(resultsContainer)
        rootLayout.addView(scrollResults)

        // Typography Selector (Instagram Style)
        val styleLabel = TextView(this).apply {
            text = "Aa Typography"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 32, 0, 16)
        }
        rootLayout.addView(styleLabel)
        
        val styleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        
        btnStylePixel = createStyleButton("PIXEL").apply { 
            setOnClickListener { setFontStyle(GlyphFontEngine.FontStyle.PIXEL_TINY, this) } 
        }
        btnStyleBold = createStyleButton("BOLD").apply { 
            setOnClickListener { setFontStyle(GlyphFontEngine.FontStyle.BLOCK_BOLD, this) } 
        }
        btnStyleSmooth = createStyleButton("SMOOTH").apply { 
            setOnClickListener { setFontStyle(GlyphFontEngine.FontStyle.SMOOTH, this) } 
        }
        
        styleRow.addView(btnStylePixel)
        styleRow.addView(btnStyleBold)
        styleRow.addView(btnStyleSmooth)
        rootLayout.addView(styleRow)
        
        setFontStyle(GlyphFontEngine.FontStyle.SMOOTH, btnStyleSmooth) // Default

        // Animation Selector
        val animRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 32, 0, 32)
        }
        
        btnAnimFlash = createStyleButton("⚡ FLASH").apply {
            setOnClickListener { setAnimStyle(0, this) }
        }
        btnAnimScroll = createStyleButton("⟵ SCROLL").apply {
            setOnClickListener { setAnimStyle(1, this) }
        }
        
        animRow.addView(btnAnimFlash)
        animRow.addView(btnAnimScroll)
        rootLayout.addView(animRow)
        
        setAnimStyle(0, btnAnimFlash) // Default

        // Attach Audio
        btnAttachAudio = MaterialButton(this).apply {
            text = "Attach MP3 Track"
            setBackgroundColor(Color.parseColor("#333333"))
            setOnClickListener { selectAudioLauncher.launch(arrayOf("audio/*")) }
        }
        rootLayout.addView(btnAttachAudio)

        // Render Button
        btnRender = MaterialButton(this).apply {
            text = "RENDER LYRIC HOLOGRAM"
            setPadding(0, 32, 0, 32)
            textSize = 16f
            setBackgroundColor(Color.parseColor("#E91E63")) // Vibrant pink
            isEnabled = false // Disabled until song selected
            setOnClickListener { renderLyrics() }
        }
        rootLayout.addView(btnRender)
        
        tvStatus = TextView(this).apply {
            text = ""
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }
        rootLayout.addView(tvStatus)
        
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
            setPadding(0, 16, 0, 0)
        }
        rootLayout.addView(progressBar)

        setContentView(rootLayout)
    }
    
    private fun createStyleButton(label: String): MaterialButton {
        return MaterialButton(this).apply {
            text = label
            cornerRadius = 32
            setBackgroundColor(Color.parseColor("#222222"))
            setTextColor(Color.parseColor("#888888"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(8, 0, 8, 0)
            }
        }
    }
    
    private fun setFontStyle(style: GlyphFontEngine.FontStyle, activeBtn: MaterialButton) {
        currentFontStyle = style
        btnStylePixel.apply { setBackgroundColor(Color.parseColor("#222222")); setTextColor(Color.parseColor("#888888")) }
        btnStyleBold.apply { setBackgroundColor(Color.parseColor("#222222")); setTextColor(Color.parseColor("#888888")) }
        btnStyleSmooth.apply { setBackgroundColor(Color.parseColor("#222222")); setTextColor(Color.parseColor("#888888")) }
        
        activeBtn.apply { setBackgroundColor(Color.WHITE); setTextColor(Color.BLACK) }
    }
    
    private fun setAnimStyle(style: Int, activeBtn: MaterialButton) {
        currentAnimStyle = style
        btnAnimFlash.apply { setBackgroundColor(Color.parseColor("#222222")); setTextColor(Color.parseColor("#888888")) }
        btnAnimScroll.apply { setBackgroundColor(Color.parseColor("#222222")); setTextColor(Color.parseColor("#888888")) }
        
        activeBtn.apply { setBackgroundColor(Color.WHITE); setTextColor(Color.BLACK) }
    }
    
    private fun performSearch(query: String) {
        resultsContainer.removeAllViews()
        val loadingText = TextView(this).apply {
            text = "Searching..."
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }
        resultsContainer.addView(loadingText)
        
        thread {
            val results = LRCLibClient.searchLyrics(query)
            
            runOnUiThread {
                resultsContainer.removeAllViews()
                if (results.isEmpty()) {
                    resultsContainer.addView(TextView(this).apply {
                        text = "No synced lyrics found."
                        setTextColor(Color.parseColor("#FF5252"))
                        gravity = Gravity.CENTER
                    })
                    return@runOnUiThread
                }
                
                for (track in results) {
                    if (track.syncedLyrics == null) continue // Only show synced
                    
                    val card = MaterialCardView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, 16)
                        }
                        setCardBackgroundColor(Color.parseColor("#2A2235"))
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
                            setTextColor(Color.parseColor("#AAAAAA"))
                            textSize = 12f
                        })
                        
                        addView(inner)
                        
                        setOnClickListener {
                            selectedTrack = track
                            etSearch.setText(track.trackName)
                            resultsContainer.removeAllViews()
                            btnRender.isEnabled = true
                            tvStatus.text = "Selected: ${track.trackName}"
                        }
                    }
                    resultsContainer.addView(card)
                }
            }
        }
    }
    
    private fun renderLyrics() {
        val track = selectedTrack ?: return
        val lyrics = track.syncedLyrics ?: return
        
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Rendering hologram matrices..."
        btnRender.isEnabled = false
        
        VideoProcessor.processLyrics(
            context = this,
            syncedLyrics = lyrics,
            audioUri = selectedAudioUri,
            fontStyle = currentFontStyle,
            animationStyle = currentAnimStyle,
            targetFps = 12,
            slotIndex = 3, // Hardcode to slot 3 for Lyric Studio for now, or add a spinner
            onProgress = { prog ->
                progressBar.progress = prog
            },
            onComplete = { success, error ->
                btnRender.isEnabled = true
                progressBar.visibility = View.GONE
                if (success) {
                    tvStatus.text = "Success! Saved to Slot 3."
                    tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                    
                    val intent = Intent()
                    intent.component = ComponentName(
                        "com.nothing.thirdparty",
                        "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
                    )
                    startActivity(intent)
                    sendBroadcast(Intent("com.example.odysseyglyph.RELOAD_FRAMES"))
                } else {
                    tvStatus.text = "Error: $error"
                    tvStatus.setTextColor(Color.parseColor("#FF5252"))
                }
            }
        )
    }
}
