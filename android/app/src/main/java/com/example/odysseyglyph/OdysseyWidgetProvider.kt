package com.example.odysseyglyph

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

class OdysseyWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.odysseyglyph.ACTION_UPDATE_WIDGET"
        const val ACTION_TOGGLE_SERVICE = "com.example.odysseyglyph.ACTION_TOGGLE_SERVICE"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, OdysseyWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            updateAllWidgetsUI(context)
        } else if (intent.action == ACTION_TOGGLE_SERVICE) {
            toggleService(context)
        }
    }

    private fun toggleService(context: Context) {
        val prefs = context.getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE)
        val isLyricsRunning = LiveLyricsService.isRunning
        val isVisualizerRunning = VisualizerService.isRunning

        if (isLyricsRunning || isVisualizerRunning) {
            if (isVisualizerRunning) {
                prefs.edit().putString("last_used_service", "visualizer").apply()
            } else {
                prefs.edit().putString("last_used_service", "live_lyrics").apply()
            }
            if (isLyricsRunning) {
                try {
                    val stopLyrics = Intent(context, LiveLyricsService::class.java).apply { action = "STOP_LIVE_LYRICS" }
                    context.startService(stopLyrics)
                } catch (e: Exception) {}
            }
            if (isVisualizerRunning) {
                try {
                    val stopVisualizer = Intent(context, VisualizerService::class.java).apply { action = "STOP_VISUALIZER" }
                    context.startService(stopVisualizer)
                } catch (e: Exception) {}
            }
        } else {
            val lastUsed = prefs.getString("last_used_service", "live_lyrics")
            if (lastUsed == "visualizer") {
                val visualizerIntent = Intent(context, VisualizerService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try { androidx.core.content.ContextCompat.startForegroundService(context, visualizerIntent) } catch (e: Exception) {}
                } else {
                    context.startService(visualizerIntent)
                }
            } else {
                val startLyrics = Intent(context, LiveLyricsService::class.java).apply { action = "START_LIVE_LYRICS" }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try { androidx.core.content.ContextCompat.startForegroundService(context, startLyrics) } catch (e: Exception) {}
                } else {
                    context.startService(startLyrics)
                }
            }
        }
        updateAllWidgetsUI(context)
    }

    private fun updateAllWidgetsUI(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, OdysseyWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE)
        val isLyricsRunning = LiveLyricsService.isRunning
        val isVisualizerRunning = VisualizerService.isRunning
        val isActive = isLyricsRunning || isVisualizerRunning

        val views = RemoteViews(context.packageName, R.layout.odyssey_widget)

        if (isLyricsRunning) {
            views.setInt(R.id.widgetContainer, "setBackgroundResource", R.drawable.widget_bg_active)
            views.setTextViewText(R.id.widgetText, "LIVE LYRICS")
            views.setTextViewText(R.id.widgetSubtitle, "Listening to media...")
        } else if (isVisualizerRunning) {
            views.setInt(R.id.widgetContainer, "setBackgroundResource", R.drawable.widget_bg_active)
            views.setTextViewText(R.id.widgetText, "VISUALIZER")
            val style = prefs.getInt("fallback_style", 2)
            val isMic = style == 2 || style == 3 || style == 5 || style == 8
            views.setTextViewText(R.id.widgetSubtitle, if (isMic) "Mic active" else "Ambient math")
        } else {
            views.setInt(R.id.widgetContainer, "setBackgroundResource", R.drawable.widget_bg_inactive)
            views.setTextViewText(R.id.widgetText, "GLYPH OFF")
            views.setTextViewText(R.id.widgetSubtitle, "Tap to activate")
        }

        val intent = Intent(context, OdysseyWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_SERVICE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
