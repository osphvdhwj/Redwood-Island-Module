package com.example.dynamicisland.ui

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.R
import com.example.dynamicisland.model.LiveActivityModel

// ── Translation adapter ───────────────────────────────────────────────────

@Composable
fun DynamicIslandView.TranslationGeneralMid(model: LiveActivityModel.General) {
    val batIconSize = 36.dp
    val alertTitleSize = 14.sp
    val alertMsgSize = 12.sp
    val accent = Color(0xFF4FC3F7)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(batIconSize + 4.dp)
                .background(accent.copy(alpha = 0.12f), CircleShape)
                .border(1.dp, accent.copy(alpha = 0.30f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🌐", fontSize = 18.sp)
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.dataText,
                color = Color.White.copy(alpha = 0.50f),
                fontSize = alertMsgSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = model.title,
                color = accent,
                fontSize = alertTitleSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Barcode adapter ───────────────────────────────────────────────────────

@Composable
fun DynamicIslandView.BarcodeGeneralMid(model: LiveActivityModel.General) {
    val batIconSize = 36.dp
    val alertTitleSize = 14.sp
    val alertMsgSize = 12.sp
    val accent = Color(model.accentColor)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(batIconSize + 4.dp)
                .background(accent.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, accent.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_map_vector),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.dataText,
                color = Color.White,
                fontSize = alertTitleSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = model.title,
                color = accent.copy(alpha = 0.80f),
                fontSize = alertMsgSize,
                maxLines = 1
            )
        }
    }
}

// ── LinkIntercept mid card ────────────────────────────────────────────────

@Composable
fun DynamicIslandView.LinkInterceptMid(model: LiveActivityModel.LinkIntercept) {
    val haptic = LocalHapticFeedback.current
    val batIconSize = 36.dp
    val alertTitleSize = 14.sp
    val alertMsgSize = 12.sp

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(batIconSize + 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(0.10f)),
            contentAlignment = Alignment.Center
        ) {
            if (model.targetAppIcon != null) {
                Image(
                    bitmap = model.targetAppIcon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_sync_vector),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.targetAppName,
                color = Color.White,
                fontSize = alertTitleSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = model.urlHost,
                color = Color.White.copy(0.55f),
                fontSize = alertMsgSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

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
                text = "Open",
                color = Color.White,
                fontSize = alertMsgSize,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
