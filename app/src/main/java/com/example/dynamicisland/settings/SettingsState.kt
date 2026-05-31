package com.example.dynamicisland.settings

import androidx.compose.ui.graphics.Color

data class SettingsState(
    // === Appearance ===
    val designLanguage: DesignLanguage = DesignLanguage.MATERIAL_YOU,
    val dynamicColors: Boolean = true,
    val customAccentColor: Color = Color(0xFF6750A4),
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

    // === Notifications & Detection ===
    val otpDetection: Boolean = true,
    val linkIntercept: Boolean = true,
    val translation: Boolean = true,
    val barcode: Boolean = true,
    val navigation: Boolean = true,
    val notificationCoalescing: Boolean = true,
    val appPermissionChecker: Boolean = true,

    // === Gaming HUD ===
    val gamingHud: Boolean = true,
    val showFps: Boolean = false,
    val showCpuTemp: Boolean = false,
    val gamingDashboardOverlay: Boolean = false,

    // === Media & Audio ===
    val mediaArtworkBlur: Boolean = true,
    val waveformEnabled: Boolean = true,
    val ambientReactiveRing: Boolean = true,
    val audioSensitivity: Float = 0.5f,
    val bpmPulse: Boolean = true,
    val liveMusicVisualizer: Boolean = false,
    val nowPlaying: Boolean = true,
    val liveCaption: Boolean = false,
    val voiceMemoTranscription: Boolean = true,

    // === Haptics ===
    val hapticFeedback: Boolean = true,
    val hapticIntensity: Float = 1f,
    val ringCadenceVibration: Boolean = true,
    val hapticMorseAlerts: Boolean = false,

    // === Prediction & Smart Features ===
    val predictionTint: Boolean = true,
    val predictiveActions: Boolean = true,
    val autoDismissDelay: Int = 5,
    val contextualSuggestions: Boolean = true,
    val gestureLearning: Boolean = true,
    val voiceTrigger: Boolean = false,
    val adaptiveBrightnessVolume: Boolean = false,
    val appPredictionSuggestion: Boolean = true,
    val contextualRoutineLauncher: Boolean = true,

    // === Cross-Device & Continuity ===
    val clipboardSync: Boolean = false,
    val universalControl: Boolean = false,
    val quickNote: Boolean = true,
    val phoneToTabletHandoff: Boolean = false,
    val nearbyShareProgress: Boolean = true,
    val multiDeviceClipboard: Boolean = false,
    val wearOsRemote: Boolean = false,
    val airpodsPopup: Boolean = true,
    val airplayCastIndicator: Boolean = true,
    val homePodControl: Boolean = false,

    // === iOS-Inspired ===
    val liveActivitiesApi: Boolean = true,
    val focusFilterIntegration: Boolean = true,
    val universalClipboardPreviews: Boolean = true,
    val alwaysOnDisplayCompanion: Boolean = true,
    val faceIDPadlock: Boolean = true,
    val ringModeSwitch: Boolean = true,
    val timerIntegration: Boolean = true,
    val magsafeChargingAnimation: Boolean = true,
    val proximityWake: Boolean = false,

    // === Android Ecosystem ===
    val materialYouDynamicContrast: Boolean = true,
    val quickSettingsTile: Boolean = true,
    val digitalWellbeingIntegration: Boolean = false,
    val rootAdbFeatures: Boolean = false,
    val iconPack: IconPack = IconPack.MaterialYou,

    // === Accessibility ===
    val talkbackIntegration: Boolean = true,
    val oneHandMode: Boolean = false,
    val dedicatedOneHandPlacement: Boolean = false,

    // === Customisation ===
    val customPillAnimations: String? = null,
    val thirdPartyWidgetApi: Boolean = false,

    // === Battery & Performance ===
    val batteryAwareAnimation: Boolean = true,
    val dozeModeOptimisation: Boolean = true,
    val quickPerformanceProfile: Boolean = true,
    val dataSaver: Boolean = false,

    // === Gamification ===
    val islandStreaks: Boolean = true,
    val leaderboard: Boolean = false,
    val exclusiveThemes: Boolean = false,
    val achievementsEnabled: Boolean = true,
    val showAchievementBadge: Boolean = true,

    // === Privacy & Security ===
    val clipboardCleaner: Boolean = true,
    val vpnTorIndicator: Boolean = true,

    // === Experimental ===
    val arIsland: Boolean = false,
    val mindfulnessBreathPacer: Boolean = false,
    val morseCodeInput: Boolean = false,
    val multiUserProfileSwitching: Boolean = false,
    val cryptoStockTicker: Boolean = false,

    // === Developer Tools ===
    val adbCommandInjector: Boolean = false,
    val taskerPlugin: Boolean = true,
    val logDebugOverlay: Boolean = false,
    val openSourceSdk: Boolean = true,

    // === Advanced Triggers & Sensors ===
    val ringMediaVisible: Boolean = true,
    val ringBatteryVisible: Boolean = true,
    val ringDownloadVisible: Boolean = true,
    val ringBluetoothVisible: Boolean = true,
    val ringHotspotVisible: Boolean = true,
    val ringDataVisible: Boolean = true,
    
    val invisibleRingTouchPassthrough: Boolean = true,
    val antiBurnInEnabled: Boolean = true,
    val antiBurnInIntensity: Float = 1.5f, // 1px to 3px
    
    val wifiAlertDuration: Int = 3,
    val btAlertDuration: Int = 3,
    val hotspotAlertDuration: Int = 5,
    val dataAlertDuration: Int = 3,
    
    val liveDownloadTracking: Boolean = true,
    val networkSpeedRing: Boolean = true,

    // === Smart AI Gestures ===
    val smartGesturesEnabled: Boolean = true,
    val smartMediaOverride: Boolean = true,
    val smartGamingOverride: Boolean = true,
    val smartCallOverride: Boolean = true,

    // === State Constraint Engine ===
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

    // === Floating / Freeform Windows ===
    val freeformLaunchEnabled: Boolean = true,
    val freeformSmartGesture: Boolean = true, // Swipe Down to open in floating window

    // === Global Controls ===
    val islandEnabled: Boolean = true,
    val islandOnLockscreen: Boolean = true,
    val lockscreenFeatures: Set<String> = setOf("music", "notifications"),
    val allowedNotificationApps: Set<String> = emptySet(),
    val swipeLeftAction: String = "dismiss",
    val swipeRightAction: String = "next_track",

    // === Call & Communication ===
    val callScreenTranscript: Boolean = true,
    val silentRingModeSwitch: Boolean = true,

    // === Weather & Environment ===
    val weatherMoodRing: Boolean = true,
    val atAGlance: Boolean = true,

    // === Split & Multi-Event ===
    val splitPillEnabled: Boolean = true,
    val mergeSimultaneousEvents: Boolean = true,
    val multiIslandStack: Boolean = false,

    // === Continuity Camera ===
    val continuityCameraActions: Boolean = true,

    // === Focus & DND ===
    val focusModePill: Boolean = true,

    // === App Roles ===
    val roleCallingApp: String = "",
    val roleGameLauncher: String = "",
    val allowedMusicApps: Set<String> = emptySet(),
    val allowedMediaApps: Set<String> = emptySet(),
    val allowedNotesApps: Set<String> = emptySet(),

    // === Styles ===
    val callStyle: CallStyle = CallStyle.IOS,
    val chargingStyle: ChargingStyle = ChargingStyle.RING,
    val batteryStyle: BatteryStyle = BatteryStyle.PILL
)

enum class DesignLanguage { MATERIAL_YOU, APPLE_LIQUID_GLASS }
enum class AnimationSpeed { SLOW, NORMAL, FAST }
enum class CallStyle { IOS, MINIMAL, MODERN }
enum class ChargingStyle { RING, WAVE, CUBE }
enum class BatteryStyle { PILL, GAUGE, DIGITAL }
