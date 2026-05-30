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

        fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
            if (lpparam.packageName != "com.android.systemui") return
            
            XposedBridge.log("$TAG: 🚀 Initializing Pro-Grade SystemUI Integration (Enhanced Reliability)...")

            // ── 0. Brain Initialization ───────────────────────────────────────
            try {
                XposedHelpers.findAndHookMethod(
                    "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
                    lpparam.classLoader,
                    "start",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                            ensureControllerInitialized(context)
                        }
                    }
                )
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: CentralSurfaces hook failed: ${e.message}")
            }

            // ── 1. Multiple UI Injection Points ───────────────────────────────
            
            // Primary: Status Bar View
            try {
                XposedHelpers.findAndHookMethod(
                    "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                    lpparam.classLoader,
                    "onAttachedToWindow",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            injectIsland(param.thisObject as FrameLayout, "PhoneStatusBarView")
                        }
                    }
                )
            } catch (e: Throwable) {}

            // Secondary: Notification Shade Window (Ensures visibility during lockscreen/shade interaction)
            try {
                XposedHelpers.findAndHookMethod(
                    "com.android.systemui.shade.NotificationShadeWindowViewController",
                    lpparam.classLoader,
                    "onViewAttached",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val view = XposedHelpers.callMethod(param.thisObject, "getView") as? FrameLayout ?: return
                            injectIsland(view, "NotificationShadeWindow")
                        }
                    }
                )
            } catch (e: Throwable) {}

            // ── 2. Hardware Controller Hooks ──────────────────────────────────
            hookHardwareControllers(lpparam)

            // ── 3. Native Data Pipelines ──────────────────────────────────────
            hookNotifPipeline(lpparam)
            hookMediaPipeline(lpparam)
            
            // ── 4. Recovery Broadcast ──────────────────────────────────────────
            // Allows the settings app to trigger a re-injection if the view is lost
            setupRecoveryReceiver(lpparam)
        }

        private fun ensureControllerInitialized(context: Context) {
            if (controller != null) return
            try {
                XposedBridge.log("$TAG: Initializing Island Controller Engine...")
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
                XposedBridge.log("$TAG ❌: Controller init failed: ${e.message}")
            }
        }

        private fun injectIsland(root: FrameLayout, source: String) {
            val currentView = islandViewRef?.get()
            if (currentView != null && currentView.parent == root) return
            
            val context = root.context
            ensureControllerInitialized(context)
            
            XposedBridge.log("$TAG: Injecting Island from $source")

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val ctrl = controller ?: return@postDelayed
                    
                    val moduleContext = try { 
                        context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) 
                    } catch (e: Exception) { context }

                    val islandView = DynamicIslandView(context, moduleContext)
                    ctrl.islandView = islandView
                    islandViewRef = WeakReference(islandView)
                    
                    islandView.elevation = 1000f
                    islandView.translationZ = 1000f
                    
                    val lp = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    }
                    
                    unclipRecursive(root)
                    
                    // Remove old instance if exists in this root
                    for (i in 0 until root.childCount) {
                        val child = root.getChildAt(i)
                        if (child?.javaClass?.name?.contains("DynamicIslandView") == true) {
                            root.removeViewAt(i)
                            break
                        }
                    }
                    
                    root.addView(islandView, lp)
                    islandView.bringToFront()
                    
                    XposedBridge.log("$TAG ✅: Island active in $source hierarchy")
                } catch (e: Throwable) {
                    XposedBridge.log("$TAG ❌: Injection failed — ${e.message}")
                }
            }, 1000)
        }

        private fun unclipRecursive(view: View?) {
            var current = view
            repeat(10) {
                (current as? ViewGroup)?.let {
                    it.clipChildren = false
                    it.clipToPadding = false
                    it.layoutTransition = null // Prevent system animations from hiding us
                }
                current = current?.parent as? View
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

        private fun setupRecoveryReceiver(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                XposedHelpers.findAndHookMethod(
                    "com.android.systemui.SystemUIService",
                    lpparam.classLoader,
                    "onCreate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val context = param.thisObject as Context
                            val filter = android.content.IntentFilter("com.example.dynamicisland.RE_INJECT")
                            context.registerReceiver(object : android.content.BroadcastReceiver() {
                                override fun onReceive(c: Context, intent: Intent) {
                                    XposedBridge.log("$TAG: Recovery broadcast received. Forcing re-injection.")
                                    // We can't easily find the root view from here, 
                                    // but we can clear the ref to allow next auto-injection
                                    islandViewRef = null
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
