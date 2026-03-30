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
        val USER_ALL: UserHandle by lazy { UserHandle::class.java.getField("ALL").get(null) as UserHandle }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        
        // 🚀 Extracting 100% Potential of System Server (Backend)
        if (lpparam.packageName == "android") {
            SystemEventsHook.apply(lpparam, USER_ALL)
            FrameworkTelecomHook.apply(lpparam, USER_ALL)
            FrameworkHardwareHook.apply(lpparam, USER_ALL) // <-- NEW
        }

        // 🎨 Extracting 100% Potential of SystemUI (Frontend)
        if (lpparam.packageName == "com.android.systemui") {
            SystemUIHardwareHook.apply(lpparam) // <-- NEW

            // 🔪 ASSASSINATE THE NATIVE CLIPBOARD OVERLAY
            // This prevents the annoying Android 13+ bottom-left clipboard chip from ever appearing
            try {
                val clipboardListenerClass = XposedHelpers.findClassIfExists("com.android.systemui.clipboardoverlay.ClipboardListener", lpparam.classLoader)
                if (clipboardListenerClass != null) {
                    XposedHelpers.findAndHookMethod(
                        clipboardListenerClass, 
                        "start", 
                        object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any? {
                                // By returning null and replacing the method, SystemUI is entirely blocked 
                                // from showing the default clipboard UI. The Island is now the sole owner.
                                return null
                            }
                        }
                    )
                }
            } catch (e: Throwable) {
                XposedBridge.log("DynamicIsland: Failed to hook Clipboard Overlay - ${e.message}")
            }

            // 📸 THE SCREENSHOT ASSASSIN & HIJACKER
            try {
                val screenshotControllerClass = XposedHelpers.findClassIfExists("com.android.systemui.screenshot.ScreenshotController", lpparam.classLoader)
                if (screenshotControllerClass != null) {
                    // Hook the method that normally displays the bottom-corner preview
                    XposedHelpers.findAndHookMethod(
                        screenshotControllerClass,
                        "showScreenshotDropInUI", // Name varies slightly by Android version, can also be "saveScreenshot"
                        "com.android.systemui.screenshot.ScreenshotSavedResult",
                        object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any? {
                                val result = param.args[0]
                                val uri = XposedHelpers.getObjectField(result, "uri")?.toString() ?: ""
                                
                                // Send the broadcast to the Island with the Screenshot URI
                                val intent = android.content.Intent("com.example.dynamicisland.SCREENSHOT_CAUGHT")
                                intent.putExtra("uri", uri)
                                intent.setPackage("com.android.systemui")
                                android.app.AndroidAppHelper.currentApplication().sendBroadcast(intent)
                                
                                // Return null to abort the standard Android UI
                                return null
                            }
                        }
                    )
                }
            } catch (e: Throwable) {
                XposedBridge.log("DynamicIsland: Failed to hook ScreenshotController - ${e.message}")
            }
            
            IslandHookEngine.hookMethodSafe(
                "android.app.Instrumentation", lpparam.classLoader, "callApplicationOnCreate", Application::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.args[0] as Application
                        if (app.packageName == "com.android.systemui") { injectDynamicIsland(app.applicationContext) }
                    }
                }
            )

            IslandHookEngine.hookAllConstructorsSafe(
                "com.android.systemui.qs.QSTileHost", lpparam.classLoader,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) { SystemUIContextKeeper.qsTileHost = param.thisObject }
                }
            )
        }
    }

    // Inside MainHook.kt
    private fun injectDynamicIsland(systemUiContext: Context) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val displayManager = systemUiContext.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
                val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                val windowContext = systemUiContext.createWindowContext(display, 2024, null)
                val windowManager = windowContext.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                
                val layoutParams = android.view.WindowManager.LayoutParams(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT, 2024,
                    android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    android.graphics.PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                    title = "DynamicIslandOverlay"
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
                
                val controller = IslandController(windowContext)
                val islandView = controller.createIslandView(windowManager, layoutParams)
                windowManager.addView(islandView, layoutParams)
            } catch (e: Exception) {
                de.robv.android.xposed.XposedBridge.log("DynamicIsland ❌: FATAL ERROR during injection: ${e.message}")
            }
        }, 1500) // ⚡ FIXED: Reduced from 6000ms to 1500ms for near-instant boot!
    }
}
