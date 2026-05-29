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
        
        private var flashlightController: Any? = null

        fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
            if (lpparam.packageName != "com.android.systemui") return
            
            XposedBridge.log("$TAG: 🚀 Initializing Pro-Grade SystemUI Integration...")

            // ── 1. Core UI Injection ──────────────────────────────────────────
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

            // ── 2. Hardware Controller Hooks (System Feel) ────────────────────
            hookHardwareControllers(lpparam)

            // ── 3. Native Data Pipelines ──────────────────────────────────────
            hookNotifPipeline(lpparam)
            hookMediaPipeline(lpparam)
        }

        private fun injectIsland(root: FrameLayout, source: String) {
            if (injected) return
            val context = root.context
            XposedBridge.log("$TAG: System-level injection starting from $source (Parent: ${root.parent})")

            Handler(Looper.getMainLooper()).postDelayed({
                if (injected) return@postDelayed
                
                try {
                    XposedBridge.log("$TAG: Creating Controller and IslandView...")
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
                        val moduleContext = try { 
                            context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY) 
                        } catch (e: Exception) { context }

                        val islandView = DynamicIslandView(context, moduleContext)
                        ctrl.islandView = islandView
                        
                        val lp = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        ).apply {
                            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        }
                        
                        // Aggressive recursive un-clipping
                        unclipRecursive(root)
                        
                        root.addView(islandView, lp)
                        islandView.bringToFront()
                        
                        injected = true
                        XposedBridge.log("$TAG ✅: Pro-Grade Island active in SystemUI hierarchy")
                    }
                } catch (e: Throwable) {
                    XposedBridge.log("$TAG ❌: Native injection failed — ${e.message}")
                }
            }, 1000)
        }

        private fun unclipRecursive(view: View?) {
            var current = view
            repeat(8) {
                (current as? ViewGroup)?.let {
                    it.clipChildren = false
                    it.clipToPadding = false
                }
                current = current?.parent as? View
            }
        }

        private fun hookHardwareControllers(lpparam: XC_LoadPackage.LoadPackageParam) {
            // Hook FlashlightController to sync state and grab instance
            try {
                val flashlightClass = "com.android.systemui.statusbar.policy.FlashlightControllerImpl"
                IslandHookEngine.hookAllMethodsByName(flashlightClass, lpparam.classLoader, "onTorchModeChanged", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        flashlightController = param.thisObject
                        val enabled = param.args.getOrNull(0) as? Boolean ?: return
                        XposedBridge.log("$TAG: System Flashlight changed -> $enabled")
                        controller?.postTransientNotification(
                            LiveActivityModel.General(
                                id = "sys_torch", type = ActivityType.HARDWARE,
                                title = if (enabled) "Flashlight On" else "Flashlight Off",
                                dataText = if (enabled) "System torch is active" else "Torch disabled",
                                accentColor = if (enabled) android.graphics.Color.YELLOW else android.graphics.Color.GRAY
                            ), 2500L
                        )
                    }
                })
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: Flashlight hook failed")
            }

            // Hook VolumeDialogController for zero-latency volume sync
            try {
                val volumeClass = "com.android.systemui.volume.VolumeDialogControllerImpl"
                IslandHookEngine.hookAllMethodsByName(volumeClass, lpparam.classLoader, "onVolumeChangedW", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // Signature: onVolumeChangedW(int stream, int flags)
                        val stream = param.args.getOrNull(0) as? Int ?: return
                        if (stream == AudioManager.STREAM_MUSIC) {
                            controller?.islandView?.let { view ->
                                // Trigger volume update in UI
                                view.updateHardwareVolume(-1) // Signal to re-read
                            }
                        }
                    }
                })
            } catch (e: Throwable) {}
        }

        private fun hookNotifPipeline(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                val pipelineClass = "com.android.systemui.statusbar.notification.collection.NotifPipeline"
                IslandHookEngine.hookAllMethodsByName(
                    pipelineClass, 
                    lpparam.classLoader, 
                    "addCollectionListener", 
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val listener = param.args.firstOrNull() ?: return
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
                val dataManagerClass = "com.android.systemui.media.controls.domain.pipeline.LegacyMediaDataManagerImpl"
                IslandHookEngine.hookAllMethodsByName(
                    dataManagerClass,
                    lpparam.classLoader,
                    "onMediaDataLoaded",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
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
                val sbn = XposedHelpers.callMethod(entry, "getSbn") as android.service.notification.StatusBarNotification
                XposedBridge.log("$TAG: Catching native notification from ${sbn.packageName}")
                controller?.notificationManager?.processIncomingNotification(sbn.packageName, sbn.notification)
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ❌: processNativeNotification error: ${e.message}")
            }
        }

        private fun processNativeMedia(mediaData: Any) {
            try {
                controller?.let { ctrl ->
                    val pkg = XposedHelpers.getObjectField(mediaData, "packageName") as? String ?: return
                    val song = XposedHelpers.getObjectField(mediaData, "song") as? CharSequence ?: ""
                    val artist = XposedHelpers.getObjectField(mediaData, "artist") as? CharSequence ?: ""
                    val artworkIcon = XposedHelpers.getObjectField(mediaData, "artwork") as? android.graphics.drawable.Icon
                    val isPlaying = XposedHelpers.getBooleanField(mediaData, "isPlaying")
                    
                    XposedBridge.log("$TAG: Catching native media: $song by $artist from $pkg (isPlaying: $isPlaying)")
                    
                    val artworkBmp = artworkIcon?.let { decodeIcon(it, ctrl.context) }
                    ctrl.mediaManager.updateMediaFromNative(pkg, song.toString(), artist.toString(), artworkBmp, isPlaying)
                }
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ❌: processNativeMedia error: ${e.message}")
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
        
        // --- Pro-Grade Direct System Toggles ---
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
