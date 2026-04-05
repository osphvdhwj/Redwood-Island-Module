package com.example.dynamicisland.ui
import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*
import com.example.dynamicisland.manager.*

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
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

@Composable
fun MiniPillLayout(model: LiveActivityModel) {
    // Smoothly animate between different Mini views (e.g., Music to Download to Link)
    AnimatedContent(
        targetState = model,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) using
            SizeTransform { initialSize, targetSize -> tween(300) }
        },
        label = "MiniPillContent"
    ) { currentModel ->
        when (currentModel) {
            is LiveActivityModel.Music -> MiniMusicView(currentModel)
            is LiveActivityModel.OngoingTask -> MiniDownloadView(currentModel)
            is LiveActivityModel.LinkIntercept -> MiniLinkView(currentModel)
            else -> MiniDefaultView()
        }
    }
}

@Composable
private fun MiniMusicView(music: LiveActivityModel.Music) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Album Art
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.DarkGray)) {
            if (music.albumArt != null) {
                Image(
                    bitmap = music.albumArt.asImageBitmap(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Right: The Animated Waveform (We will build the actual liquid animation later, using a placeholder for now)
        Box(modifier = Modifier.width(30.dp).height(20.dp), contentAlignment = Alignment.Center) {
            val color = if (music.dominantColor != null) Color(music.dominantColor) else Color.White
            Text(if (music.isPlaying) "ılılı" else "—", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniDownloadView(task: LiveActivityModel.OngoingTask) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: App Icon + Download Label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.Gray)) {
                // Placeholder for App Icon
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Downloading", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        
        // Right: Live Network Speed String from TrafficStats!
        if (task.networkSpeed != null) {
            Text(text = task.networkSpeed, color = Color.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniLinkView(link: LiveActivityModel.LinkIntercept) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Target App Icon (e.g., YouTube Vanced)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (link.targetAppIcon != null) {
                Image(
                    bitmap = link.targetAppIcon.asImageBitmap(), contentDescription = null,
                    modifier = Modifier.size(20.dp).clip(CircleShape)
                )
            } else {
                Icon(Icons.Default.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = link.targetAppName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        
        // Right: The Domain (e.g., youtube.com)
        Text(
            text = link.urlHost, color = Color.Gray, fontSize = 12.sp, 
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 80.dp)
        )
    }
}

@Composable
private fun MiniDefaultView() {
    // Empty spacer for when the pill is transitioning or empty
    Spacer(modifier = Modifier.fillMaxSize())
}
