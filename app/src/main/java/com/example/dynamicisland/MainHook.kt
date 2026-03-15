package com.example.dynamicisland

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
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
            } catch (e: Throwable) { Log.e("Redwood", "System server hook failed", e) }
            return
        }

        if (lpparam.packageName == "com.android.systemui") {
            Log.d("Redwood", "Targeting SystemUI Process. Injecting Instrumentation Hook...")
            
            try {
                // 🚀 BULLETPROOF FIX: Hooking the lowest-level app creation event. 
                // It is impossible for SystemUI to bypass this.
                XposedHelpers.findAndHookMethod(
                    "android.app.Instrumentation",
                    lpparam.classLoader,
                    "callApplicationOnCreate",
                    Application::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val app = param.args[0] as Application
                            if (app.packageName == "com.android.systemui") {
                                if (isInitialized) return
                                Log.d("Redwood", "SystemUI Application onCreate detected! Initiating boot delay...")
                                
                                // Delay 3 seconds to let SystemUI fully stabilize its UI thread
                                Handler(Looper.getMainLooper()).postDelayed({
                                    try {
                                        Log.d("Redwood", "Injecting WindowManager LayoutParams...")
                                        val windowManager = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                                        val layoutParams = WindowManager.LayoutParams(
                                            WindowManager.LayoutParams.WRAP_CONTENT, // 🚀 Pure WRAP_CONTENT width
                                            WindowManager.LayoutParams.WRAP_CONTENT, // 🚀 Pure WRAP_CONTENT height
                                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
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

                                        Log.d("Redwood", "Initializing IslandController...")
                                        islandController = IslandController(app)
                                        
                                        Log.d("Redwood", "Creating ComposeView...")
                                        val islandView = islandController?.createIslandView(windowManager, layoutParams)

                                        if (islandView != null) {
                                            windowManager.addView(islandView, layoutParams)
                                            isInitialized = true
                                            Log.d("Redwood", "SUCCESS! Island View added to WindowManager.")
                                        } else {
                                            Log.e("Redwood", "ERROR: IslandView creation returned null.")
                                        }
                                    } catch (e: Throwable) {
                                        Log.e("Redwood", "FATAL ERROR during UI injection", e)
                                    }
                                }, 3000)
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                Log.e("Redwood", "FATAL ERROR setting up Instrumentation hook", e)
            }
        }
    }
}
