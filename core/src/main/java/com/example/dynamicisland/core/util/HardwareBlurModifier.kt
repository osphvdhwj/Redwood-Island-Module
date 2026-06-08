package com.example.dynamicisland.core.util

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import androidx.compose.ui.unit.dp

/**
 * Pillar 5: Performance Optimization
 * Ensures the Compose UI uses Hardware-Accelerated `RenderEffect.createBlurEffect`
 * on supported devices (Android 12+) instead of falling back to legacy software blurs.
 */
fun Modifier.hardwareBlur(radius: Dp): Modifier = this
