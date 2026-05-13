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

class IslandController(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val activeExternalActivities = mutableMapOf<String, LiveActivityModel.ExternalActivity>()

    private var pendingNotificationColor: Int = android.graphics.Color.WHITE
    private var hasUnseenNotification = false

    private var lastHapticState: IslandState = IslandState.HIDDEN

    private fun triggerTransitionHaptic(newState: IslandState) {
        if (newState == lastHapticState) return
        val prev = lastHapticState
        lastHapticState = newState

        val strength = when {
            newState == IslandState.TYPE_3_MAX -> 3
            newState == IslandState.TYPE_2_MID && currentCall?.state == "RINGING" -> 2
            newState == IslandState.TYPE_0_RING && prev != IslandState.HIDDEN -> 1
            newState.ordinal > prev.ordinal -> 2
            else -> 0
        }
        if (strength > 0) performCustomHaptic(context, strength, topAppPackage)
    }

    private fun performCustomHaptic(context: Context, strength: Int, topAppPackage: String) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val timing = longArrayOf(0, (30 * strength).toLong(), 50, (40 * strength).toLong())
        val amplitudes = intArrayOf(0, (100 * strength).coerceAtMost(255), 0, (150 * strength).coerceAtMost(255))
        vibrator.vibrate(VibrationEffect.createWaveform(timing, amplitudes, -1))
    }

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

    private val connectivityManager = IslandConnectivityManager(context) { connModel ->
        if (isAlertsEnabled) postTransientNotification(connModel, 4000L)
    }

    private var downloadSpeedJob: Job? = null
    private var lastRxBytes = 0L

    private val notificationManager = IslandNotificationManager(context, scope,
        onProgressCaught = { progressModel ->
            if (downloadSpeedJob?.isActive != true) {
                lastRxBytes = android.net.TrafficStats.getTotalRxBytes()
                downloadSpeedJob = scope.launch(Dispatchers.Main) {
                    while (isActive) {
                        delay(1000)
                        val currentRx = android.net.TrafficStats.getTotalRxBytes()
                        val bytesPerSec = currentRx - lastRxBytes
                        lastRxBytes = currentRx

                        val speedStr = if (bytesPerSec > 1048576) {
                            String.format("%.1f MB/s", bytesPerSec / 1048576f)
                        } else {
                            String.format("%d KB/s", bytesPerSec / 1024)
                        }

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
        onNavigationCaught = { navModel ->
            postTransientNotification(navModel, 5000L)
        }
    )

    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private var qsTilesCache = listOf<QSTileState>()
    private var pinnedAppsCache = listOf<String>()

    private val hardwareManager = IslandHardwareManager(context, audioManager, scope)
    private val actionManager = IslandActionManager(context, scope)
    private val callManager = IslandCallManager(context, audioManager) { newCall ->
        currentCall = newCall; evaluatePriority()
    }

    private val hardwareMonitor = IslandHardwareMonitor(scope) { newHw ->
        currentHardware = newHw; evaluatePriority()
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

    private val _islandState = MutableStateFlow(IslandState.TYPE_0_RING)
    val islandState = _islandState.asStateFlow()
    private val _activeModel = MutableStateFlow<LiveActivityModel?>(null)
    val activeModel = _activeModel.asStateFlow()
    private val _splitModel = MutableStateFlow<LiveActivityModel?>(null)
    val splitModel = _splitModel.asStateFlow()

    private var currentCall: LiveActivityModel.Call? = null
    private var currentMedia: LiveActivityModel.Music? = null
    var currentHardware: LiveActivityModel.HardwareMonitor? = null
    private var transientModel: LiveActivityModel? = null

    private var transientJob: Job? = null
    private var pauseFadeJob: Job? = null

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
    // FIXED: gestureMatrix now stores action names (String) instead of IslandAction objects
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
                        val timeStr = sdf.format(java.util.Date(triggerTimeMs))
                        postTransientNotification(LiveActivityModel.General(id = "sys_alarm", type = ActivityType.ALARM, title = "Alarm Set", dataText = "Ringing at $timeStr", accentColor = android.graphics.Color.CYAN), 3500L)
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
                "com.crdroid.batterywellbeing.SYSTEM_OVERRIDE" -> {
                    if (!isAlertsEnabled) return
                    when (intent.getStringExtra("action")) {
                        "SMART_CHARGE_LIMIT" -> postTransientNotification(LiveActivityModel.Charging("sys_smart_charge", ActivityType.CHARGING, intent.getIntExtra("level", 100), true, true), 6000L)
                        "THERMAL_WARNING" -> postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_thermal", alertType = "THERMAL", title = "Thermal Throttling", message = "Battery temp at ${intent.getStringExtra("extra_info") ?: "High"}", alertColor = android.graphics.Color.RED), 6000L)
                        "ROGUE_APP_DETECTED" -> postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_rogue", alertType = "ROGUE", title = "High Battery Drain", message = "${intent.getStringExtra("extra_info") ?: "Unknown App"} is draining battery", alertColor = android.graphics.Color.rgb(255, 165, 0)), 6000L)
                    }
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
                                    tilesList.add(QSTileState(tileSpec = obj.getString("spec"), label = obj.getString("label"), isActive = false, isUnavailable = false))
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
                "com.example.dynamicisland.EXTERNAL_ACTIVITY_UPDATED" -> {
                    val activityId = intent.getStringExtra("activity_id") ?: return
                    val pkg = intent.getStringExtra("package_name") ?: ""
                    val layoutType = intent.getStringExtra("layout_type") ?: ""
                    val state = intent.getBundleExtra("state") ?: Bundle()

                    val info = com.example.dynamicisland.ipc.LiveActivityInfo(activityId, pkg, layoutType, state)
                    val model = LiveActivityModel.ExternalActivity(
                        id = activityId,
                        info = info,
                        state = state,
                        isTransient = false,
                        isCritical = false
                    )
                    activeExternalActivities[activityId] = model

                    _activeModel.value = model
                    _islandState.value = IslandState.TYPE_2_MID
                    evaluatePriority()
                }
                "com.example.dynamicisland.EXTERNAL_ACTIVITY_ENDED" -> {
                    val activityId = intent.getStringExtra("activity_id") ?: return
                    activeExternalActivities.remove(activityId)
                    if (_activeModel.value?.id == activityId) {
                        evaluatePriority()
                    }
                }
                CrDroidAPIHook.ACTION_GAME_MODE_CHANGED -> {
                    val isActive = intent.getBooleanExtra("isActive", false)
                    currentHardware = if (isActive) {
                        LiveActivityModel.HardwareMonitor(
                            id = "hw_monitor", type = ActivityType.HARDWARE,
                            cpuTempCelsius = 0f, cpuFreqMhz = 0, isGamingModeOn = true
                        )
                    } else null
                    evaluatePriority()
                }
                CrDroidAPIHook.ACTION_THERMAL_PROFILE -> {
                    if (!isAlertsEnabled) return
                    val level   = intent.getIntExtra("level", 0)
                    val profile = intent.getStringExtra("profile") ?: "UNKNOWN"
                    if (level >= 3) {
                        postTransientNotification(
                            LiveActivityModel.SystemAlert(
                                id         = "sys_thermal_crdroid",
                                alertType  = "THERMAL",
                                title      = "Thermal Throttling",
                                message    = "Performance reduced — $profile",
                                alertColor = android.graphics.Color.RED,
                                isCritical = level >= 4
                            ),
                            if (level >= 4) 8000L else 5000L
                        )
                    }
                }
                CrDroidAPIHook.ACTION_DISPLAY_MODE -> {
                    if (!isAlertsEnabled) return
                    val hz = intent.getIntExtra("refreshRate", 0)
                    if (hz > 0) {
                        postTransientNotification(
                            LiveActivityModel.General(
                                id          = "sys_display_mode",
                                type        = ActivityType.HARDWARE,
                                title       = "Display Mode",
                                dataText    = "${hz}Hz",
                                accentColor = android.graphics.Color.parseColor("#80DEEA")
                            ),
                            2500L
                        )
                    }
                }
                CrDroidAPIHook.ACTION_SMART_CHARGE -> {
                    if (!isAlertsEnabled || !isChargingEnabled) return
                    val limit  = intent.getIntExtra("limit", 100)
                    val active = intent.getBooleanExtra("active", false)
                    if (active) {
                        postTransientNotification(
                            LiveActivityModel.General(
                                id          = "sys_smart_charge",
                                type        = ActivityType.CHARGING,
                                title       = "Smart Charge Active",
                                dataText    = "Charge limited to $limit%",
                                accentColor = android.graphics.Color.parseColor("#66BB6A")
                            ),
                            4000L
                        )
                    }
                }
                SurfaceFlingerHook.ACTION_FRAME_STATS -> {
                    val fps        = intent.getFloatExtra("fps", 0f)
                    val frameMs    = intent.getFloatExtra("frameMs", 0f)
                    val jankPct    = intent.getFloatExtra("jankPct", 0f)
                    val currentHw  = currentHardware
                    if (currentHw?.isGamingModeOn == true) {
                        currentHardware = currentHw.copy(
                            cpuFreqMhz = fps.toInt()
                        )
                        islandView?.updateGamingStats(fps, frameMs, jankPct)
                    }
                }
                "com.example.dynamicisland.hook.ContinuityCameraScanner.ACTION_BARCODE" -> {
                    if (!isAlertsEnabled) return
                    val raw     = intent.getStringExtra("raw")     ?: return
                    val display = intent.getStringExtra("display") ?: raw
                    val action  = intent.getStringExtra("action")  ?: "Copy"
                    postTransientNotification(
                        LiveActivityModel.General(
                            id          = "sys_barcode",
                            type        = ActivityType.MESSAGE,
                            title       = action,
                            dataText    = display,
                            accentColor = android.graphics.Color.parseColor("#4FC3F7"),
                            isCritical  = false
                        ),
                        10_000L
                    )
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
        userForceCollapsed = IslandPriorityEngine.evaluatePriority(
            context = context,
            windowManager = windowManager,
            topAppPackage = topAppPackage,
            isPanelExpanded = isPanelExpanded,
            currentCall = currentCall,
            transientModel = transientModel,
            activeExternalActivity = activeExternalActivities.values.firstOrNull(),
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

        val productivityApps = listOf("com.notion.id", "com.microsoft.teams", "com.google.android.apps.docs")
        if (productivityApps.contains(topAppPackage)) {
            if (_activeModel.value !is LiveActivityModel.RealityPill) {
                _activeModel.value = LiveActivityModel.RealityPill(appName = "Focus Mode", sessionMinutes = 0)
                _islandState.value = IslandState.TYPE_1_MINI
                try { if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT } catch (e: Throwable) {}
            }
        }
    }

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams): android.view.View {
        val moduleContext = try { context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) } catch (e: Exception) { context }
        val view = DynamicIslandView(context, moduleContext)
        view.id = android.view.View.generateViewId()
        val lifecycleOwner = ComposeLifecycleOwner()
        lifecycleOwner.onCreate()
        lifecycleOwner.attachToView(view)
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()
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
        view.onBrightnessDrag = { pct -> hardwareManager.setSystemBrightness(pct, view) }
        view.onMicToggle = { hardwareManager.toggleMicMute() }
        view.onSpeakerToggle = { hardwareManager.toggleSpeakerphone() }
        view.onEndCallClick = { callManager.endActiveCall() }
        view.onOpenCallUI = { callManager.openNativeCallUI(view) }
        view.onAutoBrightnessToggle = { hardwareManager.toggleAutoBrightness(view) }
        view.onRingerToggle = { hardwareManager.toggleRingerMode(view) }
        view.onAppPinnedClick = { pkg -> actionManager.launchAppIntent(pkg) { userForceCollapsed = true; _islandState.value = IslandState.TYPE_0_RING; evaluatePriority() } }
        view.onQsTileClick = { tileSpec -> actionManager.handleQSTileClick(tileSpec) { } }
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

                hideInLandscape = prefs.getBoolean("hide_landscape", false)
                idleSwipeAction = prefs.getString("idle_swipe_action", "BRIGHTNESS") ?: "BRIGHTNESS"
                longPressAction = prefs.getString("long_press_action", "SCREENSHOT") ?: "SCREENSHOT"

                if (payload != null && payload.length < 5000) {
                    val json = JSONObject(payload)
                    gestureMatrix.clear()
                    json.keys().forEach { key ->
                        try {
                            if (key.startsWith("TYPE_")) {
                                // FIXED: Store the action string directly, not IslandAction.valueOf
                                gestureMatrix[key] = json.getString(key)
                            }
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {}
        }

        view.onGestureEvent = { gesture ->
            val currentState = _islandState.value.name
            // FIXED: gestureMatrix now holds action name strings
            var actionName = gestureMatrix["${currentState}_${gesture.name}"]

            if (gesture.name == "SWIPE_LEFT" || gesture.name == "SWIPE_RIGHT") {
                if (currentMedia != null && currentMedia?.isPlaying == true) {
                    actionName = if (gesture.name == "SWIPE_RIGHT") "NEXT_TRACK" else "PREV_TRACK"
                } else {
                    actionName = idleSwipeAction
                }
            }

            if (gesture.name == "LONG_PRESS") {
                actionName = longPressAction
                if (actionName == "SCREENSHOT") triggerVisualScreenshotFlash()
            }

            if (actionName == null) {
                actionName = if (gesture.name == "SINGLE_TAP") "EXPAND" else "NONE"
            }

            if (gesture.name.startsWith("QS_CLICK_")) {
                actionManager.handleQSTileClick(gesture.name) { newTiles ->
                    if (_activeModel.value is LiveActivityModel.Dashboard) _activeModel.value = LiveActivityModel.Dashboard(activeTiles = newTiles)
                }
            } else {
                executeSmartAction(actionName)
            }
        }

        view.onAudioOutputClick = { actionManager.launchAudioOutputSwitcher(currentMedia?.appPackageName) }
        view.onPlayPauseClick = { if (currentMedia?.isPlaying == true) mediaManager.sendMediaCommand("PAUSE") else mediaManager.sendMediaCommand("PLAY") }
        view.onPrevClick = { mediaManager.sendMediaCommand("PREV") }
        view.onNextClick = { mediaManager.sendMediaCommand("NEXT") }
        view.onSeekTo = { position -> mediaManager.activeMediaController?.transportControls?.seekTo(position) }
        view.onCustomMediaAction = { action -> mediaManager.activeMediaController?.transportControls?.sendCustomAction(action, null) }

        scope.launch {
            islandState.collect { state ->
                view.islandState.value = state
                val isVisible = state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING
                mediaManager.isIslandVisible = isVisible
                hardwareMonitor.isDashboardOpen = (state == IslandState.TYPE_3_MAX)
            }
        }
        scope.launch { activeModel.collect { model -> view.activeModel.value = model } }
        scope.launch { splitModel.collect { model -> view.splitModel.value = model } }
        return view
    }

    private fun executeSmartAction(actionName: String) {
        when (actionName) {
            "PLAY_PAUSE" -> { if (currentMedia?.isPlaying == true) mediaManager.sendMediaCommand("PAUSE") else mediaManager.sendMediaCommand("PLAY") }
            "NEXT_TRACK" -> mediaManager.sendMediaCommand("NEXT")
            "PREV_TRACK" -> mediaManager.sendMediaCommand("PREV")
            "VOLUME" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
            }
            "BRIGHTNESS" -> { }
            "PREV_APP" -> {
                context.sendBroadcast(Intent("com.example.dynamicisland.GLOBAL_ACTION").putExtra("action", "PREV_APP"))
            }
            "SCREENSHOT" -> {
                val intent = Intent("com.example.dynamicisland.TRIGGER_SCREENSHOT")
                context.sendBroadcast(intent)
            }
            "OPEN_DASHBOARD" -> {
                if (_activeModel.value == null) _activeModel.value = LiveActivityModel.Dashboard()
                _islandState.value = IslandState.TYPE_3_MAX
            }
            "OPEN_QUICK_TOGGLES" -> {
                if (_activeModel.value == null || _activeModel.value is LiveActivityModel.Dashboard) {
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
                        if (currentMedia != null) { _islandState.value = IslandState.TYPE_2_MID }
                        else { _activeModel.value = LiveActivityModel.Dashboard(activeTiles = qsTilesCache, pinnedApps = pinnedAppsCache); _islandState.value = IslandState.TYPE_3_MAX }
                    }
                    IslandState.TYPE_1_MINI -> {
                        _activeModel.value = LiveActivityModel.Dashboard(activeTiles = qsTilesCache, pinnedApps = pinnedAppsCache)
                        _islandState.value = IslandState.TYPE_3_MAX
                    }
                    IslandState.TYPE_2_MID, IslandState.TYPE_SPLIT -> {
                        _activeModel.value = LiveActivityModel.Dashboard(activeTiles = qsTilesCache, pinnedApps = pinnedAppsCache)
                        _islandState.value = IslandState.TYPE_3_MAX
                    }
                    else -> {}
                }
                evaluatePriority()
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

    private fun triggerVisualScreenshotFlash() {
        val current = _islandState.value
        _islandState.value = IslandState.HIDDEN
        scope.launch { delay(50); _islandState.value = current }
    }

    private fun postTransientNotification(model: LiveActivityModel, durationMs: Long = 5000L) {
        if (!model.isCritical && topAppPackage.isNotEmpty()) {
            val profile = PerAppProfileManager
                .getProfile(topAppPackage)
            val eventFlag = when (model.type) {
                ActivityType.CALL           -> PerAppProfileManager.Events.CALLS
                ActivityType.CHARGING,
                ActivityType.BATTERY_LOW    -> PerAppProfileManager.Events.CHARGING
                ActivityType.BLUETOOTH,
                ActivityType.WIFI           -> PerAppProfileManager.Events.CONNECTIVITY
                ActivityType.NAVIGATION     -> PerAppProfileManager.Events.NAVIGATION
                else -> PerAppProfileManager.Events.ALERTS
            }
            if (!profile.allowsEvent(eventFlag)) return
        }
        triggerTransitionHaptic(_islandState.value)
        transientJob?.cancel(); transientModel = model; evaluatePriority()
        transientJob = scope.launch { delay(durationMs); transientModel = null; evaluatePriority() }
    }

    fun destroy() {
        try { callManager.javaClass.getMethod("destroy").invoke(callManager) } catch (e: Exception) {}
        try { mediaManager.javaClass.getMethod("destroy").invoke(mediaManager) } catch (e: Exception) {}
        clipboardManager.stopListening()
        connectivityManager.stopListening()
    }

    init {
        hardwareManager.updateVolumeState(null)
        hardwareManager.updateBrightnessState(null)

        val screenFilter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
        context.registerReceiver(screenStateReceiver, screenFilter)
        context.registerComponentCallbacks(componentCallbacks)

        var isTorchOn = false
        val hardwareReceiver = object : BroadcastReceiver() {
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
                                "WiFi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                                "Bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                "Location" -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                "Airplane" -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                                "DND" -> Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
                                "Battery Saver" -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                                "Hotspot" -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
                                "NFC" -> Intent(Settings.ACTION_NFC_SETTINGS)
                                "Cast" -> Intent(Settings.ACTION_CAST_SETTINGS)
                                "Settings" -> Intent(Settings.ACTION_SETTINGS)
                                "Torch" -> {
                                    val cameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                                    val cameraId = cameraManager.cameraIdList[0]
                                    isTorchOn = !isTorchOn
                                    cameraManager.setTorchMode(cameraId, isTorchOn)
                                    null
                                }
                                else -> Intent(Settings.ACTION_SETTINGS)
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

        val hardwareFilter = IntentFilter().apply { addAction("com.example.dynamicisland.ACTION_LAUNCH_APP"); addAction("com.example.dynamicisland.ACTION_QS") }
        context.registerReceiver(hardwareReceiver, hardwareFilter, Context.RECEIVER_EXPORTED)

        callManager.startMonitoring()

        val volFilter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(hardwareSyncReceiver, volFilter)
        context.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true, brightnessObserver)

        val ecoFilter = IntentFilter().apply {
            addAction("com.example.dynamicisland.SYNC_CONFIG")
            addAction("com.crdroid.batterywellbeing.SYSTEM_OVERRIDE")
            addAction("com.crdroid.batterywellbeing.SYSTEM_ALERT")
            addAction("com.crdroid.batterywellbeing.WARNING_1_MINUTE_REMAINING")
            addAction("com.crdroid.batterywellbeing.REALITY_PILL_TICK")
            addAction("com.crdroid.batterywellbeing.SYNC_CONFIG")
            addAction("com.example.dynamicisland.APP_CHANGED")
            addAction("com.example.dynamicisland.LIVE_ACTIVITY_CAUGHT")
            addAction("com.example.dynamicisland.HARDWARE_TOGGLE")
            addAction("com.example.dynamicisland.PANEL_STATE_CHANGED")
            addAction("com.example.dynamicisland.ALARM_SET")
            addAction("com.example.dynamicisland.hook.ContinuityCameraScanner.ACTION_BARCODE")
            addAction("com.example.dynamicisland.EXTERNAL_ACTIVITY_UPDATED")
            addAction("com.example.dynamicisland.EXTERNAL_ACTIVITY_ENDED")
            addAction("com.example.dynamicisland.SCREENSHOT_CAUGHT")
            addAction("com.example.dynamicisland.OTP_CAUGHT")
            addAction(SurfaceFlingerHook.ACTION_FRAME_STATS)
            addAction(CrDroidAPIHook.ACTION_GAME_MODE_CHANGED)
            addAction(CrDroidAPIHook.ACTION_THERMAL_PROFILE)
            addAction(CrDroidAPIHook.ACTION_DISPLAY_MODE)
            addAction(CrDroidAPIHook.ACTION_SMART_CHARGE)
        }

        val securePermission = "com.redwood.permission.SECURE_IPC"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(ecosystemReceiver, ecoFilter, securePermission, null, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(ecosystemReceiver, ecoFilter, securePermission, null)
        }

        PerAppProfileManager.init(context)

        BatteryPlugin.onBatteryChanged = { level, isCharging, color, wattage ->
            if (isChargingEnabled) {
                if (isCharging && !wasCharging) {
                    val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    if (hideInLandscape && isLandscape) {
                        postTransientNotification(LiveActivityModel.Charging("sys_battery", ActivityType.CHARGING, level, true, false), 5000L)
                    } else {
                        _islandState.value = IslandState.TYPE_3_MAX
                        _activeModel.value = LiveActivityModel.Charging(id = "sys_battery", level = level, isPluggedIn = true, isTransient = true, isCritical = false)

                        scope.launch { delay(3000); evaluatePriority() }
                        performCustomHaptic(context, 2, topAppPackage)
                    }
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
            islandView?.pendingNotifColor?.intValue = pendingNotificationColor
            islandView?.hasUnseenNotif?.value = hasUnseenNotification
        }

        BatteryPlugin.start(context)
        mediaManager.start()
        clipboardManager.startListening()
        connectivityManager.startListening()

        if (com.example.dynamicisland.util.IslandProcessUtils.isOwnProcess(context)) {
            try {
                val translationEngine = com.example.dynamicisland.intelligence
                    .IslandTranslationEngine.get(context)
                scope.launch {
                    translationEngine.result.collect { result ->
                        if (result != null) {
                            postTransientNotification(
                                LiveActivityModel.General(
                                    id          = "sys_translation",
                                    type        = ActivityType.MESSAGE,
                                    title       = result.translatedText.ifEmpty { "Translating…" },
                                    dataText    = result.originalText,
                                    accentColor = android.graphics.Color.parseColor("#4FC3F7")
                                ),
                                12_000L
                            )
                        }
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.w("IslandController", "Translation engine init skipped: ${e.message}")
            }

            try {
                val prefs = context.getSharedPreferences(
                    "dynamic_island_prefs", Context.MODE_PRIVATE
                )
                if (prefs.getBoolean("enable_continuity_camera", false)) {
                    val barcodeScanner = com.example.dynamicisland.hook
                        .ContinuityCameraScanner(context)
                    barcodeScanner.start()
                }
            } catch (e: Throwable) {
                android.util.Log.w("IslandController", "Barcode scanner init skipped: ${e.message}")
            }
        }
    }
}