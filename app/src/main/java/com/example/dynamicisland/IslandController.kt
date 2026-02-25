package com.example.dynamicisland

import android.app.PendingIntent
import android.content.*
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.*
import android.os.Handler
import android.os.Looper
import android.view.View
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.ref.WeakReference

object IslandController {
    private var islandViewRef: WeakReference<DynamicIslandView>? = null
    private var currentController: MediaController? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(view: DynamicIslandView) {
        islandViewRef = WeakReference(view)
        setupMediaListener(view.context)
        view.setOnClickListener { collapse() }
    }

    private fun setupMediaListener(context: Context) {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(context, "com.android.systemui.SystemUIService")

        manager.addOnActiveSessionsChangedListener({ controllers ->
            val active = controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: controllers?.firstOrNull()
            updateController(active)
        }, component)

        updateController(manager.getActiveSessions(component).firstOrNull())
    }

    private fun updateController(controller: MediaController?) {
        currentController?.unregisterCallback(mediaCallback)
        currentController = controller
        controller?.registerCallback(mediaCallback)
        updateMetadata(controller?.metadata)
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            if (state?.state == PlaybackState.STATE_PLAYING) expand() else {
                mainHandler.postDelayed({ if (currentController?.playbackState?.state != PlaybackState.STATE_PLAYING) collapse() }, 2500)
            }
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) = updateMetadata(metadata)
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        val island = islandViewRef?.get() ?: return
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)

        island.post { island.updateMusicInfo(title, artist, art) }
    }

    fun expand() = islandViewRef?.get()?.expand()
    fun collapse() = islandViewRef?.get()?.collapse()

    // Hook for Android 15 Notification IPC
    fun hookFrameworkNotifications(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val listenerClass = XposedHelpers.findClass("android.service.notification.INotificationListener.Stub", lpparam.classLoader)
            XposedBridge.hookAllMethods(listenerClass, "onNotificationPosted", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val sbn = XposedHelpers.callMethod(param.args[0], "get") as android.service.notification.StatusBarNotification
                        val notif = sbn.notification
                        if ((notif.flags and android.app.Notification.FLAG_ONGOING_EVENT) == 0) {
                            val title = notif.extras.getString(android.app.Notification.EXTRA_TITLE)
                            val text = notif.extras.getString(android.app.Notification.EXTRA_TEXT)
                            onNotificationShow(title, text, notif.getLargeIcon(), notif.contentIntent)
                        }
                    } catch (e: Throwable) {
                        XposedBridge.log("DynamicIsland: Error extracting IPC notif: " + e)
                    }
                }
            })
        } catch (e: Throwable) { XposedBridge.log("Island: Hook Failed ") }
    }

    private fun onNotificationShow(title: String?, text: String?, icon: Icon?, intent: PendingIntent?) {
        val island = islandViewRef?.get() ?: return
        island.post {
            island.updateNotificationInfo(title, text, icon)
            island.expand()
            island.setOnClickListener { try { intent?.send(); collapse() } catch (e: Exception) {} }
            mainHandler.postDelayed({ if (island.isExpanded) collapse() }, 5000)
        }
    }
}
