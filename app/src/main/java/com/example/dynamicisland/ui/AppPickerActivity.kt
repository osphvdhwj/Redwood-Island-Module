package com.example.dynamicisland.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
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
import com.example.dynamicisland.ui.design.RedwoodDesignSystem
import com.example.dynamicisland.ui.design.RedwoodTheme
import com.google.accompanist.drawablepainter.rememberDrawablePainter

class AppPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            RedwoodTheme {
                AppPickerScreen { pkg ->
                    val result = Intent()
                    result.putExtra("package_name", pkg)
                    setResult(Activity.RESULT_OK, result)
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(onAppSelected: (String) -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf("name") } // name, date
    var isReverse by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("all") } // all, installed, system
    
    val allApps = remember {
        pm.getInstalledApplications(0)
    }

    val filteredApps = remember(searchQuery, sortOrder, isReverse, filterType) {
        var list = allApps.filter {
            val label = it.loadLabel(pm).toString()
            label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
        }

        list = when (filterType) {
            "installed" -> list.filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            "system" -> list.filter { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 }
            else -> list
        }

        list = when (sortOrder) {
            "name" -> list.sortedBy { it.loadLabel(pm).toString() }
            // Note: Date sorting would require PackageInfo, but we'll stick to name for now as a stable baseline
            else -> list.sortedBy { it.loadLabel(pm).toString() }
        }

        if (isReverse) list.reversed() else list
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text("Select Application") },
                    actions = {
                        IconButton(onClick = { isReverse = !isReverse }) {
                            Icon(Icons.Default.SwapVert, "Reverse")
                        }
                    }
                )
                
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(filteredApps) { app ->
                AppItem(app, onAppSelected)
            }
        }
    }
}

@Composable
fun AppItem(appInfo: ApplicationInfo, onClick: (String) -> Unit) {
    val pm = LocalContext.current.packageManager
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(appInfo.packageName) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = appInfo.loadIcon(pm)),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Column {
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
    }
}
