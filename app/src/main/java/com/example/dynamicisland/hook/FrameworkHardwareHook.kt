package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
object FrameworkHardwareHook {

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {

        val audioClasses = listOf(
            "com.android.server.audio.AudioService",
            "com.android.server.AudioService",
        )

        val ringerCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    if (!isBootCompleted()) return

                    val ringerMode = param.args
                        .filterIsInstance<Int>()
                        .firstOrNull() ?: return

                    val ctx = getContext(param) ?: return
                    ctx.sendBroadcastAsUser(
                        Intent("com.example.dynamicisland.HARDWARE_TOGGLE").apply {
                            setPackage("com.example.dynamicisland.core")
                            putExtra("type",  "RINGER")
                            putExtra("state", ringerMode)
                        },
                        userAll,
                        "com.redwood.permission.SECURE_IPC"
                    )
                } catch (_: Throwable) {}
            }
        }

        for (cls in audioClasses) {
            val count = IslandHookEngine.hookAllMethodsByName(
                cls, lpparam.classLoader, "setRingerModeInternal", ringerCallback
            )
            if (count > 0) break
            IslandHookEngine.hookAllMethodsByName(
                cls, lpparam.classLoader, "setRingerMode", ringerCallback
            )
        }
    }

    /**
     * Checks sys.boot_completed via reflection — avoids the hidden-API
     * android.os.SystemProperties which is not in the public SDK.
     */
    private fun isBootCompleted(): Boolean {
        return try {
            val spClass = Class.forName("android.os.SystemProperties")
            val method  = spClass.getMethod("getBoolean", String::class.java, Boolean::class.java)
            method.invoke(null, "sys.boot_completed", false) as Boolean
        } catch (_: Throwable) {
            // If reflection fails (shouldn't happen inside Xposed), assume booted
            true
        }
    }

    private fun getContext(param: XC_MethodHook.MethodHookParam): Context? = try {
        XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
    } catch (_: Throwable) {
        try { android.app.AndroidAppHelper.currentApplication() } catch (_: Throwable) { null }
    }
}
