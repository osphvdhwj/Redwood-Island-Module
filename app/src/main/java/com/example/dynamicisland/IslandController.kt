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
import android.util.LruCache
import java.util.concurrent.ConcurrentHashMap

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
    private var islandView: DynamicIslandView? = null
    private var currentHardware: LiveActivityModel.HardwareMonitor? = null
    private var transientModel: LiveActivityModel? = null
    private var transientJob: Job? = null
    private var pauseFadeJob: Job? = null
    private var userForceCollapsed = false 
    private var lastReportedBattery = -1
    private var wasCharging = false // 🚀 FIX: Track previous charging state
    private var isScreenOn = true 
    private var isLandscape = false

    private val gestureMatrix = mutableMapOf<String, IslandAction>()

    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeMediaController: MediaController? = null
    private var mediaTickerJob: Job? = null

    private fun getBestMediaController(controllers: List<MediaController>?): MediaController? {
        if (controllers.isNullOrEmpty()) return null
        // 🚀 LOGIC FIX: Prioritize playing media over paused background apps
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
                    // 🚀 HARDWARE FIX: Prevent OLED Burn-In by nuking the UI on AOD
                    _islandState.value = IslandState.HIDDEN
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    if (currentMedia?.isPlaying == true) startMediaTicker()
                    // 🚀 Restore the correct state when the screen wakes up
                    evaluatePriority()
                }
            }
        }
    }

    // 🚀 NEW: THE ECOSYSTEM BRIDGE (Listens to your Battery Manager module)
    // 🚀 CONCURRENCY FIX: Thread-safe collections
    private val activeAppTimers = ConcurrentHashMap<String, Long>()
    private val exemptedApps = ConcurrentHashMap.newKeySet<String>()

    // 🚀 MEMORY FIX: 10MB LruCache for App Icons to prevent OOM fragmentation
    private val iconCache = object : LruCache<String, Bitmap>(10 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

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

                    scope.launch(Dispatchers.IO) {
                        val pm = context.packageManager
                        var appName = providedAppName ?: pkg

                        // 🚀 MEMORY FIX: Check the cache first!
                        var appIcon: Bitmap? = iconCache.get(pkg)

                        if (appIcon == null) {
                            try {
                                val appInfo = pm.getApplicationInfo(pkg, 0)
                                if (providedAppName == null) appName = pm.getApplicationLabel(appInfo).toString()

                                val drawable = pm.getApplicationIcon(appInfo)
                                val bmp = Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bmp)
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(canvas)
                                appIcon = getScaledBitmap(bmp, 150)

                                // 🚀 MEMORY FIX: Destroy the raw bitmap immediately after scaling!
                                if (bmp != appIcon) {
                                    bmp.recycle()
                                }

                                // Save to cache for next time
                                appIcon?.let { iconCache.put(pkg, it) }
                            } catch (e: Exception) {}
                        }

                        val warningModel = LiveActivityModel.AppTimerWarning(
                            packageName = pkg, appName = appName,
                            appIcon = appIcon, targetTimeMs = System.currentTimeMillis() + 60000L
                        )

                        withContext(Dispatchers.Main) {
                            postTransientNotification(warningModel, 60000L)
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
                            // 🚀 SECURITY FIX: JSON Bomb Protection
                            if (timersJson.length > 5000) return@launch

                            val json = org.json.JSONObject(timersJson)
                            activeAppTimers.clear()
                            json.keys().forEach { activeAppTimers[it] = json.getLong(it) }

                            val exemptionsCsv = intent.getStringExtra("exemptions_csv") ?: ""
                            if (exemptionsCsv.length > 2000) return@launch // Length check

                            exemptedApps.clear()
                            exemptionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { exemptedApps.add(it) }
                        } catch (e: Exception) {}
                    }
                }
            }
        }
    }

    private val componentCallbacks = object : android.content.ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
            isLandscape = newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            evaluatePriority()
        }
        @Deprecated("Deprecated in Java") // 🚀 FIX: Suppress Android's deprecated interface requirement
        override fun onLowMemory() {}
    }

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams): android.view.View {
        val moduleContext = try { context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) } catch (e: Exception) { context }
        val view = DynamicIslandView(context, moduleContext)
        this.islandView = view // 🚀 FIX: Store the reference
        view.onSplitPillClick = {
            // If the split model is the Battery Manager's Reality Pill or Charging, do nothing.
            // If it's a second app, launch it here!
            val sModel = _splitModel.value
            if (sModel is LiveActivityModel.Charging) {
                 _islandState.value = IslandState.TYPE_CUBE
            }
        }
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

            // 🚀 BUG 1 FIX: Read from RAM, not the blocked file!
            // This instantly restores your Swipe to Expand (M Pill) and all other gestures.
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
                    // 🚀 FIX: Smoothly collapse, allowing Media/Split to hide into the Ring
                    when (_islandState.value) {
                        IslandState.TYPE_3_MAX -> _islandState.value = IslandState.TYPE_2_MID
                        IslandState.TYPE_2_MID -> _islandState.value = IslandState.TYPE_1_MINI
                        IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> {
                            userForceCollapsed = true // Tells the system NOT to auto-expand media
                            _islandState.value = IslandState.TYPE_0_RING
                        }
                        else -> {}
                    }
                }
                "EXPAND" -> {
                    userForceCollapsed = false
                    // 🚀 FIX: Smoothly expand step-by-step
                    when (_islandState.value) {
                        IslandState.TYPE_0_RING -> { 
                            if (_activeModel.value == null) _activeModel.value = LiveActivityModel.Dashboard()
                            _islandState.value = IslandState.TYPE_1_MINI 
                        }
                        IslandState.TYPE_1_MINI -> _islandState.value = IslandState.TYPE_2_MID
                        IslandState.TYPE_2_MID -> _islandState.value = IslandState.TYPE_3_MAX
                        IslandState.TYPE_SPLIT -> _islandState.value = IslandState.TYPE_3_MAX
                        else -> {}
                    }
                }
                "OPEN_APP" -> {
                    val model = _activeModel.value
                    if (model is LiveActivityModel.Music && model.appPackageName.isNotEmpty()) {
                        try {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(model.appPackageName)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                                val options = android.app.ActivityOptions.makeBasic()
                                if (android.os.Build.VERSION.SDK_INT >= 34) { 
                                    options.pendingIntentBackgroundActivityStartMode = android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                }
                                pendingIntent.send(context, 0, null, null, null, null, options.toBundle())
                            }
                        } catch (e: Exception) {}
                    }
                }
                "HEART_SONG" -> {
                    val heartAction = currentMedia?.customActions?.find { it.actionName.contains("heart", true) || it.actionName.contains("favorite", true) || it.actionName.contains("like", true) }
                    if (heartAction != null) activeMediaController?.transportControls?.sendCustomAction(heartAction.actionName, null)
                    else activeMediaController?.transportControls?.setRating(android.media.Rating.newHeartRating(true))
                }
                "TOGGLE_TORCH" -> {
                    try {
                        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                        val cameraId = cameraManager.cameraIdList.firstOrNull()
                        // Add toggle logic here if needed
                    } catch(e: Exception) {}
                }
                "NONE" -> {
                    // 🚀 FIX: Default gestures now use the smooth cascade ladder!
                    if (gesture == IslandGesture.SWIPE_UP || gesture == IslandGesture.SWIPE_LEFT || gesture == IslandGesture.SWIPE_RIGHT) {
                        if (_islandState.value == IslandState.TYPE_3_MAX) _islandState.value = IslandState.TYPE_2_MID
                        else if (_islandState.value == IslandState.TYPE_2_MID) _islandState.value = IslandState.TYPE_1_MINI
                        else if (_islandState.value == IslandState.TYPE_1_MINI || _islandState.value == IslandState.TYPE_SPLIT) {
                            userForceCollapsed = true
                            _islandState.value = IslandState.TYPE_0_RING
                        }
                    }
                    if (gesture == IslandGesture.SWIPE_DOWN) {
                        if (_islandState.value == IslandState.TYPE_0_RING) {
                            if (_activeModel.value == null) _activeModel.value = LiveActivityModel.Dashboard()
                            _islandState.value = IslandState.TYPE_1_MINI
                        } else if (_islandState.value == IslandState.TYPE_1_MINI) {
                            _islandState.value = IslandState.TYPE_2_MID
                        } else if (_islandState.value == IslandState.TYPE_2_MID || _islandState.value == IslandState.TYPE_SPLIT) {
                            _islandState.value = IslandState.TYPE_3_MAX
                        } else if (_islandState.value == IslandState.TYPE_3_MAX) {
                            try {
                                @android.annotation.SuppressLint("WrongConstant")
                                val sbs = context.getSystemService("statusbar")
                                val expandMethod = sbs?.javaClass?.getMethod("expandNotificationsPanel")
                                expandMethod?.invoke(sbs)
                            } catch (e: Exception) {}
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

    // 🚀 RESTORED: The missing media command helper!
    private fun sendMediaCommand(command: String) {
        val controls = activeMediaController?.transportControls ?: return
        try {
            when (command) {
                "PLAY" -> controls.play()
                "PAUSE" -> controls.pause()
                "NEXT" -> controls.skipToNext()
                "PREV" -> controls.skipToPrevious()
            }
        } catch (e: android.os.DeadObjectException) {
            // Target app died, clear the Island!
            currentMedia = null
            evaluatePriority()
        } catch (e: Exception) {}
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
        if (isLandscape) {
            val isAlertCritical = transientModel?.isCritical == true
            if (!isAlertCritical) {
                _islandState.value = IslandState.HIDDEN
                return
            }
        }
        
        if (transientModel != null) {
            userForceCollapsed = false // 🚀 FIX: Let popups auto-expand even if user previously forced collapse!
            if (currentHardware?.isGamingModeOn == true && transientModel is LiveActivityModel.Charging) {
                // Ignore the popup, leave Island hidden or in mini mode
            } else if (transientModel is LiveActivityModel.SystemAlert || transientModel is LiveActivityModel.AppTimerWarning) {
                _activeModel.value = transientModel
                _splitModel.value = null
                _islandState.value = IslandState.TYPE_2_MID
            } else if (transientModel is LiveActivityModel.RealityPill) {
                _activeModel.value = transientModel
                _splitModel.value = null
                _islandState.value = IslandState.TYPE_1_MINI
            } else if (currentMedia?.isPlaying == true || currentMedia != null) {
                _activeModel.value = currentMedia
                _splitModel.value = transientModel
                _islandState.value = IslandState.TYPE_SPLIT
            } else {
                _activeModel.value = transientModel
                _splitModel.value = null
                _islandState.value = IslandState.TYPE_CUBE
            }
            return
        }
        
        _splitModel.value = null
        
        // 🚀 FIX: Prevent the system from deleting the Dashboard when you open it from idle
        if (_activeModel.value is LiveActivityModel.Dashboard) return
        
        if (currentMedia != null) {
            _activeModel.value = currentMedia
            // 🚀 FIX: The !userForceCollapsed check prevents media from bouncing back to MINI if you just swiped up!
            if (!userForceCollapsed && (_islandState.value == IslandState.HIDDEN || _islandState.value == IslandState.TYPE_0_RING || _islandState.value == IslandState.TYPE_CUBE || _islandState.value == IslandState.TYPE_SPLIT)) {
                _islandState.value = IslandState.TYPE_1_MINI
            }
            return
        }
        
        if (currentHardware?.isGamingModeOn == true) { 
            _activeModel.value = currentHardware; _islandState.value = IslandState.TYPE_1_MINI; return 
        }
        
        _activeModel.value = null
        _islandState.value = IslandState.TYPE_0_RING
    }
    private fun setupMediaListener() {
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, ComponentName(context, "com.example.dynamicisland.DummyListener"))
            updateActiveMediaController(getBestMediaController(mediaSessionManager.getActiveSessions(ComponentName(context, "com.example.dynamicisland.DummyListener"))))
        } catch (e: Exception) {}
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
        // 🚀 FIX: Recycle the massive original bitmap so SystemUI doesn't run out of memory!
        if (rawAlbumArt != null && rawAlbumArt != albumArtBitmap) { rawAlbumArt.recycle() }
        //fix here, low RAM fix
    
        var appIconBitmap: Bitmap? = null
        try { 
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(controller.packageName)
            val bmp = Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            appIconBitmap = getScaledBitmap(bmp, 150)

            // 🚀 MEMORY FIX: Destroy the raw bitmap immediately after scaling!
            if (bmp != appIconBitmap) {
                bmp.recycle()
            }
        } catch (e: Exception) {}

        val extractedActions = pbState.customActions.map { CustomMediaAction(actionName = it.action, icon = null, pendingIntent = null, isEnabled = true) }

        // Push heavy blur/color math to the background
        scope.launch(Dispatchers.IO) {
            var blurredArtBitmap: Bitmap? = null
            var bgColor: Int? = null; var txtColor: Int = android.graphics.Color.WHITE

            if (albumArtBitmap != null) {
                // 🚀 CHOREOGRAPHER FIX: Pre-blur the background on the IO Thread!
                @Suppress("DEPRECATION")
                try {
                    val rs = android.renderscript.RenderScript.create(context)
                    val input = android.renderscript.Allocation.createFromBitmap(rs, albumArtBitmap)
                    val output = android.renderscript.Allocation.createTyped(rs, input.type)
                    val script = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs))
                    script.setRadius(24f) // Maximum blur radius
                    script.setInput(input)
                    script.forEach(output)
                    blurredArtBitmap = Bitmap.createBitmap(albumArtBitmap.width, albumArtBitmap.height, albumArtBitmap.config ?: Bitmap.Config.ARGB_8888)
                    output.copyTo(blurredArtBitmap)
                    rs.destroy()
                } catch (e: Exception) {
                    blurredArtBitmap = albumArtBitmap // Fallback if RenderScript fails
                }

                // 🚀 CONTRAST FIX: Calculate mathematical luminance
                val palette = Palette.from(albumArtBitmap).generate()
                val swatch = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch
                if (swatch != null) {
                    bgColor = swatch.rgb
                    val luminance = androidx.core.graphics.ColorUtils.calculateLuminance(bgColor)
                    // If the background is too bright (>0.5 luminance), flip text to Black
                    txtColor = if (luminance > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            }

            // Return to Main Thread to update the UI state
            withContext(Dispatchers.Main) {
                currentMedia?.blurredAlbumArt?.takeIf { it != currentMedia?.albumArt }?.recycle()

                currentMedia = LiveActivityModel.Music(
                    id = "media_main", title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
                    artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
                    albumArt = albumArtBitmap,
                    blurredAlbumArt = blurredArtBitmap,
                    appIcon = appIconBitmap, dominantColor = bgColor, titleTextColor = txtColor,
                    isPlaying = isPlaying, durationMs = duration, positionMs = pbState.position,
                    appPackageName = controller.packageName, customActions = extractedActions
                )

                if (isPlaying && !wasPlaying) { userForceCollapsed = false; pauseFadeJob?.cancel() }
                if (isPlaying) { startMediaTicker() } else {
                    stopMediaTicker()
                    if (wasPlaying) {
                        pauseFadeJob?.cancel()
                        pauseFadeJob = scope.launch { 
                            delay(3000)
                            // 🚀 FIX: Don't delete the media! Just force it to collapse into the Ring.
                            // Now you can still swipe down to unpause it later!
                            userForceCollapsed = true 
                            evaluatePriority() 
                        }
                    }
                }
                evaluatePriority()
            }
        }
    }

    private fun startMediaTicker() {
        if (!isScreenOn) return 
        mediaTickerJob?.cancel()
        mediaTickerJob = scope.launch { 
            while (isActive) { 
                activeMediaController?.playbackState?.position?.let { pos -> 
                    (activeModel.value as? LiveActivityModel.Music)?.let { 
                        // 🚀 FIX: Direct memory update! Zero IPC overhead!
                        islandView?.updateTicker(pos)
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
        // 🚀 SECURITY FIX: Register receiver with strict Signature Permission
        val ecoFilter = IntentFilter().apply {
            addAction("com.crdroid.batterywellbeing.SYSTEM_OVERRIDE")
            addAction("com.crdroid.batterywellbeing.SYSTEM_ALERT")
            addAction("com.crdroid.batterywellbeing.WARNING_1_MINUTE_REMAINING")
            addAction("com.crdroid.batterywellbeing.REALITY_PILL_TICK")
            addAction("com.crdroid.batterywellbeing.SYNC_CONFIG")
        }

        val securePermission = "com.redwood.permission.SECURE_IPC"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(ecosystemReceiver, ecoFilter, securePermission, null, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(ecosystemReceiver, ecoFilter, securePermission, null)
        }

        BatteryPlugin.onBatteryChanged = { level, isCharging, _ ->
             // 🚀 SPAM FIX: Only show Cube if it JUST plugged in!
             if (isCharging && !wasCharging) {
                 postTransientNotification(LiveActivityModel.Charging(id = "sys_battery", level = level, isPluggedIn = true, isTransient = true), 4000L)
             } else if (!isCharging) {
                 if (lastReportedBattery != -1 && level < lastReportedBattery) {
                     if (level == 20 || level == 10 || level == 5) postTransientNotification(LiveActivityModel.Charging(id = "sys_battery_low", level = level, isPluggedIn = false, isTransient = true).copy(type = ActivityType.BATTERY_LOW), 6000L)
                 }
             }

             wasCharging = isCharging
             lastReportedBattery = level
             // Direct memory update for the R ring
             islandView?.updateBattery(level, isCharging)
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
