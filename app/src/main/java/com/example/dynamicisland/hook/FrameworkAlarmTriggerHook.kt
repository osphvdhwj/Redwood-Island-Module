package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
 * BATCH 4: Framework Alarm Trigger Hook
 *
 * The existing FrameworkAlarmHook.kt fires when an alarm is *scheduled*.
 * This companion hook fires when an alarm actually *triggers*, giving the
 * island a chance to display a live countdown ring and then a full-screen
 * alert the moment the alarm fires — before the system notification appears.
 *
 * Hooks:
 *   AlarmManagerService.triggerAlarmLocked() — fires just before the alarm
 *     PendingIntent is dispatched. We inspect the AlarmClockInfo to filter
 *     for genuine clock alarms vs system background alarms.
 *
 *   AlarmManagerService.cancelAlarmLocked() — fires when an alarm is
 *     dismissed or snoozed. The island uses this to clear its countdown.
 *
 * Broadcast schema (ACTION_ALARM_TRIGGERED):
 *   "triggerTime"  long    epoch millis of the alarm trigger
 *   "pkg"          String  package that set the alarm
 *   "label"        String? user-visible alarm label (if available)
 *   "type"         String  "CLOCK_ALARM" | "TIMER" | "UNKNOWN"
 *
 * Broadcast schema (ACTION_ALARM_DISMISSED):
 *   "triggerTime"  long    same field as above (for matching)
 */
object FrameworkAlarmTriggerHook {

    const val ACTION_TRIGGERED = "com.example.dynamicisland.ALARM_TRIGGERED"
    const val ACTION_DISMISSED  = "com.example.dynamicisland.ALARM_DISMISSED"
    const val ACTION_SNOOZED    = "com.example.dynamicisland.ALARM_SNOOZED"

    // Field/method names differ across AOSP versions — we try in order
    private val TRIGGER_METHOD_CANDIDATES = listOf(
        "triggerAlarmLocked",
        "deliverAlarmsLocked",
        "triggerAlarm"
    )

    private val CANCEL_METHOD_CANDIDATES = listOf(
        "cancelAlarmLocked",
        "removeAlarmsLocked",
        "cancelAlarm"
    )

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {

        // ── Trigger hook ──────────────────────────────────────────────────────
        for (methodName in TRIGGER_METHOD_CANDIDATES) {
            val count = IslandHookEngine.hookAllMethodsByName(
                "com.android.server.alarm.AlarmManagerService",
                lpparam.classLoader,
                methodName,
                object : de.robv.android.xposed.XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        broadcastTrigger(param, userAll)
                    }
                }
            )
            if (count > 0) break
        }

        // ── Cancel/dismiss hook ───────────────────────────────────────────────
        for (methodName in CANCEL_METHOD_CANDIDATES) {
            val count = IslandHookEngine.hookAllMethodsByName(
                "com.android.server.alarm.AlarmManagerService",
                lpparam.classLoader,
                methodName,
                object : de.robv.android.xposed.XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        broadcastDismissed(param, userAll)
                    }
                }
            )
            if (count > 0) break
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun broadcastTrigger(param: de.robv.android.xposed.XC_MethodHook.MethodHookParam, userAll: UserHandle) {
        try {
            val alarmObj = param.args?.firstOrNull() ?: return
            val context  = getContext(param) ?: return

            // Read alarm fields — field names are consistent across AOSP 12-15
            val triggerTime = runCatching {
                XposedHelpers.getLongField(alarmObj, "whenElapsed")
                    .let { System.currentTimeMillis() + (it - android.os.SystemClock.elapsedRealtime()) }
            }.getOrDefault(System.currentTimeMillis())

            val pkg   = runCatching { XposedHelpers.getObjectField(alarmObj, "packageName") as? String }.getOrNull() ?: "android"
            val label = runCatching { XposedHelpers.getObjectField(alarmObj, "statsTag") as? String }.getOrNull()

            // Distinguish clock alarms from background wake-ups
            val clockInfo = runCatching { XposedHelpers.getObjectField(alarmObj, "alarmClock") }.getOrNull()
            val alarmType = when {
                clockInfo != null                       -> "CLOCK_ALARM"
                label?.contains("timer", true) == true -> "TIMER"
                else                                   -> "UNKNOWN"
            }

            if (alarmType == "UNKNOWN") return   // ignore system background alarms

            context.sendBroadcastAsUser(
                Intent(ACTION_TRIGGERED).apply {
                    setPackage("com.example.dynamicisland.core")
                    putExtra("triggerTime", triggerTime)
                    putExtra("pkg",         pkg)
                    putExtra("label",       label)
                    putExtra("type",        alarmType)
                },
                userAll
            )
        } catch (_: Throwable) {}
    }

    private fun broadcastDismissed(param: de.robv.android.xposed.XC_MethodHook.MethodHookParam, userAll: UserHandle) {
        try {
            val alarmObj    = param.args?.firstOrNull() ?: return
            val context     = getContext(param) ?: return
            val triggerTime = runCatching {
                XposedHelpers.getLongField(alarmObj, "whenElapsed")
                    .let { System.currentTimeMillis() + (it - android.os.SystemClock.elapsedRealtime()) }
            }.getOrDefault(0L)

            context.sendBroadcastAsUser(
                Intent(ACTION_DISMISSED).apply {
                    setPackage("com.example.dynamicisland.core")
                    putExtra("triggerTime", triggerTime)
                },
                userAll
            )
        } catch (_: Throwable) {}
    }

    private fun getContext(param: de.robv.android.xposed.XC_MethodHook.MethodHookParam): Context? = runCatching {
        XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
    }.getOrNull()
}