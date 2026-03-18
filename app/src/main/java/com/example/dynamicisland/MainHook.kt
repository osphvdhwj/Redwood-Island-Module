package com.example.dynamicisland

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        
        // 1. SYSTEM SERVER HOOKS
        if (lpparam.packageName == "android") {
            try {
                val atmsClass = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader)
                if (atmsClass != null) {
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
                    XposedBridge.hookAllMethods(nmsClass, "enqueueNotificationInternal", 
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                try {
                                    val pkgName = param.args[0] as? String ?: return
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

        // 2. SYSTEM UI HOOK (The Instrumentation Fix)
        if (lpparam.packageName == "com.android.systemui") {
            try {
                // 🚀 FIX: Hooking the un-bypassable OS Ignition Switch instead of the App class
                XposedHelpers.findAndHookMethod(
                    "android.app.Instrumentation",
                    lpparam.classLoader,
                    "callApplicationOnCreate",
                    Application::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val app = param.args[0] as Application
                            if (app.packageName == "com.android.systemui") {
                                injectDynamicIsland(app.applicationContext)
                            }
                        }
                    }
                )
                XposedBridge.log("RedwoodIsland: Instrumentation hook deployed successfully.")
            } catch (e: Exception) {
                XposedBridge.log("RedwoodIsland: Failed to hook Instrumentation - ${e.message}")
            }
        }
    }

    private fun injectDynamicIsland(systemUiContext: Context) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                XposedBridge.log("RedwoodIsland: Starting Android 16 compliant injection...")
                
                val displayManager = systemUiContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                
                val windowContext = systemUiContext.createWindowContext(display, 2024, null)
                val windowManager = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                val layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, 
                    WindowManager.LayoutParams.MATCH_PARENT, 
                    2024, 
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

                val controller = IslandController(windowContext)
                val islandView = controller.createIslandView(windowManager, layoutParams)

                windowManager.addView(islandView, layoutParams)
                XposedBridge.log("RedwoodIsland: Successfully injected overlay using WindowContext.")
            } catch (e: Exception) {
                XposedBridge.log("RedwoodIsland: FATAL ERROR during injection: ${e.message}")
            }
        }, 15000) 
    }
}
