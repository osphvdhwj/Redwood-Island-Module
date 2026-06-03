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
import com.example.dynamicisland.model.ActivityType
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.settings.SettingsManager
import com.example.dynamicisland.settings.SettingsState
import com.example.dynamicisland.ui.DynamicIslandView
import com.example.dynamicisland.ui.mvi.IslandEventBus
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.lang.ref.WeakReference

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
            
            XposedBridge.log("$TAG: 🚀 Initializing Dual-Window Island Architecture...")

            try {
                XposedHelpers.findAndHookMethod(
                    "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
                    lpparam.classLoader,
                    "start",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                            ensureControllerInitialized(context)
                            syncWindows(context)
                        }
                    }
                )
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: CentralSurfaces hook failed")
            }

            hookNotifPipeline(lpparam)
            hookMediaPipeline(lpparam)
            hookHardwareControllers(lpparam)
            hookRecordingController(lpparam)
            hookAssistManager(lpparam)
            hookScreenshotService(lpparam)
            setupReceiver(lpparam)
        }

        private fun ensureControllerInitialized(context: Context) {
            if (controller != null) return
            
            try {
                val eventBus = IslandEventBus()
                val settingsManager = SettingsManager(context)
                val hapticsManager = IslandHapticsManager(context, settingsManager)
                val networkMonitor = IslandNetworkMonitor()
                val mediaManager = IslandMediaManager(context, scope)
                val hardwareMonitor = IslandHardwareMonitor(context, scope)
                val neuralCore = IslandNeuralCore(context)
                val ipcClient = IslandIPCClient.get(context)
                val backupManager = IslandBackupManager(context, ipcClient)
                val locationManager = IslandLocationManager(context)

                // 📸 CONTINUITY CAMERA (QR SCANNER)
                val cameraScanner = ContinuityCameraScanner(context)

                controller = IslandController(
                    context, settingsManager, mediaManager, hardwareMonitor,
                    eventBus, hapticsManager, networkMonitor, neuralCore,
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
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            Handler(Looper.getMainLooper()).post {
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
            val settings = ctrl.settingsState
            val ref = if (isTop) topIslandViewRef else bottomIslandViewRef
            
            if (ref?.get() != null) return // Already exists

            try {
                val moduleContext = try { 
                    context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) 
                } catch (e: Exception) { context }

                val view = DynamicIslandView(context, moduleContext)
                // Note: In a real multi-window setup, the controller might need to distinguish
                // which view it's talking to. For now, we sync state to both.
                if (isTop) {
                    ctrl.createIslandView(view)
                    topIslandViewRef = WeakReference(view)
                } else {
                    // bottom view might need separate registration if logic diverges
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
                    
                    // Mark as trusted to bypass touch occlusion
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
                try { wm.removeView(it) } catch (e: Exception) {}
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
        }

        // ... (Remaining hook methods stay the same, but use controller safely)
        private fun hookNotifPipeline(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                val pipelineClass = "com.android.systemui.statusbar.notification.collection.NotifPipeline"
                IslandHookEngine.hookAllMethodsByName(pipelineClass, lpparam.classLoader, "addCollectionListener", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val listener = param.args.firstOrNull() ?: return
                        IslandHookEngine.hookAllMethodsByName(listener.javaClass.name, lpparam.classLoader, "onEntryAdded", object : XC_MethodHook() {
                            override fun afterHookedMethod(innerParam: MethodHookParam) {
                                val entry = innerParam.args.firstOrNull() ?: return
                                val sbn = XposedHelpers.callMethod(entry, "getSbn") as android.service.notification.StatusBarNotification
                                controller?.notificationManager?.processIncomingNotification(sbn.packageName, sbn.notification)
                            }
                        })
                    }
                })
            } catch (e: Throwable) {}
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
