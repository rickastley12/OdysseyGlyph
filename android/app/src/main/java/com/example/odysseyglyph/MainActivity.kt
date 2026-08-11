package com.example.odysseyglyph

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
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
    
    private lateinit var trimmerCard: MaterialCardView
    private lateinit var rangeSlider: RangeSlider
    private lateinit var tvTrimTimes: TextView
    
    private lateinit var modeSpinner: AutoCompleteTextView
    private lateinit var slotSpinner: AutoCompleteTextView
    
    private lateinit var audioCard: MaterialCardView
    private lateinit var tvAudioStatus: TextView
    private lateinit var btnAttachAudio: MaterialButton
    private lateinit var btnDrmInfo: ImageButton
    
    private lateinit var btnAdvancedToggle: LinearLayout
    private lateinit var advancedContainer: LinearLayout
    private lateinit var icAdvancedChevron: ImageView
    private lateinit var etFps: TextInputEditText
    private lateinit var cbInvert: MaterialSwitch
    private lateinit var sharpenSwitch: MaterialSwitch
    private lateinit var contrastSlider: Slider
    private lateinit var contrastLabel: TextView
    private lateinit var brightnessSlider: Slider
    private lateinit var brightnessLabel: TextView
    private lateinit var durationSlider: Slider
    private lateinit var durationLabel: TextView
    
    private lateinit var btnProcess: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnOpenManager: MaterialButton
    private lateinit var btnExport: MaterialButton
    private lateinit var btnImport: MaterialButton
    private lateinit var btnLaunchLyricStudio: MaterialButton

    private var selectedMediaUri: Uri? = null
    private var currentMediaType = 0 // 0=video, 1=gif, 2=static
    private var selectedAudioUri: Uri? = null
    private var videoDurationMs: Long = 0
    private var isRendering = AtomicBoolean(false)
    private var isAdvancedExpanded = false

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
            showCoachmarks()
        }
    }

    private val selectAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            selectedAudioUri = uri
            updateAudioStatus("Attached: Custom Audio", true)
        }
    }

    private val requestAudioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission granted
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Audio permission denied.", Snackbar.LENGTH_LONG).show()
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
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE)

        bindViews()
        setupListeners()
        syncSlotState()
    }

    override fun onResume() {
        super.onResume()
        syncSlotState()
        checkToysManagerVisibility()
    }

    private fun bindViews() {
        btnSelectVideo = findViewById(R.id.btnSelectVideo)
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
        btnDrmInfo = findViewById(R.id.btnDrmInfo)
        
        btnAdvancedToggle = findViewById(R.id.btnAdvancedToggle)
        advancedContainer = findViewById(R.id.advancedContainer)
        icAdvancedChevron = findViewById(R.id.icAdvancedChevron)
        
        etFps = findViewById(R.id.etFps)
        cbInvert = findViewById(R.id.cbInvert)
        sharpenSwitch = findViewById(R.id.sharpenSwitch)
        
        contrastSlider = findViewById(R.id.contrastSlider)
        contrastLabel = findViewById(R.id.contrastLabel)
        brightnessSlider = findViewById(R.id.brightnessSlider)
        brightnessLabel = findViewById(R.id.brightnessLabel)
        durationSlider = findViewById(R.id.durationSlider)
        durationLabel = findViewById(R.id.durationLabel)
        
        btnProcess = findViewById(R.id.btnProcess)
        btnCancel = findViewById(R.id.btnCancel)
        progressBar = findViewById(R.id.progressBar)
        btnOpenManager = findViewById(R.id.btnOpenManager)
        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)
        btnLaunchLyricStudio = findViewById(R.id.btnLaunchLyricStudio)
    }

    private fun setupListeners() {
        btnSelectVideo.setOnClickListener {
            selectMediaLauncher.launch(arrayOf("video/*", "image/*"))
        }

        btnLaunchLyricStudio.setOnClickListener {
            startActivity(Intent(this, LyricStudioActivity::class.java))
        }

        // Advanced Options Toggle
        btnAdvancedToggle.setOnClickListener {
            isAdvancedExpanded = !isAdvancedExpanded
            advancedContainer.visibility = if (isAdvancedExpanded) View.VISIBLE else View.GONE
            icAdvancedChevron.rotation = if (isAdvancedExpanded) 180f else 0f
        }

        // Audio Panel
        btnAttachAudio.setOnClickListener { selectAudioLauncher.launch(arrayOf("audio/*")) }
        btnDrmInfo.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Why can't you just grab the song from Spotify?")
                .setMessage("Streaming apps protect their audio so other apps can't access the files — that's true industry-wide, not something we can work around. We can only attach audio that's actually stored as a file on your phone. If we find a match, it's one tap. If not, you can still pick any audio file manually.")
                .setPositiveButton("Got it", null)
                .show()
        }

        // Sliders
        rangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            tvTrimTimes.text = String.format("Trim: %.1fs - %.1fs", values[0], values[1])
        }
        contrastSlider.addOnChangeListener { slider, value, _ ->
            contrastLabel.text = String.format("Contrast Multiplier: %.1fx", value)
        }
        brightnessSlider.addOnChangeListener { slider, value, _ ->
            brightnessLabel.text = String.format("LED Brightness: %.0f%%", value)
        }
        durationSlider.addOnChangeListener { slider, value, _ ->
            durationLabel.text = String.format("Image Duration: %.0fs", value)
        }

        // Process Action
        btnProcess.setOnClickListener { startProcessing() }
        btnCancel.setOnClickListener { cancelProcessing() }

        // Open Toys Manager
        btnOpenManager.setOnClickListener {
            try {
                val intent = Intent()
                intent.component = ComponentName("com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity")
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(findViewById(android.R.id.content), "Nothing OS Toys Manager not found.", Snackbar.LENGTH_LONG).show()
            }
        }
        
        // Preset Export / Import
        btnExport.setOnClickListener {
            val slot = prefs.getInt("selected_slot", 1)
            exportLauncher.launch("slot${slot}_preset.odyssey")
        }
        btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
        }

        // Slot selection persistence
        slotSpinner.setOnItemClickListener { _, _, position, _ ->
            prefs.edit().putInt("selected_slot", position + 1).apply()
        }
    }

    private fun syncSlotState() {
        val slot = prefs.getInt("selected_slot", 1)
        // AutoCompleteTextView uses setText(text, filter). 
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
            btnAttachAudio.text = "Change Audio"
        } else {
            tvAudioStatus.setTextColor(ContextCompat.getColor(this, R.color.colorOnSurface))
            btnAttachAudio.text = "Select Local Audio File"
        }
    }

    private fun setupVideo(uri: Uri) {
        imageView.visibility = View.GONE
        videoView.visibility = View.VISIBLE
        trimmerCard.visibility = View.VISIBLE
        durationSlider.visibility = View.GONE
        durationLabel.visibility = View.GONE

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
            tvTrimTimes.text = String.format("Trim: 0.0s - %.1fs", maxSecs)
        }
    }

    private fun setupGif(uri: Uri) {
        videoView.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        trimmerCard.visibility = View.VISIBLE
        durationSlider.visibility = View.GONE
        durationLabel.visibility = View.GONE

        imageView.setImageURI(uri)
        // Fake a 10s max trim for GIF
        rangeSlider.valueFrom = 0f
        rangeSlider.valueTo = 10f
        rangeSlider.values = listOf(0f, 10f)
        tvTrimTimes.text = "Trim: 0.0s - 10.0s"
    }

    private fun setupStaticImage(uri: Uri) {
        videoView.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        trimmerCard.visibility = View.GONE // Hide trimmer for static image
        durationSlider.visibility = View.VISIBLE
        durationLabel.visibility = View.VISIBLE

        imageView.setImageURI(uri)
    }
    
    private fun showCoachmarks() {
        // Simple coachmark implementation
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
        progressBar.progress = 0
        btnOpenManager.visibility = View.GONE

        val startTimeMs = if (currentMediaType == 2) 0L else (rangeSlider.values[0] * 1000).toLong()
        val endTimeMs = if (currentMediaType == 2) 0L else (rangeSlider.values[1] * 1000).toLong()

        val fps = etFps.text.toString().toIntOrNull() ?: 12
        val invert = cbInvert.isChecked
        val mode = if (modeSpinner.text.toString() == "LOOP") 1 else 0
        val slot = prefs.getInt("selected_slot", 1)

        val imageDurationSec = durationSlider.value.toInt()
        val contrastMulti = contrastSlider.value
        val brightnessMulti = brightnessSlider.value / 100f
        val sharpen = sharpenSwitch.isChecked

        val matrix = Matrix()
        if (currentMediaType == 0) {
            videoView.getTransformMatrix(matrix)
        } else {
            imageView.getTransformMatrix(matrix)
        }

        val inverseTransform = Matrix()
        if (!matrix.invert(inverseTransform)) {
            cancelProcessing()
            Snackbar.make(findViewById(android.R.id.content), "Matrix inversion failed.", Snackbar.LENGTH_LONG).show()
            return
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
                progressBar.progress = prog
            },
            onComplete = { success, error ->
                btnProcess.visibility = View.VISIBLE
                btnCancel.visibility = View.GONE
                progressBar.visibility = View.GONE

                if (success) {
                    Snackbar.make(findViewById(android.R.id.content), "Success! Rendered to Slot $slot.", Snackbar.LENGTH_LONG).show()
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
