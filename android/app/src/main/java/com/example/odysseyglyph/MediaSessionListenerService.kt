package com.example.odysseyglyph

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log

class MediaSessionListenerService : NotificationListenerService() {
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private var isConnected = false
    private var activeSource = ""
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    
    /**
     * Active position re-sync: Spotify/YT Music only send PlaybackState callbacks on
     * state CHANGES (play, pause, skip). Between those events, our position estimate
     * drifts because we're extrapolating from stale data. This runnable actively polls 
     * the MediaController every 500ms to get fresh position data, preventing drift.
     */
    private val positionSyncRunnable = object : Runnable {
        override fun run() {
            if (!isConnected) return
            activeController?.playbackState?.let { state ->
                MusicPlaybackState.updatePlaybackState(state)
            }
            handler.postDelayed(this, 500L)
        }
    }
    
    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            metadata?.let {
                val title = it.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                val artist = it.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                Log.d("MusicListener", "Metadata changed: $title - $artist")
                MusicPlaybackState.updateMetadata(title, artist, activeSource)
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            Log.d("MusicListener", "Playback state changed: ${state?.state}")
            MusicPlaybackState.updatePlaybackState(state)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, MediaSessionListenerService::class.java)
        
        try {
            val sessions = mediaSessionManager?.getActiveSessions(componentName)
            sessions?.let { updateActiveSessions(it) }
            
            mediaSessionManager?.addOnActiveSessionsChangedListener({ newSessions ->
                updateActiveSessions(newSessions)
            }, componentName)
            
        } catch (e: SecurityException) {
            Log.e("MusicListener", "Missing Notification Access permission", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        handler.removeCallbacks(positionSyncRunnable)
        
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        MusicPlaybackState.hasActiveSession = false
    }

    private fun updateActiveSessions(controllers: List<MediaController>?) {
        if (!isConnected) return
        
        // Prioritize Spotify or YT Music if multiple exist
        val musicController = controllers?.find { 
            it.packageName == "com.spotify.music" || it.packageName == "com.google.android.apps.youtube.music" 
        }
        
        if (musicController != null) {
            if (activeController?.sessionToken != musicController.sessionToken) {
                activeController?.unregisterCallback(controllerCallback)
                handler.removeCallbacks(positionSyncRunnable)
                
                activeController = musicController
                activeSource = if (musicController.packageName.contains("spotify")) "Spotify" else "YouTube Music"
                
                activeController?.registerCallback(controllerCallback)
                
                MusicPlaybackState.hasActiveSession = true
                Log.d("MusicListener", "Found active session: $activeSource")
                
                // Initial state dump
                controllerCallback.onMetadataChanged(musicController.metadata)
                controllerCallback.onPlaybackStateChanged(musicController.playbackState)
                
                // Start active position polling to prevent drift
                handler.post(positionSyncRunnable)
            }
        } else {
            if (activeController != null) {
                activeController?.unregisterCallback(controllerCallback)
                handler.removeCallbacks(positionSyncRunnable)
                activeController = null
                MusicPlaybackState.hasActiveSession = false
                Log.d("MusicListener", "Music session ended")
            }
        }
    }
}
