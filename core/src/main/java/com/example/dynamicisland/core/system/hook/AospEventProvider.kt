package com.example.dynamicisland.core.system.hook

import com.example.dynamicisland.core.hook.IslandHookEngine
import com.example.dynamicisland.shared.model.LiveActivityModel
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * AOSP Implementation of SystemEventProvider.
 * 
 * Uses standard AOSP method hooks to intercept events.
 */
class AospEventProvider : SystemEventProvider {

    private val _activityEvents = MutableSharedFlow<LiveActivityModel>(extraBufferCapacity = 16)
    override val activityEvents: SharedFlow<LiveActivityModel> = _activityEvents.asSharedFlow()

    private val _hardwareEvents = MutableSharedFlow<HardwareEvent>(extraBufferCapacity = 16)
    override val hardwareEvents: SharedFlow<HardwareEvent> = _hardwareEvents.asSharedFlow()

    private val _mediaEvents = MutableSharedFlow<LiveActivityModel.Music>(extraBufferCapacity = 8)
    override val mediaEvents: SharedFlow<LiveActivityModel.Music> = _mediaEvents.asSharedFlow()

    private val _callEvents = MutableSharedFlow<LiveActivityModel.Call>(extraBufferCapacity = 4)
    override val callEvents: SharedFlow<LiveActivityModel.Call> = _callEvents.asSharedFlow()

    override fun initialize(classLoader: ClassLoader) {
        hookBattery(classLoader)
        hookNotifications(classLoader)
    }

    private fun hookBattery(classLoader: ClassLoader) {
        IslandHookEngine.hookAfter(
            "com.android.server.BatteryService", classLoader,
            "processValuesLocked", Boolean::class.javaPrimitiveType!!
        ) { param ->
            try {
                val batteryProps = XposedHelpers.getObjectField(param.thisObject, "mBatteryProps")
                val level = XposedHelpers.getIntField(batteryProps, "batteryLevel")
                val status = XposedHelpers.getIntField(batteryProps, "batteryStatus")
                
                val isCharging = status == 2 || status == 5 // STATUS_CHARGING or STATUS_FULL
                
                _hardwareEvents.tryEmit(HardwareEvent.BatteryChanged(level, isCharging))
            } catch (_: Exception) {}
        }
    }

    private fun hookNotifications(classLoader: ClassLoader) {
        IslandHookEngine.hookAllMethodsByName(
            "com.android.server.notification.NotificationManagerService", classLoader,
            "enqueueNotificationInternal", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    // Logic to emit events to _activityEvents
                }
            }
        )
    }
}
