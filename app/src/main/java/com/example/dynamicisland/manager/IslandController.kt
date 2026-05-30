package com.example.dynamicisland.manager

import android.content.*
import android.content.ComponentName
import android.database.ContentObserver
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.view.WindowManager
import android.util.LruCache
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView
import com.example.dynamicisland.hook.*
import com.example.dynamicisland.util.ComposeLifecycleOwner
import com.example.dynamicisland.audio.AudioBeatDetector
import com.example.dynamicisland.achievements.AchievementManager
import com.example.dynamicisland.accessibility.IslandAccessibilityManager
import com.example.dynamicisland.sensors.ProximityWakeManager
import com.example.dynamicisland.privacy.ClipboardCleaner
import com.example.dynamicisland.settings.SettingsManager
import com.example.dynamicisland.settings.SettingsState
import com.example.dynamicisland.settings.SettingsViewModel
import com.example.dynamicisland.ipc.IslandState
import com.example.dynamicisland.gesture.IslandGesture
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

import com.example.dynamicisland.ui.mvi.IslandEventBus
import com.example.dynamicisland.ui.mvi.IslandIntent

@Singleton
class IslandController @Inject constructor(
    @ApplicationContext internal val context: Context,
    private val settingsManager: SettingsManager,
    val mediaManager: IslandMediaManager,
    private val hardwareMonitor: IslandHardwareMonitor,
    private val eventBus: IslandEventBus,
    private val hapticsManager: IslandHapticsManager,
    private val networkMonitor: IslandNetworkMonitor
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val activeExternalActivities = mutableMapOf<String, LiveActivityModel.ExternalActivity>()
    
    val currentGradientColors: List<Color> = listOf(Color(0xFF1E1E2E), Color(0xFF0A0A0A))
    val currentBrandColor: Color = Color.White
    
    // 🧠 SYSTEM CONTEXT DETECTION
    private val isSystemProcess = context.packageName == "com.android.systemui"

    var settingsState by mutableStateOf(com.example.dynamicisland.settings.SettingsState())
        private set

    private val audioBeatDetector by lazy { com.example.dynamicisland.audio.AudioBeatDetector() }
    private val achievementManager by lazy { com.example.dynamicisland.achievements.AchievementManager(context) }
    private val accessibilityManager by lazy { com.example.dynamicisland.accessibility.IslandAccessibilityManager(context) }
    private val proximityWakeManager by lazy { com.example.dynamicisland.sensors.ProximityWakeManager(context) }
    
    private val connectivityManager by lazy { 
        IslandConnectivityManager(context) { model ->
            val duration = when(model.title) {
                "Wi-Fi Connected" -> settingsState.wifiAlertDuration * 1000L
                "Bluetooth Connected" -> settingsState.btAlertDuration * 1000L
                "Hotspot Active" -> settingsState.hotspotAlertDuration * 1000L
                else -> 3000L
            }
            postTransientNotification(model, duration)
        }
    }

    fun loadAndApplySettings() {
        settingsState = settingsManager.getSettingsState()
        applySettings(settingsState)
    }

    private fun applySettings(state: com.example.dynamicisland.settings.SettingsState) {
        if (!isSystemProcess) return 

        mediaManager.isMediaEnabled = state.mediaArtworkBlur || state.waveformEnabled || state.nowPlaying
        mediaManager.allowedMusicApps = state.allowedMusicApps
        mediaManager.allowedMediaApps = state.allowedMediaApps
        
        callManager.userCallingApp = state.roleCallingApp.takeIf { it.isNotEmpty() }
        
        isChargingEnabled = state.magsafeChargingAnimation || state.batteryAwareAnimation
        isAlertsEnabled = state.otpDetection || state.linkIntercept || state.barcode || state.translation
        isTimersEnabled = state.timerIntegration

        if (state.bpmPulse || state.ambientReactiveRing) audioBeatDetector.start() else audioBeatDetector.stop()
        if (state.talkbackIntegration) accessibilityManager.start() else accessibilityManager.stop()
        if (state.proximityWake) proximityWakeManager.start() else proximityWakeManager.stop()
        
        if (state.ringDataVisible) {
            networkMonitor.startMonitoring(scope) { speed ->
                // Custom model for speed ring or update current model
            }
        } else networkMonitor.stopMonitoring()

        evaluatePriority()
    }
    
    fun swallowClipData(clipData: android.content.ClipData) {
        scope.launch {
            val saved = storageManager.swallowDroppedData(clipData)
            if (saved) {
                postTransientNotification(
                    LiveActivityModel.General(
                        id = "sys_stash", type = ActivityType.MESSAGE, title = "Item Stashed", 
                        dataText = "Added to Archive", accentColor = android.graphics.Color.GREEN
                    ), 3000L
                )
            }
        }
    }

    val stashHistory: StateFlow<List<com.example.dynamicisland.manager.StashedItem>> 
        get() = storageManager.stashHistory

    private fun triggerTransitionHaptic(newState: IslandState) {
        hapticsManager.triggerTransitionHaptic(newState, currentCall?.state, topAppPackage)
    }

    private val storageManager = IslandStorageManager(context)
    private val clipboardManager = IslandClipboardManager(context, scope) { copiedText ->
        if (isAlertsEnabled && isSystemProcess) {
            postTransientNotification(
                LiveActivityModel.General(
                    id = "sys_clipboard", type = ActivityType.MESSAGE, title = "Copied to Clipboard",
                    dataText = copiedText, accentColor = android.graphics.Color.CYAN
                ), 4000L
            )
        }
    }

    internal val notificationManager = IslandNotificationManager(context, scope,
        onProgressCaught = { progressModel ->
            _lastActiveModel = progressModel
            if (_lastIslandState == IslandState.TYPE_0_RING) _lastIslandState = IslandState.TYPE_1_MINI
            evaluatePriority()
        },
        onNavigationCaught = { navModel -> postTransientNotification(navModel, 5000L) },
        onNotificationStackCaught = { stackModel ->
            _lastActiveModel = stackModel
            if (_lastIslandState == IslandState.TYPE_0_RING) _lastIslandState = IslandState.TYPE_1_MINI
            evaluatePriority()
        }
    )

    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val hardwareManager = IslandHardwareManager(context, audioManager, scope)
    private val actionManager = IslandActionManager(context, scope)
    private val weatherManager = IslandWeatherManager(context, scope) { newWeather ->
        currentWeather = newWeather; evaluatePriority()
    }
    private val callManager = IslandCallManager(context, audioManager) { newCall ->
        currentCall = newCall; evaluatePriority()
    }

    private var windowManager: WindowManager? = null
    internal var islandView: DynamicIslandView? = null

    private var _lastIslandState = IslandState.TYPE_0_RING
    private var _lastActiveModel: LiveActivityModel? = null
    private var _lastSplitModel: LiveActivityModel? = null

    private var currentCall: LiveActivityModel.Call? = null
    private var currentMedia: LiveActivityModel.Music? = null
    private var currentWeather: LiveActivityModel.WeatherMood? = null
    var currentHardware: LiveActivityModel.HardwareMonitor? = null
    private var transientModel: LiveActivityModel? = null

    private var transientJob: Job? = null
    private var userForceCollapsed = false
    private var wasCharging = false
    private var topAppPackage = ""
    private var isPanelExpanded = false

    private var isChargingEnabled = true
    private var isAlertsEnabled = true
    private var isTimersEnabled = true

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> { mediaManager.isScreenOn = false; hardwareMonitor.isScreenOn = false }
                Intent.ACTION_SCREEN_ON -> { mediaManager.isScreenOn = true; hardwareMonitor.isScreenOn = true; evaluatePriority() }
            }
        }
    }

    private val iconCache = object : LruCache<String, Bitmap>(5 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val ecosystemReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.example.dynamicisland.RELOAD_PREFS" -> {
                    android.util.Log.d("IslandController", "Reloading Advanced Settings...")
                    loadAndApplySettings()
                }
                "com.example.dynamicisland.DEBUG_TEST" -> {
                    postTransientNotification(
                        LiveActivityModel.General(
                            id = "debug_test", type = ActivityType.MESSAGE,
                            title = "System Native Integration", dataText = "UI rendering confirmed.",
                            accentColor = android.graphics.Color.GREEN
                        ), 15000L
                    )
                }
                "com.example.dynamicisland.CALIBRATION_MODE" -> {
                    val enabled = intent.getBooleanExtra("enabled", false)
                    val target = intent.getStringExtra("target") ?: "ring"
                    islandView?.let { view ->
                        view.calibrationMode.value = enabled
                        view.calibrationTarget.value = target
                        if (enabled) {
                            val targetState = when(target.lowercase()) {
                                "mini" -> IslandState.TYPE_1_MINI
                                "mid" -> IslandState.TYPE_2_MID
                                "max" -> IslandState.TYPE_3_MAX
                                "cube" -> IslandState.TYPE_CUBE
                                else -> IslandState.TYPE_0_RING
                            }
                            _lastIslandState = targetState
                            eventBus.emit(IslandIntent.SyncState(targetState, _lastActiveModel, _lastSplitModel))
                        } else evaluatePriority()
                    }
                }
                "com.example.dynamicisland.CALIBRATION_UPDATE" -> {
                    val prefix = intent.getStringExtra("prefix") ?: return
                    islandView?.let { view ->
                        val w = intent.getFloatExtra("w", -1f); val h = intent.getFloatExtra("h", -1f)
                        val x = intent.getFloatExtra("x", -1f); val y = intent.getFloatExtra("y", -1f)
                        val r = intent.getFloatExtra("r", -1f)
                        when(prefix.lowercase()) {
                            "ring" -> { if (w >= 0) view.ringW.value = w; if (h >= 0) view.ringH.value = h; if (x != -1f) view.ringX.value = x; if (y != -1f) view.ringY.value = y; if (r >= 0) view.ringR.value = r }
                            "mini" -> { if (w >= 0) view.miniW.value = w; if (h >= 0) view.miniH.value = h; if (x != -1f) view.miniX.value = x; if (y != -1f) view.miniY.value = y; if (r >= 0) view.miniR.value = r }
                            "mid" -> { if (w >= 0) view.midW.value = w; if (h >= 0) view.midH.value = h; if (x != -1f) view.midX.value = x; if (y != -1f) view.midY.value = y; if (r >= 0) view.midR.value = r }
                            "max" -> { if (w >= 0) view.maxW.value = w; if (h >= 0) view.maxH.value = h; if (x != -1f) view.maxX.value = x; if (y != -1f) view.maxY.value = y; if (r >= 0) view.maxR.value = r }
                            "cube" -> { if (w >= 0) view.cubeW.value = w; if (h >= 0) view.cubeH.value = h; if (x != -1f) view.cubeX.value = x; if (y != -1f) view.cubeY.value = y; if (r >= 0) view.cubeR.value = r }
                        }
                    }
                }
                "com.example.dynamicisland.HARDWARE_TOGGLE" -> {
                    if (!isAlertsEnabled) return
                    val type = intent.getStringExtra("type") ?: return
                    val state = intent.getIntExtra("state", 0)
                    if (type == "RINGER") {
                        val (title, text, color) = when (state) {
                            0 -> Triple("Silent Mode", "Muted", android.graphics.Color.GRAY)
                            1 -> Triple("Vibrate", "Vibrating", android.graphics.Color.rgb(255, 165, 0))
                            else -> Triple("Ring", "Active", android.graphics.Color.GREEN)
                        }
                        postTransientNotification(LiveActivityModel.General("hw_ringer", ActivityType.HARDWARE, title, text, color), 3000L)
                    }
                }
                "com.example.dynamicisland.APP_CHANGED" -> { 
                    topAppPackage = intent.getStringExtra("pkg") ?: ""
                    evaluatePriority() 
                }
                "com.example.dynamicisland.NOTIFICATION_CAUGHT" -> {
                    val pkg = intent.getStringExtra("pkg") ?: ""
                    val notif = if (android.os.Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra("notification", android.app.Notification::class.java) else intent.getParcelableExtra("notification")
                    if (notif != null) notificationManager.processIncomingNotification(pkg, notif)
                }
            }
        }
    }

    private val componentCallbacks = object : android.content.ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) { if (isSystemProcess) evaluatePriority() }
        @Suppress("OVERRIDE_DEPRECATION") override fun onLowMemory() { iconCache.evictAll(); notificationManager.clearAll(); System.gc() }
        override fun onTrimMemory(level: Int) { if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) { iconCache.evictAll(); notificationManager.clearAll() } }
    }

    internal fun evaluatePriority() {
        if (!isSystemProcess || !settingsState.islandEnabled) {
            _lastIslandState = IslandState.HIDDEN
            eventBus.emit(IslandIntent.SyncState(IslandState.HIDDEN, null, null))
            updateWindowHeight(IslandState.HIDDEN)
            return
        }
        
        if (islandView?.calibrationMode?.value == true) { updateWindowHeight(IslandState.TYPE_2_MID); return }

        val result = IslandPriorityEngine.evaluatePriority(
            context = context, windowManager = windowManager, topAppPackage = topAppPackage,
            isPanelExpanded = isPanelExpanded, currentCall = currentCall, transientModel = transientModel,
            activeExternalActivity = activeExternalActivities.values.firstOrNull(),
            currentMedia = currentMedia, currentHardware = currentHardware, currentWeather = currentWeather,
            isMediaEnabled = mediaManager.isMediaEnabled, userForceCollapsed = userForceCollapsed,
            currentActiveModel = _lastActiveModel, currentVisualState = _lastIslandState
        )

        // Visibility Filter for Ring
        var finalState = result.islandState
        if (finalState == IslandState.TYPE_0_RING) {
            val isRingAllowed = when {
                currentMedia != null -> settingsState.ringMediaVisible
                wasCharging -> settingsState.ringBatteryVisible
                else -> true
            }
            if (!isRingAllowed) finalState = IslandState.HIDDEN
        }

        if (finalState == _lastIslandState && result.activeModel == _lastActiveModel && result.splitModel == _lastSplitModel) return

        userForceCollapsed = result.userForceCollapsed
        _lastIslandState = finalState
        _lastActiveModel = result.activeModel
        _lastSplitModel = result.splitModel

        eventBus.emit(IslandIntent.SyncState(finalState, result.activeModel, result.splitModel))
        triggerTransitionHaptic(finalState)
        updateWindowHeight(finalState)
    }

    private fun updateWindowHeight(state: IslandState) {
        val wm = windowManager ?: return
        val view = islandView ?: return
        val wp = windowParams ?: return
        
        val density = context.resources.displayMetrics.density
        val targetHeight = when (state) {
            IslandState.HIDDEN -> 1 
            IslandState.TYPE_0_RING, IslandState.TYPE_1_MINI -> (94 * density).toInt()
            else -> (320 * density).toInt()
        }
        
        val isInteractive = when (state) {
            IslandState.TYPE_2_MID, IslandState.TYPE_3_MAX, IslandState.TYPE_CUBE, IslandState.TYPE_SPLIT -> true
            else -> false
        } || (islandView?.calibrationMode?.value == true)
        
        val newFlags = if (isInteractive) {
             wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
             val settings = settingsState
             // Handle "Invisible but touchable" dashboard trigger
             if (state == IslandState.TYPE_0_RING && !settings.ringMediaVisible && !settings.invisibleRingTouchPassthrough) {
                  wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
             } else {
                  wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
             }
        }
        
        if (wp.height != targetHeight || wp.flags != newFlags) {
            wp.height = targetHeight; wp.flags = newFlags
            try { wm.updateViewLayout(view, wp) } catch (e: Exception) {}
        }
    }

    var windowParams: WindowManager.LayoutParams? = null

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams?): android.view.View {
        val moduleContext = try { context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) } catch (e: Exception) { context }
        val view = DynamicIslandView(context, moduleContext)
        view.id = android.view.View.generateViewId()
        val lifecycleOwner = ComposeLifecycleOwner()
        lifecycleOwner.onCreate(); lifecycleOwner.attachToView(view); lifecycleOwner.onStart(); lifecycleOwner.onResume()
        this.islandView = view
        this.windowManager = wm
        this.windowParams = params
        view.controller = this

        view.onVolumeDrag = { pct -> hardwareManager.setSystemVolume(pct, view) }
        view.onBrightnessDrag = { pct -> hardwareManager.setSystemBrightness(pct, view) }
        view.onAppPinnedClick = { pkg -> actionManager.launchAppIntent(pkg) { userForceCollapsed = true; _lastIslandState = IslandState.TYPE_0_RING; evaluatePriority() } }
        view.onQsTileClick = { tileSpec -> actionManager.handleQSTileClick(tileSpec) { } }
        
        // --- PRO-GRADE SMART GESTURES ---
        view.onGestureEvent = { gesture ->
            val smartAction = evaluateSmartGesture(gesture)
            if (smartAction != null) {
                android.util.Log.d("IslandController", "🚀 Smart AI Gesture triggered: $smartAction")
                executeSmartAction(smartAction)
            } else {
                // Fallback to static user configuration
                val stateKey = when (_lastIslandState) {
                    IslandState.TYPE_0_RING -> "TYPE_0_RING"
                    IslandState.TYPE_1_MINI -> "TYPE_1_MINI"
                    IslandState.TYPE_2_MID -> "TYPE_2_MID"
                    IslandState.TYPE_3_MAX -> "TYPE_3_MAX"
                    else -> "TYPE_1_MINI"
                }
                val suffix = when (gesture) {
                    IslandGesture.SINGLE_TAP -> "single_tap"
                    IslandGesture.DOUBLE_TAP -> "double_tap"
                    IslandGesture.LONG_PRESS -> "long_press"
                    IslandGesture.SWIPE_LEFT -> "swipe_left"
                    IslandGesture.SWIPE_RIGHT -> "swipe_right"
                    IslandGesture.SWIPE_UP -> "swipe_up"
                    IslandGesture.SWIPE_DOWN -> "swipe_down"
                }
                val action = settingsManager.getString("${stateKey}_$suffix", "NONE") ?: "NONE"
                if (action != "NONE") executeSmartAction(action)
            }
        }

        scope.launch {
            eventBus.intents.collect { intent ->
                if (intent is IslandIntent.SyncState) {
                    view.islandState.value = intent.state
                    view.activeModel.value = intent.activeModel
                    view.splitModel.value = intent.splitModel
                    mediaManager.isIslandVisible = intent.state != IslandState.HIDDEN && intent.state != IslandState.TYPE_0_RING
                }
            }
        }
        return view
    }

    private fun evaluateSmartGesture(gesture: IslandGesture): String? {
        if (!settingsState.smartGesturesEnabled) return null

        // 1. 📞 ACTIVE CALL RULE
        if (currentCall != null && settingsState.smartCallOverride) {
            return when (gesture) {
                IslandGesture.SINGLE_TAP -> "MUTE_TOGGLE"
                IslandGesture.LONG_PRESS -> "END_CALL"
                else -> null
            }
        }

        // 2. 🎵 SOCIAL MEDIA OVERRIDE
        val socialApps = listOf("com.instagram.android", "com.zhiliaoapp.musically", "com.reddit.frontpage", "com.twitter.android")
        if (currentMedia?.isPlaying == true && settingsState.smartMediaOverride && socialApps.contains(topAppPackage)) {
            return when (gesture) {
                IslandGesture.SWIPE_LEFT -> "PREV_TRACK"
                IslandGesture.SWIPE_RIGHT -> "NEXT_TRACK"
                else -> null
            }
        }

        // 3. 🎮 GAMING / IMMERSION RULE
        if ((currentHardware?.isGamingModeOn == true || topAppPackage.contains("game")) && settingsState.smartGamingOverride) {
            return when (gesture) {
                IslandGesture.SWIPE_DOWN -> "OPEN_DASHBOARD"
                IslandGesture.SWIPE_UP -> "FORCE_DISMISS"
                else -> null
            }
        }
        return null
    }

    private fun executeSmartAction(actionName: String) {
        when (actionName) {
            "PLAY_PAUSE" -> { if (currentMedia?.isPlaying == true) mediaManager.sendMediaCommand("PAUSE") else mediaManager.sendMediaCommand("PLAY") }
            "NEXT_TRACK" -> mediaManager.sendMediaCommand("NEXT")
            "PREV_TRACK" -> mediaManager.sendMediaCommand("PREV")
            "SCREENSHOT" -> { context.sendBroadcast(Intent("com.example.dynamicisland.TRIGGER_SCREENSHOT")) }
            "OPEN_DASHBOARD" -> {
                val dashboard = _lastActiveModel as? LiveActivityModel.Dashboard ?: LiveActivityModel.Dashboard()
                _lastActiveModel = dashboard; _lastIslandState = IslandState.TYPE_3_MAX
                eventBus.emit(IslandIntent.SyncState(IslandState.TYPE_3_MAX, dashboard, _lastSplitModel))
            }
            "COLLAPSE" -> { userForceCollapsed = true; _lastIslandState = IslandState.TYPE_0_RING; evaluatePriority() }
            "EXPAND" -> { userForceCollapsed = false; if (_lastIslandState == IslandState.TYPE_0_RING) _lastIslandState = IslandState.TYPE_1_MINI; evaluatePriority() }
            "VOLUME_UP" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            "VOLUME_DOWN" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            "MUTE_TOGGLE" -> {
                val isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (isMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            }
            "TOGGLE_TORCH" -> { SystemUIA15Hooks.toggleSystemFlashlight() }
            "OPEN_SOURCE_APP" -> {
                val pkg = when (val m = _lastActiveModel) {
                    is LiveActivityModel.Music -> m.appPackageName
                    is LiveActivityModel.OngoingTask -> m.pkgName
                    is LiveActivityModel.NotificationStack -> m.pkgName
                    is LiveActivityModel.Call -> m.sourceApp
                    else -> null
                }
                if (pkg != null) actionManager.launchAppIntent(pkg) { userForceCollapsed = true; _lastIslandState = IslandState.TYPE_0_RING; evaluatePriority() }
            }
            "FORCE_DISMISS" -> { userForceCollapsed = true; _lastIslandState = IslandState.HIDDEN; evaluatePriority() }
            "LAUNCH_SETTINGS" -> actionManager.launchAppIntent("com.android.settings") { userForceCollapsed = true; _lastIslandState = IslandState.TYPE_0_RING; evaluatePriority() }
        }
    }

    internal fun postTransientNotification(model: LiveActivityModel, durationMs: Long = 5000L) {
        if (!isSystemProcess) return
        triggerTransitionHaptic(_lastIslandState)
        transientJob?.cancel(); transientModel = model; evaluatePriority()
        transientJob = scope.launch { delay(durationMs); transientModel = null; evaluatePriority() }
    }

    init {
        android.util.Log.d("IslandController", "Initializing Advanced Island (Process: ${context.packageName}, isSystem: $isSystemProcess)")
        context.registerComponentCallbacks(componentCallbacks)
        loadAndApplySettings()
        
        if (isSystemProcess) {
            weatherManager.startPolling()
            connectivityManager.startListening()
            hardwareMonitor.onHardwareUpdate = { newHw -> currentHardware = newHw; evaluatePriority() }
            mediaManager.onMediaChanged = { newMedia -> currentMedia = newMedia; evaluatePriority() }
            mediaManager.onMediaTick = { pos -> islandView?.updateTicker(pos) }
            
            val screenFilter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
            context.registerReceiver(screenStateReceiver, screenFilter)
            
            val ecoFilter = IntentFilter().apply {
                addAction("com.example.dynamicisland.RELOAD_PREFS")
                addAction("com.example.dynamicisland.DEBUG_TEST")
                addAction("com.example.dynamicisland.CALIBRATION_MODE")
                addAction("com.example.dynamicisland.CALIBRATION_UPDATE")
                addAction("com.example.dynamicisland.HARDWARE_TOGGLE")
                addAction("com.example.dynamicisland.APP_CHANGED")
                addAction("com.example.dynamicisland.NOTIFICATION_CAUGHT")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.registerReceiver(ecosystemReceiver, ecoFilter, Context.RECEIVER_EXPORTED)
            else context.registerReceiver(ecosystemReceiver, ecoFilter)

            BatteryPlugin.onBatteryChanged = { level, isCharging, _, _ ->
                if (isChargingEnabled && isCharging && !wasCharging) {
                    postTransientNotification(LiveActivityModel.Charging("sys_battery", level, true, true, false), 3000L)
                }
                wasCharging = isCharging; islandView?.updateBattery(level, isCharging)
            }
            BatteryPlugin.start(context)
            mediaManager.start()
        }
    }
}
