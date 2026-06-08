package com.example.dynamicisland.core.ui.states

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import com.example.dynamicisland.core.ui.components.PillSurface
import com.example.dynamicisland.shared.ipc.*
@Composable
fun MiniPill(
    modifier: Modifier = Modifier,
    leadingContent: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit,
    centerContent: @Composable () -> Unit
) {
    PillSurface(
        modifier = modifier
            .height(40.dp)
            .widthIn(min = 180.dp), // Dynamic width based on content
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(contentAlignment = Alignment.CenterStart) { leadingContent() }
            
            Box(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) { 
                centerContent() 
            }
            Box(contentAlignment = Alignment.CenterEnd) { trailingContent() }
        }
    }
}
