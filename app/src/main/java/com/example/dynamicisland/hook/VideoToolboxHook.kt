package com.example.dynamicisland.hook

import android.os.Bundle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * 🎬 VIDEO TOOLBOX HOOK
 *
 * Spoofs screen state for targeted media applications to prevent them from pausing
 * when the screen is turned off. (Background Playback Enforcer)
 */
object VideoToolboxHook {
    private const val TAG = "VideoToolboxHook"
    private val TARGET_APPS = setOf(
        "com.google.android.youtube",
        "com.vanced.android.youtube"
    )

    fun apply(lpparam: de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam) {
        if (!TARGET_APPS.contains(lpparam.packageName)) return

        try {
            // Hook PowerManager or DisplayManager to spoof screen state.
            // Often, apps check isInteractive()
            XposedHelpers.findAndHookMethod(
                "android.os.PowerManager",
                lpparam.classLoader,
                "isInteractive",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // Force it to always return true for these apps,
                        // tricking them into thinking the screen is on.
                        param.result = true
                    }
                }
            )

            // Alternatively, intercept the onPause lifecycle if hooking Activity directly
            // This is a more aggressive fallback
            XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                lpparam.classLoader,
                "onPause",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // We could potentially block the onPause call entirely,
                        // but this can cause lifecycle issues. Spoofing isInteractive is safer.
                        // XposedBridge.log("$TAG: Intercepted onPause for ${lpparam.packageName}")
                    }
                }
            )

            XposedBridge.log("$TAG: Applied background playback spoof to ${lpparam.packageName}")
        } catch (e: Throwable) {
            XposedBridge.log("$TAG ❌: Failed to apply hook to ${lpparam.packageName}: ${e.message}")
        }
    }
}
