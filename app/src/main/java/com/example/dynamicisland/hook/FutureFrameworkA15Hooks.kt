package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Future-Ready Framework Hooks for Android 15 (A15)
 * Focuses on zero-latency event interception from the system_server.
 */
object FutureFrameworkA15Hooks {

    const val ACTION_FUTURE_VOLUME_CHANGED     = "com.example.dynamicisland.FUTURE_VOLUME"
    const val ACTION_FUTURE_BRIGHTNESS_CHANGED   = "com.example.dynamicisland.FUTURE_BRIGHTNESS"
    const val ACTION_FUTURE_PRIVACY_INDICATOR    = "com.example.dynamicisland.FUTURE_PRIVACY"
    const val ACTION_FUTURE_BIOMETRIC_AUTH       = "com.example.dynamicisland.FUTURE_BIOMETRIC"
    const val ACTION_FUTURE_TOP_APP_CHANGED      = "com.example.dynamicisland.APP_CHANGED"

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        hookVolumeChanges(lpparam, userAll)
        hookBrightnessChanges(lpparam, userAll)
        hookPrivacyIndicators(lpparam, userAll)
        hookBiometricAuth(lpparam, userAll)
        hookTopAppChanges(lpparam, userAll)
    }

    private fun hookTopAppChanges(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        // Intercept foreground activity changes at the source
        try {
            val className = "com.android.server.wm.ActivityRecord"
            val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: return

            XposedBridge.hookAllMethods(clazz, "onWindowsDrawn", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val packageName = XposedHelpers.getObjectField(param.thisObject, "packageName") as? String ?: return
                        val context = getContextFromParam(param) ?: return
                        
                        context.sendBroadcastAsUser(
                            Intent(ACTION_FUTURE_TOP_APP_CHANGED).apply {
                                setPackage("com.example.dynamicisland.core")
                                putExtra("pkg", packageName)
                            },
                            userAll
                        )
                    } catch (_: Throwable) {}
                }
            })
        } catch (_: Throwable) {}
    }

    private fun hookVolumeChanges(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        val className = "com.android.server.audio.AudioService"
        val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: return

        XposedBridge.hookAllMethods(clazz, "setStreamVolume", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    if (param.args.size < 2) return
                    val streamType = param.args[0] as? Int ?: return
                    val index = param.args[1] as? Int ?: return
                    val context = getContextFromParam(param) ?: return
                    
                    context.sendBroadcastAsUser(
                        Intent(ACTION_FUTURE_VOLUME_CHANGED).apply {
                            setPackage("com.example.dynamicisland.core")
                            putExtra("streamType", streamType)
                            putExtra("index", index)
                        },
                        userAll
                    )
                } catch (_: Throwable) {}
            }
        })
    }

    private fun hookBrightnessChanges(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        val className = "com.android.server.display.DisplayPowerController"
        val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: return

        XposedBridge.hookAllMethods(clazz, "updatePowerState", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val mAppliedBrightness = XposedHelpers.getFloatField(param.thisObject, "mAppliedBrightness")
                    val context = getContextFromParam(param) ?: return
                    
                    context.sendBroadcastAsUser(
                        Intent(ACTION_FUTURE_BRIGHTNESS_CHANGED).apply {
                            setPackage("com.example.dynamicisland.core")
                            putExtra("brightness", mAppliedBrightness)
                        },
                        userAll
                    )
                } catch (_: Throwable) {}
            }
        })
    }

    private fun hookPrivacyIndicators(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        val className = "com.android.server.appop.AppOpsService"
        val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: return

        XposedBridge.hookAllMethods(clazz, "noteOperation", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val code = param.args[0] as Int
                    val pkg = param.args[2] as? String ?: ""
                    if (code == 26 || code == 27) {
                        val context = getContextFromParam(param) ?: return
                        context.sendBroadcastAsUser(
                            Intent(ACTION_FUTURE_PRIVACY_INDICATOR).apply {
                                setPackage("com.example.dynamicisland.core")
                                putExtra("op", if (code == 26) "CAMERA" else "MIC")
                                putExtra("pkg", pkg)
                            },
                            userAll
                        )
                    }
                } catch (_: Throwable) {}
            }
        })
    }

    private fun hookBiometricAuth(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        val biometricClasses = listOf(
            "com.android.server.biometrics.sensors.fingerprint.FingerprintService",
            "com.android.server.biometrics.sensors.face.FaceService"
        )
        
        val authCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val context = getContextFromParam(param) ?: return
                    context.sendBroadcastAsUser(
                        Intent(ACTION_FUTURE_BIOMETRIC_AUTH).apply {
                            setPackage("com.example.dynamicisland.core")
                            putExtra("authenticated", true)
                        },
                        userAll
                    )
                } catch (_: Throwable) {}
            }
        }

        for (cls in biometricClasses) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            XposedBridge.hookAllMethods(clazz, "onAuthenticationSucceeded", authCallback)
        }
    }

    private fun getContextFromParam(param: XC_MethodHook.MethodHookParam): Context? {
        return try {
            XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
        } catch (_: Throwable) {
            try { android.app.AndroidAppHelper.currentApplication() } catch (_: Throwable) { null }
        }
    }
}
