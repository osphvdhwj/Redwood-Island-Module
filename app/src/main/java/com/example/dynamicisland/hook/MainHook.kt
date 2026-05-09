package com.example.dynamicisland.hook

import android.app.Application
import android.content.Context
import android.os.UserHandle
import android.view.WindowManager
import com.example.dynamicisland.manager.IslandController
import com.example.dynamicisland.ui.DynamicIslandView
import com.example.dynamicisland.util.ComposeLifecycleOwner
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

object SystemUIContextKeeper {
    var qsTileHost: Any? = null
}

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        lateinit var modulePath: String

        val USER_ALL: UserHandle by lazy {
            try {
                UserHandle::class.java.getField("ALL").get(null) as UserHandle
            } catch (_: Throwable) {
                try {
                    XposedHelpers.callStaticMethod(
                        UserHandle::class.java, "of",
                        arrayOf(Int::class.javaPrimitiveType), -1
                    ) as UserHandle
                } catch (_: Throwable) {
                    android.os.Process.myUserHandle()
                }
            }
        }

        // ── Logging ──────────────────────────────────────────────────────────
        fun log(msg: String)      = XposedBridge.log("DynamicIsland: $msg")
        fun logOk(msg: String)    = XposedBridge.log("DynamicIsland ✅: $msg")
        fun logWarn(msg: String)  = XposedBridge.log("DynamicIsland ⚠️: $msg")
        fun logError(msg: String, t: Throwable? = null) =
            XposedBridge.log("DynamicIsland ❌: $msg${t?.message?.let { " — $it" } ?: ""}")
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
        log("initZygote — module loaded from $modulePath")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("handleLoadPackage: ${lpparam.packageName}")
        when (lpparam.packageName) {
            "android"                     -> hookAndroidProcess(lpparam)
            "com.android.systemui"        -> hookSystemUIProcess(lpparam)
            "com.android.intentresolver",
            "com.google.android.intentresolver" -> hookIntentResolver(lpparam)
        }
    }

    // ── android process ───────────────────────────────────────────────────────

    private fun hookAndroidProcess(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("Applying android-process hooks")

        SurfaceFlingerHook.apply(lpparam, USER_ALL)
        CrDroidAPIHook.apply(lpparam, USER_ALL)
        SystemEventsHook.apply(lpparam, USER_ALL)
        FrameworkTelecomHook.apply(lpparam, USER_ALL)
        DeepTelecomHook.apply(lpparam, USER_ALL)
        FrameworkAlarmHook.apply(lpparam, USER_ALL)
        FrameworkAlarmTriggerHook.apply(lpparam, USER_ALL)
        FrameworkHardwareHook.apply(lpparam, USER_ALL)
        hookLinkInterceptor(lpparam)
    }

    private fun hookLinkInterceptor(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            IslandHookEngine.hookMethodSafe(
                "android.app.Instrumentation", lpparam.classLoader, "execStartActivity",
                Context::class.java,
                android.os.IBinder::class.java,
                android.os.IBinder::class.java,
                android.app.Activity::class.java,
                android.content.Intent::class.java,
                Int::class.javaPrimitiveType!!,
                android.os.Bundle::class.java,
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: XC_MethodHook.MethodHookParam): Any? {
                        val intent = param.args[4] as android.content.Intent
                        if (intent.action == android.content.Intent.ACTION_VIEW &&
                            intent.data?.scheme?.startsWith("http") == true) {
                            val host = intent.data?.host ?: ""
                            if (host.contains("youtube.com") || host.contains("youtu.be") ||
                                host.contains("spotify.com")) {
                                val islandIntent = android.content.Intent(
                                    "com.example.dynamicisland.LINK_INTERCEPTED"
                                ).apply {
                                    putExtra("url", intent.dataString)
                                    putExtra("host", host)
                                }
                                android.app.AndroidAppHelper.currentApplication()
                                    ?.sendBroadcast(islandIntent)
                                return null
                            }
                        }
                        return XposedBridge.invokeOriginalMethod(
                            param.method, param.thisObject, param.args
                        )
                    }
                }
            )
        } catch (_: Throwable) {}
    }

    // ── SystemUI process ──────────────────────────────────────────────────────

    private fun hookSystemUIProcess(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("Applying SystemUI-process hooks")

        SystemUIHardwareHook.apply(lpparam)
        SystemUIPanelHook.apply(lpparam)
        suppressClipboardOverlay(lpparam)
        suppressScreenshotNative(lpparam)
        hookQSTileHost(lpparam)
        injectIslandMultiStrategy(lpparam)
    }

    /**
     * Island injection — four independent strategies tried in order.
     * Any one success is sufficient; the rest become no-ops via [injected] flag.
     */
    private fun injectIslandMultiStrategy(lpparam: XC_LoadPackage.LoadPackageParam) {
        var injected = false

        fun inject(ctx: Context, via: String) {
            if (injected) return
            injected = true
            logOk("Island injection triggered via: $via")
            injectDynamicIsland(ctx)
        }

        // ── Strategy 1: Hook SystemUIApplication directly ────────────────────
        val appClasses = listOf(
            "com.android.systemui.SystemUIApplication",          // AOSP 12–15
            "com.android.systemui.SystemUIAppComponentFactory",  // some AOSP builds
            "com.nothing.systemui.NothingSystemUIApplication",   // Nothing OS
            "com.miui.home.launcher.LauncherApplication",        // MIUI (fallback)
        )
        for (cls in appClasses) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                XposedBridge.hookAllMethods(clazz, "onCreate", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Application ?: return
                        inject(app.applicationContext, "Strategy1/$cls.onCreate")
                    }
                })
                logOk("Strategy1 hooked: $cls.onCreate")
                break
            } catch (e: Throwable) {
                logWarn("Strategy1 $cls failed: ${e.message}")
            }
        }

        // ── Strategy 2: Instrumentation.callApplicationOnCreate ──────────────
        try {
            IslandHookEngine.hookMethodSafe(
                "android.app.Instrumentation", lpparam.classLoader,
                "callApplicationOnCreate", Application::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.args[0] as? Application ?: return
                        if (app.packageName != "com.android.systemui") return
                        inject(app.applicationContext, "Strategy2/Instrumentation")
                    }
                }
            )
            logOk("Strategy2 hooked: Instrumentation.callApplicationOnCreate")
        } catch (e: Throwable) {
            logWarn("Strategy2 failed: ${e.message}")
        }

        // ── Strategy 3: ActivityThread.handleBindApplication ─────────────────
        try {
            val atClass = XposedHelpers.findClassIfExists(
                "android.app.ActivityThread", lpparam.classLoader
            )
            if (atClass != null) {
                XposedBridge.hookAllMethods(
                    atClass, "handleBindApplication", object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val app = android.app.AndroidAppHelper.currentApplication()
                                ?: return
                            if (app.packageName != "com.android.systemui") return
                            inject(app.applicationContext, "Strategy3/ActivityThread")
                        }
                    }
                )
                logOk("Strategy3 hooked: ActivityThread.handleBindApplication")
            }
        } catch (e: Throwable) {
            logWarn("Strategy3 failed: ${e.message}")
        }

        // ── Strategy 4: PhoneStatusBar / CentralSurfaces start ───────────────
        val statusBarClasses = listOf(
            "com.android.systemui.statusbar.phone.CentralSurfacesImpl", // A13+
            "com.android.systemui.statusbar.phone.PhoneStatusBar",      // A12-
            "com.android.systemui.statusbar.phone.StatusBar",           // older
        )
        for (cls in statusBarClasses) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                XposedBridge.hookAllMethods(clazz, "start", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val ctx = try {
                            XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                        } catch (_: Throwable) {
                            android.app.AndroidAppHelper.currentApplication()
                        } ?: return
                        inject(ctx, "Strategy4/$cls.start")
                    }
                })
                logOk("Strategy4 hooked: $cls.start")
                break
            } catch (e: Throwable) {
                logWarn("Strategy4 $cls failed: ${e.message}")
            }
        }
    }

    private fun hookQSTileHost(lpparam: XC_LoadPackage.LoadPackageParam) {
        val qsClasses = listOf(
            "com.android.systemui.qs.QSTileHost",
            "com.android.systemui.qs.tileimpl.QSTileHost",
        )
        for (cls in qsClasses) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                XposedBridge.hookAllConstructors(clazz, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        SystemUIContextKeeper.qsTileHost = param.thisObject
                        logOk("QSTileHost captured via $cls")
                    }
                })
                break
            } catch (e: Throwable) {
                logWarn("QSTileHost hook failed for $cls: ${e.message}")
            }
        }
    }

    private fun suppressClipboardOverlay(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz = XposedHelpers.findClassIfExists(
            "com.android.systemui.clipboardoverlay.ClipboardListener", lpparam.classLoader
        ) ?: return
        try {
            XposedHelpers.findAndHookMethod(clazz, "start", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? = null
            })
            logOk("ClipboardListener.start suppressed")
        } catch (e: Throwable) {
            logWarn("ClipboardListener suppress failed: ${e.message}")
        }
    }

    private fun suppressScreenshotNative(lpparam: XC_LoadPackage.LoadPackageParam) {
        val candidates = listOf(
            "com.android.systemui.screenshot.ScreenshotController" to "showScreenshotDropInUI",
            "com.android.systemui.screenshot.ScreenshotController" to "handleImageAsShared",
        )
        for ((cls, method) in candidates) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                XposedBridge.hookAllMethods(clazz, method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // Extract URI if it's the save-result overload
                        val result = param.args.firstOrNull() ?: return
                        val uri = try {
                            XposedHelpers.getObjectField(result, "uri")?.toString() ?: ""
                        } catch (_: Throwable) { "" }
                        val intent = android.content.Intent(
                            "com.example.dynamicisland.SCREENSHOT_CAUGHT"
                        ).apply {
                            setPackage("com.android.systemui")
                            putExtra("uri", uri)
                        }
                        android.app.AndroidAppHelper.currentApplication()?.sendBroadcast(intent)
                        param.result = null  // suppress native UI
                    }
                })
                logOk("Screenshot suppressed via $cls.$method")
            } catch (e: Throwable) {
                logWarn("Screenshot suppress $cls.$method failed: ${e.message}")
            }
        }
    }

    // ── Intent resolver process ───────────────────────────────────────────────

    private fun hookIntentResolver(lpparam: XC_LoadPackage.LoadPackageParam) {
        val chooserCandidates = listOf(
            "com.android.intentresolver.ChooserActivity",
            "com.android.internal.app.ChooserActivity",
        )
        for (cls in chooserCandidates) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                XposedHelpers.findAndHookMethod(
                    clazz, "onCreate", android.os.Bundle::class.java,
                    object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any? {
                            val activity = param.thisObject as android.app.Activity
                            val target = activity.intent
                                .getParcelableExtra<android.content.Intent>(
                                    android.content.Intent.EXTRA_INTENT
                                ) ?: return XposedBridge.invokeOriginalMethod(
                                    param.method, param.thisObject, param.args
                                )
                            android.app.AndroidAppHelper.currentApplication()
                                ?.sendBroadcast(
                                    android.content.Intent(
                                        "com.example.dynamicisland.SHARE_INTERCEPTED"
                                    ).putExtra("raw_intent", target)
                                )
                            activity.finish()
                            return null
                        }
                    }
                )
                logOk("ChooserActivity hooked via $cls")
                break
            } catch (e: Throwable) {
                logWarn("ChooserActivity hook $cls failed: ${e.message}")
            }
        }
    }

    // ── Island injection ──────────────────────────────────────────────────────

    private fun injectDynamicIsland(systemUiContext: Context) {
        // Always post to main looper; delay gives SystemUI time to finish its own init
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val dm = systemUiContext.getSystemService(Context.DISPLAY_SERVICE)
                    as android.hardware.display.DisplayManager
                val display = dm.getDisplay(android.view.Display.DEFAULT_DISPLAY)

                // Window type 2024 = TYPE_NAVIGATION_BAR_PANEL — bypasses A14 status-bar touch guard
                val windowContext = systemUiContext.createWindowContext(display, 2024, null)
                val wm = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    2024,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                    android.graphics.PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                    title = "DynamicIslandOverlay"
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                }

                val controller = IslandController(windowContext)
                val islandView = controller.createIslandView(wm, params)
                wm.addView(islandView, params)
                logOk("DynamicIslandView injected into WindowManager")
            } catch (e: Exception) {
                logError("Island injection failed", e)
            }
        }, 2000L) // 2s — safer than 1.5s on slower devices
    }
}
