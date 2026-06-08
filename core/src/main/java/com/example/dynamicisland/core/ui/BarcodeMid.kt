package com.example.dynamicisland.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
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
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.hook.ContinuityCameraScanner
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.settings.SettingsState

@Composable
fun BarcodeMid(
    barcode: LiveActivityModel.Barcode,
    settings: SettingsState,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current

    val (icon, accentColor) = when {
        barcode.content.startsWith("http") -> Icons.Default.Search to Color(0xFF007AFF)
        barcode.content.contains("@")      -> Icons.Default.Email to Color(0xFFFF9500)
        barcode.content.matches(Regex("[\\d\\s\\+\\-\\(\\)]{7,}")) -> Icons.Default.Call to Color(0xFF34C759)
        barcode.content.startsWith("WIFI:")  -> Icons.Default.Refresh to Color(0xFF5856D6)
        barcode.content.startsWith("geo:")   -> Icons.Default.Place to Color(0xFFFF2D55)
        else                                 -> Icons.Default.Info to Color.White
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- High-Fidelity Icon Backdrop ---
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(accentColor.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        // --- Contextual Text ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                barcode.format.uppercase(),
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
            Text(
                barcode.content,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // --- Action Badge ---
        Box(
            modifier = Modifier
                .squishClickable { executeBarcodeAction(context, barcode) }
                .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (barcode.label ?: "Copy").uppercase(),
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
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
