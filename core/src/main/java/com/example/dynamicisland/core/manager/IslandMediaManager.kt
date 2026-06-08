package com.example.dynamicisland.core.manager

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.dynamicisland.shared.model.LiveActivityModel

/**
 * 🎵 ISLAND MEDIA MANAGER
 * 
 * Central hub for tracking media playback across all Android apps.
 * Extracts album art, track info, and playback state.
 */
class IslandMediaManager(
    private val context: Context,
    private val scope: CoroutineScope,
    var onMediaChanged: (LiveActivityModel.Music?) -> Unit = {},
    var onMediaTick: (Long) -> Unit = {},
    var onPeekRequested: () -> Unit = {},
    var onPauseFadeRequested: () -> Unit = {},
    var onUncollapseRequested: () -> Unit = {}
) {
    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeController: MediaController? = null
    
    var isMediaEnabled = true
    var isScreenOn = true
    var allowedMusicApps = setOf<String>()
    var allowedMediaApps = setOf<String>()

    var currentMedia: LiveActivityModel.Music? = null
        private set

    val isPlaying: Boolean get() = currentMedia?.isPlaying == true
    val currentMediaModel: LiveActivityModel.Music? get() = currentMedia

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateActiveSession()
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateActiveSession()
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveSession()
    }

    init {
        mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, null)
        updateActiveSession()
    }

    fun updateActiveSession() {
        val controllers = mediaSessionManager.getActiveSessions(null)
        val controller = controllers.firstOrNull { 
            allowedMusicApps.contains(it.packageName) || allowedMediaApps.contains(it.packageName) 
        } ?: controllers.firstOrNull()

        if (controller != activeController) {
            activeController?.unregisterCallback(callback)
            activeController = controller
            activeController?.registerCallback(callback)
        }

        val metadata = controller?.metadata
        val playbackState = controller?.playbackState

        if (metadata != null && playbackState != null) {
            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
            val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val isPlaying = playbackState.state == PlaybackState.STATE_PLAYING
            
            val newMedia = LiveActivityModel.Music(
                id = "media_${controller.packageName}",
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
                positionMs = playbackState.position,
                appPackageName = controller.packageName
            )
            
            if (newMedia != currentMedia) {
                currentMedia = newMedia
                onMediaChanged(newMedia)
            }
        } else {
            currentMedia = null
            onMediaChanged(null)
        }
    }

    fun sendMediaCommand(action: String) {
        when (action) {
            "PLAY_PAUSE" -> {
                if (isPlaying) activeController?.transportControls?.pause()
                else activeController?.transportControls?.play()
            }
            "NEXT" -> activeController?.transportControls?.skipToNext()
            "PREV" -> activeController?.transportControls?.skipToPrevious()
        }
    }

    fun destroy() {
        mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        activeController?.unregisterCallback(callback)
    }
}
