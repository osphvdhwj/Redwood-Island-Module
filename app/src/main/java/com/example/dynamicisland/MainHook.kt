package com.example.dynamicisland

import android.content.Context
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
        if (lpparam.packageName != "com.android.systemui") return

        // 🚀 BULLETPROOF FIX 1: Catch everything. Never let SystemUI crash.
        try {
            val systemUIApplicationClass = XposedHelpers.findClassIfExists(
                "com.android.systemui.SystemUIApplication",
                lpparam.classLoader
            ) ?: return

            XposedHelpers.findAndHookMethod(
                systemUIApplicationClass,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isInitialized) return
                        
                        try {
                            val context = param.thisObject as Context
                            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                            // Run on main thread safely
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
                                } catch (e: Throwable) {
                                    XposedBridge.log("DynamicIsland: FATAL UI INJECTION ERROR -> ${e.stackTraceToString()}")
                                }
                            }
                        } catch (e: Throwable) {
                            XposedBridge.log("DynamicIsland: FATAL ONCREATE ERROR -> ${e.stackTraceToString()}")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: FATAL HOOK ERROR -> ${e.stackTraceToString()}")
        }
    }
}
