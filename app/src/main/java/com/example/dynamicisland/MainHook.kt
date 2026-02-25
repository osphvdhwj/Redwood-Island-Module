package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
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
    private var islandView: DynamicIslandView? = null

    private fun log(msg: String) {
        XposedBridge.log("DynamicIsland: $msg")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return
        log("[LOAD] Hooking SystemUI package: ${lpparam.packageName}")

        // 1. Reliable UI Injection via SystemUIService (Standard for A12+)
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.SystemUIService",
                lpparam.classLoader,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val serviceContext = param.thisObject as Context
                        log("[HOOK] SystemUIService.onCreate triggered")

                        // Register Broadcast Receiver for settings reload
                        try {
                            val receiver = object : BroadcastReceiver() {
                                override fun onReceive(context: Context?, intent: Intent?) {
                                    if (intent?.action == "com.example.dynamicisland.RELOAD_SETTINGS") {
                                        log("[SETTINGS] Reload requested")
                                        reloadSettings(serviceContext)
                                    }
                                }
                            }

                            val filter = IntentFilter("com.example.dynamicisland.RELOAD_SETTINGS")
                            // Android 14+ requires explicit export flag.
                            // Using Context.RECEIVER_EXPORTED (0x2) for inter-app communication.
                            serviceContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                        } catch (e: Throwable) {
                            log("[WARN] Failed to register settings receiver: $e")
                        }

                        // Use a layout listener or just a small delay to ensure display is ready.
                        // 500ms is usually enough for boot on modern devices like Poco X5 Pro.
                        Handler(Looper.getMainLooper()).postDelayed({
                            setupIsland(serviceContext)
                        }, 500)
                    }
                }
            )
        } catch (e: Throwable) {
            log("[ERROR] Failed to hook SystemUIService: $e")
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

    @SuppressLint("WrongConstant")
    private fun setupIsland(context: Context) {
        if (islandInitialized) return

        try {
            // Android 15 requires a WindowContext tied to a Display
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

            // Use TYPE_NAVIGATION_BAR_PANEL (2024) to allow touch interaction
            // and correct Z-ordering above most system UI but below critical overlays.
            val windowContext = context.createDisplayContext(display).createWindowContext(
                2024, null
            )

            val wm = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            // Initial load of settings
            var offsetY = 0
            try {
                // Use XSharedPreferences to read from the app's prefs file
                val prefs = de.robv.android.xposed.XSharedPreferences("com.example.dynamicisland", "dynamic_island_prefs")
                prefs.makeWorldReadable() // Try to ensure readability
                offsetY = prefs.getInt("offset_y", 0)
            } catch (e: Throwable) {
                // Fallback or ignore
            }

            islandView = DynamicIslandView(windowContext)
            islandView!!.id = View.generateViewId()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, // Allow dynamic width
                WindowManager.LayoutParams.WRAP_CONTENT,
                2024, // TYPE_NAVIGATION_BAR_PANEL
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.y = offsetY

            // Always layout in display cutout mode for Android 15 (P+)
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

            islandView!!.windowManager = wm
            islandView!!.windowParams = params

            wm.addView(islandView, params)
            islandInitialized = true
            log("[UI] SUCCESS: Added Island Overlay via WindowContext")

            IslandController.init(islandView!!)
        } catch (e: Throwable) {
            log("[ERROR] setupIsland crashed: $e")
        }
    }

    private fun reloadSettings(context: Context) {
        try {
            val prefs = de.robv.android.xposed.XSharedPreferences("com.example.dynamicisland", "dynamic_island_prefs")
            prefs.reload()
            val offsetY = prefs.getInt("offset_y", 0)

            if (islandView != null && windowManager != null) {
                val params = islandView!!.windowParams
                if (params != null) {
                    params.y = offsetY
                    windowManager!!.updateViewLayout(islandView, params)
                    log("[SETTINGS] Updated Y offset to $offsetY")
                }
            }
        } catch (e: Throwable) {
            log("[ERROR] Failed to reload settings: $e")
        }
    }
}
