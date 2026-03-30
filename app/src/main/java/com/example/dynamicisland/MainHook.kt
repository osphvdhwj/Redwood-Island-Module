package com.example.dynamicisland

import android.app.Application
import android.content.Context
import android.view.WindowManager
import android.os.UserHandle
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
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
        
        if (lpparam.packageName == "android") {
            SystemEventsHook.apply(lpparam, USER_ALL)
            FrameworkTelecomHook.apply(lpparam, USER_ALL)
            FrameworkHardwareHook.apply(lpparam, USER_ALL)
            
            // 🚀 SHARE SHEET ASSASSIN & REDIRECTOR
            try {
                val chooserClass = XposedHelpers.findClassIfExists("com.android.internal.app.ChooserActivity", lpparam.classLoader)
                if (chooserClass != null) {
                    XposedHelpers.findAndHookMethod(
                        chooserClass, "onCreate", android.os.Bundle::class.java,
                        object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any? {
                                val activity = param.thisObject as android.app.Activity
                                val targetIntent = activity.intent.getParcelableExtra<android.content.Intent>(android.content.Intent.EXTRA_INTENT)
                                
                                if (targetIntent != null) {
                                    val islandIntent = android.content.Intent("com.example.dynamicisland.SHARE_INTERCEPTED")
                                    islandIntent.putExtra("raw_intent", targetIntent)
                                    android.app.AndroidAppHelper.currentApplication().sendBroadcast(islandIntent)
                                    activity.finish()
                                    return null
                                }
                                return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                            }
                        }
                    )
                }
            } catch (e: Throwable) {}

            // 🔗 UNIVERSAL MEDIA LINK SWITCHER
            try {
                IslandHookEngine.hookMethodSafe(
                    "android.app.Instrumentation", lpparam.classLoader, "execStartActivity",
                    Context::class.java, android.os.IBinder::class.java, android.os.IBinder::class.java, 
                    android.app.Activity::class.java, android.content.Intent::class.java, 
                    Int::class.javaPrimitiveType!!, android.os.Bundle::class.java,
                    object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any? {
                            val intent = param.args[4] as android.content.Intent
                            
                            if (intent.action == android.content.Intent.ACTION_VIEW && intent.data?.scheme?.startsWith("http") == true) {
                                val urlHost = intent.data?.host ?: ""
                                if (urlHost.contains("youtube.com") || urlHost.contains("youtu.be") || urlHost.contains("spotify.com")) {
                                    val islandIntent = android.content.Intent("com.example.dynamicisland.LINK_INTERCEPTED")
                                    islandIntent.putExtra("url", intent.dataString)
                                    islandIntent.putExtra("host", urlHost)
                                    android.app.AndroidAppHelper.currentApplication().sendBroadcast(islandIntent)
                                    return null
                                }
                            }
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                    }
                )
            } catch (e: Throwable) {}
        }

        if (lpparam.packageName == "com.android.systemui") {
            SystemUIHardwareHook.apply(lpparam)
            
            // 🔪 ASSASSINATE THE NATIVE CLIPBOARD OVERLAY
            try {
                val clipboardListenerClass = XposedHelpers.findClassIfExists("com.android.systemui.clipboardoverlay.ClipboardListener", lpparam.classLoader)
                if (clipboardListenerClass != null) {
                    XposedHelpers.findAndHookMethod(
                        clipboardListenerClass, "start", 
                        object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any? = null
                        }
                    )
                }
            } catch (e: Throwable) {}

            // 📸 THE SCREENSHOT ASSASSIN
            try {
                val screenshotControllerClass = XposedHelpers.findClassIfExists("com.android.systemui.screenshot.ScreenshotController", lpparam.classLoader)
                if (screenshotControllerClass != null) {
                    XposedHelpers.findAndHookMethod(
                        screenshotControllerClass, "showScreenshotDropInUI", "com.android.systemui.screenshot.ScreenshotSavedResult",
                        object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any? {
                                val result = param.args[0]
                                val uri = XposedHelpers.getObjectField(result, "uri")?.toString() ?: ""
                                val intent = android.content.Intent("com.example.dynamicisland.SCREENSHOT_CAUGHT")
                                intent.putExtra("uri", uri)
                                intent.setPackage("com.android.systemui")
                                android.app.AndroidAppHelper.currentApplication().sendBroadcast(intent)
                                return null
                            }
                        }
                    )
                }
            } catch (e: Throwable) {}
            
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

    private fun injectDynamicIsland(systemUiContext: Context) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val displayManager = systemUiContext.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
                val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                val windowContext = systemUiContext.createWindowContext(display, 2024, null)
                val windowManager = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                
                val layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, 2024,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    android.graphics.PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                    title = "DynamicIslandOverlay"
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                }
                
                val controller = IslandController(windowContext)
                val islandView = controller.createIslandView(windowManager, layoutParams)
                windowManager.addView(islandView, layoutParams)
            } catch (e: Exception) {}
        }, 1500)
    }
}
