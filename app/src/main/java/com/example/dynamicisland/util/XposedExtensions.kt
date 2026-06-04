package com.example.dynamicisland.util

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * DEFENSIVE HOOKING ENGINE
 *
 * Industry-standard wrappers for Xposed helpers to prevent SystemUI crashes
 * when class or method signatures differ across ROMs (MIUI, OxygenOS, etc.)
 */
object XposedExtensions {

    fun hookMethodIfExists(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg parameterTypesAndCallback: Any
    ): XC_MethodHook.Unhook? {
        return try {
            val clazz = XposedHelpers.findClassIfExists(className, classLoader)
            if (clazz != null) {
                XposedHelpers.findAndHookMethod(clazz, methodName, *parameterTypesAndCallback)
            } else {
                XposedBridge.log("DynamicIsland ⚠️: Class not found for hooking: $className")
                null
            }
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland ❌: Failed to hook $className#$methodName: ${e.message}")
            null
        }
    }

    fun getObjectFieldSafe(obj: Any, fieldName: String): Any? {
        return try {
            XposedHelpers.getObjectField(obj, fieldName)
        } catch (e: Throwable) {
            null
        }
    }

    fun callMethodSafe(obj: Any, methodName: String, vararg args: Any): Any? {
        return try {
            XposedHelpers.callMethod(obj, methodName, *args)
        } catch (e: Throwable) {
            null
        }
    }

    fun setStaticObjectFieldSafe(clazz: Class<*>, fieldName: String, value: Any?) {
        try {
            XposedHelpers.setStaticObjectField(clazz, fieldName, value)
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland ⚠️: Failed to set static field $fieldName on ${clazz.name}")
        }
    }

    fun setAdditionalInstanceFieldSafe(obj: Any, key: String, value: Any?) {
        try {
            XposedHelpers.setAdditionalInstanceField(obj, key, value)
        } catch (e: Throwable) {}
    }

    fun getAdditionalInstanceFieldSafe(obj: Any, key: String): Any? {
        return try {
            XposedHelpers.getAdditionalInstanceField(obj, key)
        } catch (e: Throwable) {
            null
        }
    }
}
