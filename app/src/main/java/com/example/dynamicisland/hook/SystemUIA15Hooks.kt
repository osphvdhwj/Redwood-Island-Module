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
import android.widget.FrameLayout
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.ActivityType
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.settings.SettingsManager
import com.example.dynamicisland.ui.DynamicIslandView
import com.example.dynamicisland.ui.mvi.IslandEventBus
import de.robv.android.xposed.IXposedHookLoadPackage
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
        private var islandViewRef: WeakReference<DynamicIslandView>? = null
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        
        private var flashlightController: Any? = null
        private var isWindowAdded = false

        fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
            if (lpparam.packageName != "com.android.systemui") return
            
            XposedBridge.log("$TAG: 🚀 Initializing Pro-Grade SystemUI Integration (Window Overlay Mode)...")

            // ── 0. Core Engine Initialization ─────────────────────────────────
            try {
                // Hook CentralSurfacesImpl.start() to initialize the controller brain
                XposedHelpers.findAndHookMethod(
                    "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
                    lpparam.classLoader,
                    "start",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                            ensureControllerInitialized(context)
                            
                            // Once brain is ready, we can add our window
                            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                            addIslandWindow(context, wm)
                        }
                    }
                )
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: CentralSurfaces hook failed, falling back to Service hook")
            }

            // Fallback for earlier initialization if CentralSurfaces fails
            try {
                XposedHelpers.findAndHookMethod(
                    "com.android.systemui.SystemUIService",
                    lpparam.classLoader,
                    "onCreate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val context = param.thisObject as Context
                            ensureControllerInitialized(context)
                        }
                    }
                )
            } catch (e: Throwable) {}

            // ── 1. Native Data Pipelines ──────────────────────────────────────
            hookNotifPipeline(lpparam)
            hookMediaPipeline(lpparam)
            hookHardwareControllers(lpparam)
            hookRecordingController(lpparam)
            hookAssistManager(lpparam)
            hookScreenshotService(lpparam)
            
            // ── 2. Recovery & External Triggers ───────────────────────────────
            setupReceiver(lpparam)
        }

        private fun ensureControllerInitialized(context: Context) {
            if (controller != null) return
            try {
                XposedBridge.log("$TAG: Powering up the Island Engine Brain...")
                val eventBus = IslandEventBus()
                val settingsManager = SettingsManager(context)
                val hapticsManager = IslandHapticsManager(context, settingsManager)
                val networkMonitor = IslandNetworkMonitor()
                val mediaManager = IslandMediaManager(context, scope)
                val hardwareMonitor = IslandHardwareMonitor(scope)

                controller = IslandController(
                    context,
                    settingsManager,
                    mediaManager,
                    hardwareMonitor,
                    eventBus,
                    hapticsManager,
                    networkMonitor
                )
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ❌: Controller bootstrap failed: ${e.message}")
            }
        }

        private fun addIslandWindow(context: Context, wm: WindowManager) {
            if (isWindowAdded) return
            
            Handler(Looper.getMainLooper()).post {
                try {
                    val ctrl = controller ?: return@post
                    
                    val moduleContext = try { 
                        context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) 
                    } catch (e: Exception) { context }

                    val islandView = DynamicIslandView(context, moduleContext)
                    ctrl.islandView = islandView
                    islandViewRef = WeakReference(islandView)

                    // 💎 PRO-GRADE OVERLAY PARAMETERS
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        (320 * context.resources.displayMetrics.density).toInt(), // Constrained height (320dp)
                        2017, // WindowManager.LayoutParams.TYPE_STATUS_BAR_SUB_PANEL
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        title = "RedwoodDynamicIsland"
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                        }
                        // Ensure it bypasses MIUI's aggressive status bar clipping
                        XposedHelpers.setObjectField(this, "privateFlags", 
                            (XposedHelpers.getIntField(this, "privateFlags") or 0x00000040)) // PRIVATE_FLAG_TRUSTED_OVERLAY
                    }

                    wm.addView(islandView, params)
                    isWindowAdded = true
                    XposedBridge.log("$TAG ✅: Pro-Grade Global Overlay Window added successfully")
                } catch (e: Throwable) {
                    XposedBridge.log("$TAG ❌: Window creation failed — ${e.message}")
                }
            }
        }

        private fun hookHardwareControllers(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                val flashlightClass = "com.android.systemui.statusbar.policy.FlashlightControllerImpl"
                IslandHookEngine.hookAllMethodsByName(flashlightClass, lpparam.classLoader, "setFlashlight", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        flashlightController = param.thisObject
                        val enabled = param.args.getOrNull(0) as? Boolean ?: return
                        controller?.postTransientNotification(
                            LiveActivityModel.General(
                                id = "sys_torch", type = ActivityType.HARDWARE,
                                title = if (enabled) "Flashlight On" else "Flashlight Off",
                                dataText = if (enabled) "System torch active" else "Torch disabled",
                                accentColor = if (enabled) android.graphics.Color.YELLOW else android.graphics.Color.GRAY
                            ), 2500L
                        )
                    }
                })
            } catch (e: Throwable) {}
        }

        private fun hookRecordingController(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                val recordClass = "com.android.systemui.screenrecord.RecordingController"
                IslandHookEngine.hookAllMethodsByName(recordClass, lpparam.classLoader, "updateState", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val isRecording = XposedHelpers.callMethod(param.thisObject, "isRecording") as Boolean
                        val isStarting = XposedHelpers.callMethod(param.thisObject, "isStarting") as Boolean
                        controller?.setSystemRecordingState(isRecording || isStarting)
                    }
                })
            } catch (e: Throwable) {}
        }

        private fun hookAssistManager(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                val assistClass = "com.android.systemui.assist.AssistManager"
                XposedHelpers.findAndHookMethod(assistClass, lpparam.classLoader, "onInvocationProgress", Int::class.javaPrimitiveType, Float::class.javaPrimitiveType, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val progress = param.args[1] as Float
                        controller?.triggerAssistantAura(progress)
                    }
                })
                
                XposedHelpers.findAndHookMethod(assistClass, lpparam.classLoader, "startAssist", android.os.Bundle::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (controller?.interceptAssistant() == true) {
                            param.result = null 
                        }
                    }
                })
            } catch (e: Throwable) {}
        }

        private fun hookScreenshotService(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                val screenClass = "com.android.systemui.screenshot.TakeScreenshotService"
                XposedHelpers.findAndHookMethod(screenClass, lpparam.classLoader, "handleMessage", android.os.Message::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        controller?.setSystemScreenshotActive(true)
                    }
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Handler(Looper.getMainLooper()).postDelayed({ controller?.setSystemScreenshotActive(false) }, 1000)
                    }
                })
            } catch (e: Throwable) {}
        }

        private fun setupReceiver(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                XposedHelpers.findAndHookMethod(
                    "com.android.systemui.SystemUIService",
                    lpparam.classLoader,
                    "onCreate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val context = param.thisObject as Context
                            val filter = android.content.IntentFilter().apply {
                                addAction("com.example.dynamicisland.RE_INJECT")
                                addAction("com.example.dynamicisland.DEBUG_TEST")
                            }
                            context.registerReceiver(object : android.content.BroadcastReceiver() {
                                override fun onReceive(c: Context, intent: Intent) {
                                    XposedBridge.log("$TAG: System Hook Broadcast -> ${intent.action}")
                                    if (intent.action == "com.example.dynamicisland.RE_INJECT") {
                                        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                                        val currentView = islandViewRef?.get()
                                        if (currentView != null) {
                                            try { wm.removeView(currentView) } catch (e: Exception) {}
                                        }
                                        isWindowAdded = false
                                        addIslandWindow(context, wm)
                                    } else if (intent.action == "com.example.dynamicisland.DEBUG_TEST") {
                                        controller?.postTransientNotification(
                                            LiveActivityModel.General(
                                                id = "debug_test", type = ActivityType.MESSAGE,
                                                title = "System Integration Active",
                                                dataText = "Global Window Rendering Confirmed",
                                                accentColor = android.graphics.Color.GREEN
                                            ), 10000L
                                        )
                                    }
                                }
                            }, filter, Context.RECEIVER_EXPORTED)
                        }
                    }
                )
            } catch (e: Throwable) {}
        }

        private fun hookNotifPipeline(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                val pipelineClass = "com.android.systemui.statusbar.notification.collection.NotifPipeline"
                IslandHookEngine.hookAllMethodsByName(pipelineClass, lpparam.classLoader, "addCollectionListener", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val listener = param.args.firstOrNull() ?: return
                        IslandHookEngine.hookAllMethodsByName(listener.javaClass.name, lpparam.classLoader, "onEntryAdded", object : XC_MethodHook() {
                            override fun afterHookedMethod(innerParam: MethodHookParam) {
                                val entry = innerParam.args.firstOrNull() ?: return
                                processNativeNotification(entry)
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
                        if (data.javaClass.name.contains("MediaData")) processNativeMedia(data)
                    }
                })
            } catch (e: Throwable) {}
        }

        private fun processNativeNotification(entry: Any) {
            try {
                val sbn = XposedHelpers.callMethod(entry, "getSbn") as android.service.notification.StatusBarNotification
                controller?.notificationManager?.processIncomingNotification(sbn.packageName, sbn.notification)
            } catch (e: Throwable) {}
        }

        private fun processNativeMedia(mediaData: Any) {
            try {
                controller?.let { ctrl ->
                    val pkg = XposedHelpers.getObjectField(mediaData, "packageName") as? String ?: return
                    val song = XposedHelpers.getObjectField(mediaData, "song") as? CharSequence ?: ""
                    val artist = XposedHelpers.getObjectField(mediaData, "artist") as? CharSequence ?: ""
                    val artworkIcon = XposedHelpers.getObjectField(mediaData, "artwork") as? android.graphics.drawable.Icon
                    val isPlaying = XposedHelpers.getBooleanField(mediaData, "isPlaying")
                    
                    val artworkBmp = artworkIcon?.let { decodeIcon(it, ctrl.context) }
                    ctrl.mediaManager.updateMediaFromNative(pkg, song.toString(), artist.toString(), artworkBmp, isPlaying)
                }
            } catch (e: Throwable) {}
        }

        private fun decodeIcon(icon: android.graphics.drawable.Icon, context: Context): android.graphics.Bitmap? {
            return try {
                val drawable = icon.loadDrawable(context) ?: return null
                val bitmap = android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height); drawable.draw(canvas)
                bitmap
            } catch (e: Exception) { null }
        }
        
        fun toggleSystemFlashlight() {
            flashlightController?.let {
                try {
                    val currentState = XposedHelpers.callMethod(it, "isEnabled") as Boolean
                    XposedHelpers.callMethod(it, "setFlashlight", !currentState)
                } catch (e: Exception) {}
            }
        }
    }
}
