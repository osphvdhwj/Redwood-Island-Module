package com.example.dynamicisland

import android.content.Context
import android.graphics.Color
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

    private var islandInitialized = false

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        XposedBridge.log("DynamicIsland: [LOAD] Hooking SystemUI package: " + lpparam.packageName)

        val initHook = object : XC_MethodHook() {
             override fun afterHookedMethod(param: MethodHookParam) {
                 if (islandInitialized) return
                 try {
                     val view = param.thisObject as ViewGroup
                     val context = view.context
                     XposedBridge.log("DynamicIsland: [HOOK] View inflated: " + view.javaClass.simpleName)

                     setupIsland(context, view)
                     islandInitialized = true
                 } catch (e: Throwable) {
                     XposedBridge.log("DynamicIsland: [ERROR] Hook failed: " + e)
                 }
             }
        }

        // STRATEGY 1: Hook PhoneStatusBarView
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                lpparam.classLoader,
                "onFinishInflate",
                initHook
            )
            XposedBridge.log("DynamicIsland: [INIT] Hooked PhoneStatusBarView")
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [WARN] Failed to find PhoneStatusBarView: " + e)
        }

        // STRATEGY 2: Hook StatusBarWindowView (Often higher in hierarchy)
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.window.StatusBarWindowView",
                lpparam.classLoader,
                "onFinishInflate",
                initHook
            )
             XposedBridge.log("DynamicIsland: [INIT] Hooked StatusBarWindowView")
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [WARN] Failed to find StatusBarWindowView: " + e)
        }

        // Hook HeadsUpManager
        IslandController.hookHeadsUpManager(lpparam)
    }

    private fun setupIsland(context: Context, parentView: ViewGroup) {
        try {
            // Load prefs
            val prefs = XSharedPreferences("com.example.dynamicisland", "dynamic_island_prefs")

            val offsetY = prefs.getInt("offset_y", 0)
            XposedBridge.log("DynamicIsland: [CONFIG] Offset Y: " + offsetY)

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

            // DEBUG: Force visibility and distinct color
            islandView.visibility = View.VISIBLE
            islandView.elevation = 2000f // Extremely High Z-index
            islandView.setBackgroundColor(Color.RED) // TEMPORARY: Red for visibility check

            // Add to hierarchy
            parentView.addView(islandView)
            XposedBridge.log("DynamicIsland: [UI] Added Island view (RED) to parent: " + parentView.javaClass.simpleName)

            // Fix Z-Order / Clipping
            var p: Any? = parentView.parent
            while (p != null && p is ViewGroup) {
                p.clipChildren = false
                p.clipToPadding = false
                p = p.parent
            }
            parentView.clipChildren = false
            parentView.clipToPadding = false

            // Initialize Controller
            IslandController.init(islandView)

            // Find Clock
            findClock(parentView)

        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [ERROR] setupIsland crashed: " + e)
        }
    }

    private fun findClock(view: View) {
        try {
            val clsName = view.javaClass.name
            if (clsName.contains("Clock") && !clsName.contains("Desupported")) {
                XposedBridge.log("DynamicIsland: [CLOCK] Found potential clock: " + clsName)
                IslandController.setClock(view)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    findClock(view.getChildAt(i))
                }
            }
        } catch (e: Throwable) {
             // Ignore traversal errors
        }
    }
}
