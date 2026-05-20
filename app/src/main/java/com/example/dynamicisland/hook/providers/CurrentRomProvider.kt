package com.example.dynamicisland.hook.providers

import android.app.Notification
import android.content.ClipData
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import com.example.dynamicisland.hook.SystemEventListener
import com.example.dynamicisland.hook.SystemEventProvider
import com.example.dynamicisland.hook.IslandHookEngine

/**
 * Provider specific to the currently running ROM.
 * Implements robust Xposed hooks targeting core SystemUI components.
 */
class CurrentRomProvider : SystemEventProvider {
    private var listener: SystemEventListener? = null
    private val TAG = "DynamicIsland-CurrentRom"

    override fun initHooks(classLoader: ClassLoader) {
        hookNotifications(classLoader)
        hookMediaStates(classLoader)
        hookClipboardManager(classLoader)
    }

    override fun setSystemEventListener(listener: SystemEventListener) {
        this.listener = listener
    }

    private fun hookClipboardManager(classLoader: ClassLoader) {
        // Intercepts anytime text is copied to the clipboard across the entire system
        IslandHookEngine.hookAllMethodsByName(
            "android.content.ClipboardManager",
            classLoader,
            "setPrimaryClip",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val clipData = param.args.firstOrNull() as? ClipData
                        if (clipData != null && clipData.itemCount > 0) {
                            Log.d(TAG, "Clipboard update intercepted via Xposed.")
                            
                            // Route this event back to our app's listener bridge safely
                            try {
                                listener?.javaClass?.getMethod("onClipboardChanged")?.invoke(listener)
                            } catch (e: Exception) {
                                Log.w(TAG, "Listener missing onClipboardChanged bridge, skipping.")
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in ClipboardManager hook payload", e)
                    }
                }
            }
        )
    }

    private fun hookNotifications(classLoader: ClassLoader) {
        IslandHookEngine.hookAllMethodsByName(
            "com.android.systemui.statusbar.notification.NotificationEntryManager",
            classLoader,
            "addNotification",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        extractAndRouteNotification(param)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in NotificationEntryManager hook payload", e)
                    }
                }
            }
        )

        IslandHookEngine.hookAllMethodsByName(
            "com.android.systemui.statusbar.notification.collection.NotifCollection",
            classLoader,
            "onNotificationPosted",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        extractAndRouteNotification(param)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in NotifCollection hook payload", e)
                    }
                }
            }
        )
    }

    private fun extractAndRouteNotification(param: XC_MethodHook.MethodHookParam) {
        val sbn = param.args.firstOrNull { it?.javaClass?.simpleName == "StatusBarNotification" }
        if (sbn != null) {
            val pkg = sbn.javaClass.getMethod("getPackageName").invoke(sbn) as? String ?: ""
            val notif = sbn.javaClass.getMethod("getNotification").invoke(sbn) as? Notification
            val extras = notif?.extras
            
            val title = extras?.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            
            listener?.onNotification(title, text, pkg)
        }
    }

    private fun hookMediaStates(classLoader: ClassLoader) {
        IslandHookEngine.hookAllMethodsByName(
            "com.android.systemui.media.MediaDataManager",
            classLoader,
            "onMediaDataLoaded",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        listener?.onMediaPlay()
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in MediaDataManager hook payload", e)
                    }
                }
            }
        )

        IslandHookEngine.hookAllMethodsByName(
            "com.android.systemui.statusbar.NotificationMediaManager",
            classLoader,
            "onPlaybackStateChanged",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        listener?.onMediaPlay()
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in NotificationMediaManager hook payload", e)
                    }
                }
            }
        )
    }
}
