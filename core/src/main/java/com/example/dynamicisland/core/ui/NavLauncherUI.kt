package com.example.dynamicisland.core.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.MD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.model.IslandTheme
import com.example.dynamicisland.shared.model.LocalIslandTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.settings.*

@Composable
fun DynamicIslandView.NavLauncherMini(model: LiveActivityModel.Dashboard) {
    val context = LocalContext.current
    val pm = context.packageManager
    val pinned = model.pinnedApps.take(5) // Limit to 5 for the mini pill

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (pinned.isEmpty()) {
            Text("No Pinned Apps", color = Color.White.copy(alpha=0.4f), fontSize = 12.sp)
        } else {
            pinned.forEach { pkg ->
                val icon = remember(pkg) {
                    try {
                        val drawable = pm.getApplicationIcon(pkg)
                        if (drawable is BitmapDrawable) drawable.bitmap.asImageBitmap() else null
                    } catch (e: Exception) { null }
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .pointerInput(pkg) {
                            detectTapGestures(
                                onTap = { controller?.actionManager?.launchAppIntent(pkg, false) { } },
                                onLongPress = { controller?.actionManager?.launchAppIntent(pkg, true) { } }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Image(bitmap = icon, contentDescription = null, modifier = Modifier.fillMaxSize(0.7f))
                    }
                }
            }
        }
    }
}
