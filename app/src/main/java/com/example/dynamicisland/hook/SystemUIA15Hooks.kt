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
        
        // 🚀 PROPER HOOK: Inject into native SystemUI Hierarchy
        injectIslandNative(lpparam)

        suppressVolumeDialog(lpparam)
    }

    private fun injectIslandNative(lpparam: XC_LoadPackage.LoadPackageParam) {
        val shadeWindowClasses = listOf(
            "com.android.systemui.shade.NotificationShadeWindowView",
            "com.android.systemui.statusbar.phone.NotificationShadeWindowView"
        )
        
        var injected = false

        for (cls in shadeWindowClasses) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            
            XposedBridge.hookAllMethods(clazz, "onAttachedToWindow", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (injected) return
                    val root = param.thisObject as? android.view.ViewGroup ?: return
                    val context = root.context
                    
                    XposedBridge.log("DynamicIsland ✅: Injecting into $cls native hierarchy")
                    
                    try {
                        val settingsManager = com.example.dynamicisland.settings.SettingsManager(context)
                        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
                        val mediaManager = com.example.dynamicisland.manager.IslandMediaManager(
                            context = context,
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
                        val hapticsManager = com.example.dynamicisland.manager.IslandHapticsManager(context, settingsManager)
                        val networkMonitor = com.example.dynamicisland.manager.IslandNetworkMonitor()
                        
                        val controller = com.example.dynamicisland.manager.IslandController(
                            context, 
                            settingsManager, 
                            mediaManager, 
                            hardwareMonitor,
                            eventBus,
                            hapticsManager,
                            networkMonitor
                        )

                        val islandView = controller.createIslandView(
                            context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager,
                            null // No specific WindowManager params needed for child view
                        )

                        val lp = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        root.addView(islandView, lp)
                        injected = true
                    } catch (e: Exception) {
                        XposedBridge.log("DynamicIsland ❌: Native injection failed — ${e.message}")
                    }
                }
            })
            XposedBridge.log("$TAG ✅: shadeWindowClasses hooked: $cls")
            break
        }
    }

    private fun suppressVolumeDialog(lpparam: XC_LoadPackage.LoadPackageParam) {
        val candidates = listOf(
            "com.android.systemui.volume.VolumeDialogImpl",
            "com.android.systemui.volume.VolumeDialog"
        )
        for (cls in candidates) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                XposedBridge.hookAllMethods(clazz, "show", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        val context = try {
                            XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                        } catch (e: Exception) { null } ?: android.app.AndroidAppHelper.currentApplication()
                        
                        context?.sendBroadcast(
                            Intent("com.example.dynamicisland.SHOW_VOLUME_MIXER").apply {
                                setPackage("com.android.systemui")
                            }
                        )
                        return null // Suppress original dialog
                    }
                })
                XposedBridge.log("$TAG ✅: VolumeDialog suppressed via $cls")
                break
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: VolumeDialog suppress $cls failed: ${e.message}")
            }
        }
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
