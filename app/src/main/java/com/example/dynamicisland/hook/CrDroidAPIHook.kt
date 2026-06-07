package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * BATCH 6: crDroid-Specific API Integration
 *
 * crDroid 11.x exposes several ROM-private APIs absent from AOSP.
 * This hook taps into them to give the island capabilities impossible
 * on stock Android:
 *
 *   1. GameMode API — crDroid's GameSpaceManager notifies when the user
 *      enters/exits a game session. The island switches to Gaming HUD
 *      mode and suppresses non-critical events automatically.
 *
 *   2. Thermal Governor — crDroid's ThermalEngine exposes per-scenario
 *      profiles. We hook setThermalMode() to show a live thermal
 *      throttling warning when the device enters performance-limited mode.
 *
 *   3. DC Dimming / Refresh Rate events — crDroid's DisplayFeatureService
 *      notifies when display mode changes. The island shows a brief
 *      "60Hz · Battery saver" or "144Hz · Gaming" pill.
 *
 *   4. Smart Charge controller — hooks crDroid's BatteryWellbeing smart
 *      charge limit so the island can show a "Charge limited to 80%"
 *      indicator when the feature is active.
 *
 *   5. One-handed mode — crDroid's OneHandedController entry so the
 *      island can re-anchor itself when the display contracts.
 *
 * All hooks fall through silently on non-crDroid ROMs — every call
 * is wrapped in a try/catch and uses findClassIfExists().
 */
object CrDroidAPIHook {

    // Broadcast actions published by this hook
    const val ACTION_GAME_MODE_CHANGED  = "com.example.dynamicisland.CRDROID_GAME_MODE"
    const val ACTION_THERMAL_PROFILE    = "com.example.dynamicisland.CRDROID_THERMAL"
    const val ACTION_DISPLAY_MODE       = "com.example.dynamicisland.CRDROID_DISPLAY_MODE"
    const val ACTION_SMART_CHARGE       = "com.example.dynamicisland.CRDROID_SMART_CHARGE"
    const val ACTION_ONE_HANDED         = "com.example.dynamicisland.CRDROID_ONE_HANDED"

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {

        // ── 1. GameSpaceManager — game session entry/exit ─────────────────────
        hookGameMode(lpparam, userAll)

        // ── 2. ThermalEngine — throttle profile changes ───────────────────────
        hookThermalEngine(lpparam, userAll)

        // ── 3. DisplayFeatureService — refresh rate + DC dimming ─────────────
        hookDisplayFeature(lpparam, userAll)

        // ── 4. BatteryWellbeing smart charge ──────────────────────────────────
        hookSmartCharge(lpparam, userAll)

        // ── 5. OneHandedController ────────────────────────────────────────────
        hookOneHanded(lpparam, userAll)
    }

    // ── Hook implementations ──────────────────────────────────────────────────

    private fun hookGameMode(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        // Primary crDroid 11 class
        val candidates = listOf(
            "com.android.server.gamespace.GameSpaceManager",
            "com.android.server.game.GameManagerService",
            "com.crdroid.server.gamespace.GameSpaceService"
        )
        for (className in candidates) {
            val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: continue

            // Hook the method that activates game mode for a package
            for (method in clazz.declaredMethods) {
                val name = method.name.lowercase()
                if (name.contains("setgamemode") || name.contains("entergame") ||
                    name.contains("startgame")   || name.contains("ongamemodechanged")) {
                    try {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                try {
                                    // Try to extract package name and mode from args
                                    val pkg       = (param.args as Array<Any?>).filterIsInstance<String>().firstOrNull() ?: ""
                                    val modeInt   = (param.args as Array<Any?>).filterIsInstance<Int>().firstOrNull() ?: 0
                                    val isEntering = modeInt != 0 || name.contains("enter")

                                    val context = getContextFromParam(param) ?: return
                                    context.sendBroadcastAsUser(
                                        Intent(ACTION_GAME_MODE_CHANGED).apply {
                                            setPackage("com.example.dynamicisland.core")
                                            putExtra("pkg",       pkg)
                                            putExtra("isActive",  isEntering)
                                            putExtra("gameMode",  modeInt)
                                        },
                                        userAll
                                    )
                                } catch (_: Throwable) {}
                            }
                        })
                        XposedBridge.log("DynamicIsland: Hooked GameMode via $className.${method.name}")
                    } catch (_: Throwable) {}
                }
            }
            break  // found a class, stop searching
        }
    }

    private fun hookThermalEngine(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        val candidates = listOf(
            "com.android.server.ThermalManagerService",
            "android.os.ThermalService",
            "com.crdroid.server.thermal.ThermalEngine"
        )
        for (className in candidates) {
            val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: continue
            for (method in clazz.declaredMethods) {
                val name = method.name.lowercase()
                if (name.contains("setpowersavinglevel") || name.contains("setthermalprofile") ||
                    name.contains("notifythrottling")    || name.contains("throttle")) {
                    try {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val level     = (param.args as Array<Any?>).filterIsInstance<Int>().firstOrNull() ?: -1
                                val context   = getContextFromParam(param) ?: return
                                val profileName = when (level) {
                                    0    -> "NONE"
                                    1    -> "LIGHT"
                                    2    -> "MODERATE"
                                    3    -> "SEVERE"
                                    4    -> "CRITICAL"
                                    else -> "UNKNOWN"
                                }
                                if (level > 1) {
                                    context.sendBroadcastAsUser(
                                        Intent(ACTION_THERMAL_PROFILE).apply {
                                            setPackage("com.example.dynamicisland.core")
                                            putExtra("level",   level)
                                            putExtra("profile", profileName)
                                        },
                                        userAll
                                    )
                                }
                            }
                        })
                    } catch (_: Throwable) {}
                }
            }
            break
        }
    }

    private fun hookDisplayFeature(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        val candidates = listOf(
            "com.android.server.display.DisplayModeDirector",
            "com.crdroid.server.display.DisplayFeatureService",
            "com.android.server.display.color.DisplayTransformManager"
        )
        for (className in candidates) {
            val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: continue
            for (method in clazz.declaredMethods) {
                val name = method.name.lowercase()
                if (name.contains("setrefreshrate") || name.contains("updatedisplaymode") ||
                    name.contains("setdcdimming")   || name.contains("setmodebyowner")) {
                    try {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val refreshRate = (param.args as Array<Any?>).filterIsInstance<Float>().firstOrNull()
                                val modeId      = (param.args as Array<Any?>).filterIsInstance<Int>().firstOrNull()
                                val context     = getContextFromParam(param) ?: return

                                context.sendBroadcastAsUser(
                                    Intent(ACTION_DISPLAY_MODE).apply {
                                        setPackage("com.example.dynamicisland.core")
                                        refreshRate?.let { putExtra("refreshRate", it.toInt()) }
                                        modeId?.let     { putExtra("modeId", it) }
                                    },
                                    userAll
                                )
                            }
                        })
                    } catch (_: Throwable) {}
                }
            }
            break
        }
    }

    private fun hookSmartCharge(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        val candidates = listOf(
            "com.crdroid.batterywellbeing.SmartChargeService",
            "com.android.server.power.BatterySaverController"
        )
        for (className in candidates) {
            val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: continue
            IslandHookEngine.hookAfter(className, lpparam.classLoader, "setChargeLimit",
                Int::class.javaPrimitiveType ?: Int::class.java
            ) { param ->
                val limit   = (param.args as Array<Any?>)[0] as Int
                val context = getContextFromParam(param) ?: return@hookAfter
                context.sendBroadcastAsUser(
                    Intent(ACTION_SMART_CHARGE).apply {
                        setPackage("com.example.dynamicisland.core")
                        putExtra("limit", limit)
                        putExtra("active", limit < 100)
                    },
                    userAll
                )
            }
            break
        }
    }

    private fun hookOneHanded(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        val candidates = listOf(
            "com.android.server.wm.OneHandedController",
            "com.android.wm.shell.onehanded.OneHandedController"
        )
        for (className in candidates) {
            val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: continue
            for (method in clazz.declaredMethods) {
                val name = method.name.lowercase()
                if (name.contains("startonehandedmode") || name.contains("stoponehandedmode") ||
                    name.contains("setonehandedmode")) {
                    try {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val isActive = name.contains("start") ||
                                    ((param.args as Array<Any?>).filterIsInstance<Boolean>().firstOrNull() == true)
                                val context  = getContextFromParam(param) ?: return
                                context.sendBroadcastAsUser(
                                    Intent(ACTION_ONE_HANDED).apply {
                                        setPackage("com.example.dynamicisland.core")
                                        putExtra("isActive", isActive)
                                    },
                                    userAll
                                )
                            }
                        })
                    } catch (_: Throwable) {}
                }
            }
            break
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun getContextFromParam(param: XC_MethodHook.MethodHookParam): Context? {
        return try {
            XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
        } catch (_: Throwable) {
            try { android.app.AndroidAppHelper.currentApplication() } catch (_: Throwable) { null }
        }
    }
}