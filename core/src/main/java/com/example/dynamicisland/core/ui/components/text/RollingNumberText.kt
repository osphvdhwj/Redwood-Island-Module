package com.example.dynamicisland.core.ui.components.text

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*

/**
 * RollingNumberText
 * 
 * Animates number changes by rolling them vertically like a slot machine.
 * Inspired by HyperOS and premium countdown widgets.
 */
@Composable
fun RollingNumberText(
    value: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
) {
    Row(modifier = modifier) {
        value.forEach { char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically(animationSpec = tween(300)) { it } + fadeIn()) togetherWith
                        (slideOutVertically(animationSpec = tween(300)) { -it } + fadeOut())
                    } else {
                        (slideInVertically(animationSpec = tween(300)) { -it } + fadeIn()) togetherWith
                        (slideOutVertically(animationSpec = tween(300)) { it } + fadeOut())
                    }
                },
                label = "RollingChar"
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    style = style,
                    softWrap = false
                )
            }
        }
    }
}
