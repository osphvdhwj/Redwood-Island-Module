package com.example.dynamicisland.hook

import android.app.Application
import android.content.Context
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

object SystemUIContextKeeper {
    var qsTileHost: Any? = null
}

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        lateinit var modulePath: String

        val USER_ALL: android.os.UserHandle by lazy {
            try {
                android.os.UserHandle::class.java.getField("ALL").get(null)
                    as android.os.UserHandle
            } catch (_: Throwable) {
                try {
                    XposedHelpers.callStaticMethod(
                        android.os.UserHandle::class.java, "of",
                        arrayOf(Int::class.javaPrimitiveType), -1
                    ) as android.os.UserHandle
                } catch (_: Throwable) {
                    android.os.Process.myUserHandle()
                }
            }
        }

        fun log(msg: String)      = XposedBridge.log("DynamicIsland: $msg")
        fun logOk(msg: String)    = XposedBridge.log("DynamicIsland ✅: $msg")
        fun logWarn(msg: String)  = XposedBridge.log("DynamicIsland ⚠️: $msg")
        fun logError(msg: String, t: Throwable? = null) =
            XposedBridge.log("DynamicIsland ❌: $msg${t?.message?.let { " — $it" } ?: ""}")
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
        log("initZygote — module loaded from $modulePath")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("handleLoadPackage: ${lpparam.packageName} (process: ${lpparam.processName})")
        
        val isSystemServer = lpparam.packageName == "android" || 
                           (lpparam.packageName == "system" && lpparam.processName == "android")
        
        val isSystemUI = lpparam.packageName == "com.android.systemui" ||
                        (lpparam.processName == "com.android.systemui" && 
                         (lpparam.packageName == "system" || lpparam.packageName == "android"))

        when {
            isSystemServer -> hookAndroidProcess(lpparam)
            isSystemUI -> hookSystemUIProcess(lpparam)
            lpparam.packageName == "com.android.intentresolver" ||
            lpparam.packageName == "com.google.android.intentresolver" -> hookIntentResolver(lpparam)
        }
    }

    // ── android process ───────────────────────────────────────────────────────

    private fun hookAndroidProcess(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("Applying android-process hooks")
        SurfaceFlingerHook.apply(lpparam, USER_ALL)
        CrDroidAPIHook.apply(lpparam, USER_ALL)
        InfinityXAPIHook.apply(lpparam, USER_ALL)
        FutureFrameworkA15Hooks.apply(lpparam, USER_ALL)
        SystemEventsHook.apply(lpparam, USER_ALL)
        FrameworkTelecomHook.apply(lpparam, USER_ALL)
        DeepTelecomHook.apply(lpparam, USER_ALL)
        FrameworkAlarmHook.apply(lpparam, USER_ALL)
        FrameworkAlarmTriggerHook.apply(lpparam, USER_ALL)
        FrameworkHardwareHook.apply(lpparam, USER_ALL)
        FloatingWindowHook.apply(lpparam)
        hookLinkInterceptor(lpparam)
    }

    private fun hookLinkInterceptor(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            IslandHookEngine.hookMethodSafe(
                "android.app.Instrumentation", lpparam.classLoader, "execStartActivity",
                Context::class.java,
                android.os.IBinder::class.java,
                android.os.IBinder::class.java,
                android.app.Activity::class.java,
                android.content.Intent::class.java,
                Int::class.javaPrimitiveType!!,
                android.os.Bundle::class.java,
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: XC_MethodHook.MethodHookParam): Any? {
                        val intent = param.args[4] as android.content.Intent
                        if (intent.action == android.content.Intent.ACTION_VIEW &&
                            intent.data?.scheme?.startsWith("http") == true) {
                            val host = intent.data?.host ?: ""
                            if (host.contains("youtube.com") || host.contains("youtu.be") ||
                                host.contains("spotify.com")) {
                                android.app.AndroidAppHelper.currentApplication()
                                    ?.sendBroadcast(
                                        android.content.Intent(
                                            "com.example.dynamicisland.LINK_INTERCEPTED"
                                        ).apply {
                                            putExtra("url", intent.dataString)
                                            putExtra("host", host)
                                        }
                                    )
                                return null
                            }
                        }
                        return XposedBridge.invokeOriginalMethod(
                            param.method, param.thisObject, param.args
                        )
                    }
                }
            )
        } catch (_: Throwable) {}
    }

    // ── SystemUI process ──────────────────────────────────────────────────────

    private fun hookSystemUIProcess(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("Applying SystemUI-process hooks (Consolidated A15)")
        SystemUIA15Hooks.init(lpparam)
    }

    // ── Intent resolver process ───────────────────────────────────────────────

    private fun hookIntentResolver(lpparam: XC_LoadPackage.LoadPackageParam) {
        val chooserCandidates = listOf(
            "com.android.intentresolver.ChooserActivity",
            "com.android.internal.app.ChooserActivity",
        )
        for (cls in chooserCandidates) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                XposedHelpers.findAndHookMethod(
                    clazz, "onCreate", android.os.Bundle::class.java,
                    object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: XC_MethodHook.MethodHookParam): Any? {
                            val activity = param.thisObject as android.app.Activity
                            val target = activity.intent
                                .getParcelableExtra<android.content.Intent>(
                                    android.content.Intent.EXTRA_INTENT
                                ) ?: return XposedBridge.invokeOriginalMethod(
                                    param.method, param.thisObject, param.args
                                )
                            android.app.AndroidAppHelper.currentApplication()
                                ?.sendBroadcast(
                                    android.content.Intent(
                                        "com.example.dynamicisland.SHARE_INTERCEPTED"
                                    ).putExtra("raw_intent", target)
                                )
                            activity.finish()
                            return null
                        }
                    }
                )
                logOk("ChooserActivity hooked via $cls")
                break
            } catch (e: Throwable) {
                logWarn("ChooserActivity hook $cls failed: ${e.message}")
            }
        }
    }
}
