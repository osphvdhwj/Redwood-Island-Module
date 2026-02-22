package com.example.dynamicisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
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
    private var testReceiver: BroadcastReceiver? = null

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

        // Hook PhoneStatusBarView
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

        // Hook StatusBarWindowView fallback
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

        // Hook NotificationStackScrollLayout for "Ghost" fix
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout",
                lpparam.classLoader,
                "onChildViewAdded",
                View::class.java,
                View::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val child = param.args[1] as View
                            // Check if island is trying to expand
                            if (IslandController.isExpanding()) {
                                // Basic check: If it looks like a notification row
                                if (child.javaClass.name.contains("ExpandableNotificationRow")) {
                                    XposedBridge.log("DynamicIsland: [GHOST] Suppressing new notification view during expansion")
                                    child.alpha = 0f
                                    child.visibility = View.INVISIBLE
                                }
                            }
                        } catch (e: Throwable) {
                             // Ignore
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("DynamicIsland: [WARN] Failed to hook StackScrollLayout: " + e)
        }
    }

    private fun setupIsland(context: Context, parentView: ViewGroup) {
        try {
            // Load prefs
            val prefs = XSharedPreferences("com.example.dynamicisland", "dynamic_island_prefs")
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

            // Visibility & Z-Order
            islandView.visibility = View.VISIBLE
            islandView.elevation = 2000f
            islandView.setBackgroundColor(Color.BLACK) // True Black

            // Add to hierarchy
            parentView.addView(islandView)
            XposedBridge.log("DynamicIsland: [UI] Added Island view to " + parentView.javaClass.simpleName)

            // Fix Clipping Recursively
            islandView.post {
                try {
                    var p: Any? = parentView
                    while (p != null && p is ViewGroup) {
                        p.clipChildren = false
                        p.clipToPadding = false
                        p = p.parent
                    }
                    XposedBridge.log("DynamicIsland: [UI] Disabled clipping on parent chain")
                } catch (e: Throwable) {
                    XposedBridge.log("DynamicIsland: [WARN] Failed to disable clipping: " + e)
                }
            }

            // Initialize Controller
            IslandController.init(islandView)

            // Find Clock
            findClock(parentView)

            // Register Broadcast Receiver for Testing
            try {
                val filter = IntentFilter("com.example.dynamicisland.TEST_EXPAND")
                testReceiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        XposedBridge.log("DynamicIsland: [TEST] Broadcast received!")
                        IslandController.testExpand()
                    }
                }
                context.registerReceiver(testReceiver, filter, Context.RECEIVER_EXPORTED)
                XposedBridge.log("DynamicIsland: [TEST] Receiver registered")
            } catch (e: Throwable) {
                XposedBridge.log("DynamicIsland: [WARN] Failed to register receiver: " + e)
            }

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
