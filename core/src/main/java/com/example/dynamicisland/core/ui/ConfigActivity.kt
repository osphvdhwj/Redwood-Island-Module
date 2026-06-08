package com.example.dynamicisland.core.ui

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.manager.IslandBackupManager
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.settings.SettingsManager
import com.example.dynamicisland.shared.settings.SettingsViewModel
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
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("App Roles", style = MaterialTheme.typography.labelLarge) })
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("Pins & Tiles", style = MaterialTheme.typography.labelLarge) })
            Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, text = { Text("Gestures", style = MaterialTheme.typography.labelLarge) })
            Tab(selected = selectedSubTab == 3, onClick = { selectedSubTab = 3 }, text = { Text("Focus", style = MaterialTheme.typography.labelLarge) })
        }
        when (selectedSubTab) {
            0 -> AppRolesScreen(prefs)
            1 -> PinsTilesScreen(prefs)
            2 -> GesturesScreen(prefs)
            3 -> FocusModeScreen(prefs)
        }
    }
}
