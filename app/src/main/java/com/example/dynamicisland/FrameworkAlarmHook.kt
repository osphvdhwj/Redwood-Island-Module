package com.example.dynamicisland

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object FrameworkAlarmHook {
    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        
        // Hooks into the core Alarm Manager Service inside the Android Kernel
        IslandHookEngine.hookAfter(
            "com.android.server.alarm.AlarmManagerService",
            lpparam.classLoader,
            "setImpl",
            Int::class.javaPrimitiveType ?: Int::class.java,
            Long::class.javaPrimitiveType ?: Long::class.java,
            Long::class.javaPrimitiveType ?: Long::class.java,
            Long::class.javaPrimitiveType ?: Long::class.java,
            android.app.PendingIntent::class.java,
            "android.app.IAlarmListener",
            String::class.java,
            Int::class.javaPrimitiveType ?: Int::class.java,
            android.os.WorkSource::class.java,
            "android.app.AlarmManager.AlarmClockInfo",
            Int::class.javaPrimitiveType ?: Int::class.java,
            String::class.java
        ) { param ->
            val triggerTimeMs = param.args[2] as Long
            val callingPackage = param.args[11] as? String ?: return@hookAfter
            
            // Only care about actual clock alarms, not background system syncs
            val isClockAlarm = param.args[9] != null // AlarmClockInfo is not null
            
            if (isClockAlarm && callingPackage != "android") {
                val mContext = XposedHelpers.callMethod(param.thisObject, "getContext") as? Context ?: return@hookAfter
                
                val intent = Intent("com.example.dynamicisland.ALARM_SET").apply {
                    setPackage("com.android.systemui")
                    putExtra("triggerTime", triggerTimeMs)
                    putExtra("pkg", callingPackage)
                }
                mContext.sendBroadcastAsUser(intent, userAll)
            }
        }
    }
}
