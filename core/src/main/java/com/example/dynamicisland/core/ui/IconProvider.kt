package com.example.dynamicisland.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.settings.IconPack

/**
 * ELITE ICON PROVIDER
 * Maps logical island events to high-fidelity icon packs.
 * Optimized for Custom ROM aesthetics.
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
            is IconPack.iOS            -> getiOSIcon(logicalIcon)
            is IconPack.OxygenOS       -> getOxygenIcon(logicalIcon)
            is IconPack.OneUI          -> getSamIcon(logicalIcon)
            is IconPack.Pixel          -> getPixelIcon(logicalIcon)
            is IconPack.Outline        -> getOutlineIcon(logicalIcon)
            is IconPack.Futuristic     -> getFuturisticIcon(logicalIcon)
            is IconPack.Minimal        -> getMinimalIcon(logicalIcon)
            is IconPack.AmoledCyberpunk -> getCyberpunkIcon(logicalIcon)
            else                       -> getMaterialYouIcon(logicalIcon)
        }
    }

    // 🍎 iOS Style: Rounded & Symmetrical
    private fun getiOSIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.WIFI -> Icons.Rounded.Wifi
        LogicalIcon.BLUETOOTH -> Icons.Rounded.Bluetooth
        LogicalIcon.HOTSPOT -> Icons.Rounded.SettingsInputAntenna
        LogicalIcon.MUSIC -> ProgrammaticIcons.CupertinoMusic
        LogicalIcon.BATTERY_CHARGING -> ProgrammaticIcons.CupertinoBattery
        LogicalIcon.ALARM -> ProgrammaticIcons.CupertinoBell
        else -> getMaterialYouIcon(icon)
    }

    // ⚡ OxygenOS Style: Light, Outlined, "Never Settle"
    private fun getOxygenIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.WIFI -> ProgrammaticIcons.OxygenWifi
        LogicalIcon.BLUETOOTH -> Icons.Outlined.Bluetooth
        LogicalIcon.BATTERY_FULL -> Icons.Outlined.BatteryFull
        LogicalIcon.SPEED -> Icons.Outlined.Speed
        LogicalIcon.DATA -> Icons.Outlined.SwapVert
        LogicalIcon.RAM -> Icons.Outlined.Memory
        LogicalIcon.CPU -> Icons.Outlined.Cpu
        else -> getMaterialYouIcon(icon)
    }

    // 📱 Samsung OneUI Style: Chunky, Squircle-friendly
    private fun getSamIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.WIFI -> Icons.Filled.Wifi
        LogicalIcon.BLUETOOTH -> Icons.Filled.Bluetooth
        LogicalIcon.PHONE -> Icons.Filled.Phone
        LogicalIcon.DATA -> Icons.Filled.SettingsInputAntenna
        LogicalIcon.RAM -> Icons.Filled.Memory
        LogicalIcon.CPU -> Icons.Filled.Cpu
        else -> getMaterialYouIcon(icon)
    }

    // 🍭 Pixel Style: Circular variants
    private fun getPixelIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.WIFI -> Icons.Rounded.Wifi
        LogicalIcon.SETTINGS -> Icons.Rounded.Settings
        LogicalIcon.RAM -> Icons.Rounded.Memory
        LogicalIcon.CPU -> Icons.Rounded.Cpu
        else -> getMaterialYouIcon(icon)
    }

    // 🪟 Outline Style: Minimalist Glass
    private fun getOutlineIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.WIFI -> Icons.Outlined.Wifi
        LogicalIcon.BLUETOOTH -> Icons.Outlined.Bluetooth
        LogicalIcon.BATTERY_FULL -> Icons.Outlined.BatteryFull
        LogicalIcon.MAIL -> Icons.Outlined.Mail
        LogicalIcon.RAM -> Icons.Outlined.Memory
        LogicalIcon.CPU -> Icons.Outlined.Cpu
        else -> getMaterialYouIcon(icon)
    }

    private fun getFuturisticIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.RAM -> ProgrammaticIcons.FuturisticRAM
        LogicalIcon.CPU -> ProgrammaticIcons.FuturisticCPU
        LogicalIcon.WIFI -> ProgrammaticIcons.CyberpunkWifi
        else -> getCyberpunkIcon(icon)
    }

    // 🌫️ Minimal Style: Ultra-thin lines
    private fun getMinimalIcon(icon: LogicalIcon): ImageVector = when (icon) {
        LogicalIcon.WIFI -> Icons.Outlined.Wifi
        LogicalIcon.CLOSE -> Icons.Outlined.Close
        LogicalIcon.RAM -> Icons.Outlined.Memory
        LogicalIcon.CPU -> Icons.Outlined.Cpu
        else -> getMaterialYouIcon(icon)
    }

    // 🌌 Cyberpunk Style: High-contrast, Fractured
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

    // 🎨 Material You (Default): Google M3
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
        LogicalIcon.CPU -> Icons.Filled.Cpu
        LogicalIcon.ALARM -> ProgrammaticIcons.MaterialBell
        LogicalIcon.MUSIC -> ProgrammaticIcons.MaterialMusic
        else -> Icons.Filled.Info
    }
}
