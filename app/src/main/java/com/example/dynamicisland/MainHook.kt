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
                                    val extras = notification.extras
                                    val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                                    
                                    val text = extras.getString(android.app.Notification.EXTRA_TEXT) ?: ""
                                    val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
                                    val isOngoing = (notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0

                                    // 🚀 ADVANCED AUDIT: Live Activity Interception
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

        if (lpparam.packageName == "com.android.systemui") {
            try {
                XposedHelpers.findAndHookMethod(
                    Application::class.java,
                    "onCreate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val app = param.thisObject as Application
                            if (app.packageName == "com.android.systemui") {
                                if (isInitialized) return
                                isInitialized = true
                                
                                Handler(Looper.getMainLooper()).postDelayed({
                                    try {
                                        val windowManager = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                                        val layoutParams = WindowManager.LayoutParams(
                                            WindowManager.LayoutParams.WRAP_CONTENT,
                                            WindowManager.LayoutParams.WRAP_CONTENT,
                                            2024,
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
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                                            }
                                        }

                                        islandController = IslandController(app)
                                        val islandView = islandController?.createIslandView(windowManager, layoutParams)

                                        if (islandView != null) {
                                            windowManager.addView(islandView, layoutParams)
                                        }
                                    } catch (e: Throwable) { }
                                }, 3000)
                            }
                        }
                    }
                )
            } catch (e: Throwable) { }
        }
    }
}
