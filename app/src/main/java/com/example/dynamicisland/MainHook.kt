package com.example.dynamicisland

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.graphics.PixelFormat
import android.os.UserHandle
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        lateinit var modulePath: String
        // Tracks the Timestamp AND the Last Title seen for each app
        val lastBroadcastMap = mutableMapOf<String, Pair<Long, String>>()
        
        // 🎛️ FIXED: Bypass Compiler SDK Restrictions
        // We use Reflection to fetch the hidden UserHandle.ALL variable dynamically at runtime.
        val USER_ALL: UserHandle by lazy {
            UserHandle::class.java.getField("ALL").get(null) as UserHandle
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
        XposedBridge.log("DynamicIsland: Zygote Initialized")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        
        // 1. SYSTEM SERVER HOOKS
        if (lpparam.packageName == "android") {
            try {
                val atmsClass = XposedHelpers.findClassIfExists("com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader)
                if (atmsClass != null) {
                    XposedBridge.hookAllMethods(atmsClass, "setResumedActivityUncheckLocked", object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            try {
                                val activityRecord = param.args[0] ?: return
                                val packageName = XposedHelpers.getObjectField(activityRecord, "packageName") as? String ?: return
                                val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                                
                                val intent = Intent("com.example.dynamicisland.APP_CHANGED")
                                    .setPackage("com.android.systemui")
                                    .putExtra("pkg", packageName)
                                
                                // 🎛️ FIXED: Uses our Reflection-based USER_ALL
                                mContext?.sendBroadcastAsUser(intent, USER_ALL)
                            } catch (e: Throwable) {}
                        }
                    })
                }

                val nmsClass = XposedHelpers.findClassIfExists("com.android.server.notification.NotificationManagerService", lpparam.classLoader)
                if (nmsClass != null) {
                    XposedBridge.hookAllMethods(nmsClass, "enqueueNotificationInternal", object : XC_MethodHook() {
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
                                    val currentTime = System.currentTimeMillis()
                                    val lastData = lastBroadcastMap[pkgName]
                                    val lastTime = lastData?.first ?: 0L
                                    val lastTitle = lastData?.second ?: ""

                                    val isNewContent = title != lastTitle

                                    if (isNewContent || currentTime - lastTime > 1000) {
                                        lastBroadcastMap[pkgName] = Pair(currentTime, title)
                                        val progress = extras.getInt(android.app.Notification.EXTRA_PROGRESS, -1)
                                        val progressMax = extras.getInt(android.app.Notification.EXTRA_PROGRESS_MAX, -1)

                                        val intent = Intent("com.example.dynamicisland.LIVE_ACTIVITY_CAUGHT").apply {
                                            setPackage("com.android.systemui")
                                            putExtra("pkg", pkgName)
                                            putExtra("title", title)
                                            putExtra("text", text)
                                            if (progress != -1) putExtra("progress", progress)
                                            if (progressMax != -1) putExtra("progressMax", progressMax)
                                        }
                                        // 🎛️ FIXED: Uses our Reflection-based USER_ALL
                                        mContext?.sendBroadcastAsUser(intent, USER_ALL)
                                    }
                                }

                                if (text.contains("OTP", true) || text.contains("code", true) || text.contains("verification", true)) {
                                    val otpRegex = Regex("\\b\\d{4,8}\\b")
                                    val match = otpRegex.find(text)
                                    if (match != null) {
                                        val intent = Intent("com.example.dynamicisland.OTP_CAUGHT")
                                            .setPackage("com.android.systemui")
                                            .putExtra("otp", match.value)
                                            .putExtra("pkg", pkgName)
                                        // 🎛️ FIXED: Uses our Reflection-based USER_ALL
                                        mContext?.sendBroadcastAsUser(intent, USER_ALL)
                                    }
                                }
                            } catch (e: Throwable) {}
                        }
                    })
                }
            } catch (e: Throwable) {
                XposedBridge.log("DynamicIsland: System server hook failed -> ${e.message}")
            }
        }

        // 2. SYSTEM UI HOOK
        if (lpparam.packageName == "com.android.systemui") {
            try {
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
                XposedBridge.log("DynamicIsland: Instrumentation hook deployed successfully.")
            } catch (e: Exception) {
                XposedBridge.log("DynamicIsland: Failed to hook Instrumentation - ${e.message}")
            }
        }
    }

    private fun injectDynamicIsland(systemUiContext: Context) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                XposedBridge.log("DynamicIsland: Starting Android 16 compliant injection...")
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
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    title = "DynamicIslandOverlay"
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                }
                
                val controller = IslandController(windowContext)
                val islandView = controller.createIslandView(windowManager, layoutParams)
                windowManager.addView(islandView, layoutParams)
                
                XposedBridge.log("DynamicIsland: Successfully injected overlay using WindowContext.")
            } catch (e: Exception) {
                XposedBridge.log("DynamicIsland: FATAL ERROR during injection: ${e.stackTraceToString()}")
            }
        }, 6000)
    }
}
