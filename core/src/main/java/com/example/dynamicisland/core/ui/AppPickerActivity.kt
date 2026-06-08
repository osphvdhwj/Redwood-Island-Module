package com.example.dynamicisland.core.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter

class AppPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val isMultiSelect = intent.getBooleanExtra("multi_select", false)
        val initialSelection = intent.getStringArrayExtra("initial_selection")?.toSet() ?: emptySet()
        val roleType = intent.getStringExtra("role_type") ?: "Apps"

        setContent {
            RedwoodTheme {
                AppPickerScreen(
                    title = "Select $roleType",
                    isMultiSelect = isMultiSelect,
                    initialSelection = initialSelection
                ) { selected ->
                    val result = Intent()
                    result.putExtra("package_names", selected.toTypedArray())
                    setResult(Activity.RESULT_OK, result)
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    title: String,
    isMultiSelect: Boolean,
    initialSelection: Set<String>,
    onDone: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("all") }
    val selectedApps = remember { mutableStateListOf<String>().apply { addAll(initialSelection) } }
    
    var allApps by remember { mutableStateOf<List<ApplicationInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            allApps = pm.getInstalledApplications(0)
            isLoading = false
        }
    }

    val filteredApps = remember(searchQuery, filterType, allApps) {
        if (isLoading) return@remember emptyList()
        var list = allApps.filter {
            val label = it.loadLabel(pm).toString()
            label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
        }

        list = when (filterType) {
            "installed" -> list.filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            "system" -> list.filter { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 }
            else -> list
        }

        list.sortedBy { it.loadLabel(pm).toString() }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text(title) },
                    actions = {
                        if (isMultiSelect) {
                            TextButton(onClick = { onDone(selectedApps.toSet()) }) {
                                Text("DONE (${selectedApps.size})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
                
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text("Search system & user apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterType == "all",
                        onClick = { filterType = "all" },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = filterType == "installed",
                        onClick = { filterType = "installed" },
                        label = { Text("User") }
                    )
                    FilterChip(
                        selected = filterType == "system",
                        onClick = { filterType = "system" },
                        label = { Text("System") }
                    )
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(filteredApps) { app ->
                    val isSelected = selectedApps.contains(app.packageName)
                    AppItem(
                        appInfo = app,
                        isSelected = isSelected,
                        showCheckbox = isMultiSelect
                    ) { pkg ->
                        if (isMultiSelect) {
                            if (isSelected) selectedApps.remove(pkg) else selectedApps.add(pkg)
                        } else {
                            onDone(setOf(pkg))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(
    appInfo: ApplicationInfo, 
    isSelected: Boolean,
    showCheckbox: Boolean,
    onClick: (String) -> Unit
) {
    val pm = LocalContext.current.packageManager
    Surface(
        onClick = { onClick(appInfo.packageName) },
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberDrawablePainter(drawable = appInfo.loadIcon(pm)),
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.loadLabel(pm).toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showCheckbox) {
                Checkbox(checked = isSelected, onCheckedChange = null)
            } else if (isSelected) {
                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
