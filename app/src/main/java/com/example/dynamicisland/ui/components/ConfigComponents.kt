package com.example.dynamicisland.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.ui.design.IslandColors
import com.example.dynamicisland.ui.design.glowBorder
import com.example.dynamicisland.ui.design.squishClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StaggeredItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 50L)
        visible = true
    }
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 60f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "translateY"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )
    Box(modifier = Modifier.graphicsLayer {
        this.translationY = translateY
        this.alpha = alpha
    }) {
        content()
    }
}

@Composable
fun PullToRefreshContainer(onRefresh: () -> Unit, content: @Composable () -> Unit) {
    var isRefreshing by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                // If dragging up and we have a positive offset, consume the scroll to hide indicator
                return if (available.y < 0 && dragOffset > 0) {
                    val consumed = if (dragOffset + available.y >= 0) available.y else -dragOffset
                    dragOffset += consumed
                    androidx.compose.ui.geometry.Offset(0f, consumed)
                } else {
                    androidx.compose.ui.geometry.Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                // If dragging down and there's available scroll (at the top), increase indicator offset
                return if (available.y > 0 && source == NestedScrollSource.UserInput) {
                    dragOffset += available.y * 0.5f
                    androidx.compose.ui.geometry.Offset(0f, available.y)
                } else {
                    androidx.compose.ui.geometry.Offset.Zero
                }
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (dragOffset > 150f && !isRefreshing) {
                    isRefreshing = true
                    onRefresh()
                    delay(1000)
                    isRefreshing = false
                }
                dragOffset = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    val animatedOffset by animateFloatAsState(
        targetValue = if (isRefreshing) 100f else dragOffset,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "ptr_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Simple PTR Indicator
        if (animatedOffset > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (animatedOffset - 24).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(IslandColors.accentCyan),
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
        }
        
        Box(modifier = Modifier.offset(y = if (animatedOffset > 0) (animatedOffset * 0.3f).dp else 0.dp)) {
            content()
        }
    }
}

@Composable
fun ThemeSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    formatValue: (Float) -> String = { "%.1f".format(it) }
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D0D))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = IslandColors.textPrimary, style = MaterialTheme.typography.titleSmall)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(IslandColors.accentCyan.copy(alpha = 0.2f))
                    .border(1.dp, IslandColors.accentCyan, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatValue(value),
                    color = IslandColors.accentCyan,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = IslandColors.accentCyan,
                activeTrackColor = IslandColors.accentCyan,
                inactiveTrackColor = IslandColors.surfaceVariant
            )
        )
    }
}

@Composable
fun PrecisionSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float = 1f,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D0D))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = IslandColors.textPrimary, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "%.2f".format(value),
                color = IslandColors.accentCyan,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { onValueChange((value - step).coerceAtLeast(valueRange.start)) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = IslandColors.textPrimary)
            }
            
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = IslandColors.accentCyan,
                    activeTrackColor = IslandColors.accentCyan,
                    inactiveTrackColor = IslandColors.surfaceVariant
                )
            )

            IconButton(
                onClick = { onValueChange((value + step).coerceAtMost(valueRange.endInclusive)) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = IslandColors.textPrimary)
            }
        }
    }
}

@Composable
fun FeatureSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D0D))
            .squishClickable { onCheckedChange(!checked) }
    ) {
        if (accentColor != null) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, color = IslandColors.textPrimary, style = MaterialTheme.typography.titleMedium)
                
                val thumbOffset by animateDpAsState(
                    targetValue = if (checked) 24.dp else 2.dp,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
                    label = "thumbOffset"
                )
                
                val trackBrush = if (checked) Brush.horizontalGradient(
                    colors = listOf(IslandColors.accentCyan, IslandColors.accentCyan.copy(alpha = 0.6f))
                ) else SolidColor(IslandColors.surfaceVariant)

                val glowModifier = if (checked) Modifier.glowBorder(IslandColors.accentCyan, 50.dp, 12.dp, 0.dp) else Modifier

                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(brush = trackBrush)
                        .then(glowModifier)
                        .padding(2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = thumbOffset)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
            
            AnimatedVisibility(
                visible = checked,
                enter = fadeIn(animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)) + expandVertically(animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)),
                exit = fadeOut(animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)) + shrinkVertically(animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f))
            ) {
                Text(
                    text = description,
                    color = IslandColors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "arrowRotation"
    )
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(IslandColors.surface)
                .clickable { expanded = true }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = label, color = IslandColors.textSecondary, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = selectedOption, color = IslandColors.textPrimary, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand",
                tint = IslandColors.textPrimary,
                modifier = Modifier.rotate(arrowRotation)
            )
        }
        
        if (expanded) {
            ModalBottomSheet(
                onDismissRequest = { expanded = false },
                containerColor = IslandColors.surface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = IslandColors.surfaceVariant) }
            ) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = IslandColors.textPrimary
                    )
                    options.forEach { option ->
                        val isSelected = option == selectedOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOptionSelected(option)
                                    expanded = false
                                }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) IslandColors.accentCyan else IslandColors.textPrimary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        if (isSelected) IslandColors.accentCyan else IslandColors.textSecondary,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(IslandColors.accentCyan)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    icon: ImageVector,
    summary: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var collapsed by remember { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (collapsed) 0f else 180f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "arrowRotation"
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF080808))
            .border(
                1.dp,
                Brush.linearGradient(listOf(IslandColors.border, Color.Transparent)),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { collapsed = !collapsed },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IslandColors.accentCyan,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = IslandColors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            
            AnimatedVisibility(
                visible = collapsed,
                enter = fadeIn(animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)),
                exit = fadeOut(animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f))
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(IslandColors.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = summary,
                        color = IslandColors.textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle",
                tint = IslandColors.textSecondary,
                modifier = Modifier.rotate(arrowRotation)
            )
        }
        
        AnimatedVisibility(
            visible = !collapsed,
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)) + expandVertically(animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)),
            exit = fadeOut(animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)) + shrinkVertically(animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}
