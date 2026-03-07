package com.example.dynamicisland

import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class IslandController(private val context: Context) {

    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _islandState = MutableStateFlow(IslandState.TYPE_0_RING)
    val islandState = _islandState.asStateFlow()

    private val _activeModel = MutableStateFlow<LiveActivityModel?>(null)
    val activeModel = _activeModel.asStateFlow()

    private var currentMedia: LiveActivityModel.Music? = null
    private var currentHardware: LiveActivityModel.HardwareMonitor? = null
    private var transientModel: LiveActivityModel? = null
    private var transientJob: Job? = null

    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeMediaController: MediaController? = null
    private var mediaTickerJob: Job? = null

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams): android.view.View {
        
        // 1. Get our module's resources context
        val moduleContext = try {
            context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY)
        } catch (e: Exception) {
            context 
        }

        // 🚀 2. FIX: Pass BOTH contexts! 
        // context = SystemUI (prevents NPE crash), moduleContext = Your Drawables
        val view = DynamicIslandView(context, moduleContext)
        view.windowManager = wm
        view.windowParams = params

        view.onSingleTap = { onSingleTap() }
        view.onSwipeUp = { onSwipeUp() }
        view.onSwipeDown = { onSwipeDown() }
        view.onSwipeLeft = { onSwipeLeft() }
        view.onSwipeRight = { onSwipeRight() }
        view.onDoubleTap = { onDoubleTap() }
        view.onCloseClick = { onTripleTap() }

        view.onPlayPauseClick = {
            if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY")
        }
        view.onPrevClick = { sendMediaCommand("PREV") }
        view.onNextClick = { sendMediaCommand("NEXT") }

        scope.launch { islandState.collect { state -> view.setState(state) } }
        scope.launch { activeModel.collect { model -> view.setModel(model) } }

        return view
    }

    private fun evaluatePriority() {
        if (transientModel != null) {
            _activeModel.value = transientModel
            _islandState.value = IslandState.TYPE_2_MID
            return
        }
        if (currentMedia != null && currentMedia?.isPlaying == true) {
            _activeModel.value = currentMedia
            if (_islandState.value != IslandState.TYPE_3_MAX) _islandState.value = IslandState.TYPE_2_MID
            return
        }
        if (currentHardware?.isGamingModeOn == true) {
            _activeModel.value = currentHardware
            _islandState.value = IslandState.TYPE_1_MINI
            return
        }
        _activeModel.value = null
        _islandState.value = IslandState.TYPE_0_RING
    }

    fun postTransientNotification(model: LiveActivityModel, durationMs: Long = 5000L) {
        transientJob?.cancel()
        transientModel = model
        evaluatePriority()
        transientJob = scope.launch {
            delay(durationMs)
            transientModel = null
            evaluatePriority()
        }
    }

    private fun setupMediaListener() {
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener({ controllers ->
                updateActiveMediaController(controllers?.firstOrNull())
            }, ComponentName(context, "com.example.dynamicisland.DummyListener"))
            updateActiveMediaController(mediaSessionManager.getActiveSessions(ComponentName(context, "com.example.dynamicisland.DummyListener")).firstOrNull())
        } catch (e: Exception) {}
    }

    private fun updateActiveMediaController(controller: MediaController?) {
        activeMediaController?.unregisterCallback(mediaCallback)
        activeMediaController = controller
        if (controller == null) {
            currentMedia = null
            stopMediaTicker()
            evaluatePriority()
            return
        }
        controller.registerCallback(mediaCallback)
        extractMediaData(controller)
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) { extractMediaData(activeMediaController) }
        override fun onMetadataChanged(metadata: MediaMetadata?) { extractMediaData(activeMediaController) }
    }

    private fun extractMediaData(controller: MediaController?) {
        if (controller == null) return
        val metadata = controller.metadata
        val pbState = controller.playbackState ?: return

        val isPlaying = pbState.state == PlaybackState.STATE_PLAYING
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        val albumArtBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        var bgColor: Int? = null
        var txtColor: Int = android.graphics.Color.WHITE

        if (albumArtBitmap != null) {
            val palette = Palette.from(albumArtBitmap).generate()
            val swatch = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch
            if (swatch != null) {
                bgColor = swatch.rgb
                txtColor = swatch.bodyTextColor 
            }
        }

        currentMedia = LiveActivityModel.Music(
            id = "media_main",
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
            albumArt = albumArtBitmap,
            dominantColor = bgColor,      
            titleTextColor = txtColor,    
            isPlaying = isPlaying,
            durationMs = duration,
            positionMs = pbState.position,
            appPackageName = controller.packageName
        )

        if (isPlaying) startMediaTicker() else stopMediaTicker()
        evaluatePriority()
    }

    private fun startMediaTicker() {
        mediaTickerJob?.cancel()
        mediaTickerJob = scope.launch {
            while (isActive) {
                activeMediaController?.playbackState?.position?.let { pos ->
                    currentMedia = currentMedia?.copy(positionMs = pos)
                    if (_activeModel.value is LiveActivityModel.Music) _activeModel.value = currentMedia
                }
                delay(1000)
            }
        }
    }

    private fun stopMediaTicker() { mediaTickerJob?.cancel() }

    fun onSingleTap() {
        if (_activeModel.value is LiveActivityModel.Music) {
            if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY")
        }
    }
    fun onSwipeDown() {
        _islandState.value = when (_islandState.value) {
            IslandState.TYPE_0_RING -> IslandState.TYPE_1_MINI
            IslandState.TYPE_1_MINI -> IslandState.TYPE_2_MID
            IslandState.TYPE_2_MID -> IslandState.TYPE_3_MAX
            IslandState.TYPE_3_MAX -> IslandState.TYPE_3_MAX
            else -> IslandState.TYPE_0_RING
        }
    }
    fun onSwipeUp() {
        if (transientModel != null) {
            transientJob?.cancel()
            transientModel = null
            evaluatePriority()
        } else {
            _islandState.value = when (_islandState.value) {
                IslandState.TYPE_3_MAX -> IslandState.TYPE_2_MID
                IslandState.TYPE_2_MID -> IslandState.TYPE_1_MINI
                IslandState.TYPE_1_MINI -> IslandState.TYPE_0_RING
                else -> IslandState.TYPE_0_RING
            }
        }
    }
    fun onSwipeLeft() { sendMediaCommand("NEXT") }
    fun onSwipeRight() { sendMediaCommand("PREV") }
    fun onDoubleTap() { if (_islandState.value == IslandState.TYPE_3_MAX) _islandState.value = IslandState.TYPE_1_MINI }
    fun onTripleTap() { _islandState.value = IslandState.TYPE_0_RING }

    fun sendMediaCommand(command: String) {
        val controls = activeMediaController?.transportControls ?: return
        when (command) {
            "PLAY" -> controls.play()
            "PAUSE" -> controls.pause()
            "NEXT" -> controls.skipToNext()
            "PREV" -> controls.skipToPrevious()
        }
    }

    private fun setupHardwareMonitor() {
        BatteryPlugin.onBatteryChanged = { level, isCharging, _ ->
             if (isCharging) {
                 val act = LiveActivityModel.Charging(id = "sys_battery", level = level, isPluggedIn = true, isTransient = true)
                 postTransientNotification(act)
             } else {
                 val act = LiveActivityModel.Charging(id = "sys_battery_dc", level = level, isPluggedIn = false, isTransient = true).copy(type = ActivityType.BATTERY_LOW)
                 postTransientNotification(act)
             }
        }
        BatteryPlugin.start(context)

        scope.launch {
            HardwareMonitors.startMonitoring().collect { hw ->
                currentHardware = hw
                if (hw.isGamingModeOn || _activeModel.value is LiveActivityModel.HardwareMonitor) evaluatePriority()
            }
        }
    }

    init {
        setupHardwareMonitor()
        setupMediaListener()
    }
    fun cleanup() { scope.cancel() }
}
