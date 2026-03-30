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
        set(value) { field = value; if (!value) { currentMedia = null; stopMediaTicker(); onMediaChanged(null) } else { updateActiveMediaController(getBestMediaController(mediaSessionManager.getActiveSessions(null))) } }
        
    var isScreenOn = true
        set(value) { field = value; evaluateTicker() }
        
    var isIslandVisible = false
        set(value) { field = value; evaluateTicker() }

    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    var activeMediaController: MediaController? = null
        private set
    var currentMedia: LiveActivityModel.Music? = null
        private set

    private var mediaProcessJob: Job? = null
    private var mediaTickerJob: Job? = null
    private var lastTrackTitle = ""

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers -> updateActiveMediaController(getBestMediaController(controllers)) }

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
        if (controller == null || !isMediaEnabled) { currentMedia = null; stopMediaTicker(); onMediaChanged(null); return }
        controller.registerCallback(mediaCallback); extractMediaData(controller)
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

        // 🎬 THE VIDEO CLASSIFIER HEURISTIC
                val pkg = controller.packageName ?: ""
                
                // Check 1: Known Video Dictionaries
                val knownVideoApps = listOf(
                    "com.netflix.mediaclient", "org.videolan.vlc", "com.mxtech.videoplayer.ad",
                    "com.mxtech.videoplayer.pro", "com.amazon.avod.thirdpartyclient", 
                    "com.plexapp.android", "tv.twitch.android", "com.google.android.youtube"
                )
                val isVideoPackage = knownVideoApps.contains(pkg)

                // Check 2: The Aspect Ratio Trick
                // If the thumbnail is significantly wider than it is tall (e.g., 16:9), it's almost certainly a video.
                var isWideThumbnail = false
                if (albumArtBitmap != null) {
                    val ratio = albumArtBitmap.width.toFloat() / albumArtBitmap.height.toFloat()
                    if (ratio > 1.3f) isWideThumbnail = true // 16:9 is ~1.77, 1:1 is 1.0
                }

                // If either condition is true, classify it as Video
                val isVideoContent = isVideoPackage || isWideThumbnail

                // 🎧 Check for Hi-Fi audio (from previous step)
                var isHiFi = false
                var codecName = "Standard"
                try {
                    if (audioManager.isBluetoothA2dpOn) { codecName = "Bluetooth" } 
                    else if (audioManager.isWiredHeadsetOn) { isHiFi = true; codecName = "Wired DAC" }
                } catch (e: Throwable) {}

                val artistString = if (isHiFi && !isVideoContent) "$artist • $codecName" else artist

                currentMedia = LiveActivityModel.Music(
                    "media_main", ActivityType.MESSAGE, newTitle, artistString,
                    albumArtBitmap, blurredArtBitmap, appIconBmp, bgColor, txtColor, isPlaying, duration, pbState.position, 
                    pkg, extractedActions, nativeShuffle, nativeRepeat, systemLiked, 
                    isVideoContent // 🚀 Pass the classification to the UI
                )

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
                    var rgb = swatch.rgb; val hsl = FloatArray(3); androidx.core.graphics.ColorUtils.colorToHSL(rgb, hsl)
                    if (hsl[2] < 0.35f) { hsl[2] = 0.35f; rgb = androidx.core.graphics.ColorUtils.HSLToColor(hsl) }
                    bgColor = rgb; txtColor = if (androidx.core.graphics.ColorUtils.calculateLuminance(bgColor) > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            }

            withContext(Dispatchers.Main) {
                
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

                currentMedia = LiveActivityModel.Music("media_main", ActivityType.MESSAGE, newTitle, metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown", albumArtBitmap, blurredArtBitmap, appIconBmp, bgColor, txtColor, isPlaying, duration, pbState.position, controller.packageName, extractedActions, systemShuffle, systemRepeat, systemLiked)

                if (isPlaying && !wasPlaying) { onUncollapseRequested() }
                if (isPlaying) { evaluateTicker() } else { stopMediaTicker(); if (wasPlaying) onPauseFadeRequested() }
                onMediaChanged(currentMedia)
            }
        }
    }

    private fun evaluateTicker() {
        // 🛑 TIER 2 & 3 SLEEP PROTOCOL: Only run the clock ticker if Screen is ON and Island is VISIBLE.
        if (isScreenOn && isIslandVisible && currentMedia?.isPlaying == true && isMediaEnabled) {
            if (mediaTickerJob == null || mediaTickerJob?.isActive != true) {
                mediaTickerJob = scope.launch { 
                    while (isActive) { 
                        activeMediaController?.playbackState?.position?.let { pos -> onMediaTick(pos) }
                        delay(1000) 
                    } 
                }
            }
        } else {
            stopMediaTicker()
        }
    }

    private fun getScaledBitmap(bitmap: Bitmap?, maxDim: Int = 400): Bitmap? {
        if (bitmap == null) return null
        val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
        if (ratio >= 1.0f) return bitmap
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    }

    private fun blurBitmap(bitmap: Bitmap): Bitmap? {
        var rs: android.renderscript.RenderScript? = null
        var input: android.renderscript.Allocation? = null
        var output: android.renderscript.Allocation? = null
        var script: android.renderscript.ScriptIntrinsicBlur? = null
        
        return try {
            rs = android.renderscript.RenderScript.create(context)
            input = android.renderscript.Allocation.createFromBitmap(rs, bitmap)
            output = android.renderscript.Allocation.createTyped(rs, input.type)
            script = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs))
            
            script.setRadius(24f)
            script.setInput(input)
            script.forEach(output)
            
            val blurred = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
            output.copyTo(blurred)
            blurred
        } catch (e: Exception) { 
            bitmap 
        } finally {
            // 🛡️ MEMORY LEAK TRAP: Guarantee destruction even if it crashes!
            input?.destroy()
            output?.destroy()
            script?.destroy()
            rs?.destroy()
        }
    }

    private fun getAppIcon(pkg: String): Bitmap? {
        return try {
            val drawable = context.packageManager.getApplicationIcon(pkg)
            val bmp = Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height); drawable.draw(canvas)
            getScaledBitmap(bmp, 100) 
        } catch(e: Exception) { null }
    }
    
    private fun stopMediaTicker() { mediaTickerJob?.cancel(); mediaTickerJob = null }

    fun sendMediaCommand(command: String) {
        val controls = activeMediaController?.transportControls ?: return
        try { when (command) { "PLAY" -> controls.play(); "PAUSE" -> controls.pause(); "NEXT" -> controls.skipToNext(); "PREV" -> controls.skipToPrevious() } } catch (e: Exception) {}
    }
    
    fun destroy() {
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
            activeMediaController?.unregisterCallback(mediaCallback)
            stopMediaTicker()
        } catch (e: Exception) {}
    }
}
