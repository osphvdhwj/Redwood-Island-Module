package com.example.dynamicisland.settings

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.dynamicisland.settings.SettingsManager.SettingKey

class SettingsViewModel(private val settingsManager: SettingsManager) {
    var state by mutableStateOf(SettingsState())
        private set

    init {
        loadAllSettings()
    }

    private fun loadAllSettings() {
        state = SettingsState(
            // ==================== Appearance ====================
            designLanguage = if (settingsManager.getBoolean(SettingKey.USE_LIQUID_GLASS, false))
                DesignLanguage.APPLE_LIQUID_GLASS else DesignLanguage.MATERIAL_YOU,
            dynamicColors = settingsManager.getBoolean(SettingKey.DYNAMIC_COLORS, true),
            customAccentColor = Color(settingsManager.getInt(SettingKey.ACCENT_COLOR, 0xFF6750A4.toInt())),
            blurIntensity = settingsManager.getFloat(SettingKey.BLUR_INTENSITY, 15f),
            pillCornerRadius = settingsManager.getFloat(SettingKey.PILL_RADIUS, 100f),
            animationSpeed = AnimationSpeed.valueOf(
                settingsManager.getString(SettingKey.ANIM_SPEED, "NORMAL") ?: "NORMAL"
            ),
            showRingIdle = settingsManager.getBoolean(SettingKey.RING_IDLE, true),
            pillShape = settingsManager.getString(SettingKey.PILL_SHAPE, "pill") ?: "pill",
            dynamicGradient = settingsManager.getBoolean(SettingKey.DYNAMIC_GRADIENT, true),
            glowEffect = settingsManager.getBoolean(SettingKey.GLOW_EFFECT, true),
            dotMode = settingsManager.getBoolean(SettingKey.DOT_MODE, false),
            elasticStretch = settingsManager.getBoolean(SettingKey.ELASTIC_STRETCH, true),
            shadowCasting = settingsManager.getBoolean(SettingKey.SHADOW_CASTING, true),
            contentAwareBlur = settingsManager.getBoolean(SettingKey.CONTENT_AWARE_BLUR, true),
            timeBasedThemes = settingsManager.getBoolean(SettingKey.TIME_BASED_THEMES, false),
            highContrastMode = settingsManager.getBoolean(SettingKey.HIGH_CONTRAST_MODE, false),
            iconPackIntegration = settingsManager.getBoolean(SettingKey.ICON_PACK_INTEGRATION, false),

            // ==================== Notifications & Detection ====================
            otpDetection = settingsManager.getBoolean(SettingKey.OTP_DETECTION, true),
            linkIntercept = settingsManager.getBoolean(SettingKey.LINK_INTERCEPT, true),
            translation = settingsManager.getBoolean(SettingKey.TRANSLATION, true),
            barcode = settingsManager.getBoolean(SettingKey.BARCODE, true),
            navigation = settingsManager.getBoolean(SettingKey.NAVIGATION, true),
            notificationCoalescing = settingsManager.getBoolean(SettingKey.NOTIFICATION_COALESCING, true),
            appPermissionChecker = settingsManager.getBoolean(SettingKey.APP_PERMISSION_CHECKER, true),

            // ==================== Gaming HUD ====================
            gamingHud = settingsManager.getBoolean(SettingKey.GAMING_HUD, true),
            showFps = settingsManager.getBoolean(SettingKey.SHOW_FPS, false),
            showCpuTemp = settingsManager.getBoolean(SettingKey.SHOW_CPU_TEMP, false),
            gamingDashboardOverlay = settingsManager.getBoolean(SettingKey.GAMING_DASHBOARD_OVERLAY, false),

            // ==================== Media & Audio ====================
            mediaArtworkBlur = settingsManager.getBoolean(SettingKey.MEDIA_ARTWORK_BLUR, true),
            waveformEnabled = settingsManager.getBoolean(SettingKey.WAVEFORM_ENABLED, true),
            ambientReactiveRing = settingsManager.getBoolean(SettingKey.AMBIENT_REACTIVE, true),
            audioSensitivity = settingsManager.getFloat(SettingKey.AUDIO_SENSITIVITY, 0.5f),
            bpmPulse = settingsManager.getBoolean(SettingKey.BPM_PULSE, true),
            liveMusicVisualizer = settingsManager.getBoolean(SettingKey.LIVE_MUSIC_VISUALIZER, false),
            nowPlaying = settingsManager.getBoolean(SettingKey.NOW_PLAYING, true),
            liveCaption = settingsManager.getBoolean(SettingKey.LIVE_CAPTION, false),
            voiceMemoTranscription = settingsManager.getBoolean(SettingKey.VOICE_MEMO_TRANSCRIPTION, true),

            // ==================== Haptics ====================
            hapticFeedback = settingsManager.getBoolean(SettingKey.HAPTIC_FEEDBACK, true),
            hapticIntensity = settingsManager.getFloat(SettingKey.HAPTIC_INTENSITY, 1f),
            ringCadenceVibration = settingsManager.getBoolean(SettingKey.RING_CADENCE_VIBRATION, true),
            hapticMorseAlerts = settingsManager.getBoolean(SettingKey.HAPTIC_MORSE_ALERTS, false),

            // ==================== Prediction & Smart Features ====================
            predictionTint = settingsManager.getBoolean(SettingKey.PREDICTION_TINT, true),
            predictiveActions = settingsManager.getBoolean(SettingKey.PREDICTIVE_ACTIONS, true),
            autoDismissDelay = settingsManager.getInt(SettingKey.AUTO_DISMISS_DELAY, 5),
            contextualSuggestions = settingsManager.getBoolean(SettingKey.CONTEXTUAL_SUGGESTIONS, true),
            gestureLearning = settingsManager.getBoolean(SettingKey.GESTURE_LEARNING, true),
            voiceTrigger = settingsManager.getBoolean(SettingKey.VOICE_TRIGGER, false),
            adaptiveBrightnessVolume = settingsManager.getBoolean(SettingKey.ADAPTIVE_BRIGHTNESS_VOLUME, false),
            appPredictionSuggestion = settingsManager.getBoolean(SettingKey.APP_PREDICTION_SUGGESTION, true),
            contextualRoutineLauncher = settingsManager.getBoolean(SettingKey.CONTEXTUAL_ROUTINE_LAUNCHER, true),

            // ==================== Cross-Device & Continuity ====================
            clipboardSync = settingsManager.getBoolean(SettingKey.CLIPBOARD_SYNC, false),
            universalControl = settingsManager.getBoolean(SettingKey.UNIVERSAL_CONTROL, false),
            quickNote = settingsManager.getBoolean(SettingKey.QUICK_NOTE, true),
            phoneToTabletHandoff = settingsManager.getBoolean(SettingKey.PHONE_TO_TABLET_HANDOFF, false),
            nearbyShareProgress = settingsManager.getBoolean(SettingKey.NEARBY_SHARE_PROGRESS, true),
            multiDeviceClipboard = settingsManager.getBoolean(SettingKey.MULTI_DEVICE_CLIPBOARD, false),
            wearOsRemote = settingsManager.getBoolean(SettingKey.WEAR_OS_REMOTE, false),
            airpodsPopup = settingsManager.getBoolean(SettingKey.AIRPODS_POPUP, true),
            airplayCastIndicator = settingsManager.getBoolean(SettingKey.AIRPLAY_CAST_INDICATOR, true),
            homePodControl = settingsManager.getBoolean(SettingKey.HOME_POD_CONTROL, false),

            // ==================== iOS-Inspired ====================
            liveActivitiesApi = settingsManager.getBoolean(SettingKey.LIVE_ACTIVITIES_API, true),
            focusFilterIntegration = settingsManager.getBoolean(SettingKey.FOCUS_FILTER_INTEGRATION, true),
            universalClipboardPreviews = settingsManager.getBoolean(SettingKey.UNIVERSAL_CLIPBOARD_PREVIEWS, true),
            alwaysOnDisplayCompanion = settingsManager.getBoolean(SettingKey.ALWAYS_ON_DISPLAY_COMPANION, true),
            faceIDPadlock = settingsManager.getBoolean(SettingKey.FACE_ID_PADLOCK, true),
            ringModeSwitch = settingsManager.getBoolean(SettingKey.RING_MODE_SWITCH, true),
            timerIntegration = settingsManager.getBoolean(SettingKey.TIMER_INTEGRATION, true),
            magsafeChargingAnimation = settingsManager.getBoolean(SettingKey.MAGSAFE_CHARGING_ANIMATION, true),
            proximityWake = settingsManager.getBoolean(SettingKey.PROXIMITY_WAKE, false),

            // ==================== Android Ecosystem ====================
            materialYouDynamicContrast = settingsManager.getBoolean(SettingKey.MATERIAL_YOU_DYNAMIC_CONTRAST, true),
            quickSettingsTile = settingsManager.getBoolean(SettingKey.QUICK_SETTINGS_TILE, true),
            digitalWellbeingIntegration = settingsManager.getBoolean(SettingKey.DIGITAL_WELLBEING_INTEGRATION, false),
            rootAdbFeatures = settingsManager.getBoolean(SettingKey.ROOT_ADB_FEATURES, false),

            // ==================== Accessibility ====================
            talkbackIntegration = settingsManager.getBoolean(SettingKey.TALKBACK_INTEGRATION, true),
            oneHandMode = settingsManager.getBoolean(SettingKey.ONE_HAND_MODE, false),
            dedicatedOneHandPlacement = settingsManager.getBoolean(SettingKey.DEDICATED_ONE_HAND_PLACEMENT, false),

            // ==================== Customisation ====================
            customPillAnimations = settingsManager.getString(SettingKey.CUSTOM_PILL_ANIMATIONS, null),
            thirdPartyWidgetApi = settingsManager.getBoolean(SettingKey.THIRD_PARTY_WIDGET_API, false),

            // ==================== Battery & Performance ====================
            batteryAwareAnimation = settingsManager.getBoolean(SettingKey.BATTERY_AWARE_ANIMATION, true),
            dozeModeOptimisation = settingsManager.getBoolean(SettingKey.DOZE_MODE_OPTIMISATION, true),
            quickPerformanceProfile = settingsManager.getBoolean(SettingKey.QUICK_PERFORMANCE_PROFILE, true),
            dataSaver = settingsManager.getBoolean(SettingKey.DATA_SAVER, false),

            // ==================== Gamification ====================
            islandStreaks = settingsManager.getBoolean(SettingKey.ISLAND_STREAKS, true),
            leaderboard = settingsManager.getBoolean(SettingKey.LEADERBOARD, false),
            exclusiveThemes = settingsManager.getBoolean(SettingKey.EXCLUSIVE_THEMES, false),
            achievementsEnabled = settingsManager.getBoolean(SettingKey.ACHIEVEMENTS_ENABLED, true),
            showAchievementBadge = settingsManager.getBoolean(SettingKey.ACHIEVEMENTS_DISPLAY, true),

            // ==================== Privacy & Security ====================
            clipboardCleaner = settingsManager.getBoolean(SettingKey.CLIPBOARD_CLEANER, true),
            vpnTorIndicator = settingsManager.getBoolean(SettingKey.VPN_TOR_INDICATOR, true),

            // ==================== Experimental ====================
            arIsland = settingsManager.getBoolean(SettingKey.AR_ISLAND, false),
            mindfulnessBreathPacer = settingsManager.getBoolean(SettingKey.MINDFULNESS_BREATH_PACER, false),
            morseCodeInput = settingsManager.getBoolean(SettingKey.MORSE_CODE_INPUT, false),
            multiUserProfileSwitching = settingsManager.getBoolean(SettingKey.MULTI_USER_PROFILE_SWITCHING, false),
            cryptoStockTicker = settingsManager.getBoolean(SettingKey.CRYPTO_STOCK_TICKER, false),

            // ==================== Developer Tools ====================
            adbCommandInjector = settingsManager.getBoolean(SettingKey.ADB_COMMAND_INJECTOR, false),
            taskerPlugin = settingsManager.getBoolean(SettingKey.TASKER_PLUGIN, true),
            logDebugOverlay = settingsManager.getBoolean(SettingKey.LOG_DEBUG_OVERLAY, false),
            openSourceSdk = settingsManager.getBoolean(SettingKey.OPEN_SOURCE_SDK, true),

            // ==================== Global Controls ====================
            islandEnabled = settingsManager.getBoolean(SettingKey.ISLAND_ENABLED, true),
            islandOnLockscreen = settingsManager.getBoolean(SettingKey.ISLAND_ON_LOCKSCREEN, true),
            lockscreenFeatures = settingsManager.getStringSet(SettingKey.FEATURES_ON_LOCKSCREEN, setOf("music", "notifications")),
            allowedMusicApps = settingsManager.getStringSet(SettingKey.ALLOWED_MUSIC_APPS, emptySet()),
            allowedNotificationApps = settingsManager.getStringSet(SettingKey.ALLOWED_NOTIFICATION_APPS, emptySet()),
            swipeLeftAction = settingsManager.getString(SettingKey.SWIPE_LEFT_ACTION, "dismiss") ?: "dismiss",
            swipeRightAction = settingsManager.getString(SettingKey.SWIPE_RIGHT_ACTION, "next_track") ?: "next_track",

            // ==================== Call & Communication ====================
            callScreenTranscript = settingsManager.getBoolean(SettingKey.CALL_SCREEN_TRANSCRIPT, true),
            silentRingModeSwitch = settingsManager.getBoolean(SettingKey.SILENT_RING_MODE_SWITCH, true),

            // ==================== Weather & Environment ====================
            weatherMoodRing = settingsManager.getBoolean(SettingKey.WEATHER_MOOD_RING, true),
            atAGlance = settingsManager.getBoolean(SettingKey.AT_A_GLANCE, true),

            // ==================== Split & Multi-Event ====================
            splitPillEnabled = settingsManager.getBoolean(SettingKey.SPLIT_PILL_ENABLED, true),
            mergeSimultaneousEvents = settingsManager.getBoolean(SettingKey.MERGE_SIMULTANEOUS_EVENTS, true),
            multiIslandStack = settingsManager.getBoolean(SettingKey.MULTI_ISLAND_STACK, false),

            // ==================== Continuity Camera ====================
            continuityCameraActions = settingsManager.getBoolean(SettingKey.CONTINUITY_CAMERA_ACTIONS, true),

            // ==================== Focus & DND ====================
            focusModePill = settingsManager.getBoolean(SettingKey.FOCUS_MODE_PILL, true)
        )
    }

    fun <T> updateSetting(key: SettingKey, value: T) {
        when (value) {
            is Boolean -> settingsManager.putBoolean(key, value)
            is Int -> settingsManager.putInt(key, value)
            is Float -> settingsManager.putFloat(key, value)
            is String -> settingsManager.putString(key, value)
            is Set<*> -> {
                @Suppress("UNCHECKED_CAST")
                settingsManager.putStringSet(key, value as Set<String>)
            }
        }
        loadAllSettings()  // refresh state from persistent storage
    }

    fun resetAll() {
        settingsManager.resetAll()
        loadAllSettings()
    }
}