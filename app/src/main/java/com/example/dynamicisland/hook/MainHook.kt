package com.example.dynamicisland.hook

import android.app.Application
import android.content.Context
import com.example.dynamicisland.satellite.GboardSatellite
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

object SystemUIContextKeeper {
    var qsTileHost: Any? = null
}

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
        // 🛡️ MANDATE 2: Stealth Interception (Global)
        StealthInterceptor.apply(lpparam)

        val isSystemServer = lpparam.packageName == "android" || 
                           (lpparam.packageName == "system" && lpparam.processName == "android")
        
        val isSystemUI = lpparam.packageName == "com.android.systemui" ||
                        (lpparam.processName == "com.android.systemui")

        when {
            isSystemServer -> hookAndroidProcess(lpparam)
            isSystemUI -> hookSystemUIProcess(lpparam)
            else -> hookAppSatellite(lpparam)
        }
    }

    // --- 🛰️ SATELLITE LOADER (App-Level Synergy) ---

    private fun hookAppSatellite(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Ghost Satellites: Minimal footprint, no heavy dependencies.
        when (lpparam.packageName) {
            "com.google.android.inputmethod.latin" -> {
                log("Injecting Gboard Satellite")
                GboardSatellite().onInitialize(
                    AndroidAppHelper.currentApplication() ?: return, 
                    lpparam.packageName
                )
            }
            "com.google.android.apps.nexuslauncher" -> {
                log("Injecting Launcher Satellite")
                // TODO: Implement LauncherSatellite
            }
        }
    }

    // --- android process ---

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
