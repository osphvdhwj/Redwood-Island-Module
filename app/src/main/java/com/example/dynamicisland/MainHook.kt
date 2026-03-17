package com.example.dynamicisland

import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        
        // 1. SYSTEM SERVER HOOKS (Android 16 / CrDroid 12.7 Bulletproof)
        if (lpparam.packageName == "android") {
            try {
                val atmsClass = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader)
                if (atmsClass != null) {
                    // 🚀 FIX: Using hookAllMethods because Android 16 changes parameter counts!
                    XposedBridge.hookAllMethods(atmsClass, "setResumedActivityUncheckLocked",
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
                    // 🚀 FIX: Using hookAllMethods to bypass Android 16's new enqueueNotification signatures
                    XposedBridge.hookAllMethods(nmsClass, "enqueueNotificationInternal", 
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                try {
                                    val pkgName = param.args[0] as? String ?: return
                                    // Dynamically find the Notification object regardless of Android 16 index shifts
                                    val notification = param.args.firstOrNull { it is android.app.Notification } as? android.app.Notification ?: return
                                    
                                    val extras = notification.extras
                                    val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                                    
                                    val text = extras.getString(android.app.Notification.EXTRA_TEXT) ?: ""
                                    val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
                                    val isOngoing = (notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0

                                    if (isOngoing && pkgName != "com.android.systemui" && pkgName != "android") {
                                        val progress = extras.getInt(android.app.Notification.EXTRA_PROGRESS, -1)
                                        val progressMax = extras.getInt(android.app.Notification.EXTRA_PROGRESS_MAX, -1)
                                        
                                        val intent = Intent("com.example.dynamicisland.LIVE_ACTIVITY_CAUGHT").apply {
                                            putExtra("pkg", pkgName)
                                            putExtra("title", title)
                                            putExtra("text", text)
                                            if (progress != -1) putExtra("progress", progress)
                                            if (progressMax != -1) putExtra("progressMax", progressMax)
                                        }
                                        mContext?.sendBroadcast(intent)
                                    }

                                    if (text.contains("OTP", true) || text.contains("code", true) || text.contains("verification", true)) {
                                        val otpRegex = Regex("\\b\\d{4,8}\\b")
                                        val match = otpRegex.find(text)
                                        if (match != null) {
                                            mContext?.sendBroadcast(Intent("com.example.dynamicisland.OTP_CAUGHT").putExtra("otp", match.value).putExtra("pkg", pkgName))
                                        }
                                    }
                                } catch (e: Throwable) {}
                            }
                        }
                    )
                }
            } catch (e: Throwable) { XposedBridge.log("Redwood: System server hook failed -> ${e.message}") }
            return
        }

        // 2. SYSTEM UI HOOK (Android 16 Universal Application Hook)
        if (lpparam.packageName == "com.android.systemui") {
            try {
                // 🚀 FIX: Hooking the base Application class is un-bypassable.
                XposedHelpers.findAndHookMethod(
                    Application::class.java,
                    "onCreate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val app = param.thisObject as Application
                            if (app.packageName == "com.android.systemui") {
                                injectDynamicIsland(app.applicationContext)
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                XposedBridge.log("DynamicIsland: Failed to hook SystemUI base Application - ${e.message}")
            }
        }
    }

    private fun injectDynamicIsland(systemUiContext: Context) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                XposedBridge.log("RedwoodIsland: Starting delayed injection...")
                val windowManager = systemUiContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                val layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, 
                    WindowManager.LayoutParams.MATCH_PARENT, 
                    2024, // TYPE_NAVIGATION_BAR_PANEL
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,   
                    android.graphics.PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                    title = "RedwoodIslandOverlay"

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                }

                val controller = IslandController(systemUiContext)
                val islandView = controller.createIslandView(windowManager, layoutParams)

                windowManager.addView(islandView, layoutParams)
                XposedBridge.log("RedwoodIsland: Successfully injected overlay with dynamic bounds.")
            } catch (e: Exception) {
                XposedBridge.log("RedwoodIsland: FATAL ERROR during injection: ${e.message}")
            }
        }, 15000) 
    }
}
