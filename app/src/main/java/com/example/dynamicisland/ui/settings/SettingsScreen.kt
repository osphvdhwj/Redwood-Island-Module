// File: app/src/main/java/com/example/dynamicisland/ui/settings/SettingsScreen.kt
package com.example.dynamicisland.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.settings.*
import com.example.dynamicisland.settings.SettingsManager.SettingKey
import kotlinx.coroutines.delay

@Composable
fun AppSelectorDialog(
    title: String,
    currentSelection: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text("App selection dialog") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GestureActionChips(selectedAction: String, onSelect: (String) -> Unit) {
    val actions = listOf("dismiss", "next_track", "previous_track", "toggle_play_pause", "none")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { action ->
            FilterChip(
                selected = selectedAction == action,
                onClick = { onSelect(action) },
                label = { Text(action.replace('_', ' ').replaceFirstChar(Char::uppercase)) }
            )
        }
    }
}

/**
 * Premium Dynamic Island Settings Screen
 * A complete from-scratch rewrite focusing on UX, animations, and AMOLED-first design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state = viewModel.state
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // ── UI THEME DEFINITION ──────────────────────────────────────────────────
    val premiumBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF001A33), Color.Black)
    )

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "PREMIUM CONFIG", 
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = Color.White.copy(alpha = 0.7f)
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Black.copy(alpha = 0.8f)
                ),
                actions = {
                    IconButton(onClick = { viewModel.resetAll(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                        Icon(Icons.Default.Refresh, "Reset", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(premiumBrush)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                // 🚀 HERO PREVIEW SECTION
                PremiumHeroHeader(state)

                // 🔍 FLOATING SEARCH
                PremiumSearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it }
                )

                Spacer(Modifier.height(16.dp))

                // 🏗️ SETTINGS CATEGORIES
                SettingsCategories(state, viewModel, searchQuery)

                Spacer(Modifier.height(120.dp)) // Extra space for FloatingNavBar padding
            }
        }
    }
}

@Composable
fun PremiumHeroHeader(state: SettingsState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated Glow Background
        val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.8f, targetValue = 1.2f,
            animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
            label = "glow_scale"
        )
        
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer { scaleX = glowScale; scaleY = glowScale }
                .blur(80.dp)
                .background(Color(0xFF00FFFF).copy(alpha = 0.15f), CircleShape)
        )

        // The Island Preview
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                    .shadow(20.dp, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(24.dp).background(Color(0xFF34A853), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text("System Active", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                text = "LEVEL 3 OVERLAY",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Cyan,
                letterSpacing = 2.sp
            )
            Text(
                text = if (state.islandEnabled) "ENGINE ONLINE" else "SYSTEM OFFLINE",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = if (state.islandEnabled) Color.White else Color.Gray
            )
        }
    }
}

@Composable
fun PremiumSearchBar(value: String, onValueChange: (String) -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .glassCard(cornerRadius = 16.dp),
            placeholder = { Text("Locate feature...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Cyan) },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Cyan.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                cursorColor = Color.Cyan
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingsCategories(state: SettingsState, viewModel: SettingsViewModel, query: String) {
    val categories = remember(state) {
        listOf(
            Category("ESSENTIALS", Icons.Default.FlashOn, listOf(
                SettingItem("Enable Dynamic Island", "Master engine switch", SettingKey.ISLAND_ENABLED, state.islandEnabled),
                SettingItem("Show on Lockscreen", "Always-on visibility", SettingKey.ISLAND_ON_LOCKSCREEN, state.islandOnLockscreen),
                SettingItem("Split Pill Mode", "Multitasking support", SettingKey.SPLIT_PILL_ENABLED, state.splitPillEnabled),
                SettingItem("Haptic Feedback", "Tactile response", SettingKey.HAPTIC_FEEDBACK, state.hapticFeedback)
            )),
            Category("VISUALS", Icons.Default.Palette, listOf(
                SettingItem("Design Language", "Modern vs Classic", SettingKey.USE_LIQUID_GLASS, state.designLanguage == DesignLanguage.APPLE_LIQUID_GLASS),
                SettingItem("Dynamic Colors", "Material You sync", SettingKey.DYNAMIC_COLORS, state.dynamicColors),
                SettingItem("Glow Effect", "Neon aura", SettingKey.GLOW_EFFECT, state.glowEffect),
                SettingItem("Dot Mode", "Minimalist idle", SettingKey.DOT_MODE, state.dotMode),
                SettingItem("Elastic Stretch", "Physics-based interaction", SettingKey.ELASTIC_STRETCH, state.elasticStretch)
            )),
            Category("INTELLIGENCE", Icons.Default.AutoAwesome, listOf(
                SettingItem("OTP Detection", "Smart code extraction", SettingKey.OTP_DETECTION, state.otpDetection),
                SettingItem("Link Intercept", "URL smart actions", SettingKey.LINK_INTERCEPT, state.linkIntercept),
                SettingItem("AI Prediction", "Context-aware UI", SettingKey.PREDICTIVE_ACTIONS, state.predictiveActions),
                SettingItem("Translation", "Live text conversion", SettingKey.TRANSLATION, state.translation)
            )),
            Category("MEDIA", Icons.Default.MusicNote, listOf(
                SettingItem("Waveform Seeker", "Visualized playback", SettingKey.WAVEFORM_ENABLED, state.waveformEnabled),
                SettingItem("Artwork Blur", "Immersive background", SettingKey.MEDIA_ARTWORK_BLUR, state.mediaArtworkBlur),
                SettingItem("Reactive Ring", "Audio-reactive border", SettingKey.AMBIENT_REACTIVE, state.ambientReactiveRing),
                SettingItem("BPM Pulse", "Sync with track beat", SettingKey.BPM_PULSE, state.bpmPulse)
            )),
            Category("ADVANCED LABS", Icons.Default.Science, listOf(
                SettingItem("Gaming HUD", "FPS & Thermal monitor", SettingKey.GAMING_HUD, state.gamingHud),
                SettingItem("iOS Padlock", "Face ID unlock style", SettingKey.FACE_ID_PADLOCK, state.faceIDPadlock),
                SettingItem("Continuity Camera", "Pro cam integration", SettingKey.CONTINUITY_CAMERA_ACTIONS, state.continuityCameraActions),
                SettingItem("MagSafe Anim", "Custom charging visuals", SettingKey.MAGSAFE_CHARGING_ANIMATION, state.magsafeChargingAnimation)
            ))
        )
    }

    categories.forEachIndexed { index, category ->
        val filteredItems = if (query.isEmpty()) category.items 
                           else category.items.filter { it.title.contains(query, ignoreCase = true) || it.desc.contains(query, ignoreCase = true) }
        
        if (filteredItems.isNotEmpty()) {
            PremiumCategorySection(category.title, category.icon, index) {
                filteredItems.forEach { item ->
                    PremiumSettingToggle(
                        title = item.title,
                        desc = item.desc,
                        checked = item.value as Boolean,
                        onCheckedChange = { viewModel.updateSetting(item.key, it) }
                    )
                }
                
                // Special Sliders for certain categories
                if (category.title == "VISUALS") {
                    PremiumSettingSlider("Blur Intensity", state.blurIntensity, 5f..30f) { viewModel.updateSetting(SettingKey.BLUR_INTENSITY, it) }
                    PremiumSettingSlider("Corner Radius", state.pillCornerRadius, 8f..200f) { viewModel.updateSetting(SettingKey.PILL_RADIUS, it) }
                }
                if (category.title == "MEDIA" && state.ambientReactiveRing) {
                    PremiumSettingSlider("Audio Sensitivity", state.audioSensitivity, 0.1f..1f) { viewModel.updateSetting(SettingKey.AUDIO_SENSITIVITY, it) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun PremiumCategorySection(
    title: String,
    icon: ImageVector,
    index: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 100L); visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 40 },
        exit = fadeOut()
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color.Cyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
                    color = Color.Cyan.copy(alpha = 0.8f)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24.dp)
                    .padding(4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun PremiumSettingToggle(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked) 
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        
        // Custom Animated Switch
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(26.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Cyan else Color.White.copy(alpha = 0.1f))
                .padding(2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            val thumbOffset by animateDpAsState(
                targetValue = if (checked) 22.dp else 0.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "switch_thumb"
            )
            Box(
                modifier = Modifier
                    .offset(x = if (checked) 0.dp else thumbOffset) // Simplified logic for demo
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (checked) Color.Black else Color.Gray)
            )
        }
    }
}

@Composable
fun PremiumSettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            Text("%.1f".format(value), color = Color.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Cyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

// ── UTILITY MODIFIERS ────────────────────────────────────────────────────────

fun Modifier.glassCard(cornerRadius: Float): Modifier = this.glassCard(cornerRadius.dp)
fun Modifier.glassCard(cornerRadius: androidx.compose.ui.unit.Dp): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(Color.White.copy(alpha = 0.05f))
    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(cornerRadius))

// ── DATA MODELS ──────────────────────────────────────────────────────────────

private data class Category(
    val title: String,
    val icon: ImageVector,
    val items: List<SettingItem>
)

private data class SettingItem(
    val title: String,
    val desc: String,
    val key: SettingKey,
    val value: Any
)
