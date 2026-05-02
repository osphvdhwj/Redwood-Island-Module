package com.example.dynamicisland.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.hook.ContinuityCameraScanner

/**
 * BATCH 6: Barcode/QR result UI in the MID pill.
 * Shows the scan result with a context-specific action button.
 */
@Composable
fun DynamicIslandView.BarcodeMid(result: ContinuityCameraScanner.BarcodeResult) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current
    val theme   = LocalIslandTheme.current

    val (icon, accentColor) = when (result.type) {
        ContinuityCameraScanner.BarcodeType.URL          -> Icons.Default.Link           to Color(0xFF4FC3F7)
        ContinuityCameraScanner.BarcodeType.EMAIL        -> Icons.Default.Email          to Color(0xFFFFB74D)
        ContinuityCameraScanner.BarcodeType.PHONE        -> Icons.Default.Phone          to Color(0xFF81C784)
        ContinuityCameraScanner.BarcodeType.CONTACT      -> Icons.Default.Person         to Color(0xFFBA68C8)
        ContinuityCameraScanner.BarcodeType.WIFI         -> Icons.Default.Wifi           to Color(0xFF4FC3F7)
        ContinuityCameraScanner.BarcodeType.GEO          -> Icons.Default.LocationOn     to Color(0xFFFF8A65)
        ContinuityCameraScanner.BarcodeType.PRODUCT_CODE -> Icons.Default.QrCodeScanner  to Color.White
        ContinuityCameraScanner.BarcodeType.TEXT         -> Icons.Default.QrCode         to Color.White
    }

    // Action button bounce animation
    var actionPressed by remember { mutableStateOf(false) }
    val actionScale by animateFloatAsState(
        targetValue   = if (actionPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label         = "btn_scale"
    )

    Row(
        modifier          = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Icon ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(theme.batIconSize + 4.dp)
                .background(accentColor.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, accentColor.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        // ── Text ─────────────────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = result.type.name.replace("_", " ").lowercase()
                              .replaceFirstChar { it.uppercaseChar() },
                color      = Color.White.copy(alpha = 0.55f),
                fontSize   = theme.alertMsgSize,
                fontWeight = FontWeight.Medium
            )
            Text(
                text       = result.displayText,
                color      = Color.White,
                fontSize   = theme.alertTitleSize,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // ── Action button ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .scale(actionScale)
                .background(accentColor.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    actionPressed = true
                    executeBarcodeAction(context, result)
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = result.actionLabel,
                color      = accentColor,
                fontSize   = theme.alertMsgSize,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    LaunchedEffect(actionPressed) {
        if (actionPressed) { kotlinx.coroutines.delay(300); actionPressed = false }
    }
}

private fun executeBarcodeAction(context: Context, result: ContinuityCameraScanner.BarcodeResult) {
    try {
        when (result.type) {
            ContinuityCameraScanner.BarcodeType.URL ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.rawValue))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

            ContinuityCameraScanner.BarcodeType.EMAIL ->
                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${result.displayText}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

            ContinuityCameraScanner.BarcodeType.PHONE ->
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${result.displayText}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

            ContinuityCameraScanner.BarcodeType.GEO ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${result.rawValue}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

            ContinuityCameraScanner.BarcodeType.CONTACT ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.rawValue))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

            else -> {
                // Copy to clipboard as fallback
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("scan", result.rawValue))
            }
        }
    } catch (_: Exception) {}
}