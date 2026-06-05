package com.example.dynamicisland.shared.settings

enum class DesignLanguage { MATERIAL_YOU, APPLE_LIQUID_GLASS, VYXEL_EXPRESSIVE }
enum class AnimationSpeed { SLOW, NORMAL, FAST }
enum class CallStyle { IOS, MINIMAL, MODERN }
enum class ChargingStyle { RING, WAVE, CUBE }
enum class BatteryStyle { PILL, GAUGE, DIGITAL }
enum class PhysicsStyle { APPLE, OXYGEN_OS }
enum class ContentTransitionStyle { SLIDE, FADE_SCALE, FLIP }
enum class RingPulseStyle { BREATH, LASER, NONE }
enum class AestheticStyle { GLASS, VOID_BLACK, LIQUID_GLASS }
enum class ShortcutLayout { GRID, CAROUSEL }
enum class FontAesthetic { DEFAULT, MONOSPACE, KILO, CHOCOCOOKY, GOOGLE_SANS }
enum class IconPack { MaterialYou, iOS, OxygenOS, Samsung, Pixel, Futuristic, Minimal, Bold, Outline }

/**
 * THE DEFINITIVE SETTINGS STATE (CLEAN VERSION)
 * No Compose dependencies.
 */
data class SettingsState(
    // Appearance & Aesthetics
    val designLanguage: DesignLanguage = DesignLanguage.MATERIAL_YOU,
    val dynamicColors: Boolean = true,
    val customAccentColor: Int = 0xFF6750A4.toInt(),
    val blurIntensity: Float = 15f,
    val pillCornerRadius: Float = 100f,
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val showRingIdle: Boolean = true,
    val pillShape: String = "pill",
    val dynamicGradient: Boolean = true,
    val glowEffect: Boolean = true,
    val dotMode: Boolean = false,
    val elasticStretch: Boolean = true,
    val shadowCasting: Boolean = true,
    val contentAwareBlur: Boolean = true,
    val timeBasedThemes: Boolean = false,
    val highContrastMode: Boolean = false,
    val iconPackIntegration: Boolean = false,
    val aestheticStyle: AestheticStyle = AestheticStyle.GLASS,
    val monochromeIcons: Boolean = false,
    val enableMetaballTear: Boolean = true,
    val squishIntensity: Float = 1.0f,
    val fontAesthetic: FontAesthetic = FontAesthetic.DEFAULT,
    val appLanguage: String = "System",
    val expressiveMeshEnabled: Boolean = true,

    // Dual-Mode Architecture (Pillar 1)
    val liveBridgeEnabled: Boolean = false,
    val magneticEdgeDocking: Boolean = true,
    val navIslandMode: Boolean = false,
    val isNavIslandFloating: Boolean = false,
    val oneHandModeEnabled: Boolean = true,
    val navIslandBatteryColoring: Boolean = true,
    val navIslandDullBackground: Boolean = true,
    val navIslandShowPipeIndicator: Boolean = true,
    val navIslandMusicBarMorph: Boolean = true,
    val navIslandPanicTile: Boolean = true,

    // Advanced Visuals (Pillar 2)
    val geminiAuraEnabled: Boolean = true,
    val rollingTypographyEnabled: Boolean = true,
    val thermalThrottlingEnabled: Boolean = true,

    // Advanced Live Activities (Pillar 3)
    val parseDeliveryNotifications: Boolean = true,
    val warpChargeAnimation: Boolean = true,
    val batteryAwareAnimation: Boolean = true,
    val nowPlaying: Boolean = true,
    val musicVisualizerStyle: String = "NEURAL_CIRCLE",
    val waveformEnabled: Boolean = true,
    val mediaArtworkBlur: Boolean = true,
    val bpmPulse: Boolean = true,
    val ambientReactiveRing: Boolean = true,
    val ambientReactive: Boolean = true,

    // Fluid Physics & Gestures (Pillar 4)
    val physicsStyle: PhysicsStyle = PhysicsStyle.APPLE,
    val contentTransitionStyle: ContentTransitionStyle = ContentTransitionStyle.SLIDE,
    val velocitySquishEnabled: Boolean = true,
    val inlineReplyEnabled: Boolean = true,

    // Dashboard & Widgets (Pillar 5)
    val enableMaxWidgets: Boolean = true,
    val showVitalsRam: Boolean = true,
    val showVitalsCpu: Boolean = true,
    val showVitalsNet: Boolean = true,
    val showVitalsFps: Boolean = true,
    val showVitalsBatCycles: Boolean = true,
    val shortcutLayout: ShortcutLayout = ShortcutLayout.GRID,

    // DeGoogled Bridge (Pillar 6)
    val assistBridgeEnabled: Boolean = false,
    val assistBridgeTarget: String = "com.brave.browser",
    val lensBridgeEnabled: Boolean = false,
    val lensBridgeTarget: String = "com.brave.browser",

    // Smart AI & Prediction
    val smartGesturesEnabled: Boolean = true,
    val smartCallOverride: Boolean = true,
    val smartMediaOverride: Boolean = true,
    val smartGamingOverride: Boolean = true,
    val predictionTint: Boolean = true,
    val predictiveActions: Boolean = true,
    val autoDismissDelay: Int = 5,
    val contextualSuggestions: Boolean = true,
    val gestureLearning: Boolean = true,
    val aiConfidenceThreshold: Int = 10,
    val aiReinforcementRate: Float = 1.0f,

    // State Constraints
    val allowMusicMid: Boolean = true,
    val allowMusicMax: Boolean = true,
    val allowChargingMini: Boolean = true,
    val allowChargingMid: Boolean = true,
    val allowNotifMini: Boolean = true,
    val allowNotifMid: Boolean = true,
    val allowNotifMax: Boolean = true,
    val allowCallMid: Boolean = true,
    val allowCallMax: Boolean = true,
    val allowTaskMini: Boolean = true,
    val allowTaskMid: Boolean = true,

    // Floating Windows & Accessibility
    val freeformLaunchEnabled: Boolean = true,
    val freeformSmartGesture: Boolean = true,
    val enableFreeformPortalAnim: Boolean = true,
    val talkbackIntegration: Boolean = true,
    val proximityWake: Boolean = false,
    val timerIntegration: Boolean = true,

    // Global Controls & Privacy
    val islandEnabled: Boolean = true,
    val redwoodEnabled: Boolean = true,
    val islandOnLockscreen: Boolean = true,
    val lockscreenFeatures: Set<String> = setOf("music", "notifications"),
    val allowedNotificationApps: Set<String> = emptySet(),
    val swipeLeftAction: String = "dismiss",
    val swipeRightAction: String = "next_track",
    val showFps: Boolean = false,
    val showCpuTemp: Boolean = false,
    val hideOnScreenshot: Boolean = true,
    val hideOnScreenRecord: Boolean = true,
    val hideIslandPerApp: Set<String> = emptySet(),
    val enableFocusMode: Boolean = false,
    val productiveApps: Set<String> = emptySet(),
    val pinnedApps: Set<String> = emptySet(),
    val enableLowLatencyMode: Boolean = false,
    val enableClipboardPaperclip: Boolean = true,
    val clipboardCleaner: Boolean = true,
    val privacyDotsEnabled: Boolean = false,
    val dozeModeOptimisation: Boolean = true,

    // Detection & Alerts
    val otpDetection: Boolean = true,
    val linkIntercept: Boolean = true,
    val translation: Boolean = true,
    val barcode: Boolean = true,
    val navigation: Boolean = true,
    val notificationCoalescing: Boolean = true,
    val appPermissionChecker: Boolean = true,
    val gamingHud: Boolean = true,

    // Connectivity & Durations
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

    // Haptics
    val hapticFeedback: Boolean = true,
    val hapticIntensity: Float = 1f,
    val ringCadenceVibration: Boolean = true,
    val hapticMorseAlerts: Boolean = false,
    val syncPulseVibration: Boolean = false,

    // Roles & Storage
    val roleCallingApp: String = "",
    val allowedMusicApps: Set<String> = emptySet(),
    val allowedMediaApps: Set<String> = emptySet(),
    val allowedNotesApps: Set<String> = emptySet(),
    val autoBackupEnabled: Boolean = false,
    val autoBackupFreqDays: Int = 7,
    val stashStoragePath: String = "/sdcard/DynamicIsland/Archive",
    val splitPillEnabled: Boolean = true,

    // Styles & Enums
    val callStyle: CallStyle = CallStyle.IOS,
    val chargingStyle: ChargingStyle = ChargingStyle.RING,
    val batteryStyle: BatteryStyle = BatteryStyle.PILL,
    val ringPulseStyle: RingPulseStyle = RingPulseStyle.BREATH,
    val iconPack: IconPack = IconPack.MaterialYou
)
