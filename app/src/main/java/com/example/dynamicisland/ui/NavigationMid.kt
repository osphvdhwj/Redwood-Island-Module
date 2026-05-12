package com.example.dynamicisland.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.model.LiveActivityModel

/**
 * Navigation mid‑pill – shows turn‑by‑turn directions.
 * Accent defaults to Maps green (#34A853) but can be overridden.
 */
@Composable
fun NavigationMid(
    navigation: LiveActivityModel.Navigation,
    accentColor: Color = Color(0xFF34A853)
) {
    // --- Resolve direction icon and label ---
    val combined = navigation.instruction.lowercase()
    val (dirIcon, dirLabel) = resolveDirection(combined)

    // --- Gentle pulse on the directional icon ---
    val pulse = rememberInfiniteTransition(label = "navPulse")
    val iconScale by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Directional icon ---
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(iconScale)
                .background(accentColor.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, accentColor.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = dirIcon,
                contentDescription = dirLabel,
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // --- Instruction text ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = navigation.instruction,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (navigation.distance.isNotBlank()) {
                Text(
                    text = navigation.distance,
                    color = accentColor.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- Maps quick‑launch pill (icon only, tappable if needed) ---
        Box(
            modifier = Modifier
                .background(accentColor.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Map,
                contentDescription = "Open Maps",
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// --- Direction resolver ---
private data class DirectionResult(val icon: ImageVector, val label: String)

private fun resolveDirection(text: String): DirectionResult = when {
    text.contains("u-turn") || text.contains("uturn")            ->
        DirectionResult(Icons.Default.UTurnLeft, "U-turn")
    text.contains("slight left") || text.contains("keep left")   ->
        DirectionResult(Icons.Default.TurnSlightLeft, "Slight left")
    text.contains("slight right") || text.contains("keep right") ->
        DirectionResult(Icons.Default.TurnSlightRight, "Slight right")
    text.contains("turn left") || text.contains(" left")         ->
        DirectionResult(Icons.Default.TurnLeft, "Turn left")
    text.contains("turn right") || text.contains(" right")       ->
        DirectionResult(Icons.Default.TurnRight, "Turn right")
    text.contains("roundabout") || text.contains("rotary")       ->
        DirectionResult(Icons.Default.RotateLeft, "Roundabout")
    text.contains("merge")                                        ->
        DirectionResult(Icons.Default.MergeType, "Merge")
    text.contains("exit")                                        ->
        DirectionResult(Icons.Default.ExitToApp, "Exit")            // fixed: Icons.Default.Ramp -> ExitToApp
    text.contains("arrive") || text.contains("destination")      ->
        DirectionResult(Icons.Default.LocationOn, "Arrive")
    else                                                          ->
        DirectionResult(Icons.Default.ArrowUpward, "Continue straight")
}