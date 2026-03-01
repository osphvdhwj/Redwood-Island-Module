package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    private var islandInitialized = false
    private var windowManager: WindowManager? = null
    private var islandView: DynamicIslandView? = null

    private fun log(msg: String) {
        XposedBridge.log("DynamicIsland: $msg")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return
        log("[LOAD] Hooking SystemUI package: ${lpparam.packageName}")

        // 1. UI Injection - SAFE BOOT STRATEGY
        try {
            // Try hooking CentralSurfacesImpl (Android 13+)
            val centralSurfacesClass = try {
                XposedHelpers.findClass("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpparam.classLoader)
            } catch (e: XposedHelpers.ClassNotFoundError) {
                // Fallback to StatusBar (Android 12 and below)
                try {
                    XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBar", lpparam.classLoader)
                } catch (e2: XposedHelpers.ClassNotFoundError) {
                    log("[ERROR] Could not find CentralSurfacesImpl or StatusBar classes.")
                    null
                }
            }

            if (centralSurfacesClass != null) {
                XposedHelpers.findAndHookMethod(
                    centralSurfacesClass.name,
                    lpparam.classLoader,
                    "start",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val serviceContext = try {
                                XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                            } catch (e: Throwable) {
                                log("[ERROR] Could not find mContext in ${centralSurfacesClass.name}")
                                return
                            }

                            log("[BOOT] ${centralSurfacesClass.simpleName}.start() hooked. Initializing Island...")

                            // Register Screen State Receiver
                            registerScreenReceiver(serviceContext)

                            // Post to main thread just to be safe, but no huge delay needed anymore
                            Handler(Looper.getMainLooper()).post {
                                setupIsland(serviceContext)
                            }
                        }
                    }
                )
            }
        } catch (e: Throwable) {
            log("[ERROR] Failed to hook start method: $e")
        }

        // 2. Initialize Island Framework Hooks (Safe logic hooks)
        try {
            IslandController.hookFrameworkNotifications(lpparam)
        } catch (e: Throwable) {
            log("[ERROR] Framework hook init failed: $e")
        }
    }


    private fun registerScreenReceiver(context: Context) {
        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    when (intent.action) {
                        Intent.ACTION_SCREEN_OFF -> {
                            log("[SCREEN] OFF")
                            IslandController.onScreenStateChanged(false)
                        }
                        Intent.ACTION_SCREEN_ON -> {
                            log("[SCREEN] ON")
                            IslandController.onScreenStateChanged(true)
                        }
                    }
                }
            }

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            context.registerReceiver(receiver, filter)
            log("[SYSTEM] Screen Receiver Registered")
        } catch (e: Throwable) {
            log("[ERROR] Failed to register screen receiver: $e")
        }
    }

    private class ModuleContextWrapper(base: Context, private val module: Context) : android.content.ContextWrapper(base) {
        override fun getResources(): android.content.res.Resources = module.resources
        override fun getTheme(): android.content.res.Resources.Theme = module.theme
        override fun getAssets(): android.content.res.AssetManager = module.assets
        override fun getClassLoader(): ClassLoader = module.classLoader
        override fun getApplicationInfo(): android.content.pm.ApplicationInfo = module.applicationInfo
        override fun getPackageName(): String = module.packageName
    }

    private fun createFallbackContext(systemContext: Context): Context? {
        try {
            val apkPath = MainHook::class.java.protectionDomain.codeSource.location.path
            log("[FALLBACK] Attempting to load resources from: $apkPath")

            // Create AssetManager
            val assetManagerClass = android.content.res.AssetManager::class.java
            val assetManager = assetManagerClass.newInstance()
            val addAssetPathMethod = assetManagerClass.getMethod("addAssetPath", String::class.java)
            addAssetPathMethod.invoke(assetManager, apkPath)

            // Create Resources
            val resources = android.content.res.Resources(
                assetManager,
                systemContext.resources.displayMetrics,
                systemContext.resources.configuration
            )

            // Create Theme
            val theme = resources.newTheme()
            theme.applyStyle(android.R.style.Theme_DeviceDefault_DayNight, true)

            return object : android.content.ContextWrapper(systemContext) {
                override fun getResources(): android.content.res.Resources = resources
                override fun getAssets(): android.content.res.AssetManager = assetManager
                override fun getTheme(): android.content.res.Resources.Theme = theme
                override fun getPackageName(): String = "com.example.dynamicisland"
            }
        } catch (e: Throwable) {
            log("[FATAL] Fallback context creation failed: $e")
            return null
        }
    }

    @SuppressLint("WrongConstant")
    private fun setupIsland(context: Context) {
        if (islandInitialized) {
            log("[INFO] Island already initialized, skipping setup.")
            return
        }

        try {
            // FIX: STRICT Context Creation. If this fails, we try fallback.
            var moduleContext: Context? = null

            try {
                moduleContext = context.createPackageContext(
                    "com.example.dynamicisland",
                    Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
                )
            } catch (e: Exception) {
                log("[WARN] Standard createPackageContext failed: $e. Trying fallback...")
            }

            if (moduleContext == null) {
                moduleContext = createFallbackContext(context)
            }

            if (moduleContext == null) {
                 log("[FATAL] All context creation attempts failed. Aborting.")
                 return
            }

            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            // Use TYPE_STATUS_BAR_SUB_PANEL (2017)
            val windowContext = context.createDisplayContext(display).createWindowContext(2017, null)

            // Combine Window Context (Base) with Module Resources
            val hybridContext = ModuleContextWrapper(windowContext, moduleContext)

            // Wrap with theme for Compose Material 3 (Extra Safety)
            val finalContext = android.view.ContextThemeWrapper(
                hybridContext,
                android.R.style.Theme_DeviceDefault_DayNight
            )

            val wm = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            // Load saved offsets via XSharedPreferences
            var initX = 0
            var initY = 48
            var initW = 24
            var initH = 24

            try {
                val prefs = XSharedPreferences("com.example.dynamicisland", "island_prefs")
                prefs.makeWorldReadable()
                prefs.reload()
                initX = prefs.getInt("offsetX", 0)
                initY = prefs.getInt("offsetY", 48)
                initW = prefs.getInt("camWidth", 24)
                initH = prefs.getInt("camHeight", 24)
            } catch (e: Throwable) {
                log("[WARN] Failed to read XSharedPreferences: $e")
            }

            // Pass THEMED MODULE CONTEXT to View
            try {
                islandView = DynamicIslandView(finalContext)
                islandView!!.id = View.generateViewId()

                // Set initial camera values
                islandView!!.camOffsetX.value = initX
                islandView!!.camOffsetY.value = initY
                islandView!!.camWidth.value = initW
                islandView!!.camHeight.value = initH
            } catch (e: Throwable) {
                log("[FATAL] View creation failed. Aborting: $e")
                return
            }

            // FIX 1: Change Window Type to 2024 (Navigation Bar Panel) to stop crashes and fix touch boundaries
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                2024, // TYPE_NAVIGATION_BAR_PANEL
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.x = initX
            params.y = initY
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

            islandView!!.windowManager = wm
            islandView!!.windowParams = params

            wm.addView(islandView, params)
            islandInitialized = true
            IslandController.init(islandView!!)
            log("[SUCCESS] Dynamic Island initialized safely with Window Type 2024.")
        } catch (e: Throwable) {
            log("[FATAL] setupIsland crashed gracefully: $e")
            // Catch-all to ensure we don't kill SystemUI process
        }
    }
}
