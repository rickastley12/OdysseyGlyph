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
    private var activeSpotifyController: MediaController? = null
    private var isConnected = false
    
    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            metadata?.let {
                val title = it.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                val artist = it.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                Log.d("SpotifyListener", "Metadata changed: $title - $artist")
                SpotifyPlaybackState.updateMetadata(title, artist)
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            Log.d("SpotifyListener", "Playback state changed: ${state?.state}")
            SpotifyPlaybackState.updatePlaybackState(state)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.d("SpotifyListener", "Listener connected")
        
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        
        val componentName = ComponentName(this, MediaSessionListenerService::class.java)
        
        try {
            val sessions = mediaSessionManager?.getActiveSessions(componentName)
            sessions?.let { updateActiveSessions(it) }
            
            mediaSessionManager?.addOnActiveSessionsChangedListener({ newSessions ->
                updateActiveSessions(newSessions)
            }, componentName)
            
        } catch (e: SecurityException) {
            Log.e("SpotifyListener", "Missing Notification Access permission", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.d("SpotifyListener", "Listener disconnected")
        
        activeSpotifyController?.unregisterCallback(controllerCallback)
        activeSpotifyController = null
        SpotifyPlaybackState.hasActiveSession = false
    }

    private fun updateActiveSessions(controllers: List<MediaController>?) {
        if (!isConnected) return
        
        val spotifyController = controllers?.find { it.packageName == "com.spotify.music" }
        
        if (spotifyController != null) {
            if (activeSpotifyController?.sessionToken != spotifyController.sessionToken) {
                activeSpotifyController?.unregisterCallback(controllerCallback)
                
                activeSpotifyController = spotifyController
                activeSpotifyController?.registerCallback(controllerCallback)
                
                SpotifyPlaybackState.hasActiveSession = true
                Log.d("SpotifyListener", "Found active Spotify session")
                
                // Initial state dump
                controllerCallback.onMetadataChanged(spotifyController.metadata)
                controllerCallback.onPlaybackStateChanged(spotifyController.playbackState)
            }
        } else {
            if (activeSpotifyController != null) {
                activeSpotifyController?.unregisterCallback(controllerCallback)
                activeSpotifyController = null
                SpotifyPlaybackState.hasActiveSession = false
                Log.d("SpotifyListener", "Spotify session ended")
            }
        }
    }
}
