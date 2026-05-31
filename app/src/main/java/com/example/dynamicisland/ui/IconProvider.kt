package com.example.dynamicisland.ui

import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dynamicisland.settings.IconPack

val LocalIconPack = compositionLocalOf<IconPack> { IconPack.MaterialYou }

object IconProvider {
    
    enum class LogicalIcon {
        PLAY, PAUSE, NEXT, PREV, PHONE, BATTERY_FULL, BATTERY_CHARGING, BATTERY_LOW,
        ALARM, MAP, MAIL, SETTINGS, WIFI, BLUETOOTH, CLOSE, HEART, 
        SYNC, TIMER, TORCH, ADD, SHUFFLE, CALL, LOCK, DOWNLOAD, MUSIC, 
        MESSAGE, CHAT, LINK
    }

    @Composable
    fun getIcon(logicalIcon: LogicalIcon, pack: IconPack): ImageVector {
        return when (pack) {
            is IconPack.CupertinoGlass -> getCupertinoIcon(logicalIcon)
            is IconPack.MaterialYou -> getMaterialYouIcon(logicalIcon)
            is IconPack.AmoledCyberpunk -> getCyberpunkIcon(logicalIcon)
            is IconPack.iOS -> getCupertinoIcon(logicalIcon) // Fallback to Cupertino
            is IconPack.OxygenOS -> getMaterialYouIcon(logicalIcon) // Fallback to Material
            is IconPack.OneUI -> getMaterialYouIcon(logicalIcon) // Fallback to Material
        }
    }

    private fun getCupertinoIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.PLAY -> Icons.Rounded.PlayArrow
        LogicalIcon.PAUSE -> Icons.Rounded.Pause
        LogicalIcon.NEXT -> Icons.Rounded.SkipNext
        LogicalIcon.PREV -> Icons.Rounded.SkipPrevious
        LogicalIcon.PHONE, LogicalIcon.CALL -> Icons.Rounded.Call
        LogicalIcon.BATTERY_FULL -> Icons.Rounded.BatteryFull
        LogicalIcon.BATTERY_CHARGING -> ProgrammaticIcons.CupertinoBattery
        LogicalIcon.BATTERY_LOW -> Icons.Rounded.BatteryAlert
        LogicalIcon.ALARM -> ProgrammaticIcons.CupertinoBell
        LogicalIcon.MAP -> Icons.Rounded.LocationOn
        LogicalIcon.MAIL -> Icons.Rounded.Email
        LogicalIcon.SETTINGS -> Icons.Rounded.Settings
        LogicalIcon.WIFI -> Icons.Rounded.Wifi
        LogicalIcon.BLUETOOTH -> Icons.Rounded.Bluetooth
        LogicalIcon.CLOSE -> Icons.Rounded.Close
        LogicalIcon.HEART -> Icons.Rounded.Favorite
        LogicalIcon.SYNC -> Icons.Rounded.Sync
        LogicalIcon.TIMER -> Icons.Rounded.Timer
        LogicalIcon.TORCH -> Icons.Rounded.FlashlightOn
        LogicalIcon.ADD -> Icons.Rounded.Add
        LogicalIcon.SHUFFLE -> Icons.Rounded.Shuffle
        LogicalIcon.LOCK -> Icons.Rounded.Lock
        LogicalIcon.DOWNLOAD -> Icons.Rounded.Download
        LogicalIcon.MUSIC -> ProgrammaticIcons.CupertinoMusic
        LogicalIcon.MESSAGE, LogicalIcon.CHAT -> Icons.Rounded.Chat
        LogicalIcon.LINK -> Icons.Rounded.Link
    }

    private fun getMaterialYouIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.PLAY -> Icons.Filled.PlayArrow
        LogicalIcon.PAUSE -> Icons.Filled.Pause
        LogicalIcon.NEXT -> Icons.Filled.SkipNext
        LogicalIcon.PREV -> Icons.Filled.SkipPrevious
        LogicalIcon.PHONE, LogicalIcon.CALL -> Icons.Filled.Call
        LogicalIcon.BATTERY_FULL -> Icons.Filled.BatteryFull
        LogicalIcon.BATTERY_CHARGING -> ProgrammaticIcons.MaterialBattery
        LogicalIcon.BATTERY_LOW -> Icons.Filled.BatteryAlert
        LogicalIcon.ALARM -> ProgrammaticIcons.MaterialBell
        LogicalIcon.MAP -> Icons.Filled.LocationOn
        LogicalIcon.MAIL -> Icons.Filled.Email
        LogicalIcon.SETTINGS -> Icons.Filled.Settings
        LogicalIcon.WIFI -> Icons.Filled.Wifi
        LogicalIcon.BLUETOOTH -> Icons.Filled.Bluetooth
        LogicalIcon.CLOSE -> Icons.Filled.Close
        LogicalIcon.HEART -> Icons.Filled.Favorite
        LogicalIcon.SYNC -> Icons.Filled.Sync
        LogicalIcon.TIMER -> Icons.Filled.Timer
        LogicalIcon.TORCH -> Icons.Filled.FlashlightOn
        LogicalIcon.ADD -> Icons.Filled.Add
        LogicalIcon.SHUFFLE -> Icons.Filled.Shuffle
        LogicalIcon.LOCK -> Icons.Filled.Lock
        LogicalIcon.DOWNLOAD -> Icons.Filled.Download
        LogicalIcon.MUSIC -> ProgrammaticIcons.MaterialMusic
        LogicalIcon.MESSAGE, LogicalIcon.CHAT -> Icons.Filled.Chat
        LogicalIcon.LINK -> Icons.Filled.Link
    }

    private fun getCyberpunkIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.PLAY -> Icons.Outlined.PlayArrow
        LogicalIcon.PAUSE -> Icons.Outlined.Pause
        LogicalIcon.NEXT -> Icons.Outlined.SkipNext
        LogicalIcon.PREV -> Icons.Outlined.SkipPrevious
        LogicalIcon.PHONE, LogicalIcon.CALL -> Icons.Outlined.Call
        LogicalIcon.BATTERY_FULL -> Icons.Outlined.BatteryFull
        LogicalIcon.BATTERY_CHARGING -> ProgrammaticIcons.CyberpunkBattery
        LogicalIcon.BATTERY_LOW -> Icons.Outlined.BatteryAlert
        LogicalIcon.ALARM -> ProgrammaticIcons.CyberpunkBell
        LogicalIcon.MAP -> Icons.Outlined.LocationOn
        LogicalIcon.MAIL -> Icons.Outlined.Email
        LogicalIcon.SETTINGS -> Icons.Outlined.Settings
        LogicalIcon.WIFI -> Icons.Outlined.Wifi
        LogicalIcon.BLUETOOTH -> Icons.Outlined.Bluetooth
        LogicalIcon.CLOSE -> Icons.Outlined.Close
        LogicalIcon.HEART -> Icons.Outlined.FavoriteBorder
        LogicalIcon.SYNC -> Icons.Outlined.Sync
        LogicalIcon.TIMER -> Icons.Outlined.Timer
        LogicalIcon.TORCH -> Icons.Outlined.FlashlightOn
        LogicalIcon.ADD -> Icons.Outlined.Add
        LogicalIcon.SHUFFLE -> Icons.Outlined.Shuffle
        LogicalIcon.LOCK -> Icons.Outlined.Lock
        LogicalIcon.DOWNLOAD -> Icons.Outlined.Download
        LogicalIcon.MUSIC -> ProgrammaticIcons.CyberpunkMusic
        LogicalIcon.MESSAGE, LogicalIcon.CHAT -> Icons.Outlined.Chat
        LogicalIcon.LINK -> Icons.Outlined.Link
    }
}
