package com.example.dynamicisland.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
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
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.settings.*

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
                .width(batIconSize + 4.dp).height(batIconSize + 4.dp)
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
                .width(batIconSize + 4.dp).height(batIconSize + 4.dp)
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
