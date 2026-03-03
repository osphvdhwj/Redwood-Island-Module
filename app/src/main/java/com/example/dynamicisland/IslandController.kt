package com.example.dynamicisland

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers

object IslandController {
    private var islandViewRef: WeakReference<DynamicIslandView>? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var currentController: MediaController? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaDuration = 0L

    private val activeActivities = ConcurrentHashMap<String, LiveActivityModel>()
    private val dismissalRunnables = ConcurrentHashMap<String, Runnable>()
    private val dismissedActivities = mutableSetOf<String>()
    private var isScreenOn = true
    private var isUserExpanded = false
    private var resolveRunnable: Runnable? = null

    private var btReceiver: android.content.BroadcastReceiver? = null
    private var wifiReceiver: android.content.BroadcastReceiver? = null



    fun cleanup(context: Context) {
        btReceiver?.let {
            try { context.unregisterReceiver(it) } catch (e: Exception) {}
            btReceiver = null
        }
        wifiReceiver?.let {
            try { context.unregisterReceiver(it) } catch (e: Exception) {}
            wifiReceiver = null
        }
        BatteryPlugin.stop(context)
    }
    private fun log(msg: String) { XposedBridge.log("DynamicIsland: $msg") }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) { updateMediaState(state) }
        override fun onMetadataChanged(metadata: MediaMetadata?) { updateMetadata(metadata) }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { updateActiveController(it) }

    private val progressUpdater = object : Runnable {
        override fun run() {
            if (!isScreenOn) return
            val controller = currentController ?: return
            val state = controller.playbackState
            if (state != null && (state.state == PlaybackState.STATE_PLAYING || state.state == PlaybackState.STATE_BUFFERING)) {
                // FIXED: Android 50-year offset bug. Must use elapsedRealtime!
                val timeDelta = android.os.SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
                val position = state.position + (timeDelta * state.playbackSpeed).toLong()
                islandViewRef?.get()?.updateMusicProgress(position, mediaDuration)
                mainHandler.postDelayed(this, 1000)
            }
        }
    }

    fun init(view: DynamicIslandView) {
        islandViewRef = WeakReference(view)
        setupMediaListener(view.context)
        setupSystemReceivers(view.context)

        // Bluetooth Connection Listener
        val btFilter = android.content.IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        btReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = intent.getIntExtra(android.bluetooth.BluetoothAdapter.EXTRA_CONNECTION_STATE, -1)
                if (state == android.bluetooth.BluetoothAdapter.STATE_CONNECTED) {
                    val device = intent.getParcelableExtra<android.bluetooth.BluetoothDevice>(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
                    postActivity(LiveActivityModel(
                        id = "sys_bt", type = ActivityType.BLUETOOTH, title = "Connected",
                        dataText = device?.name ?: "Bluetooth Device", accentColor = android.graphics.Color.BLUE, isTransient = true
                    ))
                }
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            view.context.registerReceiver(btReceiver, btFilter, Context.RECEIVER_EXPORTED)
        } else {
            view.context.registerReceiver(btReceiver, btFilter)
        }

        // Wi-Fi Connection Listener
        val wifiFilter = android.content.IntentFilter(android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION)
        wifiReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val info = intent.getParcelableExtra<android.net.NetworkInfo>(android.net.wifi.WifiManager.EXTRA_NETWORK_INFO)
                if (info != null && info.isConnected) {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                    val ssid = wifiManager.connectionInfo.ssid.removeSurrounding(""")
                    postActivity(LiveActivityModel(
                        id = "sys_wifi", type = ActivityType.WIFI, title = "Wi-Fi Connected",
                        dataText = ssid, accentColor = android.graphics.Color.CYAN, isTransient = true
                    ))
                }
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            view.context.registerReceiver(wifiReceiver, wifiFilter, Context.RECEIVER_EXPORTED)
        } else {
            view.context.registerReceiver(wifiReceiver, wifiFilter)
        }

        BatteryPlugin.onBatteryChanged = { level, isCharging, color ->
             if (isCharging) {
                 dismissedActivities.remove("sys_battery")
                 postActivity(LiveActivityModel(
                     id = "sys_battery", type = ActivityType.CHARGING,
                     title = "Charging", dataText = "$level%", progress = level / 100f, accentColor = color, isTransient = true
                 ))
             } else {
                 // Trigger a 3-second pop-up when unplugged
                 postActivity(LiveActivityModel(
                     id = "sys_battery_disconnect", type = ActivityType.BATTERY_LOW,
                     title = "Disconnected", dataText = "Battery at $level%", progress = level / 100f, accentColor = color, isTransient = true
                 ))
             }
        }
        BatteryPlugin.start(view.context)

        // CENTRALIZED GESTURE MACHINE WITH INTERACTION LOCKS
        view.onSingleTap = {
            val island = islandViewRef?.get()
            if (island != null) {
                when (island.islandState.value) {
                    DynamicIslandView.IslandState.TYPE_1_MINI -> {
                        isUserExpanded = true
                        island.setState(DynamicIslandView.IslandState.TYPE_2_MID)
                    }
                    DynamicIslandView.IslandState.TYPE_2_MID -> {
                        isUserExpanded = true
                        island.setState(DynamicIslandView.IslandState.TYPE_3_MAX)
                    }
                    DynamicIslandView.IslandState.TYPE_SPLIT -> {
                        isUserExpanded = true
                        island.setState(DynamicIslandView.IslandState.TYPE_2_MID)
                    }
                    DynamicIslandView.IslandState.TYPE_3_MAX -> {
                        isUserExpanded = false // User manually closed it, release the lock
                        island.setState(DynamicIslandView.IslandState.TYPE_1_MINI)
                        resolveHighestPriorityDebounced()
                    }
                    else -> {}
                }
            }
        }

        view.onSwipeDown = {
            val island = islandViewRef?.get()
            if (island != null) {
                isUserExpanded = true // Swiping down implies manual expansion
                when (island.islandState.value) {
                    DynamicIslandView.IslandState.TYPE_1_MINI -> island.setState(DynamicIslandView.IslandState.TYPE_2_MID)
                    DynamicIslandView.IslandState.TYPE_2_MID -> island.setState(DynamicIslandView.IslandState.TYPE_3_MAX)
                    DynamicIslandView.IslandState.TYPE_SPLIT -> island.setState(DynamicIslandView.IslandState.TYPE_2_MID)
                    else -> {}
                }
            }
        }

        view.onSwipeUp = {
            val island = islandViewRef?.get()
            if (island != null) {
                when (island.islandState.value) {
                    DynamicIslandView.IslandState.TYPE_3_MAX -> {
                        isUserExpanded = true
                        island.setState(DynamicIslandView.IslandState.TYPE_2_MID)
                    }
                    DynamicIslandView.IslandState.TYPE_2_MID -> {
                        isUserExpanded = false // Releasing the lock
                        island.setState(DynamicIslandView.IslandState.TYPE_1_MINI)
                        resolveHighestPriorityDebounced()
                    }
                    else -> {
                        isUserExpanded = false
                        forceHide()
                    }
                }
            }
        }

        view.onSwipeLeft = {
            val state = currentController?.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING || state == PlaybackState.STATE_PAUSED) {
                currentController?.transportControls?.skipToNext() // Swiping left typically means "Next"
            } else {
                forceHide()
            }
        }

        view.onSwipeRight = {
            val state = currentController?.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING || state == PlaybackState.STATE_PAUSED) {
                currentController?.transportControls?.skipToPrevious()
            } else {
                forceHide()
            }
        }

        view.onCloseClick = { forceHide() }
        view.onDoubleTap = {
            val island = islandViewRef?.get()
            if (island != null) {
                val state = island.islandState.value
                if (currentController != null && state == DynamicIslandView.IslandState.TYPE_3_MAX) {
                    // Action 1: Fast-Forward 10 seconds when in MAX media view
                    val currentPos = currentController?.playbackState?.position ?: 0L
                    currentController?.transportControls?.seekTo(currentPos + 10000L)
                } else {
                    // Action 2: Instantly collapse the island and release the Interaction Lock
                    isUserExpanded = false
                    island.setState(DynamicIslandView.IslandState.TYPE_1_MINI)
                    resolveHighestPriorityDebounced()
                }
            }
        }

        view.onPlayPauseClick = {
            val state = currentController?.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) currentController?.transportControls?.pause() else currentController?.transportControls?.play()
        }
        view.onLongPress = {
            val island = islandViewRef?.get()
            if (island != null && currentController != null) {
                try {
                    val intent = view.context.packageManager.getLaunchIntentForPackage(currentController!!.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    view.context.startActivity(intent)
                    forceHide() // Hide island to show the app
                } catch (e: Exception) {}
            }
        }

        view.onOutputSwitcherClick = {
            try {
                // First try the native Android 11+ Media Output Panel
                val intent = Intent("com.android.settings.panel.action.MEDIA_OUTPUT")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("com.android.settings.panel.extra.PACKAGE_NAME", currentController?.packageName)
                view.context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback: Show Volume Panel
                val audioManager = view.context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_SAME, android.media.AudioManager.FLAG_SHOW_UI)
            }
        }

        view.onPrevClick = { currentController?.transportControls?.skipToPrevious() }
        view.onNextClick = { currentController?.transportControls?.skipToNext() }
        view.onSeekTo = { pos -> currentController?.transportControls?.seekTo(pos) }

        // Connect the Loop/Shuffle Button
        view.onLoopClick = {
            val transport = currentController?.transportControls
            if (transport != null) {
                // In native Android, repeat modes aren't directly in PlaybackState, so we use common extras
                val currentMode = currentController?.playbackState?.extras?.getInt("android.media.session.extra.REPEAT_MODE") ?: 0
                val REPEAT_MODE_NONE = 0
                val REPEAT_MODE_ALL = 2
                val nextMode = if (currentMode == REPEAT_MODE_NONE) REPEAT_MODE_ALL else REPEAT_MODE_NONE

                try {
                    // Requires API 29+
                    val setRepeatModeMethod = transport.javaClass.getMethod("setRepeatMode", Int::class.java)
                    setRepeatModeMethod.invoke(transport, nextMode)
                } catch (e: Exception) {
                    // Fallback for older APIs or if method is hidden
                    val b = android.os.Bundle().apply { putInt("android.media.session.extra.REPEAT_MODE", nextMode) }
                    transport.sendCustomAction("android.media.session.action.SET_REPEAT_MODE", b)
                }
            }
        }

        // Connect the Heart/Like Button
        view.onLikeClick = {
            val transport = currentController?.transportControls
            if (transport != null) {
                transport.setRating(android.media.Rating.newHeartRating(true))
                /*
                currentController?.playbackState?.customActions?.firstOrNull {
                    it.action.contains("like", ignoreCase = true) || it.action.contains("thumb", ignoreCase = true)
                }?.let { customAction ->
                    transport.sendCustomAction(customAction, null)
                }
                */
            }
        }

        resolveHighestPriority()
    }

    fun onScreenStateChanged(isOn: Boolean) {
        isScreenOn = isOn
        islandViewRef?.get()?.post { islandViewRef?.get()?.updateScreenState(isOn) }
        if (isOn && currentController?.playbackState?.state == PlaybackState.STATE_PLAYING) {
            mainHandler.post(progressUpdater)
        } else {
            mainHandler.removeCallbacks(progressUpdater)
        }
    }

    fun postActivity(activity: LiveActivityModel) {
        dismissedActivities.remove(activity.id) // Ensure new triggers wake it up
        activeActivities[activity.id] = activity
        resolveHighestPriorityDebounced() // Updated

        if (activity.isTransient) {
            dismissalRunnables[activity.id]?.let { mainHandler.removeCallbacks(it) }
            val task = Runnable { removeActivity(activity.id) }
            dismissalRunnables[activity.id] = task
            mainHandler.postDelayed(task, 5000)
        }
    }

    private fun removeActivity(id: String) {
        dismissalRunnables.remove(id)?.let { mainHandler.removeCallbacks(it) }
        if (activeActivities.remove(id) != null) resolveHighestPriorityDebounced() // Updated
    }

    private fun resolveHighestPriorityDebounced() {
        resolveRunnable?.let { mainHandler.removeCallbacks(it) }
        resolveRunnable = Runnable { resolveHighestPriority() }
        mainHandler.postDelayed(resolveRunnable!!, 250) // Wait 250ms for the dust to settle before changing the UI
    }

    private fun resolveHighestPriority() {
        val island = islandViewRef?.get() ?: return

        // Filter out anything the user swiped up to dismiss
        val available = activeActivities.values.filter { !dismissedActivities.contains(it.id) }
        val sorted = available.sortedByDescending { it.type.priority }

        island.post {
            val primary = sorted.getOrNull(0)
            val secondary = sorted.getOrNull(1)

            island.clearLiveActivityUI()
            island.clearSecondaryActivityUI()

            if (primary != null && secondary != null) {
                // MULTIPLE ACTIVITIES = SPLIT PILL
                island.updateActivities(primary, secondary)
                if (island.islandState.value == DynamicIslandView.IslandState.HIDDEN) island.setState(DynamicIslandView.IslandState.TYPE_SPLIT)

            } else if (primary != null && currentController != null && primary.type != ActivityType.MEDIA && !dismissedActivities.contains("sys_media")) {
                // MEDIA ACTIVE + NOTIFICATION = SPLIT PILL
                island.updateActivities(LiveActivityModel("sys_media", ActivityType.MEDIA, "Media", "Playing", accentColor = android.graphics.Color.MAGENTA), primary)
                if (island.islandState.value == DynamicIslandView.IslandState.HIDDEN) island.setState(DynamicIslandView.IslandState.TYPE_SPLIT)

            } else if (primary != null) {
                // SINGLE NOTIFICATION
                if (primary.type == ActivityType.MEDIA && currentController != null) {
                    if (island.islandState.value == DynamicIslandView.IslandState.HIDDEN) island.setState(DynamicIslandView.IslandState.TYPE_1_MINI)
                } else {
                    island.updateLiveActivity(primary.title, primary.dataText, primary.progress, primary.accentColor, primary.type)
                    val targetState = if (primary.type == ActivityType.MESSAGE || primary.type == ActivityType.CALL) DynamicIslandView.IslandState.TYPE_2_MID else DynamicIslandView.IslandState.TYPE_1_MINI
                    if (island.islandState.value == DynamicIslandView.IslandState.HIDDEN) island.setState(targetState)
                }
            } else {
                // NOTHING ACTIVE
                if (currentController == null || dismissedActivities.contains("sys_media")) {
                    island.setState(DynamicIslandView.IslandState.HIDDEN)
                }
            }
        }
    }

    fun forceHide() {
        val available = activeActivities.values.filter { !dismissedActivities.contains(it.id) }.sortedByDescending { it.type.priority }
        val highest = available.getOrNull(0)

        if (highest != null) {
            dismissedActivities.add(highest.id)
        } else if (currentController != null) {
            dismissedActivities.add("sys_media")
        }
        islandViewRef?.get()?.setState(DynamicIslandView.IslandState.HIDDEN)
        resolveHighestPriority() // Recalculate to see if something else should pop up
    }

    fun hookFrameworkNotifications(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val listenerClass = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.NotificationListener", lpparam.classLoader)
            if (listenerClass != null) {
                XposedBridge.hookAllMethods(listenerClass, "onNotificationPosted", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) { param.args.forEach { arg -> processNotificationArg(arg, ::handleNotificationPosted) } }
                })
            }
            val collectionClass = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.notification.collection.NotifCollection", lpparam.classLoader)
            if (collectionClass != null) {
                XposedBridge.hookAllMethods(collectionClass, "postNotification", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) { param.args.forEach { arg -> processNotificationArg(arg, ::handleNotificationPosted) } }
                })
                arrayOf("tryRemoveNotification", "onNotificationRemoved").forEach { methodName ->
                    XposedBridge.hookAllMethods(collectionClass, methodName, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) { param.args.forEach { arg -> processNotificationArg(arg, ::handleNotificationRemoved) } }
                    })
                }
            }
            log("[SUCCESS] Safe SystemUI Notification Hooks Applied")
        } catch (e: Throwable) { log("[FATAL] SystemUI Hook Error: $e") }
    }

    private fun processNotificationArg(arg: Any?, action: (android.service.notification.StatusBarNotification) -> Unit) {
        if (arg == null) return
        try {
            if (arg.javaClass.name.contains("StatusBarNotification")) { action(arg as android.service.notification.StatusBarNotification)
            } else if (arg.javaClass.name.contains("NotificationEntry")) {
                var currentClass: Class<*>? = arg.javaClass; var sbnField: java.lang.reflect.Field? = null
                while (currentClass != null) {
                    try { sbnField = currentClass.getDeclaredField("mSbn"); sbnField.isAccessible = true; break } catch (e: Exception) { currentClass = currentClass.superclass }
                }
                val sbn = sbnField?.get(arg) as? android.service.notification.StatusBarNotification
                if (sbn != null) action(sbn)
            }
        } catch (e: Throwable) { log("[ERROR] Process arg failed: $e") }
    }

    private fun handleNotificationPosted(sbn: android.service.notification.StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras ?: return
            val category = notification.category ?: ""
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            when (category) {
                Notification.CATEGORY_ALARM -> postActivity(LiveActivityModel(sbn.key, ActivityType.ALARM, "ALARM", title, accentColor = android.graphics.Color.RED))
                Notification.CATEGORY_CALL -> postActivity(LiveActivityModel(sbn.key, ActivityType.CALL, "Incoming Call", title, accentColor = android.graphics.Color.GREEN))
                Notification.CATEGORY_NAVIGATION -> postActivity(LiveActivityModel(sbn.key, ActivityType.NAVIGATION, title, text, accentColor = android.graphics.Color.parseColor("#4285F4")))
                Notification.CATEGORY_MESSAGE, Notification.CATEGORY_SOCIAL -> postActivity(LiveActivityModel(sbn.key, ActivityType.MESSAGE, title, text, accentColor = android.graphics.Color.YELLOW, isTransient = true))
                else -> {
                    val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
                    val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, -1)

                    if (max > 0 && progress >= 0) {
                        val percent = try { (progress.toFloat() / max.toFloat()).coerceIn(0f, 1f) } catch (e: Exception) { 0f }
                        val finalPercent = if (percent.isNaN() || percent.isInfinite()) 0f else percent
                        postActivity(LiveActivityModel(sbn.key, ActivityType.DOWNLOAD, if (title.isBlank()) "Downloading" else title, "${(finalPercent * 100).toInt()}%", finalPercent, android.graphics.Color.CYAN))
                    } else if (progress < 0 || max <= 0) {
                        if (activeActivities.containsKey(sbn.key)) removeActivity(sbn.key)
                    }
                }
            }
        } catch (e: Throwable) { log("[ERROR] Parse posted failed: $e") }
    }

    private fun handleNotificationRemoved(sbn: android.service.notification.StatusBarNotification) { removeActivity(sbn.key) }

    private fun setupSystemReceivers(context: Context) {
        val ringerReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: android.content.Intent) {
                val am = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                val mode = when (am.ringerMode) {
                    android.media.AudioManager.RINGER_MODE_SILENT -> "Silent"; android.media.AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"; else -> "Ringing"
                }
                postActivity(LiveActivityModel(id = "sys_ringer", type = ActivityType.GENERAL, title = "Ringer", dataText = mode, accentColor = if (mode == "Silent") android.graphics.Color.RED else android.graphics.Color.LTGRAY, isTransient = true))
            }
        }
        val headsetReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: android.content.Intent) {
                if (intent.getIntExtra("state", -1) == 1) postActivity(LiveActivityModel(id = "sys_headset", type = ActivityType.GENERAL, title = "Headphones", dataText = "Connected", accentColor = android.graphics.Color.CYAN, isTransient = true))
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(ringerReceiver, android.content.IntentFilter(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION), Context.RECEIVER_NOT_EXPORTED)
            context.registerReceiver(headsetReceiver, android.content.IntentFilter(android.content.Intent.ACTION_HEADSET_PLUG), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(ringerReceiver, android.content.IntentFilter(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION))
            context.registerReceiver(headsetReceiver, android.content.IntentFilter(android.content.Intent.ACTION_HEADSET_PLUG))
        }
    }

    private fun setupMediaListener(context: Context, retries: Int = 3) {
        try {
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            try {
                val componentName = android.content.ComponentName("com.android.systemui", "com.android.systemui.SystemUIService")
                mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, componentName)
                updateActiveController(mediaSessionManager?.getActiveSessions(componentName))
            } catch (e: SecurityException) {
                mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, null)
                updateActiveController(mediaSessionManager?.getActiveSessions(null))
            }
            log("[SUCCESS] MediaSessionManager initialized safely.")
        } catch (e: Throwable) {
            if (retries > 0) mainHandler.postDelayed({ setupMediaListener(context, retries - 1) }, 5000)
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            currentController?.unregisterCallback(mediaCallback); currentController = null
            updateMediaState(null); islandViewRef?.get()?.post { islandViewRef?.get()?.clearMusicState() }
            return
        }
        val bestController = controllers.firstOrNull {
            val state = it.playbackState?.state; state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        } ?: controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PAUSED } ?: controllers.first()

        if (currentController != null && currentController?.packageName == bestController.packageName) {
            updateMediaState(bestController.playbackState); updateMetadata(bestController.metadata); return
        }
        currentController?.unregisterCallback(mediaCallback); currentController = bestController
        currentController?.registerCallback(mediaCallback)
        updateMediaState(currentController?.playbackState); updateMetadata(currentController?.metadata)
    }

    private fun updateMediaState(state: PlaybackState?) {
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING || state?.state == PlaybackState.STATE_BUFFERING
        if (isPlaying) postActivity(LiveActivityModel("sys_media", ActivityType.MEDIA, "Media", "Playing", accentColor = android.graphics.Color.MAGENTA)) else removeActivity("sys_media")
        islandViewRef?.get()?.post {
            islandViewRef?.get()?.updatePlayPauseState(isPlaying)
            if (isPlaying && isScreenOn) { mainHandler.removeCallbacks(progressUpdater); mainHandler.post(progressUpdater) } else { mainHandler.removeCallbacks(progressUpdater) }
        }
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        val island = islandViewRef?.get() ?: return
        if (metadata == null) return

        val title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        val artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown"
        val albumArt = metadata.description?.iconBitmap
            ?: metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)

        mediaDuration = metadata.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION)
        val packageName = currentController?.packageName ?: ""

        // Process the App Icon in the background to prevent Compose UI stutter
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var appIconBitmap: android.graphics.Bitmap? = null
            if (packageName.isNotEmpty()) {
                try {
                    val pm = island.context.packageManager
                    val drawable = pm.getApplicationIcon(packageName)
                    // Convert Drawable to Bitmap safely
                    val width = drawable.intrinsicWidth.coerceAtLeast(1)
                    val height = drawable.intrinsicHeight.coerceAtLeast(1)
                    appIconBitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(appIconBitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                } catch (e: Exception) {
                    // App not found or failed to load icon, appIconBitmap remains null
                }
            }

            var accentColor = android.graphics.Color.CYAN
            if (albumArt != null && !albumArt.isRecycled) {
                try {
                    val palette = androidx.palette.graphics.Palette.from(albumArt).generate()
                    accentColor = palette.getDominantColor(android.graphics.Color.CYAN)
                } catch (e: Exception) {
                    log("Failed to extract palette color: $e")
                }
            }

            // Post the fully prepared data to the UI thread
            island.post {
                island.updateMusicInfo(title, artist, albumArt, packageName, appIconBitmap, androidx.compose.ui.graphics.Color(accentColor))
                island.updateMusicProgress(currentController?.playbackState?.position ?: 0L, mediaDuration)
            }
        }
    }
}
