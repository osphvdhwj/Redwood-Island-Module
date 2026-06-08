package com.example.dynamicisland.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodDesignSystem
import com.example.dynamicisland.core.ui.design.glassmorphicCard
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.rememberHapticManager
import com.example.dynamicisland.shared.ipc.*

data class NavItemData(
    val title: String,
    val icon: ImageVector
)

@Composable
fun FloatingNavBar(
    items: List<NavItemData>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHapticManager()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glassmorphic container for nav items
        Row(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .glassmorphicCard(cornerRadius = 50.dp, glowColor = IslandColors.accentCyan.copy(alpha = 0.2f), glowRadius = 8.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                AnimatedNavItem(
                    selected = selectedIndex == index,
                    onClick = { 
                        haptics.light()
                        onItemSelected(index) 
                    },
                    icon = item.icon,
                    label = item.title
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Circular FAB with Neon Glow
        Box(
            modifier = Modifier
                .size(64.dp)
                .glassmorphicCard(cornerRadius = 50.dp, glowColor = IslandColors.accentCyan, glowRadius = 12.dp)
                .premiumClickable { 
                    haptics.medium()
                    onFabClick() 
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PowerSettingsNew, 
                contentDescription = "Toggle Island", 
                tint = IslandColors.accentCyan,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun AnimatedNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) IslandColors.accentCyan else IslandColors.textSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_content_color"
    )

    Row(
        modifier = Modifier
            .premiumClickable { onClick() }
            .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 12.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = contentColor,
                    style = RedwoodDesignSystem.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
