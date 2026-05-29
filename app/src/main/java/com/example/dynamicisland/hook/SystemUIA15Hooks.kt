package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*
import com.example.dynamicisland.settings.SettingsManager
import com.example.dynamicisland.ui.mvi.IslandEventBus
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.lang.reflect.Proxy

class SystemUIA15Hooks {

    companion object {
        private const val TAG = "DynamicIsland-Native"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        internal var controller: IslandController? = null
        private var injected = false

        fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
            if (lpparam.packageName != "com.android.systemui") return

            XposedBridge.log("$TAG: 🚀 Initializing Modern SystemUI Hooks...")

            // ── 1. Core UI Injection Points ───────────────────────────────────
            
            // Primary: Status Bar Area
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                lpparam.classLoader,
                "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        injectIsland(param.thisObject as FrameLayout, "PhoneStatusBarView")
                    }
                }
            )

            // Secondary: Notification Shade Window (Ensures visibility on lockscreen/expanded shade)
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.shade.NotificationShadeWindowView",
                lpparam.classLoader,
                "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        injectIsland(param.thisObject as FrameLayout, "NotificationShadeWindowView")
                    }
                }
            )

            // ── 2. Native Data Pipelines ──────────────────────────────────────
            
            hookNotifPipeline(lpparam)
            hookMediaPipeline(lpparam)
        }

        private fun injectIsland(root: FrameLayout, source: String) {
            if (injected) return
            val context = root.context
            XposedBridge.log("$TAG: Injecting Island from $source")

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
                        val islandView = ctrl.createIslandView(
                            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager,
                            null
                        )

                        val hardwareManager = IslandHardwareManager(
                            context,
                            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
                            scope
                        )
                        val sidebarView = com.example.dynamicisland.ui.SidebarView(context, hardwareManager)

                        val lp = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        // FIX: Ensure the parent doesn't clip the Island when moved or expanded
                        root.clipChildren = false
                        root.clipToPadding = false
                        (root.parent as? ViewGroup)?.let {
                            it.clipChildren = false
                            it.clipToPadding = false
                        }
                        
                        root.addView(islandView, lp)
                        root.addView(sidebarView, FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            android.view.Gravity.END
                        ))
                        injected = true
                        XposedBridge.log("$TAG: ✅ Island injected successfully into $source")
                    }
                } catch (e: Exception) {
                    XposedBridge.log("$TAG ❌: Native injection failed — ${e.message}")
                    e.printStackTrace()
                }
            }, 2000)
        }

        /**
         * Hooks the modern Notification Pipeline to capture events directly.
         */
        private fun hookNotifPipeline(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                val pipelineClass = "com.android.systemui.statusbar.notification.collection.NotifPipeline"
                val listenerClass = "com.android.systemui.statusbar.notification.collection.notifcollection.NotifCollectionListener"
                
                XposedHelpers.findAndHookMethod(
                    pipelineClass, 
                    lpparam.classLoader, 
                    "addCollectionListener", 
                    listenerClass, 
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val listener = param.args[0] ?: return
                            XposedBridge.log("$TAG: Intercepting NotifPipeline Listener")
                            
                            // We hook the listener's methods to steal the data
                            XposedHelpers.findAndHookMethod(listener.javaClass, "onEntryAdded", 
                                "com.android.systemui.statusbar.notification.collection.NotificationEntry",
                                object : XC_MethodHook() {
                                    override fun afterHookedMethod(innerParam: MethodHookParam) {
                                        val entry = innerParam.args[0] ?: return
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

        /**
         * Hooks the modern Media Pipeline.
         */
        private fun hookMediaPipeline(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                val dataManagerClass = "com.android.systemui.media.controls.domain.pipeline.MediaDataManager"
                val listenerClass = "$dataManagerClass\$Listener"

                XposedHelpers.findAndHookMethod(
                    dataManagerClass,
                    lpparam.classLoader,
                    "addListener",
                    listenerClass,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val listener = param.args[0] ?: return
                            XposedBridge.log("$TAG: Intercepting MediaDataManager Listener")

                            IslandHookEngine.hookAllMethodsByName(listener.javaClass.name, lpparam.classLoader, "onMediaDataLoaded", object : XC_MethodHook() {
                                override fun afterHookedMethod(innerParam: MethodHookParam) {
                                    // Typically: onMediaDataLoaded(key, oldKey, data, immediately, ...)
                                    val data = innerParam.args.find { it?.javaClass?.name?.contains("MediaData") == true } ?: return
                                    processNativeMedia(data)
                                }
                            })
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
                    val packageName = XposedHelpers.getObjectField(mediaData, "app") as? String ?: return
                    val song = XposedHelpers.getObjectField(mediaData, "song") as? CharSequence ?: ""
                    val artist = XposedHelpers.getObjectField(mediaData, "artist") as? CharSequence ?: ""
                    val artwork = XposedHelpers.getObjectField(mediaData, "artwork") as? android.graphics.drawable.Icon
                    val isPlaying = XposedHelpers.getBooleanField(mediaData, "active") // This might be 'active' or 'isPlaying' depending on version
                    
                    XposedBridge.log("$TAG: Native media caught from $packageName: $song")
                    // Note: We need to map this to ctrl.mediaManager or a new method
                }
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ❌: processNativeMedia failed — ${e.message}")
            }
        }
    }
}
