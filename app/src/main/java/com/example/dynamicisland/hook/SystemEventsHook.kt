package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object SystemEventsHook {

    private const val TAG = "DynamicIsland"
    private val lastBroadcastMap = mutableMapOf<String, Pair<Long, String>>()

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        hookAppTransitions(lpparam, userAll)
        hookNotifications(lpparam, userAll)
    }

    // ── 1. App transitions ────────────────────────────────────────────────────

    private fun hookAppTransitions(
        lpparam: XC_LoadPackage.LoadPackageParam,
        userAll: UserHandle
    ) {
        // Ordered list of (class, method) candidates across AOSP 12–15 + OEM ROMs
        val candidates = listOf(
            "com.android.server.wm.ActivityTaskManagerService" to "setResumedActivityUncheckLocked",
            "com.android.server.am.ActivityManagerService"     to "setResumedActivityUncheckLocked",
            "com.android.server.wm.ActivityRecord"             to "onResumeActivityItem",  // fallback
        )

        val callback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    // First arg is the ActivityRecord on ATMS variants
                    val record = param.args.firstOrNull() ?: return
                    val pkg = try {
                        XposedHelpers.getObjectField(record, "packageName") as? String
                    } catch (_: Throwable) {
                        try {
                            XposedHelpers.getObjectField(record, "mPackageName") as? String
                        } catch (_: Throwable) { null }
                    } ?: return

                    val ctx = getContext(param) ?: return
                    ctx.sendBroadcastAsUser(
                        Intent("com.example.dynamicisland.APP_CHANGED").apply {
                            setPackage("com.android.systemui")
                            putExtra("pkg", pkg)
                        },
                        userAll
                    )
                } catch (_: Throwable) {}
            }
        }

        var hooked = false
        for ((cls, method) in candidates) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                // Hook all overloads — the signature differs between Android versions
                val unhooks = XposedBridge.hookAllMethods(clazz, method, callback)
                if (unhooks.isNotEmpty()) {
                    XposedBridge.log("$TAG ✅: App transitions hooked via $cls.$method")
                    hooked = true
                    break
                }
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: App transition $cls.$method — ${e.message}")
            }
        }

        if (!hooked) {
            // Last resort: scan ActivityTaskManagerService for any "setResumed" method
            IslandHookEngine.scanAndHook(
                "com.android.server.wm.ActivityTaskManagerService",
                lpparam.classLoader,
                "setResumed",
                callback
            )
        }
    }

    // ── 2. Notifications (OTP + live activities) ──────────────────────────────

    private fun hookNotifications(
        lpparam: XC_LoadPackage.LoadPackageParam,
        userAll: UserHandle
    ) {
        val nmsClass = XposedHelpers.findClassIfExists(
            "com.android.server.notification.NotificationManagerService",
            lpparam.classLoader
        ) ?: run {
            XposedBridge.log("$TAG ⚠️: NotificationManagerService not found")
            return
        }

        // Hook ALL overloads of enqueueNotificationInternal — signature changes every major version
        val count = IslandHookEngine.hookAllMethodsByName(
            "com.android.server.notification.NotificationManagerService",
            lpparam.classLoader,
            "enqueueNotificationInternal",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        processNotificationParams(param, userAll)
                    } catch (_: Throwable) {}
                }
            }
        )

        // Fallback: also hook enqueueNotification (older AOSP name)
        if (count == 0) {
            IslandHookEngine.hookAllMethodsByName(
                "com.android.server.notification.NotificationManagerService",
                lpparam.classLoader,
                "enqueueNotification",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            processNotificationParams(param, userAll)
                        } catch (_: Throwable) {}
                    }
                }
            )
        }
    }

    private fun processNotificationParams(
        param: XC_MethodHook.MethodHookParam,
        userAll: UserHandle
    ) {
        // Robustly extract fields by type scanning — not by index
        val pkgName = param.args
            .filterIsInstance<String>()
            .firstOrNull()
            ?: return

        val notification = param.args
            .filterIsInstance<android.app.Notification>()
            .firstOrNull()
            ?: return

        val extras = notification.extras ?: return
        val ctx = getContext(param) ?: return

        val text  = extras.getString(android.app.Notification.EXTRA_TEXT)  ?: ""
        val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
        val isOngoing = (notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0

        // ── Live activity (ongoing notification with progress) ────────────────
        if (isOngoing && pkgName != "com.android.systemui" && pkgName != "android") {
            val now = System.currentTimeMillis()
            val last = lastBroadcastMap[pkgName]
            if (title != (last?.second ?: "") || now - (last?.first ?: 0L) > 1000) {
                lastBroadcastMap[pkgName] = Pair(now, title)

                val progress    = extras.getInt(android.app.Notification.EXTRA_PROGRESS, -1)
                val progressMax = extras.getInt(android.app.Notification.EXTRA_PROGRESS_MAX, -1)

                ctx.sendBroadcastAsUser(
                    Intent("com.example.dynamicisland.LIVE_ACTIVITY_CAUGHT").apply {
                        setPackage("com.android.systemui")
                        putExtra("pkg",  pkgName)
                        putExtra("title", title)
                        putExtra("text",  text)
                        if (progress    != -1) putExtra("progress",    progress)
                        if (progressMax != -1) putExtra("progressMax", progressMax)
                    },
                    userAll
                )
            }
        }

        // ── OTP extraction ────────────────────────────────────────────────────
        val combined = "$title $text"
        if (combined.contains("OTP", true) ||
            combined.contains("code", true) ||
            combined.contains("verification", true) ||
            combined.contains("one time", true)) {

            val match = Regex("\\b\\d{4,8}\\b").find(combined)
            if (match != null) {
                ctx.sendBroadcastAsUser(
                    Intent("com.example.dynamicisland.OTP_CAUGHT").apply {
                        setPackage("com.android.systemui")
                        putExtra("otp", match.value)
                        putExtra("pkg", pkgName)
                    },
                    userAll
                )
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun getContext(param: XC_MethodHook.MethodHookParam): Context? {
        return try {
            XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
        } catch (_: Throwable) {
            try {
                android.app.AndroidAppHelper.currentApplication()
            } catch (_: Throwable) { null }
        }
    }
}
