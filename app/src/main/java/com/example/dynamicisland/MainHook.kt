package com.example.dynamicisland

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
        
        // 1. SYSTEM SERVER HOOKS (From your new features: Detects Gaming Mode & OTPs)
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

                val nmsClass = XposedHelpers.findClassIfExists("com.android.server.notification.NotificationManagerService", lpparam.classLoader)
                if (nmsClass != null) {
                    XposedHelpers.findAndHookMethod(nmsClass, "enqueueNotificationInternal", 
                        String::class.java, String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, 
                        String::class.java, Int::class.javaPrimitiveType, android.app.Notification::class.java, Int::class.javaPrimitiveType,
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                try {
                                    val pkgName = param.args[0] as? String ?: return
                                    val notification = param.args[6] as? android.app.Notification ?: return
                                    val text = notification.extras.getString(android.app.Notification.EXTRA_TEXT) ?: return
                                    
                                    if (text.contains("OTP", true) || text.contains("code", true) || text.contains("verification", true)) {
                                        val otpRegex = Regex("\\b\\d{4,8}\\b")
                                        val match = otpRegex.find(text)
                                        if (match != null) {
                                            val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                                            mContext?.sendBroadcast(Intent("com.example.dynamicisland.OTP_CAUGHT").putExtra("otp", match.value).putExtra("pkg", pkgName))
                                        }
                                    }
                                } catch (e: Throwable) {}
                            }
                        }
                    )
                }
            } catch (e: Throwable) { 
                XposedBridge.log("Redwood: System server hook failed -> ${e.message}") 
            }
            return
        }

        // 2. SYSTEM_UI VISUAL HOOK (Restored to Stable Branch Logic)
        if (lpparam.packageName == "com.android.systemui") {
            try {
                // 🚀 Restored hook target to SystemUIApplication
                XposedHelpers.findAndHookMethod(
                    "com.android.systemui.SystemUIApplication",
                    lpparam.classLoader,
                    "onCreate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (isInitialized) return
                            isInitialized = true
                            
                            val systemUiContext = param.thisObject as Context
                            
                            // 🚀 Restored longer delay (8 seconds) to let SystemUI build the status bar first
                            Handler(Looper.getMainLooper()).postDelayed({
                                try {
                                    XposedBridge.log("RedwoodIsland: Starting delayed injection...")
                                    val windowManager = systemUiContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                                    // 🚀 Restored PROVEN WindowManager flags from the Stable branch
                                    val layoutParams = WindowManager.LayoutParams(
                                        WindowManager.LayoutParams.WRAP_CONTENT,
                                        WindowManager.LayoutParams.WRAP_CONTENT,
                                        2024, // TYPE_NAVIGATION_BAR_PANEL (The magic bullet that fixes visibility)
                                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                                        PixelFormat.TRANSLUCENT
                                    ).apply {
                                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                                        title = "RedwoodIslandOverlay"

                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                                        }
                                    }

                                    islandController = IslandController(systemUiContext)
                                    val islandView = islandController?.createIslandView(windowManager, layoutParams)

                                    if (islandView != null) {
                                        windowManager.addView(islandView, layoutParams)
                                        XposedBridge.log("RedwoodIsland: Successfully injected overlay with dynamic bounds.")
                                    } else {
                                        XposedBridge.log("RedwoodIsland ERROR: Compose View failed to build.")
                                    }
                                } catch (e: Throwable) {
                                    XposedBridge.log("RedwoodIsland: FATAL ERROR during injection: ${e.stackTraceToString()}")
                                }
                            }, 8000) // 8 second compromise between the old 15s and new 2s
                        }
                    }
                )
            } catch (e: Throwable) {
                XposedBridge.log("RedwoodIsland FATAL HOOK ERROR: ${e.stackTraceToString()}")
            }
        }
    }
}
