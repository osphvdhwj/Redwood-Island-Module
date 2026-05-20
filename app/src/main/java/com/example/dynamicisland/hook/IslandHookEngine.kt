package com.example.dynamicisland.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

import com.example.dynamicisland.hook.providers.DefaultAospProvider

/**
 * Central hook engine.
 *
 * Key design choices for cross-ROM robustness:
 *
 *  • hookMethodSafe         — single method by exact signature, swallows all errors
 *  • hookAllMethodsByName   — hooks EVERY overload matching a name (no signature needed);
 *                             essential for methods like enqueueNotificationInternal
 *                             whose signatures change across AOSP versions
 *  • hookAfterAllOverloads  — convenience wrapper for hookAllMethodsByName with afterHook
 *  • hookFirstMatch         — tries a list of (className, methodName) pairs, hooks the
 *                             first one that resolves; ideal for renamed ROM methods
 *  • scanAndHook            — when even the method name is uncertain, scans all declared
 *                             methods and hooks ones whose name contains a keyword
 */
object IslandHookEngine {

    private const val TAG = "DynamicIsland"
    private var activeProvider: SystemEventProvider? = null

    /**
     * Determines the current OS/ROM environment and instantiates the correct SystemEventProvider.
     * Routes the initialization of hooks to the chosen provider.
     */
    fun initHooks(classLoader: ClassLoader, listener: SystemEventListener) {
        val provider = determineProvider()
        provider.setSystemEventListener(listener)
        provider.initHooks(classLoader)
        activeProvider = provider
    }

    private fun determineProvider(): SystemEventProvider {
        // In the future, check Build.MANUFACTURER or Build.DISPLAY to return ROM-specific providers
        // e.g., if (android.os.Build.MANUFACTURER.equals("xiaomi", true)) return MiuiProvider()
        return DefaultAospProvider()
    }

    // ── Core safe hook ────────────────────────────────────────────────────────

    fun hookMethodSafe(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg parameterTypesAndCallback: Any
    ) {
        try {
            val clazz = XposedHelpers.findClassIfExists(className, classLoader)
            if (clazz != null) {
                XposedHelpers.findAndHookMethod(clazz, methodName, *parameterTypesAndCallback)
            } else {
                XposedBridge.log("$TAG ⚠️: Class not found — $className")
            }
        } catch (e: Throwable) {
            XposedBridge.log("$TAG ❌: hookMethodSafe $className.$methodName — ${e.message}")
        }
    }

    // ── All-overloads hook (signature-independent) ────────────────────────────

    /**
     * Hooks ALL overloads of [methodName] on [className].
     * This is the safest way to hook methods whose signatures vary across ROM versions.
     *
     * Returns the number of overloads successfully hooked.
     */
    fun hookAllMethodsByName(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        callback: XC_MethodHook
    ): Int {
        val clazz = XposedHelpers.findClassIfExists(className, classLoader)
        if (clazz == null) {
            XposedBridge.log("$TAG ⚠️: hookAllMethodsByName — class not found: $className")
            return 0
        }
        return try {
            val unhooks = XposedBridge.hookAllMethods(clazz, methodName, callback)
            if (unhooks.isEmpty()) {
                XposedBridge.log("$TAG ⚠️: hookAllMethodsByName — no overloads found: $className.$methodName")
            } else {
                XposedBridge.log("$TAG ✅: hookAllMethodsByName — hooked ${unhooks.size} overload(s) of $className.$methodName")
            }
            unhooks.size
        } catch (e: Throwable) {
            XposedBridge.log("$TAG ❌: hookAllMethodsByName $className.$methodName — ${e.message}")
            0
        }
    }

    fun hookAllConstructorsSafe(
        className: String,
        classLoader: ClassLoader,
        callback: XC_MethodHook
    ) {
        try {
            val clazz = XposedHelpers.findClassIfExists(className, classLoader)
            if (clazz != null) {
                XposedBridge.hookAllConstructors(clazz, callback)
                XposedBridge.log("$TAG ✅: hookAllConstructors — $className")
            } else {
                XposedBridge.log("$TAG ⚠️: hookAllConstructors — class not found: $className")
            }
        } catch (e: Throwable) {
            XposedBridge.log("$TAG ❌: hookAllConstructors $className — ${e.message}")
        }
    }

    // ── First-match hook (tries multiple class/method candidates) ─────────────

    /**
     * Tries each (className, methodName) pair in [candidates] until one resolves.
     * Hooks ALL overloads of the first matching method.
     *
     * Use this when a method was renamed between ROM versions.
     */
    fun hookFirstMatch(
        classLoader: ClassLoader,
        candidates: List<Pair<String, String>>,
        callback: XC_MethodHook
    ): Boolean {
        for ((cls, method) in candidates) {
            val clazz = XposedHelpers.findClassIfExists(cls, classLoader) ?: continue
            return try {
                val unhooks = XposedBridge.hookAllMethods(clazz, method, callback)
                if (unhooks.isNotEmpty()) {
                    XposedBridge.log("$TAG ✅: hookFirstMatch — $cls.$method (${unhooks.size} overloads)")
                    true
                } else {
                    false
                }
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: hookFirstMatch $cls.$method — ${e.message}")
                false
            }
        }
        XposedBridge.log("$TAG ⚠️: hookFirstMatch — no candidate resolved: $candidates")
        return false
    }

    /**
     * Scans all declared methods of [className] and hooks any whose name
     * contains [nameKeyword] (case-insensitive). Use as last resort when
     * even method names are unpredictable across ROM variants.
     */
    fun scanAndHook(
        className: String,
        classLoader: ClassLoader,
        nameKeyword: String,
        callback: XC_MethodHook
    ): Int {
        val clazz = XposedHelpers.findClassIfExists(className, classLoader) ?: run {
            XposedBridge.log("$TAG ⚠️: scanAndHook — class not found: $className")
            return 0
        }
        var count = 0
        for (method in clazz.declaredMethods) {
            if (!method.name.contains(nameKeyword, ignoreCase = true)) continue
            try {
                XposedBridge.hookMethod(method, callback)
                XposedBridge.log("$TAG ✅: scanAndHook — $className.${method.name}")
                count++
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: scanAndHook $className.${method.name} — ${e.message}")
            }
        }
        if (count == 0) {
            XposedBridge.log("$TAG ⚠️: scanAndHook — no methods matching '$nameKeyword' in $className")
        }
        return count
    }

    // ── Convenience inline wrappers ────────────────────────────────────────────

    inline fun hookBefore(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg params: Any,
        crossinline action: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        hookMethodSafe(className, classLoader, methodName, *params, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try { action(param) } catch (_: Throwable) {}
            }
        })
    }

    inline fun hookAfter(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg params: Any,
        crossinline action: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        hookMethodSafe(className, classLoader, methodName, *params, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try { action(param) } catch (_: Throwable) {}
            }
        })
    }

    inline fun hookAfterAllOverloads(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        crossinline action: (XC_MethodHook.MethodHookParam) -> Unit
    ): Int = hookAllMethodsByName(className, classLoader, methodName, object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            try { action(param) } catch (_: Throwable) {}
        }
    })
}
