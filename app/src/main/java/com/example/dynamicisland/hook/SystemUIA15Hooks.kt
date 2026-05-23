package com.example.dynamicisland.hook

import android.app.Application
import android.content.Context
import android.content.Intent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Consolidated SystemUI Hooks for Android 15 (A15)
 *
 * This file contains all hooks targeting the com.android.systemui package,
 * ensuring deep integration for the Dynamic Island overlay.
 */
object SystemUIA15Hooks {

    private const val TAG = "DynamicIsland-SystemUI"
    private var isPanelExpanded = false

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam) {
        // For Infinity X A15
        hookQSTileHost(lpparam)
        // For Infinity X A15
        suppressClipboardOverlay(lpparam)
        // For Infinity X A15
        suppressScreenshotNative(lpparam)
        // For Infinity X A15
        hookNotificationPanel(lpparam)
        // For Infinity X A15
        hookFlashlight(lpparam)
        // For Infinity X A15
        hookNotifications(lpparam)
        // For Infinity X A15
        hookMediaStates(lpparam)
        // For Infinity X A15
        injectIslandMultiStrategy(lpparam)
    }

    private fun hookEdgeLighting(classLoader: ClassLoader) {
        try {
            IslandHookEngine.hookAllConstructorsSafe(
                "com.android.systemui.statusbar.notification.row.wrapper.NotificationCompactHeadsUpTemplateViewWrapper",
                classLoader,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val view = XposedHelpers.getObjectField(param.thisObject, "mView") as? android.view.View
                                ?: return

                            val drawable = android.graphics.drawable.GradientDrawable().apply {
                                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                                cornerRadius = 48f
                                setColor(android.graphics.Color.TRANSPARENT)
                                setStroke(4, android.graphics.Color.parseColor("#00FFFF"))
                            }
                            view.foreground = drawable
                        } catch (e: Throwable) {
                            XposedBridge.log("DynamicIsland-CurrentRom ❌: Edge lighting application failed — ${e.message}")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG ✅: Edge lighting hook applied")
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland-CurrentRom ❌: hookEdgeLighting setup failed — ${e.message}")
        }
    }

    // ── 1. Island Injection Strategies ────────────────────────────────────────

    private fun injectIslandMultiStrategy(lpparam: XC_LoadPackage.LoadPackageParam) {
        var injected = false

        fun inject(ctx: Context, via: String) {
            if (injected) return
            injected = true
            XposedBridge.log("DynamicIsland ✅: Island injection triggered via: $via")
            injectDynamicIsland(ctx)
        }

        // ── Strategy 1: CentralSurfacesImpl.start ────────────────────────────
        // Confirmed present on Evolution X via KeyguardViewController.smali line 63
        // Also present on CrDroid and Infinity X //For Infinity X A15
        val statusBarClasses = listOf(
            "com.android.systemui.statusbar.phone.CentralSurfacesImpl", // A13+ all ROMs
            "com.android.systemui.statusbar.phone.PhoneStatusBar",      // older
            "com.android.systemui.statusbar.phone.StatusBar",           // legacy
        )
        for (cls in statusBarClasses) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                XposedBridge.hookAllMethods(clazz, "start", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // Try mContext field first, then AndroidAppHelper
                        val ctx = try {
                            XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                        } catch (_: Throwable) { null }
                            ?: try {
                                android.app.AndroidAppHelper.currentApplication()
                            } catch (_: Throwable) { null }
                            ?: return
                        inject(ctx, "Strategy1/$cls.start")
                    }
                })
                XposedBridge.log("$TAG ✅: Strategy1 hooked: $cls.start")
                break
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: Strategy1 $cls failed: ${e.message}")
            }
        }

        // ── Strategy 2: SystemUIApplication.onCreate ─────────────────────────
        // Confirmed present on Evolution X from SystemUIApplication.smali
        val appClasses = listOf(
            "com.android.systemui.SystemUIApplication",
            "com.android.systemui.SystemUIAppComponentFactory",
            "com.nothing.systemui.NothingSystemUIApplication",
        )
        for (cls in appClasses) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                XposedBridge.hookAllMethods(clazz, "onCreate", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Application ?: return
                        inject(app.applicationContext, "Strategy2/$cls.onCreate")
                    }
                })
                XposedBridge.log("$TAG ✅: Strategy2 hooked: $cls.onCreate")
                break
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: Strategy2 $cls failed: ${e.message}")
            }
        }

        // ── Strategy 3: Instrumentation.callApplicationOnCreate ───────────────
        try {
            IslandHookEngine.hookMethodSafe(
                "android.app.Instrumentation", lpparam.classLoader,
                "callApplicationOnCreate", Application::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.args[0] as? Application ?: return
                        if (app.packageName != "com.android.systemui") return
                        inject(app.applicationContext, "Strategy3/Instrumentation")
                    }
                }
            )
            XposedBridge.log("$TAG ✅: Strategy3 hooked: Instrumentation.callApplicationOnCreate")
        } catch (e: Throwable) {
            XposedBridge.log("$TAG ⚠️: Strategy3 failed: ${e.message}")
        }

        // ── Strategy 4: ActivityThread.handleBindApplication ──────────────────
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
                            inject(app.applicationContext, "Strategy4/ActivityThread")
                        }
                    }
                )
                XposedBridge.log("$TAG ✅: Strategy4 hooked: ActivityThread.handleBindApplication")
            }
        } catch (e: Throwable) {
            XposedBridge.log("$TAG ⚠️: Strategy4 failed: ${e.message}")
        }
    }

    private fun injectDynamicIsland(systemUiContext: Context) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val dm = systemUiContext.getSystemService(Context.DISPLAY_SERVICE)
                    as android.hardware.display.DisplayManager
                val display = dm.getDisplay(android.view.Display.DEFAULT_DISPLAY)

                val windowContext = systemUiContext.createWindowContext(display, 2024, null)
                val wm = windowContext.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager

                val params = android.view.WindowManager.LayoutParams(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    2024,
                    android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                    android.graphics.PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                    title = "DynamicIslandOverlay"
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                }

                val settingsManager = com.example.dynamicisland.settings.SettingsManager(windowContext)
                val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
                val mediaManager = com.example.dynamicisland.manager.IslandMediaManager(
                    context = windowContext,
                    scope = scope,
                    onMediaChanged = {},
                    onMediaTick = {},
                    onPeekRequested = {},
                    onPauseFadeRequested = {},
                    onUncollapseRequested = {}
                )
                val hardwareMonitor = com.example.dynamicisland.manager.IslandHardwareMonitor(
                    scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob()),
                    onHardwareUpdate = {}
                )
                val eventBus = com.example.dynamicisland.ui.state.IslandEventBus()
                val hapticsManager = com.example.dynamicisland.manager.IslandHapticsManager(windowContext)
                val networkMonitor = com.example.dynamicisland.manager.IslandNetworkMonitor()
                
                val controller = com.example.dynamicisland.manager.IslandController(
                    windowContext, 
                    settingsManager, 
                    mediaManager, 
                    hardwareMonitor,
                    eventBus,
                    hapticsManager,
                    networkMonitor
                )
                val islandView = controller.createIslandView(wm, params)
                wm.addView(islandView, params)
                XposedBridge.log("$TAG ✅: DynamicIslandView injected into WindowManager")
            } catch (e: Exception) {
                XposedBridge.log("$TAG ❌: Island injection failed — ${e.message}")
            }
        }, 2000L)
    }

    // ── 2. QS Tile Host ───────────────────────────────────────────────────────

    private fun hookQSTileHost(lpparam: XC_LoadPackage.LoadPackageParam) {
        val qsClasses = listOf(
            "com.android.systemui.qs.QSTileHost",
            "com.android.systemui.qs.tileimpl.QSTileHost"
        )
        for (cls in qsClasses) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                XposedBridge.hookAllConstructors(clazz, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        SystemUIContextKeeper.qsTileHost = param.thisObject
                        XposedBridge.log("$TAG ✅: QSTileHost captured via $cls")
                    }
                })
                break
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: QSTileHost hook failed for $cls: ${e.message}")
            }
        }
    }

    // ── 2. UI Suppression ─────────────────────────────────────────────────────

    private fun suppressClipboardOverlay(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz = XposedHelpers.findClassIfExists(
            "com.android.systemui.clipboardoverlay.ClipboardListener", lpparam.classLoader
        ) ?: return
        try {
            XposedHelpers.findAndHookMethod(clazz, "start", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? = null
            })
            XposedBridge.log("$TAG ✅: ClipboardListener suppressed")
        } catch (e: Throwable) {
            XposedBridge.log("$TAG ⚠️: ClipboardListener suppress failed: ${e.message}")
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
                        val result = param.args.firstOrNull() ?: return
                        val uri = try {
                            XposedHelpers.getObjectField(result, "uri")?.toString() ?: ""
                        } catch (_: Throwable) { "" }
                        
                        val context = android.app.AndroidAppHelper.currentApplication()
                        context?.sendBroadcast(
                            Intent("com.example.dynamicisland.SCREENSHOT_CAUGHT").apply {
                                setPackage("com.android.systemui")
                                putExtra("uri", uri)
                            }
                        )
                        param.result = null
                    }
                })
                XposedBridge.log("$TAG ✅: Screenshot suppressed via $cls.$method")
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: Screenshot suppress $cls.$method failed: ${e.message}")
            }
        }
    }

    // ── 3. State Tracking ─────────────────────────────────────────────────────

    private fun hookNotificationPanel(lpparam: XC_LoadPackage.LoadPackageParam) {
        IslandHookEngine.hookAfter(
            "com.android.systemui.shade.NotificationPanelViewController",
            lpparam.classLoader,
            "setExpandedFraction",
            Float::class.javaPrimitiveType!!
        ) { param ->
            val fraction = param.args[0] as Float
            val isCurrentlyExpanded = fraction > 0.05f

            if (isCurrentlyExpanded != isPanelExpanded) {
                isPanelExpanded = isCurrentlyExpanded
                
                val context: Context? = try {
                    val mView = XposedHelpers.getObjectField(param.thisObject, "mView") as? android.view.View
                    mView?.context
                } catch (_: Throwable) {
                    null
                } ?: try {
                    XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                } catch (_: Throwable) {
                    null
                } ?: android.app.AndroidAppHelper.currentApplication()

                context?.sendBroadcast(
                    Intent("com.example.dynamicisland.PANEL_STATE_CHANGED").apply {
                        setPackage("com.android.systemui")
                        putExtra("isExpanded", isPanelExpanded)
                    }
                )
            }
        }
    }

    private fun hookFlashlight(lpparam: XC_LoadPackage.LoadPackageParam) {
        IslandHookEngine.hookAfter(
            "com.android.systemui.statusbar.policy.FlashlightControllerImpl",
            lpparam.classLoader,
            "setFlashlight",
            Boolean::class.javaPrimitiveType!!
        ) { param ->
            val isEnabled = param.args[0] as Boolean
            
            val context: Context? = try {
                XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
            } catch (_: Throwable) {
                null
            } ?: android.app.AndroidAppHelper.currentApplication()

            context?.sendBroadcast(
                Intent("com.example.dynamicisland.HARDWARE_TOGGLE").apply {
                    setPackage("com.android.systemui")
                    putExtra("type", "TORCH")
                    putExtra("state", if (isEnabled) 1 else 0)
                }
            )
        }
    }

    // ── 4. Notification & Media Management ────────────────────────────────────

    private fun hookNotifications(lpparam: XC_LoadPackage.LoadPackageParam) {
        val callback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val sbn = param.args.firstOrNull { it?.javaClass?.simpleName == "StatusBarNotification" }
                    if (sbn != null) {
                        val pkg = XposedHelpers.callMethod(sbn, "getPackageName") as? String ?: ""
                        val notif = XposedHelpers.callMethod(sbn, "getNotification") as? android.app.Notification
                        val extras = notif?.extras
                        val title = extras?.getString(android.app.Notification.EXTRA_TITLE) ?: ""
                        val text = extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
                        
                        val context = android.app.AndroidAppHelper.currentApplication()
                        context?.sendBroadcast(
                            Intent("com.example.dynamicisland.NOTIFICATION_CAUGHT").apply {
                                setPackage("com.android.systemui")
                                putExtra("title", title)
                                putExtra("text", text)
                                putExtra("pkg", pkg)
                                putExtra("notification", notif)
                            }
                        )
                    }
                } catch (_: Throwable) {}
            }
        }

        // 🚨 Android 15+ Resilience: Try multiple candidates for notification entry points
        IslandHookEngine.hookFirstMatch(lpparam.classLoader, listOf(
            "com.android.systemui.statusbar.notification.collection.NotifCollection" to "dispatchPostNotification",
            "com.android.systemui.statusbar.notification.collection.NotifCollection" to "onNotificationPosted",
            "com.android.systemui.statusbar.notification.NotificationEntryManager" to "addNotification",
            "com.android.systemui.statusbar.policy.HeadsUpManager" to "showNotification"
        ), callback)
    }

    private fun hookMediaStates(lpparam: XC_LoadPackage.LoadPackageParam) {
        val mediaCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val context = android.app.AndroidAppHelper.currentApplication()
                context?.sendBroadcast(
                    Intent("com.example.dynamicisland.MEDIA_STATE_CHANGED").apply {
                        setPackage("com.android.systemui")
                    }
                )
            }
        }

        // 🚨 Android 15+ Resilience: Try multiple candidates for MediaDataManager
        IslandHookEngine.hookFirstMatch(lpparam.classLoader, listOf(
            "com.android.systemui.media.controls.pipeline.MediaDataManager" to "onMediaDataLoaded",
            "com.android.systemui.media.controls.domain.pipeline.MediaDataManager" to "onMediaDataLoaded",
            "com.android.systemui.media.MediaDataManager" to "onMediaDataLoaded"
        ), mediaCallback)

        IslandHookEngine.hookFirstMatch(lpparam.classLoader, listOf(
            "com.android.systemui.statusbar.NotificationMediaManager" to "updatePlaybackState",
            "com.android.systemui.statusbar.NotificationMediaManager" to "onPlaybackStateChanged"
        ), mediaCallback)
    }
}
