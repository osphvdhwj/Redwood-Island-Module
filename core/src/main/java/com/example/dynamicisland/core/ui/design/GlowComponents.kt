package com.example.dynamicisland.core.ui.design

import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

fun Modifier.glowBorder(
    color: Color,
    cornerRadius: Dp,
    glowRadius: Dp,
    borderWidth: Dp
): Modifier = this
    .drawBehind {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.TRANSPARENT
            setShadowLayer(glowRadius.toPx(), 0f, 0f, color.toArgb())
        }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                0f, 0f, size.width, size.height, cornerRadius.toPx(), cornerRadius.toPx(), paint
            )
        }
    }
    .border(borderWidth, color.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))

@Composable
fun GradientText(
    text: String,
    gradient: Brush,
    style: TextStyle,
    modifier: Modifier
) {
    Text(
        text = text,
        style = style.copy(brush = gradient),
        modifier = modifier
    )
}

@Composable
fun GlassCard(
    modifier: Modifier,
    glowColor: Color,
    cornerRadius: Dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glowBorder(glowColor, cornerRadius, glowRadius = 12.dp, borderWidth = 1.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(IslandColors.surface.copy(alpha = 0.7f))
            .border(1.dp, IslandColors.border, RoundedCornerShape(cornerRadius)),
        content = content
    )
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Applying the required spring animation for visual state transitions
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "NeonButtonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .glowBorder(IslandColors.accentCyan, cornerRadius = 12.dp, glowRadius = 12.dp, borderWidth = 1.5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IslandColors.surface)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        GradientText(
            text = text,
            gradient = Brush.linearGradient(listOf(IslandColors.accentCyan, IslandColors.accentPurple)),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .glowBorder(accentColor, cornerRadius = 12.dp, glowRadius = 8.dp, borderWidth = 1.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(IslandColors.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = IslandColors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.textSecondary
            )
        }
    }
}

@Composable
fun SkeletonLoader(modifier: Modifier = Modifier, cornerRadius: Dp = 16.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(IslandColors.surfaceVariant)
            .graphicsLayer { this.alpha = alpha }
    )
}

