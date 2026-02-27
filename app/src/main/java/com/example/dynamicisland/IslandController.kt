package com.example.dynamicisland

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
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
    private var currentNotificationIntent: PendingIntent? = null
    private var dismissRunnable: Runnable? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private var mediaDuration = 0L

    private var activeLiveActivity: LiveActivityModel? = null

    private var isExpanding = false
    private var isScreenOn = true

    // Media Callback
    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaState(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMetadata(metadata)
        }
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

        // Initialize Battery Plugin
        BatteryPlugin.onBatteryChanged = { level, isCharging, color ->
             val island = islandViewRef?.get()
             if (island != null) {
                 island.post {
                     island.updateChargingInfo(level, isCharging, color)
                     if (isCharging && island.islandState.value == DynamicIslandView.IslandState.HIDDEN) {
                         expand()
                         progressHandler.postDelayed({
                             if (island.islandState.value == DynamicIslandView.IslandState.TYPE_2_MID) {
                                 collapse()
                             }
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

        // --- Gesture & Touch Callbacks ---
        view.onSingleTap = {
            val island = islandViewRef?.get()
            if (island != null) {
                when (island.islandState.value) {
                    DynamicIslandView.IslandState.HIDDEN -> showMini()
                    DynamicIslandView.IslandState.TYPE_1_MINI -> expand()
                    DynamicIslandView.IslandState.TYPE_2_MID -> collapse()
                    DynamicIslandView.IslandState.TYPE_3_MAX -> collapse()
                }
            }
        }

        view.onDoubleTap = {
            val island = islandViewRef?.get()
            if (island != null) {
                if (island.islandState.value == DynamicIslandView.IslandState.HIDDEN) {
                     expand()
                } else {
                    forceHide()
                }
            }
        }

        view.onLongPress = {
             showDashboard()
        }

        view.onPrevClick = {
            currentController?.transportControls?.skipToPrevious()
        }

        view.onNextClick = {
            currentController?.transportControls?.skipToNext()
        }

        view.onPlayPauseClick = {
            val state = currentController?.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                currentController?.transportControls?.pause()
            } else {
                currentController?.transportControls?.play()
            }
        }

        view.onSeekTo = { pos ->
            currentController?.transportControls?.seekTo(pos)
        }

        view.onShuffleClick = {
            // Shuffle not supported on older APIs directly in TransportControls
        }

        view.onLoopClick = {
            // Repeat not supported on older APIs directly in TransportControls
        }

        view.onCloseClick = {
            forceHide()
        }

        view.onSwipeLeft = {
            if (isNotificationShowing(view)) {
                dismissNotification()
            } else {
                currentController?.transportControls?.skipToNext()
            }
        }

        view.onSwipeRight = {
            if (isNotificationShowing(view)) {
                dismissNotification()
            } else {
                currentController?.transportControls?.skipToPrevious()
            }
        }

        // Wire up new swipe up/down if needed, or handle in view
        // The view handles state changes for these directly

        // --- Notification Action Callbacks ---
        view.onActionClick = { actionModel ->
            try {
                actionModel.actionIntent.send()
                collapse()
            } catch (e: Exception) {
                XposedBridge.log("DynamicIsland: Failed to send action: $e")
            }
        }

        view.onReplySend = { actionModel, replyText ->
            val island = islandViewRef?.get()
            if (island != null && actionModel.remoteInputs != null) {
                try {
                    val intent = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    val bundle = Bundle()
                    val remoteInput = actionModel.remoteInputs.firstOrNull()
                    if (remoteInput != null) {
                        bundle.putCharSequence(remoteInput.resultKey, replyText)
                        RemoteInput.addResultsToIntent(actionModel.remoteInputs, intent, bundle)
                        actionModel.actionIntent.send(island.context, 0, intent)
                        collapse()
                    }
                } catch (e: Exception) {
                    XposedBridge.log("DynamicIsland: Failed to send reply: $e")
                }
            }
        }
    }

    private fun isNotificationShowing(view: DynamicIslandView): Boolean {
        return currentController?.playbackState?.state != PlaybackState.STATE_PLAYING
    }

    private fun dismissNotification() {
        currentNotificationIntent = null
        collapse()
    }

    fun isExpanding(): Boolean = isExpanding

    fun onScreenStateChanged(isOn: Boolean) {
        isScreenOn = isOn
        val island = islandViewRef?.get() ?: return

        island.post {
            island.updateScreenState(isOn)
        }

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
            island.updateMiniPillContent(activity.title + ": " + activity.dataText, null, activity.accentColor)

            if (island.islandState.value == DynamicIslandView.IslandState.HIDDEN) {
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
            } else {
                collapse()
            }
        }
    }

    fun showDashboard() {
        if (!isScreenOn) return
        isExpanding = true
        islandViewRef?.get()?.showDashboard()
    }

    fun expand() {
        if (!isScreenOn) return
        isExpanding = true
        islandViewRef?.get()?.expand()

        if (currentController?.playbackState?.state == PlaybackState.STATE_PLAYING) {
             progressHandler.removeCallbacks(progressUpdater)
             progressHandler.post(progressUpdater)
        }
    }

    fun showMini() {
        if (!isScreenOn) return
        isExpanding = false
        islandViewRef?.get()?.showMini()
    }

    fun collapse() {
        isExpanding = false
        islandViewRef?.get()?.collapse()
        progressHandler.removeCallbacks(progressUpdater)
    }

    fun forceHide() {
        isExpanding = false
        islandViewRef?.get()?.hide()
        progressHandler.removeCallbacks(progressUpdater)
    }

    // --- Notification Handling ---
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
                        val notif = sbn.notification

                        val liveActivity = ClockInterceptor.inspect(sbn)
                        if (liveActivity != null) {
                            postLiveActivity(liveActivity)
                            return
                        }

                        if ((notif.flags and android.app.Notification.FLAG_ONGOING_EVENT) == 0) {
                            val title = notif.extras.getString(android.app.Notification.EXTRA_TITLE)
                            val text = notif.extras.getString(android.app.Notification.EXTRA_TEXT)
                            val icon = notif.getLargeIcon() ?: notif.getSmallIcon()

                            val actions = notif.actions
                            val category = notif.category

                            val island = islandViewRef?.get()
                            if (island != null) {
                                onNotificationShow(title, text, icon, notif.contentIntent, category, actions, island.context)
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

    private fun onNotificationShow(
        title: String?,
        text: String?,
        icon: Icon?,
        contentIntent: PendingIntent?,
        category: String?,
        actions: Array<android.app.Notification.Action>?,
        context: Context
    ) {
        if (!isScreenOn) return
        val island = islandViewRef?.get() ?: return

        currentNotificationIntent = contentIntent

        var bitmap: Bitmap? = null
        try {
            val drawable = icon?.loadDrawable(context)
            if (drawable is BitmapDrawable) {
                bitmap = drawable.bitmap
            } else if (drawable != null) {
                bitmap = Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        } catch (e: Exception) {
            XposedBridge.log("DynamicIsland: Failed to load icon: $e")
        }

        val actionModels = actions?.map { action ->
            NotificationActionModel(
                title = action.title.toString(),
                actionIntent = action.actionIntent,
                remoteInputs = action.remoteInputs
            )
        } ?: emptyList()

        island.post {
            island.setContextGlow(bitmap)
            island.updateNotificationInfo(title, text, icon, category, actionModels)
            expand()

            dismissRunnable?.let { island.removeCallbacks(it) }
            dismissRunnable = Runnable {
                if (activeLiveActivity == null && currentController?.playbackState?.state != PlaybackState.STATE_PLAYING) {
                    collapse()
                }
            }
            island.postDelayed(dismissRunnable, 5000)
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
        if (controllers == null) return

        if (controllers.isEmpty()) {
            currentController?.unregisterCallback(mediaCallback)
            currentController = null
            updateMediaState(null)

            // FIX: Explicitly clear view state on UI thread to remove "ghost" media
            islandViewRef?.get()?.post {
                islandViewRef?.get()?.clearMusicState()
            }
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
                if (island.islandState.value == DynamicIslandView.IslandState.HIDDEN) {
                    showMini()
                }
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
                       ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

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
