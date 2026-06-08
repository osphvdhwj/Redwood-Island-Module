package com.example.dynamicisland.core.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.shared.model.*

@Composable
fun IslandMiniUI(model: LiveActivityModel) {
    // Smoothly animate between different Mini views
    AnimatedContent(
        targetState = model,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) using
            SizeTransform { _, _ -> tween(300) }
        },
        label = "MiniPillContent"
    ) { currentModel ->
        // Tiny coloured dot to indicate event type (adds premium feel)
        val dotColor = when (currentModel) {
            is LiveActivityModel.Music -> Color(0xFF1DB954)   // Spotify green
            is LiveActivityModel.Otp -> Color(0xFF00E676)     // green
            is LiveActivityModel.LinkIntercept -> Color(0xFFFF9800) // orange
            is LiveActivityModel.OngoingTask -> Color(0xFF2196F3) // blue
            else -> Color.White
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Status dot (top‑right corner)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            // Content
            when (currentModel) {
                is LiveActivityModel.Music -> MiniMusicView(currentModel)
                is LiveActivityModel.OngoingTask -> MiniDownloadView(currentModel)
                is LiveActivityModel.LinkIntercept -> MiniLinkView(currentModel)
                is LiveActivityModel.Otp -> MiniOtpView(currentModel)
                is LiveActivityModel.General -> MiniNotificationView(currentModel)
                else -> MiniDefaultView()
            }
        }
    }
}

// ---------- Music Mini Pill ----------
@Composable
private fun MiniMusicView(music: LiveActivityModel.Music) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Album Art
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.DarkGray)) {
            val art = music.albumArt
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))

        // Middle: Artist – Title (ellipsis if too long)
        Text(
            text = "${music.artist} – ${music.title}",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Right: Play/pause indicator (waveform placeholder)
        Box(modifier = Modifier.width(30.dp).height(20.dp), contentAlignment = Alignment.Center) {
            val dColor = music.dominantColor
            val color = if (dColor != null) Color(dColor) else Color.White
            Text(
                text = if (music.isPlaying) "ılılı" else "—",
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------- Download / Ongoing Task Mini Pill ----------
@Composable
private fun MiniDownloadView(task: LiveActivityModel.OngoingTask) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: App Icon + "Downloading" label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.Gray)) {
                // Placeholder for app icon – replace with actual icon if available
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Downloading",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Right: Live network speed
        val speed = task.networkSpeed
        if (speed != null) {
            Text(
                text = speed,
                color = Color.Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------- Link Intercept Mini Pill ----------
@Composable
private fun MiniLinkView(link: LiveActivityModel.LinkIntercept) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Target app icon and name
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = link.targetAppIcon
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = IconProvider.getIcon(IconProvider.LogicalIcon.LINK, LocalIconPack.current),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = link.targetAppName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Right: URL domain (short)
        Text(
            text = link.urlHost,
            color = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 80.dp)
        )
    }
}

// ---------- OTP Mini Pill (new) ----------
@Composable
private fun MiniOtpView(otp: LiveActivityModel.Otp) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = IconProvider.getIcon(IconProvider.LogicalIcon.LOCK, LocalIconPack.current),
            contentDescription = null,
            tint = Color(0xFF00E676),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = otp.code,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------- General Notification Mini Pill ----------
@Composable
private fun MiniNotificationView(general: LiveActivityModel.General) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = IconProvider.getIcon(IconProvider.LogicalIcon.ALARM, LocalIconPack.current),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = general.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------- Default spacer ----------
@Composable
private fun MiniDefaultView() {
    Spacer(modifier = Modifier.fillMaxSize())
}
