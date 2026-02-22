package com.example.dynamicisland

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.Gravity
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        XposedBridge.log("DynamicIsland: Hooking SystemUI")

        XposedHelpers.findAndHookMethod(
            "com.android.systemui.statusbar.phone.PhoneStatusBarView",
            lpparam.classLoader,
            "onFinishInflate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val statusBarView = param.thisObject as ViewGroup
                    val context = statusBarView.context

                    XposedBridge.log("DynamicIsland: PhoneStatusBarView inflated")

                    // Load prefs
                    val prefs = XSharedPreferences("com.example.dynamicisland", "dynamic_island_prefs")
                    prefs.makeWorldReadable()

                    val offsetY = prefs.getInt("offset_y", 0)

                    // Create Island
                    val islandView = DynamicIslandView(context)

                    // Set LayoutParams
                    val lp = FrameLayout.LayoutParams(
                        islandView.collapsedWidth,
                        islandView.collapsedHeight
                    )
                    lp.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
                    lp.topMargin = offsetY

                    islandView.layoutParams = lp

                    // Add to hierarchy
                    statusBarView.addView(islandView)

                    // Fix Z-Order / Clipping
                    var parent = statusBarView.parent
                    while (parent != null && parent is ViewGroup) {
                        parent.clipChildren = false
                        parent.clipToPadding = false
                        parent = parent.parent
                    }
                    statusBarView.clipChildren = false
                    statusBarView.clipToPadding = false

                    // Initialize Controller
                    IslandController.init(islandView)

                    // Find Clock
                    this@MainHook.findClock(statusBarView)
                }
            }
        )

        // Hook HeadsUpManager
        IslandController.hookHeadsUpManager(lpparam)
    }

    private fun findClock(view: View) {
        if (view.javaClass.name == "com.android.systemui.statusbar.policy.Clock") {
            XposedBridge.log("DynamicIsland: Clock found")
            IslandController.setClock(view)
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findClock(view.getChildAt(i))
            }
        }
    }
}
