package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object SystemUIPanelHook {

    private var isPanelExpanded = false

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam) {

        // ── Notification panel expansion hook ─────────────────────────────────
        //
        // Confirmed class on ALL three ROMs:
        //   com.android.systemui.shade.NotificationPanelViewController
        //
        // Confirmed method on ALL three ROMs:
        //   setExpandedFraction(F)V
        //
        // Context access:
        //   CrDroid / Infinity X  → mView field → view.context
        //   Evolution X           → mView field → view.context (same, confirmed)
        //   Fallback              → AndroidAppHelper

        IslandHookEngine.hookAfter(
            "com.android.systemui.shade.NotificationPanelViewController",
            lpparam.classLoader,
            "setExpandedFraction",
            Float::class.javaPrimitiveType ?: Float::class.java
        ) { param ->
            val fraction = param.args[0] as Float
            val isCurrentlyExpanded = fraction > 0.05f

            if (isCurrentlyExpanded != isPanelExpanded) {
                isPanelExpanded = isCurrentlyExpanded

                // Try mView field first (confirmed on all ROMs from smali)
                val context: Context? = try {
                    val mView = XposedHelpers.getObjectField(param.thisObject, "mView")
                        as? android.view.View
                    mView?.context
                } catch (_: Throwable) {
                    null
                } ?: try {
                    // Fallback for any ROM variant
                    XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                } catch (_: Throwable) {
                    null
                } ?: try {
                    android.app.AndroidAppHelper.currentApplication()
                } catch (_: Throwable) {
                    null
                }

                context ?: return@hookAfter

                val intent = Intent("com.example.dynamicisland.PANEL_STATE_CHANGED").apply {
                    setPackage("com.android.systemui")
                    putExtra("isExpanded", isPanelExpanded)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
