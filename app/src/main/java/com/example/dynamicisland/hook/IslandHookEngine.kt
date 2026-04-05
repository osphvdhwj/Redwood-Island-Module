package com.example.dynamicisland.hook
import com.example.dynamicisland.model.*

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object IslandHookEngine {
    
    // 🛡️ The Base Safe Hook (FIXED: Now catches Throwable instead of Exception)
    fun hookMethodSafe(className: String, classLoader: ClassLoader, methodName: String, vararg parameterTypesAndCallback: Any) {
        try {
            val clazz = XposedHelpers.findClassIfExists(className, classLoader)
            if (clazz != null) { XposedHelpers.findAndHookMethod(clazz, methodName, *parameterTypesAndCallback) } 
            else { XposedBridge.log("DynamicIsland ⚠️: Class not found - $className") }
        } catch (e: Throwable) { 
            XposedBridge.log("DynamicIsland ❌: Hook failed in $className.$methodName -> ${e.message}") 
        }
    }

    fun hookAllConstructorsSafe(className: String, classLoader: ClassLoader, callback: XC_MethodHook) {
        try {
            val clazz = XposedHelpers.findClassIfExists(className, classLoader)
            if (clazz != null) XposedBridge.hookAllConstructors(clazz, callback)
        } catch (e: Throwable) {}
    }

    // ✨ Kotlin Magic Helpers
    inline fun hookBefore(className: String, classLoader: ClassLoader, methodName: String, vararg params: Any, crossinline action: (XC_MethodHook.MethodHookParam) -> Unit) {
        hookMethodSafe(className, classLoader, methodName, *params, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) { try { action(param) } catch(e:Throwable){} }
        })
    }

    inline fun hookAfter(className: String, classLoader: ClassLoader, methodName: String, vararg params: Any, crossinline action: (XC_MethodHook.MethodHookParam) -> Unit) {
        hookMethodSafe(className, classLoader, methodName, *params, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) { try { action(param) } catch(e:Throwable){} }
        })
    }
}
