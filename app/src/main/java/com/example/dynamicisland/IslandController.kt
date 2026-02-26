package com.example.dynamicisland

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
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
    private var currentNotificationIntent: PendingIntent? = null
    private var dismissRunnable: Runnable? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private var mediaDuration = 0L

    private var activeLiveActivity: LiveActivityModel? = null

    private var isExpanding = false

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

        // Handle Taps on the Camera Hole / Pill
        view.onSingleTap = {
            val island = islandViewRef?.get()
            if (island != null) {
                when (island.islandState.value) {
                    DynamicIslandView.IslandState.HIDDEN -> showMini()
                    DynamicIslandView.IslandState.TYPE_1_MINI -> expand()
                    DynamicIslandView.IslandState.TYPE_2_MID -> {
                        // Launch intent if available (Music or Notification)
                        if (currentController != null) {
                             // Try to launch music app?
                             // For now, collapse.
                             collapse()
                        } else if (currentNotificationIntent != null) {
                            try {
                                currentNotificationIntent?.send()
                                collapse()
                            } catch (e: Exception) {}
                        } else {
                            collapse()
                        }
                    }
                    DynamicIslandView.IslandState.TYPE_3_MAX -> collapse()
                }
            }
        }

        view.onLongPress = {
            val island = islandViewRef?.get()
            if (island != null && island.islandState.value == DynamicIslandView.IslandState.HIDDEN) {
                // Invisible Punch Hole Long-Pressed!
                showDashboard()
            }
        }
    }

    fun isExpanding(): Boolean = isExpanding

    // --- Live Activities API ---
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
        isExpanding = true
        islandViewRef?.get()?.showDashboard()
    }

    fun expand() {
        isExpanding = true
        islandViewRef?.get()?.expand()

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
                "android.service.notification.NotificationListenerService\$NotificationListenerWrapper",
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
                if (island.islandState.value == DynamicIslandView.IslandState.HIDDEN) {
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
