package com.example.dynamicisland.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.ui.design.IslandColors
import com.example.dynamicisland.ui.design.premiumClickable

@Composable
fun GestureActionChips(
    selectedAction: String,
    inUseActions: List<String>,
    onSelect: (String) -> Unit
) {
    val allActions = listOf("none", "dismiss", "expand", "next_track", "previous_track", "toggle_play_pause")
    
    // Sort actions: selected first, then unused, then in-use (dimmed)
    val sortedActions = allActions.sortedWith(compareBy(
        { it != selectedAction },
        { it in inUseActions && it != selectedAction },
        { allActions.indexOf(it) }
    ))
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(sortedActions) { action ->
            val isSelected = selectedAction == action
            val isInUse = action in inUseActions && !isSelected
            val alphaMod = if (isInUse) 0.3f else 1f
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) IslandColors.accentCyan.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        1.dp, 
                        if (isSelected) IslandColors.accentCyan else IslandColors.textSecondary.copy(alpha = 0.3f * alphaMod),
                        RoundedCornerShape(12.dp)
                    )
                    .premiumClickable { onSelect(action) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = action.replace("_", " ").capitalize(),
                    color = if (isSelected) IslandColors.accentCyan else IslandColors.textSecondary.copy(alpha = alphaMod),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
