package com.example.dynamicisland

import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.ref.WeakReference
import android.provider.Settings
import android.content.ContentResolver

object IslandController {

    private var islandViewRef: WeakReference<DynamicIslandView>? = null
    private var clockViewRef: WeakReference<View>? = null
    private var isExpanding = false

    fun init(view: DynamicIslandView) {
        islandViewRef = WeakReference(view)
        checkClockPosition(view.context.contentResolver)
    }

    fun setClock(view: View) {
        clockViewRef = WeakReference(view)
    }

    fun isExpanding(): Boolean {
        return isExpanding
    }

    private fun checkClockPosition(resolver: ContentResolver) {
        try {
            // crDroid clock style: 0=Right, 1=Center, 2=Left
            val style = Settings.System.getInt(resolver, "status_bar_clock_style", 0)
            if (style == 1) {
                XposedBridge.log("DynamicIsland: [WARN] Clock is set to CENTER. This conflicts with Island.")
                // In a real module, we might force change it, but that's intrusive.
                // Just logging for now, or we could permanently hide the clock.
                clockViewRef?.get()?.visibility = View.GONE
            }
        } catch (e: Throwable) {
             // Ignore
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
