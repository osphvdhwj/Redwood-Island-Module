package com.example.dynamicisland.ui.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.manager.NewConfigManager
import com.example.dynamicisland.ui.AppPickerActivity
import com.example.dynamicisland.ui.design.SectionHeader
import com.example.dynamicisland.ui.components.StaggeredItem
import com.example.dynamicisland.ui.design.IslandColors
import com.example.dynamicisland.ui.design.glassmorphicCard
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun AppRolesScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StaggeredItem(0) {
                SectionHeader(
                    title = "App Roles",
                    subtitle = "Define your default apps for Island integrations",
                    icon = Icons.Default.Apps,
                    accentColor = IslandColors.accentCyan
                )
            }
        }

        item {
            StaggeredItem(1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoleSelector("Calling App", "role_calling_app", prefs, context, scope)
                    RoleSelector("Music App", "role_music_app", prefs, context, scope)
                    RoleSelector("Video App", "role_video_app", prefs, context, scope)
                    RoleSelector("Notes App", "role_notes_app", prefs, context, scope)
                    RoleSelector("Game Launcher", "role_game_launcher", prefs, context, scope)
                }
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun RoleSelector(
    roleName: String,
    roleKey: String,
    prefs: SharedPreferences,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var selectedPkg by remember { mutableStateOf(prefs.getString(roleKey, "") ?: "") }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val pkg = result.data?.getStringExtra("package_name") ?: ""
            selectedPkg = pkg
            NewConfigManager.commitAndBroadcast(prefs, scope, context, {
                putString(roleKey, pkg)
            }) {
                NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
            }
        }
    }

    val pm = context.packageManager
    val appInfo = remember(selectedPkg) {
        try { pm.getApplicationInfo(selectedPkg, 0) } catch (e: Exception) { null }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphicCard(cornerRadius = 20.dp)
            .clickable {
                val intent = Intent(context, AppPickerActivity::class.java)
                launcher.launch(intent)
            }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (appInfo != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = appInfo.loadIcon(pm)),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                )
            } else {
                Box(modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.1f), CircleShape))
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(roleName, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = appInfo?.loadLabel(pm)?.toString() ?: "Tap to select",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
        }
    }
}

