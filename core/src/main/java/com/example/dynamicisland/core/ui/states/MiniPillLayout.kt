package com.example.dynamicisland.core.ui.states

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.core.ui.components.PillSurface
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

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
