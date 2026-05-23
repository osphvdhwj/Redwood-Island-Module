package com.example.dynamicisland.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.ui.components.FloatingNavBar
import com.example.dynamicisland.ui.components.NavItemData
import com.example.dynamicisland.ui.design.IslandColors
import com.example.dynamicisland.ui.design.RedwoodDesignSystem
import com.example.dynamicisland.ui.screens.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)

        val composeView = ComposeView(this).apply {
            setContent {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        background = IslandColors.background,
                        surface = IslandColors.surface,
                        primary = IslandColors.accentCyan
                    ),
                    typography = RedwoodDesignSystem.typography
                ) {
                    ConfigScreenNav(prefs)
                }
            }
        }
        setContentView(composeView)
    }
}

@Composable
fun ConfigScreenNav(prefs: android.content.SharedPreferences) {
    var selectedNav by remember { mutableIntStateOf(0) }
    
    val navItems = listOf(
        NavItemData("Layout", Icons.Default.Build),
        NavItemData("Appearance", Icons.Default.Palette),
        NavItemData("Smart", Icons.Default.Star),
        NavItemData("Shortcuts", Icons.Default.Apps),
        NavItemData("System", Icons.Default.Settings)
    )

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            FloatingNavBar(
                items = navItems,
                selectedIndex = selectedNav,
                onItemSelected = { selectedNav = it },
                onFabClick = { /* Can be used for a Quick Enable/Disable toggle */ }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HeroHeader()
                
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedNav) {
                        0 -> LayoutScreen(prefs)
                        1 -> AppearanceScreen(prefs)
                        2 -> IntelligenceTab(prefs)
                        3 -> InteractionsTab(prefs)
                        4 -> SystemScreen(prefs)
                    }
                }
            }
        }
    }
}

@Composable
fun IntelligenceTab(prefs: android.content.SharedPreferences) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    Column {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.Transparent,
            contentColor = IslandColors.accentCyan,
            divider = {}
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("AI & Detection") })
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("Continuity") })
        }
        when (selectedSubTab) {
            0 -> IntelligenceScreen(prefs)
            1 -> ContinuityScreen(prefs)
        }
    }
}

@Composable
fun InteractionsTab(prefs: android.content.SharedPreferences) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    Column {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.Transparent,
            contentColor = IslandColors.accentCyan,
            divider = {}
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("Pins & Tiles") })
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("Gestures") })
        }
        when (selectedSubTab) {
            0 -> PinsTilesScreen(prefs)
            1 -> GesturesScreen(prefs)
        }
    }
}

@Composable
fun HeroHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF001A33), Color.Black))),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Dynamic Island",
                    color = IslandColors.textPrimary,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = IslandColors.accentCyan,
                            blurRadius = 24f
                        )
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(IslandColors.accentCyan.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "v3.0",
                        color = IslandColors.accentCyan,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
