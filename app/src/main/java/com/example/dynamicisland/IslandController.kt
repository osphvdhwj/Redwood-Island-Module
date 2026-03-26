@file:Suppress("DEPRECATION")
package com.example.dynamicisland

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.Rating
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import android.util.LruCache
import java.util.concurrent.ConcurrentHashMap
import android.app.ActivityOptions
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collectLatest

class IslandController(private val context: Context) {

    private var mediaProcessJob: Job? = null

    private var isAutoBrightnessEnabled = false
    private var currentRingerMode = AudioManager.RINGER_MODE_NORMAL
    private var windowManager: WindowManager? = null
    private var currentCall: LiveActivityModel.Call? = null
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
    private var userForceCollapsed = false 
    private var lastReportedBattery = -1
    private var wasCharging = false 
    private var isScreenOn = true 
    private var topAppPackage = "" 
    private var lastTrackTitle = ""
    private var isPeeking = false

    private var isMediaEnabled = true
    private var isChargingEnabled = true
    private var isAlertsEnabled = true
    private var isTimersEnabled = true

    private val gestureMatrix = mutableMapOf<String, IslandAction>()

    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeMediaController: MediaController? = null
    private var mediaTickerJob: Job? = null

    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // In IslandController.kt
    private val volumeFlow = MutableStateFlow(-1)

    init {
        scope.launch(Dispatchers.IO) {
            volumeFlow
                .filter { it >= 0 }
                .collectLatest { percent ->
                    delay(50) // Debounce rapid drag events
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val targetVolume = ((percent.toFloat() / 100f) * maxVolume).toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                }
        }
    }

    private val brightnessObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            updateBrightnessState()
        }
    }

    private val hardwareSyncReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                updateVolumeState()
            }
        }
    }

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
                Intent.ACTION_SCREEN_OFF -> { isScreenOn = false; stopMediaTicker() }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    if (currentMedia?.isPlaying == true && isMediaEnabled) startMediaTicker()
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
                "com.example.dynamicisland.APP_CHANGED" -> { topAppPackage = intent.getStringExtra("pkg") ?: ""; evaluatePriority() }
                
                "com.example.dynamicisland.LIVE_ACTIVITY_CAUGHT" -> {
                    val pkg = intent.getStringExtra("pkg") ?: return
                    val title = intent.getStringExtra("title") ?: ""
                    val text = intent.getStringExtra("text") ?: ""
                    val progress = intent.getIntExtra("progress", -1)
                    val progressMax = intent.getIntExtra("progressMax", -1)
                    
                    if (progress != -1 && progressMax > 0) {
                        postTransientNotification(LiveActivityModel.OngoingTask(pkgName = pkg, title = title, text = text, progress = progress, progressMax = progressMax), 4000L)
                    }
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
                            postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_thermal", alertType = "THERMAL", title = "Thermal Throttling", message = "Battery temp at $temp", alertColor = android.graphics.Color.RED), 6000L)
                        }
                        "ROGUE_APP_DETECTED" -> {
                            val appName = intent.getStringExtra("extra_info") ?: "Unknown App"
                            postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_rogue", alertType = "ROGUE", title = "High Battery Drain", message = "$appName is draining battery", alertColor = android.graphics.Color.rgb(255, 165, 0)), 6000L)
                        }
                    }
                }
                "com.crdroid.batterywellbeing.SYSTEM_ALERT" -> {
                    if (!isAlertsEnabled) return
                    val alertType = intent.getStringExtra("alertType") ?: "INFO"
                    val title = intent.getStringExtra("title") ?: "System Alert"
                    val message = intent.getStringExtra("message") ?: ""
                    val colorHex = intent.getStringExtra("colorHex") ?: "#FFFFFF"
                    val colorInt = try { android.graphics.Color.parseColor(colorHex) } catch(e: Exception) { android.graphics.Color.WHITE }
                    postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_alert_$alertType", alertType = alertType, title = title, message = message, alertColor = colorInt), 5000L)
                }
            }
        }
    }

    private val componentCallbacks = object : android.content.ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) { evaluatePriority() }
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onLowMemory() {}
        override fun onTrimMemory(level: Int) { if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) iconCache.evictAll() }
    }

    // 🎛️ FIXED: Send to background flow instead of blocking UI Thread
    fun setSystemVolume(percent: Int) {
        try {
            // 1. Instantly update the UI slider so it feels 120fps smooth
            islandView?.updateHardwareVolume(percent)
            
            // 2. Pass the data to our background Coroutine Flow (which safely handles the audioManager)
            volumeFlow.value = percent
        } catch (e: Exception) {}
    }

    // 🎛️ FIXED: Dynamic Hardware Scaling (Bypasses 255 hardcode limits on Xiaomi/Poco)
    fun setSystemBrightness(percent: Int) {
        try {
            val resolver = context.contentResolver
            
            // Fetch true hardware ceiling (e.g. 4095 on Poco), fallback to 255 if standard AOSP
            val maxBrightness = try { 
                Settings.System.getInt(resolver, "screen_brightness_maximum") 
            } catch (e: Exception) { 
                255 
            }
            
            // Map 0-100% UI slider perfectly to 0-HardwareMax
            val targetBrightness = ((percent.toFloat() / 100f) * maxBrightness).toInt()
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, targetBrightness)
            
            // Push immediately to local state, avoiding the system return loop
            islandView?.updateHardwareBrightness(percent)
        } catch (e: Exception) {}
    }

    private fun updateVolumeState() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val percent = if (maxVolume > 0) (current * 100) / maxVolume else 0
        islandView?.updateHardwareVolume(percent)
    }

    private fun updateBrightnessState() {
        try {
            val resolver = context.contentResolver
            val brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
            val maxBrightness = try { Settings.System.getInt(resolver, "screen_brightness_maximum") } catch (e: Exception) { 255 }
            
            isAutoBrightnessEnabled = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == 1
            
            val percent = ((brightness.toFloat() / maxBrightness.toFloat()) * 100f).toInt()
            islandView?.updateHardwareBrightness(percent.coerceIn(0, 100))
            islandView?.updateAutoBrightnessState(isAutoBrightnessEnabled)
        } catch (e: Exception) {}
    }

    fun toggleAutoBrightness() {
        try {
            val resolver = context.contentResolver
            val newMode = if (isAutoBrightnessEnabled) 0 else 1
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, newMode)
            updateBrightnessState()
        } catch (e: Exception) {}
    }

    fun toggleRingerMode() {
        try {
            currentRingerMode = audioManager.ringerMode
            val nextMode = when (currentRingerMode) {
                AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            audioManager.ringerMode = nextMode
            islandView?.updateRingerState(nextMode)
        } catch (e: Exception) {}
    }

    private fun executeBackgroundIntent(intent: android.content.Intent) {
        try {
            val options = android.app.ActivityOptions.makeBasic()
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                options.pendingIntentBackgroundActivityStartMode = android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
            val pendingIntent = android.app.PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            pendingIntent.send(options.toBundle())
            userForceCollapsed = true
            _islandState.value = IslandState.TYPE_0_RING
            evaluatePriority()
        } catch (e: Exception) {
            try { context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e2: Exception) {}
        }
    }

    private fun launchAppIntent(packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            executeBackgroundIntent(launchIntent)
        }
    }

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams): android.view.View {
        val moduleContext = try { context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) } catch (e: Exception) { context }
        val view = DynamicIslandView(context, moduleContext)
        this.islandView = view 
        this.windowManager = wm 

        // 🎛️ Connect Callbacks
        view.onVolumeDrag = { pct -> setSystemVolume(pct) }
        view.onBrightnessDrag = { pct -> setSystemBrightness(pct) }
        view.onMicToggle = { toggleMicMute() }
        view.onSpeakerToggle = { toggleSpeakerphone() }
        view.onEndCallClick = { endActiveCall() }
        view.onOpenCallUI = { openNativeCallUI() }
        view.onAutoBrightnessToggle = { toggleAutoBrightness() }
        view.onRingerToggle = { toggleRingerMode() }
        
        view.onSplitPillClick = { 
            val sModel = _splitModel.value;
            if (sModel is LiveActivityModel.Charging) { 
                try {
                    val intent = Intent().setComponent(ComponentName("com.crdroid.batterywellbeing", "com.crdroid.batterywellbeing.MainActivity"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                } catch(e: Throwable) { _islandState.value = IslandState.TYPE_CUBE }
            } 
        }
        
        view.windowManager = wm;
        view.windowParams = params
        view.updateRingerState(audioManager.ringerMode)

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
                        try { if (key.startsWith("TYPE_")) { gestureMatrix[key] = IslandAction.valueOf(json.getString(key)) } } catch (e: Exception) {} 
                    }
                }
            } catch (e: Exception) {}
        }

        view.onGestureEvent = { gesture ->
            val currentState = _islandState.value.name
            val actionName = gestureMatrix["${currentState}_${gesture.name}"]?.name ?: "NONE"

            when (actionName) {
                "PLAY_PAUSE" -> { if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY") }
                "NEXT_TRACK" -> sendMediaCommand("NEXT")
                "PREV_TRACK" -> sendMediaCommand("PREV")
                "OPEN_DASHBOARD" -> {
                    if (_activeModel.value == null) _activeModel.value = LiveActivityModel.Dashboard()
                    _islandState.value = IslandState.TYPE_3_MAX
                }
                "OPEN_QUICK_TOGGLES" -> {
                    if (_activeModel.value == null || _activeModel.value is LiveActivityModel.Dashboard) {
                        _activeModel.value = LiveActivityModel.Dashboard()
                        _islandState.value = IslandState.TYPE_2_MID
                    }
                }
                "OPEN_QUICK_TOGGLES" -> {
                    if (_activeModel.value == null || _activeModel.value is LiveActivityModel.Dashboard) {
                        _activeModel.value = LiveActivityModel.Dashboard()
                        _islandState.value = IslandState.TYPE_2_MID
                    }
                }
                "COLLAPSE" -> {
                    if (_activeModel.value is LiveActivityModel.Dashboard) {
                        if (currentMedia != null) {
                            if (currentMedia?.isPlaying == true) {
                                _islandState.value = IslandState.TYPE_1_MINI
                                userForceCollapsed = false
                            } else {
                                _islandState.value = IslandState.TYPE_0_RING
                                userForceCollapsed = true
                            }
                            _activeModel.value = currentMedia
                            evaluatePriority() 
                        } else {
                            _islandState.value = IslandState.TYPE_0_RING
                            evaluatePriority()
                        }
                    } else {
                        userForceCollapsed = true
                        _islandState.value = IslandState.TYPE_0_RING
                        evaluatePriority()
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
                            if (currentMedia?.isPlaying == false) {
                                _activeModel.value = LiveActivityModel.Dashboard(); _islandState.value = IslandState.TYPE_3_MAX
                            } else {
                                _islandState.value = IslandState.TYPE_2_MID
                            }
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
                "LAUNCH_SETTINGS" -> launchAppIntent("com.android.settings")
                "LAUNCH_CAMERA" -> {
                    val cameraIntent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try { context.startActivity(cameraIntent) } catch (e: Exception) {}
                }
                else -> {
                    if (actionName.startsWith("LAUNCH_APP_")) {
                        val pkgName = actionName.removePrefix("LAUNCH_APP_")
                        launchAppIntent(pkgName)
                    }
                }
            }
        }
        
        view.onAudioOutputClick = { launchAudioOutputSwitcher() }
        view.onPlayPauseClick = { if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY") }
        view.onPrevClick = { sendMediaCommand("PREV") }
        view.onNextClick = { sendMediaCommand("NEXT") }
        view.onSeekTo = { position -> activeMediaController?.transportControls?.seekTo(position) }
        view.onCustomMediaAction = { action -> activeMediaController?.transportControls?.sendCustomAction(action, null) }

        scope.launch { islandState.collect { state -> view.setState(state) } }
        scope.launch { activeModel.collect { model -> view.setModel(model) } }
        scope.launch { splitModel.collect { model -> view.setSplitModel(model) } }
        return view
    }

    private fun sendMediaCommand(command: String) {
        val controls = activeMediaController?.transportControls ?: return
        try { when (command) { "PLAY" -> controls.play(); "PAUSE" -> controls.pause(); "NEXT" -> controls.skipToNext(); "PREV" -> controls.skipToPrevious() } } catch (e: Exception) {}
    }

    private fun launchAudioOutputSwitcher() {
        try {
            val intent = Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
            intent.setPackage("com.android.systemui")
            currentMedia?.appPackageName?.let { intent.putExtra("package_name", it) }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            try {
                val fallback = Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallback)
            } catch (ex: Exception) {}
        }
    }

    // 🎛️ FIXED: Deterministic Priority Weights
    private fun LiveActivityModel.getPriorityWeight(): Int {
        return when (this) {
            is LiveActivityModel.Call -> if (state == "RINGING") 100 else 90
            is LiveActivityModel.SystemAlert -> if (alertType == "THERMAL") 85 else 60
            is LiveActivityModel.Charging -> if (isCritical) 80 else 40 // 20% warning beats music, but not a call
            is LiveActivityModel.AppTimerWarning -> 70
            is LiveActivityModel.OngoingTask -> 50
            is LiveActivityModel.Music -> 20
            is LiveActivityModel.RealityPill -> 10
            is LiveActivityModel.General -> 5
            is LiveActivityModel.HardwareMonitor -> 1
            else -> 0
        }
    }

    // 🎛️ FIXED: Weighted State Machine Router
    private fun evaluatePriority() {
        val rotation = try { windowManager?.defaultDisplay?.rotation ?: 0 } catch(e: Throwable) { 0 }
        val isLandscapeNow = rotation == android.view.Surface.ROTATION_90 || rotation == android.view.Surface.ROTATION_270
        
        val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        val blacklistedGames = prefs.getString("gaming_blacklist", "com.dts.freefiremax,com.tencent.ig") ?: ""
        val isBlacklistedAppActive = topAppPackage.isNotEmpty() && blacklistedGames.contains(topAppPackage)
        val shouldHideLandscape = isLandscapeNow && prefs.getBoolean("hide_landscape", false)

        // 1. Gather all currently active states
        val activeCandidates = listOfNotNull(
            currentCall,
            transientModel,
            if (isMediaEnabled) currentMedia else null
        ).sortedByDescending { it.getPriorityWeight() }

        val dominantModel = activeCandidates.firstOrNull()

        // 2. Global Hide Overrides (Games/Video)
        if ((shouldHideLandscape || currentHardware?.isGamingModeOn == true || isBlacklistedAppActive)) {
            // ONLY pierce the game overlay if the weight is critical (80+)
            if (dominantModel == null || dominantModel.getPriorityWeight() < 80) {
                _islandState.value = IslandState.HIDDEN
                return
            }
        }

        // 3. Apply the Dominant State
        if (dominantModel != null) {
            _activeModel.value = dominantModel
            
            // 4. Handle Split-Screen (Cube) Logic
            val secondaryModel = activeCandidates.drop(1).firstOrNull { it is LiveActivityModel.Music || it is LiveActivityModel.Charging }
            
            if (secondaryModel != null && dominantModel.getPriorityWeight() >= 80) {
                _splitModel.value = secondaryModel
                _islandState.value = IslandState.TYPE_SPLIT
            } else {
                _splitModel.value = null
                
                // Route to correct visual state based on model type and user interaction
                _islandState.value = when {
                    userForceCollapsed -> IslandState.TYPE_0_RING
                    dominantModel is LiveActivityModel.SystemAlert || dominantModel is LiveActivityModel.Charging || dominantModel is LiveActivityModel.AppTimerWarning -> IslandState.TYPE_2_MID
                    dominantModel is LiveActivityModel.Call && dominantModel.state == "RINGING" -> IslandState.TYPE_1_MINI
                    dominantModel is LiveActivityModel.Call && dominantModel.state == "ONGOING" -> IslandState.TYPE_1_MINI // User must tap to expand to MID
                    dominantModel is LiveActivityModel.Music -> IslandState.TYPE_1_MINI
                    else -> IslandState.TYPE_1_MINI
                }
            }
        } else {
            // Nothing active
            _activeModel.value = null
            _splitModel.value = null
            _islandState.value = IslandState.TYPE_0_RING
        }
    }

    fun postTransientNotification(model: LiveActivityModel, durationMs: Long = 5000L) {
        transientJob?.cancel(); transientModel = model; evaluatePriority()
        transientJob = scope.launch { delay(durationMs); transientModel = null; evaluatePriority() }
    }

    private fun setupMediaListener() {
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, null)
            updateActiveMediaController(getBestMediaController(mediaSessionManager.getActiveSessions(null)))
        } catch (e: Exception) {}
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
        val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
        if (ratio >= 1.0f) return bitmap
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    }

    private fun extractMediaData(controller: MediaController?) {
        if (controller == null || !isMediaEnabled) return

        mediaProcessJob?.cancel()
        
        val metadata = controller.metadata
        val pbState = controller.playbackState ?: return

        val isPlaying = pbState.state == PlaybackState.STATE_PLAYING
        val wasPlaying = currentMedia?.isPlaying == true
        if (!isPlaying && currentMedia == null) return 

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val rawAlbumArt = try { metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART) } catch (e: Exception) { null }
        val albumArtBitmap = getScaledBitmap(rawAlbumArt)
        
        val newTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        val isNewTrack = newTitle != lastTrackTitle && lastTrackTitle.isNotEmpty()
        lastTrackTitle = newTitle

        val isFirstLoad = currentMedia == null || isNewTrack
        if (isFirstLoad) {
            currentMedia = LiveActivityModel.Music(
                id = "media_main", title = newTitle,
                artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
                albumArt = albumArtBitmap, blurredAlbumArt = null,
                appIcon = null, dominantColor = android.graphics.Color.DKGRAY, titleTextColor = android.graphics.Color.WHITE,
                isPlaying = isPlaying, durationMs = duration, positionMs = pbState.position,
                appPackageName = controller.packageName, customActions = emptyList(),
                isShuffled = false, repeatMode = 0, isLiked = false
            )
            evaluatePriority() // Push to UI immediately!
        }

        if (isNewTrack && userForceCollapsed && isPlaying && !isPeeking) {
            isPeeking = true
            userForceCollapsed = false
            scope.launch(Dispatchers.Main) {
                delay(3000)
                userForceCollapsed = true
                isPeeking = false
                evaluatePriority()
            }
        }

        scope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val appIconBmp = try {
                val drawable = pm.getApplicationIcon(controller.packageName)
                val bmp = Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                getScaledBitmap(bmp, 100) 
            } catch(e: Exception) { null }

            var blurredArtBitmap: Bitmap? = null
            var bgColor: Int? = null; var txtColor: Int = android.graphics.Color.WHITE
            if (albumArtBitmap != null) {
                @Suppress("DEPRECATION")
                try {
                    val rs = android.renderscript.RenderScript.create(context)
                    val input = android.renderscript.Allocation.createFromBitmap(rs, albumArtBitmap)
                    val output = android.renderscript.Allocation.createTyped(rs, input.type)
                    val script = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs))
                    script.setRadius(24f)
                    script.setInput(input)
                    script.forEach(output)
                    blurredArtBitmap = Bitmap.createBitmap(albumArtBitmap.width, albumArtBitmap.height, albumArtBitmap.config ?: Bitmap.Config.ARGB_8888)
                    output.copyTo(blurredArtBitmap)
                    input.destroy(); output.destroy(); script.destroy(); rs.destroy()
                } catch (e: Exception) {
                    blurredArtBitmap = albumArtBitmap
                }

                val palette = Palette.from(albumArtBitmap).generate()
                val swatch = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch
                if (swatch != null) {
                    var rgb = swatch.rgb
                    val hsl = FloatArray(3)
                    androidx.core.graphics.ColorUtils.colorToHSL(rgb, hsl)
                    if (hsl[2] < 0.35f) { 
                        hsl[2] = 0.35f 
                        rgb = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
                    }
                    bgColor = rgb
                    txtColor = if (androidx.core.graphics.ColorUtils.calculateLuminance(bgColor) > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            }

            withContext(Dispatchers.Main) {
                currentMedia?.blurredAlbumArt?.takeIf { it != currentMedia?.albumArt }?.recycle()

                val extractedActions = pbState.customActions.map { CustomMediaAction(it.action, null, null, true) }
                var systemLiked = false; var systemShuffle = false; var systemRepeat = 0

                if (pbState.customActions != null) {
                    for (action in pbState.customActions) {
                        val actionId = action.action?.lowercase() ?: ""
                        val localizedName = action.name?.toString()?.lowercase() ?: ""

                        if (actionId.contains("unlike") || actionId.contains("remove") || 
                            localizedName.contains("unlike") || localizedName.contains("remove") ||
                            localizedName.contains("dislike")) {
                            systemLiked = true
                        }

                        if (actionId.contains("shuffle") || localizedName.contains("shuffle")) {
                            if (localizedName.contains("disable") || localizedName.contains("off") || localizedName.contains("stop")) systemShuffle = true
                        }

                        if (actionId.contains("repeat") || localizedName.contains("repeat") || 
                            actionId.contains("loop") || localizedName.contains("loop")) {
                            if (localizedName.contains("one") || localizedName.contains("single")) {
                                systemRepeat = 1 
                            } else if (localizedName.contains("disable") || localizedName.contains("off") || localizedName.contains("stop")) {
                                systemRepeat = 2 
                            }
                        }
                    }
                }

                val extras = pbState.extras
                if (extras != null) {
                    if (extras.containsKey("android.media.session.extra.SHUFFLE_MODE")) {
                        val shuf = extras.getInt("android.media.session.extra.SHUFFLE_MODE", 0)
                        if (shuf == 1 || shuf == 2) systemShuffle = true
                    }
                    if (extras.containsKey("android.media.session.extra.REPEAT_MODE")) {
                        val rep = extras.getInt("android.media.session.extra.REPEAT_MODE", 0)
                        if (rep > 0) systemRepeat = rep
                    }
                }

                currentMedia = LiveActivityModel.Music(
                    id = "media_main", title = newTitle,
                    artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
                    albumArt = albumArtBitmap, blurredAlbumArt = blurredArtBitmap,
                    appIcon = appIconBmp, dominantColor = bgColor, titleTextColor = txtColor,
                    isPlaying = isPlaying, durationMs = duration, positionMs = pbState.position,
                    appPackageName = controller.packageName, customActions = extractedActions,
                    isShuffled = systemShuffle, repeatMode = systemRepeat, isLiked = systemLiked
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
                activeMediaController?.playbackState?.position?.let { pos -> 
                    (activeModel.value as? LiveActivityModel.Music)?.let { islandView?.updateTicker(pos) }
                }
                delay(1000) 
            } 
        }
    }
    
    private fun stopMediaTicker() { mediaTickerJob?.cancel() }
    
    init {
        updateVolumeState()
        updateBrightnessState()

        val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
        context.registerReceiver(screenStateReceiver, filter)
        context.registerComponentCallbacks(componentCallbacks)

        // 🎛️ FIXED: Kernel-Safe Universal Call Detector (Bypasses userfaultfd timeouts)
        val callHandler = Handler(Looper.getMainLooper())
        val callCheckRunnable = object : Runnable {
            override fun run() {
                try {
                    val mode = audioManager.mode
                    val isRinging = mode == AudioManager.MODE_RINGTONE
                    val isInCall = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION

                    // 🎛️ UPGRADED: Distinct States for Ringing vs Active
                    if (isRinging) {
                        if (currentCall?.state != "RINGING") {
                            currentCall = LiveActivityModel.Call(state = "RINGING", startTime = 0L)
                            evaluatePriority()
                        }
                    } else if (isInCall) {
                        if (currentCall?.state != "ONGOING") {
                            currentCall = LiveActivityModel.Call(state = "ONGOING", startTime = System.currentTimeMillis())
                            evaluatePriority()
                        }
                    } else {
                        if (currentCall != null) {
                            currentCall = null
                            evaluatePriority()
                        }
                    }
                } catch (e: Exception) {}

                // Re-queue the check safely without triggering Kernel memory throttles
                if (isScreenOn) {
                    callHandler.postDelayed(this, 1000)
                } else {
                    callHandler.postDelayed(this, 5000) // Super efficient while screen is off
                }
            }
        }
        callHandler.post(callCheckRunnable)
        
        val volFilter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(hardwareSyncReceiver, volFilter)
        context.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true, brightnessObserver)

        val ecoFilter = IntentFilter().apply {
            addAction("com.crdroid.batterywellbeing.SYSTEM_OVERRIDE")
            addAction("com.crdroid.batterywellbeing.SYSTEM_ALERT")
            addAction("com.crdroid.batterywellbeing.WARNING_1_MINUTE_REMAINING")
            addAction("com.crdroid.batterywellbeing.REALITY_PILL_TICK")
            addAction("com.crdroid.batterywellbeing.SYNC_CONFIG")
            addAction("com.example.dynamicisland.APP_CHANGED")
            addAction("com.example.dynamicisland.LIVE_ACTIVITY_CAUGHT")
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
                         // 🎛️ FIXED: Low Battery Alerts are now flagged as CRITICAL so they pierce through games/videos
                         if (level == 20 || level == 10 || level == 5) {
                             postTransientNotification(
                                 LiveActivityModel.Charging(id = "sys_battery_low", level = level, isPluggedIn = false, isTransient = true, isCritical = true).copy(type = ActivityType.BATTERY_LOW), 
                                 6000L
                             )
                         }
                     }
                 }
             }
             wasCharging = isCharging
             lastReportedBattery = level
             islandView?.updateBattery(level, isCharging)
        }
        BatteryPlugin.start(context)
        scope.launch { HardwareMonitors.startMonitoring().collect { hw -> currentHardware = hw; evaluatePriority() } }
        setupMediaListener()
    }

    // 🎛️ PREMIUM CALL CONTROLS
    fun toggleMicMute() {
        try { audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute } catch (e: Exception) {}
    }

    fun toggleSpeakerphone() {
        try { audioManager.isSpeakerphoneOn = !audioManager.isSpeakerphoneOn } catch (e: Exception) {}
    }

    fun openNativeCallUI() {
        try {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            tm.showInCallScreen(false) 
            islandView?.setState(IslandState.TYPE_1_MINI)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            islandView?.setState(IslandState.TYPE_1_MINI)
        }
    }

    fun endActiveCall() {
        try {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            tm.javaClass.getMethod("endCall").invoke(tm)
        } catch (e: Exception) {
            try {
                val eventDown = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_HEADSETHOOK)
                val eventUp = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_HEADSETHOOK)
                audioManager.dispatchMediaKeyEvent(eventDown)
                audioManager.dispatchMediaKeyEvent(eventUp)
            } catch (e2: Exception) {}
        }
    }
}
