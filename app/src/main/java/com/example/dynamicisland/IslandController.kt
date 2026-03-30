@file:Suppress("DEPRECATION")
package com.example.dynamicisland

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import android.util.LruCache

class IslandController(private val context: Context) {

    private val storageManager = IslandStorageManager(context)
    private val clipboardManager = IslandClipboardManager(context, scope) { copiedText ->
        // When text is copied, show it in the Island temporarily
        if (isAlertsEnabled) {
            postTransientNotification(
                LiveActivityModel.General(
                    id = "sys_clipboard",
                    type = ActivityType.MESSAGE,
                    title = "Copied to Clipboard",
                    dataText = copiedText,
                    accentColor = android.graphics.Color.CYAN
                ), 
                4000L
            )
        }
    }
    
    private var downloadSpeedJob: Job? = null
    private var lastRxBytes = 0L

    private val notificationManager = IslandNotificationManager(context, scope,
        onProgressCaught = { progressModel ->
            // Start the speed tracker if it isn't running
            if (downloadSpeedJob?.isActive != true) {
                lastRxBytes = android.net.TrafficStats.getTotalRxBytes()
                downloadSpeedJob = scope.launch(Dispatchers.Main) {
                    while (isActive) {
                        delay(1000) // Calculate every 1 second
                        val currentRx = android.net.TrafficStats.getTotalRxBytes()
                        val bytesPerSec = currentRx - lastRxBytes
                        lastRxBytes = currentRx
                        
                        val speedStr = if (bytesPerSec > 1048576) {
                            String.format("%.1f MB/s", bytesPerSec / 1048576f)
                        } else {
                            String.format("%d KB/s", bytesPerSec / 1024)
                        }

                        // Update the active download model with the new speed
                        val current = _activeModel.value
                        if (current is LiveActivityModel.OngoingTask) {
                            _activeModel.value = current.copy(networkSpeed = speedStr)
                        }
                    }
                }
            }

            _activeModel.value = progressModel
            if (_islandState.value == IslandState.TYPE_0_RING) _islandState.value = IslandState.TYPE_1_MINI
            evaluatePriority()
        },
        onDownloadFinished = {
            downloadSpeedJob?.cancel()
        }
    )

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // 🚀 FIXED: Change from Pair<String, String> to QSTileState
    private var qsTilesCache = listOf<QSTileState>() 
    private var pinnedAppsCache = listOf<String>()
    
    // 🎛️ The Clean Helper Architecture
    private val hardwareManager = IslandHardwareManager(context, audioManager, scope)
    private val actionManager = IslandActionManager(context, scope)
    private val callManager = IslandCallManager(context, audioManager) { newCall -> 
        currentCall = newCall
        evaluatePriority()
    }
    
    // 🛑 3-Tier Sleep Protocol Hardware Monitor
    private val hardwareMonitor = IslandHardwareMonitor(scope) { newHw -> 
        currentHardware = newHw
        evaluatePriority() 
    }
    
    private val mediaManager = IslandMediaManager(context, scope,
        onMediaChanged = { newMedia -> currentMedia = newMedia; evaluatePriority() },
        onMediaTick = { pos -> islandView?.updateTicker(pos) },
        onPeekRequested = { 
            if (userForceCollapsed && !isPeeking) {
                isPeeking = true; userForceCollapsed = false
                scope.launch { delay(3000); userForceCollapsed = true; isPeeking = false; evaluatePriority() }
            }
        },
        onPauseFadeRequested = {
            pauseFadeJob?.cancel()
            pauseFadeJob = scope.launch { delay(3000); userForceCollapsed = true; evaluatePriority() }
        },
        onUncollapseRequested = {
            userForceCollapsed = false; pauseFadeJob?.cancel()
        }
    )

    private var windowManager: WindowManager? = null
    private var islandView: DynamicIslandView? = null

    // State Flows
    private val _islandState = MutableStateFlow(IslandState.TYPE_0_RING)
    val islandState = _islandState.asStateFlow()
    private val _activeModel = MutableStateFlow<LiveActivityModel?>(null)
    val activeModel = _activeModel.asStateFlow()
    private val _splitModel = MutableStateFlow<LiveActivityModel?>(null) 
    val splitModel = _splitModel.asStateFlow()

    // Activity Trackers
    private var currentCall: LiveActivityModel.Call? = null
    private var currentMedia: LiveActivityModel.Music? = null
    private var currentHardware: LiveActivityModel.HardwareMonitor? = null
    private var transientModel: LiveActivityModel? = null
    
    // System Trackers
    private var transientJob: Job? = null
    private var pauseFadeJob: Job? = null
    
    private var userForceCollapsed = false 
    private var lastReportedBattery = -1
    private var wasCharging = false 
    private var topAppPackage = "" 
    private var isPeeking = false
    private var isPanelExpanded = false // ⬅️ FIXED: Added missing Panel tracker

    private var isChargingEnabled = true
    private var isAlertsEnabled = true
    private var isTimersEnabled = true
    private val gestureMatrix = mutableMapOf<String, IslandAction>()

    private val brightnessObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { super.onChange(selfChange); hardwareManager.updateBrightnessState(islandView) }
    }

    private val hardwareSyncReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") hardwareManager.updateVolumeState(islandView)
        }
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> { 
                    mediaManager.isScreenOn = false
                    hardwareMonitor.isScreenOn = false 
                }
                Intent.ACTION_SCREEN_ON -> { 
                    mediaManager.isScreenOn = true
                    hardwareMonitor.isScreenOn = true 
                    evaluatePriority() 
                }
            }
        }
    }

    private val iconCache = object : LruCache<String, Bitmap>(10 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val ecosystemReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.example.dynamicisland.HARDWARE_TOGGLE" -> {
                    if (!isAlertsEnabled) return
                    val type = intent.getStringExtra("type") ?: return
                    val state = intent.getIntExtra("state", 0)
                    
                    if (type == "RINGER") {
                        val (title, text, color) = when (state) {
                            0 -> Triple("Silent Mode", "Calls and notifications muted", android.graphics.Color.GRAY)
                            1 -> Triple("Vibrate", "Device will vibrate", android.graphics.Color.rgb(255, 165, 0)) // Orange
                            else -> Triple("Ring", "Ringer is active", android.graphics.Color.GREEN)
                        }
                        postTransientNotification(LiveActivityModel.General(
                            id = "hw_ringer", type = ActivityType.HARDWARE,
                            title = title, dataText = text, accentColor = color
                        ), 3000L)
                        
                    } else if (type == "TORCH") {
                        val title = if (state == 1) "Flashlight On" else "Flashlight Off"
                        val text = if (state == 1) "System torch active" else "Torch disabled"
                        val color = if (state == 1) android.graphics.Color.YELLOW else android.graphics.Color.GRAY
                        
                        postTransientNotification(LiveActivityModel.General(
                            id = "hw_torch", type = ActivityType.HARDWARE,
                            title = title, dataText = text, accentColor = color
                        ), 3000L)
                    }
                }
                "com.example.dynamicisland.SCREENSHOT_CAUGHT" -> {
                    if (!isAlertsEnabled) return
                    val uriString = intent.getStringExtra("uri") ?: return
                    postTransientNotification(
                        LiveActivityModel.General(
                            id = "sys_screenshot",
                            type = ActivityType.MESSAGE,
                            title = "Screenshot Saved",
                            dataText = "Tap to view or share",
                            accentColor = android.graphics.Color.WHITE
                        ), 
                        4000L
                    )
                }
                "com.example.dynamicisland.PANEL_STATE_CHANGED" -> {
                    isPanelExpanded = intent.getBooleanExtra("isExpanded", false)
                    evaluatePriority()
                }
                "com.example.dynamicisland.ALARM_SET" -> {
                    if (!isAlertsEnabled) return
                    val triggerTimeMs = intent.getLongExtra("triggerTime", 0L)
                    if (triggerTimeMs > System.currentTimeMillis()) {
                        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                        val timeStr = sdf.format(java.util.Date(triggerTimeMs))
                        postTransientNotification(LiveActivityModel.General(
                            id = "sys_alarm", type = ActivityType.ALARM, title = "Alarm Set",
                            dataText = "Ringing at $timeStr", accentColor = android.graphics.Color.CYAN
                        ), 3500L)
                    }
                }
                "com.example.dynamicisland.APP_CHANGED" -> { topAppPackage = intent.getStringExtra("pkg") ?: ""; evaluatePriority() }
                "com.example.dynamicisland.LIVE_ACTIVITY_CAUGHT" -> {
                    val progress = intent.getIntExtra("progress", -1)
                    val progressMax = intent.getIntExtra("progressMax", -1)
                    if (progress != -1 && progressMax > 0) {
                        postTransientNotification(LiveActivityModel.OngoingTask(pkgName = intent.getStringExtra("pkg") ?: "", title = intent.getStringExtra("title") ?: "", text = intent.getStringExtra("text") ?: "", progress = progress, progressMax = progressMax), 4000L)
                    }
                }
                "com.crdroid.batterywellbeing.SYSTEM_OVERRIDE" -> {
                    if (!isAlertsEnabled) return
                    when (intent.getStringExtra("action")) {
                        "SMART_CHARGE_LIMIT" -> postTransientNotification(LiveActivityModel.Charging("sys_smart_charge", ActivityType.CHARGING, intent.getIntExtra("level", 100), true, true), 6000L)
                        "THERMAL_WARNING" -> postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_thermal", alertType = "THERMAL", title = "Thermal Throttling", message = "Battery temp at ${intent.getStringExtra("extra_info") ?: "High"}", alertColor = android.graphics.Color.RED), 6000L)
                        "ROGUE_APP_DETECTED" -> postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_rogue", alertType = "ROGUE", title = "High Battery Drain", message = "${intent.getStringExtra("extra_info") ?: "Unknown App"} is draining battery", alertColor = android.graphics.Color.rgb(255, 165, 0)), 6000L)
                    }
                }
                "com.crdroid.batterywellbeing.SYSTEM_ALERT" -> {
                    if (!isAlertsEnabled) return
                    val colorHex = intent.getStringExtra("colorHex") ?: "#FFFFFF"
                    val colorInt = try { android.graphics.Color.parseColor(colorHex) } catch(e: Exception) { android.graphics.Color.WHITE }
                    postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_alert_${intent.getStringExtra("alertType") ?: "INFO"}", alertType = intent.getStringExtra("alertType") ?: "INFO", title = intent.getStringExtra("title") ?: "System Alert", message = intent.getStringExtra("message") ?: "", alertColor = colorInt), 5000L)
                }
                "com.crdroid.batterywellbeing.SYNC_CONFIG" -> {
                     // Keep your battery sync if it's there
                }
                "com.example.dynamicisland.SYNC_CONFIG" -> {
                    val type = intent.getStringExtra("type")
                    if (type == "dashboard") {
                        val payload = intent.getStringExtra("payload") ?: return
                        try {
                            val json = org.json.JSONObject(payload)
                            val appsArr = json.optJSONArray("pinned_apps")
                            val tilesArr = json.optJSONArray("qs_tiles")
                            
                            val appsList = mutableListOf<String>()
                            if (appsArr != null) for (i in 0 until appsArr.length()) appsList.add(appsArr.getString(i))
                            
                            // 🚀 FIXED: Provide the default initial states for the tiles
                            val tilesList = mutableListOf<QSTileState>()
                            if (tilesArr != null) {
                                for (i in 0 until tilesArr.length()) {
                                    val obj = tilesArr.getJSONObject(i)
                                    tilesList.add(
                                        QSTileState(
                                            tileSpec = obj.getString("spec"), 
                                            label = obj.getString("label"),
                                            isActive = false,
                                            isUnavailable = false
                                        )
                                    )
                                }
                            }
                            
                            pinnedAppsCache = appsList
                            qsTilesCache = tilesList
                            
                            if (_activeModel.value is LiveActivityModel.Dashboard) {
                                _activeModel.value = LiveActivityModel.Dashboard(activeTiles = qsTilesCache, pinnedApps = pinnedAppsCache)
                            }
                        } catch (e: Throwable) {}
                    }
                }
            }
        }
    }

    private val componentCallbacks = object : android.content.ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) { evaluatePriority() }
        @Suppress("OVERRIDE_DEPRECATION") override fun onLowMemory() {}
        override fun onTrimMemory(level: Int) { if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) iconCache.evictAll() }
    }

    private fun evaluatePriority() {
        // ⬅️ FIXED: Passed isPanelExpanded parameter correctly
        userForceCollapsed = IslandPriorityEngine.evaluatePriority(
            context = context,
            windowManager = windowManager,
            topAppPackage = topAppPackage,
            isPanelExpanded = isPanelExpanded, 
            currentCall = currentCall,
            transientModel = transientModel,
            currentMedia = currentMedia,
            currentHardware = currentHardware,
            isMediaEnabled = mediaManager.isMediaEnabled,
            userForceCollapsed = userForceCollapsed,
            currentActiveModel = _activeModel.value,
            currentVisualState = _islandState.value,
            _activeModel = _activeModel,
            _splitModel = _splitModel,
            _islandState = _islandState
        )
        // 🧠 FEATURE: Smart Focus Mode
        val productivityApps = listOf("com.notion.id", "com.microsoft.teams", "com.google.android.apps.docs")
        if (productivityApps.contains(topAppPackage)) {
            // Check if we are already in a focus session to prevent spamming
            if (_activeModel.value !is LiveActivityModel.RealityPill) {
                _activeModel.value = LiveActivityModel.RealityPill(
                    appName = "Focus Mode", 
                    sessionMinutes = 0
                )
                _islandState.value = IslandState.TYPE_1_MINI
                
                // Silently enable DND via AudioManager
                try {
                    if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    }
                } catch (e: Throwable) {}
            }
        }
    }

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams): android.view.View {
        val moduleContext = try { context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) } catch (e: Exception) { context }
        val view = DynamicIslandView(context, moduleContext)
        this.islandView = view 
        this.windowManager = wm 

        // 🏓 THE PING: Actively wake up the Config App and demand the latest settings
        scope.launch {
            delay(500) // Wait half a second for the View to finish attaching
            val requestIntent = Intent("com.example.dynamicisland.REQUEST_PREFS")
            requestIntent.setPackage("com.example.dynamicisland") // Force wake the app even if closed
            context.sendBroadcast(requestIntent)
        }

        view.onVolumeDrag = { pct -> hardwareManager.setSystemVolume(pct, view) }
        view.onBrightnessDrag = { pct -> hardwareManager.setSystemBrightness(pct, view) }
        view.onMicToggle = { hardwareManager.toggleMicMute() }
        view.onSpeakerToggle = { hardwareManager.toggleSpeakerphone() }
        view.onEndCallClick = { callManager.endActiveCall() }
        view.onOpenCallUI = { callManager.openNativeCallUI(view) }
        view.onAutoBrightnessToggle = { hardwareManager.toggleAutoBrightness(view) }
        view.onRingerToggle = { hardwareManager.toggleRingerMode(view) }
        // Wire up the new Direct Callbacks for Dashboard
        view.onAppPinnedClick = { pkg -> 
            actionManager.launchAppIntent(pkg) {
                userForceCollapsed = true; _islandState.value = IslandState.TYPE_0_RING; evaluatePriority()
            }
        }
        
        view.onQsTileClick = { tileSpec ->
            actionManager.handleQSTileClick(tileSpec) { newTiles ->
                // Optional: Update tile state visually if needed
            }
        }
        
        view.onSplitPillClick = { 
            if (_splitModel.value is LiveActivityModel.Charging) { 
                try {
                    val intent = Intent().setComponent(ComponentName("com.crdroid.batterywellbeing", "com.crdroid.batterywellbeing.MainActivity"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                } catch(e: Throwable) { _islandState.value = IslandState.TYPE_CUBE }
            } 
        }
        
        view.windowManager = wm
        view.windowParams = params
        view.updateRingerState(audioManager.ringerMode)

        view.onGestureSettingsUpdated = { payload ->
            try {
                val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
                mediaManager.isMediaEnabled = prefs.getBoolean("enable_media", true)
                isChargingEnabled = prefs.getBoolean("enable_charging", true)
                isAlertsEnabled = prefs.getBoolean("enable_alerts", true)
                isTimersEnabled = prefs.getBoolean("enable_timers", true)
                
                if (payload != null && payload.length < 5000) {
                    val json = JSONObject(payload)
                    gestureMatrix.clear()
                    json.keys().forEach { key -> try { if (key.startsWith("TYPE_")) { gestureMatrix[key] = IslandAction.valueOf(json.getString(key)) } } catch (e: Exception) {} }
                }
            } catch (e: Exception) {}
        }

        view.onGestureEvent = { gesture ->
            val currentState = _islandState.value.name
            val actionName = gestureMatrix["${currentState}_${gesture.name}"]?.name ?: "NONE"

            if (gesture.name.startsWith("QS_CLICK_")) {
                actionManager.handleQSTileClick(gesture.name) { newTiles ->
                    if (_activeModel.value is LiveActivityModel.Dashboard) _activeModel.value = LiveActivityModel.Dashboard(activeTiles = newTiles)
                }
            } else {
                when (actionName) {
                    "PLAY_PAUSE" -> { if (currentMedia?.isPlaying == true) mediaManager.sendMediaCommand("PAUSE") else mediaManager.sendMediaCommand("PLAY") }
                    "NEXT_TRACK" -> mediaManager.sendMediaCommand("NEXT")
                    "PREV_TRACK" -> mediaManager.sendMediaCommand("PREV")
                    "OPEN_DASHBOARD" -> {
                        if (_activeModel.value == null) _activeModel.value = LiveActivityModel.Dashboard()
                        _islandState.value = IslandState.TYPE_3_MAX
                    }
                    "OPEN_QUICK_TOGGLES" -> {
                        if (_activeModel.value == null || _activeModel.value is LiveActivityModel.Dashboard) {
                            // 🚀 FIXED: Pass the cached apps and tiles!
                            _activeModel.value = LiveActivityModel.Dashboard(activeTiles = qsTilesCache, pinnedApps = pinnedAppsCache)
                            _islandState.value = IslandState.TYPE_2_MID
                        }
                    }
                    "COLLAPSE" -> {
                        if (_activeModel.value is LiveActivityModel.Dashboard) {
                            if (currentMedia != null) {
                                if (currentMedia?.isPlaying == true) { _islandState.value = IslandState.TYPE_1_MINI; userForceCollapsed = false } 
                                else { _islandState.value = IslandState.TYPE_0_RING; userForceCollapsed = true }
                                _activeModel.value = currentMedia
                            } else {
                                _activeModel.value = null; _islandState.value = IslandState.TYPE_0_RING
                            }
                        } else {
                            userForceCollapsed = true; _islandState.value = IslandState.TYPE_0_RING
                        }
                        evaluatePriority()
                    }
                    "EXPAND" -> {
                        userForceCollapsed = false
                        when (_islandState.value) {
                            IslandState.TYPE_0_RING -> { 
                                if (currentMedia != null) _islandState.value = IslandState.TYPE_1_MINI 
                                else { _activeModel.value = LiveActivityModel.Dashboard(); _islandState.value = IslandState.TYPE_3_MAX }
                            }
                            IslandState.TYPE_1_MINI -> {
                                if (currentMedia?.isPlaying == false) { _activeModel.value = LiveActivityModel.Dashboard(); _islandState.value = IslandState.TYPE_3_MAX } 
                                else { _islandState.value = IslandState.TYPE_2_MID }
                            }
                            IslandState.TYPE_2_MID, IslandState.TYPE_SPLIT -> _islandState.value = IslandState.TYPE_3_MAX
                            else -> {}
                        }
                    }
                    "VOLUME_UP" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    "VOLUME_DOWN" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    "MUTE_TOGGLE" -> {
                        val isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (isMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                    }
                    "LAUNCH_SETTINGS" -> actionManager.launchAppIntent("com.android.settings") { userForceCollapsed = true; _islandState.value = IslandState.TYPE_0_RING; evaluatePriority() }
                    "LAUNCH_CAMERA" -> {
                        val cameraIntent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        try { context.startActivity(cameraIntent) } catch (e: Exception) {}
                    }
                    else -> { if (actionName.startsWith("LAUNCH_APP_")) actionManager.launchAppIntent(actionName.removePrefix("LAUNCH_APP_")) { userForceCollapsed = true; _islandState.value = IslandState.TYPE_0_RING; evaluatePriority() } }
                }
            }
        }
        
        view.onAudioOutputClick = { actionManager.launchAudioOutputSwitcher(currentMedia?.appPackageName) }
        view.onPlayPauseClick = { if (currentMedia?.isPlaying == true) mediaManager.sendMediaCommand("PAUSE") else mediaManager.sendMediaCommand("PLAY") }
        view.onPrevClick = { mediaManager.sendMediaCommand("PREV") }
        view.onNextClick = { mediaManager.sendMediaCommand("NEXT") }
        view.onSeekTo = { position -> mediaManager.activeMediaController?.transportControls?.seekTo(position) }
        view.onCustomMediaAction = { action -> mediaManager.activeMediaController?.transportControls?.sendCustomAction(action, null) }

        // 🛑 FIXED: Added 3-Tier Sleep Protocol Orchestrator
        scope.launch { 
            islandState.collect { state -> 
                view.setState(state) 
                val isVisible = state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING
                mediaManager.isIslandVisible = isVisible
                hardwareMonitor.isDashboardOpen = state == IslandState.TYPE_3_MAX 
            } 
        }
        scope.launch { activeModel.collect { model -> view.setModel(model) } }
        scope.launch { splitModel.collect { model -> view.setSplitModel(model) } }
        return view
    }

    private fun postTransientNotification(model: LiveActivityModel, durationMs: Long = 5000L) {
        transientJob?.cancel(); transientModel = model; evaluatePriority()
        transientJob = scope.launch { delay(durationMs); transientModel = null; evaluatePriority() }
    }

    // 🧹 Destroy Method for Memory Kill Switch
    fun destroy() {
        try { callManager.javaClass.getMethod("destroy").invoke(callManager) } catch (e: Exception) {}
        try { mediaManager.javaClass.getMethod("destroy").invoke(mediaManager) } catch (e: Exception) {}
    }
    
    init {
        hardwareManager.updateVolumeState(null)
        hardwareManager.updateBrightnessState(null)

        val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
        context.registerReceiver(screenStateReceiver, filter)
        context.registerComponentCallbacks(componentCallbacks)

        var isTorchOn = false
        val hardwareReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    "com.example.dynamicisland.ACTION_LAUNCH_APP" -> {
                        val pkg = intent.getStringExtra("pkg") ?: return
                        actionManager.launchAppIntent(pkg) { userForceCollapsed = true; _islandState.value = IslandState.TYPE_0_RING; evaluatePriority() }
                    }
                    "com.example.dynamicisland.ACTION_QS" -> {
                        val tile = intent.getStringExtra("tile") ?: return
                        try {
                            val settingsIntent = when (tile) {
                                "WiFi" -> Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                                "Bluetooth" -> Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                "Location" -> Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                "Airplane" -> Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                                "DND" -> Intent(android.provider.Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
                                "Battery Saver" -> Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
                                "Hotspot" -> Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                                "NFC" -> Intent(android.provider.Settings.ACTION_NFC_SETTINGS)
                                "Cast" -> Intent(android.provider.Settings.ACTION_CAST_SETTINGS)
                                "Settings" -> Intent(android.provider.Settings.ACTION_SETTINGS)
                                "Torch" -> {
                                    val cameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                                    val cameraId = cameraManager.cameraIdList[0]
                                    isTorchOn = !isTorchOn
                                    cameraManager.setTorchMode(cameraId, isTorchOn)
                                    null
                                }
                                else -> Intent(android.provider.Settings.ACTION_SETTINGS)
                            }
                            if (settingsIntent != null) {
                                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                actionManager.executeBackgroundIntent(settingsIntent) { userForceCollapsed = true; _islandState.value = IslandState.TYPE_0_RING; evaluatePriority() }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        }
        
        val hardwareFilter = android.content.IntentFilter().apply { addAction("com.example.dynamicisland.ACTION_LAUNCH_APP"); addAction("com.example.dynamicisland.ACTION_QS") }
        context.registerReceiver(hardwareReceiver, hardwareFilter, Context.RECEIVER_EXPORTED)
        
        callManager.startMonitoring()
        
        val volFilter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(hardwareSyncReceiver, volFilter)
        context.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true, brightnessObserver)

        // ⬅️ FIXED: Re-added PANEL_STATE_CHANGED and ALARM_SET 
        val ecoFilter = IntentFilter().apply {
            addAction("com.crdroid.batterywellbeing.SYSTEM_OVERRIDE"); addAction("com.crdroid.batterywellbeing.SYSTEM_ALERT")
            addAction("com.crdroid.batterywellbeing.WARNING_1_MINUTE_REMAINING"); addAction("com.crdroid.batterywellbeing.REALITY_PILL_TICK")
            addAction("com.crdroid.batterywellbeing.SYNC_CONFIG"); addAction("com.example.dynamicisland.APP_CHANGED")
            addAction("com.example.dynamicisland.LIVE_ACTIVITY_CAUGHT")
            addAction("com.example.dynamicisland.HARDWARE_TOGGLE")
            addAction("com.example.dynamicisland.PANEL_STATE_CHANGED") 
            addAction("com.example.dynamicisland.ALARM_SET") 
        }

        val securePermission = "com.redwood.permission.SECURE_IPC"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(ecosystemReceiver, ecoFilter, securePermission, null, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(ecosystemReceiver, ecoFilter, securePermission, null)
        }

        BatteryPlugin.onBatteryChanged = { level, isCharging, _ ->
             if (isChargingEnabled) {
                 if (isCharging && !wasCharging) {
                     postTransientNotification(LiveActivityModel.Charging(id = "sys_battery", level = level, isPluggedIn = true, isTransient = true), 4000L)
                 } else if (!isCharging) {
                     if (lastReportedBattery != -1 && level < lastReportedBattery) {
                         if (level == 20 || level == 10 || level == 5) {
                             postTransientNotification(LiveActivityModel.Charging(id = "sys_battery_low", level = level, isPluggedIn = false, isTransient = true, isCritical = true).copy(type = ActivityType.BATTERY_LOW), 6000L)
                         }
                     }
                 }
             }
             wasCharging = isCharging
             lastReportedBattery = level
             islandView?.updateBattery(level, isCharging)
        }
        BatteryPlugin.start(context)
        mediaManager.start()
        clipboardManager.startListening()
        
        // ⬅️ FIXED: Removed old dead `HardwareMonitors` loop
    }
}
