package com.example.dynamicisland

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
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

    private var isScreenOn = true

    private fun log(msg: String) {
        XposedBridge.log("DynamicIsland: $msg")
    }

    // --- Media Callbacks ---
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

        // Charging Interception
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

        // Gesture Actions
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

    // --- Priority Engine ---
    fun postActivity(activity: LiveActivityModel) {
        activeActivities[activity.id] = activity
        resolveHighestPriority()

        // Auto-dismiss transient notifications (like messages or charging)
        if (activity.isTransient) {
            mainHandler.postDelayed({
                removeActivity(activity.id)
            }, 5000) // 5 seconds display time
        }
    }

    fun removeActivity(id: String) {
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
                    // Auto-expand messages to MID pill so you can read them
                    val targetState = if (highest.type == ActivityType.MESSAGE || highest.type == ActivityType.CALL)
                                      DynamicIslandView.IslandState.TYPE_2_MID
                                      else DynamicIslandView.IslandState.TYPE_1_MINI
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
        islandViewRef?.get()?.setState(DynamicIslandView.IslandState.HIDDEN)
    }

    // --- Framework Interception ---
    fun hookFrameworkNotifications(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val className = "android.service.notification.NotificationListenerService\$NotificationListenerWrapper"
            val wrapperClass = XposedHelpers.findClass(className, lpparam.classLoader)

            XposedBridge.hookAllMethods(wrapperClass, "onNotificationPosted", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val holder = param.args[0]
                        val sbn = XposedHelpers.callMethod(holder, "get") as android.service.notification.StatusBarNotification
                        val notification = sbn.notification
                        val extras = notification.extras
                        val category = notification.category

                        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

                        when (category) {
                            Notification.CATEGORY_ALARM -> {
                                postActivity(LiveActivityModel(sbn.key, ActivityType.ALARM, "ALARM", title, accentColor = android.graphics.Color.RED))
                            }
                            Notification.CATEGORY_CALL -> {
                                postActivity(LiveActivityModel(sbn.key, ActivityType.CALL, "Incoming Call", title, accentColor = android.graphics.Color.GREEN))
                            }
                            Notification.CATEGORY_NAVIGATION -> {
                                postActivity(LiveActivityModel(sbn.key, ActivityType.NAVIGATION, title, text, accentColor = android.graphics.Color.parseColor("#4285F4")))
                            }
                            Notification.CATEGORY_MESSAGE, Notification.CATEGORY_SOCIAL -> {
                                postActivity(LiveActivityModel(sbn.key, ActivityType.MESSAGE, title, text, accentColor = android.graphics.Color.YELLOW, isTransient = true))
                            }
                            else -> {
                                // Fallbacks (Downloads, Timers)
                                val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
                                val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, -1)
                                if (max > 0 && progress >= 0) {
                                    val percent = (progress.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                                    postActivity(LiveActivityModel(sbn.key, ActivityType.DOWNLOAD, if (title.isBlank()) "Downloading" else title, "${(percent * 100).toInt()}%", percent, android.graphics.Color.CYAN))
                                } else if (progress < 0 || max <= 0) {
                                    removeActivity(sbn.key)
                                }
                            }
                        }
                    } catch (e: Throwable) { log("[ERROR] Parse: $e") }
                }
            })

            XposedBridge.hookAllMethods(wrapperClass, "onNotificationRemoved", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val holder = param.args[0]
                        val sbn = XposedHelpers.callMethod(holder, "get") as android.service.notification.StatusBarNotification
                        removeActivity(sbn.key)
                    } catch (e: Throwable) {}
                }
            })
        } catch (e: Throwable) { log("[FATAL] IPC Hook Error: $e") }
    }

    private fun setupMediaListener(context: Context) {
        try {
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(context, "com.android.systemui.statusbar.phone.NotificationListenerWithPlugins")
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, componentName)
            updateActiveController(mediaSessionManager?.getActiveSessions(componentName))
        } catch (e: Exception) {
            log("[ERROR] Failed to setup MediaSessionManager: \$e")
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        val newController = controllers?.firstOrNull {
            val state = it.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        } ?: controllers?.firstOrNull()

        if (currentController != newController) {
            currentController?.unregisterCallback(mediaCallback)
            currentController = newController
            currentController?.registerCallback(mediaCallback)
            updateMetadata(currentController?.metadata)
            updateMediaState(currentController?.playbackState)
        }
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
        if (metadata == null) return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val art: Bitmap? = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        mediaDuration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

        islandViewRef?.get()?.post {
            islandViewRef?.get()?.updateMusicInfo(title, artist, art)
        }
    }
}
