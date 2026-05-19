// File: app/src/main/java/com/example/dynamicisland/ui/settings/SettingsScreen.kt
// Full settings UI with all sections implemented.
// Requires:
//   - com.dynamicisland.util.getPillShape
//   - com.dynamicisland.util.glassBackground
//   - com.dynamicisland.ui.settings.AppSelectorDialog (already defined earlier)

package com.example.dynamicisland.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow      // ← added
import androidx.compose.ui.draw.blur        // ← added
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.settings.*
import com.example.dynamicisland.settings.SettingsManager.SettingKey

// ---------- Main entry point ----------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state = viewModel.state
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Essentials", "Advanced & Labs")
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dynamic Island Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search settings...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Content per tab
            when (selectedTab) {
                0 -> EssentialsTab(state, viewModel, searchQuery)
                1 -> AdvancedTab(state, viewModel, searchQuery)
            }
        }
    }
}

// ---------- Essentials Tab ----------
@OptIn(ExperimentalLayoutApi::class)   // ← added for FlowRow
@Composable
fun EssentialsTab(state: SettingsState, viewModel: SettingsViewModel, searchQuery: String) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        // Live preview (Appearance only)
        if (searchQuery.isEmpty() || searchQuery.contains("appearance", ignoreCase = true)) {
            IslandPreview(state)
        }

        // --- Global Controls ---
        CollapsibleSection(
            title = "Global Controls",
            icon = Icons.Default.ToggleOff,
            expanded = true,
            searchQuery = searchQuery,
            toggleTitles = listOf("Enable Dynamic Island", "Show on Lockscreen", "Lockscreen Features", "One-Hand Mode Placement")
        ) {
            SettingSwitch("Enable Dynamic Island", "Master on/off switch", state.islandEnabled) {
                viewModel.updateSetting(SettingKey.ISLAND_ENABLED, it)
            }
            SettingSwitch("Show on Lockscreen", null, state.islandOnLockscreen) {
                viewModel.updateSetting(SettingKey.ISLAND_ON_LOCKSCREEN, it)
            }
            if (state.islandOnLockscreen) {
                Text("Lockscreen Features", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("music", "notifications", "timer", "weather", "focus").forEach { feature ->
                        FilterChip(
                            selected = state.lockscreenFeatures.contains(feature),
                            onClick = {
                                val newSet = state.lockscreenFeatures.toMutableSet()
                                if (newSet.contains(feature)) newSet.remove(feature) else newSet.add(feature)
                                viewModel.updateSetting(SettingKey.FEATURES_ON_LOCKSCREEN, newSet)
                            },
                            label = { Text(feature.replaceFirstChar(Char::uppercase)) }
                        )
                    }
                }
            }
            SettingSwitch("One-Hand Mode Placement", "Shift island to bottom", state.dedicatedOneHandPlacement) {
                viewModel.updateSetting(SettingKey.DEDICATED_ONE_HAND_PLACEMENT, it)
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- Appearance ---
        CollapsibleSection(
            title = "Appearance",
            icon = Icons.Default.Palette,
            expanded = true,
            searchQuery = searchQuery,
            toggleTitles = listOf("Design Language", "Dynamic Colors", "Blur Intensity", "Dynamic Gradient",
                "Glow Effect", "Shadow Casting", "Content-Aware Blur", "Time-Based Themes", "High Contrast Mode",
                "Elastic Stretch", "Dot Mode", "Icon Pack Integration", "Material You Dynamic Contrast",
                "Pill Shape", "Corner Radius", "Animation Speed")
        ) {
            Text("Design Language", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.designLanguage == DesignLanguage.MATERIAL_YOU,
                    onClick = { viewModel.updateSetting(SettingKey.USE_LIQUID_GLASS, false) },
                    label = { Text("Material You") })
                FilterChip(selected = state.designLanguage == DesignLanguage.APPLE_LIQUID_GLASS,
                    onClick = { viewModel.updateSetting(SettingKey.USE_LIQUID_GLASS, true) },
                    label = { Text("Liquid Glass") })
            }
            Spacer(Modifier.height(8.dp))
            if (state.designLanguage == DesignLanguage.MATERIAL_YOU) {
                SettingSwitch("Dynamic Colors", null, state.dynamicColors) {
                    viewModel.updateSetting(SettingKey.DYNAMIC_COLORS, it)
                }
            }
            SettingSlider("Blur Intensity", state.blurIntensity, 5f..30f) {
                viewModel.updateSetting(SettingKey.BLUR_INTENSITY, it)
            }
            SettingSwitch("Dynamic Gradient from Artwork", null, state.dynamicGradient) {
                viewModel.updateSetting(SettingKey.DYNAMIC_GRADIENT, it)
            }
            SettingSwitch("Glow Effect", null, state.glowEffect) {
                viewModel.updateSetting(SettingKey.GLOW_EFFECT, it)
            }
            SettingSwitch("Shadow Casting", "Colored shadow", state.shadowCasting) {
                viewModel.updateSetting(SettingKey.SHADOW_CASTING, it)
            }
            SettingSwitch("Content-Aware Blur", "Blurs on touch", state.contentAwareBlur) {
                viewModel.updateSetting(SettingKey.CONTENT_AWARE_BLUR, it)
            }
            SettingSwitch("Time-Based Themes", null, state.timeBasedThemes) {
                viewModel.updateSetting(SettingKey.TIME_BASED_THEMES, it)
            }
            SettingSwitch("High Contrast Mode", null, state.highContrastMode) {
                viewModel.updateSetting(SettingKey.HIGH_CONTRAST_MODE, it)
            }
            SettingSwitch("Elastic Stretch Effect", null, state.elasticStretch) {
                viewModel.updateSetting(SettingKey.ELASTIC_STRETCH, it)
            }
            SettingSwitch("Dot Mode", "Minimal idle dot", state.dotMode) {
                viewModel.updateSetting(SettingKey.DOT_MODE, it)
            }
            SettingSwitch("Icon Pack Integration", null, state.iconPackIntegration) {
                viewModel.updateSetting(SettingKey.ICON_PACK_INTEGRATION, it)
            }
            SettingSwitch("Material You Dynamic Contrast", null, state.materialYouDynamicContrast) {
                viewModel.updateSetting(SettingKey.MATERIAL_YOU_DYNAMIC_CONTRAST, it)
            }

            Text("Pill Shape", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("pill", "capsule", "squircle").forEach { shape ->
                    FilterChip(selected = state.pillShape == shape,
                        onClick = { viewModel.updateSetting(SettingKey.PILL_SHAPE, shape) },
                        label = { Text(shape.replaceFirstChar(Char::uppercase)) })
                }
            }
            SettingSlider("Corner Radius", state.pillCornerRadius, 8f..200f) {
                viewModel.updateSetting(SettingKey.PILL_RADIUS, it)
            }

            Text("Animation Speed", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimationSpeed.values().forEach { speed ->
                    FilterChip(selected = state.animationSpeed == speed,
                        onClick = { viewModel.updateSetting(SettingKey.ANIM_SPEED, speed.name) },
                        label = { Text(speed.name.lowercase().replaceFirstChar(Char::uppercase)) })
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- Notifications ---
        CollapsibleSection(
            title = "Notifications & Detection",
            icon = Icons.Default.Notifications,
            expanded = false,
            searchQuery = searchQuery,
            toggleTitles = listOf("OTP Detection", "Link Intercept", "Translation", "Barcode/QR", "Navigation",
                "Coalescing", "App Permission Checker")
        ) {
            SettingSwitch("OTP Detection", "Auto-copy one-time passwords", state.otpDetection) { viewModel.updateSetting(SettingKey.OTP_DETECTION, it) }
            SettingSwitch("Link Intercept", "Inline URL actions", state.linkIntercept) { viewModel.updateSetting(SettingKey.LINK_INTERCEPT, it) }
            SettingSwitch("Translation", null, state.translation) { viewModel.updateSetting(SettingKey.TRANSLATION, it) }
            SettingSwitch("Barcode / QR Scanner", "Contextual actions", state.barcode) { viewModel.updateSetting(SettingKey.BARCODE, it) }
            SettingSwitch("Navigation", "Turn-by-turn", state.navigation) { viewModel.updateSetting(SettingKey.NAVIGATION, it) }
            SettingSwitch("Notification Coalescing", "Group similar", state.notificationCoalescing) { viewModel.updateSetting(SettingKey.NOTIFICATION_COALESCING, it) }
            SettingSwitch("App Permission Checker", "Camera/mic indicator", state.appPermissionChecker) { viewModel.updateSetting(SettingKey.APP_PERMISSION_CHECKER, it) }
        }

        Spacer(Modifier.height(12.dp))

        // --- Media & Audio ---
        CollapsibleSection(
            title = "Media & Audio",
            icon = Icons.Default.MusicNote,
            expanded = false,
            searchQuery = searchQuery,
            toggleTitles = listOf("Artwork Blur", "Waveform", "Ambient Reactive Ring", "Audio Sensitivity",
                "BPM Pulse", "Live Music Visualizer", "Now Playing", "Live Caption", "Voice Memo Transcription")
        ) {
            SettingSwitch("Artwork Blur Background", null, state.mediaArtworkBlur) { viewModel.updateSetting(SettingKey.MEDIA_ARTWORK_BLUR, it) }
            SettingSwitch("Waveform Seeker", "AGSL shader", state.waveformEnabled) { viewModel.updateSetting(SettingKey.WAVEFORM_ENABLED, it) }
            SettingSwitch("Ambient Reactive Ring", "Thickness reacts to sound", state.ambientReactiveRing) { viewModel.updateSetting(SettingKey.AMBIENT_REACTIVE, it) }
            if (state.ambientReactiveRing) {
                SettingSlider("Audio Sensitivity", state.audioSensitivity, 0.1f..1f) { viewModel.updateSetting(SettingKey.AUDIO_SENSITIVITY, it) }
            }
            SettingSwitch("BPM Pulse", "Ring pulses with beat", state.bpmPulse) { viewModel.updateSetting(SettingKey.BPM_PULSE, it) }
            SettingSwitch("Live Music Visualizer", "Microphone-based", state.liveMusicVisualizer) { viewModel.updateSetting(SettingKey.LIVE_MUSIC_VISUALIZER, it) }
            SettingSwitch("Now Playing", "Ambient recognition", state.nowPlaying) { viewModel.updateSetting(SettingKey.NOW_PLAYING, it) }
            SettingSwitch("Live Caption", null, state.liveCaption) { viewModel.updateSetting(SettingKey.LIVE_CAPTION, it) }
            SettingSwitch("Voice Memo Transcription", null, state.voiceMemoTranscription) { viewModel.updateSetting(SettingKey.VOICE_MEMO_TRANSCRIPTION, it) }
        }

        Spacer(Modifier.height(12.dp))

        // --- Haptics ---
        CollapsibleSection(
            title = "Haptics",
            icon = Icons.Default.Vibration,
            expanded = false,
            searchQuery = searchQuery,
            toggleTitles = listOf("Haptic Feedback", "Intensity", "Ring Cadence Vibration", "Haptic Morse Alerts")
        ) {
            SettingSwitch("Haptic Feedback", null, state.hapticFeedback) { viewModel.updateSetting(SettingKey.HAPTIC_FEEDBACK, it) }
            if (state.hapticFeedback) {
                SettingSlider("Intensity", state.hapticIntensity, 0.1f..1f) { viewModel.updateSetting(SettingKey.HAPTIC_INTENSITY, it) }
                SettingSwitch("Ring Cadence Vibration", null, state.ringCadenceVibration) { viewModel.updateSetting(SettingKey.RING_CADENCE_VIBRATION, it) }
                SettingSwitch("Haptic Morse Alerts", "Distinct patterns", state.hapticMorseAlerts) { viewModel.updateSetting(SettingKey.HAPTIC_MORSE_ALERTS, it) }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- Gestures ---
        CollapsibleSection(
            title = "Gestures",
            icon = Icons.Default.Swipe,
            expanded = false,
            searchQuery = searchQuery,
            toggleTitles = listOf("Swipe Left", "Swipe Right")
        ) {
            Text("Swipe Left Action", style = MaterialTheme.typography.labelLarge)
            GestureActionChips(state.swipeLeftAction) { action ->
                viewModel.updateSetting(SettingKey.SWIPE_LEFT_ACTION, action)
            }
            Spacer(Modifier.height(8.dp))
            Text("Swipe Right Action", style = MaterialTheme.typography.labelLarge)
            GestureActionChips(state.swipeRightAction) { action ->
                viewModel.updateSetting(SettingKey.SWIPE_RIGHT_ACTION, action)
            }
        }

        // App selectors
        Spacer(Modifier.height(12.dp))
        var showMusicSelector by remember { mutableStateOf(false) }
        var showNotifSelector by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = { showMusicSelector = true }) { Text("Allowed Music Apps") }
            OutlinedButton(onClick = { showNotifSelector = true }) { Text("Allowed Notification Apps") }
        }
        if (showMusicSelector) {
            AppSelectorDialog(
                title = "Choose Music Apps",
                currentSelection = state.allowedMusicApps,
                onSelectionChanged = { viewModel.updateSetting(SettingKey.ALLOWED_MUSIC_APPS, it) },
                onDismiss = { showMusicSelector = false }
            )
        }
        if (showNotifSelector) {
            AppSelectorDialog(
                title = "Choose Notification Apps",
                currentSelection = state.allowedNotificationApps,
                onSelectionChanged = { viewModel.updateSetting(SettingKey.ALLOWED_NOTIFICATION_APPS, it) },
                onDismiss = { showNotifSelector = false }
            )
        }
    }
}

// ---------- Advanced Tab ----------
@Composable
fun AdvancedTab(state: SettingsState, viewModel: SettingsViewModel, searchQuery: String) {
    val scrollState = rememberScrollState()
    Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = 32.dp)) {
        // Prediction & AI
        CollapsibleSection("Prediction & AI", Icons.Default.Psychology, false, searchQuery,
            listOf("Pre-warming Tint", "Predictive Actions", "Auto-dismiss Delay", "Contextual Suggestions",
                "Gesture Learning", "Voice Trigger", "Adaptive Brightness/Volume", "App Prediction", "Routine Launcher")) {
            SettingSwitch("Pre-warming Tint", null, state.predictionTint) { viewModel.updateSetting(SettingKey.PREDICTION_TINT, it) }
            SettingSwitch("Predictive Actions", null, state.predictiveActions) { viewModel.updateSetting(SettingKey.PREDICTIVE_ACTIONS, it) }
            SettingSlider("Auto-dismiss Delay", state.autoDismissDelay.toFloat(), 2f..10f, steps = 7) { viewModel.updateSetting(SettingKey.AUTO_DISMISS_DELAY, it.toInt()) }
            SettingSwitch("Contextual Suggestions", null, state.contextualSuggestions) { viewModel.updateSetting(SettingKey.CONTEXTUAL_SUGGESTIONS, it) }
            SettingSwitch("Gesture Learning", null, state.gestureLearning) { viewModel.updateSetting(SettingKey.GESTURE_LEARNING, it) }
            SettingSwitch("Voice Trigger", "\"Hey Island\"", state.voiceTrigger) { viewModel.updateSetting(SettingKey.VOICE_TRIGGER, it) }
            SettingSwitch("Adaptive Brightness/Volume", null, state.adaptiveBrightnessVolume) { viewModel.updateSetting(SettingKey.ADAPTIVE_BRIGHTNESS_VOLUME, it) }
            SettingSwitch("App Prediction Suggestion", null, state.appPredictionSuggestion) { viewModel.updateSetting(SettingKey.APP_PREDICTION_SUGGESTION, it) }
            SettingSwitch("Contextual Routine Launcher", null, state.contextualRoutineLauncher) { viewModel.updateSetting(SettingKey.CONTEXTUAL_ROUTINE_LAUNCHER, it) }
        }
        Spacer(Modifier.height(12.dp))

        // Cross-Device
        CollapsibleSection("Cross-Device & Continuity", Icons.Default.Devices, false, searchQuery,
            listOf("Clipboard Sync", "Universal Control", "Quick Note", "Phone↔Tablet", "Nearby Share",
                "Multi-Device Clipboard", "Wear OS", "AirPods", "AirPlay", "HomePod")) {
            SettingSwitch("Clipboard Sync", null, state.clipboardSync) { viewModel.updateSetting(SettingKey.CLIPBOARD_SYNC, it) }
            SettingSwitch("Universal Control", null, state.universalControl) { viewModel.updateSetting(SettingKey.UNIVERSAL_CONTROL, it) }
            SettingSwitch("Quick Note", "Vol key + tap", state.quickNote) { viewModel.updateSetting(SettingKey.QUICK_NOTE, it) }
            SettingSwitch("Phone ↔ Tablet Handoff", null, state.phoneToTabletHandoff) { viewModel.updateSetting(SettingKey.PHONE_TO_TABLET_HANDOFF, it) }
            SettingSwitch("Nearby Share Progress", null, state.nearbyShareProgress) { viewModel.updateSetting(SettingKey.NEARBY_SHARE_PROGRESS, it) }
            SettingSwitch("Multi-Device Clipboard", null, state.multiDeviceClipboard) { viewModel.updateSetting(SettingKey.MULTI_DEVICE_CLIPBOARD, it) }
            SettingSwitch("Wear OS Remote", "Answer on watch", state.wearOsRemote) { viewModel.updateSetting(SettingKey.WEAR_OS_REMOTE, it) }
            SettingSwitch("AirPods Pop-up", null, state.airpodsPopup) { viewModel.updateSetting(SettingKey.AIRPODS_POPUP, it) }
            SettingSwitch("AirPlay/Cast Indicator", null, state.airplayCastIndicator) { viewModel.updateSetting(SettingKey.AIRPLAY_CAST_INDICATOR, it) }
            SettingSwitch("HomePod Remote Control", null, state.homePodControl) { viewModel.updateSetting(SettingKey.HOME_POD_CONTROL, it) }
        }
        Spacer(Modifier.height(12.dp))

        // iOS-Inspired
        CollapsibleSection("iOS-Inspired", Icons.Default.Phone, false, searchQuery,   // ← changed from Apple
            listOf("Live Activities", "Focus Filter", "Universal Clipboard Previews", "Always-On Display",
                "Face ID Padlock", "Ring/Silent Switch", "Timer", "MagSafe Animation", "Proximity Wake", "Focus Mode Pill")) {
            SettingSwitch("Live Activities API", null, state.liveActivitiesApi) { viewModel.updateSetting(SettingKey.LIVE_ACTIVITIES_API, it) }
            SettingSwitch("Focus Filter Integration", null, state.focusFilterIntegration) { viewModel.updateSetting(SettingKey.FOCUS_FILTER_INTEGRATION, it) }
            SettingSwitch("Universal Clipboard Previews", null, state.universalClipboardPreviews) { viewModel.updateSetting(SettingKey.UNIVERSAL_CLIPBOARD_PREVIEWS, it) }
            SettingSwitch("Always-On Display Companion", null, state.alwaysOnDisplayCompanion) { viewModel.updateSetting(SettingKey.ALWAYS_ON_DISPLAY_COMPANION, it) }
            SettingSwitch("Face ID / Unlock Padlock", null, state.faceIDPadlock) { viewModel.updateSetting(SettingKey.FACE_ID_PADLOCK, it) }
            SettingSwitch("Ring/Silent Mode Switch", null, state.ringModeSwitch) { viewModel.updateSetting(SettingKey.RING_MODE_SWITCH, it) }
            SettingSwitch("Timer Integration", null, state.timerIntegration) { viewModel.updateSetting(SettingKey.TIMER_INTEGRATION, it) }
            SettingSwitch("MagSafe Charging Animation", null, state.magsafeChargingAnimation) { viewModel.updateSetting(SettingKey.MAGSAFE_CHARGING_ANIMATION, it) }
            SettingSwitch("Proximity Wake", null, state.proximityWake) { viewModel.updateSetting(SettingKey.PROXIMITY_WAKE, it) }
            SettingSwitch("Focus Mode Pill", null, state.focusModePill) { viewModel.updateSetting(SettingKey.FOCUS_MODE_PILL, it) }
        }
        Spacer(Modifier.height(12.dp))

        // Gaming HUD
        CollapsibleSection("Gaming HUD", Icons.Default.SportsEsports, false, searchQuery,
            listOf("Enable Gaming HUD", "Show FPS", "Show CPU Temp", "Dashboard Overlay")) {
            SettingSwitch("Enable Gaming HUD", null, state.gamingHud) { viewModel.updateSetting(SettingKey.GAMING_HUD, it) }
            if (state.gamingHud) {
                SettingSwitch("Show FPS", null, state.showFps) { viewModel.updateSetting(SettingKey.SHOW_FPS, it) }
                SettingSwitch("Show CPU Temp", null, state.showCpuTemp) { viewModel.updateSetting(SettingKey.SHOW_CPU_TEMP, it) }
                SettingSwitch("Dashboard Overlay", null, state.gamingDashboardOverlay) { viewModel.updateSetting(SettingKey.GAMING_DASHBOARD_OVERLAY, it) }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Accessibility
        CollapsibleSection("Accessibility", Icons.Default.Accessibility, false, searchQuery,
            listOf("TalkBack Integration", "One-Hand Mode")) {
            SettingSwitch("TalkBack Integration", "Screen-reader support", state.talkbackIntegration) { viewModel.updateSetting(SettingKey.TALKBACK_INTEGRATION, it) }
            SettingSwitch("One-Hand Mode", null, state.oneHandMode) { viewModel.updateSetting(SettingKey.ONE_HAND_MODE, it) }
        }
        Spacer(Modifier.height(12.dp))

        // Customisation
        CollapsibleSection("Customisation", Icons.Default.Brush, false, searchQuery,
            listOf("Third-Party Widget API", "Custom Pill Animations")) {
            SettingSwitch("Third-Party Widget API", "Mini widgets in island", state.thirdPartyWidgetApi) { viewModel.updateSetting(SettingKey.THIRD_PARTY_WIDGET_API, it) }
            SettingSwitch("Custom Pill Animations", "Lottie/Rive support", state.customPillAnimations != null) {
                viewModel.updateSetting(SettingKey.CUSTOM_PILL_ANIMATIONS, if (it) "default" else null)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Battery & Performance
        CollapsibleSection("Battery & Performance", Icons.Default.BatterySaver, false, searchQuery,
            listOf("Battery-Aware Animation", "Doze Mode Optimisation", "Quick Performance Profile", "Data Saver")) {
            SettingSwitch("Battery-Aware Animation", "Disable effects <15%", state.batteryAwareAnimation) { viewModel.updateSetting(SettingKey.BATTERY_AWARE_ANIMATION, it) }
            SettingSwitch("Doze Mode Optimisation", null, state.dozeModeOptimisation) { viewModel.updateSetting(SettingKey.DOZE_MODE_OPTIMISATION, it) }
            SettingSwitch("Quick Performance Profile", null, state.quickPerformanceProfile) { viewModel.updateSetting(SettingKey.QUICK_PERFORMANCE_PROFILE, it) }
            SettingSwitch("Data Saver", "Disable blur/fetch on mobile", state.dataSaver) { viewModel.updateSetting(SettingKey.DATA_SAVER, it) }
        }
        Spacer(Modifier.height(12.dp))

        // Gamification
        CollapsibleSection("Gamification", Icons.Default.EmojiEvents, false, searchQuery,
            listOf("Achievements", "Show Badges", "Island Streaks", "Leaderboard", "Exclusive Themes")) {
            SettingSwitch("Achievements", null, state.achievementsEnabled) { viewModel.updateSetting(SettingKey.ACHIEVEMENTS_ENABLED, it) }
            if (state.achievementsEnabled) {
                SettingSwitch("Show Achievement Badges", null, state.showAchievementBadge) { viewModel.updateSetting(SettingKey.ACHIEVEMENTS_DISPLAY, it) }
            }
            SettingSwitch("Island Streaks", "Daily interaction counter", state.islandStreaks) { viewModel.updateSetting(SettingKey.ISLAND_STREAKS, it) }
            SettingSwitch("Leaderboard", "Compare with friends", state.leaderboard) { viewModel.updateSetting(SettingKey.LEADERBOARD, it) }
            SettingSwitch("Exclusive Themes", "Beta tester rewards", state.exclusiveThemes) { viewModel.updateSetting(SettingKey.EXCLUSIVE_THEMES, it) }
        }
        Spacer(Modifier.height(12.dp))

        // Privacy & Security
        CollapsibleSection("Privacy & Security", Icons.Default.Security, false, searchQuery,
            listOf("Clipboard Cleaner", "VPN/Tor Indicator", "Silent/Ring Switch", "Call Screen Transcript")) {
            SettingSwitch("Clipboard Cleaner", "Clear passwords after timeout", state.clipboardCleaner) { viewModel.updateSetting(SettingKey.CLIPBOARD_CLEANER, it) }
            SettingSwitch("VPN / Tor Indicator", null, state.vpnTorIndicator) { viewModel.updateSetting(SettingKey.VPN_TOR_INDICATOR, it) }
            SettingSwitch("Silent/Ring Mode Switch", null, state.silentRingModeSwitch) { viewModel.updateSetting(SettingKey.SILENT_RING_MODE_SWITCH, it) }
            SettingSwitch("Call Screen Transcript", null, state.callScreenTranscript) { viewModel.updateSetting(SettingKey.CALL_SCREEN_TRANSCRIPT, it) }
        }
        Spacer(Modifier.height(12.dp))

        // Weather & Environment
        CollapsibleSection("Weather & Environment", Icons.Default.WbSunny, false, searchQuery,
            listOf("Weather Mood Ring", "At a Glance")) {
            SettingSwitch("Weather Mood Ring", "Color reflects weather", state.weatherMoodRing) { viewModel.updateSetting(SettingKey.WEATHER_MOOD_RING, it) }
            SettingSwitch("At a Glance", "Calendar/commute/weather", state.atAGlance) { viewModel.updateSetting(SettingKey.AT_A_GLANCE, it) }
        }
        Spacer(Modifier.height(12.dp))

        // Multi-Event Handling
        CollapsibleSection("Multi-Event Handling", Icons.Default.Layers, false, searchQuery,
            listOf("Split Pill", "Merge Events", "Multi-Island Stack")) {
            SettingSwitch("Split Pill", null, state.splitPillEnabled) { viewModel.updateSetting(SettingKey.SPLIT_PILL_ENABLED, it) }
            SettingSwitch("Merge Simultaneous Events", null, state.mergeSimultaneousEvents) { viewModel.updateSetting(SettingKey.MERGE_SIMULTANEOUS_EVENTS, it) }
            SettingSwitch("Multi-Island Stack", "Vertical stack pills", state.multiIslandStack) { viewModel.updateSetting(SettingKey.MULTI_ISLAND_STACK, it) }
        }
        Spacer(Modifier.height(12.dp))

        // Experimental
        CollapsibleSection("Experimental", Icons.Default.Science, false, searchQuery,
            listOf("AR Island", "Mindfulness Breath Pacer", "Morse Code Input", "Multi-User Profile Switching", "Crypto/Stock Ticker")) {
            SettingSwitch("AR Island", null, state.arIsland) { viewModel.updateSetting(SettingKey.AR_ISLAND, it) }
            SettingSwitch("Mindfulness Breath Pacer", null, state.mindfulnessBreathPacer) { viewModel.updateSetting(SettingKey.MINDFULNESS_BREATH_PACER, it) }
            SettingSwitch("Morse Code Input", "Long-press to type", state.morseCodeInput) { viewModel.updateSetting(SettingKey.MORSE_CODE_INPUT, it) }
            SettingSwitch("Multi-User Profile Switching", null, state.multiUserProfileSwitching) { viewModel.updateSetting(SettingKey.MULTI_USER_PROFILE_SWITCHING, it) }
            SettingSwitch("Crypto / Stock Ticker", "Live scrolling ticker", state.cryptoStockTicker) { viewModel.updateSetting(SettingKey.CRYPTO_STOCK_TICKER, it) }
        }
        Spacer(Modifier.height(12.dp))

        // Developer Tools
        CollapsibleSection("Developer Tools", Icons.Default.Code, false, searchQuery,
            listOf("ADB Command Injector", "Tasker Plugin", "Log/Debug Overlay", "Open-Source SDK", "Root/ADB Features")) {
            SettingSwitch("ADB Command Injector", null, state.adbCommandInjector) { viewModel.updateSetting(SettingKey.ADB_COMMAND_INJECTOR, it) }
            SettingSwitch("Tasker / MacroDroid Plugin", null, state.taskerPlugin) { viewModel.updateSetting(SettingKey.TASKER_PLUGIN, it) }
            SettingSwitch("Log & Debug Overlay", "FPS/battery stats", state.logDebugOverlay) { viewModel.updateSetting(SettingKey.LOG_DEBUG_OVERLAY, it) }
            SettingSwitch("Open-Source SDK", "Community contributions", state.openSourceSdk) { viewModel.updateSetting(SettingKey.OPEN_SOURCE_SDK, it) }
            SettingSwitch("Root / ADB Features", "Advanced system hooks", state.rootAdbFeatures) { viewModel.updateSetting(SettingKey.ROOT_ADB_FEATURES, it) }
        }
        Spacer(Modifier.height(12.dp))

        // Quick Settings & Tiles
        CollapsibleSection("Quick Settings & Tiles", Icons.Default.Settings, false, searchQuery,
            listOf("Quick Settings Tile", "Digital Wellbeing", "Continuity Camera Actions")) {
            SettingSwitch("Quick Settings Tile", null, state.quickSettingsTile) { viewModel.updateSetting(SettingKey.QUICK_SETTINGS_TILE, it) }
            SettingSwitch("Digital Wellbeing Integration", null, state.digitalWellbeingIntegration) { viewModel.updateSetting(SettingKey.DIGITAL_WELLBEING_INTEGRATION, it) }
            SettingSwitch("Continuity Camera Actions", null, state.continuityCameraActions) { viewModel.updateSetting(SettingKey.CONTINUITY_CAMERA_ACTIONS, it) }
        }
    }
}

// ---------- Live Preview Composables ----------
@Composable
fun IslandPreview(state: SettingsState) {
    val shape = getPillShape(state.pillShape, state.pillCornerRadius)
    val gradient = if (state.dynamicGradient) Brush.horizontalGradient(listOf(state.customAccentColor, Color.Cyan)) else null
    val bgModifier = Modifier
        .size(width = 200.dp, height = 40.dp)
        .clip(shape)
        .background(gradient ?: Brush.verticalGradient(listOf(Color.DarkGray, Color.Black)))
        .then(if (state.glowEffect) Modifier.shadow(8.dp, shape) else Modifier)   // shadow import now present
        .padding(8.dp)

    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Live Preview", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(bgModifier, contentAlignment = Alignment.Center) {
                Text("Music • 2:34", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

// ---------- Collapsible Section Helper ----------
@Composable
fun CollapsibleSection(
    title: String,
    icon: ImageVector? = null,
    expanded: Boolean = false,
    searchQuery: String = "",
    toggleTitles: List<String> = emptyList(),
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(expanded) }

    // Filter visibility
    val matchesSearch = if (searchQuery.isEmpty()) true
        else title.contains(searchQuery, ignoreCase = true) || toggleTitles.any { it.contains(searchQuery, ignoreCase = true) }

    AnimatedVisibility(visible = matchesSearch, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Collapsible content
            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    content()
                }
            }
        }
    }
}

// ---------- Reusable Setting Components ----------
@Composable
fun SettingSwitch(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingSlider(title: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, steps: Int = 0, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(String.format("%.1f", value), style = MaterialTheme.typography.labelSmall)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps)
    }
}

// Add these functions in the same file or in a separate helper file

fun getPillShape(shape: String, cornerRadius: Float): androidx.compose.foundation.shape.RoundedCornerShape {
    return when (shape) {
        "capsule" -> RoundedCornerShape(50)
        "squircle" -> RoundedCornerShape(cornerRadius / 2)
        else -> RoundedCornerShape(cornerRadius.dp)
    }
}

fun Modifier.glassBackground(blurRadius: androidx.compose.ui.unit.Dp): Modifier = this
    .blur(blurRadius)                          // blur import now present
    .background(Color.White.copy(alpha = 0.1f))

@Composable
fun AppSelectorDialog(
    title: String,
    currentSelection: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // Simple dialog implementation – replace with your actual UI
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text("App selection dialog – implement as needed") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)   // ← added for FlowRow
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