package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.View
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

class SystemUIA15Hooks {

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private var controller: IslandController? = null
        private var injected = false

        fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
            if (lpparam.packageName != "com.android.systemui") return

            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                lpparam.classLoader,
                "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val root = param.thisObject as FrameLayout
                        injectIsland(root)
                    }
                }
            )
        }

        private fun injectIsland(root: FrameLayout) {
            if (injected) return
            val context = root.context

            Handler(Looper.getMainLooper()).postDelayed({
                if (injected) return@postDelayed
                
                try {
                    val eventBus = IslandEventBus()
                    val settingsManager = SettingsManager(context)
                    val hapticsManager = IslandHapticsManager(context, settingsManager)
                    val networkMonitor = IslandNetworkMonitor()
                    val mediaManager = IslandMediaManager(context, scope)
                    val hardwareMonitor = IslandHardwareMonitor(scope)

                    // Create the singleton controller
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
                        
                        root.addView(islandView, lp)
                        root.addView(sidebarView, FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            android.view.Gravity.END
                        ))
                        injected = true
                    }
                } catch (e: Exception) {
                    XposedBridge.log("DynamicIsland ❌: Native injection failed — ${e.message}")
                    e.printStackTrace()
                }
            }, 2000)
        }
    }
}
