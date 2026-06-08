package com.example.dynamicisland.hook

import android.content.pm.PackageManager
import android.os.Bundle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Modifier
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*

/**
 * 🛡️ STEALTH INTERCEPTOR (The "No ID Ban" Shield)
 *
 * Implements a "Cloaking Layer" that filters out Xposed and Redwood references 
 * from common detection APIs used by anti-cheat and integrity scanners.
 */
object StealthInterceptor {
    private const val MY_PKG = "com.example.dynamicisland"
    private val BLACKLIST_SIGS = listOf("dynamicisland", "redwood", "xposed", "lsposed", "edxp", "sandhook")

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Skip cloaking for SystemUI and our own app to avoid self-breaking
        if (lpparam.packageName == MY_PKG || lpparam.packageName == "android" || 
            lpparam.packageName == "com.android.systemui") return

        cloakPackageManager(lpparam)
        cloakStackTraces(lpparam)
        cloakReflection(lpparam)
    }

    private fun cloakPackageManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pmClass = "android.app.ApplicationPackageManager"
        
        // 1. Scrub getInstalledPackages/Applications results
        val listFilter = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val list = param.result as? MutableList<*> ?: return
                list.removeAll { item ->
                    val pkg = XposedHelpers.getObjectField(item, "packageName") as? String
                    pkg != null && (pkg == MY_PKG || BLACKLIST_SIGS.any { pkg.contains(it) })
                }
            }
        }
        
        try {
            XposedHelpers.findAndHookMethod(pmClass, lpparam.classLoader, "getInstalledPackages", 
                Int::class.javaPrimitiveType, listFilter)
            XposedHelpers.findAndHookMethod(pmClass, lpparam.classLoader, "getInstalledApplications", 
                Int::class.javaPrimitiveType, listFilter)
        } catch (_: Throwable) {}

        // 2. Hide module presence from direct queries
        try {
            XposedHelpers.findAndHookMethod(pmClass, lpparam.classLoader, "getPackageInfo", 
                String::class.java, Int::class.javaPrimitiveType, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val pkg = param.args[0] as? String
                    if (pkg == MY_PKG) {
                        param.throwable = PackageManager.NameNotFoundException(MY_PKG)
                    }
                }
            })
        } catch (_: Throwable) {}
    }

    private fun cloakStackTraces(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod("java.lang.Throwable", lpparam.classLoader, "getStackTrace", 
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val stack = param.result as? Array<StackTraceElement> ?: return
                        param.result = stack.filter { el -> 
                            BLACKLIST_SIGS.none { sig -> el.className.contains(sig, ignoreCase = true) } 
                        }.toTypedArray()
                    }
                })
        } catch (_: Throwable) {}
    }

    private fun cloakReflection(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Strip the NATIVE modifier from hooked methods to appear as original bytecode
        try {
            XposedHelpers.findAndHookMethod("java.lang.reflect.Method", lpparam.classLoader, "getModifiers", 
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        var mods = param.result as Int
                        if (Modifier.isNative(mods)) {
                            val declClass = XposedHelpers.callMethod(param.thisObject, "getDeclaringClass") as Class<*>
                            if (!declClass.name.startsWith("android.") && !declClass.name.startsWith("java.")) {
                                param.result = mods and Modifier.NATIVE.inv()
                            }
                        }
                    }
                })
        } catch (_: Throwable) {}
    }
}
