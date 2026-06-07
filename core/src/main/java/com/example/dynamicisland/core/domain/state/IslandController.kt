package com.example.dynamicisland.core.domain.state

import android.content.*
import android.database.ContentObserver
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.util.LruCache
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.dynamicisland.core.accessibility.IslandAccessibilityManager
import com.example.dynamicisland.core.achievements.AchievementManager
import com.example.dynamicisland.core.audio.AudioBeatDetector
import com.example.dynamicisland.core.bridge.MediaBridge
import com.example.dynamicisland.core.data.repository.BatteryRepository
import com.example.dynamicisland.core.data.repository.HardwareRepository
import com.example.dynamicisland.core.data.repository.cleanup.CleanerManager
import com.example.dynamicisland.core.gesture.IslandGesture
import com.example.dynamicisland.core.gesture.MLGestureClassifier
import com.example.dynamicisland.core.hook.CrDroidAPIHook
import com.example.dynamicisland.core.hook.InfinityXAPIHook
import com.example.dynamicisland.core.intelligence.IslandPredictionEngine
import com.example.dynamicisland.core.performance.IslandBlurEngine
import com.example.dynamicisland.core.sensors.ProximityWakeManager
import com.example.dynamicisland.core.ui.DynamicIslandView
import com.example.dynamicisland.core.util.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.ipc.BrainRelay
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.model.IslandShape
import com.example.dynamicisland.core.model.IslandTheme
import com.example.dynamicisland.shared.model.IslandUiState
import com.example.dynamicisland.shared.settings.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 🚀 ELITE ISLAND CONTROLLER
 * 
 * The central logic hub for the standalone Root Core Application.
 * Orchestrates UI state, hardware interactions, and AI intelligence.
 */
@Singleton
class IslandController @Inject constructor(
    @ApplicationContext internal val context: Context,
    private val settingsManager: SettingsManager,
    val mediaManager: IslandMediaManager,
    private val hardwareRepository: HardwareRepository,
    private val hapticsManager: IslandHapticsManager,
    private val networkMonitor: IslandNetworkMonitor,
    val neuralCore: IslandNeuralCore,
    private val backupManager: IslandBackupManager,
    private val locationManager: IslandLocationManager,
    private val predictionEngine: IslandPredictionEngine,
    val gestureClassifier: MLGestureClassifier,
    private val cleanerManager: CleanerManager,
    private val batteryRepository: BatteryRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val activeExternalActivities = mutableMapOf<String, LiveActivityModel.ExternalActivity>()
    private val mediaBridge by lazy { MediaBridge(context, mediaManager) }
    private val blurEngine by lazy { IslandBlurEngine.get(context) }
    
    val actionManager by lazy { IslandActionManager(context, scope) }
    val storageManager by lazy { IslandStorageManager(context) }

    private var _lastIslandState: IslandState = IslandState.HIDDEN
    private var _lastActiveModel: LiveActivityModel? = null
    private var _lastSplitModel: LiveActivityModel? = null
    private val _activeTheme = mutableStateOf(IslandTheme())
    
    val currentHardware = mutableStateOf<LiveActivityModel.HardwareMonitor?>(null)
    val stashHistory get() = storageManager.stashHistory

    internal var islandView: DynamicIslandView? = null
    private var currentViewSyncJob: Job? = null
    
    private var isSystemRecordingActive = false
    private var isScreenshotActiveInternal = false
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
        // --- 🎨 Dynamic Color Sync ---
        scope.launch {
            blurEngine.vibrantColor.collect { vibrant ->
                if (vibrant != null && settingsState.dynamicColors) {
                    val newTheme = _activeTheme.value.copy(
                        accentColor = Color(vibrant),
                        glowColor = Color(vibrant)
                    )
                    _activeTheme.value = newTheme
                    islandView?.activeTheme?.value = newTheme
                }
            }
        }

        // --- 🤖 AI INTELLIGENCE LOOP ---
        scope.launch {
            predictionEngine.prediction.collect { prediction ->
                if (prediction != null && prediction.confidenceScore > 0.7f) {
                    RedwoodLogger.i("AI Prediction: User likely to open ${prediction.predictedPackage}")
                    DensityAwareIconCache.get(context).getOrLoadIcon(context, prediction.predictedPackage)
                }
            }
        }

        // --- 👆 ML GESTURE LOOP ---
        scope.launch {
            gestureClassifier.gestureFlow.collect { result ->
                if (!result.wasAccidental) {
                    handleGesture(result.gesture)
                }
            }
        }
    }

    private val ecosystemReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.example.dynamicisland.RELOAD_PREFS" -> loadAndApplySettings()
                "com.example.dynamicisland.APP_CHANGED" -> {
                    topAppPackage = intent.getStringExtra("pkg") ?: ""
                    predictionEngine.onAppForegrounded(topAppPackage)
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
                Intent.ACTION_SCREEN_ON -> { 
                    mediaManager.isScreenOn = true; 
                    hardwareRepository.isScreenOn = true; 
                    neuralCore.dispatch(IslandIntent.UpdateScreenState(true))
                    evaluatePriority() 
                }
                Intent.ACTION_SCREEN_OFF -> { 
                    mediaManager.isScreenOn = false; 
                    hardwareRepository.isScreenOn = false; 
                    neuralCore.dispatch(IslandIntent.UpdateScreenState(false))
                    evaluatePriority() 
                }
            }
        }
    }

    fun start(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(screenStateReceiver, filter)
        
        val ecoFilter = IntentFilter().apply {
            addAction("com.example.dynamicisland.RELOAD_PREFS")
            addAction("com.example.dynamicisland.HARDWARE_TOGGLE")
            addAction("com.example.dynamicisland.APP_CHANGED")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(ecosystemReceiver, ecoFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(ecosystemReceiver, ecoFilter)
        }
        
        loadAndApplySettings()
        cleanerManager.onStart()
        hardwareRepository.onStart()
        batteryRepository.onStart()

        scope.launch {
            batteryRepository.batteryState.collect { state ->
                neuralCore.dispatch(IslandIntent.UpdateBattery(state.level, state.isCharging))
                evaluatePriority()
            }
        }
    }

    fun loadAndApplySettings() {
        settingsState = settingsManager.getSettingsState()
        neuralCore.dispatch(IslandIntent.UpdateSettings(settingsState))
        applySettings(settingsState)
    }

    private fun applySettings(state: SettingsState) {
        mediaManager.isMediaEnabled = state.nowPlaying
        mediaManager.allowedMusicApps = state.allowedMusicApps
        mediaManager.allowedMediaApps = state.allowedMediaApps
        evaluatePriority()
    }

    fun autoDetectCutout() {
        val view = islandView ?: return
        val insets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) view.rootWindowInsets else null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout = insets?.displayCutout
            if (cutout != null) {
                val rect = cutout.boundingRects.firstOrNull()
                if (rect != null) {
                    val density = context.resources.displayMetrics.density
                    val x = rect.centerX() / density
                    val y = rect.centerY() / density
                    view.ringX.floatValue = x
                    view.ringY.floatValue = y
                    XposedBridge.log("Redwood: Auto-detected cutout at ($x, $y)")
                }
            }
        }
    }

    fun is3ButtonNavActive(): Boolean {
        return Settings.Secure.getInt(context.contentResolver, "navigation_mode", 2) == 0
    }

    fun evaluatePriority() {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        if (isSystemRecordingActive || isScreenshotActiveInternal) {
            neuralCore.dispatch(IslandIntent.SyncState(IslandState.HIDDEN, null, null))
            return
        }

        // --- Hardware stats integration ---
        val hw = hardwareRepository.hardwareState.value
        if (hw != null) {
            neuralCore.dispatch(IslandIntent.UpdateGamingStats(
                fps = hw.fps,
                frameMs = hw.frameMs,
                jankPct = hw.jankPct,
                cpuUsage = hw.cpuFreqMhz, // Simplified mapping
                gpuUsage = 0
            ))
        }

        val newState = if (mediaManager.isPlaying) IslandState.TYPE_2_MID else IslandState.TYPE_0_RING
        val newModel = if (mediaManager.isPlaying) mediaManager.currentMediaModel else null

        if (newState != _lastIslandState || newModel != _lastActiveModel) {
            _lastIslandState = newState
            _lastActiveModel = newModel
            neuralCore.dispatch(IslandIntent.SyncState(newState, newModel, null))
            triggerTransitionHaptic(newState)
        }
    }

    private fun handleGesture(gesture: IslandGesture) {
        RedwoodLogger.d("Handling ML Gesture: $gesture")
        val suffix = when (gesture) {
            IslandGesture.TAP, IslandGesture.SINGLE_TAP -> "single_tap"
            IslandGesture.DOUBLE_TAP -> "double_tap"
            IslandGesture.LONG_PRESS -> "long_press"
            IslandGesture.SWIPE_LEFT -> "swipe_left"
            IslandGesture.SWIPE_RIGHT -> "swipe_right"
            IslandGesture.SWIPE_UP -> "swipe_up"
            IslandGesture.SWIPE_DOWN -> "swipe_down"
            else -> "none"
        }

        if (gesture == IslandGesture.TWO_FINGER_TAP) {
            executeSmartAction("TOGGLE_FLASHLIGHT")
        } else {
            val action = settingsManager.getRawString("${_lastIslandState.name}_$suffix", "NONE")
            if (action != "NONE") executeSmartAction(action)
        }
    }

    private fun triggerTransitionHaptic(state: IslandState) {
        hapticsManager.triggerTransitionHaptic(state, null, topAppPackage)
    }

    fun createIslandView(view: DynamicIslandView): DynamicIslandView {
        this.islandView = view
        view.controller = this
        
        currentViewSyncJob?.cancel()
        val syncJob = Job()

        view.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: android.view.View) {
                scope.launch(syncJob) {
                    neuralCore.uiState.collect { state ->
                        view.islandState.value = state.islandState
                        view.activeModel.value = state.activeModel
                        view.splitModel.value = state.splitModel
                        if (state.isBatteryPulsing) view.triggerBatteryPulse()
                        view.updateBattery(state.batteryLevel, state.isCharging)
                        view.gpuLoad.floatValue = state.gamingGpuUsage / 100f
                    }
                }
            }
            override fun onViewDetachedFromWindow(v: android.view.View) { 
                syncJob.cancel() 
            }
        })
        currentViewSyncJob = syncJob
        return view
    }

    fun applyProactivePerformance(pkg: String) {
        if (pkg.contains("game", ignoreCase = true) || pkg.contains("benchmark", ignoreCase = true)) {
            RedwoodLogger.i("AI Intent: Game detected. Pre-applying Wild Mode.")
            executeSmartAction("SET_PERFORMANCE_WILD")
        }
    }

    fun executeSmartAction(action: String, data: Intent? = null) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (action) {
            "TOGGLE_FLASHLIGHT" -> setSystemFlashlightActive(!isFlashlightActive)
            "OPEN_DASHBOARD" -> { _lastIslandState = IslandState.TYPE_3_MAX; evaluatePriority() }
            "PLAY_PAUSE" -> mediaManager.sendMediaCommand("PLAY_PAUSE")
            "NEXT_TRACK" -> mediaManager.sendMediaCommand("NEXT")
            "PREV_TRACK" -> mediaManager.sendMediaCommand("PREV")
            "SET_PERFORMANCE_WILD" -> {
                hardwareRepository.setPerformanceLevel(com.example.dynamicisland.core.data.repository.GameHubRepository.PerformanceLevel.WILD)
                postTransientNotification(
                    LiveActivityModel.General("sys_perf", ActivityType.MESSAGE, "Performance", "Wild Mode Active", android.graphics.Color.YELLOW),
                    3000L
                )
            }
        }
    }

    private fun setSystemFlashlightActive(active: Boolean) {
        val intent = Intent("com.example.dynamicisland.SET_FLASHLIGHT")
        intent.putExtra("active", active)
        context.sendBroadcast(intent)
    }

    fun postTransientNotification(model: LiveActivityModel, duration: Long) {
        scope.launch {
            _lastActiveModel = model
            _lastIslandState = IslandState.TYPE_2_MID
            neuralCore.dispatch(IslandIntent.SyncState(IslandState.TYPE_2_MID, model, null))
            delay(duration)
            evaluatePriority()
        }
    }

    fun destroy() {
        try { context.unregisterReceiver(screenStateReceiver) } catch (_: Exception) {}
        try { context.unregisterReceiver(ecosystemReceiver) } catch (_: Exception) {}
        batteryRepository.onStop()
        hardwareRepository.onStop()
        cleanerManager.onStop()
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
