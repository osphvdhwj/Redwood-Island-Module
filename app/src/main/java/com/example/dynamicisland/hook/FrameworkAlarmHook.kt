package com.example.dynamicisland.hook
import com.example.dynamicisland.model.*

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object FrameworkAlarmHook {
    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {
        
        // Hooks into the core Alarm Manager Service inside the Android Kernel
        IslandHookEngine.hookAllMethodsByName(
            "com.android.server.alarm.AlarmManagerService",
            lpparam.classLoader,
            "setImpl",
            object : de.robv.android.xposed.XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val triggerTimeMs = param.args.filterIsInstance<Long>().getOrNull(1) ?: return
                        val callingPackage = param.args.filterIsInstance<String>().lastOrNull() ?: return
                        
                        // Detect AlarmClockInfo presence robustly
                        val isClockAlarm = param.args.any { 
                            it?.javaClass?.name == "android.app.AlarmManager\$AlarmClockInfo" 
                        }
                        
                        if (isClockAlarm && callingPackage != "android") {
                            val mContext = XposedHelpers.callMethod(param.thisObject, "getContext") as? Context ?: return
                            
                            val intent = Intent("com.example.dynamicisland.ALARM_SET").apply {
                                setPackage("com.android.systemui")
                                putExtra("triggerTime", triggerTimeMs)
                                putExtra("pkg", callingPackage)
                            }
                            mContext.sendBroadcastAsUser(intent, userAll)
                        }
                    } catch (_: Throwable) {}
                }
            }
        )
    }
}
