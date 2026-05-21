package com.example.dynamicisland.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dynamicisland.settings.IconPack

object IconProvider {
    
    enum class LogicalIcon {
        PLAY, PAUSE, NEXT, PREV, PHONE, BATTERY_FULL, BATTERY_CHARGING, ALARM, MAP, MAIL, SETTINGS, WIFI, BLUETOOTH
    }

    @Composable
    fun getIcon(logicalIcon: LogicalIcon, pack: IconPack): ImageVector {
        return when (pack) {
            IconPack.CUPERTINO_GLASS -> when (logicalIcon) {
                LogicalIcon.PLAY -> Icons.Rounded.PlayArrow
                LogicalIcon.PAUSE -> Icons.Rounded.Pause
                LogicalIcon.NEXT -> Icons.Rounded.SkipNext
                LogicalIcon.PREV -> Icons.Rounded.SkipPrevious
                LogicalIcon.PHONE -> Icons.Rounded.Call
                LogicalIcon.BATTERY_FULL -> Icons.Rounded.BatteryFull
                LogicalIcon.BATTERY_CHARGING -> Icons.Rounded.BatteryChargingFull
                LogicalIcon.ALARM -> Icons.Rounded.Notifications
                LogicalIcon.MAP -> Icons.Rounded.LocationOn
                LogicalIcon.MAIL -> Icons.Rounded.Email
                LogicalIcon.SETTINGS -> Icons.Rounded.Settings
                LogicalIcon.WIFI -> Icons.Rounded.Wifi
                LogicalIcon.BLUETOOTH -> Icons.Rounded.Bluetooth
            }
            IconPack.MATERIAL_YOU -> when (logicalIcon) {
                LogicalIcon.PLAY -> Icons.Filled.PlayArrow
                LogicalIcon.PAUSE -> Icons.Filled.Pause
                LogicalIcon.NEXT -> Icons.Filled.SkipNext
                LogicalIcon.PREV -> Icons.Filled.SkipPrevious
                LogicalIcon.PHONE -> Icons.Filled.Call
                LogicalIcon.BATTERY_FULL -> Icons.Filled.BatteryFull
                LogicalIcon.BATTERY_CHARGING -> Icons.Filled.BatteryChargingFull
                LogicalIcon.ALARM -> Icons.Filled.Notifications
                LogicalIcon.MAP -> Icons.Filled.LocationOn
                LogicalIcon.MAIL -> Icons.Filled.Email
                LogicalIcon.SETTINGS -> Icons.Filled.Settings
                LogicalIcon.WIFI -> Icons.Filled.Wifi
                LogicalIcon.BLUETOOTH -> Icons.Filled.Bluetooth
            }
            IconPack.AMOLED_CYBERPUNK -> when (logicalIcon) {
                LogicalIcon.PLAY -> Icons.Outlined.PlayArrow
                LogicalIcon.PAUSE -> Icons.Outlined.Pause
                LogicalIcon.NEXT -> Icons.Outlined.SkipNext
                LogicalIcon.PREV -> Icons.Outlined.SkipPrevious
                LogicalIcon.PHONE -> Icons.Outlined.Call
                LogicalIcon.BATTERY_FULL -> Icons.Outlined.BatteryFull
                LogicalIcon.BATTERY_CHARGING -> Icons.Outlined.BatteryChargingFull
                LogicalIcon.ALARM -> Icons.Outlined.Notifications
                LogicalIcon.MAP -> Icons.Outlined.LocationOn
                LogicalIcon.MAIL -> Icons.Outlined.Email
                LogicalIcon.SETTINGS -> Icons.Outlined.Settings
                LogicalIcon.WIFI -> Icons.Outlined.Wifi
                LogicalIcon.BLUETOOTH -> Icons.Outlined.Bluetooth
            }
        }
    }
}
