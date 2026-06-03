package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import com.example.dynamicisland.bridge.MediaBridge
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * PRO-GRADE MEDIA HOOK
 * Intercepts internal events from PowerAmp and other players to provide zero-latency updates.
 */
object SpecializedMediaHook {
    private const val TAG = "RedwoodMediaHook"

    fun init(classLoader: ClassLoader) {
        hookPowerAmp(classLoader)
    }

    private fun hookPowerAmp(classLoader: ClassLoader) {
        try {
            // PowerAmp often updates its internal track state before MediaSession
            XposedHelpers.findAndHookMethod(
                "com.maxmpz.audioplayer.PlayerService",
                classLoader,
                "updateTrack",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.thisObject as? Context ?: return
                        val intent = Intent("com.example.dynamicisland.SPECIALIZED_MEDIA_UPDATE")
                        intent.putExtra("pkg", MediaBridge.PKG_POWERAMP)
                        context.sendBroadcast(intent)
                    }
                }
            )
        } catch (_: Throwable) {}
    }
}
