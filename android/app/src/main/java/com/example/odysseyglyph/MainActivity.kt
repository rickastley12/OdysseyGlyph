package com.example.odysseyglyph

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.RangeSlider
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : ComponentActivity() {

    private lateinit var btnSelectVideo: MaterialButton
    private lateinit var settingsPanel: LinearLayout
    private lateinit var videoCard: MaterialCardView
    private lateinit var videoContainer: FrameLayout
    private lateinit var videoView: CenteredVideoView
    private lateinit var imageView: CenteredImageView
    private lateinit var cropOverlay: CropOverlayView
    
    private lateinit var rangeSlider: RangeSlider
    private lateinit var tvTrimTimes: TextView
    
    private lateinit var etFps: EditText
    private lateinit var modeSpinner: Spinner
    private lateinit var slotSpinner: Spinner
    private lateinit var cbInvert: MaterialSwitch
    private lateinit var contrastSlider: com.google.android.material.slider.Slider
    private lateinit var sharpenSwitch: MaterialSwitch
    private lateinit var btnProcess: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var btnOpenManager: MaterialButton

    private var selectedVideoUri: Uri? = null
    private var videoDurationMs = 0L
    private var videoRawWidth = 0
    private var videoRawHeight = 0
    private var currentMediaType = 0 // 0=video, 1=gif, 2=static

    private val loopHandler = Handler(Looper.getMainLooper())
    private val loopRunnable = object : Runnable {
        override fun run() {
            if (this@MainActivity::videoView.isInitialized && videoView.isPlaying && this@MainActivity::rangeSlider.isInitialized) {
                val endMs = rangeSlider.values[1].toInt()
                val startMs = rangeSlider.values[0].toInt()
                if (videoView.currentPosition >= endMs) {
                    videoView.seekTo(startMs)
                }
            }
            loopHandler.postDelayed(this, 30) // 30ms interval for smooth looping
        }
    }

    private val selectMediaLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            showSettingsForMedia(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure dark background
        window.decorView.setBackgroundColor(Color.parseColor("#121212"))

        // --- Device Compatibility Gate ---
        val prefs = getSharedPreferences("OdysseyGlyphPrefs", MODE_PRIVATE)
        val devBypass = prefs.getBoolean("dev_bypass", false)
        val model = android.os.Build.MODEL ?: ""
        val isSupportedDevice = model.contains("Phone (3)") || model.contains("Phone (4a) Pro") || model.contains("A024")
        
        if (!isSupportedDevice && !devBypass) {
            val errorLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(64, 64, 64, 64)
            }
            errorLayout.addView(TextView(this).apply {
                text = "Unsupported Device"
                textSize = 28f
                setTextColor(Color.parseColor("#FF5252"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 32)
            })
            errorLayout.addView(TextView(this).apply {
                text = "Odyssey Glyph requires the 25x25 Glyph Matrix found on the Nothing Phone (3) or Phone (4a) Pro.\n\nYour device model (${model}) is not supported."
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 64)
            })
            
            // Hidden developer override
            var taps = 0
            val btnBypass = MaterialButton(this).apply {
                text = "Exit"
                setOnClickListener {
                    taps++
                    if (taps >= 5) {
                        prefs.edit().putBoolean("dev_bypass", true).apply()
                        recreate()
                    } else if (taps == 1) {
                        finish()
                    }
                }
            }
            errorLayout.addView(btnBypass)
            setContentView(errorLayout)
            return
        }
        // ---------------------------------

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }
        scrollView.addView(layout)

        layout.addView(TextView(this).apply {
            text = "Odyssey Glyph"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        })

        tvStatus = TextView(this).apply {
            text = "Select a video to map to the Nothing Phone Glyph Matrix."
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 14f
            setPadding(0, 0, 0, 48)
        }
        layout.addView(tvStatus)

        btnSelectVideo = MaterialButton(this).apply {
            text = "Choose Media"
            setOnClickListener { selectMediaLauncher.launch(arrayOf("video/*", "image/*")) }
        }
        layout.addView(btnSelectVideo)

        // --- Settings Panel ---
        settingsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 32, 0, 24)
        }

        // Modern Video Preview Card
        videoCard = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                850
            )
            setCardBackgroundColor(Color.BLACK)
            radius = 32f // Rounded corners
            cardElevation = 8f
        }
        
        videoContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        videoView = CenteredVideoView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        videoContainer.addView(videoView)
        
        imageView = CenteredImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }
        videoContainer.addView(imageView)
        
        cropOverlay = CropOverlayView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        videoContainer.addView(cropOverlay)
        videoCard.addView(videoContainer)
        settingsPanel.addView(videoCard)
        
        // Instructional text
        settingsPanel.addView(TextView(this).apply {
            text = "Pinch to zoom. Drag to pan."
            textSize = 12f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 16, 0, 32)
        })
        
        // Custom Trimmer UI
        val trimmerCard = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setCardBackgroundColor(Color.parseColor("#1E1E1E"))
            radius = 24f
            setContentPadding(32, 32, 32, 32)
        }
        
        val trimmerLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        
        tvTrimTimes = TextView(this).apply {
            text = "Trim: 0.0s - 0.0s"
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        trimmerLayout.addView(tvTrimTimes)
        
        // Solid color timeline background
        val timelineBg = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120)
            setBackgroundColor(Color.parseColor("#333333"))
            
            rangeSlider = RangeSlider(context).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
                // Show the drag bubble in seconds, not raw milliseconds
                setLabelFormatter { value -> String.format("%.1fs", value / 1000f) }
                // Pause the looping preview while actively scrubbing so the seek
                // below is actually what shows on screen, instead of the loop
                // immediately playing over it
                addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(slider: RangeSlider) {
                        videoView.pause()
                    }
                    override fun onStopTrackingTouch(slider: RangeSlider) {
                        videoView.seekTo(slider.values[0].toInt())
                        videoView.start()
                    }
                })
                addOnChangeListener { slider, value, fromUser ->
                    val startMs = slider.values[0]
                    val endMs = slider.values[1]
                    tvTrimTimes.text = String.format("Trim: %.1fs - %.1fs", startMs / 1000f, endMs / 1000f)
                    // Seek to whichever handle is actually being dragged, not
                    // always the start handle
                    if (fromUser) {
                        videoView.seekTo(value.toInt())
                    }
                }
            }
            addView(rangeSlider)
        }
        trimmerLayout.addView(timelineBg)
        trimmerCard.addView(trimmerLayout)
        settingsPanel.addView(trimmerCard)

        // Settings row 1 (FPS, Mode, Invert)
        val settingsRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 48, 0, 8)
            gravity = Gravity.CENTER_VERTICAL
        }
        
        settingsRow1.addView(TextView(this).apply {
            text = "FPS:"
            setTextColor(Color.WHITE)
            setPadding(0, 0, 16, 0)
        })
        
        etFps = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("12")
            setTextColor(Color.WHITE)
            background = null
            layoutParams = LinearLayout.LayoutParams(100, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        settingsRow1.addView(etFps)
        
        modeSpinner = Spinner(this).apply {
            val modes = arrayOf("Once", "Loop", "Ping-Pong")
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, modes)
            this.adapter = adapter
            setSelection(1) // Default to Loop
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        settingsRow1.addView(modeSpinner)
        
        cbInvert = MaterialSwitch(this).apply {
            text = "Invert"
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(16, 0, 0, 0)
        }
        settingsRow1.addView(cbInvert)
        settingsPanel.addView(settingsRow1)

        // Settings row 2 (Slot Picker)
        val settingsRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 16)
            gravity = Gravity.CENTER_VERTICAL
        }
        
        settingsRow2.addView(TextView(this).apply {
            text = "Save to:"
            setTextColor(Color.WHITE)
            setPadding(0, 0, 16, 0)
        })

        slotSpinner = Spinner(this).apply {
            val slots = arrayOf("Slot 1", "Slot 2", "Slot 3")
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, slots)
            this.adapter = adapter
            setSelection(0)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        settingsRow2.addView(slotSpinner)
        settingsPanel.addView(settingsRow2)

        // --- Advanced Panel ---
        val advancedToggle = TextView(this).apply {
            text = "Advanced Options 🔽"
            setTextColor(Color.parseColor("#888888"))
            setPadding(0, 16, 0, 16)
        }
        val advancedContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 16, 0, 32)
        }
        
        advancedToggle.setOnClickListener {
            if (advancedContainer.visibility == View.GONE) {
                advancedContainer.visibility = View.VISIBLE
                advancedToggle.text = "Advanced Options 🔼"
            } else {
                advancedContainer.visibility = View.GONE
                advancedToggle.text = "Advanced Options 🔽"
            }
        }
        
        val contrastLabel = TextView(this).apply {
            text = "Contrast Multiplier: 1.0x"
            setTextColor(Color.WHITE)
        }
        advancedContainer.addView(contrastLabel)
        
        contrastSlider = com.google.android.material.slider.Slider(this).apply {
            valueFrom = 0.5f
            valueTo = 3.0f
            value = 1.0f
            stepSize = 0.1f
            addOnChangeListener { _, value, _ ->
                contrastLabel.text = String.format("Contrast Multiplier: %.1fx", value)
            }
        }
        advancedContainer.addView(contrastSlider)
        
        sharpenSwitch = MaterialSwitch(this).apply {
            text = "Sharpen Image (Enhances faces/edges)"
            setTextColor(Color.WHITE)
            isChecked = true
        }
        advancedContainer.addView(sharpenSwitch)
        
        settingsPanel.addView(advancedToggle)
        settingsPanel.addView(advancedContainer)
        // ----------------------

        btnProcess = MaterialButton(this).apply {
            text = "RENDER TO GLYPH"
            setPadding(0, 24, 0, 24)
            textSize = 16f
            setOnClickListener { startProcessing() }
        }
        settingsPanel.addView(btnProcess)

        layout.addView(settingsPanel)
        // ----------------------

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
            setPadding(0, 48, 0, 48)
        }
        layout.addView(progressBar)

        btnOpenManager = MaterialButton(this).apply {
            text = "Open Toys Manager"
            visibility = View.GONE
            setOnClickListener {
                val intent = Intent()
                intent.component = ComponentName(
                    "com.nothing.thirdparty",
                    "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
                )
                startActivity(intent)
            }
        }
        layout.addView(btnOpenManager)

        if (java.io.File(filesDir, "frames.bin").exists()) {
            btnOpenManager.visibility = View.VISIBLE
        }

        setContentView(scrollView)
    }
    
    private fun showSettingsForMedia(uri: Uri) {
        try {
            val mimeType = contentResolver.getType(uri) ?: ""
            currentMediaType = when {
                mimeType == "image/gif" -> 1
                mimeType.startsWith("image/") -> 2
                else -> 0
            }
            
            var mediaWidth = 1
            var mediaHeight = 1
            
            if (currentMediaType == 0) {
                // Video
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(this, uri)
                    videoDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                    val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                    
                    if (rotation == 90 || rotation == 270) {
                        mediaWidth = h
                        mediaHeight = w
                    } else {
                        mediaWidth = w
                        mediaHeight = h
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }
            } else if (currentMediaType == 1) {
                // GIF
                try {
                    val stream = contentResolver.openInputStream(uri)
                    val movie = android.graphics.Movie.decodeStream(stream)
                    if (movie != null) {
                        videoDurationMs = movie.duration().toLong()
                        mediaWidth = movie.width()
                        mediaHeight = movie.height()
                    }
                    stream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // Static Image
                try {
                    val stream = contentResolver.openInputStream(uri)
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    android.graphics.BitmapFactory.decodeStream(stream, null, options)
                    mediaWidth = options.outWidth
                    mediaHeight = options.outHeight
                    videoDurationMs = 0
                    stream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            videoRawWidth = mediaWidth
            videoRawHeight = mediaHeight

            if (currentMediaType == 2 || videoDurationMs <= 0) {
                rangeSlider.isEnabled = false
                tvTrimTimes.text = "Trim: N/A (Static Image)"
            } else {
                rangeSlider.isEnabled = true
                val maxDur = videoDurationMs.toFloat()
                rangeSlider.valueFrom = 0f
                if (maxDur < rangeSlider.valueTo) {
                    rangeSlider.values = listOf(0f, maxDur)
                    rangeSlider.valueTo = maxDur
                } else {
                    rangeSlider.valueTo = maxDur
                    rangeSlider.values = listOf(0f, maxDur)
                }
                tvTrimTimes.text = String.format("Trim: 0.0s - %.1fs", videoDurationMs / 1000f)
            }

        if (mediaWidth > 0 && mediaHeight > 0) {
            val availableWidth = resources.displayMetrics.widthPixels - 96
            var targetHeight = (availableWidth.toFloat() * mediaHeight / mediaWidth).toInt()
            val maxHeight = (resources.displayMetrics.heightPixels * 0.65f).toInt()
            if (targetHeight > maxHeight) targetHeight = maxHeight
            
            videoCard.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                targetHeight
            )
        }

        // UX Improvement: Allow user to change media if they selected the wrong one
        btnSelectVideo.text = "Change Media"
        btnSelectVideo.visibility = View.VISIBLE
        
        btnOpenManager.visibility = View.GONE
        settingsPanel.visibility = View.VISIBLE
        tvStatus.text = "Pinch to zoom and align the media inside the circle."
        
        if (currentMediaType == 0) {
            imageView.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            videoView.setVideoURI(uri)
            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                videoView.start()
                loopHandler.post(loopRunnable)
                
                showFirstRunTutorial()
            }
        } else {
            videoView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            imageView.setImageURIWithAnim(uri)
            showFirstRunTutorial()
        }
        
        } catch (e: Throwable) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            tvStatus.text = "Crash prevented. Error: ${e.javaClass.simpleName}: ${e.message}"
        }
    }
    
    private fun showFirstRunTutorial() {
        val prefs = getSharedPreferences("OdysseyGlyphPrefs", MODE_PRIVATE)
        if (prefs.getBoolean("first_run", true)) {
            Toast.makeText(this, "Tutorial: Pinch to zoom. Drag to pan. Use sliders to trim.", Toast.LENGTH_LONG).show()
            prefs.edit().putBoolean("first_run", false).apply()
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop the preview instead of leaving it playing (and holding the
        // decoder/audio pipeline) once the app is no longer in the foreground
        if (this::videoView.isInitialized) {
            videoView.pause()
        }
        loopHandler.removeCallbacks(loopRunnable)
    }

    override fun onResume() {
        super.onResume()
        if (this::videoView.isInitialized && settingsPanel.visibility == View.VISIBLE) {
            videoView.start()
            loopHandler.post(loopRunnable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loopHandler.removeCallbacks(loopRunnable)
        if (this::videoView.isInitialized) {
            videoView.release()
        }
    }

    private fun startProcessing() {
        val uri = selectedVideoUri ?: return

        val fps = etFps.text.toString().toIntOrNull() ?: 12
        val invert = cbInvert.isChecked
        val startMs = rangeSlider.values[0].toLong()
        val endMs = rangeSlider.values[1].toLong()
        val playbackMode = if (this::modeSpinner.isInitialized) modeSpinner.selectedItemPosition else 1
        
        val contrastMulti = if (this::contrastSlider.isInitialized) contrastSlider.value else 1.0f
        val sharpen = if (this::sharpenSwitch.isInitialized) sharpenSwitch.isChecked else true
        val slotIndex = if (this::slotSpinner.isInitialized) slotSpinner.selectedItemPosition + 1 else 1
        
        // Use the ZoomSurfaceView/ZoomImageView engine matrix
        // Since we call setContentSize with the raw video dimensions,
        // the engine matrix already maps directly from screen layout coordinates
        // back to the original media pixels!
        val inverseMatrix = android.graphics.Matrix()
        if (currentMediaType == 0) {
            videoView.engine.matrix.invert(inverseMatrix)
            videoView.pause()
        } else {
            imageView.engine.matrix.invert(inverseMatrix)
        }
        settingsPanel.visibility = View.GONE
        btnSelectVideo.visibility = View.GONE // Hide during processing to prevent overlapping jobs
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        tvStatus.text = "Rendering matrix frames... Please wait."

        VideoProcessor.processMedia(
            context = this, 
            mediaUri = uri,
            mediaType = currentMediaType,
            startTimeMs = startMs,
            endTimeMs = endMs,
            targetFps = fps,
            playbackMode = playbackMode,
            invertColors = invert,
            contrastMulti = contrastMulti,
            sharpen = sharpen,
            cropCx = cropOverlay.circleX,
            cropCy = cropOverlay.circleY,
            cropRadius = cropOverlay.circleRadius,
            inverseTransform = inverseMatrix, // New parameter for VideoProcessor
            slotIndex = slotIndex,
            onProgress = { progress ->
                progressBar.progress = progress
            },
            onComplete = { success, errorMsg ->
                btnSelectVideo.visibility = View.VISIBLE
                btnSelectVideo.text = "Create Another"
                progressBar.visibility = View.GONE
                
                if (success) {
                    tvStatus.text = "Success! Your animation is ready."
                    btnOpenManager.visibility = View.VISIBLE
                    sendBroadcast(Intent("com.example.odysseyglyph.RELOAD_FRAMES"))
                } else {
                    tvStatus.text = "Failed: ${errorMsg ?: "Unknown error"}"
                    Toast.makeText(this, "Processing failed: ${errorMsg ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}
