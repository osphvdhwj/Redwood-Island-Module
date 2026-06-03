package com.example.dynamicisland.manager

import android.content.*
import android.database.ContentObserver
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.*
import android.util.LruCache
import android.provider.Settings
import android.view.WindowManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView
import com.example.dynamicisland.hook.*
import com.example.dynamicisland.util.*
import com.example.dynamicisland.audio.AudioBeatDetector
import com.example.dynamicisland.achievements.AchievementManager
import com.example.dynamicisland.accessibility.IslandAccessibilityManager
import com.example.dynamicisland.sensors.ProximityWakeManager
import com.example.dynamicisland.settings.*
import com.example.dynamicisland.ipc.*
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
import com.example.dynamicisland.bridge.MediaBridge
import java.lang.ref.WeakReference
import com.example.dynamicisland.performance.IslandBlurEngine

@Singleton
class IslandController @Inject constructor(
    @ApplicationContext internal val context: Context,
    private val settingsManager: SettingsManager,
    val mediaManager: IslandMediaManager,
    private val hardwareMonitor: IslandHardwareMonitor,
    private val eventBus: IslandEventBus,
    private val hapticsManager: IslandHapticsManager,
    private val networkMonitor: IslandNetworkMonitor,
    private val neuralCore: IslandNeuralCore,
    private val backupManager: IslandBackupManager,
    private val locationManager: IslandLocationManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val activeExternalActivities = mutableMapOf<String, LiveActivityModel.ExternalActivity>()
    private val mediaBridge by lazy { MediaBridge(context, mediaManager) }
    private val blurEngine by lazy { IslandBlurEngine.get(context) }
    
    val actionManager by lazy { IslandActionManager(context, scope) }
    val storageManager by lazy { IslandStorageManager(context) }

    // 🧠 SHARED KNOWLEDGE BASE (Synergy)
    // Caches non-volatile data to prevent redundant IPC/IO across Redwood and Nav Island
    private val knowledgeBase = LruCache<String, Any>(50)

    private var _lastIslandState: IslandState = IslandState.HIDDEN
    private var _lastActiveModel: LiveActivityModel? = null
    private var _lastSplitModel: LiveActivityModel? = null
    private val _activeTheme = mutableStateOf(IslandTheme())
    
    val currentHardware = mutableStateOf<LiveActivityModel.HardwareMonitor?>(null)
    val stashHistory get() = storageManager.stashHistory

    internal var islandView: DynamicIslandView? = null
    private var currentViewSyncJob: Job? = null
    
    private var isChargingEnabled = true
    private var isAlertsEnabled = true
    private var isTimersEnabled = true
    private var isSystemRecording = false
    private var isScreenshotActive = false
    private var isFlashlightActive = false
    private var wasCharging = false
    private var topAppPackage = ""
    private var moduleIndex = 0

    var settingsState by mutableStateOf(SettingsState())
        private set

    private val audioBeatDetector by lazy { AudioBeatDetector() }
    private val accessibilityManager by lazy { IslandAccessibilityManager(context) }
    private val proximityWakeManager by lazy { ProximityWakeManager(context) }

    init {
        scope.launch {
            blurEngine.vibrantColor.collect { vibrant ->
                if (vibrant != null && settingsState.dynamicColors) {
                    val newTheme = _activeTheme.value.copy(
                        accentColor = vibrant,
                        glowColor = vibrant
                    )
                    _activeTheme.value = newTheme
                    islandView?.activeTheme?.value = newTheme
                }
            }
        }
    }

    private val ecoFilter = IntentFilter().apply {
        addAction("com.example.dynamicisland.RELOAD_PREFS")
        addAction("com.example.dynamicisland.SET_CUBE_POS")
        addAction("com.example.dynamicisland.HARDWARE_TOGGLE")
        addAction("com.example.dynamicisland.APP_CHANGED")
        addAction("com.example.dynamicisland.NOTIFICATION_CAUGHT")
        addAction("com.example.dynamicisland.OTP_CAUGHT")
        addAction("com.example.dynamicisland.SPECIALIZED_MEDIA_UPDATE")
        addAction(CrDroidAPIHook.ACTION_GAME_MODE_CHANGED)
        addAction(CrDroidAPIHook.ACTION_THERMAL_PROFILE)
        addAction(InfinityXAPIHook.ACTION_INFINITY_GAME_MODE)
        addAction(InfinityXAPIHook.ACTION_INFINITY_THERMAL)
    }

    private val ecosystemReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.example.dynamicisland.RELOAD_PREFS" -> loadAndApplySettings()
                "com.example.dynamicisland.SPECIALIZED_MEDIA_UPDATE" -> {
                    val pkg = intent.getStringExtra("pkg") ?: return
                    mediaBridge.onSpecializedUpdate(pkg, intent)
                }
                "com.example.dynamicisland.NOTIFICATION_CAUGHT" -> {
                    val pkg = intent.getStringExtra("pkg") ?: ""
                    val notif = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra("notification", android.app.Notification::class.java) else intent.getParcelableExtra("notification")
                    if (notif != null) notificationManager.processIncomingNotification(pkg, notif)
                }
                "com.example.dynamicisland.OTP_CAUGHT" -> {
                    val otp = intent.getStringExtra("otp") ?: return
                    val otpModel = LiveActivityModel.Otp(code = otp)
                    IslandPriorityEngineV2.post(IslandPriorityEngineV2.buildOtpEvent(otpModel))
                    evaluatePriority()
                }
                "com.example.dynamicisland.APP_CHANGED" -> {
                    topAppPackage = intent.getStringExtra("pkg") ?: ""
                    evaluatePriority()
                }
                "com.example.dynamicisland.HARDWARE_TOGGLE" -> {
                    val type = intent.getStringExtra("type") ?: ""
                    val active = intent.getBooleanExtra("active", false)
                    if (type == "FLASHLIGHT") isFlashlightActive = active
                    evaluatePriority()
                }
            }
        }
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> { mediaManager.isScreenOn = true; hardwareMonitor.isScreenOn = true; evaluatePriority() }
                Intent.ACTION_SCREEN_OFF -> { mediaManager.isScreenOn = false; hardwareMonitor.isScreenOn = false; evaluatePriority() }
            }
        }
    }

    fun start(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(screenStateReceiver, filter)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(ecosystemReceiver, ecoFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(ecosystemReceiver, ecoFilter)
        }
        
        loadAndApplySettings()
        hardwareMonitor.onHardwareUpdate = { 
            currentHardware.value = it
            evaluatePriority() 
        }
    }

    fun setSystemRecordingState(active: Boolean) {
        isSystemRecordingActive = active
        evaluatePriority()
    }

    fun triggerAssistantAura(progress: Float) {
        evaluatePriority()
    }

    fun interceptAssistant(): Boolean {
        return settingsState.assistBridgeEnabled
    }

    fun setSystemScreenshotActive(active: Boolean) {
        isScreenshotActiveInternal = active
        evaluatePriority()
    }

    fun loadAndApplySettings() {
        settingsState = settingsManager.getSettingsState()
        applySettings(settingsState)
    }

    private fun applySettings(state: SettingsState) {
        mediaManager.isMediaEnabled = state.nowPlaying
        mediaManager.allowedMusicApps = state.allowedMusicApps
        mediaManager.allowedMediaApps = state.allowedMediaApps
        evaluatePriority()
    }

    private var isKeyboardVisible = false
    private var isSystemRecordingActive = false
    private var isScreenshotActiveInternal = false

    fun evaluatePriority() {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        // 🛡️ PRO-GRADE VISIBILITY OVERRIDES
        if (isKeyboardVisible || isSystemRecordingActive || isScreenshotActiveInternal) {
            _lastIslandState = IslandState.HIDDEN
            scope.launch { eventBus.emit(IslandIntent.SyncState(IslandState.HIDDEN, null, null)) }
            return
        }

        // 🧠 SYNERGY CHECK: If Nav Island is active, Redwood Island can 'share' data
        // For AOSP: we prioritize Nav Island for controls, Redwood for info.
        if (settingsState.navIslandMode) {
             knowledgeBase.put("REDWOOD_ACTIVE", _lastIslandState != IslandState.HIDDEN)
        }

        IslandPriorityEngineV2.updateContext(
            screenOn = mediaManager.isScreenOn,
            gaming = hardwareMonitor.isGamingModeOn || topAppPackage.contains("game"),
            panelExpanded = false, 
            landscape = isLandscape,
            navMode = settingsState.navIslandMode,
            pinned = settingsState.pinnedApps.toList(),
            mIndex = moduleIndex
        )

        val result = IslandPriorityEngineV2.evaluate()
        val newState = result.islandState
        val newModel = result.winningEvent?.model
        val newSplit = result.splitEvent?.model

        if (newState != _lastIslandState || newModel != _lastActiveModel || newSplit != _lastSplitModel) {
            // 🧹 AGGRESSIVE RECYCLING (3-4 Day Stability)
            if (newModel != _lastActiveModel) {
                (_lastActiveModel as? LiveActivityModel.Music)?.albumArt?.let { if (it != (newModel as? LiveActivityModel.Music)?.albumArt) it.recycle() }
                (_lastActiveModel as? LiveActivityModel.Music)?.blurredAlbumArt?.recycle()
                (_lastActiveModel as? LiveActivityModel.Music)?.appIcon?.recycle()
                (_lastActiveModel as? LiveActivityModel.Call)?.contactPhoto?.recycle()
            }

            _lastIslandState = newState
            _lastActiveModel = newModel
            _lastSplitModel = newSplit
            scope.launch { eventBus.emit(IslandIntent.SyncState(newState, newModel, newSplit)) }
            triggerTransitionHaptic(newState)
        }
    }

    private fun triggerTransitionHaptic(state: IslandState) {
        hapticsManager.triggerTransitionHaptic(state, null, topAppPackage)
    }

    fun createIslandView(view: DynamicIslandView): DynamicIslandView {
        this.islandView = view
        view.controller = this
        
        view.onGestureEvent = { gesture ->
            val suffix = when (gesture) {
                IslandGesture.TAP, IslandGesture.SINGLE_TAP -> "single_tap"
                IslandGesture.DOUBLE_TAP -> "double_tap"
                IslandGesture.LONG_PRESS -> "long_press"
                IslandGesture.SWIPE_LEFT -> "swipe_left"
                IslandGesture.SWIPE_RIGHT -> "swipe_right"
                IslandGesture.SWIPE_UP -> "swipe_up"
                IslandGesture.SWIPE_DOWN -> "swipe_down"
                IslandGesture.TWO_FINGER_TAP -> "two_finger_tap"
                IslandGesture.PINCH_IN -> "pinch_in"
                IslandGesture.PINCH_OUT -> "pinch_out"
                else -> "none"
            }

            if (gesture == IslandGesture.TWO_FINGER_TAP) {
                executeSmartAction("TOGGLE_FLASHLIGHT")
            } else if (gesture == IslandGesture.PINCH_OUT) {
                _lastIslandState = IslandState.TYPE_3_MAX
                evaluatePriority()
            } else {
                val action = settingsManager.getRawString("${_lastIslandState.name}_$suffix", "NONE")
                if (action != "NONE") executeSmartAction(action)
            }
        }

        currentViewSyncJob?.cancel()
        val syncJob = Job()
        
        val layoutListener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            val r = android.graphics.Rect()
            view.getWindowVisibleDisplayFrame(r)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - r.bottom
            val nowVisible = keypadHeight > screenHeight * 0.15
            if (nowVisible != isKeyboardVisible) {
                isKeyboardVisible = nowVisible
                evaluatePriority()
            }
        }

        view.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: android.view.View) {
                v.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
                scope.launch(syncJob) {
                    eventBus.intents.collect { intent ->
                        if (intent is IslandIntent.SyncState) {
                            view.islandState.value = intent.state
                            view.activeModel.value = intent.activeModel
                            view.splitModel.value = intent.splitModel
                        }
                    }
                }
            }
            override fun onViewDetachedFromWindow(v: android.view.View) { 
                v.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
                syncJob.cancel() 
            }
        })
        currentViewSyncJob = syncJob
        return view
    }

    fun executeSmartAction(action: String, data: Intent? = null) {
        val model = _lastActiveModel
        when (action) {
            "TOGGLE_FLASHLIGHT" -> setSystemFlashlightActive(!isFlashlightActive)
            "OPEN_DASHBOARD" -> { _lastIslandState = IslandState.TYPE_3_MAX; evaluatePriority() }
            "MUTE_TOGGLE" -> {
                val newState = !audioManager.isMicrophoneMute
                audioManager.setMicrophoneMute(newState)
                postTransientNotification(
                    LiveActivityModel.General("sys_mute", ActivityType.MESSAGE, "Microphone", if(newState) "Muted" else "Active", android.graphics.Color.RED), 
                    2000L
                )
            }
            "PLAY_PAUSE" -> mediaManager.sendMediaCommand("PLAY_PAUSE")
            "NEXT_TRACK" -> mediaManager.sendMediaCommand("NEXT")
            "PREV_TRACK" -> mediaManager.sendMediaCommand("PREV")
            "LIKE_TRACK" -> {
                if (model is LiveActivityModel.Music) mediaBridge.sendSpecializedCommand(model.appPackageName ?: "", "LIKE")
            }
            "UNLIKE_TRACK" -> {
                if (model is LiveActivityModel.Music) mediaBridge.sendSpecializedCommand(model.appPackageName ?: "", "UNLIKE")
            }
        }
    }

    fun toggleCalibrationMode() {
        val view = islandView ?: return
        val current = view.calibrationMode.value
        if (current) {
            // Save settings on exit
            settingsManager.saveLayoutPositions(
                ring = android.graphics.PointF(view.ringX.value, view.ringY.value),
                mini = android.graphics.PointF(view.miniX.value, view.miniY.value),
                mid  = android.graphics.PointF(view.midX.value, view.midY.value),
                max  = android.graphics.PointF(view.maxX.value, view.maxY.value)
            )
        }
        view.calibrationMode.value = !current
        evaluatePriority()
    }

    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    private fun setSystemFlashlightActive(active: Boolean) {
        val intent = Intent("com.example.dynamicisland.SET_FLASHLIGHT")
        intent.putExtra("active", active)
        context.sendBroadcast(intent)
    }

    fun postTransientNotification(model: LiveActivityModel, duration: Long) {
        scope.launch {
            _lastActiveModel = model
            _lastIslandState = IslandState.TYPE_2_MID
            eventBus.emit(IslandIntent.SyncState(IslandState.TYPE_2_MID, model, null))
            delay(duration)
            evaluatePriority()
        }
    }

    fun destroy() {
        context.unregisterReceiver(screenStateReceiver)
        context.unregisterReceiver(ecosystemReceiver)
        scope.cancel()
        mediaManager.destroy()
        callManager.destroy()
    }

    private val callManager = IslandCallManager(context, audioManager, scope) { evaluatePriority() }
    internal val notificationManager = IslandNotificationManager(context, scope, 
        onProgressCaught = { evaluatePriority() },
        onNavigationCaught = { postTransientNotification(it, 5000L) },
        onNotificationStackCaught = { evaluatePriority() }
    )
}
