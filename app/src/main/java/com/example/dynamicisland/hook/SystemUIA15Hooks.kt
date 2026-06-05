package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.ipc.IslandIPCClient
import com.example.dynamicisland.model.ActivityType
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.settings.SettingsManager
import com.example.dynamicisland.settings.SettingsState
import com.example.dynamicisland.ui.DynamicIslandView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.lang.ref.WeakReference

import com.example.dynamicisland.util.XposedExtensions

class SystemUIA15Hooks {
    companion object {
        private const val TAG = "DynamicIsland-Native"
        private var controller: IslandController? = null
        
        private var topIslandViewRef: WeakReference<DynamicIslandView>? = null
        private var bottomIslandViewRef: WeakReference<DynamicIslandView>? = null
        
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private var flashlightController: Any? = null

        fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
            if (lpparam.packageName != "com.android.systemui") return
            
            XposedBridge.log("$TAG: 🚀 Initializing Dual-Window Island Architecture (Defensive Mode)...")

            XposedExtensions.hookMethodIfExists(
                "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
                lpparam.classLoader,
                "start",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = XposedExtensions.getObjectFieldSafe(param.thisObject, "mContext") as? Context
                        if (context != null) {
                            ensureControllerInitialized(context)
                            syncWindows(context)
                        }
                    }
                }
            )

            hookStatusBarViews(lpparam)
            hookNotifPipeline(lpparam)
            hookMediaPipeline(lpparam)
            hookHardwareControllers(lpparam)
            hookRecordingController(lpparam)
            hookAssistManager(lpparam)
            hookScreenshotService(lpparam)
            setupReceiver(lpparam)
        }

        private val statusBarViews = mutableSetOf<WeakReference<View>>()

        private fun hookStatusBarViews(lpparam: XC_LoadPackage.LoadPackageParam) {
            val clockClass = "com.android.systemui.statusbar.policy.Clock"
            val iconContainerClass = "com.android.systemui.statusbar.phone.NotificationIconContainer"
            
            val viewHook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val view = param.thisObject as View
                    statusBarViews.add(WeakReference(view))
                    updateStatusBarAlpha()
                }
            }

            XposedExtensions.hookMethodIfExists(clockClass, lpparam.classLoader, "onAttachedToWindow", viewHook)
            XposedExtensions.hookMethodIfExists(iconContainerClass, lpparam.classLoader, "onAttachedToWindow", viewHook)
            
            // Periodically check neural core state to sync alpha
            scope.launch {
                while(true) {
                    delay(500)
                    updateStatusBarAlpha()
                }
            }
        }

        private fun updateStatusBarAlpha() {
            val ctrl = controller ?: return
            val islandState = ctrl.neuralCore.uiState.value.islandState
            
            // Hide clock/icons if island is MID or MAX (expanded)
            val shouldHide = islandState == com.example.dynamicisland.ipc.IslandState.TYPE_2_MID || 
                            islandState == com.example.dynamicisland.ipc.IslandState.TYPE_3_MAX
            
            val targetAlpha = if (shouldHide) 0f else 1f
            
            Handler(Looper.getMainLooper()).post {
                statusBarViews.iterator().let { iter ->
                    while (it.hasNext()) {
                        val ref = it.next()
                        val view = ref.get()
                        if (view == null) {
                            it.remove()
                        } else if (view.alpha != targetAlpha) {
                            view.animate().alpha(targetAlpha).setDuration(200).start()
                        }
                    }
                }
            }
        }

        private fun ensureControllerInitialized(context: Context) {
            if (controller != null) return
            
            try {
                // 🛠️ MANDATORY: Initialize ML Kit for SystemUI process
                try {
                    com.google.mlkit.common.sdkinternal.MlKitContext.initializeIfNeeded(context)
                } catch (e: Exception) {
                    XposedBridge.log("$TAG ⚠️: ML Kit initialization failed or already done: ${e.message}")
                }

                val settingsManager = SettingsManager(context)
                val hapticsManager = IslandHapticsManager(context, settingsManager)
                val networkMonitor = IslandNetworkMonitor()
                val mediaManager = IslandMediaManager(context, scope)
                val hardwareMonitor = IslandHardwareMonitor(context, scope)
                val neuralCore = IslandNeuralCore(context)
                val ipcClient: IslandIPCClient = IslandIPCClient.get(context)
                val backupManager = IslandBackupManager(context, ipcClient)
                val locationManager = IslandLocationManager(context)

                // 📸 CONTINUITY CAMERA (QR SCANNER)
                val cameraScanner = ContinuityCameraScanner(context)

                controller = IslandController(
                    context, settingsManager, mediaManager, hardwareMonitor,
                    hapticsManager, networkMonitor, neuralCore,
                    backupManager, locationManager
                ).apply {
                    start(context)
                    if (settingsState.barcode) cameraScanner.start()
                }
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ❌: Controller bootstrap failed: ${e.message}")
            }
        }

        private fun syncWindows(context: Context) {
            val ctrl = controller ?: return
            val settings = ctrl.settingsState
            
            Handler(Looper.getMainLooper()).post {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                
                // 1. REDWOOD ISLAND (TOP)
                if (settings.islandEnabled && settings.redwoodEnabled) {
                    addOrUpdateWindow(context, wm, isTop = true)
                } else {
                    removeWindow(wm, isTop = true)
                }

                // 2. NAV ISLAND (BOTTOM)
                if (settings.islandEnabled && settings.navIslandMode) {
                    addOrUpdateWindow(context, wm, isTop = false)
                } else {
                    removeWindow(wm, isTop = false)
                }
            }
        }

        private fun addOrUpdateWindow(context: Context, wm: WindowManager, isTop: Boolean) {
            val ctrl = controller ?: return
            val ref = if (isTop) topIslandViewRef else bottomIslandViewRef
            val existingView = ref?.get()
            
            if (existingView != null) {
                if (existingView.isAttachedToWindow) return
                // Zombie window detected: reference exists but not attached. Clean up.
                try { wm.removeViewImmediate(existingView) } catch (_: Exception) {}
            }

            try {
                val moduleContext = try { 
                    context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) 
                } catch (e: Exception) { context }

                val view = DynamicIslandView(context, moduleContext)
                if (isTop) {
                    ctrl.createIslandView(view)
                    topIslandViewRef = WeakReference(view)
                } else {
                    bottomIslandViewRef = WeakReference(view)
                }

                val density = context.resources.displayMetrics.density
                val height = (320 * density).toInt()

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    height,
                    2017, // TYPE_STATUS_BAR_SUB_PANEL
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = if (isTop) Gravity.TOP or Gravity.CENTER_HORIZONTAL else Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    title = if (isTop) "RedwoodIslandTop" else "NavIslandBottom"
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                    
                    try {
                        val field = WindowManager.LayoutParams::class.java.getField("privateFlags")
                        field.set(this, (field.get(this) as Int) or 0x00000040)
                    } catch (e: Exception) {}
                }

                wm.addView(view, params)
                XposedBridge.log("$TAG ✅: Added ${params.title}")
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ❌: Failed to add window: ${e.message}")
            }
        }

        private fun removeWindow(wm: WindowManager, isTop: Boolean) {
            val ref = if (isTop) topIslandViewRef else bottomIslandViewRef
            ref?.get()?.let {
                try { 
                    if (it.isAttachedToWindow) wm.removeViewImmediate(it) 
                } catch (e: Exception) {}
                if (isTop) topIslandViewRef = null else bottomIslandViewRef = null
            }
        }

        private fun setupReceiver(lpparam: XC_LoadPackage.LoadPackageParam) {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.SystemUIService",
                lpparam.classLoader,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.thisObject as Context
                        val filter = android.content.IntentFilter().apply {
                            addAction("com.example.dynamicisland.RE_INJECT")
                            addAction("com.example.dynamicisland.SETTINGS_UPDATED")
                        }
                        context.registerReceiver(object : android.content.BroadcastReceiver() {
                            override fun onReceive(c: Context, intent: Intent) {
                                when (intent.action) {
                                    "com.example.dynamicisland.SETTINGS_UPDATED",
                                    "com.example.dynamicisland.RE_INJECT" -> {
                                        controller?.loadAndApplySettings()
                                        syncWindows(context)
                                    }
                                }
                            }
                        }, filter, Context.RECEIVER_EXPORTED)
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                "com.android.systemui.SystemUIService",
                lpparam.classLoader,
                "onDestroy",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        XposedBridge.log("$TAG: SystemUIService destroying, cleaning up controller")
                        controller?.destroy()
                        controller = null
                    }
                }
            )
        }

        private fun hookNotifPipeline(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                // 🎯 Direct NotifCollection hook is more robust than NotifPipeline listener
                val collectionClass = "com.android.systemui.statusbar.notification.collection.NotifCollection"
                IslandHookEngine.hookAfterAllOverloads(collectionClass, lpparam.classLoader, "dispatchOnEntryAdded") { param ->
                    val entry = param.args.firstOrNull() ?: return@hookAfterAllOverloads
                    val sbn = XposedHelpers.callMethod(entry, "getSbn") as android.service.notification.StatusBarNotification
                    controller?.notificationManager?.processIncomingNotification(sbn.packageName, sbn.notification)
                }
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: NotifCollection hook failed, falling back to pipeline listener")
                try {
                    val pipelineClass = "com.android.systemui.statusbar.notification.collection.NotifPipeline"
                    IslandHookEngine.hookAllMethodsByName(pipelineClass, lpparam.classLoader, "addCollectionListener", object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val listener = param.args.firstOrNull() ?: return
                            // Only hook if the method exists on this specific listener instance class
                            if (XposedHelpers.findMethodExactIfExists(listener.javaClass, "onEntryAdded", "com.android.systemui.statusbar.notification.collection.NotificationEntry") != null) {
                                IslandHookEngine.hookAllMethodsByName(listener.javaClass.name, lpparam.classLoader, "onEntryAdded", object : XC_MethodHook() {
                                    override fun afterHookedMethod(innerParam: MethodHookParam) {
                                        val entry = innerParam.args.firstOrNull() ?: return
                                        val sbn = XposedHelpers.callMethod(entry, "getSbn") as android.service.notification.StatusBarNotification
                                        controller?.notificationManager?.processIncomingNotification(sbn.packageName, sbn.notification)
                                    }
                                })
                            }
                        }
                    })
                } catch (e2: Throwable) {}
            }
        }

        private fun hookMediaPipeline(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                val dataManagerClass = "com.android.systemui.media.controls.domain.pipeline.LegacyMediaDataManagerImpl"
                IslandHookEngine.hookAllMethodsByName(dataManagerClass, lpparam.classLoader, "onMediaDataLoaded", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val data = param.args.getOrNull(2) ?: return
                        controller?.let { ctrl ->
                            val pkg = XposedHelpers.getObjectField(data, "packageName") as? String ?: return
                            val song = XposedHelpers.getObjectField(data, "song") as? CharSequence ?: ""
                            val artist = XposedHelpers.getObjectField(data, "artist") as? CharSequence ?: ""
                            val isPlaying = XposedHelpers.getBooleanField(data, "isPlaying")
                            ctrl.mediaManager.updateMediaFromNative(pkg, song.toString(), artist.toString(), null, isPlaying)
                        }
                    }
                })
            } catch (e: Throwable) {}
        }

        private fun hookHardwareControllers(lpparam: XC_LoadPackage.LoadPackageParam) { /* ... */ }
        private fun hookRecordingController(lpparam: XC_LoadPackage.LoadPackageParam) { /* ... */ }
        private fun hookAssistManager(lpparam: XC_LoadPackage.LoadPackageParam) { /* ... */ }
        private fun hookScreenshotService(lpparam: XC_LoadPackage.LoadPackageParam) { /* ... */ }
    }
}
