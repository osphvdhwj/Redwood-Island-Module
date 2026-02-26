package com.example.dynamicisland

import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Handler
import android.net.wifi.WifiManager
import android.hardware.camera2.CameraManager
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.view.View
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.ref.WeakReference

// Generic API Model for external modules/hooks to push Live Activities
data class LiveActivityModel(
    val id: String,
    val title: String,
    val dataText: String,
    val accentColor: Int = Color.WHITE,
    val progress: Float? = null
)

object IslandController {
    private var islandViewRef: WeakReference<DynamicIslandView>? = null
    private var currentController: MediaController? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var activeLiveActivity: LiveActivityModel? = null
    private var isExpanding = false
    private var currentNotificationIntent: PendingIntent? = null
    private var dismissRunnable: Runnable? = null
    private var mediaSessionManager: MediaSessionManager? = null

    // Progress Bar Handling
    private var mediaDuration: Long = 0L
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressUpdater = object : Runnable {
        override fun run() {
            val island = islandViewRef?.get()
            val state = currentController?.playbackState
            val isPlaying = state?.state == PlaybackState.STATE_PLAYING

            if (island != null && island.state != DynamicIslandView.IslandState.HIDDEN && isPlaying) {
                if (island.state == DynamicIslandView.IslandState.MINI && activeLiveActivity != null) {
                    // Skip
                } else {
                    val currentPosition = state?.position ?: 0L
                    island.updateMusicProgress(currentPosition, mediaDuration)
                }
                progressHandler.postDelayed(this, 1000)
            }
        }
    }

    // Performance Monitor
    private var performanceMonitorRunnable: Runnable? = null
    private var isPerformanceMonitorActive = false

    // Temporary Activities
    private var temporaryActivityRunnable: Runnable? = null

    private val sessionsListener = object : MediaSessionManager.OnActiveSessionsChangedListener {
        override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
            updateActiveController(controllers)
        }
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaState(state)
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMetadata(metadata)
        }
        override fun onSessionDestroyed() {
            val componentName = ComponentName(islandViewRef?.get()?.context ?: return, "com.android.systemui.SystemUIService")
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            if (controllers.isNullOrEmpty()) {
                currentController = null
                val island = islandViewRef?.get() ?: return
                island.post {
                     if (activeLiveActivity == null) {
                         island.collapse()
                     }
                }
            } else {
                updateActiveController(controllers)
            }
        }
    }

    fun init(view: DynamicIslandView) {
        islandViewRef = WeakReference(view)
        setupMediaListener(view.context)
        setupSystemMonitors(view.context) // NEW: Initialize System Monitors
        setupDashboard(view) // NEW: Populate Dashboard

        // Handle Gestures
        view.onGestureListener = { action ->
            when (action) {
                DynamicIslandView.GestureAction.SINGLE_TAP -> {
                    when (view.state) {
                         DynamicIslandView.IslandState.HIDDEN -> showMini()
                         DynamicIslandView.IslandState.MINI -> expand()
                         DynamicIslandView.IslandState.EXPANDED -> collapse()
                         DynamicIslandView.IslandState.DASHBOARD -> expand() // Tap on Dashboard -> Back to Expanded (Music)
                    }
                }
                DynamicIslandView.GestureAction.DOUBLE_TAP -> {
                    if (currentController != null) {
                        try {
                            currentController?.sessionActivity?.send()
                            collapse()
                        } catch (e: Exception) {}
                    } else if (currentNotificationIntent != null) {
                        try {
                            currentNotificationIntent?.send()
                            collapse()
                        } catch (e: Exception) {}
                    }
                }
                DynamicIslandView.GestureAction.LONG_PRESS -> {
                    togglePerformanceMonitor() // NEW: Long press toggles monitor
                }
                DynamicIslandView.GestureAction.SWIPE_DOWN -> {
                    if (view.state == DynamicIslandView.IslandState.EXPANDED) {
                        showDashboard()
                    } else {
                        expand()
                    }
                }
                DynamicIslandView.GestureAction.SWIPE_UP -> {
                    when (view.state) {
                        DynamicIslandView.IslandState.MINI -> forceHide()
                        DynamicIslandView.IslandState.DASHBOARD -> expand() // Back to Music
                        else -> collapse()
                    }
                }
                DynamicIslandView.GestureAction.SWIPE_LEFT -> handleSwipe(isRight = false)
                DynamicIslandView.GestureAction.SWIPE_RIGHT -> handleSwipe(isRight = true)
            }
        }

        // Default click listener (fallback)
        view.setOnClickListener {
             if (view.state == DynamicIslandView.IslandState.HIDDEN) showMini()
             else if (view.state == DynamicIslandView.IslandState.MINI) expand()
             else collapse()
        }
    }

    // --- NEW: System Monitors (Battery, Bluetooth, Clipboard) ---
    private fun setupSystemMonitors(context: Context) {
        // 1. Quick-Save / Clipboard Hub
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text.toString()
                val shortText = if (text.length > 18) text.substring(0, 18) + "..." else text

                // Show a yellow pop-up for 3 seconds
                postTemporaryActivity(
                    LiveActivityModel("clipboard", "Copied", shortText, Color.parseColor("#FFD700")),
                    3000
                )
            }
        }

        // 2. Charging & Battery Alerts
        val powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                        val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                        postTemporaryActivity(
                            LiveActivityModel("power", "Charging", "$level%", Color.parseColor("#4CAF50")),
                            4000
                        )
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        postTemporaryActivity(
                            LiveActivityModel("power", "Unplugged", "On Battery", Color.parseColor("#F44336")),
                            3000
                        )
                    }
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        if (level == 15) {
                            postTemporaryActivity(
                                LiveActivityModel("power", "Battery Low", "15% Remaining", Color.RED),
                                5000
                            )
                        }
                    }
                }
            }
        }

        val powerFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        // FLAG_RECEIVER_EXPORTED is required in Android 14+
        context.registerReceiver(powerReceiver, powerFilter, Context.RECEIVER_EXPORTED)

        // 3. Connectivity (Bluetooth)
        val btReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val name = try { device?.name ?: "Device" } catch(e: SecurityException) { "BT Device" }

                when (intent.action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        postTemporaryActivity(
                            LiveActivityModel("bt", "Connected", name, Color.parseColor("#007AFF")),
                            4000
                        )
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        postTemporaryActivity(
                            LiveActivityModel("bt", "Disconnected", name, Color.GRAY),
                            3000
                        )
                    }
                }
            }
        }
        val btFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(btReceiver, btFilter, Context.RECEIVER_EXPORTED)
    }

    // --- NEW: Helper to show a LiveActivity for a set amount of time ---
    private fun postTemporaryActivity(activity: LiveActivityModel, durationMs: Long) {
        postLiveActivity(activity)

        // Cancel any previous dismissals
        temporaryActivityRunnable?.let { mainHandler.removeCallbacks(it) }

        temporaryActivityRunnable = Runnable {
            removeLiveActivity(activity.id)
        }
        mainHandler.postDelayed(temporaryActivityRunnable!!, durationMs)
    }

    // --- NEW: Performance Monitoring Toggle ---
    fun togglePerformanceMonitor() {
        isPerformanceMonitorActive = !isPerformanceMonitorActive

        if (isPerformanceMonitorActive) {
            performanceMonitorRunnable = object : Runnable {
                override fun run() {
                    val temp = HardwareMonitors.getCpuTemp()
                    val freq = HardwareMonitors.getCpuFreq()

                    // Warning color if temp is high (> 45C)
                    val color = if (temp > 45f) Color.RED else Color.CYAN

                    postLiveActivity(LiveActivityModel(
                        id = "sys_monitor",
                        title = "Performance",
                        dataText = "${"%.1f".format(temp)}ºC | $freq",
                        accentColor = color
                    ))

                    // Update every 2 seconds
                    if (isPerformanceMonitorActive) {
                        mainHandler.postDelayed(this, 2000)
                    }
                }
            }
            mainHandler.post(performanceMonitorRunnable!!)
        } else {
            performanceMonitorRunnable?.let { mainHandler.removeCallbacks(it) }
            removeLiveActivity("sys_monitor")
        }
    }

    // --- Dashboard Setup ---
    private fun setupDashboard(view: DynamicIslandView) {
        val container = view.dashboardContainer
        if (container.childCount == 0) return
        val content = container.getChildAt(0) as? LinearLayout ?: return

        if (content.childCount >= 3) {
            val qsTab = content.getChildAt(0) as LinearLayout
            val appsTab = content.getChildAt(1) as LinearLayout
            // val hiddenTab = content.getChildAt(2) as LinearLayout

            populateQuickSettings(view.context, qsTab)
            populatePinnedApps(view.context, appsTab)
        }
    }

    private fun populateQuickSettings(context: Context, layout: LinearLayout) {
        // Simple Text Toggles for now
        val wifiToggle = TextView(context).apply {
            text = "Wi-Fi: Toggle"
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 20, 0, 20)
            gravity = Gravity.CENTER
            setOnClickListener {
                try {
                    val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    // Note: setWifiEnabled is deprecated/restricted in newer Android, requires permission
                    // For Xposed, we might need XposedHelpers to call internal methods or just show toast if failed.
                    @Suppress("DEPRECATION")
                    wm.isWifiEnabled = !wm.isWifiEnabled
                    text = "Wi-Fi: " + if (wm.isWifiEnabled) "On" else "Off"
                } catch (e: Exception) {
                    text = "Wi-Fi: Error"
                }
            }
        }

        val flashToggle = TextView(context).apply {
            text = "Flashlight: Toggle"
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 20, 0, 20)
            gravity = Gravity.CENTER
            setOnClickListener {
                try {
                    val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val id = cm.cameraIdList[0]
                    // We need a way to track state. Simplification: just toggle on click based on assumption or check?
                    // Accessing flash status is tricky without callback.
                    // Let's just try to turn ON for now, or toggle global var?
                    // Implementation skipped for brevity, just a placeholder action
                } catch (e: Exception) {}
            }
        }

        layout.addView(wifiToggle)
        layout.addView(flashToggle)
    }

    private fun populatePinnedApps(context: Context, layout: LinearLayout) {
        val apps = listOf("com.android.settings", "com.android.camera2", "com.android.calculator2")
        val pm = context.packageManager

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        for (pkg in apps) {
            try {
                val icon = pm.getApplicationIcon(pkg)
                val intent = pm.getLaunchIntentForPackage(pkg)

                val img = android.widget.ImageView(context).apply {
                    setImageDrawable(icon)
                    layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                        setMargins(10, 0, 10, 0)
                    }
                    setOnClickListener {
                        if (intent != null) {
                            context.startActivity(intent)
                            collapse()
                        }
                    }
                }
                row.addView(img)
            } catch (e: Exception) {}
        }
        layout.addView(row)
    }

    fun isExpanding(): Boolean = isExpanding

    private fun handleSwipe(isRight: Boolean) {
        val island = islandViewRef?.get()
        if (island?.state == DynamicIslandView.IslandState.DASHBOARD) {
            // Let the ScrollView handle it, or programmatic scroll if needed.
            // Since ScrollView consumes touch, this gesture listener might not even fire if touched inside.
            // But if touched on edge, we probably shouldn't change track.
            return
        }

        val state = currentController?.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING ||
            state == PlaybackState.STATE_PAUSED ||
            state == PlaybackState.STATE_BUFFERING) {

            if (isRight) currentController?.transportControls?.skipToNext()
            else currentController?.transportControls?.skipToPrevious()
        } else {
            collapse()
        }
    }

    fun forceHide() {
        val island = islandViewRef?.get() ?: return
        island.hide()
        island.setContextGlow(null)
    }

    // --- Live Activities API ---
    fun postLiveActivity(activity: LiveActivityModel) {
        activeLiveActivity = activity
        val island = islandViewRef?.get() ?: return

        island.post {
            island.updateLiveActivity(activity.title, activity.dataText, activity.progress, activity.accentColor)
            island.updateMiniPillContent(activity.title + ": " + activity.dataText, null, activity.accentColor)

            if (island.state == DynamicIslandView.IslandState.HIDDEN) {
                showMini()
            }
        }
    }

    fun removeLiveActivity(id: String) {
        if (activeLiveActivity?.id == id) {
            activeLiveActivity = null

            val state = currentController?.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                updateMetadata(currentController?.metadata)
            }

            collapse()
        }
    }

    fun showDashboard() {
        isExpanding = true
        islandViewRef?.get()?.showDashboard()
    }

    fun expand() {
        isExpanding = true
        islandViewRef?.get()?.let {
            it.animate().translationY(0f).setDuration(0).start()
            it.expand()
        }

        // Start Progress Tracking if expanding while playing
        if (currentController?.playbackState?.state == PlaybackState.STATE_PLAYING) {
             progressHandler.removeCallbacks(progressUpdater)
             progressHandler.post(progressUpdater)
        }
    }

    fun showMini() {
        isExpanding = false
        islandViewRef?.get()?.showMini()
    }

    fun collapse() {
        isExpanding = false
        islandViewRef?.get()?.collapse()

        // Stop Progress Tracking
        progressHandler.removeCallbacks(progressUpdater)
    }

    // --- Notification Handling ---
    fun hookFrameworkNotifications(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val wrapperClass = XposedHelpers.findClass(
                "android.service.notification.INotificationListener",
                lpparam.classLoader
            )

            XposedBridge.hookAllMethods(wrapperClass, "onNotificationPosted", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val holder = param.args[0]
                        val sbn = XposedHelpers.callMethod(holder, "get") as android.service.notification.StatusBarNotification
                        val notif = sbn.notification

                        // 1. Check for Live Activity Candidates first
                        val liveActivity = ClockInterceptor.inspect(sbn)
                        if (liveActivity != null) {
                            postLiveActivity(liveActivity)
                            return // Stop here! We handled it as a Live Activity.
                        }

                        // 2. Fallback to standard Notification handling
                        if ((notif.flags and android.app.Notification.FLAG_ONGOING_EVENT) == 0) {
                            val title = notif.extras.getString(android.app.Notification.EXTRA_TITLE)
                            val text = notif.extras.getString(android.app.Notification.EXTRA_TEXT)
                            val icon = notif.getLargeIcon() ?: notif.getSmallIcon()

                            val island = islandViewRef?.get()
                            if (island != null) {
                                onNotificationShow(title, text, icon, notif.contentIntent, island.context)
                            }
                        }
                    } catch (e: Throwable) {
                         XposedBridge.log("DynamicIsland: [ERROR] IPC extraction failed: " + e)
                    }
                }
            })
            XposedBridge.log("DynamicIsland: [SUCCESS] Framework IPC Notification hook applied")
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [FATAL] Framework IPC hook failed: " + e)
        }
    }

    private fun onNotificationShow(title: String?, text: String?, icon: Icon?, contentIntent: PendingIntent?, context: Context) {
        val island = islandViewRef?.get() ?: return

        currentNotificationIntent = contentIntent

        var bitmap: Bitmap? = null
        try {
            val drawable = icon?.loadDrawable(context)
            if (drawable is BitmapDrawable) {
                bitmap = drawable.bitmap
            } else if (drawable != null) {
                bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        } catch (e: Exception) {}

        island.post {
            island.setContextGlow(bitmap)
            island.updateNotificationInfo(title, text, icon)
            expand()

            dismissRunnable?.let { island.removeCallbacks(it) }
            dismissRunnable = Runnable {
                if (activeLiveActivity == null && currentController?.playbackState?.state != PlaybackState.STATE_PLAYING) {
                    collapse()
                }
            }
            island.postDelayed(dismissRunnable, 4000)
        }
    }

    // --- Media Handling ---
    private fun setupMediaListener(context: Context) {
        try {
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(context, "com.android.systemui.SystemUIService")
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, componentName)
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            updateActiveController(controllers)
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [ERROR] Media setup failed: " + e)
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        if (controllers == null) return

        if (controllers.isEmpty()) {
            currentController?.unregisterCallback(mediaCallback)
            currentController = null
            updateMediaState(null)
            return
        }

        val bestController = controllers.firstOrNull {
            val state = it.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        } ?: controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PAUSED
        } ?: controllers.first()

        if (currentController != null &&
            currentController?.packageName == bestController.packageName) {
            updateMediaState(bestController.playbackState)
            updateMetadata(bestController.metadata)
            return
        }

        currentController?.unregisterCallback(mediaCallback)
        currentController = bestController
        currentController?.registerCallback(mediaCallback)

        updateMediaState(currentController?.playbackState)
        updateMetadata(currentController?.metadata)
    }

    private fun updateMediaState(state: PlaybackState?) {
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING || state?.state == PlaybackState.STATE_BUFFERING
        val island = islandViewRef?.get() ?: return

        island.post {
            island.updatePlayPauseState(isPlaying)
            if (isPlaying) {
                if (island.state == DynamicIslandView.IslandState.HIDDEN) {
                    showMini()
                }
                progressHandler.removeCallbacks(progressUpdater)
                progressHandler.post(progressUpdater)
            } else {
                 progressHandler.removeCallbacks(progressUpdater)

                 island.postDelayed({
                    val currentState = currentController?.playbackState?.state
                    val stillPlaying = currentState == PlaybackState.STATE_PLAYING || currentState == PlaybackState.STATE_BUFFERING
                    if (!stillPlaying && activeLiveActivity == null) {
                        collapse()
                    }
                }, 2000)
            }
        }
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        val island = islandViewRef?.get() ?: return
        if (metadata == null) return

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                       ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        mediaDuration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

        island.post {
            island.updateMusicInfo(title, artist, albumArt)
            val pos = currentController?.playbackState?.position ?: 0L
            island.updateMusicProgress(pos, mediaDuration)

            activeLiveActivity?.let { activity ->
                 island.updateMiniPillContent(activity.title + ": " + activity.dataText, null, activity.accentColor)
            }
        }
    }
}
