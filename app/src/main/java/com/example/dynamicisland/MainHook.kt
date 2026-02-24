package com.example.dynamicisland

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    private var islandInitialized = false
    private var windowManager: WindowManager? = null

    private fun log(msg: String) {
        XposedBridge.log("DynamicIsland: $msg")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return
        log("[LOAD] Hooking SystemUI package: ${lpparam.packageName}")

        // 1. Reliable UI Injection via Application Context
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as Application
                        log("[HOOK] Application.onCreate triggered")

                        // Delay injection to ensure Display Manager is ready
                        Handler(Looper.getMainLooper()).postDelayed({
                            setupIsland(app)
                        }, 3000) // Inject 3 seconds after SystemUI starts
                    }
                }
            )
        } catch (e: Throwable) {
            log("[ERROR] Failed to hook Application.onCreate: $e")
        }

        // 2. Initialize Framework Notification Hooks
        IslandController.hookFrameworkNotifications(lpparam)

        // 3. Resilient Notification Hiding Hook
        try {
            XposedHelpers.findAndHookMethod(
                ViewGroup::class.java,
                "addView",
                View::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val child = param.args[0] as View
                            val className = child.javaClass.name

                            if (IslandController.isExpanding()) {
                                if (className.contains("ExpandableNotificationRow") || className.contains("Notification")) {
                                    child.alpha = 0f
                                    child.visibility = View.GONE
                                    child.translationX = 9999f
                                }
                            }
                        } catch (e: Throwable) { }
                    }
                }
            )
            log("[UI] SUCCESS: Applied resilient notification hiding hook")
        } catch (e: Throwable) {
            log("[ERROR] Generic addView hook failed: $e")
        }
    }

    private fun setupIsland(context: Context) {
        if (islandInitialized) return

        try {
            // A15 requires a WindowContext tied to a Display
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val windowContext = context.createDisplayContext(display).createWindowContext(
                2015, null // TYPE_SECURE_SYSTEM_OVERLAY
            )

            val wm = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            var offsetY = 0
            try {
                offsetY = Settings.System.getInt(context.contentResolver, "redwood_island_y_offset", 0)
            } catch (e: Throwable) {}

            val islandView = DynamicIslandView(windowContext)
            islandView.id = View.generateViewId()

            val params = WindowManager.LayoutParams(
                120, 120, 2015,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.y = offsetY

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }

            islandView.windowManager = wm
            islandView.windowParams = params

            wm.addView(islandView, params)
            islandInitialized = true
            log("[UI] SUCCESS: Added Island Overlay via WindowContext")

            IslandController.init(islandView)
        } catch (e: Throwable) {
            log("[ERROR] setupIsland crashed: $e")
        }
    }
}
