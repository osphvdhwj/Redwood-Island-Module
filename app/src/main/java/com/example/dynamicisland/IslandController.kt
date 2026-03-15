@file:Suppress("DEPRECATION")
package com.example.dynamicisland

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
import java.util.concurrent.ConcurrentHashMap

class IslandController(private val context: Context) {

    private var windowManager: WindowManager? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Concurrency-safe StateFlows
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

    private val gestureMatrix = mutableMapOf<String, IslandAction>()
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
                    if (progress != -1 && progressMax > 0) postTransientNotification(LiveActivityModel.OngoingTask(pkgName = pkg, title = title, text = text, progress = progress, progressMax = progressMax), 4000L)
                }
                "com.crdroid.batterywellbeing.SYSTEM_OVERRIDE" -> {
                    if (!isAlertsEnabled) return
                    when (intent.getStringExtra("action")) {
                        "SMART_CHARGE_LIMIT" -> postTransientNotification(LiveActivityModel.Charging("sys_smart_charge", ActivityType.CHARGING, intent.getIntExtra("level", 100), true, true), 6000L)
                        "THERMAL_WARNING" -> postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_thermal", alertType = "THERMAL", title = "Thermal Throttling", message = "Battery temperature at ${intent.getStringExtra("extra_info") ?: "High"}", alertColor = android.graphics.Color.RED), 6000L)
                        "ROGUE_APP_DETECTED" -> postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_rogue", alertType = "ROGUE", title = "High Background Drain", message = "${intent.getStringExtra("extra_info") ?: "Unknown App"} is draining battery", alertColor = android.graphics.Color.rgb(255, 165, 0)), 6000L)
                    }
                }
                "com.crdroid.batterywellbeing.SYSTEM_ALERT" -> {
                    if (!isAlertsEnabled) return
                    val colorInt = try { android.graphics.Color.parseColor(intent.getStringExtra("colorHex") ?: "#FFFFFF") } catch(e: Exception) { android.graphics.Color.WHITE }
                    postTransientNotification(LiveActivityModel.SystemAlert(id = "sys_alert", alertType = intent.getStringExtra("alertType") ?: "INFO", title = intent.getStringExtra("title") ?: "Alert", message = intent.getStringExtra("message") ?: "", alertColor = colorInt), 5000L)
                }
            }
        }
    }

    private val componentCallbacks = object : android.content.ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) { evaluatePriority() }
        override fun onLowMemory() {}
        override fun onTrimMemory(level: Int) { if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) iconCache.evictAll() }
    }

    fun createIslandView(wm: WindowManager, params: WindowManager.LayoutParams): android.view.View {
        val moduleContext = try { context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) } catch (e: Exception) { context }
        val view = DynamicIslandView(context, moduleContext).apply {
            windowManager = wm
            windowParams = params
        }
        
        this.islandView = view 
        this.windowManager = wm 
        
        view.onSplitPillClick = { 
            if (_splitModel.value is LiveActivityModel.Charging) { 
                try { context.startActivity(Intent().setComponent(ComponentName("com.crdroid.batterywellbeing", "com.crdroid.batterywellbeing.MainActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) } catch(e: Throwable) { _islandState.update { IslandState.TYPE_CUBE } }
            } 
        }

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
                "COLLAPSE" -> {
                    if (_activeModel.value is LiveActivityModel.Dashboard && currentMedia != null) {
                        userForceCollapsed = currentMedia?.isPlaying == false
                        _islandState.update { if (currentMedia?.isPlaying == true) IslandState.TYPE_1_MINI else IslandState.TYPE_0_RING }
                        _activeModel.update { currentMedia }
                    } else {
                        userForceCollapsed = true
                        _islandState.update { IslandState.TYPE_0_RING }
                    }
                    evaluatePriority()
                }
                "EXPAND" -> {
                    userForceCollapsed = false
                    when (_islandState.value) {
                        IslandState.TYPE_0_RING -> { if (currentMedia != null) _islandState.update { IslandState.TYPE_1_MINI } else { _activeModel.update { LiveActivityModel.Dashboard() }; _islandState.update { IslandState.TYPE_3_MAX } } }
                        IslandState.TYPE_1_MINI -> { if (currentMedia?.isPlaying == false) { _activeModel.update { LiveActivityModel.Dashboard() }; _islandState.update { IslandState.TYPE_3_MAX } } else _islandState.update { IslandState.TYPE_2_MID } }
                        IslandState.TYPE_2_MID, IslandState.TYPE_SPLIT -> _islandState.update { IslandState.TYPE_3_MAX }
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
            _islandState.update { IslandState.HIDDEN }
            return
        }
        
        if (transientModel != null) {
            userForceCollapsed = false
            if (currentHardware?.isGamingModeOn == true && transientModel is LiveActivityModel.Charging) { } 
            else if (transientModel is LiveActivityModel.SystemAlert || transientModel is LiveActivityModel.AppTimerWarning || transientModel is LiveActivityModel.OngoingTask) {
                _activeModel.update { transientModel }; _splitModel.update { null }; _islandState.update { IslandState.TYPE_2_MID }
            } else if (transientModel is LiveActivityModel.RealityPill) {
                _activeModel.update { transientModel }; _splitModel.update { null }; _islandState.update { IslandState.TYPE_1_MINI }
            } else if (currentMedia?.isPlaying == true || currentMedia != null) {
                _activeModel.update { currentMedia }; _splitModel.update { transientModel }; _islandState.update { IslandState.TYPE_SPLIT }
            } else {
                _activeModel.update { transientModel }; _splitModel.update { null }; _islandState.update { IslandState.TYPE_CUBE }
            }
            return
        }
        
        _splitModel.update { null }
        if (_activeModel.value is LiveActivityModel.Dashboard) return
        
        if (currentMedia != null && isMediaEnabled) {
            _activeModel.update { currentMedia }
            if (userForceCollapsed) { _islandState.update { IslandState.TYPE_0_RING }; return }
            if (_islandState.value in listOf(IslandState.HIDDEN, IslandState.TYPE_0_RING, IslandState.TYPE_CUBE, IslandState.TYPE_SPLIT)) _islandState.update { IslandState.TYPE_1_MINI }
            return
        }
        
        _activeModel.update { null }
        _islandState.update { IslandState.TYPE_0_RING }
    }

    fun postTransientNotification(model: LiveActivityModel, durationMs: Long = 5000L) {
        transientJob?.cancel(); transientModel = model; evaluatePriority()
        transientJob = scope.launch { delay(durationMs); transientModel = null; evaluatePriority() }
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
                } catch (e: Exception) { 
                    blurredArtBitmap = albumArtBitmap 
                } finally { 
                    // 🚀 The Memory Leak Fix!
                    input?.destroy(); output?.destroy(); script?.destroy(); rs?.destroy() 
                }

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
                    appPackageName = controller.packageName, customActions = pbState.customActions.map { CustomMediaAction(it.action, null, null, true) }
                )

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
        
        val ecoFilter = IntentFilter().apply { listOf("com.crdroid.batterywellbeing.SYSTEM_OVERRIDE", "com.crdroid.batterywellbeing.SYSTEM_ALERT", "com.example.dynamicisland.APP_CHANGED", "com.example.dynamicisland.LIVE_ACTIVITY_CAUGHT").forEach { addAction(it) } }
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
