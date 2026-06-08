package com.example.dynamicisland.core.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.example.dynamicisland.shared.settings.AestheticStyle
import com.example.dynamicisland.shared.settings.IconPack
import com.example.dynamicisland.shared.settings.DesignLanguage
import com.example.dynamicisland.shared.settings.PhysicsStyle
import com.example.dynamicisland.shared.settings.ContentTransitionStyle
import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.shared.model.LocalIslandTheme
import com.example.dynamicisland.shared.model.IslandTheme
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.manager.IslandBackupManager
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.components.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.settings.*
@Composable
fun DataStorageScreen(prefs: SharedPreferences, backupManager: IslandBackupManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreContent by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsCategoryHeader("Backup & Restore")
        
        SettingsMenuLink(
            title = "Create Manual Backup",
            description = "Export all settings and AI weights to storage.",
            icon = Icons.Default.Backup,
            onClick = {
                val data = backupManager.createBackup()
                if (data != null) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Island Backup", data))
                    Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                }
            }
        )
            title = "Restore from JSON",
            description = "Import settings from a previously saved JSON string.",
            icon = Icons.Default.Restore,
            onClick = { showRestoreDialog = true }
        SettingsCategoryHeader("Automation")
        SettingsSwitch(
            title = "Automatic Backup",
            description = "Periodically save settings to local storage.",
            checked = prefs.getBoolean("AUTO_BACKUP_ENABLED", false),
            onCheckedChange = { value -> 
                NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("AUTO_BACKUP_ENABLED", value) }) 
        if (prefs.getBoolean("AUTO_BACKUP_ENABLED", false)) {
            SettingsSlider(
                title = "Backup Frequency",
                description = "Days between automatic backups.",
                value = prefs.getInt("AUTO_BACKUP_FREQ_DAYS", 7).toFloat(),
                defaultValue = 7f,
                valueRange = 1f..30f,
                onValueChange = { value -> 
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putInt("AUTO_BACKUP_FREQ_DAYS", value.toInt()) }) 
            )
        }
        SettingsCategoryHeader("Storage Configuration")
            title = "Archive Path",
            description = prefs.getString("STASH_STORAGE_PATH", "/sdcard/DynamicIsland/Archive") ?: "/sdcard/DynamicIsland/Archive",
            icon = Icons.Default.Folder,
                Toast.makeText(context, "Folder picker not implemented, using default path.", Toast.LENGTH_SHORT).show()
        SettingsCategoryHeader("Clipboard Stash")
            title = "Enable Paperclip",
            description = "Show a tiny clip icon on the Island when items are stashed.",
            checked = prefs.getBoolean("ENABLE_CLIPBOARD_PAPERCLIP", true),
                NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("ENABLE_CLIPBOARD_PAPERCLIP", value) }) 
        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("Restore Settings") },
                text = {
                    OutlinedTextField(
                        value = restoreContent,
                        onValueChange = { restoreContent = it },
                        label = { Text("Paste Backup JSON here") },
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val ok = backupManager.restoreBackup(restoreContent)
                        if (ok) {
                            Toast.makeText(context, "Restore successful! Restarting UI...", Toast.LENGTH_SHORT).show()
                            showRestoreDialog = false
                        } else {
                            Toast.makeText(context, "Invalid Backup JSON", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Restore") }
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
        Spacer(Modifier.height(120.dp))
    }
}
