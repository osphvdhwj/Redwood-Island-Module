package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object SystemUIHardwareHook {
    fun apply(lpparam: XC_LoadPackage.LoadPackageParam) {
        
        // 🛡️ Intercepts the Flashlight turning on or off
        IslandHookEngine.hookAfter(
            "com.android.systemui.statusbar.policy.FlashlightControllerImpl",
            lpparam.classLoader,
            "setFlashlight",
            Boolean::class.javaPrimitiveType ?: Boolean::class.java
        ) { param ->
            val isEnabled = param.args[0] as Boolean
            val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return@hookAfter
            
            val intent = Intent("com.example.dynamicisland.HARDWARE_TOGGLE").apply {
                setPackage("com.android.systemui")
                putExtra("type", "TORCH")
                putExtra("state", if (isEnabled) 1 else 0)
            }
            mContext.sendBroadcast(intent)
        }
    }
}
