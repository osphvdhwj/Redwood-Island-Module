package com.example.dynamicisland

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import kotlinx.coroutines.*

@Suppress("DEPRECATION")
class IslandMediaManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onMediaChanged: (LiveActivityModel.Music?) -> Unit,
    private val onMediaTick: ((Long) -> Unit)? = null,
    private val onPeekRequested: (() -> Unit)? = null,
    private val onPauseFadeRequested: (() -> Unit)? = null,
    private val onUncollapseRequested: (() -> Unit)? = null
) {
    var isMediaEnabled = true
    var isScreenOn = true
    var isIslandVisible = false
    
    var activeMediaController: MediaController? = null
        private set
    private var currentMedia: LiveActivityModel.Music? = null
    private var tickerJob: Job? = null

    private val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            if (state == null) return
            
            // ☠️ SESSION DEATH TRAP: If the app was killed or fully stopped, destroy the Island UI
            if (state.state == PlaybackState.STATE_STOPPED || 
                state.state == PlaybackState.STATE_ERROR || 
                state.state == PlaybackState.STATE_NONE) {
                
                currentMedia = null
                onMediaChanged(null)
                return
            }
            
            extractMediaData(activeMediaController?.metadata, state)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            extractMediaData(metadata, activeMediaController?.playbackState)
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        val newController = controllers?.firstOrNull()
        
        // ☠️ MASTER DEATH TRAP: User swiped away the last open media player
        if (newController == null) {
            activeMediaController?.unregisterCallback(mediaCallback)
            activeMediaController = null
            currentMedia = null
            onMediaChanged(null)
            return@OnActiveSessionsChangedListener
        }

        if (activeMediaController?.sessionToken != newController.sessionToken) {
            activeMediaController?.unregisterCallback(mediaCallback)
            activeMediaController = newController
            activeMediaController?.registerCallback(mediaCallback)
            extractMediaData(activeMediaController?.metadata, activeMediaController?.playbackState)
        }
    }

    fun start() {
        try {
            // Need a valid component name for the NotificationListenerService to read sessions
            val componentName = ComponentName(context, IslandTileService::class.java) 
            sessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
            sessionListener.onActiveSessionsChanged(sessionManager.getActiveSessions(componentName))
            startMediaTicker()
        } catch (e: Exception) {
            // Permission likely not granted yet
        }
    }

    fun destroy() {
        activeMediaController?.unregisterCallback(mediaCallback)
        try { sessionManager.removeOnActiveSessionsChangedListener(sessionListener) } catch (e: Exception) {}
        tickerJob?.cancel()
    }

    private fun extractMediaData(metadata: MediaMetadata?, pbState: PlaybackState?) {
        if (!isMediaEnabled || metadata == null || pbState == null) return
        
        scope.launch(Dispatchers.Default) {
            try {
                // 1. Basic Metadata
                val newTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
                val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
                val isPlaying = pbState.state == PlaybackState.STATE_PLAYING

                // 2. Art & Colors
                val albumArtBitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                val blurredArtBitmap: Bitmap? = null // Implement blur logic here if needed via IslandImageCache
                val appIconBmp: Bitmap? = null 
                val bgColor: Int? = null 
                val txtColor = android.graphics.Color.WHITE 

                // 3. Playback States
                val nativeShuffle = false
                val nativeRepeat = 0
                val systemLiked = false
                val extractedActions = emptyList<CustomMediaAction>()

                // 🎬 THE VIDEO CLASSIFIER HEURISTIC
                val pkg = activeMediaController?.packageName ?: ""
                val knownVideoApps = listOf(
                    "com.netflix.mediaclient", "org.videolan.vlc", "com.mxtech.videoplayer.ad",
                    "com.mxtech.videoplayer.pro", "com.amazon.avod.thirdpartyclient", 
                    "com.plexapp.android", "tv.twitch.android", "com.google.android.youtube"
                )
                val isVideoPackage = knownVideoApps.contains(pkg)

                var isWideThumbnail = false
                if (albumArtBitmap != null) {
                    val ratio = albumArtBitmap.width.toFloat() / albumArtBitmap.height.toFloat()
                    if (ratio > 1.3f) isWideThumbnail = true 
                }
                val isVideoContent = isVideoPackage || isWideThumbnail

                // 🎧 THE HI-FI AUDIO MONITOR
                var isHiFi = false
                var codecName = "Standard"
                try {
                    if (audioManager.isBluetoothA2dpOn) { codecName = "Bluetooth" } 
                    else if (audioManager.isWiredHeadsetOn) { isHiFi = true; codecName = "Wired DAC" }
                } catch (e: Throwable) {}

                val artistString = if (isHiFi && !isVideoContent) "$artist • $codecName" else artist

                // 4. Update the Model
                currentMedia = LiveActivityModel.Music(
                    id = "media_main", 
                    title = newTitle, 
                    artist = artistString,
                    albumArt = albumArtBitmap, 
                    blurredAlbumArt = blurredArtBitmap, 
                    appIcon = appIconBmp, 
                    dominantColor = bgColor, 
                    titleTextColor = txtColor, 
                    isPlaying = isPlaying, 
                    durationMs = duration, 
                    positionMs = pbState.position, 
                    appPackageName = pkg, 
                    customActions = extractedActions, 
                    isShuffled = nativeShuffle, 
                    repeatMode = nativeRepeat, 
                    isLiked = systemLiked, 
                    isVideo = isVideoContent
                )

                withContext(Dispatchers.Main) {
                    onMediaChanged(currentMedia)
                    if (isPlaying) onUncollapseRequested?.invoke()
                    else onPauseFadeRequested?.invoke()
                }
            } catch (e: Exception) {}
        }
    }

    private fun startMediaTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                // 🧠 CPU SAVER GATE: Only calculate math if the music is ACTUALLY playing
                if (currentMedia?.isPlaying == true) {
                    val currentPos = activeMediaController?.playbackState?.position ?: 0L
                    onMediaTick?.invoke(currentPos)
                    delay(250) // 4 times a second is plenty for smooth UI
                } else {
                    delay(1000) 
                }
            }
        }
    }

    fun sendMediaCommand(command: String) {
        val transportControls = activeMediaController?.transportControls ?: return
        when (command) {
            "PLAY" -> transportControls.play()
            "PAUSE" -> transportControls.pause()
            "NEXT" -> transportControls.skipToNext()
            "PREV" -> transportControls.skipToPrevious()
        }
    }
    
    fun toggleShuffle() { activeMediaController?.transportControls?.setShuffleMode(PlaybackState.SHUFFLE_MODE_ALL) }
    fun toggleRepeat() { activeMediaController?.transportControls?.setRepeatMode(PlaybackState.REPEAT_MODE_ALL) }
    fun triggerCustomAction(action: String) { activeMediaController?.transportControls?.sendCustomAction(action, null) }
    fun seekTo(position: Long) { activeMediaController?.transportControls?.seekTo(position) }
}
