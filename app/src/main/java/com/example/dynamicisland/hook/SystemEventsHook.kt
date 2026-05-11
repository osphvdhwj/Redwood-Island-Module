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
    //
    // ROM compatibility:
    //   CrDroid / Infinity X → setResumedActivityUncheckLocked in ATMS
    //   Evolution X Android 15 → onTaskMovedToFront in TaskStackChangeListener
    //
    // We hook BOTH. Whichever fires on the ROM, we catch it.

    private fun hookAppTransitions(
        lpparam: XC_LoadPackage.LoadPackageParam,
        userAll: UserHandle
    ) {
        var hooked = false

        // ── Strategy A: ATMS setResumedActivityUncheckLocked ─────────────────
        // Works on CrDroid / Infinity X
        val atmsCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val record = param.args.firstOrNull() ?: return
                    val pkg = try {
                        XposedHelpers.getObjectField(record, "packageName") as? String
                    } catch (_: Throwable) {
                        try {
                            XposedHelpers.getObjectField(record, "mPackageName") as? String
                        } catch (_: Throwable) { null }
                    } ?: return

                    val ctx = getContext(param) ?: return
                    broadcastAppChange(ctx, pkg, userAll)
                } catch (_: Throwable) {}
            }
        }

        val atmsCandidates = listOf(
            "com.android.server.wm.ActivityTaskManagerService" to "setResumedActivityUncheckLocked",
            "com.android.server.am.ActivityManagerService"     to "setResumedActivityUncheckLocked",
            "com.android.server.wm.ActivityRecord"             to "onResumeActivityItem",
        )

        for ((cls, method) in atmsCandidates) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                val unhooks = XposedBridge.hookAllMethods(clazz, method, atmsCallback)
                if (unhooks.isNotEmpty()) {
                    XposedBridge.log("$TAG ✅: App transitions hooked via $cls.$method")
                    hooked = true
                    break
                }
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: $cls.$method — ${e.message}")
            }
        }

        // ── Strategy B: TaskStackChangeListener.onTaskMovedToFront ───────────
        // Confirmed on Evolution X Android 15 from TaskStackChangeListener.smali
        // onTaskMovedToFront(RunningTaskInfo) → extracts packageName from topActivity
        val taskStackCandidates = listOf(
            "com.android.systemui.shared.system.TaskStackChangeListener",
            "com.android.server.wm.TaskChangeNotificationController",
            "android.app.ITaskStackListener",
        )

        val taskStackCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val taskInfo = param.args.firstOrNull() ?: return

                    // Extract package from RunningTaskInfo.topActivity (ComponentName)
                    val pkg = try {
                        val topActivity = XposedHelpers.getObjectField(taskInfo, "topActivity")
                        XposedHelpers.callMethod(topActivity, "getPackageName") as? String
                    } catch (_: Throwable) {
                        // Fallback: try baseActivity
                        try {
                            val baseActivity = XposedHelpers.getObjectField(taskInfo, "baseActivity")
                            XposedHelpers.callMethod(baseActivity, "getPackageName") as? String
                        } catch (_: Throwable) { null }
                    } ?: return

                    val ctx = try {
                        android.app.AndroidAppHelper.currentApplication()
                    } catch (_: Throwable) { null } ?: return

                    broadcastAppChange(ctx, pkg, userAll)
                } catch (_: Throwable) {}
            }
        }

        for (cls in taskStackCandidates) {
            val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: continue
            try {
                // Hook the RunningTaskInfo overload confirmed in smali line 146
                val unhooks = XposedBridge.hookAllMethods(
                    clazz, "onTaskMovedToFront", taskStackCallback
                )
                if (unhooks.isNotEmpty()) {
                    XposedBridge.log("$TAG ✅: App transitions hooked via $cls.onTaskMovedToFront")
                    hooked = true
                    break
                }
            } catch (e: Throwable) {
                XposedBridge.log("$TAG ⚠️: $cls.onTaskMovedToFront — ${e.message}")
            }
        }

        // ── Strategy C: scan fallback ─────────────────────────────────────────
        if (!hooked) {
            XposedBridge.log("$TAG ⚠️: All app transition strategies failed — scanning ATMS")
            IslandHookEngine.scanAndHook(
                "com.android.server.wm.ActivityTaskManagerService",
                lpparam.classLoader,
                "setResumed",
                atmsCallback
            )
        }
    }

    private fun broadcastAppChange(ctx: Context, pkg: String, userAll: UserHandle) {
        ctx.sendBroadcastAsUser(
            Intent("com.example.dynamicisland.APP_CHANGED").apply {
                setPackage("com.android.systemui")
                putExtra("pkg", pkg)
            },
            userAll
        )
    }

    // ── 2. Notifications ──────────────────────────────────────────────────────
    //
    // Evolution X Android 15 confirmed overloads from NotificationManagerService.smali:
    //
    //   Overload 1 (line 8747): enqueueNotificationInternal(String,String,I,I,String,I,Notification,I,Z,Z)V
    //   Overload 2 (line 8780): enqueueNotificationInternal(String,String,I,I,String,I,Notification,I,Z,Z,Z)V  ← main
    //   Overload 3 (line 8837): enqueueNotificationInternal(String,String,I,I,String,I,Notification,I,Z,PostNotificationTracker,Z,Z)Z
    //
    // We use hookAllMethodsByName to hook ALL overloads regardless of signature.
    // This makes it work on CrDroid, Infinity X, and Evolution X simultaneously.

    private fun hookNotifications(
        lpparam: XC_LoadPackage.LoadPackageParam,
        userAll: UserHandle
    ) {
        val notifCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    processNotificationParams(param, userAll)
                } catch (_: Throwable) {}
            }
        }

        // Hook ALL overloads — confirmed 3 overloads on Evolution X
        val count = IslandHookEngine.hookAllMethodsByName(
            "com.android.server.notification.NotificationManagerService",
            lpparam.classLoader,
            "enqueueNotificationInternal",
            notifCallback
        )

        // Fallback for older ROMs that used different method name
        if (count == 0) {
            IslandHookEngine.hookAllMethodsByName(
                "com.android.server.notification.NotificationManagerService",
                lpparam.classLoader,
                "enqueueNotification",
                notifCallback
            )
        }
    }

    private fun processNotificationParams(
        param: XC_MethodHook.MethodHookParam,
        userAll: UserHandle
    ) {
        // Robustly extract fields by type — not by index
        // Works across all 3 overloads confirmed in Evolution X smali
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
            val now  = System.currentTimeMillis()
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
        if (combined.contains("OTP", true)          ||
            combined.contains("code", true)          ||
            combined.contains("verification", true)  ||
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
