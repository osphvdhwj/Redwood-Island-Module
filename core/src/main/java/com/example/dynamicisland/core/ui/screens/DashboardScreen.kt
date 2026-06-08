package com.example.dynamicisland.core.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.dynamicisland.core.manager.IslandBackupManager
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.ui.components.ArchiveGridCard
import com.example.dynamicisland.core.ui.components.ArchiveHeader
import com.example.dynamicisland.shared.ipc.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCategoryClick: (String) -> Unit = {}
) {
    val filters = listOf("Layout", "Theme", "Gestures", "Advanced")
    var selectedFilter by remember { mutableStateOf(filters.first()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp, 
                end = 16.dp, 
                top = 24.dp, 
                bottom = 100.dp // Accommodates the FloatingNavBar at the bottom
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filters) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
            }

            item {
                Column {
                    ArchiveHeader(
                        title = "Categories",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // We use standard Rows to emulate a grid since LazyVerticalGrid inside LazyColumn requires fixed heights
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ArchiveGridCard(
                            title = "Layout & Size",
                            icon = Icons.Default.Build,
                            onClick = { onCategoryClick("Layout") },
                            modifier = Modifier.weight(1f)
                        )
                        ArchiveGridCard(
                            title = "Theme Colors",
                            icon = Icons.Default.Palette,
                            onClick = { onCategoryClick("Theme") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ArchiveGridCard(
                            title = "Gestures",
                            icon = Icons.Default.TouchApp,
                            onClick = { onCategoryClick("Gestures") },
                            modifier = Modifier.weight(1f)
                        )
                        ArchiveGridCard(
                            title = "Advanced Setup",
                            icon = Icons.Default.Settings,
                            onClick = { onCategoryClick("Advanced") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
