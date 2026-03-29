package com.example.dynamicisland

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object FrameworkHardwareHook {
    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        
        // 🛡️ Intercepts the Ringer Mode physically changing
        IslandHookEngine.hookAfter(
            "com.android.server.audio.AudioService",
            lpparam.classLoader,
            "setRingerModeInternal",
            Int::class.javaPrimitiveType ?: Int::class.java,
            String::class.java
        ) { param ->
            val ringerMode = param.args[0] as Int
            val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return@hookAfter
            
            val intent = Intent("com.example.dynamicisland.HARDWARE_TOGGLE").apply {
                setPackage("com.android.systemui")
                putExtra("type", "RINGER")
                putExtra("state", ringerMode)
            }
            mContext.sendBroadcastAsUser(intent, userAll)
        }
    }
}
