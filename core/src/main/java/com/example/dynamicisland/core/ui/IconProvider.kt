package com.example.dynamicisland.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import com.example.dynamicisland.core.domain.state.*

/**
 * ELITE ICON PROVIDER
 */
val LocalIconPack = compositionLocalOf<IconPack> { IconPack.MaterialYou }

object IconProvider {
    
    enum class LogicalIcon {
        PLAY, PAUSE, NEXT, PREV, PHONE, BATTERY_FULL, BATTERY_CHARGING, BATTERY_LOW,
        ALARM, MAP, MAIL, SETTINGS, WIFI, BLUETOOTH, CLOSE, HEART, 
        SYNC, TIMER, TORCH, ADD, SHUFFLE, CALL, LOCK, DOWNLOAD, MUSIC, 
        MESSAGE, CHAT, LINK, HOTSPOT, DATA, RAM, CPU, SPEED, FIRE, SHIELD
    }

    @Composable
    fun getIcon(logicalIcon: LogicalIcon, pack: IconPack): ImageVector {
        return when (pack) {
            IconPack.iOS            -> getiOSIcon(logicalIcon)
            IconPack.OxygenOS       -> getOxygenIcon(logicalIcon)
            IconPack.Samsung        -> getSamIcon(logicalIcon)
            IconPack.Pixel          -> getPixelIcon(logicalIcon)
            IconPack.Outline        -> getOutlineIcon(logicalIcon)
            IconPack.Futuristic     -> getFuturisticIcon(logicalIcon)
            IconPack.Minimal        -> getMinimalIcon(logicalIcon)
            else                    -> getMaterialYouIcon(logicalIcon)
        }
    }

    private fun getiOSIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.WIFI -> Icons.Rounded.Wifi
        LogicalIcon.BLUETOOTH -> Icons.Rounded.Bluetooth
        LogicalIcon.HOTSPOT -> Icons.Rounded.SettingsInputAntenna
        LogicalIcon.MUSIC -> ProgrammaticIcons.CupertinoMusic
        LogicalIcon.BATTERY_CHARGING -> ProgrammaticIcons.CupertinoBattery
        LogicalIcon.ALARM -> ProgrammaticIcons.CupertinoBell
        else -> getMaterialYouIcon(icon)
    }

    private fun getOxygenIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.WIFI -> ProgrammaticIcons.OxygenWifi
        LogicalIcon.BLUETOOTH -> Icons.Outlined.Bluetooth
        LogicalIcon.BATTERY_FULL -> Icons.Outlined.BatteryFull
        LogicalIcon.SPEED -> Icons.Outlined.Speed
        LogicalIcon.DATA -> Icons.Outlined.SwapVert
        LogicalIcon.RAM -> Icons.Outlined.Memory
        LogicalIcon.CPU -> Icons.Outlined.Memory
        else -> getMaterialYouIcon(icon)
    }

    private fun getSamIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.WIFI -> Icons.Filled.Wifi
        LogicalIcon.BLUETOOTH -> Icons.Filled.Bluetooth
        LogicalIcon.PHONE -> Icons.Filled.Call
        LogicalIcon.DATA -> Icons.Filled.SettingsInputAntenna
        LogicalIcon.RAM -> Icons.Filled.Memory
        LogicalIcon.CPU -> Icons.Filled.Memory
        else -> getMaterialYouIcon(icon)
    }

    private fun getPixelIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.WIFI -> Icons.Rounded.Wifi
        LogicalIcon.SETTINGS -> Icons.Rounded.Settings
        LogicalIcon.RAM -> Icons.Rounded.Memory
        LogicalIcon.CPU -> Icons.Rounded.Memory
        else -> getMaterialYouIcon(icon)
    }

    private fun getOutlineIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.WIFI -> Icons.Outlined.Wifi
        LogicalIcon.BLUETOOTH -> Icons.Outlined.Bluetooth
        LogicalIcon.BATTERY_FULL -> Icons.Outlined.BatteryFull
        LogicalIcon.MAIL -> Icons.Outlined.Mail
        LogicalIcon.RAM -> Icons.Outlined.Memory
        LogicalIcon.CPU -> Icons.Outlined.Memory
        else -> getMaterialYouIcon(icon)
    }

    private fun getFuturisticIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.RAM -> ProgrammaticIcons.FuturisticRAM
        LogicalIcon.CPU -> ProgrammaticIcons.FuturisticCPU
        LogicalIcon.WIFI -> ProgrammaticIcons.CyberpunkWifi
        else -> getCyberpunkIcon(icon)
    }

    private fun getMinimalIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.WIFI -> Icons.Outlined.Wifi
        LogicalIcon.CLOSE -> Icons.Outlined.Close
        LogicalIcon.RAM -> Icons.Outlined.Memory
        LogicalIcon.CPU -> Icons.Outlined.Memory
        else -> getMaterialYouIcon(icon)
    }

    private fun getCyberpunkIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.PLAY -> Icons.Outlined.PlayArrow
        LogicalIcon.PAUSE -> Icons.Outlined.Pause
        LogicalIcon.WIFI -> ProgrammaticIcons.CyberpunkWifi
        LogicalIcon.BLUETOOTH -> Icons.Outlined.Bluetooth
        LogicalIcon.BATTERY_CHARGING -> ProgrammaticIcons.CyberpunkBattery
        LogicalIcon.MUSIC -> ProgrammaticIcons.CyberpunkMusic
        LogicalIcon.ALARM -> ProgrammaticIcons.CyberpunkBell
        LogicalIcon.FIRE -> Icons.Outlined.Whatshot
        LogicalIcon.SHIELD -> Icons.Outlined.Shield
        LogicalIcon.RAM -> ProgrammaticIcons.FuturisticRAM
        LogicalIcon.CPU -> ProgrammaticIcons.FuturisticCPU
        else -> Icons.Outlined.Info
    }

    private fun getMaterialYouIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.PLAY -> Icons.Filled.PlayArrow
        LogicalIcon.PAUSE -> Icons.Filled.Pause
        LogicalIcon.NEXT -> Icons.Filled.SkipNext
        LogicalIcon.PREV -> Icons.Filled.SkipPrevious
        LogicalIcon.PHONE, LogicalIcon.CALL -> Icons.Filled.Call
        LogicalIcon.BATTERY_FULL -> Icons.Filled.BatteryFull
        LogicalIcon.BATTERY_CHARGING -> ProgrammaticIcons.MaterialBattery
        LogicalIcon.WIFI -> Icons.Filled.Wifi
        LogicalIcon.BLUETOOTH -> Icons.Filled.Bluetooth
        LogicalIcon.HOTSPOT -> Icons.Filled.SettingsInputAntenna
        LogicalIcon.DATA -> Icons.Filled.DataUsage
        LogicalIcon.RAM -> Icons.Filled.Memory
        LogicalIcon.CPU -> Icons.Filled.Memory
        LogicalIcon.ALARM -> ProgrammaticIcons.MaterialBell
        LogicalIcon.MUSIC -> ProgrammaticIcons.MaterialMusic
        else -> Icons.Filled.Info
    }
}
