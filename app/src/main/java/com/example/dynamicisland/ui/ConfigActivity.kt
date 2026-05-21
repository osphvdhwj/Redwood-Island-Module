// File: app/src/main/java/com/example/dynamicisland/ui/ConfigActivity.kt
package com.example.dynamicisland.ui

import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.AndroidEntryPoint

private val AmoledCyanColorScheme = darkColorScheme(
    primary = Color(0xFF00FFFF), // Neon Cyan
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF008B8B),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF00FFFF),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004040),
    onSecondaryContainer = Color.White,
    background = Color.Black, // True AMOLED Black
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF121212), // Slightly elevated true black
    onSurfaceVariant = Color.White,
    outline = Color(0xFF00FFFF)
)

@AndroidEntryPoint
class ConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)

        // Use ComposeView directly – no need for the missing activity-compose dependency
        val composeView = ComposeView(this).apply {
            setContent {
                MaterialTheme(colorScheme = AmoledCyanColorScheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        ConfigScreenNav(prefs)
                    }
                }
            }
        }
        setContentView(composeView)
    }

    @Composable
    fun ConfigScreenNav(prefs: android.content.SharedPreferences) {
        var selectedNav by remember { mutableIntStateOf(0) }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color.Black,
                    contentColor = Color(0xFF00FFFF)
                ) {
                    NavigationBarItem(
                        selected = selectedNav == 0, onClick = { selectedNav = 0 }, icon = { Icon(Icons.Default.Build, null) }, label = { Text("Layout") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Black, selectedTextColor = Color(0xFF00FFFF), indicatorColor = Color(0xFF00FFFF), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )
                    NavigationBarItem(
                        selected = selectedNav == 1, onClick = { selectedNav = 1 }, icon = { Icon(Icons.Default.Create, null) }, label = { Text("Theme") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Black, selectedTextColor = Color(0xFF00FFFF), indicatorColor = Color(0xFF00FFFF), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )
                    NavigationBarItem(
                        selected = selectedNav == 2, onClick = { selectedNav = 2 }, icon = { Icon(Icons.Default.List, null) }, label = { Text("Dashboard") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Black, selectedTextColor = Color(0xFF00FFFF), indicatorColor = Color(0xFF00FFFF), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )
                    NavigationBarItem(
                        selected = selectedNav == 3, onClick = { selectedNav = 3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Features") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Black, selectedTextColor = Color(0xFF00FFFF), indicatorColor = Color(0xFF00FFFF), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )
                    NavigationBarItem(
                        selected = selectedNav == 4, onClick = { selectedNav = 4 }, icon = { Icon(Icons.Default.Notifications, null) }, label = { Text("Gestures") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Black, selectedTextColor = Color(0xFF00FFFF), indicatorColor = Color(0xFF00FFFF), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray)
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (selectedNav) {
                    0 -> LayoutScreen(prefs)
                    1 -> ThemeScreen(prefs)
                    2 -> DashboardScreen(prefs)
                    3 -> FeaturesScreen(prefs)
                    4 -> GesturesScreen(prefs)
                }
            }
        }
    }
}