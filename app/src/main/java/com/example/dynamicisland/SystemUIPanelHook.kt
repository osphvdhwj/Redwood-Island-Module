package com.example.dynamicisland

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object SystemUIPanelHook {
    private var isPanelExpanded = false

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hooks the exact math value that controls the notification pull-down
        IslandHookEngine.hookAfter(
            "com.android.systemui.shade.NotificationPanelViewController", 
            lpparam.classLoader, 
            "setExpandedFraction", 
            Float::class.javaPrimitiveType ?: Float::class.java
        ) { param ->
            val fraction = param.args[0] as Float
            val isCurrentlyExpanded = fraction > 0.05f // If pulled down more than 5%

            if (isCurrentlyExpanded != isPanelExpanded) {
                isPanelExpanded = isCurrentlyExpanded
                
                val mView = XposedHelpers.getObjectField(param.thisObject, "mView") as? android.view.View
                val mContext = mView?.context ?: return@hookAfter
                
                // Tell our Island to hide or show!
                val intent = Intent("com.example.dynamicisland.PANEL_STATE_CHANGED").apply {
                    setPackage("com.android.systemui")
                    putExtra("isExpanded", isPanelExpanded)
                }
                mContext.sendBroadcast(intent)
            }
        }
    }
}
