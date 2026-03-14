package com.example.dynamicisland

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
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
                XposedBridge.log("DynamicIsland: system_server hook failed -> ${e.message}")
            }
            return
        }

        if (lpparam.packageName == "com.android.systemui") {
            try {
                val systemUIApplicationClass = XposedHelpers.findClassIfExists("com.android.systemui.SystemUIApplication", lpparam.classLoader) ?: return

                XposedHelpers.findAndHookMethod(
                    systemUIApplicationClass,
                    "onCreate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (isInitialized) return
                            try {
                                val context = param.thisObject as Context
                                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                                android.os.Handler(context.mainLooper).post {
                                    try {
                                        val layoutParams = WindowManager.LayoutParams(
                                            WindowManager.LayoutParams.MATCH_PARENT,
                                            WindowManager.LayoutParams.WRAP_CONTENT,
                                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                                                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                                            PixelFormat.TRANSLUCENT
                                        ).apply {
                                            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                                            title = "Redwood Dynamic Island"
                                            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                                        }

                                        islandController = IslandController(context)
                                        val islandView = islandController?.createIslandView(windowManager, layoutParams)

                                        if (islandView != null) {
                                            windowManager.addView(islandView, layoutParams)
                                            isInitialized = true
                                        }
                                    } catch (e: Throwable) {}
                                }
                            } catch (e: Throwable) {}
                        }
                    }
                )
            } catch (e: Throwable) {}
        }
    }
}
