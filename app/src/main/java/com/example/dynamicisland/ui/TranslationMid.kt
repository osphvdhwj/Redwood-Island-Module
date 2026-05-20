package com.example.dynamicisland.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.model.LiveActivityModel

/**
 * Translation mid‑pill – Display original text dimmed and translated text highlighted.
 * Features a gentle pulsing accent around the translation icon.
 */
@Composable
fun TranslationMid(
    translation: LiveActivityModel.General, // Assuming General model is passed with translation data
    accentColor: Color = Color(0xFF4FC3F7)  // Light blue as default translation accent
) {
    // --- Gentle pulse on the translation icon ---
    val pulse = rememberInfiniteTransition(label = "translationPulse")
    val iconScale by pulse.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
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
        // --- Translation icon with pulsing halo ---
        Box(
            modifier = Modifier
                .size(42.dp)
                .scale(iconScale)
                .background(accentColor.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, accentColor.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = "Translation",
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // --- Text content ---
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Original text (dimmed)
            Text(
                text = translation.title, // E.g., "Bonjour"
                color = Color.White.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(Modifier.height(2.dp))
            
            // Translated text (highlighted)
            Text(
                text = translation.dataText, // E.g., "Hello"
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}