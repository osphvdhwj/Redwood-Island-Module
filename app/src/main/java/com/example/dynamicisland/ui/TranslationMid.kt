package com.example.dynamicisland.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.settings.SettingsState

/**
 * TranslationMid – shows a quick translation result inside the island mid-state.
 */
@Composable
fun TranslationMid(
    translation: LiveActivityModel.Translation,
    settings: SettingsState,
    onDismiss: () -> Unit = {}
) {
    // Gentle pulsing accent to draw attention
    val infiniteTransition = rememberInfiniteTransition(label = "translate_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Translation icon
        Icon(
            Icons.Default.Translate,
            contentDescription = null,
            tint = Color(0xFF64B5F6).copy(alpha = alpha),
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Text columns
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = translation.original,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = translation.translated,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Copy / dismiss button (customizable)
        TextButton(
            onClick = { /* could copy translation or dismiss */ onDismiss() },
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text("Close", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
        }
    }
}