package com.example.dynamicisland.satellite

import android.content.Context
import android.net.Uri
import android.os.Bundle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
 * ⌨️ GBOARD SATELLITE
 *
 * Sensors the Gboard process for keyboard visibility and height events.
 * Syncs these events to SystemUI to allow the Nav Island to auto-hide.
 */
class GboardSatellite : SatelliteBase {

    private val PROVIDER_URI = Uri.parse("content://com.example.dynamicisland.provider")

    override fun onInitialize(context: Context, hostPackageName: String) {
        if (hostPackageName != "com.google.android.inputmethod.latin") return

        try {
            val viewHolderClass = "com.google.android.apps.inputmethod.libs.framework.core.KeyboardViewHolder"
            XposedHelpers.findAndHookMethod(viewHolderClass, context.classLoader, "onLayout",
                Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as android.view.View
                        val height = view.height
                        val bundle = Bundle().apply {
                            putInt("keyboard_height", height)
                            putBoolean("is_visible", view.visibility == android.view.View.VISIBLE)
                            putString("package", hostPackageName)
                        }
                        dispatchEvent(context, "KEYBOARD_SYNC", bundle)
                    }
                })
        } catch (_: Throwable) {}
    }

    override fun dispatchEvent(context: Context, eventType: String, data: Bundle) {
        try {
            context.contentResolver.call(PROVIDER_URI, "SATELLITE_UPDATE", eventType, data)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        // Cleanup if necessary
    }
}
