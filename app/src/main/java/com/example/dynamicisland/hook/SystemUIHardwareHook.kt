package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object SystemUIHardwareHook {

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam) {

        // ── Flashlight hook ───────────────────────────────────────────────────
        //
        // ROM differences confirmed from smali analysis:
        //   CrDroid / Infinity X  → FlashlightControllerImpl has mContext field
        //   Evolution X           → NO mContext field; must use AndroidAppHelper
        //
        // Strategy: try mContext first, fall back to AndroidAppHelper.
        // This makes the hook work on ALL ROMs without separate builds.

        IslandHookEngine.hookAfter(
            "com.android.systemui.statusbar.policy.FlashlightControllerImpl",
            lpparam.classLoader,
            "setFlashlight",
            Boolean::class.javaPrimitiveType ?: Boolean::class.java
        ) { param ->
            val isEnabled = param.args[0] as Boolean

            // Try mContext first (CrDroid / Infinity X)
            val context: Context? = try {
                XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
            } catch (_: Throwable) {
                null
            } ?: try {
                // Fallback: Evolution X and any ROM without mContext
                android.app.AndroidAppHelper.currentApplication()
            } catch (_: Throwable) {
                null
            }

            context ?: return@hookAfter

            val intent = Intent("com.example.dynamicisland.HARDWARE_TOGGLE").apply {
                setPackage("com.android.systemui")
                putExtra("type", "TORCH")
                putExtra("state", if (isEnabled) 1 else 0)
            }
            context.sendBroadcast(intent)
        }
    }
}
