package com.example.dynamicisland

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object SystemEventsHook {
    private val lastBroadcastMap = mutableMapOf<String, Pair<Long, String>>()

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        
        // 1. Hook App Transitions (Activity Manager)
        // 1. Hook App Transitions (Activity Manager)
        // We try both the AOSP 13+ signature and the standard signature safely
        try {
            IslandHookEngine.hookMethodSafe(
                "com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader, "setResumedActivityUncheckLocked",
                "com.android.server.wm.ActivityRecord", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val activityRecord = param.args[0] ?: return
                            val packageName = XposedHelpers.getObjectField(activityRecord, "packageName") as? String ?: return
                            val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                            val intent = Intent("com.example.dynamicisland.APP_CHANGED").setPackage("com.android.systemui").putExtra("pkg", packageName)
                            mContext?.sendBroadcastAsUser(intent, userAll)
                        } catch (e: Throwable) {}
                    }
                }
            )
        } catch (e: Throwable) {
            // Fallback for custom ROMs
            IslandHookEngine.hookMethodSafe(
                "com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader, "setResumedActivityUncheckLocked",
                "com.android.server.wm.ActivityRecord",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val activityRecord = param.args[0] ?: return
                            val packageName = XposedHelpers.getObjectField(activityRecord, "packageName") as? String ?: return
                            val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                            val intent = Intent("com.example.dynamicisland.APP_CHANGED").setPackage("com.android.systemui").putExtra("pkg", packageName)
                            mContext?.sendBroadcastAsUser(intent, userAll)
                        } catch (e: Throwable) {}
                    }
                }
            )
        }

        // 2. Hook Notifications & OTP Extraction
        IslandHookEngine.hookMethodSafe(
            "com.android.server.notification.NotificationManagerService",
            lpparam.classLoader,
            "enqueueNotificationInternal",
            String::class.java, String::class.java, Int::class.javaPrimitiveType ?: Int::class.java, Int::class.javaPrimitiveType ?: Int::class.java, String::class.java, Int::class.javaPrimitiveType ?: Int::class.java, android.app.Notification::class.java, Int::class.javaPrimitiveType ?: Int::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val pkgName = param.args[0] as? String ?: return
                        val notification = param.args.firstOrNull { it is android.app.Notification } as? android.app.Notification ?: return
                        val extras = notification.extras
                        val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                        
                        val text = extras.getString(android.app.Notification.EXTRA_TEXT) ?: ""
                        val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
                        val isOngoing = (notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0

                        if (isOngoing && pkgName != "com.android.systemui" && pkgName != "android") {
                            val currentTime = System.currentTimeMillis()
                            val lastData = lastBroadcastMap[pkgName]
                            
                            if (title != (lastData?.second ?: "") || currentTime - (lastData?.first ?: 0L) > 1000) {
                                lastBroadcastMap[pkgName] = Pair(currentTime, title)
                                val progress = extras.getInt(android.app.Notification.EXTRA_PROGRESS, -1)
                                val progressMax = extras.getInt(android.app.Notification.EXTRA_PROGRESS_MAX, -1)

                                val intent = Intent("com.example.dynamicisland.LIVE_ACTIVITY_CAUGHT").apply {
                                    setPackage("com.android.systemui")
                                    putExtra("pkg", pkgName); putExtra("title", title); putExtra("text", text)
                                    if (progress != -1) putExtra("progress", progress)
                                    if (progressMax != -1) putExtra("progressMax", progressMax)
                                }
                                mContext?.sendBroadcastAsUser(intent, userAll)
                            }
                        }

                        if (text.contains("OTP", true) || text.contains("code", true) || text.contains("verification", true)) {
                            val match = Regex("\\b\\d{4,8}\\b").find(text)
                            if (match != null) {
                                val intent = Intent("com.example.dynamicisland.OTP_CAUGHT").setPackage("com.android.systemui").putExtra("otp", match.value).putExtra("pkg", pkgName)
                                mContext?.sendBroadcastAsUser(intent, userAll)
                            }
                        }
                    } catch (e: Throwable) {}
                }
            }
        )
    }
}
