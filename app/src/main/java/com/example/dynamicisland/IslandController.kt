package com.example.dynamicisland

import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.Rating
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
    private val _splitModel = MutableStateFlow<LiveActivityModel?>(null) 
    val splitModel = _splitModel.asStateFlow()

    private var currentMedia: LiveActivityModel.Music? = null
    private var currentHardware: LiveActivityModel.HardwareMonitor? = null
    private var transientModel: LiveActivityModel? = null
    private var transientJob: Job? = null
    private var pauseFadeJob: Job? = null
    private var userForceCollapsed = false 
    private var lastReportedBattery = -1
    private var isScreenOn = true 

    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeMediaController: MediaController? = null
    private var mediaTickerJob: Job? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> { isScreenOn = false; stopMediaTicker() }
                Intent.ACTION_SCREEN_ON -> { isScreenOn = true; if (currentMedia?.isPlaying == true) startMediaTicker() }
            }
        }
    }
    
    private var isLandscape = false

    // 🚀 NEW: Listen for device rotation (Landscape/Portrait)
    private val componentCallbacks = object : android.content.ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
            isLandscape = newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape) {
                _islandState.value = IslandState.HIDDEN // Hide instantly in games/video
            } else {
                evaluatePriority() // Restore the island when back in portrait
            }
        }
        override fun onLowMemory() {}
    }

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams): android.view.View {
        val moduleContext = try { context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) } catch (e: Exception) { context }
        val view = DynamicIslandView(context, moduleContext)
        view.windowManager = wm
        view.windowParams = params

        view.onSingleTap = { onSingleTap() } 
        view.onPillLongPress = { onPillLongPress() }
        
        view.onDoubleTap = { state ->
            if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_SPLIT) {
                if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY")
            } else if (state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) {
                val heartAction = currentMedia?.customActions?.find { it.actionName.contains("heart", true) || it.actionName.contains("favorite", true) || it.actionName.contains("like", true) }
                if (heartAction != null) activeMediaController?.transportControls?.sendCustomAction(heartAction.actionName, null)
                else activeMediaController?.transportControls?.setRating(Rating.newHeartRating(true))
            }
        }

        view.onSwipeLeft = { state -> if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_2_MID || state == IslandState.TYPE_SPLIT) sendMediaCommand("NEXT") }
        view.onSwipeRight = { state -> if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_2_MID || state == IslandState.TYPE_SPLIT) sendMediaCommand("PREV") }
        view.onGrabberDragDown = { onGrabberDragDown() } 
        view.onGrabberDragUp = { onGrabberDragUp() }     
        view.onGrabberLongPress = { onGrabberLongPress() } 
        
        view.onAudioOutputClick = { launchAudioOutputSwitcher() }
        view.onPlayPauseClick = { if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY") }
        view.onPrevClick = { sendMediaCommand("PREV") }
        view.onNextClick = { sendMediaCommand("NEXT") }
        view.onSeekTo = { position -> activeMediaController?.transportControls?.seekTo(position) }

        scope.launch { islandState.collect { state -> view.setState(state) } }
        scope.launch { activeModel.collect { model -> view.setModel(model) } }
        scope.launch { splitModel.collect { model -> view.setSplitModel(model) } }
        return view
    }

    private fun launchAudioOutputSwitcher() {
        try {
            val intent = Intent("com.android.systemui.action.LAUNCH_SYSTEM_MEDIA_OUTPUT_DIALOG").apply { component = ComponentName("com.android.systemui", "com.android.systemui.media.dialog.MediaOutputDialogReceiver") }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            pendingIntent.send()
        } catch (e: Exception) {}
    }

    private fun evaluatePriority() {
        if (isLandscape) {
            _islandState.value = IslandState.HIDDEN
            return
        }
        if (transientModel != null) {
            if (currentMedia?.isPlaying == true || currentMedia != null) { _activeModel.value = currentMedia; _splitModel.value = transientModel; _islandState.value = IslandState.TYPE_SPLIT } 
            else { _activeModel.value = transientModel; _splitModel.value = null; _islandState.value = IslandState.TYPE_CUBE }
            return
        }
        _splitModel.value = null
        if (_activeModel.value is LiveActivityModel.Dashboard) return
        if (currentMedia != null) {
            _activeModel.value = currentMedia
            if (!userForceCollapsed && (_islandState.value == IslandState.HIDDEN || _islandState.value == IslandState.TYPE_0_RING || _islandState.value == IslandState.TYPE_CUBE || _islandState.value == IslandState.TYPE_SPLIT)) {
                _islandState.value = IslandState.TYPE_1_MINI
            }
            return
        }
        if (currentHardware?.isGamingModeOn == true) { _activeModel.value = currentHardware; _islandState.value = IslandState.TYPE_1_MINI; return }
        _activeModel.value = null
        _islandState.value = IslandState.TYPE_0_RING
    }

    fun postTransientNotification(model: LiveActivityModel, durationMs: Long = 5000L) {
        transientJob?.cancel(); transientModel = model; evaluatePriority()
        transientJob = scope.launch { delay(durationMs); transientModel = null; evaluatePriority() }
    }

    private fun setupMediaListener() {
        try { mediaSessionManager.addOnActiveSessionsChangedListener({ controllers -> updateActiveMediaController(controllers?.firstOrNull()) }, ComponentName(context, "com.example.dynamicisland.DummyListener")); updateActiveMediaController(mediaSessionManager.getActiveSessions(ComponentName(context, "com.example.dynamicisland.DummyListener")).firstOrNull()) } catch (e: Exception) {}
    }

    private fun updateActiveMediaController(controller: MediaController?) {
        activeMediaController?.unregisterCallback(mediaCallback); activeMediaController = controller
        if (controller == null) { currentMedia = null; stopMediaTicker(); evaluatePriority(); return }
        controller.registerCallback(mediaCallback); extractMediaData(controller)
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) { extractMediaData(activeMediaController) }
        override fun onMetadataChanged(metadata: MediaMetadata?) { extractMediaData(activeMediaController) }
    }

    // 🚀 NEW: Safely downscale massive album arts to stop SystemUI Memory Leaks
    private fun getScaledBitmap(bitmap: Bitmap?, maxDim: Int = 400): Bitmap? {
        if (bitmap == null) return null
        val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
        if (ratio >= 1.0f) return bitmap
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    }

    private fun extractMediaData(controller: MediaController?) {
        if (controller == null) return
        val metadata = controller.metadata
        val pbState = controller.playbackState ?: return

        val isPlaying = pbState.state == PlaybackState.STATE_PLAYING
        val wasPlaying = currentMedia?.isPlaying == true
        if (!isPlaying && currentMedia == null) return 

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val rawAlbumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        
        // 🚀 PREVENT OOM CRASHES
        val albumArtBitmap = getScaledBitmap(rawAlbumArt) 
        
        var appIconBitmap: Bitmap? = null
        try { 
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(controller.packageName)
            // 🚀 PREVENT ADAPTIVE ICON RENDERING GLITCHES
            val bmp = Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            appIconBitmap = getScaledBitmap(bmp, 150)
        } catch (e: Exception) {}

        var bgColor: Int? = null; var txtColor: Int = android.graphics.Color.WHITE
        if (albumArtBitmap != null) { val palette = Palette.from(albumArtBitmap).generate(); val swatch = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch; if (swatch != null) { bgColor = swatch.rgb; txtColor = swatch.bodyTextColor } }

        val extractedActions = pbState.customActions.map { CustomMediaAction(actionName = it.action, icon = null, pendingIntent = null, isEnabled = true) }

        currentMedia = LiveActivityModel.Music(id = "media_main", title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown", artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown", albumArt = albumArtBitmap, appIcon = appIconBitmap, dominantColor = bgColor, titleTextColor = txtColor, isPlaying = isPlaying, durationMs = duration, positionMs = pbState.position, appPackageName = controller.packageName, customActions = extractedActions)

        if (isPlaying && !wasPlaying) { userForceCollapsed = false; pauseFadeJob?.cancel() }
        if (isPlaying) { startMediaTicker() } else {
            stopMediaTicker()
            if (wasPlaying) { pauseFadeJob?.cancel(); pauseFadeJob = scope.launch { delay(3000); currentMedia = null; evaluatePriority() } }
        }
        evaluatePriority()
    }

    private fun startMediaTicker() {
        if (!isScreenOn) return 
        mediaTickerJob?.cancel()
        mediaTickerJob = scope.launch { 
            while (isActive) { 
                activeMediaController?.playbackState?.position?.let { pos -> 
                    // 🚀 HUGE FIX: Stop re-allocating the Data Class. Broadcast the time instead!
                    (activeModel.value as? LiveActivityModel.Music)?.let { 
                        context.sendBroadcast(Intent("com.example.dynamicisland.TICKER_UPDATE").putExtra("pos", pos))
                    }
                }
                delay(1000) 
            } 
        }
    }
    private fun stopMediaTicker() { mediaTickerJob?.cancel() }

    fun onSingleTap() {
        if (_activeModel.value?.type == null && _islandState.value == IslandState.TYPE_0_RING) { _activeModel.value = LiveActivityModel.Dashboard(); _islandState.value = IslandState.TYPE_2_MID; return }
        if (_islandState.value == IslandState.HIDDEN || _islandState.value == IslandState.TYPE_0_RING) { userForceCollapsed = false; _islandState.value = IslandState.TYPE_1_MINI }
    }

    fun onGrabberDragDown() { userForceCollapsed = false; _islandState.value = when (_islandState.value) { IslandState.TYPE_1_MINI -> IslandState.TYPE_2_MID; IslandState.TYPE_2_MID -> IslandState.TYPE_3_MAX; else -> _islandState.value } }
    fun onGrabberDragUp() { userForceCollapsed = true; _islandState.value = when (_islandState.value) { IslandState.TYPE_3_MAX -> IslandState.TYPE_2_MID; IslandState.TYPE_2_MID -> IslandState.TYPE_1_MINI; IslandState.TYPE_1_MINI -> { if (_activeModel.value is LiveActivityModel.Dashboard) _activeModel.value = null; IslandState.TYPE_0_RING }; else -> IslandState.TYPE_0_RING } }
    fun onGrabberLongPress() { userForceCollapsed = true; if (_activeModel.value is LiveActivityModel.Dashboard) _activeModel.value = null; _islandState.value = IslandState.TYPE_0_RING }

    fun onPillLongPress() {
        val model = _activeModel.value
        if (model is LiveActivityModel.Music && model.appPackageName.isNotEmpty()) {
            try { val launchIntent = context.packageManager.getLaunchIntentForPackage(model.appPackageName); launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK); launchIntent?.let { context.startActivity(it) } } catch (e: Exception) {}
        }
    }

    fun sendMediaCommand(command: String) { val controls = activeMediaController?.transportControls ?: return; when (command) { "PLAY" -> controls.play(); "PAUSE" -> controls.pause(); "NEXT" -> controls.skipToNext(); "PREV" -> controls.skipToPrevious() } }

    private fun setupHardwareMonitor() {
        val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
        context.registerReceiver(screenStateReceiver, filter)
        context.registerComponentCallbacks(componentCallbacks)

        BatteryPlugin.onBatteryChanged = { level, isCharging, _ ->
             if (isCharging) { postTransientNotification(LiveActivityModel.Charging(id = "sys_battery", level = level, isPluggedIn = true, isTransient = true), 4000L)
             } else { if (lastReportedBattery != -1 && level < lastReportedBattery) { if (level == 20 || level == 10 || level == 5) postTransientNotification(LiveActivityModel.Charging(id = "sys_battery_low", level = level, isPluggedIn = false, isTransient = true).copy(type = ActivityType.BATTERY_LOW), 6000L) } }
             lastReportedBattery = level
             context.sendBroadcast(Intent("com.example.dynamicisland.BATTERY_UPDATE").putExtra("level", level).putExtra("isCharging", isCharging))
        }
        BatteryPlugin.start(context)
        scope.launch { HardwareMonitors.startMonitoring().collect { hw -> currentHardware = hw; if (hw.isGamingModeOn || _activeModel.value is LiveActivityModel.HardwareMonitor) evaluatePriority() } }
    }
    init { setupHardwareMonitor(); setupMediaListener() }
    fun cleanup() { 
        scope.cancel() 
        context.unregisterReceiver(screenStateReceiver)
        context.unregisterComponentCallbacks(componentCallbacks)
        BatteryPlugin.stop(context) // 🚀 FIXED: Prevent battery receiver leak
    }
}
