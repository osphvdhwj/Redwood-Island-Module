package com.example.dynamicisland

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        try {
            // Hook SystemUI's creation to inject our WindowManager overlay
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.SystemUIApplication",
                lpparam.classLoader,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.thisObject as Context
                        injectDynamicIsland(context)
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("DynamicIsland: Failed to hook SystemUI - ${e.message}")
        }
    }

    private fun injectDynamicIsland(systemUiContext: Context) {
        // Wrap in a Handler to delay execution by 15 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                XposedBridge.log("RedwoodIsland: Starting delayed injection...")
                val windowManager = systemUiContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                // Simplified, ultra-safe LayoutParams for debugging
                val layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    2024,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = android.view.Gravity.TOP
                    title = "RedwoodIslandOverlay"
                }

                val islandController = IslandController(systemUiContext)
                val composeView = islandController.createIslandView()

                windowManager.addView(composeView, layoutParams)
                XposedBridge.log("RedwoodIsland: Delayed injection successful.")

            } catch (e: Exception) {
                XposedBridge.log("RedwoodIsland ERROR: " + android.util.Log.getStackTraceString(e))
            }
        }, 15000) // 15 Second delay gives you time to disable module if it crashes
    }
}
