package com.example.dynamicisland

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    private var islandController: IslandController? = null
    private var isInitialized = false

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        
        // 1. SYSTEM SERVER HOOKS (Detects Gaming Mode & OTPs)
        if (lpparam.packageName == "android") {
            try {
                val atmsClass = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader)
                if (atmsClass != null) {
                    XposedHelpers.findAndHookMethod(atmsClass, "setResumedActivityUncheckLocked",
                        "com.android.server.wm.ActivityRecord", String::class.java,
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                try {
                                    val activityRecord = param.args[0] ?: return
                                    val packageName = XposedHelpers.getObjectField(activityRecord, "packageName") as? String ?: return
                                    val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                                    mContext?.sendBroadcast(Intent("com.example.dynamicisland.APP_CHANGED").putExtra("pkg", packageName))
                                } catch (e: Throwable) {}
                            }
                        }
                    )
                }
            } catch (e: Throwable) { XposedBridge.log("Redwood: System server hook failed -> ${e.message}") }
            return
        }

        // 2. SYSTEM_UI VISUAL HOOK
        if (lpparam.packageName == "com.android.systemui") {
            XposedBridge.log("Redwood: Injecting into SystemUI...")
            
            try {
                // 🚀 THE UNIVERSAL HOOK: We hook the base Application.onCreate. 
                // Every single Android app MUST pass through this, making it 100% immune to ROM obfuscation.
                XposedHelpers.findAndHookMethod(
                    Application::class.java,
                    "onCreate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val app = param.thisObject as Application
                            
                            // Ensure we are strictly inside SystemUI
                            if (app.packageName == "com.android.systemui") {
                                if (isInitialized) return
                                isInitialized = true
                                
                                XposedBridge.log("Redwood: SystemUI Application Booted! Starting 2-second UI delay...")
                                
                                // Delay allows SystemUI's massive internal components (like Status Bar) to finish booting first
                                Handler(Looper.getMainLooper()).postDelayed({
                                    try {
                                        XposedBridge.log("Redwood: Requesting WindowManager...")
                                        val windowManager = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                                        val layoutParams = WindowManager.LayoutParams(
                                            WindowManager.LayoutParams.WRAP_CONTENT, // 🚀 Pure WRAP_CONTENT
                                            WindowManager.LayoutParams.WRAP_CONTENT, // 🚀 Pure WRAP_CONTENT
                                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                                                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                                                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
                                                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                                            PixelFormat.TRANSLUCENT
                                        ).apply {
                                            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                                            title = "Redwood Dynamic Island"
                                            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                                        }

                                        XposedBridge.log("Redwood: Mounting IslandController and Jetpack Compose...")
                                        islandController = IslandController(app)
                                        val islandView = islandController?.createIslandView(windowManager, layoutParams)

                                        if (islandView != null) {
                                            windowManager.addView(islandView, layoutParams)
                                            XposedBridge.log("Redwood: SUCCESS! Island is officially on the screen.")
                                        } else {
                                            XposedBridge.log("Redwood ERROR: Compose View failed to build.")
                                        }
                                    } catch (e: Throwable) {
                                        XposedBridge.log("Redwood FATAL INJECTION ERROR: ${e.stackTraceToString()}")
                                    }
                                }, 2000)
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                XposedBridge.log("Redwood FATAL HOOK ERROR: ${e.stackTraceToString()}")
            }
        }
    }
}
