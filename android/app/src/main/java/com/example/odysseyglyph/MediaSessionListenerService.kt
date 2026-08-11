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
                
                activeController = musicController
                activeSource = if (musicController.packageName.contains("spotify")) "Spotify" else "YouTube Music"
                
                activeController?.registerCallback(controllerCallback)
                
                MusicPlaybackState.hasActiveSession = true
                Log.d("MusicListener", "Found active session: $activeSource")
                
                // Initial state dump
                controllerCallback.onMetadataChanged(musicController.metadata)
                controllerCallback.onPlaybackStateChanged(musicController.playbackState)
            }
        } else {
            if (activeController != null) {
                activeController?.unregisterCallback(controllerCallback)
                activeController = null
                MusicPlaybackState.hasActiveSession = false
                Log.d("MusicListener", "Music session ended")
            }
        }
    }
}
