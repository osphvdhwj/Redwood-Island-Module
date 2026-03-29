package com.example.dynamicisland

import android.app.Application
import android.content.Context
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

object SystemUIContextKeeper {
    var qsTileHost: Any? = null
}

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        lateinit var modulePath: String
        val USER_ALL: UserHandle by lazy {
            UserHandle::class.java.getField("ALL").get(null) as UserHandle
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
        XposedBridge.log("DynamicIsland: Zygote Initialized")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        
        // 1. Delegate System Server Hooks
        if (lpparam.packageName == "android") {
            SystemEventsHook.apply(lpparam, USER_ALL)
        }

        // 2. Core SystemUI Injection
        if (lpparam.packageName == "com.android.systemui") {
            try {
                XposedHelpers.findAndHookMethod(
                    "android.app.Instrumentation", lpparam.classLoader, "callApplicationOnCreate",
                    Application::class.java, object : XC_MethodHook() {
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

            try {
                val qsTileHostClass = XposedHelpers.findClassIfExists("com.android.systemui.qs.QSTileHost", lpparam.classLoader)
                if (qsTileHostClass != null) {
                    XposedBridge.hookAllConstructors(qsTileHostClass, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            SystemUIContextKeeper.qsTileHost = param.thisObject
                        }
                    })
                }
            } catch (e: Exception) {}
        }
    }

    private fun injectDynamicIsland(systemUiContext: Context) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val displayManager = systemUiContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                val windowContext = systemUiContext.createWindowContext(display, 2024, null)
                val windowManager = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                
                val layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, 2024,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
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
            } catch (e: Exception) {
                XposedBridge.log("DynamicIsland: FATAL ERROR during injection: ${e.stackTraceToString()}")
            }
        }, 6000)
    }
}
