package com.example.odysseyglyph

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

class WidgetToggleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE)
        val isLyricsRunning = LiveLyricsService.isRunning
        val isVisualizerRunning = VisualizerService.isRunning

        if (isLyricsRunning || isVisualizerRunning) {
            // Remember which service was active so next toggle restores it
            if (isVisualizerRunning) {
                prefs.edit().putString("last_used_service", "visualizer").apply()
            } else {
                prefs.edit().putString("last_used_service", "live_lyrics").apply()
            }
            if (isLyricsRunning) {
                try {
                    val stopLyrics = Intent(this, LiveLyricsService::class.java).apply { action = "STOP_LIVE_LYRICS" }
                    startService(stopLyrics)
                } catch (e: Exception) {}
            }
            if (isVisualizerRunning) {
                try {
                    val stopVisualizer = Intent(this, VisualizerService::class.java).apply { action = "STOP_VISUALIZER" }
                    startService(stopVisualizer)
                } catch (e: Exception) {}
            }
        } else {
            val lastUsed = prefs.getString("last_used_service", "live_lyrics")
            
            if (lastUsed == "visualizer") {
                val visualizerIntent = Intent(this, VisualizerService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try { startForegroundService(visualizerIntent) } catch (e: Exception) {}
                } else {
                    startService(visualizerIntent)
                }
            } else {
                val startLyrics = Intent(this, LiveLyricsService::class.java).apply { action = "START_LIVE_LYRICS" }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try { startForegroundService(startLyrics) } catch (e: Exception) {}
                } else {
                    startService(startLyrics)
                }
            }
        }
        
        OdysseyWidgetProvider.updateAllWidgets(this)
        finish()
        overridePendingTransition(0, 0)
    }
}
