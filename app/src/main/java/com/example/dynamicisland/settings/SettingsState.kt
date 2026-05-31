package com.example.dynamicisland.settings

import androidx.compose.ui.graphics.Color

/**
 * Pro-Grade Settings State
 * Centrally manages all 6 Pillars of the Dynamic Island.
 */
data class SettingsState(
    // === Pillar 1: Dual-Mode Architecture ===
    val designLanguage: DesignLanguage = DesignLanguage.MATERIAL_YOU,
    val liveBridgeEnabled: Boolean = false,
    val magneticEdgeDocking: Boolean = true,
    
    // === Pillar 2: Ethereal Visuals ===
    val dynamicColors: Boolean = true,
    val customAccentColor: Color = Color(0xFF6750A4),
    val blurIntensity: Float = 15f,
    val geminiAuraEnabled: Boolean = true,
    val rollingTypographyEnabled: Boolean = true,
    val aestheticStyle: AestheticStyle = AestheticStyle.GLASS,
    val monochromeIcons: Boolean = false,
    
    // === Pillar 3: Advanced Live Activities ===
    val parseDeliveryNotifications: Boolean = true,
    val warpChargeAnimation: Boolean = true,
    val batteryAwareAnimation: Boolean = true,
    val nowPlaying: Boolean = true,
    val musicVisualizerStyle: String = "NEURAL_CIRCLE",
    val waveformEnabled: Boolean = true,

    // === Pillar 4: Fluid Physics & Gestures ===
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val physicsStyle: PhysicsStyle = PhysicsStyle.APPLE,
    val contentTransitionStyle: ContentTransitionStyle = ContentTransitionStyle.SLIDE,
    val velocitySquishEnabled: Boolean = true,
    val inlineReplyEnabled: Boolean = true,
    
    // === Pillar 5: Dashboard & Widgets ===
    val enableMaxWidgets: Boolean = true,
    val showVitalsRam: Boolean = true,
    val showVitalsCpu: Boolean = true,
    val showVitalsNet: Boolean = true,
    val showVitalsFps: Boolean = true,
    val showVitalsBatCycles: Boolean = true,
    val shortcutLayout: ShortcutLayout = ShortcutLayout.GRID,
    
    // === Pillar 6: DeGoogled Bridge ===
    val assistBridgeEnabled: Boolean = false,
    val assistBridgeTarget: String = "com.brave.browser",
    val lensBridgeEnabled: Boolean = false,
    val lensBridgeTarget: String = "com.brave.browser",

    // --- Core Controls & Privacy ---
    val islandEnabled: Boolean = true,
    val showRingIdle: Boolean = true,
    val pillShape: String = "pill",
    val pillCornerRadius: Float = 100f,
    val hideOnScreenshot: Boolean = true,
    val hideOnScreenRecord: Boolean = true,
    val hideIslandPerApp: Set<String> = emptySet(),
    val enableFocusMode: Boolean = false,
    val productiveApps: Set<String> = emptySet(),
    val enableLowLatencyMode: Boolean = false,
    val enableClipboardPaperclip: Boolean = true,
    
    // --- AI & Prediction ---
    val predictionTint: Boolean = true,
    val predictiveActions: Boolean = true,
    val autoDismissDelay: Int = 5,
    val contextualSuggestions: Boolean = true,
    val gestureLearning: Boolean = true,
    
    // --- Legacy / Miscellaneous ---
    val otpDetection: Boolean = true,
    val linkIntercept: Boolean = true,
    val translation: Boolean = true,
    val barcode: Boolean = true,
    val navigation: Boolean = true,
    val notificationCoalescing: Boolean = true,
    val appPermissionChecker: Boolean = true,
    val gamingHud: Boolean = true,
    val hapticFeedback: Boolean = true,
    val hapticIntensity: Float = 1f,
    val ringCadenceVibration: Boolean = true,
    val islandOnLockscreen: Boolean = true,
    val lockscreenFeatures: Set<String> = setOf("music", "notifications"),
    val allowedNotificationApps: Set<String> = emptySet(),
    val roleCallingApp: String = "",
    val allowedMusicApps: Set<String> = emptySet(),
    val allowedMediaApps: Set<String> = emptySet(),
    val allowedNotesApps: Set<String> = emptySet(),
    val callStyle: CallStyle = CallStyle.IOS,
    val chargingStyle: ChargingStyle = ChargingStyle.RING,
    val batteryStyle: BatteryStyle = BatteryStyle.PILL,
    val ringPulseStyle: RingPulseStyle = RingPulseStyle.BREATH,
    val autoBackupEnabled: Boolean = false,
    val autoBackupFreqDays: Int = 7,
    val stashStoragePath: String = "/sdcard/DynamicIsland/Archive",
    val aiConfidenceThreshold: Int = 10,
    val aiReinforcementRate: Float = 1.0f,
    val iconPack: IconPack = IconPack.MaterialYou,
    
    // --- Missing fields from controller ---
    val wifiAlertDuration: Int = 3,
    val btAlertDuration: Int = 3,
    val hotspotAlertDuration: Int = 5,
    val dataAlertDuration: Int = 3,
    val ringMediaVisible: Boolean = true,
    val ringBatteryVisible: Boolean = true,
    val ringDataVisible: Boolean = true,
    val invisibleRingTouchPassthrough: Boolean = true,
    val antiBurnInEnabled: Boolean = true,
    val antiBurnInIntensity: Float = 1.5f,
    val smartGesturesEnabled: Boolean = true,
    val smartCallOverride: Boolean = true,
    val smartMediaOverride: Boolean = true,
    val smartGamingOverride: Boolean = true,
    val freeformSmartGesture: Boolean = true,
    val freeformLaunchEnabled: Boolean = true,
    val talkbackIntegration: Boolean = true,
    val proximityWake: Boolean = false,
    val timerIntegration: Boolean = true,
    val allowChargingMini: Boolean = true,
    val allowChargingMid: Boolean = true,
    val allowNotifMini: Boolean = true,
    val allowNotifMid: Boolean = true,
    val allowNotifMax: Boolean = true,
    val allowCallMid: Boolean = true,
    val allowCallMax: Boolean = true,
    val allowTaskMini: Boolean = true,
    val allowTaskMid: Boolean = true,
    val dynamicGradient: Boolean = true,
    val splitPillEnabled: Boolean = true
)

enum class DesignLanguage { MATERIAL_YOU, APPLE_LIQUID_GLASS }
enum class AnimationSpeed { SLOW, NORMAL, FAST }
enum class CallStyle { IOS, MINIMAL, MODERN }
enum class ChargingStyle { RING, WAVE, CUBE }
enum class BatteryStyle { PILL, GAUGE, DIGITAL }
enum class PhysicsStyle { APPLE, OXYGEN_OS }
enum class ContentTransitionStyle { SLIDE, FADE_SCALE, FLIP }
enum class RingPulseStyle { BREATH, LASER, NONE }
enum class AestheticStyle { GLASS, VOID_BLACK }
enum class ShortcutLayout { GRID, CAROUSEL }
