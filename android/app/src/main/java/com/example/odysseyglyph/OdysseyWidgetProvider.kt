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
        }
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

        val intent = Intent(context, WidgetToggleActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
