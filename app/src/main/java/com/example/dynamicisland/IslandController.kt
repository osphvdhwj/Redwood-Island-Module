package com.example.dynamicisland

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
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

    // Callbacks need to be held as strong references?
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

            // Register listener for active sessions
            // Requires permissions usually, but SystemUI has them
            val componentName = ComponentName(context, "com.android.systemui.SystemUIService")
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, componentName)

            // Initial check
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            updateActiveController(controllers)

            XposedBridge.log("DynamicIsland: [MEDIA] Listener setup complete")
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [ERROR] Media setup failed: " + e)
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) return

        // Unregister old
        currentController?.unregisterCallback(mediaCallback)

        // Pick first active one
        currentController = controllers.first()
        currentController?.registerCallback(mediaCallback)

        XposedBridge.log("DynamicIsland: [MEDIA] Active controller: " + currentController?.packageName)

        // Initial state update
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
                    XposedBridge.log("DynamicIsland: [MEDIA] Playing -> Expand")
                    island.expand()
                }
                island.showMusicVisualizer(true)
            } else {
                XposedBridge.log("DynamicIsland: [MEDIA] Paused -> Collapse (delayed)")
                island.showMusicVisualizer(false)
                // Collapse after 2s delay if paused
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
        // Attempt to find NotificationEntry class
        val entryClass = try {
            XposedHelpers.findClass("com.android.systemui.statusbar.notification.collection.NotificationEntry", lpparam.classLoader)
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [WARN] Could not find NotificationEntry class")
            null
        }

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
                            XposedBridge.log("DynamicIsland: [HEADSUP] showNotification called")
                            onHeadsUpShow()
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
                        } catch (e: Throwable) {
                             // Ignore
                        }
                    }
                }
            )

        } catch (e: Throwable) {
             XposedBridge.log("DynamicIsland: [ERROR] Error hooking HeadsUpManager: " + e)
        }
    }

    private fun onHeadsUpShow() {
        val island = islandViewRef?.get() ?: return
        val clock = clockViewRef?.get()

        isExpanding = true
        island.post {
            island.expand()
            clock?.animate()?.alpha(0f)?.setDuration(200)?.start()
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
         XposedBridge.log("DynamicIsland: [TEST] Triggering Expand Animation")
         island.post {
             island.expand()
             clock?.animate()?.alpha(0f)?.setDuration(200)?.start()

             // Auto collapse after 3 seconds
             island.postDelayed({
                 island.collapse()
                 clock?.animate()?.alpha(1f)?.setDuration(200)?.start()
                 isExpanding = false
             }, 3000)
         }
    }
}
