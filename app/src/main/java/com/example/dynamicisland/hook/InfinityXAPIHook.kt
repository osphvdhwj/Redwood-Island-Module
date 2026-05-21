package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Infinity X A15 Specific API Integration
 *
 * This hook targets private APIs and components found in Infinity X ROM (Android 15).
 * It includes support for gaming mode, thermal management, and internal
 * 'Subo' and 'Rood' environment components.
 */
object InfinityXAPIHook {

    const val ACTION_INFINITY_GAME_MODE    = "com.example.dynamicisland.INFINITY_GAME_MODE"
    const val ACTION_INFINITY_THERMAL      = "com.example.dynamicisland.INFINITY_THERMAL"
    const val ACTION_INFINITY_SUB_STATE    = "com.example.dynamicisland.INFINITY_SUB_STATE"
    const val ACTION_INFINITY_ROOD_EVENT   = "com.example.dynamicisland.INFINITY_ROOD_EVENT"
    const val ACTION_INFINITY_EDGE_LIGHT   = "com.example.dynamicisland.INFINITY_EDGE_LIGHT"

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        // For Infinity X A15
        hookInfinityGameMode(lpparam, userAll)
        // For Infinity X A15
        hookInfinityThermal(lpparam, userAll)
        // For Infinity X A15
        hookSuboEnvironment(lpparam, userAll)
        // For Infinity X A15
        hookRoodEnvironment(lpparam, userAll)
        // For Infinity X A15
        hookInfinityEdgeLighting(lpparam, userAll)
        // For Infinity X A15
        hookInfinitySuite(lpparam, userAll)
    }

    private fun hookInfinityGameMode(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        // For Infinity X A15
        val candidates = listOf(
            "com.infinity.server.gamespace.GameSpaceService",
            "com.infinity.server.gamespace.InfinityGameManager",
            "com.android.server.infinity.GameModeController"
        )
        for (className in candidates) {
            val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: continue
            
            // Hook method that manages active game sessions
            IslandHookEngine.hookAllMethodsByName(className, lpparam.classLoader, "onGameStatusChanged", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val isActive = (param.args.firstOrNull() as? Boolean) ?: false
                        val pkg = (param.args.getOrNull(1) as? String) ?: ""
                        val context = getContextFromParam(param) ?: return
                        
                        context.sendBroadcastAsUser(
                            Intent(ACTION_INFINITY_GAME_MODE).apply {
                                setPackage("com.android.systemui")
                                putExtra("isActive", isActive)
                                putExtra("pkg", pkg)
                            },
                            userAll
                        )
                        XposedBridge.log("DynamicIsland ✅: Infinity X GameMode Hooked via $className")
                    } catch (_: Throwable) {}
                }
            })
            break
        }
    }

    private fun hookInfinityThermal(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        // For Infinity X A15
        val candidates = listOf(
            "com.infinity.server.thermal.ThermalEngine",
            "com.infinity.server.thermal.InfinityThermalService"
        )
        for (className in candidates) {
            val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: continue
            
            IslandHookEngine.hookAllMethodsByName(className, lpparam.classLoader, "setThermalProfile", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val profile = param.args.firstOrNull()?.toString() ?: "Default"
                        val context = getContextFromParam(param) ?: return
                        
                        context.sendBroadcastAsUser(
                            Intent(ACTION_INFINITY_THERMAL).apply {
                                setPackage("com.android.systemui")
                                putExtra("profile", profile)
                            },
                            userAll
                        )
                    } catch (_: Throwable) {}
                }
            })
            break
        }
    }

    private fun hookSuboEnvironment(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        // For Infinity X A15 - Subo environment hook
        val className = "com.infinity.subo.SuboManager"
        val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: return
        
        XposedBridge.hookAllMethods(clazz, "notifyStateChanged", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val state = param.args.firstOrNull()?.toString() ?: ""
                    val context = getContextFromParam(param) ?: return
                    
                    context.sendBroadcastAsUser(
                        Intent(ACTION_INFINITY_SUB_STATE).apply {
                            setPackage("com.android.systemui")
                            putExtra("state", state)
                        },
                        userAll
                    )
                    XposedBridge.log("DynamicIsland ✅: Infinity X Subo Hooked")
                } catch (_: Throwable) {}
            }
        })
    }

    private fun hookRoodEnvironment(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        // For Infinity X A15 - Rood environment hook
        val className = "com.infinity.rood.RoodController"
        val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: return
        
        XposedBridge.hookAllMethods(clazz, "onRoodEvent", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val eventType = (param.args.firstOrNull() as? Int) ?: -1
                    val context = getContextFromParam(param) ?: return
                    
                    context.sendBroadcastAsUser(
                        Intent(ACTION_INFINITY_ROOD_EVENT).apply {
                            setPackage("com.android.systemui")
                            putExtra("type", eventType)
                        },
                        userAll
                    )
                    XposedBridge.log("DynamicIsland ✅: Infinity X Rood Hooked")
                } catch (_: Throwable) {}
            }
        })
    }

    private fun hookInfinityEdgeLighting(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        // For Infinity X A15
        val className = "com.infinity.systemui.edgelighting.EdgeLightingController"
        val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: return
        
        IslandHookEngine.hookAllMethodsByName(className, lpparam.classLoader, "showEdgeLight", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    val context = getContextFromParam(param) ?: return
                    context.sendBroadcastAsUser(
                        Intent(ACTION_INFINITY_EDGE_LIGHT).apply {
                            setPackage("com.android.systemui")
                            putExtra("isActive", true)
                        },
                        userAll
                    )
                } catch (_: Throwable) {}
            }
        })
    }

    private fun hookInfinitySuite(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        // For Infinity X A15
        val className = "com.infinity.suite.InfinitySuiteActivity"
        val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: return
        
        XposedHelpers.findAndHookMethod(clazz, "onResume", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val context = param.thisObject as? android.app.Activity ?: return
                    context.sendBroadcastAsUser(
                        Intent("com.example.dynamicisland.INFINITY_SUITE_OPENED").apply {
                            setPackage("com.android.systemui")
                        },
                        userAll
                    )
                } catch (_: Throwable) {}
            }
        })
    }

    private fun getContextFromParam(param: XC_MethodHook.MethodHookParam): Context? {
        return try {
            XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
        } catch (_: Throwable) {
            try { android.app.AndroidAppHelper.currentApplication() } catch (_: Throwable) { null }
        }
    }
}
