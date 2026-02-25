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
            // Re-evaluate priority when state changes
            val componentName = ComponentName(islandViewRef?.get()?.context ?: return, "com.android.systemui.SystemUIService")
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            updateActiveController(controllers)
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMetadata(metadata)
        }
        override fun onSessionDestroyed() {
            // Immediately check for other sessions before giving up
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

        // Handle Gestures
        view.onGestureListener = { action ->
            when (action) {
                DynamicIslandView.GestureAction.SINGLE_TAP -> {
                    // If playing, maybe open the app?
                    // For now, toggle expand/collapse as default behavior
                    if (view.isExpanded) collapse() else expand()

                    // Or launch pending intent if available
                    if (view.isExpanded && currentController != null) {
                        try {
                            currentController?.sessionActivity?.send()
                        } catch (e: Exception) {}
                    }
                }
                DynamicIslandView.GestureAction.SWIPE_DOWN -> expand()
                DynamicIslandView.GestureAction.SWIPE_UP -> forceHide()
                DynamicIslandView.GestureAction.SWIPE_LEFT -> handleSwipe(isRight = false)
                DynamicIslandView.GestureAction.SWIPE_RIGHT -> handleSwipe(isRight = true)
            }
        }

        // Default click listener (fallback)
        view.setOnClickListener {
             if (view.isExpanded) collapse() else expand()
        }
    }

    fun isExpanding(): Boolean = isExpanding

    private fun handleSwipe(isRight: Boolean) {
        // Skip tracks if media is playing
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
        if (controllers == null) return // Don't return if empty, we might need to clear

        // If list is empty, we must clear current controller
        if (controllers.isEmpty()) {
            currentController?.unregisterCallback(mediaCallback)
            currentController = null
            // Trigger update to clear UI?
            updateMediaState(null)
            return
        }

        // Logic to pick the best controller:
        // 1. First one that is actively PLAYING or BUFFERING
        // 2. If none, first one that is PAUSED
        // 3. Fallback to first in list
        val bestController = controllers.firstOrNull {
            val state = it.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        } ?: controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PAUSED
        } ?: controllers.first()

        // If we are already tracking this controller, just ensure state is current
        // (Use packageName/Tag to compare as objects might be different instances)
        if (currentController != null &&
            currentController?.packageName == bestController.packageName) {

            // Just ensure callback is registered (it should be) and update state
            updateMediaState(bestController.playbackState)
            updateMetadata(bestController.metadata)
            return
        }

        // Switching controller
        currentController?.unregisterCallback(mediaCallback)
        currentController = bestController
        currentController?.registerCallback(mediaCallback)

        updateMediaState(currentController?.playbackState)
        updateMetadata(currentController?.metadata)
    }

    private fun updateMediaState(state: PlaybackState?) {
        // If state is null, assume stopped/none
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING || state?.state == PlaybackState.STATE_BUFFERING

        val island = islandViewRef?.get() ?: return

        island.post {
            island.updatePlayPauseState(isPlaying)
            if (isPlaying) {
                if (!island.isExpanded) {
                    expand()
                }
            } else {
                 // Check if actually paused or stopped
                 island.postDelayed({
                    // Re-check global state
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
        if (metadata == null) {
            // Optional: clear info?
            return
        }
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                       ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        island.post {
            island.updateMusicInfo(title, artist, albumArt)
        }
    }
}
