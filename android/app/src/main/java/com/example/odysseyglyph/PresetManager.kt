package com.example.odysseyglyph

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object PresetManager {
    
    fun getPresetsDir(context: Context): File {
        val dir = File(context.filesDir, "presets")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    private fun getMetaFile(context: Context): File {
        return File(getPresetsDir(context), "presets_meta.json")
    }
    
    fun getPresets(context: Context): List<Preset> {
        val metaFile = getMetaFile(context)
        if (!metaFile.exists()) return emptyList()
        
        val jsonStr = metaFile.readText()
        if (jsonStr.isEmpty()) return emptyList()
        
        val list = mutableListOf<Preset>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Preset(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        type = obj.getString("type"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }
    
    private fun savePresetsMeta(context: Context, presets: List<Preset>) {
        val arr = JSONArray()
        for (p in presets) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("type", p.type)
            obj.put("timestamp", p.timestamp)
            arr.put(obj)
        }
        getMetaFile(context).writeText(arr.toString())
    }
    
    fun createPreset(context: Context, name: String, type: String): String {
        val id = UUID.randomUUID().toString()
        val newPreset = Preset(id, name, type, System.currentTimeMillis())
        
        val current = getPresets(context).toMutableList()
        current.add(0, newPreset) // Add to top
        savePresetsMeta(context, current)
        
        return id
    }
    
    fun deletePreset(context: Context, id: String) {
        val current = getPresets(context).toMutableList()
        current.removeAll { it.id == id }
        savePresetsMeta(context, current)
        
        // Delete files
        val dir = getPresetsDir(context)
        File(dir, "${id}.bin").delete()
        File(dir, "${id}.mp3").delete()
    }
    
    fun assignPresetToSlot(context: Context, id: String, slotIndex: Int) {
        val dir = getPresetsDir(context)
        val sourceBin = File(dir, "${id}.bin")
        val sourceMp3 = File(dir, "${id}.mp3")
        
        val targetBin = File(context.filesDir, "frames_slot$slotIndex.bin")
        val targetMp3 = File(context.filesDir, "audio_slot$slotIndex.mp3")
        
        if (sourceBin.exists()) {
            sourceBin.copyTo(targetBin, overwrite = true)
        }
        
        if (sourceMp3.exists()) {
            sourceMp3.copyTo(targetMp3, overwrite = true)
        } else {
            // Delete old audio if this preset has no audio
            if (targetMp3.exists()) {
                targetMp3.delete()
            }
        }
    }
}
