package com.example.dynamicisland.core.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.*
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.glassmorphicCard
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val tag: String,
    val message: String,
    val color: Color
)

@Composable
fun LiveVisualAid() {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<LogEntry>() }
    val listState = rememberLazyListState()

    // Listen for ecosystem events to show live output
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val action = intent.action ?: return
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                
                val (tag, color) = when {
                    action.contains("HARDWARE") -> "HARDWARE" to IslandColors.accentCyan
                    action.contains("NOTIFICATION") -> "NOTIF" to IslandColors.accentPurple
                    action.contains("MEDIA") -> "MEDIA" to Color.Green
                    action.contains("OTP") -> "SECURITY" to Color.Red
                    else -> "SYSTEM" to Color.Gray
                }

                val msg = when (action) {
                    "com.example.dynamicisland.NOTIFICATION_CAUGHT" -> "New Notification: ${intent.getStringExtra("title")}"
                    "com.example.dynamicisland.MEDIA_STATE_CHANGED" -> "Media Playback Updated"
                    "com.example.dynamicisland.OTP_CAUGHT" -> "OTP Code Intercepted"
                    "com.example.dynamicisland.HARDWARE_TOGGLE" -> "HW Toggle: ${intent.getStringExtra("type")}"
                    "com.example.dynamicisland.SHOW_VOLUME_MIXER" -> "Volume Mixer Triggered"
                    else -> action.substringAfterLast(".")
                }

                if (logs.size > 50) logs.removeAt(0)
                logs.add(LogEntry(timestamp = timestamp, tag = tag, message = msg, color = color))
            }
        }

        val filter = IntentFilter().apply {
            addAction("com.example.dynamicisland.NOTIFICATION_CAUGHT")
            addAction("com.example.dynamicisland.MEDIA_STATE_CHANGED")
            addAction("com.example.dynamicisland.OTP_CAUGHT")
            addAction("com.example.dynamicisland.HARDWARE_TOGGLE")
            addAction("com.example.dynamicisland.SHOW_VOLUME_MIXER")
            addAction("com.example.dynamicisland.PANEL_STATE_CHANGED")
            addAction("com.example.dynamicisland.SYNC_CONFIG")
        }

        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Auto-scroll to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp, end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
            },
            label = "visualAid"
        ) { expanded ->
            if (expanded) {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(400.dp)
                        .glassmorphicCard(cornerRadius = 24.dp)
                        .border(1.dp, IslandColors.accentCyan.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BugReport, null, tint = IslandColors.accentCyan, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Live Visual Aid", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            IconButton(onClick = { isExpanded = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.5f))
                            }
                        }
                        
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(logs, key = { it.id }) { log ->
                                LogItem(log)
                            }
                            if (logs.isEmpty()) {
                                item {
                                    Text(
                                        "No activity detected yet.\nTrigger a notification or change a setting to see live output.",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .glassmorphicCard(cornerRadius = 28.dp, glowColor = IslandColors.accentCyan.copy(alpha = 0.5f), glowRadius = 8.dp)
                        .premiumClickable { isExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BugReport, null, tint = IslandColors.accentCyan)
                    
                    // Live Pulse Dot
                    if (logs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(10.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color.Red)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItem(log: LogEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Circle, null, tint = log.color, modifier = Modifier.size(8.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "[${log.timestamp}] ${log.tag}",
                color = log.color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = log.message,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
        )
    }
}
