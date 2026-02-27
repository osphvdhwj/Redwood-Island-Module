package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Method

class MainHook : IXposedHookLoadPackage {
    private var islandInitialized = false
    private var windowManager: WindowManager? = null
    private var islandView: DynamicIslandView? = null

    private fun log(msg: String) {
        XposedBridge.log("DynamicIsland: $msg")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return
        log("[LOAD] Hooking SystemUI package: ${lpparam.packageName}")

        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.SystemUIService",
                lpparam.classLoader,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val serviceContext = param.thisObject as Context

                        registerScreenReceiver(serviceContext)

                        if (isBootCompleted()) {
                            log("[BOOT] System is already booted. Initializing Island immediately...")
                            Handler(Looper.getMainLooper()).postDelayed({
                                setupIsland(serviceContext)
                            }, 3000)
                        } else {
                            log("[BOOT] Waiting for BOOT_COMPLETED...")
                            val bootReceiver = object : BroadcastReceiver() {
                                override fun onReceive(context: Context, intent: Intent) {
                                    if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
                                        log("[BOOT] Boot completed event received. Initializing Island...")
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            setupIsland(serviceContext)
                                        }, 5000)
                                        try {
                                            context.unregisterReceiver(this)
                                        } catch (e: Exception) {}
                                    }
                                }
                            }
                            try {
                                serviceContext.registerReceiver(bootReceiver, IntentFilter(Intent.ACTION_BOOT_COMPLETED))
                            } catch (e: Throwable) {
                                log("[ERROR] Failed to register BOOT_COMPLETED receiver: $e")
                            }
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            log("[ERROR] Failed to hook SystemUIService: $e")
        }

        try {
            IslandController.hookFrameworkNotifications(lpparam)
        } catch (e: Throwable) {
            log("[ERROR] Framework hook init failed: $e")
        }

        try {
            val interruptStateProviderClass = XposedHelpers.findClass(
                "com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProviderImpl",
                lpparam.classLoader
            )

            XposedBridge.hookAllMethods(interruptStateProviderClass, "shouldHeadsUp", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val method = param.method as? Method
                    if (method != null && (method.returnType == Boolean::class.javaPrimitiveType || method.returnType == java.lang.Boolean::class.java)) {
                        param.result = false
                    }
                }
            })
            log("[UI] SUCCESS: Native System Heads-Up Notifications Suppressed (Safe Hook)")
        } catch (e: Throwable) {
            log("[WARN] Native HUN suppression hook failed: $e")
        }
    }

    private fun isBootCompleted(): Boolean {
        return try {
            val sysProp = Class.forName("android.os.SystemProperties")
            val getMethod = sysProp.getMethod("get", String::class.java, String::class.java)
            val bootState = getMethod.invoke(null, "sys.boot_completed", "0") as String
            bootState == "1"
        } catch (e: Exception) {
            false
        }
    }

    private fun registerScreenReceiver(context: Context) {
        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    when (intent.action) {
                        Intent.ACTION_SCREEN_OFF -> {
                            log("[SCREEN] OFF")
                            IslandController.onScreenStateChanged(false)
                        }
                        Intent.ACTION_SCREEN_ON -> {
                            log("[SCREEN] ON")
                            IslandController.onScreenStateChanged(true)
                        }
                    }
                }
            }

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            context.registerReceiver(receiver, filter)
            log("[SYSTEM] Screen Receiver Registered")
        } catch (e: Throwable) {
            log("[ERROR] Failed to register screen receiver: $e")
        }
    }

    @SuppressLint("WrongConstant")
    private fun setupIsland(context: Context) {
        if (islandInitialized) return

        try {
            val moduleContext = try {
                context.createPackageContext(
                    "com.example.dynamicisland",
                    Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
                )
            } catch (e: Exception) {
                log("[WARN] Could not create module context. Using system context: $e")
                context
            }

            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val windowContext = context.createDisplayContext(display).createWindowContext(2017, null)

            val wm = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            try {
                islandView = DynamicIslandView(moduleContext)
                islandView!!.id = View.generateViewId()
            } catch (e: Throwable) {
                log("[FATAL] View creation failed. Aborting: $e")
                return
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                2017,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.y = 0
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

            islandView!!.windowManager = wm
            islandView!!.windowParams = params

            wm.addView(islandView, params)
            islandInitialized = true
            IslandController.init(islandView!!)
            log("[SUCCESS] Dynamic Island initialized safely (Standard View).")
        } catch (e: Throwable) {
            log("[FATAL] setupIsland crashed gracefully: $e")
        }
    }
}
