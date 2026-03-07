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

    // Master States
    private val _islandState = MutableStateFlow(IslandState.TYPE_0_RING)
    val islandState = _islandState.asStateFlow()

    private val _activeModel = MutableStateFlow<LiveActivityModel?>(null)
    val activeModel = _activeModel.asStateFlow()

    // Data Backing Fields
    private var currentMedia: LiveActivityModel.Music? = null
    private var currentHardware: LiveActivityModel.HardwareMonitor? = null
    private var transientModel: LiveActivityModel? = null
    private var transientJob: Job? = null

    // Media Tracking
    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeMediaController: MediaController? = null
    private var mediaTickerJob: Job? = null

    /**
     * Binds the Compose UI to this controller.
     */
    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams): android.view.View {
        val view = DynamicIslandView(context)
        view.windowManager = wm
        view.windowParams = params

        // 🚀 OMNI-DIRECTIONAL GESTURE MAP
        view.onSingleTap = { onSingleTap() }
        view.onSwipeUp = { onSwipeUp() }
        view.onSwipeDown = { onSwipeDown() }
        view.onSwipeLeft = { onSwipeLeft() }
        view.onSwipeRight = { onSwipeRight() }
        
        // Failsafes / Extras
        view.onDoubleTap = { onDoubleTap() }
        view.onCloseClick = { onTripleTap() }

        // Slider Media Controls
        view.onPlayPauseClick = {
            if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY")
        }
        view.onPrevClick = { sendMediaCommand("PREV") }
        view.onNextClick = { sendMediaCommand("NEXT") }

        // Observe the flows and feed them to the view
        scope.launch {
            islandState.collect { state -> view.setState(state) }
        }
        scope.launch {
            activeModel.collect { model -> view.setModel(model) }
        }

        return view
    }

    // --- STATE ROUTING & PRIORITIES ---

    private fun evaluatePriority() {
        // 1. Transients (Charging, Calls) override everything temporarily
        if (transientModel != null) {
            _activeModel.value = transientModel
            _islandState.value = IslandState.TYPE_2_MID
            return
        }

        // 2. Media has next highest priority
        if (currentMedia != null && currentMedia?.isPlaying == true) {
            _activeModel.value = currentMedia
            // We default to Mid. Max is only triggered by user interaction.
            if (_islandState.value != IslandState.TYPE_3_MAX) {
                _islandState.value = IslandState.TYPE_2_MID
            }
            return
        }

        // 3. Hardware / Gaming Monitor
        if (currentHardware?.isGamingModeOn == true) {
            _activeModel.value = currentHardware
            _islandState.value = IslandState.TYPE_1_MINI
            return
        }

        // Fallback to Ring if nothing is active
        _activeModel.value = null
        _islandState.value = IslandState.TYPE_0_RING
    }

    // --- TRANSIENT (BATTERY) FIX ---

    fun postTransientNotification(model: LiveActivityModel, durationMs: Long = 5000L) {
        transientJob?.cancel()

        transientModel = model
        evaluatePriority()

        transientJob = scope.launch {
            delay(durationMs)
            transientModel = null
            evaluatePriority() // Recalculate without the transient
        }
    }

    // --- MEDIA ENGINE ---

    private fun setupMediaListener() {
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener({ controllers ->
                updateActiveMediaController(controllers?.firstOrNull())
            }, ComponentName(context, "com.example.dynamicisland.DummyListener"))

            updateActiveMediaController(mediaSessionManager.getActiveSessions(ComponentName(context, "com.example.dynamicisland.DummyListener")).firstOrNull())
        } catch (e: Exception) {
            Log.e("DynamicIsland", "Media Listener failed (Permissions missing?)", e)
        }
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
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            extractMediaData(activeMediaController)
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            extractMediaData(activeMediaController)
        }
    }

    private fun extractMediaData(controller: MediaController?) {
        if (controller == null) return
        val metadata = controller.metadata
        val pbState = controller.playbackState ?: return

        val isPlaying = pbState.state == PlaybackState.STATE_PLAYING
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        // 🌟 EXTRACT ALBUM ART
        val albumArtBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        // 🌟 ADVANCED PALETTE EXTRACTOR
        var bgColor: Int? = null
        var txtColor: Int = android.graphics.Color.WHITE

        if (albumArtBitmap != null) {
            val palette = Palette.from(albumArtBitmap).generate()
            // Try to get a Vibrant Swatch first, fallback to Muted, then Dominant
            val swatch = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch
            
            if (swatch != null) {
                bgColor = swatch.rgb
                txtColor = swatch.bodyTextColor // Automatically gives us highly readable text!
            }
        }

        val extractedCustomActions = pbState.customActions.map { action ->
            CustomMediaAction(
                actionName = action.name.toString(),
                icon = null,
                pendingIntent = null,
                isEnabled = true
            )
        }

        currentMedia = LiveActivityModel.Music(
            id = "media_main",
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
            albumArt = albumArtBitmap,
            dominantColor = bgColor,      // Pass background
            titleTextColor = txtColor,    // Pass text color
            isPlaying = isPlaying,
            durationMs = duration,
            positionMs = pbState.position,
            appPackageName = controller.packageName,
            customActions = extractedCustomActions
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
                    if (_activeModel.value is LiveActivityModel.Music) {
                        _activeModel.value = currentMedia
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopMediaTicker() {
        mediaTickerJob?.cancel()
    }

    // --- UNIVERSAL GESTURE ENGINE ---

    fun onSingleTap() {
        // Tap always plays/pauses media if available
        if (_activeModel.value is LiveActivityModel.Music) {
            if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY")
        }
    }

    fun onSwipeDown() {
        // Travel UP the size ladder
        _islandState.value = when (_islandState.value) {
            IslandState.TYPE_0_RING -> IslandState.TYPE_1_MINI
            IslandState.TYPE_1_MINI -> IslandState.TYPE_2_MID
            IslandState.TYPE_2_MID -> IslandState.TYPE_3_MAX
            IslandState.TYPE_3_MAX -> IslandState.TYPE_3_MAX
            else -> IslandState.TYPE_0_RING
        }
    }

    fun onSwipeUp() {
        // Clear transient notifications first, otherwise travel DOWN the size ladder
        if (transientModel != null) {
            transientJob?.cancel()
            transientModel = null
            evaluatePriority()
        } else {
            _islandState.value = when (_islandState.value) {
                IslandState.TYPE_3_MAX -> IslandState.TYPE_2_MID
                IslandState.TYPE_2_MID -> IslandState.TYPE_1_MINI
                IslandState.TYPE_1_MINI -> IslandState.TYPE_0_RING
                IslandState.TYPE_0_RING -> IslandState.TYPE_0_RING
                else -> IslandState.TYPE_0_RING
            }
        }
    }

    fun onSwipeLeft() {
        sendMediaCommand("NEXT")
    }

    fun onSwipeRight() {
        sendMediaCommand("PREV")
    }

    fun onDoubleTap() {
        if (_islandState.value == IslandState.TYPE_3_MAX) {
            _islandState.value = IslandState.TYPE_1_MINI
        }
    }

    fun onTripleTap() {
        _islandState.value = IslandState.TYPE_0_RING
    }

    fun launchOutputSwitcher() {
        try {
            val intent = Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        }
    }

    fun sendMediaCommand(command: String, customAction: CustomMediaAction? = null) {
        val controls = activeMediaController?.transportControls ?: return
        when (command) {
            "PLAY" -> controls.play()
            "PAUSE" -> controls.pause()
            "NEXT" -> controls.skipToNext()
            "PREV" -> controls.skipToPrevious()
            "CUSTOM" -> customAction?.let { controls.sendCustomAction(it.actionName, null) }
        }
    }

    // --- HARDWARE SUBSCRIPTION ---

    private fun setupHardwareMonitor() {
    BatteryPlugin.onBatteryChanged = { level, isCharging, _ ->
         if (isCharging) {
             // Use named arguments (id =, level =, isPluggedIn =) so Kotlin doesn't get confused
             val act = LiveActivityModel.Charging(
                 id = "sys_battery", 
                 level = level, 
                 isPluggedIn = true, 
                 isTransient = true
             )
             postTransientNotification(act)
         } else {
             val act = LiveActivityModel.Charging(
                 id = "sys_battery_disconnect", 
                 level = level, 
                 isPluggedIn = false, 
                 isTransient = true
             ).copy(type = ActivityType.BATTERY_LOW)
             postTransientNotification(act)
         }
        BatteryPlugin.start(context)

        scope.launch {
            HardwareMonitors.startMonitoring().collect { hw ->
                currentHardware = hw
                if (hw.isGamingModeOn || _activeModel.value is LiveActivityModel.HardwareMonitor) {
                    evaluatePriority()
                }
            }
        }
    }

    init {
        setupHardwareMonitor()
        setupMediaListener()
    }

    fun cleanup() {
        scope.cancel()
    }
}
