package com.example.dynamicisland

import android.animation.ValueAnimator
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.ref.WeakReference

object IslandController {

    private const val ACTION_POST = "com.example.dynamicisland.ACTION_POST"
    private const val ACTION_REMOVE = "com.example.dynamicisland.ACTION_REMOVE"
    private const val PERMISSION_TRIGGER = "com.example.dynamicisland.PERMISSION_TRIGGER"

    private var islandViewRef: WeakReference<DynamicIslandView>? = null
    private var clockViewRef: WeakReference<View>? = null
    private var statusIconsRef: WeakReference<View>? = null
    private var isExpanding = false
    private var currentController: MediaController? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var currentNotificationIntent: PendingIntent? = null
    private var dismissRunnable: Runnable? = null

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

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                ACTION_POST -> {
                    val title = intent.getStringExtra("title")
                    val text = intent.getStringExtra("text")
                    val icon = intent.getParcelableExtra<Icon>("icon")
                    val contentIntent = intent.getParcelableExtra<PendingIntent>("content_intent")
                    onNotificationShow(title, text, icon, contentIntent)
                }
                ACTION_REMOVE -> {
                    onNotificationDismiss()
                }
            }
        }
    }

    fun init(view: DynamicIslandView) {
        islandViewRef = WeakReference(view)
        setupMediaListener(view.context)
        setupReceiver(view.context)

        // Default click listener (do nothing or collapse)
        view.setOnClickListener {
            collapse()
        }
    }

    fun setClock(view: View) {
        clockViewRef = WeakReference(view)
    }

    fun setStatusIcons(view: View) {
        statusIconsRef = WeakReference(view)
    }

    fun isExpanding(): Boolean {
        return isExpanding
    }

    fun expand() {
        val island = islandViewRef?.get() ?: return
        val clock = clockViewRef?.get()
        val statusIcons = statusIconsRef?.get()

        isExpanding = true

        island.expand()

        // Animate Clock (Fade Out)
        clock?.animate()?.alpha(0f)?.setDuration(200)?.start()

        // Animate Status Icons (Fade Out instead of Translation)
        statusIcons?.animate()?.alpha(0f)?.setDuration(200)?.start()
    }

    fun collapse() {
        val island = islandViewRef?.get() ?: return
        val clock = clockViewRef?.get()
        val statusIcons = statusIconsRef?.get()

        isExpanding = false

        island.collapse()

        // Animate Clock (Fade In)
        clock?.animate()?.alpha(1f)?.setDuration(200)?.start()

        // Animate Status Icons (Fade In)
        statusIcons?.animate()?.alpha(1f)?.setDuration(200)?.start()
    }

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

    private fun setupReceiver(context: Context) {
        try {
            val filter = IntentFilter().apply {
                addAction(ACTION_POST)
                addAction(ACTION_REMOVE)
            }
            context.registerReceiver(notificationReceiver, filter, PERMISSION_TRIGGER, null, Context.RECEIVER_EXPORTED)
            XposedBridge.log("DynamicIsland: [INFO] Receiver registered successfully")
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [ERROR] Receiver registration failed: " + e)
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) return
        currentController?.unregisterCallback(mediaCallback)
        currentController = controllers.first()
        currentController?.registerCallback(mediaCallback)

        // Update click listener for media
        val island = islandViewRef?.get()
        island?.setOnClickListener {
            try {
                currentController?.sessionActivity?.send()
            } catch (e: Throwable) {
                // Ignore
            }
        }

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
                island.showMusicVisualizer(true)
            } else {
                island.showMusicVisualizer(false)
                island.postDelayed({
                    if (currentController?.playbackState?.state != PlaybackState.STATE_PLAYING) {
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

    fun hookFrameworkNotifications(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook the framework's IPC wrapper. This CANNOT be obfuscated by custom ROMs!
            val wrapperClass = XposedHelpers.findClass(
                "android.service.notification.INotificationListener",
                lpparam.classLoader
            )

            XposedBridge.hookAllMethods(
                wrapperClass,
                "onNotificationPosted",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            // param.args[0] is an IStatusBarNotificationHolder in the framework
                            val holder = param.args[0]
                            val sbn = XposedHelpers.callMethod(holder, "get") as android.service.notification.StatusBarNotification
                            val notification = sbn.notification

                            // Filter out ongoing background services and group summaries
                            if ((notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) == 0) {
                                val title = notification.extras.getString(android.app.Notification.EXTRA_TITLE)
                                val text = notification.extras.getString(android.app.Notification.EXTRA_TEXT)
                                val icon = notification.getLargeIcon() ?: notification.getSmallIcon()

                                XposedBridge.log("DynamicIsland: [NOTIF] Caught via Framework IPC: $title")
                                onNotificationShow(title, text, icon, notification.contentIntent)
                            }
                        } catch (e: Throwable) {
                            XposedBridge.log("DynamicIsland: [ERROR] IPC extraction failed: $e")
                        }
                    }
                }
            )
            XposedBridge.log("DynamicIsland: [SUCCESS] Framework IPC Notification hook applied")
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [FATAL] Framework IPC hook failed: $e")
        }
    }

    private fun onNotificationShow(title: String?, text: String?, icon: Icon?, contentIntent: PendingIntent?) {
        val island = islandViewRef?.get() ?: return

        currentNotificationIntent = contentIntent

        // Interactivity: Ensure click triggers the intent
        island.setOnClickListener {
            try {
                dismissRunnable?.let { island.removeCallbacks(it) }

                currentNotificationIntent?.send()
                collapse()

                isExpanding = false
            } catch (e: Throwable) {
                XposedBridge.log("DynamicIsland: Intent failed: " + e)
            }
        }

        island.post {
            island.updateNotificationInfo(title ?: "Notification", text ?: "Tap to view", icon)
            expand()

            dismissRunnable?.let { island.removeCallbacks(it) }
            dismissRunnable = Runnable {
                if (isExpanding && currentController?.playbackState?.state != PlaybackState.STATE_PLAYING) {
                    onNotificationDismiss()
                }
            }
            island.postDelayed(dismissRunnable, 4000)
        }
    }

    private fun onNotificationDismiss() {
         val island = islandViewRef?.get() ?: return
         island.post {
             dismissRunnable?.let { island.removeCallbacks(it) }
             collapse()
         }
    }

    fun testExpand() {
         val island = islandViewRef?.get() ?: return

         island.post {
             island.updateNotificationInfo("Test Notification", "This is a test message.", null)
             expand()

             island.postDelayed({
                 collapse()
             }, 3000)
         }
    }
}
