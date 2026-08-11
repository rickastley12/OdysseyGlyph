package com.example.odysseyglyph

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    
    // UI Elements
    private lateinit var btnSelectVideo: MaterialButton
    private lateinit var videoContainer: FrameLayout
    private lateinit var videoView: CenteredVideoView
    private lateinit var imageView: CenteredImageView
    private lateinit var cropOverlay: CropOverlayView
    private lateinit var settingsPanel: LinearLayout
    
    private lateinit var trimmerCard: LinearLayout
    private lateinit var rangeSlider: RangeSlider
    private lateinit var tvTrimTimes: TextView
    
    private lateinit var modeSpinner: AutoCompleteTextView
    private lateinit var slotSpinner: AutoCompleteTextView
    
    private lateinit var audioCard: LinearLayout
    private lateinit var tvAudioStatus: TextView
    private lateinit var btnAttachAudio: MaterialButton
    private lateinit var toolbar: MaterialToolbar
    
    private lateinit var btnAdvancedToggle: LinearLayout
    
    private lateinit var btnProcess: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressBar: GlyphProgressView
    private lateinit var btnOpenManager: MaterialButton
    private lateinit var btnExport: MaterialButton
    private lateinit var btnImport: MaterialButton
    private lateinit var btnLaunchLyricStudio: MaterialButton
    private lateinit var btnLaunchLiveLyrics: MaterialButton
    private lateinit var bottomActionBar: LinearLayout

    private var selectedMediaUri: Uri? = null
    private var currentMediaType = 0 // 0=video, 1=gif, 2=static
    private var selectedAudioUri: Uri? = null
    private var videoDurationMs: Long = 0
    private var isRendering = AtomicBoolean(false)
    private var systemBarsBottom = 0

    // Advanced Settings state
    private var advFps = 12
    private var advInvert = false
    private var advSharpen = true
    private var advContrast = 1.0f
    private var advBrightness = 100.0f
    private var advDuration = 5.0f

    private val selectMediaLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val mimeType = contentResolver.getType(it) ?: ""
            when {
                mimeType.startsWith("image/gif") -> {
                    currentMediaType = 1
                    setupGif(it)
                }
                mimeType.startsWith("image/") -> {
                    currentMediaType = 2
                    setupStaticImage(it)
                }
                else -> {
                    currentMediaType = 0
                    setupVideo(it)
                }
            }
            settingsPanel.visibility = View.VISIBLE
            bottomActionBar.visibility = View.VISIBLE
            showCoachmarks()
        }
    }

    private val selectAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            selectedAudioUri = uri
            updateAudioStatus("AUDIO: CUSTOM FILE", true)
        }
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            try {
                val slot = prefs.getInt("selected_slot", 1)
                val src = java.io.File(filesDir, "frames_slot$slot.bin")
                if (!src.exists()) {
                    Snackbar.make(findViewById(android.R.id.content), "No preset found in Slot $slot to export!", Snackbar.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }
                contentResolver.openOutputStream(uri)?.use { out ->
                    src.inputStream().use { it.copyTo(out) }
                }
                Snackbar.make(findViewById(android.R.id.content), "Preset Exported successfully!", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val slot = prefs.getInt("selected_slot", 1)
                val dest = java.io.File(filesDir, "frames_slot$slot.bin")
                contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                }
                Snackbar.make(findViewById(android.R.id.content), "Preset Imported to Slot $slot!", Snackbar.LENGTH_SHORT).show()
                btnOpenManager.visibility = View.VISIBLE
                sendBroadcast(Intent("com.example.odysseyglyph.RELOAD_FRAMES"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        
        prefs = getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE)

        loadAdvancedSettings()
        bindViews()
        
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            systemBarsBottom = systemBars.bottom
            updateScrollViewPadding()
            insets
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(bottomActionBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val basePadding = resources.getDimensionPixelSize(R.dimen.spacing_medium)
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, basePadding + systemBars.bottom)
            insets
        }
        
        bottomActionBar.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if ((bottom - top) != (oldBottom - oldTop)) {
                updateScrollViewPadding()
            }
        }

        setupListeners()
        syncSlotState()
    }

    private fun updateScrollViewPadding() {
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val paddingBottom = if (bottomActionBar.visibility == View.VISIBLE && bottomActionBar.height > 0) {
            bottomActionBar.height
        } else {
            systemBarsBottom
        }
        scrollView.setPadding(scrollView.paddingLeft, scrollView.paddingTop, scrollView.paddingRight, paddingBottom)
    }

    override fun onResume() {
        super.onResume()
        syncSlotState()
        checkToysManagerVisibility()
        if (currentMediaType == 0) {
            videoView.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (currentMediaType == 0) {
            videoView.pause()
        }
    }

    private fun loadAdvancedSettings() {
        advFps = prefs.getInt("adv_fps", 12)
        advInvert = prefs.getBoolean("adv_invert", false)
        advSharpen = prefs.getBoolean("adv_sharpen", true)
        advContrast = prefs.getFloat("adv_contrast", 1.0f)
        advBrightness = prefs.getFloat("adv_brightness", 100.0f)
        advDuration = prefs.getFloat("adv_duration", 5.0f)
    }

    private fun saveAdvancedSettings() {
        prefs.edit()
            .putInt("adv_fps", advFps)
            .putBoolean("adv_invert", advInvert)
            .putBoolean("adv_sharpen", advSharpen)
            .putFloat("adv_contrast", advContrast)
            .putFloat("adv_brightness", advBrightness)
            .putFloat("adv_duration", advDuration)
            .apply()
    }

    private fun bindViews() {
        btnSelectVideo = findViewById(R.id.btnSelectVideo)
        btnLaunchLyricStudio = findViewById(R.id.btnLaunchLyricStudio)
        btnLaunchLiveLyrics = findViewById(R.id.btnLaunchLiveLyrics)
        toolbar = findViewById(R.id.toolbar)
        videoContainer = findViewById(R.id.videoContainer)
        videoView = findViewById(R.id.videoView)
        imageView = findViewById(R.id.imageView)
        cropOverlay = findViewById(R.id.cropOverlay)
        settingsPanel = findViewById(R.id.settingsPanel)
        
        trimmerCard = findViewById(R.id.trimmerCard)
        rangeSlider = findViewById(R.id.rangeSlider)
        tvTrimTimes = findViewById(R.id.tvTrimTimes)
        
        modeSpinner = findViewById(R.id.modeSpinner)
        slotSpinner = findViewById(R.id.slotSpinner)
        
        audioCard = findViewById(R.id.audioCard)
        tvAudioStatus = findViewById(R.id.tvAudioStatus)
        btnAttachAudio = findViewById(R.id.btnAttachAudio)
        
        btnAdvancedToggle = findViewById(R.id.btnAdvancedToggle)
        
        btnProcess = findViewById(R.id.btnProcess)
        btnCancel = findViewById(R.id.btnCancel)
        progressBar = findViewById(R.id.progressBar)
        btnOpenManager = findViewById(R.id.btnOpenManager)
        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)
        bottomActionBar = findViewById(R.id.bottomActionBar)
    }

    private fun setupListeners() {
        btnSelectVideo.setOnClickListener {
            selectMediaLauncher.launch(arrayOf("video/*", "image/*"))
        }

        btnLaunchLyricStudio.setOnClickListener {
            startActivity(Intent(this, LyricStudioActivity::class.java))
        }

        btnLaunchLiveLyrics.setOnClickListener {
            startActivity(Intent(this, Class.forName("com.example.odysseyglyph.LiveLyricsActivity")))
        }

        btnAdvancedToggle.setOnClickListener {
            showAdvancedSettingsBottomSheet()
        }

        btnAttachAudio.setOnClickListener { selectAudioLauncher.launch(arrayOf("audio/*")) }

        rangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            tvTrimTimes.text = String.format("TRIM: %.1fs - %.1fs", values[0], values[1])
        }

        btnProcess.setOnClickListener { startProcessing() }
        btnCancel.setOnClickListener { cancelProcessing() }

        btnOpenManager.setOnClickListener {
            try {
                val intent = Intent()
                intent.component = ComponentName("com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity")
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(findViewById(android.R.id.content), "Nothing OS Toys Manager not found.", Snackbar.LENGTH_LONG).show()
            }
        }
        
        btnExport.setOnClickListener {
            val slot = prefs.getInt("selected_slot", 1)
            exportLauncher.launch("slot${slot}_preset.odyssey")
        }
        btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
        }

        slotSpinner.setOnItemClickListener { _, _, position, _ ->
            prefs.edit().putInt("selected_slot", position + 1).apply()
        }
    }

    private fun showAdvancedSettingsBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_advanced_settings, null)
        dialog.setContentView(view)

        val etFps = view.findViewById<TextInputEditText>(R.id.etFps)
        val cbInvert = view.findViewById<MaterialSwitch>(R.id.cbInvert)
        val sharpenSwitch = view.findViewById<MaterialSwitch>(R.id.sharpenSwitch)
        val contrastSlider = view.findViewById<Slider>(R.id.contrastSlider)
        val contrastLabel = view.findViewById<TextView>(R.id.contrastLabel)
        val brightnessSlider = view.findViewById<Slider>(R.id.brightnessSlider)
        val brightnessLabel = view.findViewById<TextView>(R.id.brightnessLabel)
        val durationSlider = view.findViewById<Slider>(R.id.durationSlider)
        val durationLabel = view.findViewById<TextView>(R.id.durationLabel)

        if (currentMediaType == 2) {
            durationSlider.visibility = View.VISIBLE
            durationLabel.visibility = View.VISIBLE
        } else {
            durationSlider.visibility = View.GONE
            durationLabel.visibility = View.GONE
        }

        etFps.setText(advFps.toString())
        cbInvert.isChecked = advInvert
        sharpenSwitch.isChecked = advSharpen
        contrastSlider.value = advContrast
        brightnessSlider.value = advBrightness
        durationSlider.value = advDuration

        contrastLabel.text = String.format("CONTRAST MULTIPLIER: %.1fx", advContrast)
        brightnessLabel.text = String.format("LED BRIGHTNESS: %.0f%%", advBrightness)
        durationLabel.text = String.format("IMAGE DURATION: %.0fs", advDuration)

        contrastSlider.addOnChangeListener { _, value, _ ->
            advContrast = value
            contrastLabel.text = String.format("CONTRAST MULTIPLIER: %.1fx", value)
        }
        brightnessSlider.addOnChangeListener { _, value, _ ->
            advBrightness = value
            brightnessLabel.text = String.format("LED BRIGHTNESS: %.0f%%", value)
        }
        durationSlider.addOnChangeListener { _, value, _ ->
            advDuration = value
            durationLabel.text = String.format("IMAGE DURATION: %.0fs", value)
        }
        cbInvert.setOnCheckedChangeListener { _, isChecked -> advInvert = isChecked }
        sharpenSwitch.setOnCheckedChangeListener { _, isChecked -> advSharpen = isChecked }

        dialog.setOnDismissListener {
            advFps = etFps.text.toString().toIntOrNull() ?: 12
            saveAdvancedSettings()
        }

        dialog.show()
    }

    private fun syncSlotState() {
        val slot = prefs.getInt("selected_slot", 1)
        val slotText = "Slot $slot"
        slotSpinner.setText(slotText, false)
    }

    private fun checkToysManagerVisibility() {
        val slot = prefs.getInt("selected_slot", 1)
        val file = java.io.File(filesDir, "frames_slot$slot.bin")
        if (file.exists()) {
            btnOpenManager.visibility = View.VISIBLE
        } else {
            btnOpenManager.visibility = View.GONE
        }
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

    private fun setupVideo(uri: Uri) {
        imageView.visibility = View.GONE
        videoView.visibility = View.VISIBLE
        trimmerCard.visibility = View.VISIBLE

        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoDurationMs = mp.duration.toLong()
            mp.start()
            
            val maxSecs = Math.max(0.1f, videoDurationMs / 1000f)
            if (maxSecs > rangeSlider.valueTo) {
                rangeSlider.valueTo = maxSecs
                rangeSlider.values = listOf(0f, maxSecs)
            } else {
                rangeSlider.values = listOf(0f, maxSecs)
                rangeSlider.valueTo = maxSecs
            }
            tvTrimTimes.text = String.format("TRIM: 0.0s - %.1fs", maxSecs)
        }
    }

    private fun setupGif(uri: Uri) {
        videoView.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        trimmerCard.visibility = View.VISIBLE

        imageView.setImageURIWithAnim(uri)
        rangeSlider.valueFrom = 0f
        rangeSlider.valueTo = 10f
        rangeSlider.values = listOf(0f, 10f)
        tvTrimTimes.text = "TRIM: 0.0s - 10.0s"
    }

    private fun setupStaticImage(uri: Uri) {
        videoView.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        trimmerCard.visibility = View.GONE

        imageView.setImageURIWithAnim(uri)
    }
    
    private fun showCoachmarks() {
        if (prefs.getBoolean("first_run_coachmark", true)) {
            val typeStr = if (currentMediaType == 2) "image" else "video"
            Snackbar.make(findViewById(android.R.id.content), "Pinch and drag the $typeStr to fit inside the circle.", Snackbar.LENGTH_LONG)
                .setAction("Got it") {
                    prefs.edit().putBoolean("first_run_coachmark", false).apply()
                }.show()
        }
    }

    private fun cancelProcessing() {
        isRendering.set(true)
        btnProcess.visibility = View.VISIBLE
        btnCancel.visibility = View.GONE
        progressBar.visibility = View.GONE
        progressBar.setProgress(0)
        Snackbar.make(findViewById(android.R.id.content), "Render cancelled.", Snackbar.LENGTH_SHORT).show()
    }

    private fun startProcessing() {
        val uri = selectedMediaUri
        if (uri == null) {
            Snackbar.make(findViewById(android.R.id.content), "Please select a media file first.", Snackbar.LENGTH_SHORT).show()
            return
        }

        isRendering.set(false)
        btnProcess.visibility = View.GONE
        btnCancel.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        progressBar.setProgress(0)
        btnOpenManager.visibility = View.GONE

        val startTimeMs = if (currentMediaType == 2) 0L else (rangeSlider.values[0] * 1000).toLong()
        val endTimeMs = if (currentMediaType == 2) 0L else (rangeSlider.values[1] * 1000).toLong()

        val fps = advFps
        val invert = advInvert
        val mode = if (modeSpinner.text.toString() == "LOOP") 1 else 0
        val slot = prefs.getInt("selected_slot", 1)

        val imageDurationSec = advDuration.toInt()
        val contrastMulti = advContrast
        val brightnessMulti = advBrightness / 100f
        val sharpen = advSharpen

        val inverseTransform = Matrix()
        if (currentMediaType == 0) {
            videoView.engine.matrix.invert(inverseTransform)
            videoView.pause()
        } else {
            imageView.engine.matrix.invert(inverseTransform)
            
            try {
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { 
                    android.graphics.BitmapFactory.decodeStream(it, null, opts) 
                }
                val rawW = opts.outWidth
                val rawH = opts.outHeight
                val drawable = imageView.drawable
                if (drawable != null && rawW > 0 && drawable.intrinsicWidth > 0) {
                    val scaleMatrix = Matrix()
                    scaleMatrix.setScale(rawW.toFloat() / drawable.intrinsicWidth, rawH.toFloat() / drawable.intrinsicHeight)
                    inverseTransform.postConcat(scaleMatrix)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        VideoProcessor.processMedia(
            context = this,
            mediaUri = uri,
            audioUri = selectedAudioUri,
            mediaType = currentMediaType,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            targetFps = fps,
            playbackMode = mode,
            invertColors = invert,
            contrastMulti = contrastMulti,
            brightnessMulti = brightnessMulti,
            imageDurationSec = imageDurationSec,
            sharpen = sharpen,
            cropCx = cropOverlay.circleX,
            cropCy = cropOverlay.circleY,
            cropRadius = cropOverlay.circleRadius,
            inverseTransform = inverseTransform,
            slotIndex = slot,
            isCancelled = isRendering,
            onProgress = { prog ->
                progressBar.setProgress(prog)
            },
            onComplete = { success, error ->
                btnProcess.visibility = View.VISIBLE
                btnCancel.visibility = View.GONE
                progressBar.visibility = View.GONE

                if (success) {
                    Snackbar.make(findViewById(android.R.id.content), "SUCCESS! RENDERED TO SLOT $slot.", Snackbar.LENGTH_LONG).show()
                    btnOpenManager.visibility = View.VISIBLE
                    sendBroadcast(Intent("com.example.odysseyglyph.RELOAD_FRAMES"))
                } else {
                    if (error != "Cancelled by user.") {
                        Snackbar.make(findViewById(android.R.id.content), "ERROR: $error", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}
