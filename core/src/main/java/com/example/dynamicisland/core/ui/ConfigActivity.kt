package com.example.dynamicisland.core.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.manager.IslandBackupManager
import com.example.dynamicisland.core.settings.SettingsManager
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.screens.*
import com.example.dynamicisland.core.ui.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ConfigActivity : ComponentActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var backupManager: IslandBackupManager
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsViewModel = SettingsViewModel(settingsManager)
        val prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        
        val composeView = ComposeView(this).apply {
            setContent {
                RedwoodTheme {
                    ConfigScreenNav(prefs, settingsViewModel, backupManager)
                }
            }
        }
        setContentView(composeView)
    }
}

data class NavItemData(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun ConfigScreenNav(
    prefs: android.content.SharedPreferences, 
    settingsViewModel: SettingsViewModel,
    backupManager: IslandBackupManager
) {
    var selectedNav by remember { mutableIntStateOf(0) }
    
    val navItems = listOf(
        NavItemData("Layout", Icons.Default.AspectRatio),
        NavItemData("Appearance", Icons.Default.Palette),
        NavItemData("Interactions", Icons.Default.TouchApp),
        NavItemData("Advanced", Icons.Default.Radar),
        NavItemData("Storage", Icons.Default.Storage),
        NavItemData("System", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedNav == index,
                        onClick = { selectedNav = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(
                targetState = selectedNav,
                label = "TabTransition"
            ) { navIndex ->
                when (navIndex) {
                    0 -> LayoutScreen(prefs)
                    1 -> AppearanceScreen(settingsViewModel)
                    2 -> InteractionsTab(prefs)
                    3 -> AdvancedTriggersScreen(prefs, settingsViewModel)
                    4 -> DataStorageScreen(prefs, backupManager)
                    5 -> SettingsScreen(settingsViewModel)
                }
            }
        }
    }
}

@Composable
fun InteractionsTab(prefs: android.content.SharedPreferences) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    Column {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            divider = {}
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("App Roles") })
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("Pins & Tiles") })
            Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, text = { Text("Gestures") })
            Tab(selected = selectedSubTab == 3, onClick = { selectedSubTab = 3 }, text = { Text("Focus") })
        }
        when (selectedSubTab) {
            0 -> AppRolesScreen(prefs)
            1 -> PinsTilesScreen(prefs)
            2 -> GesturesScreen(prefs)
            3 -> FocusModeScreen(prefs)
        }
    }
}
