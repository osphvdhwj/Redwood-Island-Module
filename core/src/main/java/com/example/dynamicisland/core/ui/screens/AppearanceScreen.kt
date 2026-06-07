package com.example.dynamicisland.core.ui.screens

import android.content.SharedPreferences
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.settings.SettingsManager.SettingKey
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.ui.components.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

@Composable
fun AppearanceScreen(viewModel: SettingsViewModel) {
    val state = viewModel.state
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        
        // --- LIVE SANDBOX PREVIEW ---
        SettingsCategoryHeader("Live Sandbox Preview")
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(width = 180.dp, height = 36.dp)
                        .clip(CircleShape)
                        .background(if (state.aestheticStyle == AestheticStyle.VOID_BLACK) Color.Black else Color.White.copy(0.1f))
                        .border(0.5.dp, Color.White.copy(0.2f), CircleShape)
                ) {
                    Text("Live Preview", color = Color.White, modifier = Modifier.align(Alignment.Center), fontSize = 12.sp)
                }
            }
        }

        SettingsCategoryHeader("Visual Surfaces")
        SettingsSwitch(
            title = "Nav Island Mode",
            description = "Anchor the interface to the bottom of the screen.",
            icon = Icons.Default.VerticalAlignBottom,
            checked = state.navIslandMode,
            onCheckedChange = { viewModel.updateSetting(SettingKey.NAV_ISLAND_MODE, it) }
        )
        if (state.navIslandMode) {
            SettingsSwitch(
                title = "One-Hand Mode Integration",
                description = "Swipe down on the bottom pill to trigger system one-hand mode.",
                icon = Icons.Default.BackHand,
                checked = state.oneHandModeEnabled,
                onCheckedChange = { viewModel.updateSetting(SettingKey.ONE_HAND_MODE_ENABLED, it) }
            )
            SettingsSwitch(
                title = "Dull Background",
                description = "Keep a subtle background behind the pill for better visibility.",
                icon = Icons.Default.Visibility,
                checked = state.navIslandDullBackground,
                onCheckedChange = { viewModel.updateSetting(SettingKey.NAV_ISLAND_DULL_BACKGROUND, it) }
            )
            SettingsSwitch(
                title = "Pipe Indicator",
                description = "Show a '|' vertical bar at the current battery level.",
                icon = Icons.Default.FormatQuote,
                checked = state.navIslandShowPipeIndicator,
                onCheckedChange = { viewModel.updateSetting(SettingKey.NAV_ISLAND_SHOW_PIPE_INDICATOR, it) }
            )
            SettingsSwitch(
                title = "Music Bar Morph",
                description = "The Pill stretches to become the background for music controls.",
                icon = Icons.Default.MusicNote,
                checked = state.navIslandMusicBarMorph,
                onCheckedChange = { viewModel.updateSetting(SettingKey.NAV_ISLAND_MUSIC_BAR_MORPH, it) }
            )
        }

        SettingsCategoryHeader("Haptics")
        SettingsSwitch(
            title = "Haptic Feedback",
            description = "Vibrate on island transitions and gestures.",
            icon = Icons.Default.Vibration,
            checked = state.hapticFeedback,
            onCheckedChange = { viewModel.updateSetting(SettingKey.HAPTIC_FEEDBACK, it) }
        )
        SettingsSwitch(
            title = "Sync Pulse Vibration",
            description = "Feel a heartbeat when both islands pulse together on battery change.",
            icon = Icons.Default.MonitorHeart,
            checked = state.syncPulseVibration,
            onCheckedChange = { viewModel.updateSetting(SettingKey.SYNC_PULSE_VIBRATION, it) }
        )

        SettingsCategoryHeader("Elite Aesthetics")
        SettingsChoiceChip("Design Language", state.designLanguage.name, listOf("MATERIAL_YOU", "APPLE_LIQUID_GLASS", "VYXEL_EXPRESSIVE")) {
            viewModel.updateSetting(SettingKey.DESIGN_LANGUAGE, it)
        }

        SettingsChoiceChip("Font Aesthetic", state.fontAesthetic.name, listOf("DEFAULT", "MONOSPACE", "KILO", "CHOCOCOOKY")) {
            viewModel.updateSetting(SettingKey.FONT_AESTHETIC, it)
        }

        SettingsChoiceChip("Module Language", state.appLanguage, listOf("System", "English", "Hindi", "Spanish")) {
            viewModel.updateSetting(SettingKey.APP_LANGUAGE, it)
        }

        SettingsChoiceChip("Aesthetic Mode", state.aestheticStyle.name, listOf("GLASS", "VOID_BLACK", "LIQUID_GLASS")) {
            viewModel.updateSetting(SettingKey.AESTHETIC_STYLE, it)
        }

        if (state.aestheticStyle == AestheticStyle.GLASS) {
            SettingsSlider(
                title = "Frosted Intensity", 
                value = state.blurIntensity, 
                defaultValue = 15f,
                valueRange = 5f..40f,
                onValueChange = { viewModel.updateSetting(SettingKey.BLUR_INTENSITY, it) }
            )
        }

        SettingsSwitch(
            title = "Monochrome Icons", 
            description = "Force app icons to adopt a consistent chalk style.", 
            checked = state.monochromeIcons,
            icon = Icons.Default.InvertColors,
            onCheckedChange = { viewModel.updateSetting(SettingKey.MONOCHROME_ICONS, it) }
        )

        SettingsCategoryHeader("Icon Engine (Pillar 5)")
        // Correcting access to IconPack enum strings
        SettingsChoiceChip("Icon Set", state.iconPack.name, listOf("MaterialYou", "iOS", "OxygenOS", "Samsung", "Pixel", "Futuristic", "Minimal", "Bold", "Outline")) {
            viewModel.updateSetting(SettingKey.ICON_PACK, it)
        }

        SettingsCategoryHeader("Motion & Physics (Pillar 4)")
        SettingsChoiceChip("Physics Profile", state.physicsStyle.name, listOf("APPLE", "OXYGEN_OS")) {
            viewModel.updateSetting(SettingKey.PHYSICS_STYLE, it)
        }
        
        SettingsChoiceChip("Content Transition", state.contentTransitionStyle.name, listOf("SLIDE", "FADE_SCALE", "FLIP")) {
            viewModel.updateSetting(SettingKey.CONTENT_TRANSITION_STYLE, it)
        }

        SettingsSlider(
            title = "Tactile Squish", 
            value = state.squishIntensity, 
            defaultValue = 1.0f,
            valueRange = 0.0f..2.0f,
            onValueChange = { viewModel.updateSetting(SettingKey.SQUISH_INTENSITY, it) }
        )

        SettingsSwitch(
            title = "Metaball Tear", 
            description = "Liquid drop effect when the Island splits.", 
            checked = state.enableMetaballTear,
            icon = Icons.Default.Waves,
            onCheckedChange = { viewModel.updateSetting(SettingKey.ENABLE_METABALL_TEAR, it) }
        )

        SettingsCategoryHeader("Component Studio")
        SettingsChoiceChip("Call UI", state.callStyle.name, listOf("IOS", "MINIMAL", "MODERN")) {
            viewModel.updateSetting(SettingKey.CALL_STYLE, it)
        }
        SettingsChoiceChip("Charging UI", state.chargingStyle.name, listOf("RING", "WAVE", "CUBE")) {
            viewModel.updateSetting(SettingKey.CHARGING_STYLE, it)
        }
        SettingsChoiceChip("Battery Style", state.batteryStyle.name, listOf("PILL", "GAUGE", "DIGITAL")) {
            viewModel.updateSetting(SettingKey.BATTERY_STYLE, it)
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}
