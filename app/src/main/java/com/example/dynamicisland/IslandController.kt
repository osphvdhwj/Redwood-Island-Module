package com.example.dynamicisland

import android.view.View
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
            XposedBridge.log("DynamicIsland: Could not find NotificationEntry class")
            return
        }

        val headsUpManagerClass = "com.android.systemui.statusbar.policy.HeadsUpManager"

        try {
            XposedHelpers.findAndHookMethod(
                headsUpManagerClass,
                lpparam.classLoader,
                "showNotification",
                entryClass,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        XposedBridge.log("DynamicIsland: HeadsUp Show")
                        onHeadsUpShow()
                    }
                }
            )

             XposedHelpers.findAndHookMethod(
                headsUpManagerClass,
                lpparam.classLoader,
                "removeNotification",
                String::class.java,
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val manager = param.thisObject
                        val hasPinned = XposedHelpers.callMethod(manager, "hasPinnedHeadsUp") as Boolean
                        XposedBridge.log("DynamicIsland: HeadsUp Remove, hasPinned=$hasPinned")

                        if (!hasPinned) {
                            onHeadsUpDismiss()
                        }
                    }
                }
            )

        } catch (e: Throwable) {
             XposedBridge.log("DynamicIsland: Error hooking HeadsUpManager: " + e)
        }
    }

    private fun onHeadsUpShow() {
        val island = islandViewRef?.get() ?: return
        val clock = clockViewRef?.get()

        island.post {
            island.expand()
            clock?.animate()?.alpha(0f)?.setDuration(200)?.start()
        }
    }

    private fun onHeadsUpDismiss() {
         val island = islandViewRef?.get() ?: return
         val clock = clockViewRef?.get()

         island.post {
             island.collapse()
             clock?.animate()?.alpha(1f)?.setDuration(200)?.start()
         }
    }
}
