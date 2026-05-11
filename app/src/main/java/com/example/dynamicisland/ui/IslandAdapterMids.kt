package com.example.dynamicisland.ui

/**
 * Adapter composables.
 *
 * Translation and Barcode results are broadcast as LiveActivityModel.General
 * (id = "sys_translation" / "sys_barcode") so the existing priority engine
 * can rank them. These thin adapters reconstruct the necessary data from the
 * title / dataText fields and delegate to the full composables.
 *
 * LinkInterceptMid shows a mid-state prompt when a URL has been intercepted.
 *
 * File: IslandAdapterMids.kt  (place in ui/ package)
 */

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.model.LiveActivityModel

// ── Translation adapter ───────────────────────────────────────────────────

/**
 * Renders a simplified translation card from a General model.
 * title   = translated text
 * dataText = original text
 */
@Composable
fun DynamicIslandView.TranslationGeneralMid(model: LiveActivityModel.General) {
    val theme  = LocalIslandTheme.current
    val accent = Color(0xFF4FC3F7)

    Row(
        modifier          = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(theme.batIconSize + 4.dp)
                .background(accent.copy(alpha = 0.12f), CircleShape)
                .border(1.dp, accent.copy(alpha = 0.30f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🌐", fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp))
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Original (dimmed)
            Text(
                text     = model.dataText,
                color    = Color.White.copy(alpha = 0.50f),
                fontSize = theme.alertMsgSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Translation (highlighted)
            Text(
                text       = model.title,
                color      = accent,
                fontSize   = theme.alertTitleSize,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Barcode adapter ───────────────────────────────────────────────────────

/**
 * Renders a simplified barcode card from a General model.
 * title    = actionLabel (e.g. "Open Link")
 * dataText = display text (URL, phone number, …)
 */
@Composable
fun DynamicIslandView.BarcodeGeneralMid(model: LiveActivityModel.General) {
    val theme  = LocalIslandTheme.current
    val accent = Color(model.accentColor)

    Row(
        modifier          = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(theme.batIconSize + 4.dp)
                .background(accent.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, accent.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.QrCode,
                contentDescription = null,
                tint               = accent,
                modifier           = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = model.dataText,
                color    = Color.White,
                fontSize = theme.alertTitleSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text     = model.title,
                color    = accent.copy(alpha = 0.80f),
                fontSize = theme.alertMsgSize,
                maxLines = 1
            )
        }
    }
}

// ── LinkIntercept mid card ────────────────────────────────────────────────

/**
 * Shown when a YouTube / Spotify URL has been intercepted.
 * Offers "Open in App" (proceeds with the original intent) and
 * "Dismiss" (reverts to previous island state).
 */
@Composable
fun DynamicIslandView.LinkInterceptMid(model: LiveActivityModel.LinkIntercept) {
    val haptic = LocalHapticFeedback.current
    val theme  = LocalIslandTheme.current

    Row(
        modifier          = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        Box(
            modifier = Modifier
                .size(theme.batIconSize + 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(0.10f)),
            contentAlignment = Alignment.Center
        ) {
            if (model.targetAppIcon != null) {
                Image(
                    bitmap             = model.targetAppIcon.asImageBitmap(),
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = model.targetAppName,
                color      = Color.White,
                fontSize   = theme.alertTitleSize,
                fontWeight = FontWeight.Bold,
                maxLines   = 1
            )
            Text(
                text     = model.urlHost,
                color    = Color.White.copy(0.55f),
                fontSize = theme.alertMsgSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // "Open" action
        Box(
            modifier = Modifier
                .background(Color.White.copy(0.15f), RoundedCornerShape(10.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    try {
                        model.rawIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(model.rawIntent)
                    } catch (_: Exception) {}
                }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "Open",
                color      = Color.White,
                fontSize   = theme.alertMsgSize,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}