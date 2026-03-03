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
        try {
            val windowManager = systemUiContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // Critical Fix: Touch Routing & Display Cutout Overlap
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                2024, // TYPE_NAVIGATION_BAR_PANEL (Ensures it draws over lockscreen/status bar)
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or // Allows touches to pass through empty space
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // Prevents OS clipping at the status bar edge
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                // Forces the view to draw strictly around the physical camera punch-hole
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                title = "RedwoodIslandOverlay"
            }

            // The View injection logic (IslandController will bind Compose to this view)
            val islandController = IslandController(systemUiContext)
            val composeView = islandController.createIslandView()

            windowManager.addView(composeView, layoutParams)
            XposedBridge.log("DynamicIsland: Successfully injected overlay.")

        } catch (e: Exception) {
            XposedBridge.log("DynamicIsland: Overlay injection failed - ${e.message}")
        }
    }
}
