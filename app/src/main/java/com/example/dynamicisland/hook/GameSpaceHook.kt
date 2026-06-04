package com.example.dynamicisland.hook

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.example.dynamicisland.util.XposedExtensions

import com.example.dynamicisland.data.repository.GameHubRepository
import com.example.dynamicisland.domain.dispatchers.StandardDispatcherProvider

object GameSpaceHook {
    private const val TAG = "GameSpaceHook"
    private var isInitialized = false
    private var gameBarView: android.view.View? = null
    
    fun apply(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Universal hooks for MIUI simulation
        hookMiuiEnvironment(lpparam)

        if (lpparam.packageName != "com.android.systemui") return
        
        XposedBridge.log("$TAG: Initializing GameSpace Overlay in SystemUI...")
        
        XposedExtensions.hookMethodIfExists(
            "com.android.systemui.statusbar.phone.CentralSurfacesImpl",
            lpparam.classLoader,
            "start",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val context = XposedExtensions.getObjectFieldSafe(param.thisObject, "mContext") as? Context
                    if (context != null && !isInitialized) {
                        isInitialized = true
                        initGameSpace(context)
                    }
                }
            }
        )
    }

    private fun hookMiuiEnvironment(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 1. Force GPU Tuner visibility
        try {
            XposedHelpers.findAndHookMethod(
                "android.provider.Settings\$Global", lpparam.classLoader, "getInt",
                android.content.ContentResolver::class.java, String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args[1] == "gpu_tuner_switch") {
                            param.result = 1
                        }
                    }
                }
            )
        } catch (_: Throwable) {}

        // 2. Fake MIUI Shared Libraries (needed for ported apps)
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", lpparam.classLoader, "getSystemSharedLibraryNames",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val libs = param.result as? Array<String> ?: return
                        if (!libs.contains("miui")) {
                            param.result = libs + arrayOf("miui", "com.miui.system", "com.miui.core")
                        }
                    }
                }
            )
        } catch (_: Throwable) {}

        // 3. Fake MIUI Product Build (trick app logic)
        try {
            XposedHelpers.setStaticObjectField(android.os.Build::class.java, "MANUFACTURER", "Xiaomi")
            XposedHelpers.setStaticObjectField(android.os.Build::class.java, "BRAND", "Xiaomi")
        } catch (_: Throwable) {}
    }
    
    private fun initGameSpace(context: Context) {
        Handler(Looper.getMainLooper()).post {
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val moduleContext = try {
                    context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY)
                } catch (e: Exception) { context }
                
                val gameHubRepo = GameHubRepository(context, StandardDispatcherProvider())
                gameHubRepo.onStart()

                val view = com.example.dynamicisland.ui.GameSpaceComposeView(context, moduleContext, wm, gameHubRepo)
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = 0
                    y = 100
                    // Allow island to float above status bar if needed
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
                
                wm.addView(view, params)
                gameBarView = view
                
                XposedBridge.log("$TAG: GameSpace overlay initialized successfully.")
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ❌: Failed to initialize GameSpace: ${e.message}")
            }
        }
    }
}
