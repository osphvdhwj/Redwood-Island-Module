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

                // LayoutParams restored to WRAP_CONTENT to allow dynamic pill resizing
                val layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    2024, // TYPE_NAVIGATION_BAR_PANEL
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                    title = "RedwoodIslandOverlay"
                }

                val islandController = IslandController(systemUiContext)
                val islandView = islandController.createIslandView(windowManager, layoutParams)

                windowManager.addView(islandView, layoutParams)
                XposedBridge.log("RedwoodIsland: Delayed injection successful.")

            } catch (e: Exception) {
                XposedBridge.log("RedwoodIsland ERROR: " + android.util.Log.getStackTraceString(e))
            }
        }, 15000) // 15 Second delay gives you time to disable module if it crashes
    }
}
