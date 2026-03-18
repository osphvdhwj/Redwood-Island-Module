@file:Suppress("DEPRECATION")
package com.example.dynamicisland

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.view.WindowManager
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import android.util.LruCache

class IslandController(private val context: Context) {

    private var windowManager: WindowManager? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _islandState = MutableStateFlow(IslandState.TYPE_0_RING)
    val islandState = _islandState.asStateFlow()
    private val _activeModel = MutableStateFlow<LiveActivityModel?>(null)
    val activeModel = _activeModel.asStateFlow()
    private val _splitModel = MutableStateFlow<LiveActivityModel?>(null) 
    val splitModel = _splitModel.asStateFlow()

    private val taskQueue = mutableListOf<LiveActivityModel>()
    private var currentTaskIndex = 0

    private var currentMedia: LiveActivityModel.Music? = null
    private var islandView: DynamicIslandView? = null
    private var currentHardware: LiveActivityModel.HardwareMonitor? = null
    private var transientModel: LiveActivityModel? = null
    
    private var transientJob: Job? = null
    private var pauseFadeJob: Job? = null
    private var mediaTickerJob: Job? = null

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

    // The Failsafe Matrix loads first, so your Island works instantly on boot.
    private val gestureMatrix = mutableMapOf<String, IslandAction>().apply {
        put("TYPE_0_RING_SINGLE_TAP", IslandAction.EXPAND)
        put("TYPE_1_MINI_SINGLE_TAP", IslandAction.EXPAND)
        put("TYPE_2_MID_SINGLE_TAP", IslandAction.EXPAND)
        put("TYPE_3_MAX_SWIPE_UP", IslandAction.COLLAPSE)
        put("TYPE_2_MID_SWIPE_UP", IslandAction.COLLAPSE)
        put("TYPE_3_MAX_SWIPE_DOWN", IslandAction.OPEN_FLOATING)
        put("TYPE_2_MID_SWIPE_LEFT", IslandAction.CYCLE_TASK_REV)
        put("TYPE_2_MID_SWIPE_RIGHT", IslandAction.CYCLE_TASK_FWD)
        put("TYPE_3_MAX_SWIPE_LEFT", IslandAction.CYCLE_TASK_REV)
        put("TYPE_3_MAX_SWIPE_RIGHT", IslandAction.CYCLE_TASK_FWD)
    }

    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeMediaController: MediaController? = null

    private fun getBestMediaController(controllers: List<MediaController>?): MediaController? {
        if (!isMediaEnabled || controllers.isNullOrEmpty()) return null
        return controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: controllers.firstOrNull()
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { updateActiveMediaController(getBestMediaController(it)) }

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

    private val iconCache = object : LruCache<String, Bitmap>(10 * 1024 * 1024) { override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount }

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
                        val task = LiveActivityModel.OngoingTask(pkgName = pkg, title = title, text = text, progress = progress, progressMax = progressMax)
                        updateQueue(task)
                        postTransientNotification(task, 4000L)
                    }
                }
                "com.example.dynamicisland.OTP_CAUGHT" -> {
                    val otpCode = intent.getStringExtra("otp") ?: return
                    val pkg = intent.getStringExtra("pkg") ?: "System"
                    postTransientNotification(LiveActivityModel.Otp(code = otpCode, sourceApp = pkg), 8000L) 
                }
            }
        }
    }

    private val componentCallbacks = object : android.content.ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) { evaluatePriority() }
        override fun onLowMemory() {}
        override fun onTrimMemory(level: Int) { if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) iconCache.evictAll() }
    }

    private fun updateQueue(model: LiveActivityModel) {
        taskQueue.removeAll { it.id == model.id || it.type == model.type }
        taskQueue.add(0, model) 
        if (taskQueue.size > 3) taskQueue.removeLast() 
        currentTaskIndex = 0
    }

    private fun cycleTask(forward: Boolean) {
        if (taskQueue.isEmpty()) return
        currentTaskIndex = if (forward) {
            (currentTaskIndex + 1) % taskQueue.size
        } else {
            if (currentTaskIndex - 1 < 0) taskQueue.size - 1 else currentTaskIndex - 1
        }
        evaluatePriority()
    }

    private fun launchFloatingWindow(model: LiveActivityModel?) {
        val packageName = when (model) {
            is LiveActivityModel.Music -> model.appPackageName
            is LiveActivityModel.OngoingTask -> model.pkgName
            is LiveActivityModel.AppTimerWarning -> model.packageName
            else -> return
        }
        
        try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            if (intent != null) {
                val options = ActivityOptions.makeBasic()
                try {
                    val method = ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                    method.invoke(options, 5) 
                } catch (e: Exception) {}
                context.startActivity(intent, options.toBundle())
                _islandState.update { IslandState.HIDDEN } 
            }
        } catch (e: Exception) {}
    }

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams): android.view.View {
        val moduleContext = try { context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) } catch (e: Exception) { context }
        val view = DynamicIslandView(context, moduleContext).apply { windowManager = wm; windowParams = params }
        this.islandView = view; this.windowManager = wm 
        
        view.onSplitPillClick = { 
            if (_splitModel.value != null) {
                cycleTask(true) 
            }
        }

        view.onGestureSettingsUpdated = { payload ->
            try {
                if (payload != null && payload.length > 5) {
                    val json = JSONObject(payload)
                    gestureMatrix.clear() // 🚀 FIX: Clears the failsafe and honors your custom Config App selections!
                    json.keys().forEach { key -> try { if (key.startsWith("TYPE_")) { gestureMatrix[key] = IslandAction.valueOf(json.getString(key)) } } catch (e: Exception) {} }
                }
            } catch (e: Exception) {}
        }

        view.onGestureEvent = { gesture ->
            val actionName = gestureMatrix["${_islandState.value.name}_${gesture.name}"]?.name ?: "NONE"
            when (actionName) {
                "PLAY_PAUSE" -> { if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY") }
                "NEXT_TRACK" -> sendMediaCommand("NEXT")
                "PREV_TRACK" -> sendMediaCommand("PREV")
                "OPEN_DASHBOARD" -> { _activeModel.update { LiveActivityModel.Dashboard() }; _islandState.update { IslandState.TYPE_3_MAX } }
                
                "CYCLE_TASK_FWD" -> cycleTask(true)
                "CYCLE_TASK_REV" -> cycleTask(false)
                "OPEN_FLOATING" -> launchFloatingWindow(_activeModel.value)

                "COLLAPSE" -> {
                    if (_activeModel.value is LiveActivityModel.Dashboard) {
                        _islandState.update { IslandState.TYPE_0_RING }
                        userForceCollapsed = true
                    } else {
                        when (_islandState.value) {
                            IslandState.TYPE_3_MAX -> _islandState.update { IslandState.TYPE_2_MID }
                            IslandState.TYPE_2_MID -> {
                                if (currentMedia?.isPlaying == true) { _islandState.update { IslandState.TYPE_1_MINI } } 
                                else { userForceCollapsed = true; _islandState.update { IslandState.TYPE_0_RING } }
                            }
                            IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> { userForceCollapsed = true; _islandState.update { IslandState.TYPE_0_RING } }
                            else -> {}
                        }
                    }
                    evaluatePriority()
                }
                "EXPAND" -> {
                    userForceCollapsed = false
                    when (_islandState.value) {
                        IslandState.TYPE_0_RING -> { 
                            if (taskQueue.isNotEmpty()) _islandState.update { IslandState.TYPE_1_MINI } 
                            else { _activeModel.update { LiveActivityModel.Dashboard() }; _islandState.update { IslandState.TYPE_2_MID } } 
                        }
                        IslandState.TYPE_1_MINI -> _islandState.update { IslandState.TYPE_2_MID }
                        IslandState.TYPE_2_MID -> _islandState.update { IslandState.TYPE_3_MAX }
                        IslandState.TYPE_SPLIT -> _islandState.update { IslandState.TYPE_2_MID }
                        else -> {}
                    }
                }
            }
        }
        
        view.onAudioOutputClick = { try { PendingIntent.getBroadcast(context, 0, Intent("com.android.systemui.action.LAUNCH_SYSTEM_MEDIA_OUTPUT_DIALOG").apply { component = ComponentName("com.android.systemui", "com.android.systemui.media.dialog.MediaOutputDialogReceiver") }, PendingIntent.FLAG_IMMUTABLE).send() } catch (e: Exception) {} }
        view.onPlayPauseClick = { if (currentMedia?.isPlaying == true) sendMediaCommand("PAUSE") else sendMediaCommand("PLAY") }
        view.onPrevClick = { sendMediaCommand("PREV") }
        view.onNextClick = { sendMediaCommand("NEXT") }
        view.onSeekTo = { position -> activeMediaController?.transportControls?.seekTo(position) }

        view.onDragHandleExpand = { view.onGestureEvent?.invoke(IslandGesture.SWIPE_DOWN) }
        view.onDragHandleCollapse = { view.onGestureEvent?.invoke(IslandGesture.SWIPE_UP) }

        scope.launch { islandState.collect { state -> view.setState(state) } }
        scope.launch { activeModel.collect { model -> view.setModel(model) } }
        scope.launch { splitModel.collect { model -> view.setSplitModel(model) } }
        return view
    }

    private fun sendMediaCommand(command: String) { try { when (command) { "PLAY" -> activeMediaController?.transportControls?.play(); "PAUSE" -> activeMediaController?.transportControls?.pause(); "NEXT" -> activeMediaController?.transportControls?.skipToNext(); "PREV" -> activeMediaController?.transportControls?.skipToPrevious() } } catch (e: Exception) {} }

    private fun evaluatePriority() {
        val rotation = try { windowManager?.defaultDisplay?.rotation ?: 0 } catch(e: Throwable) { 0 }
        val isLandscapeNow = rotation == android.view.Surface.ROTATION_90 || rotation == android.view.Surface.ROTATION_270
        val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        val blacklistedGames = prefs.getString("gaming_blacklist", "com.dts.freefiremax,com.tencent.ig") ?: ""
        val isBlacklistedAppActive = topAppPackage.isNotEmpty() && blacklistedGames.contains(topAppPackage)
        
        if ((isLandscapeNow || currentHardware?.isGamingModeOn == true || isBlacklistedAppActive) && transientModel?.isCritical != true) {
            _islandState.update { IslandState.HIDDEN }; return
        }
        
        if (transientModel != null) {
            userForceCollapsed = false
            if (transientModel is LiveActivityModel.SystemAlert || transientModel is LiveActivityModel.AppTimerWarning || transientModel is LiveActivityModel.OngoingTask || transientModel is LiveActivityModel.Otp) {
                _activeModel.update { transientModel }; _splitModel.update { null }; _islandState.update { IslandState.TYPE_2_MID }
            } else if (transientModel is LiveActivityModel.RealityPill) {
                _activeModel.update { transientModel }; _splitModel.update { null }; _islandState.update { IslandState.TYPE_1_MINI }
            } else if (taskQueue.isNotEmpty()) {
                _activeModel.update { taskQueue[currentTaskIndex] }; _splitModel.update { transientModel }; _islandState.update { IslandState.TYPE_SPLIT }
            } else {
                _activeModel.update { transientModel }; _splitModel.update { null }; _islandState.update { IslandState.TYPE_CUBE }
            }
            return
        }
        
        if (_activeModel.value is LiveActivityModel.Dashboard) return
        
        if (taskQueue.isNotEmpty()) {
            val primaryTask = taskQueue[currentTaskIndex]
            _activeModel.update { primaryTask }
            
            if (userForceCollapsed) { _islandState.update { IslandState.TYPE_0_RING }; return }
            
            if (taskQueue.size > 1) {
                val secondaryIndex = (currentTaskIndex + 1) % taskQueue.size
                _splitModel.update { taskQueue[secondaryIndex] }
                if (_islandState.value == IslandState.HIDDEN || _islandState.value == IslandState.TYPE_0_RING) {
                    _islandState.update { IslandState.TYPE_SPLIT }
                }
            } else {
                _splitModel.update { null }
                if (_islandState.value == IslandState.HIDDEN || _islandState.value == IslandState.TYPE_0_RING) {
                    _islandState.update { IslandState.TYPE_1_MINI }
                }
            }
            return
        }
        
        _activeModel.update { null }; _splitModel.update { null }
        _islandState.update { IslandState.TYPE_0_RING }
    }

    fun postTransientNotification(model: LiveActivityModel, durationMs: Long = 5000L) {
        transientJob?.cancel(); transientModel = model; evaluatePriority()
        transientJob = scope.launch { delay(durationMs); transientModel = null; evaluatePriority() }
    }

    private fun updateActiveMediaController(controller: MediaController?) {
        activeMediaController?.unregisterCallback(mediaCallback); activeMediaController = controller
        if (controller == null || !isMediaEnabled) { 
            currentMedia = null
            taskQueue.removeAll { it is LiveActivityModel.Music }
            stopMediaTicker(); evaluatePriority(); return 
        }
        controller.registerCallback(mediaCallback); extractMediaData(controller)
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) { extractMediaData(activeMediaController) }
        override fun onMetadataChanged(metadata: MediaMetadata?) { extractMediaData(activeMediaController) }
    }

    private fun extractMediaData(controller: MediaController?) {
        if (controller == null || !isMediaEnabled) return
        val metadata = controller.metadata
        val pbState = controller.playbackState ?: return

        val isPlaying = pbState.state == PlaybackState.STATE_PLAYING
        if (!isPlaying && currentMedia == null) return 

        val newTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        if (newTitle != lastTrackTitle && lastTrackTitle.isNotEmpty() && userForceCollapsed && isPlaying && !isPeeking) {
            isPeeking = true; userForceCollapsed = false
            scope.launch(Dispatchers.Main) { delay(3000); userForceCollapsed = true; isPeeking = false; evaluatePriority() }
        }
        lastTrackTitle = newTitle

        scope.launch(Dispatchers.IO) {
            val rawAlbumArt = try { metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART) } catch (e: Exception) { null }
            val albumArtBitmap = if (rawAlbumArt == null) null else { val r = Math.min(400f / rawAlbumArt.width, 400f / rawAlbumArt.height); if (r >= 1.0f) rawAlbumArt else Bitmap.createScaledBitmap(rawAlbumArt, (rawAlbumArt.width * r).toInt(), (rawAlbumArt.height * r).toInt(), true) }
            
            var blurredArtBitmap: Bitmap? = null
            var bgColor: Int? = null; var txtColor: Int = android.graphics.Color.WHITE
            
            if (albumArtBitmap != null) {
                @Suppress("DEPRECATION")
                var rs: android.renderscript.RenderScript? = null
                var input: android.renderscript.Allocation? = null
                var output: android.renderscript.Allocation? = null
                var script: android.renderscript.ScriptIntrinsicBlur? = null
                try {
                    rs = android.renderscript.RenderScript.create(context)
                    input = android.renderscript.Allocation.createFromBitmap(rs, albumArtBitmap)
                    output = android.renderscript.Allocation.createTyped(rs, input.type)
                    script = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs))
                    script.setRadius(24f); script.setInput(input); script.forEach(output)
                    blurredArtBitmap = Bitmap.createBitmap(albumArtBitmap.width, albumArtBitmap.height, albumArtBitmap.config ?: Bitmap.Config.ARGB_8888)
                    output.copyTo(blurredArtBitmap)
                } catch (e: Exception) { blurredArtBitmap = albumArtBitmap 
                } finally { input?.destroy(); output?.destroy(); script?.destroy(); rs?.destroy() }

                val palette = Palette.from(albumArtBitmap).generate()
                (palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch)?.let { swatch ->
                    val hsl = FloatArray(3)
                    androidx.core.graphics.ColorUtils.colorToHSL(swatch.rgb, hsl)
                    if (hsl[2] < 0.35f) hsl[2] = 0.35f 
                    bgColor = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
                    txtColor = if (androidx.core.graphics.ColorUtils.calculateLuminance(bgColor!!) > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            }

            withContext(Dispatchers.Main) {
                currentMedia?.blurredAlbumArt?.takeIf { it != currentMedia?.albumArt }?.recycle()
                val wasPlaying = currentMedia?.isPlaying == true
                
                currentMedia = LiveActivityModel.Music(
                    id = "media_main", title = newTitle, artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
                    albumArt = albumArtBitmap, blurredAlbumArt = blurredArtBitmap, appIcon = null, dominantColor = bgColor, titleTextColor = txtColor,
                    isPlaying = isPlaying, durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L, positionMs = pbState.position,
                    appPackageName = controller.packageName, customActions = emptyList()
                )

                if (currentMedia != null) updateQueue(currentMedia!!)

                if (isPlaying && !wasPlaying) { userForceCollapsed = false; pauseFadeJob?.cancel(); startMediaTicker() }
                else if (!isPlaying) { stopMediaTicker(); if (wasPlaying) { pauseFadeJob?.cancel(); pauseFadeJob = scope.launch { delay(3000); userForceCollapsed = true; evaluatePriority() } } }
                evaluatePriority()
            }
        }
    }

    private fun startMediaTicker() {
        if (!isScreenOn || !isMediaEnabled) return 
        mediaTickerJob?.cancel()
        mediaTickerJob = scope.launch { while (isActive) { activeMediaController?.playbackState?.position?.let { pos -> (activeModel.value as? LiveActivityModel.Music)?.let { islandView?.updateTicker(pos) } }; delay(1000) } }
    }
    private fun stopMediaTicker() { mediaTickerJob?.cancel() }
    
    init {
        context.registerReceiver(screenStateReceiver, IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) })
        context.registerComponentCallbacks(componentCallbacks)
        
        val ecoFilter = IntentFilter().apply { listOf("com.example.dynamicisland.APP_CHANGED", "com.example.dynamicisland.LIVE_ACTIVITY_CAUGHT", "com.example.dynamicisland.OTP_CAUGHT").forEach { addAction(it) } }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) context.registerReceiver(ecosystemReceiver, ecoFilter, "com.redwood.permission.SECURE_IPC", null, Context.RECEIVER_EXPORTED) else @Suppress("UnspecifiedRegisterReceiverFlag") context.registerReceiver(ecosystemReceiver, ecoFilter, "com.redwood.permission.SECURE_IPC", null)

        BatteryPlugin.onBatteryChanged = { level, isCharging, _ ->
             if (isChargingEnabled) {
                 if (isCharging && !wasCharging) postTransientNotification(LiveActivityModel.Charging(id = "sys_battery", level = level, isPluggedIn = true, isTransient = true), 4000L)
                 else if (!isCharging && lastReportedBattery != -1 && level < lastReportedBattery && level in listOf(20, 10, 5)) postTransientNotification(LiveActivityModel.Charging(id = "sys_battery_low", level = level, isPluggedIn = false, isTransient = true).copy(type = ActivityType.BATTERY_LOW), 6000L)
             }
             wasCharging = isCharging; lastReportedBattery = level; islandView?.updateBattery(level, isCharging)
        }
        BatteryPlugin.start(context)
        scope.launch { HardwareMonitors.startMonitoring().collect { hw -> currentHardware = hw; evaluatePriority() } }
        try { mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, null); updateActiveMediaController(getBestMediaController(mediaSessionManager.getActiveSessions(null))) } catch (e: Exception) {}
    }
}
