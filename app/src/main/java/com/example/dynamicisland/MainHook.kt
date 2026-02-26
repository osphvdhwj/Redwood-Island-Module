package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    private var islandInitialized = false
    private var windowManager: WindowManager? = null
    private var islandView: DynamicIslandView? = null

    private fun log(msg: String) {
        XposedBridge.log("DynamicIsland: $msg")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return
        log("[LOAD] Hooking SystemUI package: ${lpparam.packageName}")

        // 1. UI Injection
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.SystemUIService",
                lpparam.classLoader,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val serviceContext = param.thisObject as Context
                        Handler(Looper.getMainLooper()).postDelayed({
                            setupIsland(serviceContext)
                        }, 2000)
                    }
                }
            )
        } catch (e: Throwable) {
            log("[ERROR] Failed to hook SystemUIService: $e")
        }

        // 2. Initialize Island Framework Hooks (Reads the notification data)
        IslandController.hookFrameworkNotifications(lpparam)

        // 3. NEW: Native Heads-Up Notification Suppression
        try {
            val interruptStateProviderClass = XposedHelpers.findClass(
                "com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProviderImpl",
                lpparam.classLoader
            )

            XposedBridge.hookAllMethods(interruptStateProviderClass, "shouldHeadsUp", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // Suppress the system heads-up entirely so the Island can take over
                    param.result = false
                }
            })
            log("[UI] SUCCESS: Native System Heads-Up Notifications Suppressed")
        } catch (e: Throwable) {
            log("[WARN] Native HUN suppression hook failed (might be different on this crDroid version): $e")
        }
    }

    @SuppressLint("WrongConstant")
    private fun setupIsland(context: Context) {
        if (islandInitialized) return

        try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            // Use TYPE_STATUS_BAR_SUB_PANEL (2017)
            val windowContext = context.createDisplayContext(display).createWindowContext(2017, null)

            val wm = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            islandView = DynamicIslandView(windowContext)
            islandView!!.id = View.generateViewId()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                2017,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.y = 0 // We will handle cutout snapping dynamically now
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

            islandView!!.windowManager = wm
            islandView!!.windowParams = params

            wm.addView(islandView, params)
            islandInitialized = true
            IslandController.init(islandView!!)
        } catch (e: Throwable) {
            log("[ERROR] setupIsland crashed: $e")
        }
    }
}
