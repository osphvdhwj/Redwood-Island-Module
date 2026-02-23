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
import android.view.View
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.ref.WeakReference

object IslandController {

    private var islandViewRef: WeakReference<DynamicIslandView>? = null
    private var clockViewRef: WeakReference<View>? = null
    private var isExpanding = false
    private var currentController: MediaController? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var currentNotificationIntent: PendingIntent? = null
    private var dismissRunnable: Runnable? = null

    private const val ACTION_POST = "com.example.dynamicisland.ACTION_POST"
    private const val ACTION_REMOVE = "com.example.dynamicisland.ACTION_REMOVE"
    private const val PERMISSION_TRIGGER = "com.example.dynamicisland.PERMISSION_TRIGGER"

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
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_POST -> {
                    val title = intent.getStringExtra("title")
                    val text = intent.getStringExtra("text")
                    // API 33+ safe way
                    val icon = intent.getParcelableExtra("icon", Icon::class.java)
                    val contentIntent = intent.getParcelableExtra("content_intent", PendingIntent::class.java)
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
            view.collapse()
        }
    }

    fun setClock(view: View) {
        clockViewRef = WeakReference(view)
    }

    fun isExpanding(): Boolean {
        return isExpanding
    }

    private fun setupMediaListener(context: Context) {
        try {
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(context, "com.android.systemui.SystemUIService")
            try {
                mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, componentName)
                val controllers = mediaSessionManager?.getActiveSessions(componentName)
                updateActiveController(controllers)
            } catch (e: SecurityException) {
                 mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, null)
                 val controllers = mediaSessionManager?.getActiveSessions(null)
                 updateActiveController(controllers)
            }
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

        val island = islandViewRef?.get()
        island?.setOnClickListener {
            try {
                currentController?.sessionActivity?.send()
            } catch (e: Throwable) {
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
            if (isPlaying) {
                if (!island.isExpanded) {
                    island.expand()
                }
                island.showMusicVisualizer(true)
            } else {
                island.showMusicVisualizer(false)
                island.postDelayed({
                    if (currentController?.playbackState?.state != PlaybackState.STATE_PLAYING) {
                        island.collapse()
                    }
                }, 2000)
            }
        }
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        if (metadata == null) return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val island = islandViewRef?.get() ?: return
        island.post {
            island.updateMusicInfo(title, artist)
        }
    }

    fun hookHeadsUpManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("DynamicIsland: [INFO] hookHeadsUpManager is deprecated/disabled")
    }

    private fun onNotificationShow(title: String?, text: String?, icon: Icon?, contentIntent: PendingIntent?) {
        val island = islandViewRef?.get() ?: return
        val clock = clockViewRef?.get()

        currentNotificationIntent = contentIntent

        island.setOnClickListener {
            try {
                dismissRunnable?.let { island.removeCallbacks(it) }

                currentNotificationIntent?.send()
                island.collapse()

                isExpanding = false
                clock?.animate()?.alpha(1f)?.setDuration(200)?.start()
            } catch (e: Throwable) {
                XposedBridge.log("DynamicIsland: Intent failed: " + e)
            }
        }

        isExpanding = true
        island.post {
            island.updateNotificationInfo(title ?: "Notification", text ?: "Tap to view", icon)
            island.expand()
            clock?.animate()?.alpha(0f)?.setDuration(200)?.start()

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
         val clock = clockViewRef?.get()

         isExpanding = false
         island.post {
             dismissRunnable?.let { island.removeCallbacks(it) }
             island.collapse()
             clock?.animate()?.alpha(1f)?.setDuration(200)?.start()
         }
    }

    fun testExpand() {
         val island = islandViewRef?.get() ?: return
         val clock = clockViewRef?.get()

         isExpanding = true
         island.post {
             island.updateNotificationInfo("Test Notification", "This is a test message.", null)
             island.expand()
             clock?.animate()?.alpha(0f)?.setDuration(200)?.start()

             island.postDelayed({
                 island.collapse()
                 clock?.animate()?.alpha(1f)?.setDuration(200)?.start()
                 isExpanding = false
             }, 3000)
         }
    }
}
