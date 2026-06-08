package com.example.dynamicisland.hook

import android.view.Display
import android.view.WindowManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
object FloatingWindowHook {
    private const val TAG = "DynamicIsland-FloatingWindowHook"
    private const val TARGET_DISPLAY_ID = 5 // From dumpsys output
    private const val TARGET_OVERLAY_WINDOW_TYPE = 2024 // WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY (or similar overlay type)

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hook the addView method of WindowManagerImpl with precise signature
        IslandHookEngine.hookAfter(
            "android.view.WindowManagerImpl",
            lpparam.classLoader,
            "addView",
            android.view.View::class.java,
            android.view.ViewGroup.LayoutParams::class.java
        ) { param ->
            try {
                val view = param.args[0] as? android.view.View ?: return@hookAfter
                val layoutParams = param.args[1] as? WindowManager.LayoutParams ?: return@hookAfter

                // Attempt to get the displayId from the View's associated Display
                val display = view.display
                val displayId = display?.displayId ?: Display.DEFAULT_DISPLAY // Default to 0 if null

                // Check for the specific floating window characteristics
                if (displayId == TARGET_DISPLAY_ID && layoutParams.width == WindowManager.LayoutParams.MATCH_PARENT && layoutParams.height == WindowManager.LayoutParams.MATCH_PARENT) {
                    XposedBridge.log("$TAG ✅: Floating window detected on Display $displayId (Fullscreen mode)!")
                    XposedBridge.log("$TAG -> Package: ${view.context.packageName}")
                    XposedBridge.log("$TAG -> Window Type: ${layoutParams.type}")
                    XposedBridge.log("$TAG -> Window Title: ${layoutParams.title}")
                    // Generate the XC_MethodHook template as requested
                    // The class handling this is likely WindowManagerImpl (or an internal variant),
                    // and the trigger is the addView call with these specific parameters.
                    // This log acts as the "template" output for successful detection.

                    // To intercept and potentially modify this:
                    // You would typically use a 'beforeHookedMethod' for modification
                    // and 'param.args[1]' would be the WindowManager.LayoutParams to modify.
                    // Example: layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    // param.args[1] = layoutParams // If modifying, ensure the change is passed on.

                } else if (layoutParams.type == TARGET_OVERLAY_WINDOW_TYPE && layoutParams.width == WindowManager.LayoutParams.MATCH_PARENT && layoutParams.height == WindowManager.LayoutParams.MATCH_PARENT) {
                    // This is a more general case for overlays that might not be on display 5
                    // but still exhibit "floating" characteristics with fullscreen windowing mode.
                    XposedBridge.log("$TAG ✅: Overlay window detected with type ${layoutParams.type} and fullscreen windowing mode!")
                    XposedBridge.log("$TAG -> Package: ${view.context.packageName}")
                    XposedBridge.log("$TAG -> Window Display ID: $displayId")
                    XposedBridge.log("$TAG -> Window Title: ${layoutParams.title}")
                }
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ❌: Error in WindowManagerImpl.addView hook: ${e.message}")
            }
        }
    }
}
