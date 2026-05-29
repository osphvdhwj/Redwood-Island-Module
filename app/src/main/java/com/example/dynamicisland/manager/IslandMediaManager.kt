// File: app/src/main/java/com/example/dynamicisland/manager/IslandMediaManager.kt
package com.example.dynamicisland.manager

import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView
import com.example.dynamicisland.hook.*

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*
import kotlin.math.max

class IslandMediaManager(
    private val context: Context,
    private val scope: CoroutineScope,
    var onMediaChanged: (LiveActivityModel.Music?) -> Unit = {},
    var onMediaTick: (Long) -> Unit = {},
    var onPeekRequested: () -> Unit = {},
    var onPauseFadeRequested: () -> Unit = {},
    var onUncollapseRequested: () -> Unit = {}
) {
    var isMediaEnabled = true
        set(value) { field = value; if (!value) { currentMedia = null; stopTicker(); onMediaChanged(null) } else updateActiveSession() }
        
    var isScreenOn = true
        set(value) { field = value; evaluateTicker() }
        
    var isIslandVisible = false
        set(value) { field = value; evaluateTicker() }

    private val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    var activeMediaController: MediaController? = null
        private set
    var currentMedia: LiveActivityModel.Music? = null
        private set

    private var processJob: Job? = null
    private var tickerJob: Job? = null
    private var lastTrackTitle = ""
    
    var userMusicApp: String? = null
    var userVideoApp: String? = null

    // 🚀 OPTIMIZATION: Reuse RenderScript context to prevent memory leaks and GC jank
    private val rs: RenderScript by lazy { RenderScript.create(context) }
    private val blurScript: ScriptIntrinsicBlur by lazy { ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)) }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers -> updateActiveSession(controllers) }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) { extractMediaData(activeMediaController) }
        override fun onMetadataChanged(metadata: MediaMetadata?) { extractMediaData(activeMediaController) }
        override fun onSessionDestroyed() { updateActiveSession() }
    }

    fun start() {
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionListener, null, Handler(Looper.getMainLooper()))
            updateActiveSession(sessionManager.getActiveSessions(null))
        } catch (e: Exception) {}
    }

    private fun updateActiveSession(controllers: List<MediaController>? = null) {
        if (!isMediaEnabled) return
        val activeControllers = try { controllers ?: sessionManager.getActiveSessions(null) } catch (e: Exception) { emptyList() }
        
        val bestController = activeControllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: activeControllers.firstOrNull()

        if (activeMediaController != bestController) {
            activeMediaController?.unregisterCallback(mediaCallback)
            activeMediaController = bestController
            if (bestController == null) { currentMedia = null; stopTicker(); onMediaChanged(null); return }
            bestController.registerCallback(mediaCallback)
            extractMediaData(bestController)
        }
    }

    private fun extractMediaData(controller: MediaController?) {
        if (controller == null || !isMediaEnabled) return

        processJob?.cancel()
        val metadata = controller.metadata
        val pbState = controller.playbackState ?: return

        val isPlaying = pbState.state == PlaybackState.STATE_PLAYING
        val wasPlaying = currentMedia?.isPlaying == true
        if (!isPlaying && currentMedia == null) return 

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val rawAlbumArt = try { metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART) } catch (e: Exception) { null }
        val albumArtBitmap = getScaledBitmap(rawAlbumArt, 400)
        
        val newTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        
        val isNewTrack = newTitle != lastTrackTitle && lastTrackTitle.isNotEmpty()
        lastTrackTitle = newTitle

        // 🎬 THE VIDEO CLASSIFIER HEURISTIC
        val pkg = controller.packageName ?: ""
        val knownVideoApps = listOf("com.netflix.mediaclient", "org.videolan.vlc", "com.mxtech.videoplayer.ad", "com.mxtech.videoplayer.pro", "com.google.android.youtube")
        val isVideoPackage = knownVideoApps.contains(pkg) || pkg == userVideoApp

        val isWideThumbnail = albumArtBitmap?.let { (it.width.toFloat() / it.height.toFloat()) > 1.3f } ?: false
        val isVideoContent = isVideoPackage || isWideThumbnail

        // 🎧 Check for Hi-Fi audio routing
        var isHiFi = false
        var codecName = "Standard"
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (audioManager.isBluetoothA2dpOn) { codecName = "Bluetooth" } 
            else if (audioManager.isWiredHeadsetOn) { isHiFi = true; codecName = "Wired DAC" }
        } catch (e: Throwable) {}

        val artistString = if (isHiFi && !isVideoContent) "$artist • $codecName" else artist

        if (isNewTrack && isPlaying) onPeekRequested()

        // 🚀 Move all heavy image processing to IO Thread
        processJob = scope.launch(Dispatchers.IO) {
            val appIconBmp = getAppIcon(pkg)
            var blurredArtBitmap: Bitmap? = null
            var bgColor: Int? = null
            var txtColor = android.graphics.Color.WHITE

            if (albumArtBitmap != null) {
                // Optimized Blur using cached RenderScript
                blurredArtBitmap = blurBitmapOptimized(albumArtBitmap)
                
                val palette = Palette.from(albumArtBitmap).generate()
                val swatch = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch
                if (swatch != null) {
                    var rgb = swatch.rgb; val hsl = FloatArray(3); androidx.core.graphics.ColorUtils.colorToHSL(rgb, hsl)
                    if (hsl[2] < 0.35f) { hsl[2] = 0.35f; rgb = androidx.core.graphics.ColorUtils.HSLToColor(hsl) }
                    bgColor = rgb
                    txtColor = if (androidx.core.graphics.ColorUtils.calculateLuminance(bgColor) > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            }

            // ✅ FIXED: CustomMediaAction mapping – correct arguments and no extra parameter
            val extractedActions = pbState.customActions?.map { customAction ->
                CustomMediaAction(
                    action = customAction.action ?: "",
                    icon   = null,
                    label  = customAction.name?.toString() ?: customAction.action ?: ""
                )
            } ?: emptyList()

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

            withContext(Dispatchers.Main) {
                currentMedia = LiveActivityModel.Music(
                    id = "media_main", type = ActivityType.MESSAGE, title = newTitle, artist = artistString, 
                    albumArt = albumArtBitmap, blurredAlbumArt = blurredArtBitmap, appIcon = appIconBmp, 
                    dominantColor = bgColor, titleTextColor = txtColor, isPlaying = isPlaying, 
                    durationMs = duration, positionMs = pbState.position, appPackageName = pkg, 
                    customActions = extractedActions, isShuffled = systemShuffle, repeatMode = systemRepeat, 
                    isLiked = systemLiked, isVideo = isVideoContent
                )

                if (isPlaying && !wasPlaying) onUncollapseRequested()
                if (isPlaying) evaluateTicker() else { stopTicker(); if (wasPlaying) onPauseFadeRequested() }
                onMediaChanged(currentMedia)
            }
        }
    }

    private fun evaluateTicker() {
        if (isScreenOn && isIslandVisible && currentMedia?.isPlaying == true && isMediaEnabled) {
            if (tickerJob == null || tickerJob?.isActive != true) {
                tickerJob = scope.launch(Dispatchers.Main) { 
                    while (isActive) { 
                        activeMediaController?.playbackState?.let { state ->
                            // 🚀 FIX: Use elapsedRealtime to match Android's Media framework!
                            val timeDelta = android.os.SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
                            val currentPos = (state.position + (timeDelta * state.playbackSpeed)).toLong()
                            onMediaTick(currentPos)
                        }
                        delay(1000) 
                    } 
                }
            }
        } else stopTicker()
    }
    
    private fun getScaledBitmap(bitmap: Bitmap?, maxDim: Int): Bitmap? {
        if (bitmap == null) return null
        val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
        if (ratio >= 1.0f) return bitmap
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    }

    private fun blurBitmapOptimized(bitmap: Bitmap): Bitmap {
        // Downscale before blurring to save massive amounts of GPU time
        val small = getScaledBitmap(bitmap, 100) ?: bitmap
        val blurred = Bitmap.createBitmap(small.width, small.height, small.config ?: Bitmap.Config.ARGB_8888)
        
        val input = Allocation.createFromBitmap(rs, small)
        val output = Allocation.createTyped(rs, input.type)
        
        blurScript.setRadius(24f)
        blurScript.setInput(input)
        blurScript.forEach(output)
        output.copyTo(blurred)
        
        input.destroy()
        output.destroy()
        return blurred
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
    
    fun updateMediaFromNative(pkg: String, title: String, artist: String, artwork: Bitmap?, isPlaying: Boolean) {
        if (!isMediaEnabled) return
        
        processJob?.cancel()
        val isNewTrack = title != lastTrackTitle && lastTrackTitle.isNotEmpty()
        lastTrackTitle = title

        if (isNewTrack && isPlaying) onPeekRequested()

        processJob = scope.launch(Dispatchers.IO) {
            val appIconBmp = getAppIcon(pkg)
            var blurredArtBitmap: Bitmap? = null
            var bgColor: Int? = null
            var txtColor = android.graphics.Color.WHITE

            if (artwork != null) {
                blurredArtBitmap = blurBitmapOptimized(artwork)
                val palette = Palette.from(artwork).generate()
                val swatch = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch
                if (swatch != null) {
                    var rgb = swatch.rgb; val hsl = FloatArray(3); androidx.core.graphics.ColorUtils.colorToHSL(rgb, hsl)
                    if (hsl[2] < 0.35f) { hsl[2] = 0.35f; rgb = androidx.core.graphics.ColorUtils.HSLToColor(hsl) }
                    bgColor = rgb
                    txtColor = if (androidx.core.graphics.ColorUtils.calculateLuminance(bgColor) > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            }

            val musicModel = LiveActivityModel.Music(
                id = "sys_media_native",
                pkgName = pkg,
                title = title,
                artist = artist,
                albumArt = artwork,
                blurredAlbumArt = blurredArtBitmap,
                appIcon = appIconBmp,
                isPlaying = isPlaying,
                duration = 0L, 
                position = 0L,
                dominantColor = bgColor,
                contentColor = txtColor
            )

            withContext(Dispatchers.Main) {
                currentMedia = musicModel
                onMediaChanged(musicModel)
            }
        }
    }

    private fun stopTicker() { tickerJob?.cancel(); tickerJob = null }

    fun sendMediaCommand(command: String) {
        val controls = activeMediaController?.transportControls ?: return
        try { when (command) { "PLAY" -> controls.play(); "PAUSE" -> controls.pause(); "NEXT" -> controls.skipToNext(); "PREV" -> controls.skipToPrevious() } } catch (e: Exception) {}
    }
    
    fun destroy() {
        try {
            sessionManager.removeOnActiveSessionsChangedListener(sessionListener)
            activeMediaController?.unregisterCallback(mediaCallback)
            stopTicker()
            rs.destroy() // Clean up hardware resources
        } catch (e: Exception) {}
    }
}