package com.example.odysseyglyph

import android.media.session.PlaybackState

object MusicPlaybackState {
    var trackTitle: String = ""
    var artist: String = ""
    
    var isPlaying: Boolean = false
    var position: Long = 0
    var lastUpdateTime: Long = 0
    var playbackSpeed: Float = 1.0f
    
    var hasActiveSession: Boolean = false
    var manualOverrideLyrics: List<Pair<Long, String>>? = null
    var manualOverrideTrackName: String = ""
    var activeSource: String = "" // "Spotify" or "YT Music"
    
    interface StateChangeListener {
        fun onMetadataChanged(title: String, artist: String)
        fun onPlaybackStateChanged(isPlaying: Boolean)
    }
    
    private val listeners = mutableListOf<StateChangeListener>()
    
    fun addListener(listener: StateChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }
    
    fun removeListener(listener: StateChangeListener) {
        listeners.remove(listener)
    }
    
    fun updateMetadata(newTitle: String, newArtist: String, source: String) {
        if (trackTitle != newTitle || artist != newArtist) {
            trackTitle = newTitle
            artist = newArtist
            activeSource = source
            manualOverrideLyrics = null
            listeners.forEach { it.onMetadataChanged(trackTitle, artist) }
        }
    }
    
    fun updatePlaybackState(state: PlaybackState?) {
        if (state != null) {
            val newIsPlaying = state.state == PlaybackState.STATE_PLAYING
            position = state.position
            lastUpdateTime = state.lastPositionUpdateTime
            playbackSpeed = state.playbackSpeed
            
            if (isPlaying != newIsPlaying) {
                isPlaying = newIsPlaying
                listeners.forEach { it.onPlaybackStateChanged(isPlaying) }
            }
        }
    }
}
