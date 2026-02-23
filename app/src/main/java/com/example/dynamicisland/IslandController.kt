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

    private var islandViewRef: WeakReference<DynamicIslandView>? = null
    private var clockViewRef: WeakReference<View>? = null
    private var isExpanding = false
    private var currentController: MediaController? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var currentNotificationIntent: PendingIntent? = null

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
        val entryClass = try {
            XposedHelpers.findClass("com.android.systemui.statusbar.notification.collection.NotificationEntry", lpparam.classLoader)
        } catch (e: Throwable) { null }

        val headsUpManagerClass = "com.android.systemui.statusbar.policy.HeadsUpManager"

        try {
            if (entryClass != null) {
                XposedHelpers.findAndHookMethod(
                    headsUpManagerClass,
                    lpparam.classLoader,
                    "showNotification",
                    entryClass,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val entry = param.args[0]
                            onHeadsUpShow(entry)
                        }
                    }
                )
            }

             XposedHelpers.findAndHookMethod(
                headsUpManagerClass,
                lpparam.classLoader,
                "removeNotification",
                String::class.java,
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val manager = param.thisObject
                            val hasPinned = XposedHelpers.callMethod(manager, "hasPinnedHeadsUp") as Boolean
                            if (!hasPinned) {
                                onHeadsUpDismiss()
                            }
                        } catch (e: Throwable) {}
                    }
                }
            )
        } catch (e: Throwable) {
             XposedBridge.log("DynamicIsland: [ERROR] Error hooking HeadsUpManager: " + e)
        }
    }

    private fun onHeadsUpShow(entry: Any?) {
        val island = islandViewRef?.get() ?: return
        val clock = clockViewRef?.get()

        var title = "New Notification"
        var text = "Tap to view"
        var smallIcon: Icon? = null

        if (entry != null) {
            try {
                val sbn = XposedHelpers.getObjectField(entry, "mSbn") as StatusBarNotification
                val notification = sbn.notification
                val extras = notification.extras

                title = extras.getString(Notification.EXTRA_TITLE) ?: title
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: text
                smallIcon = notification.smallIcon

                // Update Click Listener
                currentNotificationIntent = notification.contentIntent
                island.setOnClickListener {
                    try {
                        currentNotificationIntent?.send()
                        island.collapse()
                    } catch (e: Throwable) {
                        XposedBridge.log("DynamicIsland: Intent failed: " + e)
                    }
                }

            } catch (e: Throwable) {
                XposedBridge.log("DynamicIsland: [ERROR] Failed to extract content: " + e)
            }
        }

        val fTitle = title
        val fText = text
        val fIcon = smallIcon

        isExpanding = true
        island.post {
            island.updateNotificationInfo(fTitle, fText, fIcon)
            island.expand()
            clock?.animate()?.alpha(0f)?.setDuration(200)?.start()

            island.postDelayed({
                if (isExpanding && currentController?.playbackState?.state != PlaybackState.STATE_PLAYING) {
                    onHeadsUpDismiss()
                }
            }, 5000)
        }
    }

    private fun onHeadsUpDismiss() {
         val island = islandViewRef?.get() ?: return
         val clock = clockViewRef?.get()

         isExpanding = false
         island.post {
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
