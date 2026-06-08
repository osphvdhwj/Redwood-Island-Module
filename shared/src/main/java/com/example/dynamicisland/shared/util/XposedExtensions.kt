package com.example.dynamicisland.shared.util

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * 🛡️ DEFENSIVE HOOKING ENGINE (Staff Level)
 *
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
 * Provides safe, failure-resilient wrappers for Xposed reflection utilities.
 *
 * ## Context:
 * Custom ROMs (MIUI, OxygenOS, OneUI) frequently modify internal SystemUI class names
 * and method signatures. Calling raw XposedHelpers can lead to ClassNotFound or 
 * NoSuchMethod exceptions that crash the entire SystemUI process.
 *
 * ## Solution:
 * This engine intercepts all reflection calls, providing graceful fallbacks and 
 * detailed logging instead of process-wide crashes.
 */
object XposedExtensions {

    /**
     * Safely attempts to hook a method if it exists on the current ROM.
     *
     * @param className Full canonical name of the class (e.g. "com.android.systemui.StatusBar").
     * @param classLoader The class loader provided by Xposed (lpparam.classLoader).
     * @param methodName The name of the method to hook.
     * @param parameterTypesAndCallback Variadic list of param types followed by the XC_MethodHook.
     * @return The Unhook handle if successful, or null if class/method is missing.
     */
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
                XposedBridge.log("Redwood ⚠️: Class not found for hooking: $className")
                null
            }
        } catch (e: Throwable) {
            XposedBridge.log("Redwood ❌: Failed to hook $className#$methodName: ${e.message}")
            null
        }
    }

    /**
     * Safely retrieves a field value from an object instance.
     */
    fun getObjectFieldSafe(obj: Any, fieldName: String): Any? {
        return try {
            XposedHelpers.getObjectField(obj, fieldName)
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Safely invokes a method on an object instance.
     */
    fun callMethodSafe(obj: Any, methodName: String, vararg args: Any): Any? {
        return try {
            XposedHelpers.callMethod(obj, methodName, *args)
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Safely sets a static field value on a class.
     * Essential for build-property spoofing (Manufacturer/Brand).
     */
    fun setStaticObjectFieldSafe(clazz: Class<*>, fieldName: String, value: Any?) {
        try {
            XposedHelpers.setStaticObjectField(clazz, fieldName, value)
        } catch (e: Throwable) {
            XposedBridge.log("Redwood ⚠️: Failed to set static field $fieldName on ${clazz.name}")
        }
    }

    /**
     * Attaches a hidden "additional" field to any object.
     * Useful for tracking metadata on system objects without modifying class structures.
     */
    fun setAdditionalInstanceFieldSafe(obj: Any, key: String, value: Any?) {
        try {
            XposedHelpers.setAdditionalInstanceField(obj, key, value)
        } catch (e: Throwable) {}
    }

    /**
     * Retrieves a hidden "additional" field from an object.
     */
    fun getAdditionalInstanceFieldSafe(obj: Any, key: String): Any? {
        return try {
            XposedHelpers.getAdditionalInstanceField(obj, key)
        } catch (e: Throwable) {
            null
        }
    }
}
