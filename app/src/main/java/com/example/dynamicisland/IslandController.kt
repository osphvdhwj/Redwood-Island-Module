package com.example.dynamicisland

import android.view.View
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.ref.WeakReference

object IslandController {

    private var islandViewRef: WeakReference<DynamicIslandView>? = null
    private var clockViewRef: WeakReference<View>? = null

    fun init(view: DynamicIslandView) {
        islandViewRef = WeakReference(view)
    }

    fun setClock(view: View) {
        clockViewRef = WeakReference(view)
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
            } else {
                XposedBridge.log("DynamicIsland: [WARN] NotificationEntry class missing, heads-up hooks disabled.")
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
                            XposedBridge.log("DynamicIsland: [HEADSUP] removeNotification, hasPinned=$hasPinned")

                            if (!hasPinned) {
                                onHeadsUpDismiss()
                            }
                        } catch (e: Throwable) {
                            XposedBridge.log("DynamicIsland: [ERROR] HeadsUp remove hook failed: " + e)
                        }
                    }
                }
            )
            XposedBridge.log("DynamicIsland: [INIT] HeadsUpManager hooked successfully")

        } catch (e: Throwable) {
             XposedBridge.log("DynamicIsland: [ERROR] Error hooking HeadsUpManager: " + e)
        }
    }

    private fun onHeadsUpShow() {
        val island = islandViewRef?.get()
        val clock = clockViewRef?.get()

        if (island == null) {
            XposedBridge.log("DynamicIsland: [WARN] Island view is null in onHeadsUpShow")
            return
        }

        island.post {
            XposedBridge.log("DynamicIsland: [ANIM] Expanding island")
            island.expand()
            clock?.animate()?.alpha(0f)?.setDuration(200)?.start()
        }
    }

    private fun onHeadsUpDismiss() {
         val island = islandViewRef?.get()
         val clock = clockViewRef?.get()

         if (island == null) {
            XposedBridge.log("DynamicIsland: [WARN] Island view is null in onHeadsUpDismiss")
            return
         }

         island.post {
             XposedBridge.log("DynamicIsland: [ANIM] Collapsing island")
             island.collapse()
             clock?.animate()?.alpha(1f)?.setDuration(200)?.start()
         }
    }
}
