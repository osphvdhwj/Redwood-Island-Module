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

class SystemUIA15Hooks {
    companion object {
        private const val TAG = "DynamicIsland-Native"
        private var controller: IslandController? = null
        private var injected = false
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
            if (lpparam.packageName != "com.android.systemui") return
            
            XposedBridge.log("$TAG: 🚀 Initializing Modern SystemUI Hooks (Direct Target Mode)...")

            // ── 1. Core UI Injection Point ────────────────────────────────────
            // We hook PhoneStatusBarView's onAttachedToWindow for reliable timing
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
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: PhoneStatusBarView hook failed: ${e.message}")
            }

            // ── 2. Native Data Pipelines (A15 Targets) ────────────────────────
            hookNotifPipeline(lpparam)
            hookMediaPipeline(lpparam)
        }

        private fun injectIsland(root: FrameLayout, source: String) {
            if (injected) return
            val context = root.context
            XposedBridge.log("$TAG: Injecting Island Overlay from $source")

            Handler(Looper.getMainLooper()).postDelayed({
                if (injected) return@postDelayed
                
                try {
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
                    ).also { ctrl ->
                        // Get the module context for resources
                        val moduleContext = try { 
                            context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) 
                        } catch (e: Exception) { context }

                        val islandView = DynamicIslandView(context, moduleContext)
                        ctrl.islandView = islandView
                        
                        // We add it directly to the root SystemUI view (PhoneStatusBarView)
                        // But we ensure it's not clipped and sits on top
                        val lp = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        ).apply {
                            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        }
                        
                        // Aggressive un-clipping up the hierarchy
                        var current: android.view.ViewParent? = root
                        repeat(5) {
                            (current as? ViewGroup)?.let {
                                it.clipChildren = false
                                it.clipToPadding = false
                            }
                            current = current?.parent
                        }
                        
                        root.addView(islandView, lp)
                        islandView.bringToFront()
                        
                        injected = true
                        XposedBridge.log("$TAG ✅: Island injected successfully into $source")
                    }
                } catch (e: Throwable) {
                    XposedBridge.log("$TAG ❌: Native injection failed — ${e.message}")
                    e.printStackTrace()
                }
            }, 1500)
        }

        private fun hookNotifPipeline(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                // Hook NotifPipeline to intercept new notifications
                val pipelineClass = "com.android.systemui.statusbar.notification.collection.NotifPipeline"
                
                IslandHookEngine.hookAllMethodsByName(
                    pipelineClass, 
                    lpparam.classLoader, 
                    "addCollectionListener", 
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val listener = param.args.firstOrNull() ?: return
                            
                            // Hook the listener's onEntryAdded method
                            IslandHookEngine.hookAllMethodsByName(listener.javaClass.name, lpparam.classLoader, "onEntryAdded", 
                                object : XC_MethodHook() {
                                    override fun afterHookedMethod(innerParam: MethodHookParam) {
                                        val entry = innerParam.args.firstOrNull() ?: return
                                        processNativeNotification(entry)
                                    }
                                }
                            )
                        }
                    }
                )
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: Failed to hook NotifPipeline: ${e.message}")
            }
        }

        private fun hookMediaPipeline(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                // In A14/A15, MediaDataManager implementation is LegacyMediaDataManagerImpl
                val dataManagerClass = "com.android.systemui.media.controls.domain.pipeline.LegacyMediaDataManagerImpl"

                IslandHookEngine.hookAllMethodsByName(
                    dataManagerClass,
                    lpparam.classLoader,
                    "onMediaDataLoaded",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            // Signature: onMediaDataLoaded(String, String, MediaData, boolean, int, boolean)
                            // MediaData is at index 2
                            val data = param.args.getOrNull(2) ?: return
                            if (data.javaClass.name.contains("MediaData")) {
                                processNativeMedia(data)
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: Failed to hook MediaPipeline: ${e.message}")
            }
        }

        private fun processNativeNotification(entry: Any) {
            try {
                // com.android.systemui.statusbar.notification.collection.NotificationEntry
                val sbn = XposedHelpers.callMethod(entry, "getSbn") as android.service.notification.StatusBarNotification
                val pkg = sbn.packageName
                val notification = sbn.notification
                
                controller?.let { ctrl ->
                    XposedBridge.log("$TAG: Native notification caught from $pkg")
                    ctrl.notificationManager.processIncomingNotification(pkg, notification)
                }
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ❌: processNativeNotification failed — ${e.message}")
            }
        }

        private fun processNativeMedia(mediaData: Any) {
            try {
                controller?.let { ctrl ->
                    // Field names verified from SystemUI.apk dexdump
                    val pkg = XposedHelpers.getObjectField(mediaData, "packageName") as? String ?: return
                    val song = XposedHelpers.getObjectField(mediaData, "song") as? CharSequence ?: ""
                    val artist = XposedHelpers.getObjectField(mediaData, "artist") as? CharSequence ?: ""
                    val artworkIcon = XposedHelpers.getObjectField(mediaData, "artwork") as? android.graphics.drawable.Icon
                    val isPlaying = XposedHelpers.getBooleanField(mediaData, "isPlaying")
                    
                    XposedBridge.log("$TAG: Native media caught: $song by $artist (Playing: $isPlaying)")

                    val artworkBmp = artworkIcon?.let { decodeIcon(it, ctrl.notificationManager.context) }
                    
                    ctrl.mediaManager.updateMediaFromNative(
                        pkg = pkg,
                        title = song.toString(),
                        artist = artist.toString(),
                        isPlaying = isPlaying,
                        artwork = artworkBmp
                    )
                }
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ❌: processNativeMedia failed — ${e.message}")
            }
        }

        private fun decodeIcon(icon: android.graphics.drawable.Icon, context: Context): android.graphics.Bitmap? {
            return try {
                val drawable = icon.loadDrawable(context) ?: return null
                val bitmap = android.graphics.Bitmap.createBitmap(
                    drawable.intrinsicWidth.coerceAtLeast(1),
                    drawable.intrinsicHeight.coerceAtLeast(1),
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            } catch (e: Exception) { null }
        }
    }
}
