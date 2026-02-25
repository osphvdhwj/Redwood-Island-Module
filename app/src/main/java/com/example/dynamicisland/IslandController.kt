package com.example.dynamicisland

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
    }

    fun init(view: DynamicIslandView) {
        islandViewRef = WeakReference(view)
        setupMediaListener(view.context)

        // Handle Gestures
        view.onGestureListener = { action ->
            when (action) {
                DynamicIslandView.GestureAction.SINGLE_TAP -> if (view.isExpanded) collapse() else expand()
                DynamicIslandView.GestureAction.SWIPE_DOWN -> expand()
                DynamicIslandView.GestureAction.SWIPE_UP -> forceHide()
                DynamicIslandView.GestureAction.SWIPE_LEFT -> handleSwipe(isRight = false)
                DynamicIslandView.GestureAction.SWIPE_RIGHT -> handleSwipe(isRight = true)
            }
        }

        // Default click listener for simple interaction if gesture not consumed
        view.setOnClickListener {
             if (view.isExpanded) collapse() else expand()
        }
    }

    fun isExpanding(): Boolean = isExpanding

    private fun handleSwipe(isRight: Boolean) {
        // Example logic: Skip tracks if media is playing, dismiss notification if showing
        if (currentController?.playbackState?.state == PlaybackState.STATE_PLAYING) {
            if (isRight) currentController?.transportControls?.skipToNext()
            else currentController?.transportControls?.skipToPrevious()
        } else {
            // Dismiss current notification state
            collapse()
        }
    }

    fun forceHide() {
        val island = islandViewRef?.get() ?: return
        collapse()
        island.setContextGlow(null) // Remove border tint
        island.animate().translationY(-300f).setDuration(300).start() // Slide off screen
    }

    // --- Live Activities API ---
    fun postLiveActivity(activity: LiveActivityModel) {
        activeLiveActivity = activity
        val island = islandViewRef?.get() ?: return

        island.post {
            island.updateLiveActivity(activity.title, activity.dataText, activity.progress, activity.accentColor)

            // ONLY expand if not already expanded.
            // This prevents the spring animation from restarting every second.
            if (!island.isExpanded) {
                expand()
            }
        }
    }

    fun removeLiveActivity(id: String) {
        if (activeLiveActivity?.id == id) {
            activeLiveActivity = null
            collapse()
        }
    }

    fun expand() {
        isExpanding = true
        islandViewRef?.get()?.let {
            it.animate().translationY(0f).setDuration(0).start()
            it.expand()
        }
    }

    fun collapse() {
        isExpanding = false
        islandViewRef?.get()?.collapse()
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

        // Convert Icon to Bitmap for Palette extraction
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
            island.setContextGlow(bitmap) // Apply app-specific color
            island.updateNotificationInfo(title, text, icon)
            expand()

            // Auto-collapse after 4 seconds unless it's a Live Activity
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
        if (controllers.isNullOrEmpty()) return
        currentController?.unregisterCallback(mediaCallback)
        currentController = controllers.first()
        currentController?.registerCallback(mediaCallback)

        updateMediaState(currentController?.playbackState)
        updateMetadata(currentController?.metadata)
    }

    private fun updateMediaState(state: PlaybackState?) {
        if (state == null) return
        val isPlaying = state.state == PlaybackState.STATE_PLAYING
        val island = islandViewRef?.get() ?: return

        island.post {
            island.updatePlayPauseState(isPlaying)
            if (isPlaying) {
                if (!island.isExpanded) {
                    expand()
                }
            } else {
                 island.postDelayed({
                    if (currentController?.playbackState?.state != PlaybackState.STATE_PLAYING && activeLiveActivity == null) {
                        collapse()
                    }
                }, 2000)
            }
        }
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        if (metadata == null) return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                       ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val island = islandViewRef?.get() ?: return
        island.post {
            island.updateMusicInfo(title, artist, albumArt)
        }
    }
}
