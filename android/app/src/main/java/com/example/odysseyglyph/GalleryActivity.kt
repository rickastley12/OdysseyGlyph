package com.example.odysseyglyph

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GalleryActivity : AppCompatActivity() {

    private lateinit var rvPresets: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: PresetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvPresets = findViewById(R.id.rvPresets)
        tvEmpty = findViewById(R.id.tvEmpty)

        rvPresets.layoutManager = LinearLayoutManager(this)
        
        loadPresets()
    }

    private fun loadPresets() {
        val presets = PresetManager.getPresets(this)
        if (presets.isEmpty()) {
            rvPresets.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            rvPresets.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            adapter = PresetAdapter(presets, this::onPresetClicked)
            rvPresets.adapter = adapter
        }
    }

    private fun onPresetClicked(preset: Preset) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_preset_options, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tvOptionsTitle).text = preset.name

        dialogView.findViewById<MaterialButton>(R.id.btnSlot1).setOnClickListener {
            assignToSlot(preset, 1)
            dialog.dismiss()
        }
        dialogView.findViewById<MaterialButton>(R.id.btnSlot2).setOnClickListener {
            assignToSlot(preset, 2)
            dialog.dismiss()
        }
        dialogView.findViewById<MaterialButton>(R.id.btnSlot3).setOnClickListener {
            assignToSlot(preset, 3)
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnShare).setOnClickListener {
            sharePreset(preset)
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
            PresetManager.deletePreset(this, preset.id)
            loadPresets()
            dialog.dismiss()
            Snackbar.make(findViewById(android.R.id.content), "Preset deleted.", Snackbar.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun assignToSlot(preset: Preset, slot: Int) {
        PresetManager.assignPresetToSlot(this, preset.id, slot)
        
        val intent = Intent("com.nothing.glyph.TOY_UPDATE")
        intent.putExtra("slot", slot)
        sendBroadcast(intent)
        
        Snackbar.make(findViewById(android.R.id.content), "Assigned to Slot $slot", Snackbar.LENGTH_SHORT).show()
    }

    private fun sharePreset(preset: Preset) {
        val dir = PresetManager.getPresetsDir(this)
        val binFile = File(dir, "${preset.id}.bin")
        val mp3File = File(dir, "${preset.id}.mp3")

        if (!binFile.exists()) {
            Snackbar.make(findViewById(android.R.id.content), "Preset data missing.", Snackbar.LENGTH_SHORT).show()
            return
        }

        try {
            val shareDir = File(cacheDir, "shared_presets")
            shareDir.mkdirs()
            val zipFile = File(shareDir, "${preset.name.replace(" ", "_")}.odyssey")
            
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // Add bin
                val binEntry = ZipEntry("frames.bin")
                zos.putNextEntry(binEntry)
                FileInputStream(binFile).use { it.copyTo(zos) }
                zos.closeEntry()
                
                // Add mp3 if exists
                if (mp3File.exists()) {
                    val mp3Entry = ZipEntry("audio.mp3")
                    zos.putNextEntry(mp3Entry)
                    FileInputStream(mp3File).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }

            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", zipFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Preset"))
        } catch (e: Exception) {
            e.printStackTrace()
            Snackbar.make(findViewById(android.R.id.content), "Failed to share preset.", Snackbar.LENGTH_SHORT).show()
        }
    }
}

class PresetAdapter(
    private val presets: List<Preset>,
    private val onClick: (Preset) -> Unit
) : RecyclerView.Adapter<PresetAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPresetName)
        val tvType: TextView = view.findViewById(R.id.tvPresetType)
        val tvDate: TextView = view.findViewById(R.id.tvPresetDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_preset, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = presets[position]
        holder.tvName.text = p.name
        holder.tvType.text = p.type
        holder.tvDate.text = dateFormat.format(Date(p.timestamp))
        
        holder.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = presets.size
}
