package com.example.dynamicisland.hook
import com.example.dynamicisland.model.*

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object FrameworkTelecomHook {
    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        
        // Hook directly into the Telecom Framework's Call state machine
        IslandHookEngine.hookMethodSafe(
            "com.android.server.telecom.Call",
            lpparam.classLoader,
            "setState",
            Int::class.javaPrimitiveType ?: Int::class.java,
            String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val stateInt = param.args[0] as Int
                        // Extract context from the Call object to send broadcast
                        val mContext = XposedHelpers.callMethod(param.thisObject, "getContext") as? Context ?: return
                        
                        // Extract Caller ID directly from the framework!
                        val handle = XposedHelpers.callMethod(param.thisObject, "getHandle")
                        val callerNumber = handle?.toString()?.replace("tel:", "") ?: "Unknown Caller"
                        val callerName = XposedHelpers.callMethod(param.thisObject, "getName") as? String ?: callerNumber
                        
                        val stateStr = when(stateInt) {
                            4 -> "RINGING"        // CallState.RINGING
                            5 -> "ONGOING"        // CallState.ACTIVE
                            7 -> "DISCONNECTED"   // CallState.DISCONNECTED
                            else -> "UNKNOWN"
                        }
                        
                        if (stateStr != "UNKNOWN") {
                            val intent = Intent("com.example.dynamicisland.CALL_STATE_CHANGED").apply {
                                setPackage("com.android.systemui")
                                putExtra("state", stateStr)
                                putExtra("caller", callerName)
                            }
                            mContext.sendBroadcastAsUser(intent, userAll)
                        }
                    } catch(e: Throwable) {}
                }
            }
        )
    }
}
