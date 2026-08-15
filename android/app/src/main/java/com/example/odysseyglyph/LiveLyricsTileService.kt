package com.example.odysseyglyph

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class LiveLyricsTileService : TileService() {

    companion object {
        fun requestTileUpdate(context: Context) {
            val componentName = ComponentName(context, LiveLyricsTileService::class.java)
            TileService.requestListeningState(context, componentName)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE).edit().putBoolean("tile_added", true).apply()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE).edit().putBoolean("tile_added", false).apply()
    }

    override fun onClick() {
        super.onClick()
        
        val isLyricsRunning = LiveLyricsService.isRunning
        val isVisualizerRunning = VisualizerService.isRunning

        if (isLyricsRunning || isVisualizerRunning) {
            if (isLyricsRunning) {
                val stopLyrics = Intent(this, LiveLyricsService::class.java).apply { action = "STOP_LIVE_LYRICS" }
                startService(stopLyrics)
            }
            if (isVisualizerRunning) {
                val stopVisualizer = Intent(this, VisualizerService::class.java).apply { action = "STOP_VISUALIZER" }
                startService(stopVisualizer)
            }
        } else {
            val prefs = getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE)
            val lastUsed = prefs.getString("last_used_service", "live_lyrics")
            if (lastUsed == "visualizer") {
                val intent = Intent(this, VisualizerService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } else {
                val startLyrics = Intent(this, LiveLyricsService::class.java).apply { action = "START_LIVE_LYRICS" }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(startLyrics)
                } else {
                    startService(startLyrics)
                }
            }
        }

        // Update tile UI optimistically
        val tile = qsTile
        if (tile != null) {
            tile.state = if (isLyricsRunning || isVisualizerRunning) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            tile.updateTile()
        }
    }

    private fun updateTileState() {
        val tile = qsTile
        if (tile != null) {
            val isLyricsRunning = LiveLyricsService.isRunning
            tile.state = if (isLyricsRunning || VisualizerService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
            OdysseyWidgetProvider.updateAllWidgets(this)
        }
    }
}
