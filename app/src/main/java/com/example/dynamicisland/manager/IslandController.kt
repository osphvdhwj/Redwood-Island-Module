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
    
    // 1. Declare your settings state and managers at the top of the controller
    var settingsState by mutableStateOf(com.example.dynamicisland.settings.SettingsState())
        private set

    // Lazy load the managers so they don't crash if they aren't needed yet
    private val audioBeatDetector by lazy { com.example.dynamicisland.audio.AudioBeatDetector() }
    private val achievementManager by lazy { com.example.dynamicisland.achievements.AchievementManager(context) }
    private val accessibilityManager by lazy { com.example.dynamicisland.accessibility.IslandAccessibilityManager(context) }
    private val proximityWakeManager by lazy { com.example.dynamicisland.sensors.ProximityWakeManager(context) }

    // 2. Add this initialization function. Call this in your controller's init{} block!
    fun loadAndApplySettings() {
        // Fetch the latest state
        settingsState = settingsManager.getSettingsState()
        
        applySettings(settingsState)
    }

    // 3. The engine that turns features on/off based on settings
    private fun applySettings(state: com.example.dynamicisland.settings.SettingsState) {
        // Global Enable/Disable
        if (!state.islandEnabled) {
            _lastIslandState = IslandState.HIDDEN
            eventBus.emit(IslandIntent.SyncState(IslandState.HIDDEN, null, null))
        }

        // Feature 199: Wire up the managers based on toggles
        mediaManager.isMediaEnabled = state.mediaArtworkBlur || state.waveformEnabled || state.nowPlaying
        mediaManager.allowedMusicApps = state.allowedMusicApps
        mediaManager.allowedMediaApps = state.allowedMediaApps
        mediaManager.userVideoApp = state.roleVideoApp.takeIf { it.isNotEmpty() }
        
        callManager.userCallingApp = state.roleCallingApp.takeIf { it.isNotEmpty() }
        
        isChargingEnabled = state.magsafeChargingAnimation || state.batteryAwareAnimation
        isAlertsEnabled = state.otpDetection || state.linkIntercept || state.barcode || state.translation
        isTimersEnabled = state.timerIntegration
        hideInLandscape = state.dataSaver || state.gamingHud // suppress in games if needed

        // Ecosystem Features
        val isAppleEcosystemEnabled = state.airpodsPopup || state.faceIDPadlock
        // ... hook additional triggers if needed ...
        
        idleSwipeAction = state.swipeLeftAction.uppercase()
        longPressAction = state.swipeRightAction.uppercase() // mapping as placeholder

        // Audio & Visualizers
        if (state.bpmPulse || state.ambientReactiveRing) {
            audioBeatDetector.start()
        } else {
            audioBeatDetector.stop()
        }

        // Accessibility
        if (state.talkbackIntegration) {
            accessibilityManager.start()
        } else {
            accessibilityManager.stop()
        }

        // Sensors
        if (state.proximityWake) {
            proximityWakeManager.start()
        } else {
            proximityWakeManager.stop()
        }
        
        evaluatePriority()
    }
    
    private var pendingNotificationColor: Int = android.graphics.Color.WHITE
    private var hasUnseenNotification = false

    private fun triggerTransitionHaptic(newState: IslandState) {
        hapticsManager.triggerTransitionHaptic(newState, currentCall?.state, topAppPackage)
    }

    private fun performCustomHaptic(context: Context, strength: Int, topAppPackage: String) {
        hapticsManager.performCustomHaptic(strength, topAppPackage)
    }

    fun swallowClipData(clipData: android.content.ClipData) {
        scope.launch {
            val saved = storageManager.swallowDroppedData(clipData)
            if (saved) {
                postTransientNotification(
                    LiveActivityModel.General(
                        id = "sys_stash", 
                        type = ActivityType.MESSAGE, 
                        title = "Item Stashed", 
                        dataText = "Added to Island Archive",
                        accentColor = android.graphics.Color.parseColor("#4CAF50")
                    ), 
                    3000L
                )
            }
        }
    }

    val stashHistory: StateFlow<List<com.example.dynamicisland.manager.StashedItem>> 
        get() = storageManager.stashHistory

    private val storageManager = IslandStorageManager(context)
    private val clipboardManager = IslandClipboardManager(context, scope) { copiedText ->
        if (isAlertsEnabled) {
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
            networkMonitor.startMonitoring(scope) { speedStr ->
                val current = _lastActiveModel
                if (current is LiveActivityModel.OngoingTask) {
                    val updated = current.copy(networkSpeed = speedStr)
                    _lastActiveModel = updated
                    eventBus.emit(IslandIntent.SyncState(_lastIslandState, updated, _lastSplitModel))
                }
            }

            _lastActiveModel = progressModel
            if (_lastIslandState == IslandState.TYPE_0_RING) _lastIslandState = IslandState.TYPE_1_MINI
            evaluatePriority()
        },
        onNavigationCaught = { navModel ->
            postTransientNotification(navModel, 5000L)
        },
        onNotificationStackCaught = { stackModel ->
            _lastActiveModel = stackModel
            if (_lastIslandState == IslandState.TYPE_0_RING) _lastIslandState = IslandState.TYPE_1_MINI
            evaluatePriority()
        }
    )

    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private var qsTilesCache = listOf<QSTileState>()
    private var pinnedAppsCache = listOf<String>()

    private val hardwareManager = IslandHardwareManager(context, audioManager, scope)
    private val actionManager = IslandActionManager(context, scope)
    private val weatherManager = IslandWeatherManager(context, scope) { newWeather ->
        currentWeather = newWeather; evaluatePriority()
    }
    private val callManager = IslandCallManager(context, audioManager) { newCall ->
        currentCall = newCall; evaluatePriority()
    }
    
    private val connectivityManager = IslandConnectivityManager(context) { model ->
        postTransientNotification(model, 5000L)
    }

    private var windowManager: WindowManager? = null
    internal var islandView: DynamicIslandView? = null

    // Trackers for the priority engine
    private var _lastIslandState = IslandState.TYPE_0_RING
    private var _lastActiveModel: LiveActivityModel? = null
    private var _lastSplitModel: LiveActivityModel? = null

    private var currentCall: LiveActivityModel.Call? = null
    private var currentMedia: LiveActivityModel.Music? = null
    private var currentWeather: LiveActivityModel.WeatherMood? = null
    var currentHardware: LiveActivityModel.HardwareMonitor? = null
    private var transientModel: LiveActivityModel? = null

    private var transientJob: Job? = null
    private var pauseFadeJob: Job? = null
    private var autoCollapseJob: Job? = null

    private var userForceCollapsed = false
    private var lastReportedBattery = -1
    private var wasCharging = false
    private var topAppPackage = ""
    private var isPeeking = false
    private var isPanelExpanded = false

    // Smart Configuration States
    private var isChargingEnabled = true
    private var isAlertsEnabled = true
    private var isTimersEnabled = true
    private var hideInLandscape = false
    private var idleSwipeAction = "BRIGHTNESS"
    private var longPressAction = "SCREENSHOT"
    private val gestureMatrix = mutableMapOf<String, String>()

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
                "com.example.dynamicisland.DEBUG_TEST" -> {
                    android.util.Log.d("IslandController", "DEBUG: Triggering test notification")
                    postTransientNotification(
                        LiveActivityModel.General(
                            id = "debug_test",
                            type = ActivityType.MESSAGE,
                            title = "System Integration Active",
                            dataText = "Direct hook confirmed.",
                            accentColor = android.graphics.Color.GREEN
                        ), 10000L
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
                                "ring" -> IslandState.TYPE_0_RING
                                "mini" -> IslandState.TYPE_1_MINI
                                "mid" -> IslandState.TYPE_2_MID
                                "max" -> IslandState.TYPE_3_MAX
                                "cube" -> IslandState.TYPE_CUBE
                                else -> IslandState.TYPE_0_RING
                            }
                            _lastIslandState = targetState
                            eventBus.emit(IslandIntent.SyncState(targetState, _lastActiveModel, _lastSplitModel))
                        } else {
                            evaluatePriority()
                        }
                    }
                }
                "com.example.dynamicisland.CALIBRATION_UPDATE" -> {
                    val prefix = intent.getStringExtra("prefix") ?: return
                    islandView?.let { view ->
                        val w = intent.getFloatExtra("w", -1f)
                        val h = intent.getFloatExtra("h", -1f)
                        val x = intent.getFloatExtra("x", -1f)
                        val y = intent.getFloatExtra("y", -1f)
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
                            0 -> Triple("Silent Mode", "Calls and notifications muted", android.graphics.Color.GRAY)
                            1 -> Triple("Vibrate", "Device will vibrate", android.graphics.Color.rgb(255, 165, 0))
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
                    postTransientNotification(LiveActivityModel.General(id = "sys_screenshot", type = ActivityType.MESSAGE, title = "Screenshot Saved", dataText = "Tap to view or share", accentColor = android.graphics.Color.WHITE), 4000L)
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
                        val timeStr = sdf.format(java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(triggerTimeMs)))
                        postTransientNotification(LiveActivityModel.General(id = "sys_alarm", type = ActivityType.ALARM, title = "Alarm Set", dataText = "Ringing at $timeStr", accentColor = android.graphics.Color.CYAN), 3500L)
                    }
                }
                "com.example.dynamicisland.APP_CHANGED" -> { 
                    topAppPackage = intent.getStringExtra("pkg") ?: ""
                    if (topAppPackage.isNotEmpty() && topAppPackage == settingsState.roleGameLauncher) {
                         currentHardware = LiveActivityModel.HardwareMonitor(
                             id = "hw_monitor", type = ActivityType.HARDWARE,
                             cpuTempCelsius = 0f, cpuFreqMhz = 0, isGamingModeOn = true
                         )
                    }
                    evaluatePriority() 
                }
                "com.example.dynamicisland.NOTIFICATION_CAUGHT" -> {
                    val pkg = intent.getStringExtra("pkg") ?: ""
                    val notif = if (android.os.Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra("notification", android.app.Notification::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra("notification")
                    }
                    if (notif != null) notificationManager.processIncomingNotification(pkg, notif)
                }
                "com.example.dynamicisland.OTP_CAUGHT" -> {
                    if (!isAlertsEnabled) return
                    val otp = intent.getStringExtra("otp") ?: return
                    postTransientNotification(
                        LiveActivityModel.SystemAlert(
                            id         = "sys_otp",
                            alertType  = "OTP_CATCHER",
                            title      = "Verification Code",
                            message    = otp,
                            alertColor = android.graphics.Color.parseColor("#4285F4"),
                            isCritical = true
                        ),
                        30_000L
                    )
                }
                "com.example.dynamicisland.SYNC_CONFIG" -> {
                    val type = intent.getStringExtra("type")
                    if (type == "dashboard") {
                        val payload = intent.getStringExtra("payload") ?: return
                        try {
                            val json = JSONObject(payload)
                            val appsArr = json.optJSONArray("pinned_apps")
                            val tilesArr = json.optJSONArray("qs_tiles")

                            val appsList = mutableListOf<String>()
                            if (appsArr != null) for (i in 0 until appsArr.length()) appsList.add(appsArr.getString(i))

                            val tilesList = mutableListOf<QSTileState>()
                            if (tilesArr != null) {
                                for (i in 0 until tilesArr.length()) {
                                    val obj = tilesArr.getJSONObject(i)
                                    tilesList.add(QSTileState(tileName = obj.getString("spec"), isActive = false, isUnavailable = false, iconRes = 0))
                                }
                            }

                            pinnedAppsCache = appsList
                            qsTilesCache = tilesList

                            if (_lastActiveModel is LiveActivityModel.Dashboard) {
                                val dashboard = LiveActivityModel.Dashboard(activeTiles = qsTilesCache, pinnedApps = pinnedAppsCache)
                                _lastActiveModel = dashboard
                                eventBus.emit(IslandIntent.SyncState(_lastIslandState, dashboard, _lastSplitModel))
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

    internal fun evaluatePriority() {
        if (!settingsState.islandEnabled) {
            _lastIslandState = IslandState.HIDDEN
            eventBus.emit(IslandIntent.SyncState(IslandState.HIDDEN, null, null))
            return
        }
        
        if (islandView?.calibrationMode?.value == true) return // Lock state during calibration

        val result = IslandPriorityEngine.evaluatePriority(
            context = context,
            windowManager = windowManager,
            topAppPackage = topAppPackage,
            isPanelExpanded = isPanelExpanded,
            currentCall = currentCall,
            transientModel = transientModel,
            activeExternalActivity = activeExternalActivities.values.firstOrNull(),
            currentMedia = currentMedia,
            currentHardware = currentHardware,
            currentWeather = currentWeather,
            isMediaEnabled = mediaManager.isMediaEnabled,
            userForceCollapsed = userForceCollapsed,
            currentActiveModel = _lastActiveModel,
            currentVisualState = _lastIslandState
        )

        // 🛑 PERFORMANCE GUARD: Only update if the state or models have actually changed.
        if (result.islandState == _lastIslandState &&
            result.activeModel == _lastActiveModel &&
            result.splitModel == _lastSplitModel) {
            return
        }

        android.util.Log.d("IslandController", "Evaluating Priority -> New State: ${result.islandState}, ActiveModel: ${result.activeModel?.id}")

        userForceCollapsed = result.userForceCollapsed
        _lastIslandState = result.islandState
        _lastActiveModel = result.activeModel
        _lastSplitModel = result.splitModel

        eventBus.emit(IslandIntent.SyncState(result.islandState, result.activeModel, result.splitModel))
        triggerTransitionHaptic(result.islandState)
    }

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams?): android.view.View {
        val moduleContext = try { context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) } catch (e: Exception) { context }
        val view = DynamicIslandView(context, moduleContext)
        view.id = android.view.View.generateViewId()
        val lifecycleOwner = ComposeLifecycleOwner()
        lifecycleOwner.onCreate(); lifecycleOwner.attachToView(view); lifecycleOwner.onStart(); lifecycleOwner.onResume()
        this.islandView = view
        this.windowManager = wm
        view.controller = this

        scope.launch {
            delay(500)
            val requestIntent = Intent("com.example.dynamicisland.REQUEST_PREFS")
            requestIntent.setPackage("com.example.dynamicisland")
            context.sendBroadcast(requestIntent)
        }

        view.onVolumeDrag = { pct -> hardwareManager.setSystemVolume(pct, view) }
        view.onStreamVolumeDrag = { streamType, pct -> hardwareManager.setStreamVolume(streamType, pct) }
        view.onBrightnessDrag = { pct -> hardwareManager.setSystemBrightness(pct, view) }
        view.onMicToggle = { hardwareManager.toggleMicMute() }
        view.onSpeakerToggle = { hardwareManager.toggleSpeakerphone() }
        view.onEndCallClick = { callManager.endActiveCall() }
        view.onOpenCallUI = { callManager.openNativeCallUI(view) }
        view.onAutoBrightnessToggle = { hardwareManager.toggleAutoBrightness(view) }
        view.onRingerToggle = { hardwareManager.toggleRingerMode(view) }
        view.onAppPinnedClick = { pkg -> actionManager.launchAppIntent(pkg) { userForceCollapsed = true; _lastIslandState = IslandState.TYPE_0_RING; evaluatePriority() } }
        view.onQsTileClick = { tileSpec -> actionManager.handleQSTileClick(tileSpec) { } }

        view.windowManager = wm
        view.windowParams = params

        scope.launch {
            eventBus.intents.collect { intent ->
                when (intent) {
                    is IslandIntent.SyncState -> {
                        view.islandState.value = intent.state
                        view.activeModel.value = intent.activeModel
                        view.splitModel.value = intent.splitModel
                        val isVisible = intent.state != IslandState.HIDDEN && intent.state != IslandState.TYPE_0_RING
                        mediaManager.isIslandVisible = isVisible
                        hardwareMonitor.isDashboardOpen = (intent.state == IslandState.TYPE_3_MAX)
                    }
                    is IslandIntent.ToggleCalibration -> {
                        view.calibrationMode.value = intent.enabled
                        view.calibrationTarget.value = intent.targetState ?: "ring"
                        if (intent.enabled) {
                            val target = when(intent.targetState?.lowercase()) {
                                "mini" -> IslandState.TYPE_1_MINI
                                "mid" -> IslandState.TYPE_2_MID
                                "max" -> IslandState.TYPE_3_MAX
                                "cube" -> IslandState.TYPE_CUBE
                                else -> IslandState.TYPE_0_RING
                            }
                            _lastIslandState = target
                            eventBus.emit(IslandIntent.SyncState(target, _lastActiveModel, _lastSplitModel))
                        } else {
                            evaluatePriority()
                        }
                    }
                    else -> {}
                }
            }
        }
        return view
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
            "TOGGLE_TORCH" -> {
                SystemUIA15Hooks.toggleSystemFlashlight()
            }
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
            "LAUNCH_CAMERA" -> {
                val cameraIntent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                try { context.startActivity(cameraIntent) } catch (e: Exception) {}
            }
            else -> { if (actionName.startsWith("LAUNCH_APP_")) actionManager.launchAppIntent(actionName.removePrefix("LAUNCH_APP_")) { userForceCollapsed = true; _lastIslandState = IslandState.TYPE_0_RING; evaluatePriority() } }
        }
    }

    internal fun postTransientNotification(model: LiveActivityModel, durationMs: Long = 5000L) {
        triggerTransitionHaptic(_lastIslandState)
        transientJob?.cancel(); transientModel = model; evaluatePriority()
        transientJob = scope.launch { delay(durationMs); transientModel = null; evaluatePriority() }
    }

    init {
        loadAndApplySettings()
        weatherManager.startPolling()
        hardwareMonitor.onHardwareUpdate = { newHw -> currentHardware = newHw; evaluatePriority() }
        mediaManager.onMediaChanged = { newMedia -> currentMedia = newMedia; evaluatePriority() }
        mediaManager.onMediaTick = { pos -> islandView?.updateTicker(pos) }
        
        val screenFilter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
        context.registerReceiver(screenStateReceiver, screenFilter)
        
        val ecoFilter = IntentFilter().apply {
            addAction("com.example.dynamicisland.DEBUG_TEST")
            addAction("com.example.dynamicisland.CALIBRATION_MODE")
            addAction("com.example.dynamicisland.CALIBRATION_UPDATE")
            addAction("com.example.dynamicisland.HARDWARE_TOGGLE")
            addAction("com.example.dynamicisland.ALARM_SET")
            addAction("com.example.dynamicisland.SCREENSHOT_CAUGHT")
            addAction("com.example.dynamicisland.OTP_CAUGHT")
            addAction("com.example.dynamicisland.APP_CHANGED")
            addAction("com.example.dynamicisland.NOTIFICATION_CAUGHT")
            addAction("com.example.dynamicisland.SHOW_VOLUME_MIXER")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(ecosystemReceiver, ecoFilter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(ecosystemReceiver, ecoFilter)
        }

        BatteryPlugin.onBatteryChanged = { level, isCharging, _, _ ->
            if (isChargingEnabled) {
                if (isCharging && !wasCharging) {
                    postTransientNotification(LiveActivityModel.Charging(id = "sys_battery", level = level, isPluggedIn = true, isTransient = true, isCritical = false), 3000L)
                }
            }
            wasCharging = isCharging; lastReportedBattery = level; islandView?.updateBattery(level, isCharging)
        }
        BatteryPlugin.start(context)
        mediaManager.start()
    }
}
