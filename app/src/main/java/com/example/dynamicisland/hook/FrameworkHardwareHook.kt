package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object FrameworkHardwareHook {

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {

        // AudioService class name is consistent across ROMs
        val audioClasses = listOf(
            "com.android.server.audio.AudioService",
            "com.android.server.AudioService",  // very old AOSP fallback
        )

        val ringerCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    // Guard: don't broadcast before boot is complete
                    if (!isBootCompleted()) return

                    val ringerMode = param.args
                        .filterIsInstance<Int>()
                        .firstOrNull() ?: return

                    val ctx = getContext(param) ?: return
                    ctx.sendBroadcastAsUser(
                        Intent("com.example.dynamicisland.HARDWARE_TOGGLE").apply {
                            setPackage("com.android.systemui")
                            putExtra("type",  "RINGER")
                            putExtra("state", ringerMode)
                        },
                        userAll
                    )
                } catch (_: Throwable) {}
            }
        }

        for (cls in audioClasses) {
            // Hook ALL overloads of setRingerModeInternal — signature varies
            val count = IslandHookEngine.hookAllMethodsByName(
                cls, lpparam.classLoader, "setRingerModeInternal", ringerCallback
            )
            if (count > 0) break

            // Fallback: setRingerMode (public version on some ROMs)
            IslandHookEngine.hookAllMethodsByName(
                cls, lpparam.classLoader, "setRingerMode", ringerCallback
            )
        }
    }

    private fun isBootCompleted(): Boolean = try {
        android.os.SystemProperties.getBoolean("sys.boot_completed", false)
    } catch (_: Throwable) { true }

    private fun getContext(param: XC_MethodHook.MethodHookParam): Context? = try {
        XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
    } catch (_: Throwable) {
        try { android.app.AndroidAppHelper.currentApplication() } catch (_: Throwable) { null }
    }
}
