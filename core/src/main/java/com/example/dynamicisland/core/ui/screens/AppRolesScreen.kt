package com.example.dynamicisland.core.ui.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.example.dynamicisland.core.manager.IslandBackupManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.ui.AppPickerActivity
import com.example.dynamicisland.core.ui.components.SettingsCategoryHeader
import com.example.dynamicisland.shared.ipc.*
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
        SettingsCategoryHeader("Exclusive System Roles")
        MD3SingleRoleSelector("Primary Dialer", "role_calling_app", Icons.Default.Call, prefs, context, scope)
        MD3SingleRoleSelector("Game Hub", "role_game_launcher", Icons.Default.Gamepad, prefs, context, scope)
        
        SettingsCategoryHeader("Intelligent Multi-App Roles")
        MD3MultiRoleSelector(
            title = "Music Players", 
            roleKey = "allowed_music_apps", 
            icon = Icons.Default.MusicNote,
            suggestions = listOf("com.spotify.music", "com.google.android.apps.youtube.music", "com.apple.android.music", "org.videolan.vlc"),
            prefs = prefs, context = context, scope = scope
        )
        
        MD3MultiRoleSelector(
            title = "Media & Video", 
            roleKey = "allowed_media_apps", 
            icon = Icons.Default.Movie,
            suggestions = listOf("com.netflix.mediaclient", "com.mxtech.videoplayer.ad", "com.google.android.youtube", "org.videolan.vlc"),
            prefs = prefs, context = context, scope = scope
        )

        MD3MultiRoleSelector(
            title = "Notes & Productivity", 
            roleKey = "allowed_notes_apps", 
            icon = Icons.Default.EditNote,
            suggestions = listOf("com.google.android.keep", "com.microsoft.office.outlook", "com.notion.id", "com.evernote"),
            prefs = prefs, context = context, scope = scope
        )

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun MD3SingleRoleSelector(
    title: String,
    roleKey: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    prefs: SharedPreferences,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var selectedPkg by remember { mutableStateOf(prefs.getString(roleKey, "") ?: "") }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val pkg = result.data?.getStringArrayExtra("package_names")?.firstOrNull() ?: ""
            selectedPkg = pkg
            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString(roleKey, pkg) })
        }
    }

    val pm = context.packageManager
    val appInfo = remember(selectedPkg) {
        if (selectedPkg.isEmpty()) null
        else try { pm.getApplicationInfo(selectedPkg, 0) } catch (e: Exception) { null }
    }

    Surface(
        onClick = {
            val intent = Intent(context, AppPickerActivity::class.java).apply {
                putExtra("multi_select", false)
                putExtra("role_type", title)
            }
            launcher.launch(intent)
        },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appInfo != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = appInfo.loadIcon(pm)),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
            } else {
                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = appInfo?.loadLabel(pm)?.toString() ?: "Tap to assign",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (appInfo == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun MD3MultiRoleSelector(
    title: String,
    roleKey: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    suggestions: List<String>,
    prefs: SharedPreferences,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var selectedPkgs by remember { mutableStateOf(prefs.getStringSet(roleKey.uppercase(), emptySet()) ?: emptySet()) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val pkgs = result.data?.getStringArrayExtra("package_names")?.toSet() ?: emptySet()
            selectedPkgs = pkgs
            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putStringSet(roleKey.uppercase(), pkgs) })
        }
    }

    val pm = context.packageManager
    
    // Auto-detect installed suggestions
    val installedSuggestions = remember {
        suggestions.filter { pkg ->
            try { pm.getApplicationInfo(pkg, 0); true } catch (e: Exception) { false }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(context, AppPickerActivity::class.java).apply {
                        putExtra("multi_select", true)
                        putExtra("role_type", title)
                        putExtra("initial_selection", selectedPkgs.toTypedArray())
                    }
                    launcher.launch(intent)
                }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (selectedPkgs.isEmpty()) "Active-priority enabled for all apps" else "${selectedPkgs.size} apps prioritized",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(Icons.Default.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary)
        }

        if (installedSuggestions.isNotEmpty() && selectedPkgs.isEmpty()) {
            Text(
                "SUGGESTED FOR YOU", 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                installedSuggestions.take(3).forEach { pkg ->
                    val name = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } catch (e: Exception) { "App" }
                    InputChip(
                        selected = false,
                        onClick = {
                            val newSet = selectedPkgs + pkg
                            selectedPkgs = newSet
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putStringSet(roleKey.uppercase(), newSet) })
                        },
                        label = { Text(name) },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }

        // Show selected app icons
        if (selectedPkgs.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                selectedPkgs.take(6).forEach { pkg ->
                    val appIcon = try { pm.getApplicationIcon(pkg) } catch (e: Exception) { null }
                    if (appIcon != null) {
                        Image(
                            painter = rememberDrawablePainter(drawable = appIcon),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                        )
                    }
                }
                if (selectedPkgs.size > 6) {
                    Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                        Text("+${selectedPkgs.size - 6}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
