package com.example.dynamicisland.ui

import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.hook.ContinuityCameraScanner
import com.example.dynamicisland.settings.SettingsState
import com.example.dynamicisland.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun BarcodeMid(
    barcode: LiveActivityModel.Barcode,
    settings: SettingsState,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current

    val (iconRes, accentColor) = when {
        barcode.content.startsWith("http") -> AnyIcon(painter = painterResource(R.drawable.ic_sync_vector)) to Color(0xFF4FC3F7)
        barcode.content.contains("@")      -> AnyIcon(imageVector = Icons.Default.Email) to Color(0xFFFFB74D)
        barcode.content.matches(Regex("[\\d\\s\\+\\-\\(\\)]{7,}")) -> AnyIcon(painter = painterResource(R.drawable.ic_phone_vector)) to Color(0xFF81C784)
        barcode.content.startsWith("WIFI:")  -> AnyIcon(painter = painterResource(R.drawable.ic_wifi_vector)) to Color(0xFF4FC3F7)
        barcode.content.startsWith("geo:")   -> AnyIcon(painter = painterResource(R.drawable.ic_map_vector)) to Color(0xFFFF8A65)
        else                                 -> AnyIcon(painter = painterResource(R.drawable.ic_map_vector)) to Color.White
    }

    var actionPressed by remember { mutableStateOf(false) }
    val actionScale by animateFloatAsState(
        targetValue = if (actionPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "barcode_btn"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accentColor.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, accentColor.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                iconRes.imageVector != null -> Icon(imageVector = iconRes.imageVector, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                iconRes.painter != null -> Icon(painter = iconRes.painter, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = barcode.format.replaceFirstChar { it.uppercase() },
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = barcode.content,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .scale(actionScale)
                .background(accentColor.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    actionPressed = true
                    executeBarcodeAction(context, barcode)
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = barcode.label,
                color = accentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    LaunchedEffect(actionPressed) {
        if (actionPressed) {
            kotlinx.coroutines.delay(300)
            actionPressed = false
        }
    }
}

private data class AnyIcon(val imageVector: ImageVector? = null, val painter: Painter? = null)

private fun executeBarcodeAction(context: Context, barcode: LiveActivityModel.Barcode) {
    try {
        when {
            barcode.content.startsWith("http") ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(barcode.content)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

            barcode.content.contains("@") ->
                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${barcode.content}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

            barcode.content.matches(Regex("[\\d\\s\\+\\-\\(\\)]{7,}")) ->
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${barcode.content}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

            barcode.content.startsWith("geo:") ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${barcode.content}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

            else -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Scanned", barcode.content))
            }
        }
    } catch (_: Exception) {}
}
