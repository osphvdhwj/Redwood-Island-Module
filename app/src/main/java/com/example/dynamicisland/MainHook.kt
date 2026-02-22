package com.example.dynamicisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.Gravity
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LayoutInflated

class MainHook : IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private var islandInitialized = false

    private fun log(msg: String) {
        XposedBridge.log("DynamicIsland: " + msg)
    }

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        if (resparam.packageName != "com.android.systemui") return

        log("[RES] Hooking resources for SystemUI")

        val layouts = arrayOf("status_bar", "super_status_bar", "phone_status_bar")

        for (layoutName in layouts) {
            try {
                resparam.res.hookLayout("com.android.systemui", "layout", layoutName, object : XC_LayoutInflated() {
                    override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                        log("[LAYOUT] Inflated: " + layoutName)
                        injectIsland(liparam.view as ViewGroup)
                    }
                })
            } catch (e: Throwable) {
                // Not found, ignore
            }
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        log("[LOAD] Hooking SystemUI package: " + lpparam.packageName)

        val initHook = object : XC_MethodHook() {
             override fun afterHookedMethod(param: MethodHookParam) {
                 try {
                     val view = param.thisObject as ViewGroup
                     log("[HOOK] View inflated via code: " + view.javaClass.simpleName)
                     injectIsland(view)
                 } catch (e: Throwable) {
                     log("[ERROR] Hook failed: " + e)
                 }
             }
        }

        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                lpparam.classLoader,
                "onFinishInflate",
                initHook
            )
            log("[INIT] Hooked PhoneStatusBarView")
        } catch (e: Throwable) {
            log("[WARN] PhoneStatusBarView not found")
        }

        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.window.StatusBarWindowView",
                lpparam.classLoader,
                "onFinishInflate",
                initHook
            )
             log("[INIT] Hooked StatusBarWindowView")
        } catch (e: Throwable) {
            log("[WARN] StatusBarWindowView not found")
        }

        IslandController.hookHeadsUpManager(lpparam)
    }

    private fun injectIsland(parentView: ViewGroup) {
        for (i in 0 until parentView.childCount) {
            if (parentView.getChildAt(i) is DynamicIslandView) {
                return
            }
        }

        try {
            val context = parentView.context

            val prefs = XSharedPreferences("com.example.dynamicisland", "dynamic_island_prefs")
            var offsetY = prefs.getInt("offset_y", 0)

            // Adjust Offset slightly up if user said it was "below" punch hole
            // Standard punch hole is usually 0-10px from top, but crDroid might pad it.
            // If user said "below", we might need negative margin or check cutout logic
            // For now, let's trust the Cutout logic in DynamicIslandView to override this if Cutout exists.

            val islandView = DynamicIslandView(context)
            islandView.id = View.generateViewId()

            val lp = FrameLayout.LayoutParams(
                islandView.collapsedWidth,
                islandView.collapsedHeight
            )
            lp.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            lp.topMargin = offsetY

            islandView.layoutParams = lp

            islandView.visibility = View.VISIBLE
            islandView.elevation = 2000f
            // Color handled in view init (Black)

            parentView.addView(islandView)
            islandInitialized = true
            log("[UI] SUCCESS: Added Island (Black) to " + parentView.javaClass.simpleName)

            islandView.post {
                try {
                    var p: Any? = parentView
                    while (p != null && p is ViewGroup) {
                        p.clipChildren = false
                        p.clipToPadding = false
                        p = p.parent
                    }
                    log("[UI] Disabled clipping on parent chain")
                } catch (e: Throwable) {
                    log("[WARN] Failed to disable clipping: " + e)
                }
            }

            IslandController.init(islandView)
            findClock(parentView)

        } catch (e: Throwable) {
            log("[ERROR] injectIsland crashed: " + e)
        }
    }

    private fun findClock(view: View) {
        try {
            val clsName = view.javaClass.name
            if (clsName.contains("Clock") && !clsName.contains("Desupported")) {
                log("[CLOCK] Found potential clock: " + clsName)
                IslandController.setClock(view)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    findClock(view.getChildAt(i))
                }
            }
        } catch (e: Throwable) {
             // Ignore
        }
    }
}
