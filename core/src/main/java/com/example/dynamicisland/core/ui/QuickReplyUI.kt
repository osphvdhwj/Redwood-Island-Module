package com.example.dynamicisland.core.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.model.SimpleNotification
import com.example.dynamicisland.shared.settings.*

@Composable
fun DynamicIslandView.NotificationStackMax(model: LiveActivityModel.NotificationStack) {
    val view = this
    var replyText by remember { mutableStateOf("") }
    val latestNotif = model.notifications.firstOrNull() ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Header (App Info) ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(model.accentColor).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                // App Icon Placeholder (In real use, load via PackageManger)
                Box(Modifier.size(16.dp).background(Color(model.accentColor), CircleShape))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = model.pkgName.substringAfterLast(".").capitalize(),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (model.totalCount > 1) {
                Spacer(Modifier.width(8.dp))
                Badge(containerColor = IslandColors.accentCyan) { 
                    Text("${model.totalCount} New", color = Color.Black, fontSize = 10.sp) 
                }
            }
        }

        // --- Active Message ---
        Row(verticalAlignment = Alignment.Top) {
            val avatar = latestNotif.avatar
            if (avatar != null) {
                Image(
                    bitmap = avatar.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column {
                Text(latestNotif.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(latestNotif.text, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, maxLines = 2)
            }
        }

        // --- Quick Reply Input ---
        val replyAction = latestNotif.remoteActions.find { it.isReply }
        if (replyAction != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(IslandColors.accentCyan),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    decorationBox = { innerTextField ->
                        if (replyText.isEmpty()) {
                            Text("Reply to ${latestNotif.title}...", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
                
                IconButton(
                    onClick = {
                        if (replyText.isNotEmpty()) {
                            view.onReplySend?.invoke(replyText)
                            replyText = ""
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Send, null, tint = IslandColors.accentCyan)
                }
            }
        } else {
            // Standard Action Buttons (Mark as read, etc)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                latestNotif.remoteActions.take(2).forEach { action ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .premiumClickable { /* trigger action.actionIntent */ }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(action.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
