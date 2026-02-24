package com.example.dynamicisland

import android.content.Context
import android.graphics.Color
import android.provider.Settings
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

        // NUCLEAR GHOST FIX
        try {
            val nsslClass = "com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout"

            XposedHelpers.findAndHookMethod(
                nsslClass,
                lpparam.classLoader,
                "onChildViewAdded",
                View::class.java,
                View::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val child = param.args[1] as View
                            if (IslandController.isExpanding()) {
                                if (child.javaClass.name.contains("ExpandableNotificationRow")) {
                                    log("[NUCLEAR] Banishing notification off-screen")
                                    // Move it waaaay off screen
                                    child.translationX = 9999f
                                    child.alpha = 0f
                                    child.scaleX = 0f
                                    child.scaleY = 0f
                                }
                            }
                        } catch (e: Throwable) {
                             // Ignore
                        }
                    }
                }
            )

        } catch (e: Throwable) {
            log("[WARN] Failed to hook StackScrollLayout: " + e)
        }
    }

    private fun injectIsland(parentView: ViewGroup) {
        for (i in 0 until parentView.childCount) {
            if (parentView.getChildAt(i) is DynamicIslandView) {
                return
            }
        }

        try {
            val context = parentView.context

            // Read Settings.System instead of SharedPreferences
            var offsetY = 0
            // Removed offsetX calculation as we are centering
            try {
                val cr = context.contentResolver
                offsetY = Settings.System.getInt(cr, "redwood_island_y_offset", 0)
            } catch (e: Throwable) {
                // Ignore errors (permission denied, etc)
            }

            val islandView = DynamicIslandView(context)
            islandView.id = View.generateViewId()

            val lp = FrameLayout.LayoutParams(
                islandView.collapsedWidth,
                islandView.collapsedHeight
            )

            // Fix: Use CENTER_HORIZONTAL gravity for perfect centering
            lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            lp.topMargin = offsetY

            // Removed leftMargin calculation to avoid offset issues

            islandView.layoutParams = lp

            islandView.visibility = View.VISIBLE
            // MAX ELEVATION to fight for top layer
            islandView.elevation = 9999f
            islandView.translationZ = 9999f

            // Add as LAST child to ensure drawing on top
            parentView.addView(islandView, parentView.childCount)

            islandView.bringToFront()

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
            findStatusIcons(parentView)

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

    private fun findStatusIcons(view: View) {
        try {
            val clsName = view.javaClass.name
            if (clsName == "com.android.systemui.statusbar.phone.StatusIconContainer") {
                log("[STATUS_ICONS] Found StatusIconContainer")
                IslandController.setStatusIcons(view)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    findStatusIcons(view.getChildAt(i))
                }
            }
        } catch (e: Throwable) {
             // Ignore
        }
    }
}
