package com.example.dynamicisland

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    private var islandInitialized = false
    private var windowManager: WindowManager? = null

    private fun log(msg: String) {
        XposedBridge.log("DynamicIsland: " + msg)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        log("[LOAD] Hooking SystemUI package: " + lpparam.packageName)

        // Hook Application.onCreate ONLY to set up early hooks or logging if needed.
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        log("[HOOK] Application.onCreate called")
                        hookStatusBarViews(lpparam.classLoader)
                    }
                }
            )
        } catch (e: Throwable) {
            log("[ERROR] Failed to hook Application.onCreate: " + e)
        }

        IslandController.hookHeadsUpManager(lpparam)

        // NUCLEAR GHOST FIX (Retained for notification hiding)
        try {
            val nsslClass = "com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout"

            XposedHelpers.findAndHookMethod(
                nsslClass,
                lpparam.classLoader,
                "onChildViewAdded",
                View::class.java,
                View::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val child = param.args[1] as View
                            if (child is DynamicIslandView) return

                            if (IslandController.isExpanding()) {
                                if (child.javaClass.name.contains("ExpandableNotificationRow")) {
                                    log("[NUCLEAR] Banishing notification off-screen")
                                    child.translationX = 9999f
                                    child.alpha = 0f
                                    child.scaleX = 0f
                                    child.scaleY = 0f
                                }
                            }
                        } catch (e: Throwable) {
                             // Ignore
                        }
                    }
                }
            )

        } catch (e: Throwable) {
            log("[WARN] Failed to hook StackScrollLayout: " + e)
        }
    }

    private fun setupIsland(context: Context) {
        if (islandInitialized) return

        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            // Read Settings.System
            var offsetY = 0
            try {
                val cr = context.contentResolver
                offsetY = Settings.System.getInt(cr, "redwood_island_y_offset", 0)
            } catch (e: Throwable) {
                // Ignore errors
            }

            val islandView = DynamicIslandView(context)
            islandView.id = View.generateViewId()

            // Configure WindowManager LayoutParams
            // CRITICAL: TYPE_SECURE_SYSTEM_OVERLAY (2015) for Native SystemUI token bypass
            val params = WindowManager.LayoutParams(
                120, // Force initial width for Poco X5 Pro cutout
                120, // Force initial height
                2015, // Native SystemUI Window Type
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )

            // Center Gravity
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.y = offsetY // Use 'y' for top margin with Gravity.TOP

            // Handle Display Cutout Mode (Important for Overlay)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }

            // Pass WM reference to View
            islandView.windowManager = wm
            islandView.windowParams = params

            // Add View to WindowManager
            wm.addView(islandView, params)

            islandInitialized = true
            log("[UI] SUCCESS: Added Island (Overlay) via WindowManager")

            IslandController.init(islandView)

        } catch (e: Throwable) {
            log("[ERROR] setupIsland crashed: " + e)
        }
    }

    private fun hookStatusBarViews(classLoader: ClassLoader) {
        try {
            // CRITICAL: Hook onAttachedToWindow to wait for physical display attachment
             XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                classLoader,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as ViewGroup
                        log("[HOOK] PhoneStatusBarView attached to window")
                        setupIsland(view.context) // Now has a valid, active display context
                        findClock(view)
                        findStatusIcons(view)
                    }
                }
            )
        } catch (e: Throwable) {
             log("[WARN] Failed to hook PhoneStatusBarView for finding icons: " + e)
        }
    }

    private fun findClock(view: View) {
        try {
            val clsName = view.javaClass.name
            if (clsName.contains("Clock") && !clsName.contains("Desupported")) {
                log("[CLOCK] Found potential clock: " + clsName)
                IslandController.setClock(view)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    findClock(view.getChildAt(i))
                }
            }
        } catch (e: Throwable) {
             // Ignore
        }
    }

    private fun findStatusIcons(view: View) {
        try {
            val clsName = view.javaClass.name
            if (clsName == "com.android.systemui.statusbar.phone.StatusIconContainer") {
                log("[STATUS_ICONS] Found StatusIconContainer")
                IslandController.setStatusIcons(view)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    findStatusIcons(view.getChildAt(i))
                }
            }
        } catch (e: Throwable) {
             // Ignore
        }
    }
}
