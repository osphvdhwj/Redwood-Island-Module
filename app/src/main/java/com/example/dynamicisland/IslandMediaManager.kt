@file:Suppress("DEPRECATION")
package com.example.dynamicisland

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*

class IslandMediaManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onMediaChanged: (LiveActivityModel.Music?) -> Unit,
    private val onMediaTick: (Long) -> Unit,
    private val onPeekRequested: () -> Unit,
    private val onPauseFadeRequested: () -> Unit,
    private val onUncollapseRequested: () -> Unit
) {
    var isMediaEnabled = true
        set(value) {
            field = value
            if (!value) { currentMedia = null; stopMediaTicker(); onMediaChanged(null) }
            else { updateActiveMediaController(getBestMediaController(mediaSessionManager.getActiveSessions(null))) }
        }
        
    var isScreenOn = true
        set(value) {
            field = value
            if (value && currentMedia?.isPlaying == true && isMediaEnabled) startMediaTicker() else stopMediaTicker()
        }

    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    var activeMediaController: MediaController? = null
        private set
    var currentMedia: LiveActivityModel.Music? = null
        private set

    private var mediaProcessJob: Job? = null
    private var mediaTickerJob: Job? = null
    private var lastTrackTitle = ""

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers -> 
        updateActiveMediaController(getBestMediaController(controllers)) 
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) { extractMediaData(activeMediaController) }
        override fun onMetadataChanged(metadata: MediaMetadata?) { extractMediaData(activeMediaController) }
    }

    fun start() {
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, null)
            updateActiveMediaController(getBestMediaController(mediaSessionManager.getActiveSessions(null)))
        } catch (e: Exception) {}
    }

    private fun getBestMediaController(controllers: List<MediaController>?): MediaController? {
        if (!isMediaEnabled || controllers.isNullOrEmpty()) return null
        return controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: controllers.firstOrNull()
    }

    private fun updateActiveMediaController(controller: MediaController?) {
        activeMediaController?.unregisterCallback(mediaCallback)
        activeMediaController = controller
        if (controller == null || !isMediaEnabled) {
            currentMedia = null
            stopMediaTicker()
            onMediaChanged(null)
            return
        }
        controller.registerCallback(mediaCallback)
        extractMediaData(controller)
    }

    private fun extractMediaData(controller: MediaController?) {
        if (controller == null || !isMediaEnabled) return

        mediaProcessJob?.cancel()
        val metadata = controller.metadata
        val pbState = controller.playbackState ?: return

        val isPlaying = pbState.state == PlaybackState.STATE_PLAYING
        val wasPlaying = currentMedia?.isPlaying == true
        if (!isPlaying && currentMedia == null) return 

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val rawAlbumArt = try { metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART) } catch (e: Exception) { null }
        val albumArtBitmap = getScaledBitmap(rawAlbumArt)
        
        val newTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        val isNewTrack = newTitle != lastTrackTitle && lastTrackTitle.isNotEmpty()
        lastTrackTitle = newTitle

        val isFirstLoad = currentMedia == null || isNewTrack
        if (isFirstLoad) {
            currentMedia = LiveActivityModel.Music(
                id = "media_main", title = newTitle, artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
                albumArt = albumArtBitmap, blurredAlbumArt = null, appIcon = null, dominantColor = android.graphics.Color.DKGRAY, 
                titleTextColor = android.graphics.Color.WHITE, isPlaying = isPlaying, durationMs = duration, positionMs = pbState.position,
                appPackageName = controller.packageName, customActions = emptyList(), isShuffled = false, repeatMode = 0, isLiked = false
            )
            onMediaChanged(currentMedia)
        }

        if (isNewTrack && isPlaying) { onPeekRequested() }

        scope.launch(Dispatchers.IO) {
            val appIconBmp = getAppIcon(controller.packageName)
            var blurredArtBitmap: Bitmap? = null
            var bgColor: Int? = null
            var txtColor = android.graphics.Color.WHITE

            if (albumArtBitmap != null) {
                blurredArtBitmap = blurBitmap(albumArtBitmap)
                val palette = Palette.from(albumArtBitmap).generate()
                val swatch = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch
                if (swatch != null) {
                    var rgb = swatch.rgb
                    val hsl = FloatArray(3)
                    androidx.core.graphics.ColorUtils.colorToHSL(rgb, hsl)
                    if (hsl[2] < 0.35f) { hsl[2] = 0.35f; rgb = androidx.core.graphics.ColorUtils.HSLToColor(hsl) }
                    bgColor = rgb
                    txtColor = if (androidx.core.graphics.ColorUtils.calculateLuminance(bgColor) > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            }

            withContext(Dispatchers.Main) {
                currentMedia?.blurredAlbumArt?.takeIf { it != currentMedia?.albumArt }?.recycle()
                val extractedActions = pbState.customActions.map { CustomMediaAction(it.action, null, null, true) }
                
                var systemLiked = false; var systemShuffle = false; var systemRepeat = 0
                pbState.customActions?.forEach { action ->
                    val actionId = action.action?.lowercase() ?: ""
                    val locName = action.name?.toString()?.lowercase() ?: ""
                    if (actionId.contains("unlike") || locName.contains("unlike") || locName.contains("dislike")) systemLiked = true
                    if (actionId.contains("shuffle") || locName.contains("shuffle")) { if (locName.contains("disable") || locName.contains("off")) systemShuffle = true }
                    if (actionId.contains("repeat") || locName.contains("repeat")) {
                        if (locName.contains("one") || locName.contains("single")) systemRepeat = 1 
                        else if (locName.contains("disable") || locName.contains("off")) systemRepeat = 2 
                    }
                }

                currentMedia = LiveActivityModel.Music(
                    id = "media_main", title = newTitle, artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
                    albumArt = albumArtBitmap, blurredAlbumArt = blurredArtBitmap, appIcon = appIconBmp, dominantColor = bgColor, titleTextColor = txtColor,
                    isPlaying = isPlaying, durationMs = duration, positionMs = pbState.position, appPackageName = controller.packageName, customActions = extractedActions,
                    isShuffled = systemShuffle, repeatMode = systemRepeat, isLiked = systemLiked
                )

                if (isPlaying && !wasPlaying) { onUncollapseRequested() }
                if (isPlaying) { startMediaTicker() } else { stopMediaTicker(); if (wasPlaying) onPauseFadeRequested() }
                
                onMediaChanged(currentMedia)
            }
        }
    }

    private fun getScaledBitmap(bitmap: Bitmap?, maxDim: Int = 400): Bitmap? {
        if (bitmap == null) return null
        val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
        if (ratio >= 1.0f) return bitmap
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    }

    private fun blurBitmap(bitmap: Bitmap): Bitmap? {
        return try {
            val rs = android.renderscript.RenderScript.create(context)
            val input = android.renderscript.Allocation.createFromBitmap(rs, bitmap)
            val output = android.renderscript.Allocation.createTyped(rs, input.type)
            val script = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs))
            script.setRadius(24f); script.setInput(input); script.forEach(output)
            val blurred = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
            output.copyTo(blurred)
            input.destroy(); output.destroy(); script.destroy(); rs.destroy()
            blurred
        } catch (e: Exception) { bitmap }
    }

    private fun getAppIcon(pkg: String): Bitmap? {
        return try {
            val drawable = context.packageManager.getApplicationIcon(pkg)
            val bmp = Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            getScaledBitmap(bmp, 100) 
        } catch(e: Exception) { null }
    }

    private fun startMediaTicker() {
        mediaTickerJob?.cancel()
        mediaTickerJob = scope.launch { 
            while (isActive) { 
                activeMediaController?.playbackState?.position?.let { pos -> onMediaTick(pos) }
                delay(1000) 
            } 
        }
    }
    
    private fun stopMediaTicker() { mediaTickerJob?.cancel() }

    fun sendMediaCommand(command: String) {
        val controls = activeMediaController?.transportControls ?: return
        try { when (command) { "PLAY" -> controls.play(); "PAUSE" -> controls.pause(); "NEXT" -> controls.skipToNext(); "PREV" -> controls.skipToPrevious() } } catch (e: Exception) {}
    }
}
