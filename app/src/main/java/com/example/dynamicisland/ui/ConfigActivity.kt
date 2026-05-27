package com.example.dynamicisland.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.example.dynamicisland.ui.settings.SettingsScreen
import com.example.dynamicisland.settings.SettingsViewModel
import com.example.dynamicisland.settings.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ConfigActivity : ComponentActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsViewModel = SettingsViewModel(settingsManager)
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
                    ConfigScreenNav(prefs, settingsViewModel)
                }
            }
        }
        setContentView(composeView)
    }
}

@Composable
fun ConfigScreenNav(prefs: android.content.SharedPreferences, settingsViewModel: SettingsViewModel) {
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
                if (selectedNav != 4) HeroHeader()
                
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent<Int>(
                        targetState = selectedNav,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn()) togetherWith
                                (slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }) + fadeOut())
                            } else {
                                (slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) + fadeIn()) togetherWith
                                (slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut())
                            }.using(SizeTransform(clip = false))
                        },
                        label = "TabTransition"
                    ) { navIndex ->
                        when (navIndex) {
                            0 -> LayoutScreen(prefs)
                            1 -> AppearanceScreen(prefs)
                            2 -> IntelligenceTab(prefs)
                            3 -> InteractionsTab(prefs)
                            4 -> SettingsScreen(settingsViewModel)
                        }
                    }
                }
            }
            
            // Floating Visual Aid overlay
            LiveVisualAid()
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
    val infiniteTransition = rememberInfiniteTransition(label = "header_anim")
    
    val orb1X by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1x"
    )
    
    val orb2X by infiniteTransition.animateFloat(
        initialValue = 300f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2x"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(Color(0xFF060606)),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = IslandColors.accentCyan.copy(alpha = 0.3f),
                radius = 120f,
                center = androidx.compose.ui.geometry.Offset(orb1X, size.height * 0.3f)
            )
            drawCircle(
                color = IslandColors.accentPurple.copy(alpha = 0.3f),
                radius = 150f,
                center = androidx.compose.ui.geometry.Offset(orb2X, size.height * 0.7f)
            )
        }

        // Frosted Glass overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(listOf(Color.White.copy(0.1f), Color.Transparent)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Redwood Engine",
                    color = IslandColors.textPrimary,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = IslandColors.accentCyan.copy(alpha = 0.5f),
                            blurRadius = 20f
                        )
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .border(1.dp, IslandColors.accentCyan.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape)
                        .background(IslandColors.accentCyan.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PRO",
                        color = IslandColors.accentCyan,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
