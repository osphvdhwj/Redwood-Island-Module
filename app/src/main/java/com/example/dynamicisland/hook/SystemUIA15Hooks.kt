package com.example.dynamicisland.hook

import android.content.Context
import android.os.Bundle
import com.example.dynamicisland.shared.ipc.BrainRelay
import com.example.dynamicisland.shared.util.XposedExtensions
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 🛰️ SYSTEMUI SATELLITE HOOKS
 * 
 * Injects into com.android.systemui to act as a sensor.
 * Forwards system events (Notifications, Media) to the Redwood Core App.
 */
object SystemUIA15Hooks {
    private const val TAG = "Redwood-SystemUI"

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return
        
        XposedBridge.log("$TAG: Initializing SystemUI Satellite...")

        hookStatusBarStart(lpparam)
        hookNotifPipeline(lpparam)
        hookMediaPipeline(lpparam)
        hookHardwareControllers(lpparam)
    }

    private fun hookStatusBarStart(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedExtensions.hookMethodIfExists(
            "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
            lpparam.classLoader,
            "start",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val context = XposedExtensions.getObjectFieldSafe(param.thisObject, "mContext") as? Context ?: return
                    BrainRelay.dispatch(context, "SYSTEMUI_READY")
                }
            }
        )
    }

    private fun hookNotifPipeline(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cls = "com.android.systemui.statusbar.notification.collection.NotifCollection"
            XposedHelpers.findAndHookMethod(cls, lpparam.classLoader, "dispatchOnEntryAdded",
                "com.android.systemui.statusbar.notification.collection.NotificationEntry",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val entry = param.args[0]
                        val sbn = XposedHelpers.callMethod(entry, "getSbn") as android.service.notification.StatusBarNotification
                        val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                        
                        val bundle = Bundle().apply {
                            putString("pkg", sbn.packageName)
                            putParcelable("notif", sbn.notification)
                        }
                        BrainRelay.dispatch(context, "NOTIFICATION_CAUGHT", bundle)
                    }
                })
        } catch (_: Throwable) {}
    }

    private fun hookMediaPipeline(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cls = "com.android.systemui.media.controls.domain.pipeline.LegacyMediaDataManagerImpl"
            XposedHelpers.findAndHookMethod(cls, lpparam.classLoader, "onMediaDataLoaded",
                String::class.java, String::class.java, 
                "com.android.systemui.media.controls.models.player.MediaData",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val data = param.args[2] ?: return
                        val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                        
                        val bundle = Bundle().apply {
                            putString("pkg", XposedHelpers.getObjectField(data, "packageName") as String)
                            putString("song", (XposedHelpers.getObjectField(data, "song") ?: "").toString())
                            putString("artist", (XposedHelpers.getObjectField(data, "artist") ?: "").toString())
                            putBoolean("isPlaying", XposedHelpers.getBooleanField(data, "isPlaying"))
                        }
                        BrainRelay.dispatch(context, "MEDIA_SYNC", bundle)
                    }
                })
        } catch (_: Throwable) {}
    }

    private fun hookHardwareControllers(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Implementation for Flashlight, Volume, etc. forwarding
    }
}
