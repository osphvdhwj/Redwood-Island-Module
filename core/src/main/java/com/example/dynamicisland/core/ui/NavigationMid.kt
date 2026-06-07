package com.example.dynamicisland.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.core.model.LocalIslandTheme
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.settings.*

@Composable
fun DynamicIslandView.NavigationMid(model: LiveActivityModel.Navigation) {
    val theme = LocalIslandTheme.current
    val accent = Color(0xFF4CAF50)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(theme.batIconSize + 4.dp).height(theme.batIconSize + 4.dp)
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

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Navigation",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_vector), // Generic direction icon
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = model.instruction,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${model.distance}m",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
