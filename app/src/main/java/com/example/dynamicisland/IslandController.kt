package com.example.dynamicisland

import android.app.Notification
import android.content.Context
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

object IslandController {
    private var islandViewRef: WeakReference<DynamicIslandView>? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var currentController: MediaController? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaDuration = 0L

    // The Smart Tracker
    private val activeActivities = ConcurrentHashMap<String, LiveActivityModel>()
    private val dismissalRunnables = ConcurrentHashMap<String, Runnable>()
    private val dismissedActivities = mutableSetOf<String>()
    private var isScreenOn = true

    private fun log(msg: String) {
        XposedBridge.log("DynamicIsland: $msg")
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) { updateMediaState(state) }
        override fun onMetadataChanged(metadata: MediaMetadata?) { updateMetadata(metadata) }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers)
    }

    private val progressUpdater = object : Runnable {
        override fun run() {
            if (!isScreenOn) return
            val controller = currentController ?: return
            val state = controller.playbackState
            if (state != null && (state.state == PlaybackState.STATE_PLAYING || state.state == PlaybackState.STATE_BUFFERING)) {
                val position = state.position + (System.currentTimeMillis() - state.lastPositionUpdateTime)
                islandViewRef?.get()?.updateMusicProgress(position, mediaDuration)
                mainHandler.postDelayed(this, 1000)
            }
        }
    }

    fun init(view: DynamicIslandView) {
        islandViewRef = WeakReference(view)
        setupMediaListener(view.context)

        BatteryPlugin.onBatteryChanged = { level, isCharging, color ->
             if (isCharging) {
                 postActivity(LiveActivityModel(
                     id = "sys_battery", type = ActivityType.CHARGING,
                     title = "Charging", dataText = "$level%",
                     progress = level / 100f, accentColor = color, isTransient = true
                 ))
             }
        }
        BatteryPlugin.start(view.context)

        view.onSingleTap = {
            val island = islandViewRef?.get()
            if (island != null) {
                when (island.islandState.value) {
                    DynamicIslandView.IslandState.TYPE_1_MINI -> island.setState(DynamicIslandView.IslandState.TYPE_2_MID)
                    DynamicIslandView.IslandState.TYPE_2_MID -> island.setState(DynamicIslandView.IslandState.TYPE_3_MAX)
                    DynamicIslandView.IslandState.TYPE_3_MAX -> island.setState(DynamicIslandView.IslandState.TYPE_1_MINI)
                    else -> {}
                }
            }
        }

        view.onSwipeUp = { forceHide() }
        view.onCloseClick = { forceHide() }
        view.onPlayPauseClick = {
            val state = currentController?.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                currentController?.transportControls?.pause()
            } else {
                currentController?.transportControls?.play()
            }
        }
        view.onPrevClick = { currentController?.transportControls?.skipToPrevious() }
        view.onNextClick = { currentController?.transportControls?.skipToNext() }
        view.onSeekTo = { pos -> currentController?.transportControls?.seekTo(pos) }

        // Refresh UI in case activities arrived during the 6-second boot delay
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
        activeActivities[activity.id] = activity
        resolveHighestPriority()

        if (activity.isTransient) {
            dismissalRunnables[activity.id]?.let { mainHandler.removeCallbacks(it) }
            val task = Runnable { removeActivity(activity.id) }
            dismissalRunnables[activity.id] = task
            mainHandler.postDelayed(task, 5000)
        }
    }

    private fun removeActivity(id: String) {
        dismissalRunnables.remove(id)?.let { mainHandler.removeCallbacks(it) }
        if (activeActivities.remove(id) != null) {
            resolveHighestPriority()
        }
    }

    private fun resolveHighestPriority() {
        val island = islandViewRef?.get() ?: return
        val highest = activeActivities.values.maxByOrNull { it.type.priority }

        island.post {
            if (highest != null) {
                if (highest.type == ActivityType.MEDIA && currentController != null) {
                    island.clearLiveActivityUI()
                    if (island.islandState.value == DynamicIslandView.IslandState.HIDDEN) {
                        island.setState(DynamicIslandView.IslandState.TYPE_1_MINI)
                    }
                } else {
                    island.updateLiveActivity(highest.title, highest.dataText, highest.progress, highest.accentColor, highest.type)
                    val targetState = if (highest.type == ActivityType.MESSAGE || highest.type == ActivityType.CALL)
                                      DynamicIslandView.IslandState.TYPE_2_MID else DynamicIslandView.IslandState.TYPE_1_MINI
                    if (island.islandState.value == DynamicIslandView.IslandState.HIDDEN) {
                        island.setState(targetState)
                    }
                }
            } else {
                if (currentController == null) {
                    island.setState(DynamicIslandView.IslandState.HIDDEN)
                } else {
                    island.clearLiveActivityUI()
                }
            }
        }
    }

    fun forceHide() {
        val highest = activeActivities.values.maxByOrNull { it.type.priority }
        if (highest != null) {
            dismissedActivities.add(highest.id)
        } else if (currentController != null) {
            dismissedActivities.add("sys_media")
        }
        islandViewRef?.get()?.setState(DynamicIslandView.IslandState.HIDDEN)
    }

    // --- PUNCH-HOLE SAFE SYSTEM UI HOOKS ---
    fun hookFrameworkNotifications(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook 1: Earliest reliable notification intercept (NotificationListener)
            val listenerClass = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.NotificationListener", lpparam.classLoader)
            if (listenerClass != null) {
                XposedBridge.hookAllMethods(listenerClass, "onNotificationPosted", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.args.forEach { arg -> processNotificationArg(arg, ::handleNotificationPosted) }
                    }
                })
            }

            // Hook 2: Modern Android 12+ Intercept (NotifCollection)
            val collectionClass = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.notification.collection.NotifCollection", lpparam.classLoader)
            if (collectionClass != null) {
                XposedBridge.hookAllMethods(collectionClass, "postNotification", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.args.forEach { arg -> processNotificationArg(arg, ::handleNotificationPosted) }
                    }
                })

                val removeMethods = arrayOf("tryRemoveNotification", "onNotificationRemoved")
                removeMethods.forEach { methodName ->
                    XposedBridge.hookAllMethods(collectionClass, methodName, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.args.forEach { arg -> processNotificationArg(arg, ::handleNotificationRemoved) }
                        }
                    })
                }
            }
            log("[SUCCESS] Safe SystemUI Notification Hooks Applied")
        } catch (e: Throwable) { log("[FATAL] SystemUI Hook Error: $e") }
    }

    private fun processNotificationArg(arg: Any?, action: (android.service.notification.StatusBarNotification) -> Unit) {
        if (arg == null) return
        try {
            if (arg.javaClass.name.contains("StatusBarNotification")) {
                action(arg as android.service.notification.StatusBarNotification)
            } else if (arg.javaClass.name.contains("NotificationEntry")) {
                var currentClass: Class<*>? = arg.javaClass
                var sbnField: java.lang.reflect.Field? = null
                while (currentClass != null) {
                    try {
                        sbnField = currentClass.getDeclaredField("mSbn")
                        sbnField.isAccessible = true
                        break
                    } catch (e: Exception) { currentClass = currentClass.superclass }
                }
                val sbn = sbnField?.get(arg) as? android.service.notification.StatusBarNotification
                if (sbn != null) action(sbn)
            }
        } catch (e: Throwable) { log("[ERROR] Process arg failed: $e") }
    }

    private fun handleNotificationPosted(sbn: android.service.notification.StatusBarNotification) {
        try {
            val notification = sbn.notification
            // FIX 2: Strict Null Safety for Extras (This caused the song download crash)
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
                        // FIX 3: Bulletproof Math Check
                        val percent = try {
                            (progress.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                        } catch (e: Exception) { 0f }

                        val finalPercent = if (percent.isNaN() || percent.isInfinite()) 0f else percent
                        postActivity(LiveActivityModel(sbn.key, ActivityType.DOWNLOAD, if (title.isBlank()) "Downloading" else title, "${(finalPercent * 100).toInt()}%", finalPercent, android.graphics.Color.CYAN))
                    } else if (progress < 0 || max <= 0) {
                        // Only remove if it was actually a download completing, don't accidentally kill standard notifications
                        if (activeActivities.containsKey(sbn.key)) {
                            removeActivity(sbn.key)
                        }
                    }
                }
            }
        } catch (e: Throwable) { log("[ERROR] Parse posted failed: $e") }
    }

    private fun handleNotificationRemoved(sbn: android.service.notification.StatusBarNotification) {
        removeActivity(sbn.key)
    }

    private fun setupMediaListener(context: Context, retries: Int = 3) {
        try {
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            try {
                val componentName = android.content.ComponentName("com.android.systemui", "com.android.systemui.SystemUIService")
                mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, componentName)
                updateActiveController(mediaSessionManager?.getActiveSessions(componentName))
            } catch (e: SecurityException) {
                // Fallback if component name is restricted
                mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, null)
                updateActiveController(mediaSessionManager?.getActiveSessions(null))
            }
            log("[SUCCESS] MediaSessionManager initialized safely.")
        } catch (e: Throwable) {
            log("[WARN] Media setup failed, retries left $retries: $e")
            if (retries > 0) {
                mainHandler.postDelayed({ setupMediaListener(context, retries - 1) }, 5000)
            }
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            currentController?.unregisterCallback(mediaCallback)
            currentController = null
            updateMediaState(null)
            islandViewRef?.get()?.post { islandViewRef?.get()?.clearMusicState() }
            return
        }

        val bestController = controllers.firstOrNull {
            val state = it.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        } ?: controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PAUSED } ?: controllers.first()

        if (currentController != null && currentController?.packageName == bestController.packageName) {
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
        if (isPlaying) {
            postActivity(LiveActivityModel("sys_media", ActivityType.MEDIA, "Media", "Playing", accentColor = android.graphics.Color.MAGENTA))
        } else {
            removeActivity("sys_media")
        }
        islandViewRef?.get()?.post {
            islandViewRef?.get()?.updatePlayPauseState(isPlaying)
            if (isPlaying && isScreenOn) {
                mainHandler.removeCallbacks(progressUpdater)
                mainHandler.post(progressUpdater)
            } else {
                mainHandler.removeCallbacks(progressUpdater)
            }
        }
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        val island = islandViewRef?.get() ?: return
        if (metadata == null) return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val albumArt = metadata.description?.iconBitmap ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        mediaDuration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        island.post {
            island.updateMusicInfo(title, artist, albumArt)
            val pos = currentController?.playbackState?.position ?: 0L
            island.updateMusicProgress(pos, mediaDuration)
        }
    }
}
