package com.example.dynamicisland

import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
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

    // 🚀 NEW: For the Split Island tiny right cube
    private val _splitModel = MutableStateFlow<LiveActivityModel?>(null) 
    val splitModel = _splitModel.asStateFlow()

    private var currentMedia: LiveActivityModel.Music? = null
    private var currentHardware: LiveActivityModel.HardwareMonitor? = null
    private var transientModel: LiveActivityModel? = null
    private var transientJob: Job? = null

    // Global Battery Tracking for the Ring Idle State
    private var lastReportedBattery = -1

    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeMediaController: MediaController? = null
    private var mediaTickerJob: Job? = null

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams): android.view.View {
        val moduleContext = try {
            context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY)
        } catch (e: Exception) { context }

        val view = DynamicIslandView(context, moduleContext)
        view.windowManager = wm
        view.windowParams = params

        view.onSingleTap = { onSingleTap() }
        view.onPillLongPress = { onPillLongPress() }
        view.onGrabberTap = { onGrabberTap() }
        view.onGrabberLongPress = { onGrabberLongPress() }
        
        view.onSwipeUp = { onGrabberTap() }
        view.onSwipeDown = { onSingleTap() }
        view.onSwipeLeft = { sendMediaCommand("NEXT") }
        view.onSwipeRight = { sendMediaCommand("PREV") }

        view.onPlayPauseClick = { if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY") }
        view.onPrevClick = { sendMediaCommand("PREV") }
        view.onNextClick = { sendMediaCommand("NEXT") }

        scope.launch { islandState.collect { state -> view.setState(state) } }
        scope.launch { activeModel.collect { model -> view.setModel(model) } }
        scope.launch { splitModel.collect { model -> view.setSplitModel(model) } }

        return view
    }

    private fun evaluatePriority() {
        // 🚀 THE NEW IOS SPLIT & CUBE STATE MACHINE
        if (transientModel != null) {
            if (currentMedia?.isPlaying == true) {
                // Island is busy -> SPLIT STATE
                _activeModel.value = currentMedia
                _splitModel.value = transientModel
                _islandState.value = IslandState.TYPE_SPLIT
            } else {
                // Island is idle -> CUBE STATE
                _activeModel.value = transientModel
                _splitModel.value = null
                _islandState.value = IslandState.TYPE_CUBE
            }
            return
        }

        _splitModel.value = null

        if (currentMedia != null && currentMedia?.isPlaying == true) {
            _activeModel.value = currentMedia
            if (_islandState.value == IslandState.HIDDEN || _islandState.value == IslandState.TYPE_0_RING || _islandState.value == IslandState.TYPE_CUBE || _islandState.value == IslandState.TYPE_SPLIT) {
                _islandState.value = IslandState.TYPE_1_MINI
            }
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
            mediaSessionManager.addOnActiveSessionsChangedListener({ controllers -> updateActiveMediaController(controllers?.firstOrNull()) }, ComponentName(context, "com.example.dynamicisland.DummyListener"))
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

        val albumArtBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        var bgColor: Int? = null
        var txtColor: Int = android.graphics.Color.WHITE

        if (albumArtBitmap != null) {
            val palette = Palette.from(albumArtBitmap).generate()
            val swatch = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch
            if (swatch != null) { bgColor = swatch.rgb; txtColor = swatch.bodyTextColor }
        }

        currentMedia = LiveActivityModel.Music(
            id = "media_main", title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown", artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
            albumArt = albumArtBitmap, dominantColor = bgColor, titleTextColor = txtColor, isPlaying = isPlaying, durationMs = duration, positionMs = pbState.position, appPackageName = controller.packageName
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
        val currentType = _activeModel.value?.type
        _islandState.value = when (_islandState.value) {
            IslandState.HIDDEN, IslandState.TYPE_0_RING -> IslandState.TYPE_1_MINI
            IslandState.TYPE_1_MINI -> if (currentType == ActivityType.MEDIA) IslandState.TYPE_2_MID else IslandState.TYPE_1_MINI
            IslandState.TYPE_2_MID -> if (currentType == ActivityType.MEDIA) IslandState.TYPE_3_MAX else IslandState.TYPE_2_MID
            IslandState.TYPE_3_MAX -> IslandState.TYPE_3_MAX
            else -> IslandState.TYPE_0_RING
        }
    }

    fun onGrabberTap() {
        _islandState.value = when (_islandState.value) {
            IslandState.TYPE_3_MAX -> IslandState.TYPE_2_MID
            IslandState.TYPE_2_MID -> IslandState.TYPE_1_MINI
            IslandState.TYPE_1_MINI -> IslandState.TYPE_0_RING
            else -> IslandState.TYPE_0_RING
        }
    }

    fun onGrabberLongPress() { _islandState.value = IslandState.TYPE_0_RING }

    fun onPillLongPress() {
        val model = _activeModel.value
        if (model is LiveActivityModel.Music && model.appPackageName.isNotEmpty()) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(model.appPackageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                launchIntent?.let { context.startActivity(it) }
            } catch (e: Exception) {}
        }
    }

    fun sendMediaCommand(command: String) {
        val controls = activeMediaController?.transportControls ?: return
        when (command) { "PLAY" -> controls.play(); "PAUSE" -> controls.pause(); "NEXT" -> controls.skipToNext(); "PREV" -> controls.skipToPrevious() }
    }

    private fun setupHardwareMonitor() {
        BatteryPlugin.onBatteryChanged = { level, isCharging, _ ->
             // 🚀 SMART BATTERY POPUP LOGIC
             if (isCharging) {
                 postTransientNotification(LiveActivityModel.Charging(id = "sys_battery", level = level, isPluggedIn = true, isTransient = true), 4000L)
             } else {
                 if (lastReportedBattery != -1 && level < lastReportedBattery) {
                     // ONLY popup on exactly 20%, 10%, and 5%
                     if (level == 20 || level == 10 || level == 5) {
                         postTransientNotification(LiveActivityModel.Charging(id = "sys_battery_low", level = level, isPluggedIn = false, isTransient = true).copy(type = ActivityType.BATTERY_LOW), 6000L)
                     }
                 }
             }
             lastReportedBattery = level
             
             // Update the global view so the Ring (R) knows real-time battery when idle
             val view = windowManager.defaultDisplay
             val intent = Intent("com.example.dynamicisland.BATTERY_UPDATE").putExtra("level", level).putExtra("isCharging", isCharging)
             context.sendBroadcast(intent)
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
