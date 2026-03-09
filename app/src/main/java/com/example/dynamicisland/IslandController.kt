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
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

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
    private var isLandscape = false

    private val gestureMatrix = mutableMapOf<String, IslandAction>()

    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeMediaController: MediaController? = null
    private var mediaTickerJob: Job? = null

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers -> updateActiveMediaController(controllers?.firstOrNull()) }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> { isScreenOn = false; stopMediaTicker() }
                Intent.ACTION_SCREEN_ON -> { isScreenOn = true; if (currentMedia?.isPlaying == true) startMediaTicker() }
            }
        }
    }

    // 🚀 NEW: THE ECOSYSTEM BRIDGE (Listens to your Battery Manager module)
    // Global Config State in Memory
    private var activeAppTimers = mapOf<String, Long>()
    private var exemptedApps = setOf<String>()

    // 🚀 THE MASTER ECOSYSTEM RECEIVER
    private val ecosystemReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.crdroid.batterywellbeing.SYSTEM_OVERRIDE" -> {
                    when (intent.getStringExtra("action")) {
                        "SMART_CHARGE_LIMIT" -> {
                            val level = intent.getIntExtra("level", 100)
                            postTransientNotification(LiveActivityModel.Charging("sys_smart_charge", ActivityType.CHARGING, level, true, true), 6000L)
                        }
                        "THERMAL_WARNING" -> {
                            val temp = intent.getStringExtra("extra_info") ?: "High"
                            val alert = LiveActivityModel.SystemAlert(
                                id = "sys_thermal", alertType = "THERMAL", title = "Thermal Throttling",
                                message = "Battery temperature at $temp", alertColor = android.graphics.Color.RED
                            )
                            postTransientNotification(alert, 6000L)
                        }
                        "ROGUE_APP_DETECTED" -> {
                            val appName = intent.getStringExtra("extra_info") ?: "Unknown App"
                            val alert = LiveActivityModel.SystemAlert(
                                id = "sys_rogue", alertType = "ROGUE", title = "High Background Drain",
                                message = "$appName is draining battery", alertColor = android.graphics.Color.rgb(255, 165, 0) // Orange
                            )
                            postTransientNotification(alert, 6000L)
                        }
                    }
                }
                // 1. HARDWARE ALERTS
                "com.crdroid.batterywellbeing.SYSTEM_ALERT" -> {
                    val alertType = intent.getStringExtra("alertType") ?: "INFO"
                    val title = intent.getStringExtra("title") ?: "System Alert"
                    val message = intent.getStringExtra("message") ?: ""
                    val colorHex = intent.getStringExtra("colorHex") ?: "#FFFFFF"

                    val colorInt = try {
                        android.graphics.Color.parseColor(colorHex)
                    } catch(e: Exception) {
                        android.graphics.Color.WHITE
                    }

                    val alert = LiveActivityModel.SystemAlert(
                        id = "sys_alert_$alertType", alertType = alertType,
                        title = title, message = message, alertColor = colorInt
                    )
                    postTransientNotification(alert, 5000L)
                }

                // 2. THE EXECUTIONER WARNING (60s)
                "com.crdroid.batterywellbeing.WARNING_1_MINUTE_REMAINING" -> {
                    val pkg = intent.getStringExtra("package_name") ?: return
                    val providedAppName = intent.getStringExtra("app_name")

                    // Thread-safe Icon Extraction
                    scope.launch(Dispatchers.IO) {
                        val pm = context.packageManager
                        var appName = providedAppName ?: pkg
                        var appIcon: Bitmap? = null

                        try {
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            if (providedAppName == null) appName = pm.getApplicationLabel(appInfo).toString()

                            val drawable = pm.getApplicationIcon(appInfo)
                            val bmp = Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bmp)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                            appIcon = getScaledBitmap(bmp, 150)
                        } catch (e: Exception) {}

                        val warningModel = LiveActivityModel.AppTimerWarning(
                            packageName = pkg, appName = appName,
                            appIcon = appIcon, targetTimeMs = System.currentTimeMillis() + 60000L
                        )

                        withContext(Dispatchers.Main) {
                            postTransientNotification(warningModel, 60000L) // Locks open for 60s
                        }
                    }
                }

                // 3. THE REALITY PILL TICK
                "com.crdroid.batterywellbeing.REALITY_PILL_TICK" -> {
                    val appName = intent.getStringExtra("app_name") ?: "App"
                    val sessionMinutes = intent.getIntExtra("session_minutes", 0)

                    // If it's exempted (like ArchiveAll running a background compression), ignore the tick
                    if (exemptedApps.contains(appName)) return

                    val pillModel = LiveActivityModel.RealityPill(appName = appName, sessionMinutes = sessionMinutes)
                    // Brief 3-second popup
                    postTransientNotification(pillModel, 3000L)
                }

                // 4. GLOBAL CONFIG SYNC
                "com.crdroid.batterywellbeing.SYNC_CONFIG" -> {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val timersJson = intent.getStringExtra("timers_json") ?: "{}"
                            val json = org.json.JSONObject(timersJson)
                            val newTimers = mutableMapOf<String, Long>()
                            json.keys().forEach { newTimers[it] = json.getLong(it) }

                            val exemptionsCsv = intent.getStringExtra("exemptions_csv") ?: ""
                            val newExemptions = exemptionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

                            activeAppTimers = newTimers
                            exemptedApps = newExemptions
                        } catch (e: Exception) {}
                    }
                }
            }
        }
    }

    private val componentCallbacks = object : android.content.ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
            isLandscape = newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape) _islandState.value = IslandState.HIDDEN else evaluatePriority()
        }
        
        @Suppress("DEPRECATION") // 🚀 FIX: Acknowledge the deprecated override
        override fun onLowMemory() {
            // This is left empty as required by the interface
        }
    }

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams): android.view.View {
        val moduleContext = try { context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) } catch (e: Exception) { context }
        val view = DynamicIslandView(context, moduleContext)
        view.windowManager = wm
        view.windowParams = params

        view.onGestureSettingsUpdated = { payload ->
            try {
                if (payload != null) {
                    val json = JSONObject(payload)
                    gestureMatrix.clear()
                    json.keys().forEach { key -> gestureMatrix[key] = IslandAction.valueOf(json.getString(key)) }
                }
            } catch (e: Exception) {}
        }

        view.onGestureEvent = { gesture ->
            val currentState = _islandState.value.name
            val actionKey = "${currentState}_${gesture.name}"
            val action = gestureMatrix[actionKey] ?: getDefaultAction(currentState, gesture)

            when (action) {
                IslandAction.PLAY_PAUSE -> { if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY") }
                IslandAction.NEXT_TRACK -> sendMediaCommand("NEXT")
                IslandAction.PREV_TRACK -> sendMediaCommand("PREV")
                IslandAction.VOL_UP -> adjustVolume(android.media.AudioManager.ADJUST_RAISE)
                IslandAction.VOL_DOWN -> adjustVolume(android.media.AudioManager.ADJUST_LOWER)
                IslandAction.EXPAND -> {
                    userForceCollapsed = false
                    _islandState.value = when (_islandState.value) {
                        IslandState.TYPE_0_RING -> IslandState.TYPE_1_MINI
                        IslandState.TYPE_1_MINI -> IslandState.TYPE_2_MID
                        IslandState.TYPE_2_MID -> IslandState.TYPE_3_MAX
                        else -> _islandState.value
                    }
                }
                IslandAction.COLLAPSE -> {
                    userForceCollapsed = true
                    _islandState.value = when (_islandState.value) {
                        IslandState.TYPE_3_MAX -> IslandState.TYPE_2_MID
                        IslandState.TYPE_2_MID -> IslandState.TYPE_1_MINI
                        IslandState.TYPE_1_MINI -> { if (_activeModel.value is LiveActivityModel.Dashboard) _activeModel.value = null; IslandState.TYPE_0_RING }
                        else -> IslandState.TYPE_0_RING
                    }
                }
                IslandAction.OPEN_APP -> {
                    val model = _activeModel.value
                    if (model is LiveActivityModel.Music && model.appPackageName.isNotEmpty()) {
                        try { val launchIntent = context.packageManager.getLaunchIntentForPackage(model.appPackageName); launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK); launchIntent?.let { context.startActivity(it) } } catch (e: Exception) {}
                    }
                }
                IslandAction.HEART_SONG -> {
                    val heartAction = currentMedia?.customActions?.find { it.actionName.contains("heart", true) || it.actionName.contains("favorite", true) || it.actionName.contains("like", true) }
                    if (heartAction != null) activeMediaController?.transportControls?.sendCustomAction(heartAction.actionName, null)
                    else activeMediaController?.transportControls?.setRating(Rating.newHeartRating(true))
                }
                IslandAction.NONE, IslandAction.TOGGLE_TORCH -> {} 
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

    // 🚀 RESTORED: The missing media command helper!
    private fun sendMediaCommand(command: String) {
        val controls = activeMediaController?.transportControls ?: return
        when (command) { 
            "PLAY" -> controls.play()
            "PAUSE" -> controls.pause()
            "NEXT" -> controls.skipToNext()
            "PREV" -> controls.skipToPrevious() 
        } 
    }

    private fun getDefaultAction(state: String, gesture: IslandGesture): IslandAction {
        return when (gesture) {
            IslandGesture.SINGLE_TAP -> if (state == "TYPE_0_RING" || state == "HIDDEN") IslandAction.EXPAND else IslandAction.NONE
            IslandGesture.SWIPE_DOWN -> IslandAction.EXPAND
            IslandGesture.SWIPE_UP -> IslandAction.COLLAPSE
            IslandGesture.SWIPE_LEFT -> IslandAction.NEXT_TRACK
            IslandGesture.SWIPE_RIGHT -> IslandAction.PREV_TRACK
            IslandGesture.LONG_PRESS -> IslandAction.OPEN_APP
            IslandGesture.DOUBLE_TAP -> IslandAction.PLAY_PAUSE
        }
    }

    private fun adjustVolume(direction: Int) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, direction, android.media.AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) {}
    }

    private fun launchAudioOutputSwitcher() {
        try {
            val intent = Intent("com.android.systemui.action.LAUNCH_SYSTEM_MEDIA_OUTPUT_DIALOG").apply { component = ComponentName("com.android.systemui", "com.android.systemui.media.dialog.MediaOutputDialogReceiver") }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            pendingIntent.send()
        } catch (e: Exception) {}
    }

    private fun evaluatePriority() {
        if (isLandscape) { _islandState.value = IslandState.HIDDEN; return }
        
        if (transientModel != null) {
            if (transientModel is LiveActivityModel.SystemAlert || transientModel is LiveActivityModel.AppTimerWarning) {
                // System Alerts (Text) demand the Mid Pill, temporarily overriding everything else
                _activeModel.value = transientModel
                _splitModel.value = null
                _islandState.value = IslandState.TYPE_2_MID
            } else if (currentMedia?.isPlaying == true || currentMedia != null) {
                // Charging while media playing = Split Cube
                _activeModel.value = currentMedia
                _splitModel.value = transientModel
                _islandState.value = IslandState.TYPE_SPLIT
            } else {
                // Charging while idle = Tiny Cube
                _activeModel.value = transientModel
                _splitModel.value = null
                _islandState.value = IslandState.TYPE_CUBE
            }
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
        try { mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, ComponentName(context, "com.example.dynamicisland.DummyListener")); updateActiveMediaController(mediaSessionManager.getActiveSessions(ComponentName(context, "com.example.dynamicisland.DummyListener")).firstOrNull()) } catch (e: Exception) {}
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

        // 🚀 OOM BOMB PROTECTION
        val rawAlbumArt = try {
            metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        } catch (e: OutOfMemoryError) {
            Log.e("DynamicIsland", "FATAL: Prevented SystemUI OOM Crash from massive Album Art!")
            null
        } catch (e: Exception) {
            null
        }

        val albumArtBitmap = getScaledBitmap(rawAlbumArt)
        
        var appIconBitmap: Bitmap? = null
        try { 
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(controller.packageName)
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
                    (activeModel.value as? LiveActivityModel.Music)?.let { 
                        context.sendBroadcast(Intent("com.example.dynamicisland.TICKER_UPDATE").putExtra("pos", pos))
                    }
                }
                delay(1000) 
            } 
        }
    }
    private fun stopMediaTicker() { mediaTickerJob?.cancel() }

    private fun setupHardwareMonitor() {
        val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
        context.registerReceiver(screenStateReceiver, filter)
        context.registerComponentCallbacks(componentCallbacks)
        
        // 🚀 Register Ecosystem Bridge
        val ecoFilter = IntentFilter().apply {
            addAction("com.crdroid.batterywellbeing.SYSTEM_OVERRIDE")
            addAction("com.crdroid.batterywellbeing.SYSTEM_ALERT")
            addAction("com.crdroid.batterywellbeing.WARNING_1_MINUTE_REMAINING")
            addAction("com.crdroid.batterywellbeing.REALITY_PILL_TICK")
            addAction("com.crdroid.batterywellbeing.SYNC_CONFIG")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(ecosystemReceiver, ecoFilter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(ecosystemReceiver, ecoFilter)
        }

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
        BatteryPlugin.stop(context)
        try { mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener) } catch(e: Exception){} 
        try { context.unregisterReceiver(ecosystemReceiver) } catch(e: Exception){} // 🚀 Clean up Bridge
    }
}
