package com.example.dynamicisland.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object GameSpaceHook {
    private const val TAG = "GameSpaceHook"
    
    fun apply(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Universal hooks for MIUI simulation (Ghost Satellite)
        hookMiuiEnvironment(lpparam)
    }

    private fun hookMiuiEnvironment(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 1. Force GPU Tuner visibility
        try {
            XposedHelpers.findAndHookMethod(
                "android.provider.Settings\$Global", lpparam.classLoader, "getInt",
                android.content.ContentResolver::class.java, String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args[1] == "gpu_tuner_switch") {
                            param.result = 1
                        }
                    }
                }
            )
        } catch (_: Throwable) {}

        // 2. Fake MIUI Shared Libraries (needed for ported apps)
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", lpparam.classLoader, "getSystemSharedLibraryNames",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val libs = param.result as? Array<String> ?: return
                        if (!libs.contains("miui")) {
                            param.result = libs + arrayOf("miui", "com.miui.system", "com.miui.core")
                        }
                    }
                }
            )
        } catch (_: Throwable) {}

        // 3. Fake MIUI Product Build (trick app logic)
        try {
            XposedHelpers.setStaticObjectField(android.os.Build::class.java, "MANUFACTURER", "Xiaomi")
            XposedHelpers.setStaticObjectField(android.os.Build::class.java, "BRAND", "Xiaomi")
        } catch (_: Throwable) {}
    }
}
