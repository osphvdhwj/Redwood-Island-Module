package com.example.dynamicisland.hook

import android.app.AndroidAppHelper
import android.app.Application
import android.content.Context
import com.example.dynamicisland.satellite.GboardSatellite
import com.example.dynamicisland.satellite.ScreenContentSatellite
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*

/**
 * 🚀 REDWOOD MAIN ENTRY POINT (Xposed)
 * 
 * Orchestrates the 'Brain vs Sensors' architecture.
 * - Handles 'Ghost Satellite' injection into user apps.
 * - Handles 'System Sensor' injection into android/systemui.
 * - Enforces Global Stealth via the StealthInterceptor.
 */
class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        lateinit var modulePath: String

        val USER_ALL: android.os.UserHandle by lazy {
            try {
                android.os.UserHandle::class.java.getField("ALL").get(null)
                    as android.os.UserHandle
            } catch (_: Throwable) {
                try {
                    XposedHelpers.callStaticMethod(
                        android.os.UserHandle::class.java, "of",
                        arrayOf(Int::class.javaPrimitiveType), -1
                    ) as android.os.UserHandle
                } catch (_: Throwable) {
                    android.os.Process.myUserHandle()
                }
            }
        }

        fun log(msg: String)      = XposedBridge.log("Redwood: $msg")
        fun logOk(msg: String)    = XposedBridge.log("Redwood ✅: $msg")
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 🛡️ MANDATE: Stealth Interception (Global)
        StealthInterceptor.apply(lpparam)

        val isSystemServer = lpparam.packageName == "android" || 
                           (lpparam.packageName == "system" && lpparam.processName == "android")
        
        val isSystemUI = lpparam.packageName == "com.android.systemui" ||
                        (lpparam.processName == "com.android.systemui")

        when {
            isSystemServer -> hookAndroidProcess(lpparam)
            isSystemUI -> hookSystemUIProcess(lpparam)
            else -> {
                hookAppSatellite(lpparam)
                // 👁️ Feature B: Content Intelligence (Ghost Sensor)
                // Skip our own package and system server
                if (!lpparam.packageName.startsWith("com.example.dynamicisland") && !isSystemServer) {
                    try {
                        ScreenContentSatellite().onInitialize(
                            AndroidAppHelper.currentApplication() ?: return, 
                            lpparam.packageName
                        )
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    // --- 🛰️ SATELLITE LOADER (App-Level Synergy) ---

    private fun hookAppSatellite(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            "com.google.android.inputmethod.latin" -> {
                log("Injecting Gboard Satellite")
                GboardSatellite().onInitialize(
                    AndroidAppHelper.currentApplication() ?: return, 
                    lpparam.packageName
                )
            }
            "com.google.android.youtube", "com.vanced.android.youtube" -> {
                log("Injecting Video Toolbox Satellite")
                VideoToolboxHook.apply(lpparam)
            }
        }
    }

    // --- android process (System Server) ---

    private fun hookAndroidProcess(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("Applying android-process hooks")
        SurfaceFlingerHook.apply(lpparam, USER_ALL)
        SystemEventsHook.apply(lpparam, USER_ALL)
        FrameworkTelecomHook.apply(lpparam, USER_ALL)
        FrameworkHardwareHook.apply(lpparam, USER_ALL)
    }

    // --- SystemUI process ---

    private fun hookSystemUIProcess(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("Applying SystemUI-process hooks")
        SystemUIA15Hooks.init(lpparam)
        GameSpaceHook.apply(lpparam)
    }
}
