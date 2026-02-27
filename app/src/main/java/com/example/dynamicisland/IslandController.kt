package com.example.dynamicisland

import android.app.PendingIntent
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

object IslandController {
    private var islandViewRef: WeakReference<DynamicIslandView>? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var currentController: MediaController? = null
    private var dismissRunnable: Runnable? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private var mediaDuration = 0L

    private var activeLiveActivity: LiveActivityModel? = null

    private var isExpanding = false
    private var isScreenOn = true

    private fun log(msg: String) {
        XposedBridge.log("DynamicIsland: $msg")
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            log("[MEDIA] Playback state changed: ${state?.state}")
            updateMediaState(state)
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            log("[MEDIA] Metadata changed: ${metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}")
            updateMetadata(metadata)
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        log("[MEDIA] Active sessions changed. Count: ${controllers?.size}")
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
                progressHandler.postDelayed(this, 1000)
            }
        }
    }

    fun init(view: DynamicIslandView) {
        islandViewRef = WeakReference(view)
        setupMediaListener(view.context)

        BatteryPlugin.onBatteryChanged = { level, isCharging, color ->
             val island = islandViewRef?.get()
             if (island != null) {
                 island.post {
                     island.updateChargingInfo(level, isCharging, color)
                     if (isCharging && island.islandState.value == DynamicIslandView.IslandState.HIDDEN) {
                         expandMid()
                         progressHandler.postDelayed({
                             if (island.islandState.value == DynamicIslandView.IslandState.TYPE_2_MID) { collapse() }
                         }, 3000)
                     } else if (!isCharging) {
                         if (island.islandState.value == DynamicIslandView.IslandState.TYPE_2_MID && activeLiveActivity == null && currentController == null) {
                             collapse()
                         }
                     }
                 }
             }
        }
        BatteryPlugin.start(view.context)

        // --- GESTURE NAVIGATION LOGIC ---
        view.onSingleTap = {
            log("[GESTURE] Single Tap")
            val island = islandViewRef?.get()
            if (island != null) {
                when (island.islandState.value) {
                    DynamicIslandView.IslandState.HIDDEN -> showMini()
                    DynamicIslandView.IslandState.TYPE_1_MINI -> expandMid()
                    DynamicIslandView.IslandState.TYPE_2_MID -> collapse()
                    DynamicIslandView.IslandState.TYPE_3_MAX -> collapse()
                }
            }
        }

        view.onDoubleTap = {
            log("[GESTURE] Double Tap")
            val island = islandViewRef?.get()
            if (island != null) {
                when (island.islandState.value) {
                    DynamicIslandView.IslandState.HIDDEN -> expandMid()
                    else -> collapse()
                }
            }
        }

        view.onLongPress = {
            log("[GESTURE] Long Press")
             val island = islandViewRef?.get()
             if (island != null) {
                 when (island.islandState.value) {
                     DynamicIslandView.IslandState.HIDDEN -> showDashboard()
                     DynamicIslandView.IslandState.TYPE_1_MINI -> showDashboard()
                     else -> {}
                 }
             }
        }

        // Media specific click listeners mapping
        view.onPrevClick = { currentController?.transportControls?.skipToPrevious() }
        view.onNextClick = { currentController?.transportControls?.skipToNext() }
        view.onPlayPauseClick = {
            val state = currentController?.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                currentController?.transportControls?.pause()
            } else {
                currentController?.transportControls?.play()
            }
        }
        view.onSeekTo = { pos -> currentController?.transportControls?.seekTo(pos) }
        view.onCloseClick = { forceHide() }

        // Swipe gestures
        view.onSwipeLeft = { currentController?.transportControls?.skipToNext() }
        view.onSwipeRight = { currentController?.transportControls?.skipToPrevious() }
        view.onSwipeUp = { collapse() }
    }

    fun onScreenStateChanged(isOn: Boolean) {
        isScreenOn = isOn
        islandViewRef?.get()?.post { islandViewRef?.get()?.updateScreenState(isOn) }

        if (isOn) {
            val state = currentController?.playbackState
            if (state?.state == PlaybackState.STATE_PLAYING) {
                progressHandler.removeCallbacks(progressUpdater)
                progressHandler.post(progressUpdater)
            }
        } else {
            progressHandler.removeCallbacks(progressUpdater)
        }
    }

    fun postLiveActivity(activity: LiveActivityModel) {
        activeLiveActivity = activity
        val island = islandViewRef?.get() ?: return
        island.post {
            island.updateLiveActivity(activity.title, activity.dataText, activity.progress, activity.accentColor)
            if (island.islandState.value == DynamicIslandView.IslandState.HIDDEN) showMini()
        }
    }

    fun showDashboard() {
        if (!isScreenOn) return
        isExpanding = true
        islandViewRef?.get()?.showDashboard()
    }

    fun expandMid() {
        if (!isScreenOn) return
        isExpanding = true
        islandViewRef?.get()?.expand()
    }

    fun showMini() {
        if (!isScreenOn) return
        isExpanding = false
        islandViewRef?.get()?.showMini()
    }

    fun collapse() {
        isExpanding = false
        islandViewRef?.get()?.hide()
    }

    fun forceHide() {
        isExpanding = false
        islandViewRef?.get()?.hide()
    }

    // --- Hooking Framework ONLY for Clocks/Timers (Removed standard Notifications) ---
    fun hookFrameworkNotifications(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // FIX: Robust string concatenation to avoid Kotlin string template interpolation of '$'
            val className = "android.service.notification.NotificationListenerService" + "$" + "NotificationListenerWrapper"
            val wrapperClass = XposedHelpers.findClass(className, lpparam.classLoader)

            XposedBridge.hookAllMethods(wrapperClass, "onNotificationPosted", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val holder = param.args[0]
                        val sbn = XposedHelpers.callMethod(holder, "get") as android.service.notification.StatusBarNotification

                        // Only intercept if it's a timer/stopwatch Live Activity
                        val liveActivity = ClockInterceptor.inspect(sbn)
                        if (liveActivity != null) {
                            postLiveActivity(liveActivity)
                        }
                    } catch (e: Throwable) {
                         XposedBridge.log("DynamicIsland: [ERROR] Clock IPC extraction failed: " + e)
                    }
                }
            })
            XposedBridge.log("DynamicIsland: [SUCCESS] Clock IPC hook applied")
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [FATAL] Framework IPC hook failed: " + e)
        }
    }

    // --- Media Handling ---
    private fun setupMediaListener(context: Context) {
        try {
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, null)
            val controllers = mediaSessionManager?.getActiveSessions(null)
            log("[MEDIA] Initial controllers found: ${controllers?.size}")
            updateActiveController(controllers)
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [ERROR] Media setup failed: " + e)
            try {
                val componentName = android.content.ComponentName(context, "com.android.systemui.SystemUIService")
                 mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, componentName)
                 val controllers = mediaSessionManager?.getActiveSessions(componentName)
                 updateActiveController(controllers)
            } catch (e2: Throwable) {
                 XposedBridge.log("DynamicIsland: [FATAL] Media setup fallback failed: " + e2)
            }
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            log("[MEDIA] No active controllers.")
            currentController?.unregisterCallback(mediaCallback)
            currentController = null
            updateMediaState(null)
            islandViewRef?.get()?.post { islandViewRef?.get()?.clearMusicState() }
            return
        }

        val bestController = controllers.firstOrNull {
            val state = it.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        } ?: controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PAUSED
        } ?: controllers.first()

        log("[MEDIA] Selected controller: ${bestController.packageName}")

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
        val island = islandViewRef?.get() ?: return

        island.post {
            island.updatePlayPauseState(isPlaying)
            if (isPlaying) {
                if (isScreenOn) {
                    progressHandler.removeCallbacks(progressUpdater)
                    progressHandler.post(progressUpdater)
                }
            } else {
                 progressHandler.removeCallbacks(progressUpdater)
            }
        }
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        val island = islandViewRef?.get() ?: return
        if (metadata == null) return

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val albumArt = metadata.description?.iconBitmap
                       ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                       ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        mediaDuration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

        island.post {
            island.updateMusicInfo(title, artist, albumArt)
            val pos = currentController?.playbackState?.position ?: 0L
            island.updateMusicProgress(pos, mediaDuration)
        }
    }
}
