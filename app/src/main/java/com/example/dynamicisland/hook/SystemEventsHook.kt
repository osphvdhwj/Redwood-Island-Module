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
    //   CrDroid / Infinity X → setResumedActivityUncheckLocked in ATMS //For Infinity X A15
    //   Evolution X Android 15 → onTaskMovedToFront in TaskStackChangeListener
    //
    // We hook BOTH. Whichever fires on the ROM, we catch it.

    private fun hookAppTransitions(
        lpparam: XC_LoadPackage.LoadPackageParam,
        userAll: UserHandle
    ) {
        var hooked = false

        // ── Strategy A: ATMS setResumedActivityUncheckLocked ─────────────────
        // Works on most AOSP variants
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

        val atmsClasses = listOf(
            "com.android.server.wm.ActivityTaskManagerService",
            "com.android.server.am.ActivityManagerService"
        )
        val atmsMethods = listOf("setResumedActivityUncheckLocked", "setResumedActivity")

        for (cls in atmsClasses) {
            for (method in atmsMethods) {
                val count = IslandHookEngine.hookAllMethodsByName(cls, lpparam.classLoader, method, atmsCallback)
                if (count > 0) {
                    hooked = true
                    break
                }
            }
            if (hooked) break
        }

        // ── Strategy B: TaskChangeNotificationController.notifyTaskMovedToFront ──
        // This is a reliable point in System Server to catch app swaps
        val taskStackCallback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val taskId = param.args.firstOrNull() as? Int ?: return
                    val atm = param.thisObject
                    val task = XposedHelpers.callMethod(atm, "getTask", taskId) ?: return
                    val pkg = XposedHelpers.getObjectField(task, "realActivity")
                        ?.let { XposedHelpers.callMethod(it, "getPackageName") as? String } ?: return

                    val ctx = getContext(param) ?: return
                    broadcastAppChange(ctx, pkg, userAll)
                } catch (_: Throwable) {}
            }
        }

        if (!hooked) {
            val count = IslandHookEngine.hookAllMethodsByName(
                "com.android.server.wm.TaskChangeNotificationController",
                lpparam.classLoader,
                "notifyTaskMovedToFront",
                taskStackCallback
            )
            if (count > 0) hooked = true
        }

        // ── Strategy C: fallback scan ─────────────────────────────────────────
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

        // ── Standard & Live Notifications ──────────────────────────────────
        if (pkgName != "com.android.systemui" && pkgName != "android") {
            ctx.sendBroadcastAsUser(
                Intent("com.example.dynamicisland.NOTIFICATION_CAUGHT").apply {
                    setPackage("com.android.systemui")
                    putExtra("pkg", pkgName)
                    putExtra("notification", notification)
                },
                userAll
            )
        }

        // ── OTP extraction (multilingual, context-aware) ──────────────────
        val otpText = "$title $text"
        if (com.example.dynamicisland.intelligence.OtpTokenizer.looksLikeOtpMessage(otpText)) {
            val result = com.example.dynamicisland.intelligence.OtpTokenizer
                .extract(otpText, pkgName)
            if (result != null && result.isHighConfidence) {
                ctx.sendBroadcastAsUser(
                    Intent("com.example.dynamicisland.OTP_CAUGHT").apply {
                        setPackage("com.android.systemui")
                        putExtra("otp", result.code)
                        putExtra("pkg", pkgName)
                        putExtra("confidence", result.confidence)
                        putExtra("language", result.language.name)
                    },
                    userAll
                )
            }
        }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun getContext(param: XC_MethodHook.MethodHookParam): Context? {
        val candidates = listOf("mContext", "context", "mBase")
        for (field in candidates) {
            try {
                val ctx = XposedHelpers.getObjectField(param.thisObject, field) as? Context
                if (ctx != null) return ctx
            } catch (_: Throwable) {}
        }
        return try {
            android.app.AndroidAppHelper.currentApplication()
        } catch (_: Throwable) { null }
    }
}
