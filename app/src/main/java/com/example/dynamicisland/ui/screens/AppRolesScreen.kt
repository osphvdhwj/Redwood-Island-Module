package com.example.dynamicisland.ui.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.manager.NewConfigManager
import com.example.dynamicisland.ui.AppPickerActivity
import com.example.dynamicisland.ui.components.SettingsCategoryHeader
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun AppRolesScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsCategoryHeader("Core System Roles")
        
        MD3RoleSelector("Default Caller", "role_calling_app", prefs, context, scope)
        MD3RoleSelector("Primary Music Player", "role_music_app", prefs, context, scope)
        
        SettingsCategoryHeader("Productivity & Tools")
        MD3RoleSelector("Primary Video Player", "role_video_app", prefs, context, scope)
        MD3RoleSelector("Quick Notes App", "role_notes_app", prefs, context, scope)
        
        SettingsCategoryHeader("Gaming Mode")
        MD3RoleSelector("Main Game Launcher", "role_game_launcher", prefs, context, scope)

        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = {
                NewConfigManager.commitAndBroadcast(prefs, scope, context, {
                    prefs.all.keys.filter { it.startsWith("role_") }.forEach { remove(it) }
                }) {
                    NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()
        ) {
            Icon(Icons.Default.DeleteSweep, null)
            Spacer(Modifier.width(8.dp))
            Text("Reset All Roles", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun MD3RoleSelector(
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
        if (selectedPkg.isEmpty()) null
        else try { pm.getApplicationInfo(selectedPkg, 0) } catch (e: Exception) { null }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, AppPickerActivity::class.java)
                launcher.launch(intent)
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (appInfo != null) {
            Image(
                painter = rememberDrawablePainter(drawable = appInfo.loadIcon(pm)),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
        } else {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = roleName, 
                color = MaterialTheme.colorScheme.onSurface, 
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = appInfo?.loadLabel(pm)?.toString() ?: "No app assigned",
                color = if (appInfo == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (selectedPkg.isNotEmpty()) {
            IconButton(onClick = {
                selectedPkg = ""
                NewConfigManager.commitAndBroadcast(prefs, scope, context, {
                    putString(roleKey, "")
                }) {
                    NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                }
            }) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight, 
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}