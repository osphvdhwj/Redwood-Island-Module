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

        // --- Gesture Mappings ---
        view.onSingleTap = {
            val island = islandViewRef?.get()
            if (island != null) {
                when (island.islandState.value) {
                    DynamicIslandView.IslandState.HIDDEN -> {
                        // Wake up to mini if music is active, else do nothing
                        if (currentController != null) showMini()
                    }
                    DynamicIslandView.IslandState.TYPE_1_MINI -> showDashboard() // Jump straight to max music player
                    DynamicIslandView.IslandState.TYPE_2_MID -> collapse()
                    DynamicIslandView.IslandState.TYPE_3_MAX -> collapse()
                }
            }
        }

        view.onDoubleTap = { forceHide() }

        view.onLongPress = {
             val island = islandViewRef?.get()
             if (island?.islandState?.value == DynamicIslandView.IslandState.TYPE_1_MINI) {
                 showDashboard() // Also map Long Press to max pill just to be safe
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

        // Swipe gestures (used on mini pill)
        view.onSwipeLeft = { currentController?.transportControls?.skipToNext() }
        view.onSwipeRight = { currentController?.transportControls?.skipToPrevious() }
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
        if (currentController != null) {
            islandViewRef?.get()?.showMini() // Drop down to mini if media exists
        } else {
            islandViewRef?.get()?.hide()
        }
    }

    fun forceHide() {
        isExpanding = false
        islandViewRef?.get()?.hide()
    }

    // --- Hooking Framework ONLY for Clocks/Timers (Removed standard Notifications) ---
    fun hookFrameworkNotifications(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val wrapperClass = XposedHelpers.findClass(
                "android.service.notification.NotificationListenerService\$NotificationListenerWrapper",
                lpparam.classLoader
            )

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
            val componentName = android.content.ComponentName(context, "com.android.systemui.SystemUIService")
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, componentName)
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            updateActiveController(controllers)
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [ERROR] Media setup failed: " + e)
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
        } ?: controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PAUSED
        } ?: controllers.first()

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
                if (island.islandState.value == DynamicIslandView.IslandState.HIDDEN) showMini()
                if (isScreenOn) {
                    progressHandler.removeCallbacks(progressUpdater)
                    progressHandler.post(progressUpdater)
                }
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
