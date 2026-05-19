package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object FrameworkTelecomHook {

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {

        // Call class candidates across AOSP + OEM ROMs
        val callClassCandidates = listOf(
            "com.android.server.telecom.Call",
            "com.android.internal.telecom.IInCallService",  // some custom ROMs
        )

        val stateCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val stateInt = param.args
                        .filterIsInstance<Int>()
                        .firstOrNull() ?: return

                    val stateStr = mapState(stateInt) ?: return
                    val ctx = getContext(param) ?: return

                    // Caller number
                    val handle = runCatching {
                        XposedHelpers.callMethod(param.thisObject, "getHandle")
                    }.getOrNull()
                    val callerNumber = handle?.toString()
                        ?.replace("tel:", "")?.trim()
                        ?.takeIf { it.isNotEmpty() } ?: "Private"

                    // Caller name (may not exist on all ROMs)
                    val callerName = runCatching {
                        XposedHelpers.callMethod(param.thisObject, "getName") as? String
                    }.getOrNull()?.takeIf { it.isNotEmpty() } ?: callerNumber

                    ctx.sendBroadcastAsUser(
                        Intent("com.example.dynamicisland.CALL_STATE_CHANGED").apply {
                            setPackage("com.android.systemui")
                            putExtra("state",  stateStr)
                            putExtra("caller", callerName)
                            putExtra("number", callerNumber)
                        },
                        userAll
                    )
                } catch (_: Throwable) {}
            }
        }

        var hooked = false
        for (cls in callClassCandidates) {
            val count = IslandHookEngine.hookAllMethodsByName(
                cls, lpparam.classLoader, "setState", stateCallback
            )
            if (count > 0) { hooked = true; break }
        }

        // Last-resort: scan telecom package for any setState containing call state logic
        if (!hooked) {
            XposedBridge.log("DynamicIsland ⚠️: Telecom setState not found via candidates — scanning")
            IslandHookEngine.scanAndHook(
                "com.android.server.telecom.Call",
                lpparam.classLoader,
                "setState",
                stateCallback
            )
        }
    }

    private fun mapState(stateInt: Int): String? = when (stateInt) {
        2, 4 -> "RINGING"
        5    -> "ONGOING"
        7    -> "DISCONNECTED"
        else -> null
    }

    private fun getContext(param: XC_MethodHook.MethodHookParam): Context? = try {
        XposedHelpers.callMethod(param.thisObject, "getContext") as? Context
    } catch (_: Throwable) {
        try {
            XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
        } catch (_: Throwable) {
            try { android.app.AndroidAppHelper.currentApplication() } catch (_: Throwable) { null }
        }
    }
}
