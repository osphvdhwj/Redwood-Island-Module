@file:Suppress("DEPRECATION")
package com.example.dynamicisland

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
import org.json.JSONObject
import android.util.LruCache
import java.util.concurrent.ConcurrentHashMap

class IslandController(private val context: Context) {

    private var windowManager: WindowManager? = null
    private lateinit var layoutParams: WindowManager.LayoutParams
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _islandState = MutableStateFlow(IslandState.TYPE_0_RING)
    val islandState = _islandState.asStateFlow()
    private val _activeModel = MutableStateFlow<LiveActivityModel?>(null)
    val activeModel = _activeModel.asStateFlow()
    private val _splitModel = MutableStateFlow<LiveActivityModel?>(null) 
    val splitModel = _splitModel.asStateFlow()

    private var currentMedia: LiveActivityModel.Music? = null
    private var islandView: DynamicIslandView? = null
    private var currentHardware: LiveActivityModel.HardwareMonitor? = null
    private var transientModel: LiveActivityModel? = null
    private var transientJob: Job? = null
    private var pauseFadeJob: Job? = null
    private var hardwareMonitorJob: Job? = null
    
    private var userForceCollapsed = false 
    private var lastReportedBattery = -1
    private var wasCharging = false 
    private var isScreenOn = true 
    private var topAppPackage = "" 

    private var isMediaEnabled = true
    private var isChargingEnabled = true
    private var isAlertsEnabled = true
    private var isTimersEnabled = true

    private val gestureMatrix = mutableMapOf<String, IslandAction>()

    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeMediaController: MediaController? = null
    private var mediaTickerJob: Job? = null

    private fun getBestMediaController(controllers: List<MediaController>?): MediaController? {
        if (!isMediaEnabled) return null 
        if (controllers.isNullOrEmpty()) return null
        return controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers -> updateActiveMediaController(getBestMediaController(controllers)) }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    stopMediaTicker()
                    hardwareMonitorJob?.cancel() 
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    if (currentMedia?.isPlaying == true && isMediaEnabled) startMediaTicker()
                    startHardwareMonitor() 
                    evaluatePriority() 
                }
            }
        }
    }

    private val activeAppTimers = ConcurrentHashMap<String, Long>()
    private val exemptedApps = ConcurrentHashMap.newKeySet<String>()

    private val iconCache = object : LruCache<String, Bitmap>(10 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val ecosystemReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.example.dynamicisland.APP_CHANGED" -> {
                    topAppPackage = intent.getStringExtra("pkg") ?: ""
                    Log.d("Redwood", "App Changed: $topAppPackage")
                    evaluatePriority() 
                }
                "com.crdroid.batterywellbeing.SYSTEM_OVERRIDE" -> {
                    if (!isAlertsEnabled) return
                    when (intent.getStringExtra("action")) {
                        "SMART_CHARGE_LIMIT" -> {
                            val level = intent.getIntExtra("level", 100)
                            postTransientNotification(LiveActivityModel.Charging("sys_smart_charge", ActivityType.CHARGING, level, true, true), 6000L)
                        }
                        "THERMAL_WARNING" -> {
                            val temp = intent.getStringExtra("extra_info") ?: "High"
                            postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_thermal", alertType = "THERMAL", title = "Thermal Throttling", message = "Battery temperature at $temp", alertColor = android.graphics.Color.RED), 6000L)
                        }
                        "ROGUE_APP_DETECTED" -> {
                            val appName = intent.getStringExtra("extra_info") ?: "Unknown App"
                            postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_rogue", alertType = "ROGUE", title = "High Background Drain", message = "$appName is draining battery", alertColor = android.graphics.Color.rgb(255, 165, 0)), 6000L)
                        }
                    }
                }
                "com.crdroid.batterywellbeing.SYSTEM_ALERT" -> {
                    if (!isAlertsEnabled) return
                    val alertType = intent.getStringExtra("alertType") ?: "INFO"
                    val title = intent.getStringExtra("title") ?: "System Alert"
                    val message = intent.getStringExtra("message") ?: ""
                    val colorHex = intent.getStringExtra("colorHex") ?: "#FFFFFF"
                    val colorInt = try { android.graphics.Color.parseColor(colorHex) } catch(e: Throwable) { android.graphics.Color.WHITE }
                    postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_alert_$alertType", alertType = alertType, title = title, message = message, alertColor = colorInt), 5000L)
                }
            }
        }
    }

    private val componentCallbacks = object : android.content.ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) { evaluatePriority() }
        override fun onLowMemory() {}
        override fun onTrimMemory(level: Int) { if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) iconCache.evictAll() }
    }

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams): android.view.View {
        val moduleContext = try { context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) } catch (e: Throwable) { context }
        val view = DynamicIslandView(context, moduleContext)
        this.islandView = view 
        this.windowManager = wm 
        
        view.onSplitPillClick = { 
            val sModel = _splitModel.value; 
            if (sModel is LiveActivityModel.Charging) { 
                try {
                    val intent = Intent().setComponent(ComponentName("com.crdroid.batterywellbeing", "com.crdroid.batterywellbeing.MainActivity"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                } catch(e: Throwable) {
                    _islandState.value = IslandState.TYPE_CUBE 
                }
            } 
        }
        view.windowManager = wm
        view.windowParams = params

        view.onGestureSettingsUpdated = { payload ->
            try {
                val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
                isMediaEnabled = prefs.getBoolean("enable_media", true)
                isChargingEnabled = prefs.getBoolean("enable_charging", true)
                isAlertsEnabled = prefs.getBoolean("enable_alerts", true)
                isTimersEnabled = prefs.getBoolean("enable_timers", true)
                
                if (!isMediaEnabled && currentMedia != null) { currentMedia = null; stopMediaTicker(); evaluatePriority() }

                if (payload != null && payload.length < 5000) {
                    val json = JSONObject(payload)
                    gestureMatrix.clear()
                    json.keys().forEach { key ->
                        try { if (key.startsWith("TYPE_")) { gestureMatrix[key] = IslandAction.valueOf(json.getString(key)) } } catch (e: Throwable) {} 
                    }
                }
            } catch (e: Throwable) {}
        }

        view.onGestureEvent = { gesture ->
            val currentState = _islandState.value.name
            val actionName = gestureMatrix["${currentState}_${gesture.name}"]?.name ?: "NONE"

            when (actionName) {
                "PLAY_PAUSE" -> { if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY") }
                "NEXT_TRACK" -> sendMediaCommand("NEXT")
                "PREV_TRACK" -> sendMediaCommand("PREV")
                "VOL_UP" -> adjustVolume(android.media.AudioManager.ADJUST_RAISE)
                "VOL_DOWN" -> adjustVolume(android.media.AudioManager.ADJUST_LOWER)
                "OPEN_DASHBOARD" -> {
                    if (_activeModel.value == null) _activeModel.value = LiveActivityModel.Dashboard()
                    _islandState.value = IslandState.TYPE_3_MAX
                }
                "COLLAPSE" -> {
                    if (_activeModel.value is LiveActivityModel.Dashboard) {
                        _activeModel.value = currentMedia
                        evaluatePriority() 
                    } else {
                        when (_islandState.value) {
                            IslandState.TYPE_3_MAX -> _islandState.value = IslandState.TYPE_2_MID
                            IslandState.TYPE_2_MID -> _islandState.value = IslandState.TYPE_1_MINI
                            IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> { userForceCollapsed = true; evaluatePriority() }
                            else -> {}
                        }
                    }
                }
                "EXPAND" -> {
                    userForceCollapsed = false
                    when (_islandState.value) {
                        IslandState.TYPE_0_RING -> { 
                            if (currentMedia != null) _islandState.value = IslandState.TYPE_1_MINI 
                            else { _activeModel.value = LiveActivityModel.Dashboard(); _islandState.value = IslandState.TYPE_3_MAX }
                        }
                        IslandState.TYPE_1_MINI -> {
                            if (currentMedia != null && currentMedia?.isPlaying == false) {
                                _activeModel.value = LiveActivityModel.Dashboard(); _islandState.value = IslandState.TYPE_3_MAX
                            } else {
                                _islandState.value = IslandState.TYPE_2_MID
                            }
                        }
                        IslandState.TYPE_2_MID, IslandState.TYPE_SPLIT -> _islandState.value = IslandState.TYPE_3_MAX
                        else -> {}
                    }
                }
            }
        }
        
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

    private fun sendMediaCommand(command: String) {
        val controls = activeMediaController?.transportControls ?: return
        try { when (command) { "PLAY" -> controls.play(); "PAUSE" -> controls.pause(); "NEXT" -> controls.skipToNext(); "PREV" -> controls.skipToPrevious() } } catch (e: Throwable) {}
    }

    private fun adjustVolume(direction: Int) { try { val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager; am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, direction, android.media.AudioManager.FLAG_SHOW_UI) } catch (e: Throwable) {} }

    private fun launchAudioOutputSwitcher() {
        try {
            val intent = Intent("com.android.systemui.action.LAUNCH_SYSTEM_MEDIA_OUTPUT_DIALOG").apply { component = ComponentName("com.android.systemui", "com.android.systemui.media.dialog.MediaOutputDialogReceiver") }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            pendingIntent.send()
        } catch (e: Throwable) {}
    }

    // 🚀 BULLETPROOF TRACKING: Will tell us exactly why the island is hiding!
    private fun evaluatePriority() {
        Log.d("Redwood", "--- Evaluating Priority ---")
        val isAlertCritical = transientModel?.isCritical == true
        val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        val blacklistedGames = prefs.getString("gaming_blacklist", "com.dts.freefiremax,com.tencent.ig") ?: ""
        
        val rotation = try { windowManager?.defaultDisplay?.rotation ?: 0 } catch(e:Throwable){0}
        val isLandscapeNow = rotation == android.view.Surface.ROTATION_90 || rotation == android.view.Surface.ROTATION_270
        val isBlacklistedAppActive = topAppPackage.isNotEmpty() && blacklistedGames.contains(topAppPackage)
        
        Log.d("Redwood", "Landscape: $isLandscapeNow | App: $topAppPackage | GameActive: $isBlacklistedAppActive | Transient: ${transientModel != null}")

        if ((isLandscapeNow || isBlacklistedAppActive) && !isAlertCritical) {
            Log.d("Redwood", "HIDING: Landscape or Blacklisted Game is active.")
            _islandState.value = IslandState.HIDDEN
            return
        }
        
        if (transientModel != null) {
            userForceCollapsed = false
            if (currentHardware?.isGamingModeOn == true && transientModel is LiveActivityModel.Charging) {
                // Ignore popup
            } else if (transientModel is LiveActivityModel.SystemAlert || transientModel is LiveActivityModel.AppTimerWarning) {
                _activeModel.value = transientModel; _splitModel.value = null; _islandState.value = IslandState.TYPE_2_MID
            } else if (transientModel is LiveActivityModel.RealityPill) {
                _activeModel.value = transientModel; _splitModel.value = null; _islandState.value = IslandState.TYPE_1_MINI
            } else if (currentMedia?.isPlaying == true || currentMedia != null) {
                _activeModel.value = currentMedia; _splitModel.value = transientModel; _islandState.value = IslandState.TYPE_SPLIT
            } else {
                _activeModel.value = transientModel; _splitModel.value = null; _islandState.value = IslandState.TYPE_CUBE
            }
            Log.d("Redwood", "STATE SET (Transient): ${_islandState.value.name}")
            return
        }
        
        _splitModel.value = null
        if (_activeModel.value is LiveActivityModel.Dashboard) return
        
        if (currentMedia != null && isMediaEnabled) {
            _activeModel.value = currentMedia
            if (!userForceCollapsed && (_islandState.value == IslandState.HIDDEN || _islandState.value == IslandState.TYPE_0_RING || _islandState.value == IslandState.TYPE_CUBE || _islandState.value == IslandState.TYPE_SPLIT)) {
                _islandState.value = IslandState.TYPE_1_MINI
            }
            
            if (userForceCollapsed && _islandState.value != IslandState.TYPE_0_RING) {
                _islandState.value = IslandState.TYPE_0_RING
            }
            Log.d("Redwood", "STATE SET (Media): ${_islandState.value.name}")
            return
        }
        
        _activeModel.value = null
        _islandState.value = IslandState.TYPE_0_RING
        Log.d("Redwood", "STATE SET (Idle): TYPE_0_RING")
    }

    fun postTransientNotification(model: LiveActivityModel, durationMs: Long = 5000L) {
        transientJob?.cancel(); transientModel = model; evaluatePriority()
        transientJob = scope.launch { delay(durationMs); transientModel = null; evaluatePriority() }
    }

    private fun setupMediaListener() {
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, null)
            updateActiveMediaController(getBestMediaController(mediaSessionManager.getActiveSessions(null)))
        } catch (e: Throwable) { Log.e("Redwood", "Failed to attach media listener", e) }
    }

    private fun updateActiveMediaController(controller: MediaController?) {
        activeMediaController?.unregisterCallback(mediaCallback); activeMediaController = controller
        if (controller == null || !isMediaEnabled) { currentMedia = null; stopMediaTicker(); evaluatePriority(); return }
        controller.registerCallback(mediaCallback); extractMediaData(controller)
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) { extractMediaData(activeMediaController) }
        override fun onMetadataChanged(metadata: MediaMetadata?) { extractMediaData(activeMediaController) }
    }

    private fun getScaledBitmap(bitmap: Bitmap?, maxDim: Int = 400): Bitmap? {
        if (bitmap == null) return null
        try {
            val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            if (ratio >= 1.0f) return bitmap
            return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } catch(e: Throwable) { return null }
    }

    private fun extractMediaData(controller: MediaController?) {
        if (controller == null || !isMediaEnabled) return
        val metadata = controller.metadata
        val pbState = controller.playbackState ?: return

        val isPlaying = pbState.state == PlaybackState.STATE_PLAYING
        val wasPlaying = currentMedia?.isPlaying == true
        if (!isPlaying && currentMedia == null) return 

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val rawAlbumArt = try { metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART) } catch (e: Throwable) { null }
        val albumArtBitmap = getScaledBitmap(rawAlbumArt)
        if (rawAlbumArt != null && rawAlbumArt != albumArtBitmap) { try { rawAlbumArt.recycle() } catch(e:Throwable){} }

        scope.launch(Dispatchers.IO) {
            var bgColor: Int? = null; var txtColor: Int = android.graphics.Color.WHITE
            if (albumArtBitmap != null && !albumArtBitmap.isRecycled) {
                try {
                    val palette = Palette.from(albumArtBitmap).generate()
                    val swatch = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch
                    if (swatch != null) {
                        bgColor = swatch.rgb
                        txtColor = if (androidx.core.graphics.ColorUtils.calculateLuminance(bgColor) > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    }
                } catch (e: Throwable) {}
            }

            withContext(Dispatchers.Main) {
                val extractedActions = pbState.customActions.map { CustomMediaAction(it.action, null, null, true) }
                currentMedia = LiveActivityModel.Music(
                    id = "media_main", title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
                    artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
                    albumArt = albumArtBitmap, blurredAlbumArt = null, appIcon = null, dominantColor = bgColor, titleTextColor = txtColor,
                    isPlaying = isPlaying, durationMs = duration, positionMs = pbState.position,
                    appPackageName = controller.packageName, customActions = extractedActions
                )

                if (isPlaying && !wasPlaying) { userForceCollapsed = false; pauseFadeJob?.cancel() }
                if (isPlaying) { startMediaTicker() } else {
                    stopMediaTicker()
                    if (wasPlaying) {
                        pauseFadeJob?.cancel()
                        pauseFadeJob = scope.launch { delay(3000); userForceCollapsed = true; evaluatePriority() }
                    }
                }
                evaluatePriority()
            }
        }
    }

    private fun startMediaTicker() {
        if (!isScreenOn || !isMediaEnabled) return 
        mediaTickerJob?.cancel()
        mediaTickerJob = scope.launch { 
            while (isActive) { 
                try {
                    activeMediaController?.playbackState?.position?.let { pos -> 
                        (activeModel.value as? LiveActivityModel.Music)?.let { islandView?.updateTicker(pos) }
                    }
                } catch(e: Throwable){}
                delay(1000) 
            } 
        }
    }
    private fun stopMediaTicker() { mediaTickerJob?.cancel() }

    private fun startHardwareMonitor() {
        hardwareMonitorJob?.cancel()
        hardwareMonitorJob = scope.launch { HardwareMonitors.startMonitoring().collect { hw -> currentHardware = hw; evaluatePriority() } }
    }

    private fun setupHardwareMonitor() {
        val filter = IntentFilter().apply { 
            addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) 
            addAction("com.example.dynamicisland.APP_CHANGED")
        }
        context.registerReceiver(screenStateReceiver, filter)
        context.registerComponentCallbacks(componentCallbacks)
        
        BatteryPlugin.onBatteryChanged = { level, isCharging, _ ->
             if (isChargingEnabled) {
                 if (isCharging && !wasCharging) { postTransientNotification(LiveActivityModel.Charging(id = "sys_battery", level = level, isPluggedIn = true, isTransient = true), 4000L)
                 } else if (!isCharging && lastReportedBattery != -1 && level < lastReportedBattery) {
                     if (level == 20 || level == 10 || level == 5) postTransientNotification(LiveActivityModel.Charging(id = "sys_battery_low", level = level, isPluggedIn = false, isTransient = true).copy(type = ActivityType.BATTERY_LOW), 6000L)
                 }
             }
             wasCharging = isCharging; lastReportedBattery = level
             islandView?.updateBattery(level, isCharging)
        }
        BatteryPlugin.start(context)
        startHardwareMonitor()  
    }
    init { setupHardwareMonitor(); setupMediaListener() }
}
