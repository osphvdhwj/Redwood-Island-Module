package com.example.dynamicisland.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.example.dynamicisland.ipc.IslandIPCClient
import org.json.JSONObject

/**
 * THE DEFINITIVE SETTINGS ARCHITECTURE
 * Consolidated into one file to eliminate symbol resolution ambiguities.
 */

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

sealed class IconPack(val id: String) {
    object MaterialYou : IconPack("MATERIAL_YOU")
    object iOS : IconPack("IOS")
    object OxygenOS : IconPack("OXYGEN_OS")
    object OneUI : IconPack("ONE_UI")
    object AmoledCyberpunk : IconPack("AMOLED_CYBERPUNK")
    object CupertinoGlass : IconPack("CUPERTINO_GLASS")
    companion object {
        fun fromString(id: String): IconPack = when (id.uppercase()) {
            "IOS" -> iOS; "OXYGEN_OS" -> OxygenOS; "ONE_UI" -> OneUI
            "AMOLED_CYBERPUNK" -> AmoledCyberpunk; "CUPERTINO_GLASS" -> CupertinoGlass
            else -> MaterialYou
        }
    }
}

data class SettingsState(
    val designLanguage: DesignLanguage = DesignLanguage.MATERIAL_YOU,
    val liveBridgeEnabled: Boolean = false,
    val magneticEdgeDocking: Boolean = true,
    val dynamicColors: Boolean = true,
    val customAccentColor: Color = Color(0xFF6750A4),
    val blurIntensity: Float = 15f,
    val geminiAuraEnabled: Boolean = true,
    val rollingTypographyEnabled: Boolean = true,
    val aestheticStyle: AestheticStyle = AestheticStyle.GLASS,
    val monochromeIcons: Boolean = false,
    val enableMetaballTear: Boolean = true,
    val dynamicGradient: Boolean = true,
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
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val physicsStyle: PhysicsStyle = PhysicsStyle.APPLE,
    val contentTransitionStyle: ContentTransitionStyle = ContentTransitionStyle.SLIDE,
    val velocitySquishEnabled: Boolean = true,
    val inlineReplyEnabled: Boolean = true,
    val enableMaxWidgets: Boolean = true,
    val showVitalsRam: Boolean = true,
    val showVitalsCpu: Boolean = true,
    val showVitalsNet: Boolean = true,
    val showVitalsFps: Boolean = true,
    val showVitalsBatCycles: Boolean = true,
    val shortcutLayout: ShortcutLayout = ShortcutLayout.GRID,
    val assistBridgeEnabled: Boolean = false,
    val assistBridgeTarget: String = "com.brave.browser",
    val lensBridgeEnabled: Boolean = false,
    val lensBridgeTarget: String = "com.brave.browser",
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
    val islandEnabled: Boolean = true,
    val islandOnLockscreen: Boolean = true,
    val lockscreenFeatures: Set<String> = setOf("music", "notifications"),
    val allowedNotificationApps: Set<String> = emptySet(),
    val swipeLeftAction: String = "dismiss",
    val swipeRightAction: String = "next_track",
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
    val clipboardCleaner: Boolean = true,
    val privacyDotsEnabled: Boolean = false,
    val dozeModeOptimisation: Boolean = true,
    val otpDetection: Boolean = true,
    val linkIntercept: Boolean = true,
    val translation: Boolean = true,
    val barcode: Boolean = true,
    val navigation: Boolean = true,
    val notificationCoalescing: Boolean = true,
    val appPermissionChecker: Boolean = true,
    val gamingHud: Boolean = true,
    val showFps: Boolean = false,
    val showCpuTemp: Boolean = false,
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
    val hapticFeedback: Boolean = true,
    val hapticIntensity: Float = 1f,
    val ringCadenceVibration: Boolean = true,
    val hapticMorseAlerts: Boolean = false,
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
    val talkbackIntegration: Boolean = true,
    val proximityWake: Boolean = false,
    val timerIntegration: Boolean = true,
    val splitPillEnabled: Boolean = true,
    val iconPack: IconPack = IconPack.MaterialYou,
    val enableFreeformPortalAnim: Boolean = true,
    val timeBasedThemes: Boolean = false,
    val highContrastMode: Boolean = false,
    val iconPackIntegration: Boolean = false
)

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
    private val isSystemUI = context.packageName == "com.android.systemui"
    private val ipcClient by lazy { IslandIPCClient.get(context) }

    fun broadcastUpdate() {
        try {
            val intent = android.content.Intent("com.example.dynamicisland.RELOAD_PREFS")
            intent.addFlags(0x01000000)
            intent.setPackage("com.android.systemui")
            context.sendBroadcast(intent)
        } catch (e: Exception) {}
    }

    enum class SettingKey {
        DESIGN_LANGUAGE, DYNAMIC_COLORS, ACCENT_COLOR, BLUR_INTENSITY,
        PILL_RADIUS, ANIMATION_SPEED, SHOW_RING_IDLE, PILL_SHAPE,
        DYNAMIC_GRADIENT, GLOW_EFFECT, DOT_MODE, ELASTIC_STRETCH,
        SHADOW_CASTING, CONTENT_AWARE_BLUR, TIME_BASED_THEMES,
        HIGH_CONTRAST_MODE, ICON_PACK_INTEGRATION, AESTHETIC_STYLE,
        MONOCHROME_ICONS, ENABLE_METABALL_TEAR,
        LIVE_BRIDGE_ENABLED, MAGNETIC_EDGE_DOCKING,
        GEMINI_AURA_ENABLED, ROLLING_TYPOGRAPHY_ENABLED,
        PARSE_DELIVERY_NOTIFICATIONS, WARP_CHARGE_ANIMATION,
        BATTERY_AWARE_ANIMATION, NOW_PLAYING, MUSIC_VISUALIZER_STYLE,
        WAVEFORM_ENABLED, MEDIA_ARTWORK_BLUR, BPM_PULSE,
        AMBIENT_REACTIVE_RING, AMBIENT_REACTIVE,
        PHYSICS_STYLE, CONTENT_TRANSITION_STYLE, VELOCITY_SQUISH_ENABLED,
        INLINE_REPLY_ENABLED, ENABLE_MAX_WIDGETS,
        SHOW_VITALS_RAM, SHOW_VITALS_CPU, SHOW_VITALS_NET,
        SHOW_VITALS_FPS, SHOW_VITALS_BAT_CYCLES, SHORTCUT_LAYOUT,
        ASSIST_BRIDGE_ENABLED, ASSIST_BRIDGE_TARGET,
        LENS_BRIDGE_ENABLED, LENS_BRIDGE_TARGET,
        SMART_GESTURES_ENABLED, SMART_CALL_OVERRIDE,
        SMART_MEDIA_OVERRIDE, SMART_GAMING_OVERRIDE,
        PREDICTION_TINT, PREDICTIVE_ACTIONS, AUTO_DISMISS_DELAY,
        CONTEXTUAL_SUGGESTIONS, GESTURE_LEARNING,
        AI_CONFIDENCE_THRESHOLD, AI_REINFORCEMENT_RATE,
        ALLOW_MUSIC_MID, ALLOW_MUSIC_MAX, ALLOW_CHARGING_MINI,
        ALLOW_CHARGING_MID, ALLOW_NOTIF_MINI, ALLOW_NOTIF_MID,
        ALLOW_NOTIF_MAX, ALLOW_CALL_MID, ALLOW_CALL_MAX,
        ALLOW_TASK_MINI, ALLOW_TASK_MID,
        ISLAND_ENABLED, ISLAND_ON_LOCKSCREEN, LOCKSCREEN_FEATURES,
        ALLOWED_NOTIFICATION_APPS, SWIPE_LEFT_ACTION, SWIPE_RIGHT_ACTION,
        HIDE_ON_SCREENSHOT, HIDE_ON_SCREEN_RECORD, HIDE_ISLAND_PER_APP,
        ENABLE_FOCUS_MODE, PRODUCTIVE_APPS, ENABLE_LOW_LATENCY_MODE,
        ENABLE_CLIPBOARD_PAPERCLIP, CLIPBOARD_CLEANER, PRIVACY_DOTS_ENABLED,
        DOZE_MODE_OPTIMISATION, OTP_DETECTION, LINK_INTERCEPT, TRANSLATION,
        BARCODE, NAVIGATION, NOTIFICATION_COALESCING, APP_PERMISSION_CHECKER,
        GAMING_HUD, SHOW_FPS, SHOW_CPU_TEMP, WIFI_ALERT_DURATION,
        BT_ALERT_DURATION, HOTSPOT_ALERT_DURATION, DATA_ALERT_DURATION,
        RING_MEDIA_VISIBLE, RING_BATTERY_VISIBLE, RING_DATA_VISIBLE,
        INVISIBLE_RING_TOUCH_PASSTHROUGH, ANTI_BURN_IN_ENABLED, ANTI_BURN_IN_INTENSITY,
        HAPTIC_FEEDBACK, HAPTIC_INTENSITY, RING_CADENCE_VIBRATION,
        HAPTIC_MORSE_ALERTS, ROLE_CALLING_APP, ALLOWED_MUSIC_APPS,
        ALLOWED_MEDIA_APPS, ALLOWED_NOTES_APPS, CALL_STYLE,
        CHARGING_STYLE, BATTERY_STYLE, RING_PULSE_STYLE,
        AUTO_BACKUP_ENABLED, AUTO_BACKUP_FREQ_DAYS, STASH_STORAGE_PATH,
        TALKBACK_INTEGRATION, PROXIMITY_WAKE, TIMER_INTEGRATION,
        SPLIT_PILL_ENABLED, ICON_PACK, ENABLE_FREEFORM_PORTAL_ANIM
    }

    fun getBoolean(key: SettingKey, d: Boolean): Boolean = if (isSystemUI) ipcClient.getBoolean(key.name, d) else prefs.getBoolean(key.name, d)
    fun putBoolean(key: SettingKey, v: Boolean) { prefs.edit().putBoolean(key.name, v).apply(); ipcClient.putBoolean(key.name, v) }
    fun getInt(key: SettingKey, d: Int): Int = if (isSystemUI) ipcClient.getInt(key.name, d) else prefs.getInt(key.name, d)
    fun putInt(key: SettingKey, v: Int) { prefs.edit().putInt(key.name, v).apply(); ipcClient.putInt(key.name, v) }
    fun getFloat(key: SettingKey, d: Float): Float = if (isSystemUI) ipcClient.getFloat(key.name, d) else prefs.getFloat(key.name, d)
    fun putFloat(key: SettingKey, v: Float) { prefs.edit().putFloat(key.name, v).apply(); ipcClient.putFloat(key.name, v) }
    fun getString(key: SettingKey, d: String?): String? = if (isSystemUI) ipcClient.getString(key.name, d ?: "") else prefs.getString(key.name, d)
    fun getRawString(k: String, d: String): String = if (isSystemUI) ipcClient.getString(k, d) else prefs.getString(k, d) ?: d
    fun putString(key: SettingKey, v: String) { prefs.edit().putString(key.name, v).apply(); ipcClient.putString(key.name, v) }
    fun getStringSet(key: SettingKey, d: Set<String>): Set<String> {
        val raw = ipcClient.getString(key.name, "")
        return if (raw.isEmpty()) { if (isSystemUI) d else prefs.getStringSet(key.name, d) ?: d } else raw.split(",").toSet()
    }
    fun putStringSet(key: SettingKey, v: Set<String>) { prefs.edit().putStringSet(key.name, v).apply(); ipcClient.putString(key.name, v.joinToString(",")) }
    fun resetAll() { prefs.edit().clear().apply() }
    fun clearAiMemory(): Boolean = ipcClient.clearAiMemory()
    fun exportAiData(): String? = ipcClient.exportAiData()

    fun getSettingsState(): SettingsState {
        val packId = getString(SettingKey.ICON_PACK, "MATERIAL_YOU") ?: "MATERIAL_YOU"
        return SettingsState(
            designLanguage = try { DesignLanguage.valueOf(getString(SettingKey.DESIGN_LANGUAGE, "MATERIAL_YOU") ?: "MATERIAL_YOU") } catch(e: Exception) { DesignLanguage.MATERIAL_YOU },
            dynamicColors = getBoolean(SettingKey.DYNAMIC_COLORS, true),
            customAccentColor = Color(getInt(SettingKey.ACCENT_COLOR, 0xFF6750A4.toInt())),
            blurIntensity = getFloat(SettingKey.BLUR_INTENSITY, 15f),
            pillCornerRadius = getFloat(SettingKey.PILL_RADIUS, 100f),
            animationSpeed = try { AnimationSpeed.valueOf(getString(SettingKey.ANIMATION_SPEED, "NORMAL") ?: "NORMAL") } catch(e: Exception) { AnimationSpeed.NORMAL },
            showRingIdle = getBoolean(SettingKey.SHOW_RING_IDLE, true),
            pillShape = getString(SettingKey.PILL_SHAPE, "pill") ?: "pill",
            dynamicGradient = getBoolean(SettingKey.DYNAMIC_GRADIENT, true),
            glowEffect = getBoolean(SettingKey.GLOW_EFFECT, true),
            dotMode = getBoolean(SettingKey.DOT_MODE, false),
            elasticStretch = getBoolean(SettingKey.ELASTIC_STRETCH, true),
            shadowCasting = getBoolean(SettingKey.SHADOW_CASTING, true),
            contentAwareBlur = getBoolean(SettingKey.CONTENT_AWARE_BLUR, true),
            timeBasedThemes = getBoolean(SettingKey.TIME_BASED_THEMES, false),
            highContrastMode = getBoolean(SettingKey.HIGH_CONTRAST_MODE, false),
            iconPackIntegration = getBoolean(SettingKey.ICON_PACK_INTEGRATION, false),
            aestheticStyle = try { AestheticStyle.valueOf(getString(SettingKey.AESTHETIC_STYLE, "GLASS") ?: "GLASS") } catch(e: Exception) { AestheticStyle.GLASS },
            monochromeIcons = getBoolean(SettingKey.MONOCHROME_ICONS, false),
            enableMetaballTear = getBoolean(SettingKey.ENABLE_METABALL_TEAR, true),
            liveBridgeEnabled = getBoolean(SettingKey.LIVE_BRIDGE_ENABLED, false),
            magneticEdgeDocking = getBoolean(SettingKey.MAGNETIC_EDGE_DOCKING, true),
            geminiAuraEnabled = getBoolean(SettingKey.GEMINI_AURA_ENABLED, true),
            rollingTypographyEnabled = getBoolean(SettingKey.ROLLING_TYPOGRAPHY_ENABLED, true),
            parseDeliveryNotifications = getBoolean(SettingKey.PARSE_DELIVERY_NOTIFICATIONS, true),
            warpChargeAnimation = getBoolean(SettingKey.WARP_CHARGE_ANIMATION, true),
            batteryAwareAnimation = getBoolean(SettingKey.BATTERY_AWARE_ANIMATION, true),
            nowPlaying = getBoolean(SettingKey.NOW_PLAYING, true),
            musicVisualizerStyle = getString(SettingKey.MUSIC_VISUALIZER_STYLE, "NEURAL_CIRCLE") ?: "NEURAL_CIRCLE",
            waveformEnabled = getBoolean(SettingKey.WAVEFORM_ENABLED, true),
            mediaArtworkBlur = getBoolean(SettingKey.MEDIA_ARTWORK_BLUR, true),
            bpmPulse = getBoolean(SettingKey.BPM_PULSE, true),
            ambientReactiveRing = getBoolean(SettingKey.AMBIENT_REACTIVE_RING, true),
            ambientReactive = getBoolean(SettingKey.AMBIENT_REACTIVE, true),
            physicsStyle = try { PhysicsStyle.valueOf(getString(SettingKey.PHYSICS_STYLE, "APPLE") ?: "APPLE") } catch(e: Exception) { PhysicsStyle.APPLE },
            contentTransitionStyle = try { ContentTransitionStyle.valueOf(getString(SettingKey.CONTENT_TRANSITION_STYLE, "SLIDE") ?: "SLIDE") } catch(e: Exception) { ContentTransitionStyle.SLIDE },
            velocitySquishEnabled = getBoolean(SettingKey.VELOCITY_SQUISH_ENABLED, true),
            inlineReplyEnabled = getBoolean(SettingKey.INLINE_REPLY_ENABLED, true),
            enableMaxWidgets = getBoolean(SettingKey.ENABLE_MAX_WIDGETS, true),
            showVitalsRam = getBoolean(SettingKey.SHOW_VITALS_RAM, true),
            showVitalsCpu = getBoolean(SettingKey.SHOW_VITALS_CPU, true),
            showVitalsNet = getBoolean(SettingKey.SHOW_VITALS_NET, true),
            showVitalsFps = getBoolean(SettingKey.SHOW_VITALS_FPS, true),
            showVitalsBatCycles = getBoolean(SettingKey.SHOW_VITALS_BAT_CYCLES, true),
            shortcutLayout = try { ShortcutLayout.valueOf(getString(SettingKey.SHORTCUT_LAYOUT, "GRID") ?: "GRID") } catch(e: Exception) { ShortcutLayout.GRID },
            assistBridgeEnabled = getBoolean(SettingKey.ASSIST_BRIDGE_ENABLED, false),
            assistBridgeTarget = getString(SettingKey.ASSIST_BRIDGE_TARGET, "com.brave.browser") ?: "com.brave.browser",
            lensBridgeEnabled = getBoolean(SettingKey.LENS_BRIDGE_ENABLED, false),
            lensBridgeTarget = getString(SettingKey.LENS_BRIDGE_TARGET, "com.brave.browser") ?: "com.brave.browser",
            smartGesturesEnabled = getBoolean(SettingKey.SMART_GESTURES_ENABLED, true),
            smartCallOverride = getBoolean(SettingKey.SMART_CALL_OVERRIDE, true),
            smartMediaOverride = getBoolean(SettingKey.SMART_MEDIA_OVERRIDE, true),
            smartGamingOverride = getBoolean(SettingKey.SMART_GAMING_OVERRIDE, true),
            predictionTint = getBoolean(SettingKey.PREDICTION_TINT, true),
            predictiveActions = getBoolean(SettingKey.PREDICTIVE_ACTIONS, true),
            autoDismissDelay = getInt(SettingKey.AUTO_DISMISS_DELAY, 5),
            contextualSuggestions = getBoolean(SettingKey.CONTEXTUAL_SUGGESTIONS, true),
            gestureLearning = getBoolean(SettingKey.GESTURE_LEARNING, true),
            aiConfidenceThreshold = getInt(SettingKey.AI_CONFIDENCE_THRESHOLD, 10),
            aiReinforcementRate = getFloat(SettingKey.AI_REINFORCEMENT_RATE, 1.0f),
            allowMusicMid = getBoolean(SettingKey.ALLOW_MUSIC_MID, true),
            allowMusicMax = getBoolean(SettingKey.ALLOW_MUSIC_MAX, true),
            allowChargingMini = getBoolean(SettingKey.ALLOW_CHARGING_MINI, true),
            allowChargingMid = getBoolean(SettingKey.ALLOW_CHARGING_MID, true),
            allowNotifMini = getBoolean(SettingKey.ALLOW_NOTIF_MINI, true),
            allowNotifMid = getBoolean(SettingKey.ALLOW_NOTIF_MID, true),
            allowNotifMax = getBoolean(SettingKey.ALLOW_NOTIF_MAX, true),
            allowCallMid = getBoolean(SettingKey.ALLOW_CALL_MID, true),
            allowCallMax = getBoolean(SettingKey.ALLOW_CALL_MAX, true),
            allowTaskMini = getBoolean(SettingKey.ALLOW_TASK_MINI, true),
            allowTaskMid = getBoolean(SettingKey.ALLOW_TASK_MID, true),
            islandEnabled = getBoolean(SettingKey.ISLAND_ENABLED, true),
            islandOnLockscreen = getBoolean(SettingKey.ISLAND_ON_LOCKSCREEN, true),
            lockscreenFeatures = getStringSet(SettingKey.LOCKSCREEN_FEATURES, setOf("music", "notifications")),
            allowedNotificationApps = getStringSet(SettingKey.ALLOWED_NOTIFICATION_APPS, emptySet()),
            swipeLeftAction = getString(SettingKey.SWIPE_LEFT_ACTION, "dismiss") ?: "dismiss",
            swipeRightAction = getString(SettingKey.SWIPE_RIGHT_ACTION, "next_track") ?: "next_track",
            hideOnScreenshot = getBoolean(SettingKey.HIDE_ON_SCREENSHOT, true),
            hideOnScreenRecord = getBoolean(SettingKey.HIDE_ON_SCREEN_RECORD, true),
            hideIslandPerApp = getStringSet(SettingKey.HIDE_ISLAND_PER_APP, emptySet()),
            enableFocusMode = getBoolean(SettingKey.ENABLE_FOCUS_MODE, false),
            productiveApps = getStringSet(SettingKey.PRODUCTIVE_APPS, emptySet()),
            enableLowLatencyMode = getBoolean(SettingKey.ENABLE_LOW_LATENCY_MODE, false),
            enableClipboardPaperclip = getBoolean(SettingKey.ENABLE_CLIPBOARD_PAPERCLIP, true),
            clipboardCleaner = getBoolean(SettingKey.CLIPBOARD_CLEANER, true),
            privacyDotsEnabled = getBoolean(SettingKey.PRIVACY_DOTS_ENABLED, false),
            dozeModeOptimisation = getBoolean(SettingKey.DOZE_MODE_OPTIMISATION, true),
            otpDetection = getBoolean(SettingKey.OTP_DETECTION, true),
            linkIntercept = getBoolean(SettingKey.LINK_INTERCEPT, true),
            translation = getBoolean(SettingKey.TRANSLATION, true),
            barcode = getBoolean(SettingKey.BARCODE, true),
            navigation = getBoolean(SettingKey.NAVIGATION, true),
            notificationCoalescing = getBoolean(SettingKey.NOTIFICATION_COALESCING, true),
            appPermissionChecker = getBoolean(SettingKey.APP_PERMISSION_CHECKER, true),
            gamingHud = getBoolean(SettingKey.GAMING_HUD, true),
            showFps = getBoolean(SettingKey.SHOW_FPS, false),
            showCpuTemp = getBoolean(SettingKey.SHOW_CPU_TEMP, false),
            wifiAlertDuration = getInt(SettingKey.WIFI_ALERT_DURATION, 3),
            btAlertDuration = getInt(SettingKey.BT_ALERT_DURATION, 3),
            hotspotAlertDuration = getInt(SettingKey.HOTSPOT_ALERT_DURATION, 5),
            dataAlertDuration = getInt(SettingKey.DATA_ALERT_DURATION, 3),
            ringMediaVisible = getBoolean(SettingKey.RING_MEDIA_VISIBLE, true),
            ringBatteryVisible = getBoolean(SettingKey.RING_BATTERY_VISIBLE, true),
            ringDataVisible = getBoolean(SettingKey.RING_DATA_VISIBLE, true),
            invisibleRingTouchPassthrough = getBoolean(SettingKey.INVISIBLE_RING_TOUCH_PASSTHROUGH, true),
            antiBurnInEnabled = getBoolean(SettingKey.ANTI_BURN_IN_ENABLED, true),
            antiBurnInIntensity = getFloat(SettingKey.ANTI_BURN_IN_INTENSITY, 1.5f),
            hapticFeedback = getBoolean(SettingKey.HAPTIC_FEEDBACK, true),
            hapticIntensity = getFloat(SettingKey.HAPTIC_INTENSITY, 1f),
            ringCadenceVibration = getBoolean(SettingKey.RING_CADENCE_VIBRATION, true),
            hapticMorseAlerts = getBoolean(SettingKey.HAPTIC_MORSE_ALERTS, false),
            roleCallingApp = getString(SettingKey.ROLE_CALLING_APP, "") ?: "",
            allowedMusicApps = getStringSet(SettingKey.ALLOWED_MUSIC_APPS, emptySet()),
            allowedMediaApps = getStringSet(SettingKey.ALLOWED_MEDIA_APPS, emptySet()),
            allowedNotesApps = getStringSet(SettingKey.ALLOWED_NOTES_APPS, emptySet()),
            callStyle = try { CallStyle.valueOf(getString(SettingKey.CALL_STYLE, "IOS") ?: "IOS") } catch(e: Exception) { CallStyle.IOS },
            chargingStyle = try { ChargingStyle.valueOf(getString(SettingKey.CHARGING_STYLE, "RING") ?: "RING") } catch(e: Exception) { ChargingStyle.RING },
            batteryStyle = try { BatteryStyle.valueOf(getString(SettingKey.BATTERY_STYLE, "PILL") ?: "PILL") } catch(e: Exception) { BatteryStyle.PILL },
            ringPulseStyle = try { RingPulseStyle.valueOf(getString(SettingKey.RING_PULSE_STYLE, "BREATH") ?: "BREATH") } catch(e: Exception) { RingPulseStyle.BREATH },
            autoBackupEnabled = getBoolean(SettingKey.AUTO_BACKUP_ENABLED, false),
            autoBackupFreqDays = getInt(SettingKey.AUTO_BACKUP_FREQ_DAYS, 7),
            stashStoragePath = getString(SettingKey.STASH_STORAGE_PATH, "/sdcard/DynamicIsland/Archive") ?: "/sdcard/DynamicIsland/Archive",
            talkbackIntegration = getBoolean(SettingKey.TALKBACK_INTEGRATION, true),
            proximityWake = getBoolean(SettingKey.PROXIMITY_WAKE, false),
            timerIntegration = getBoolean(SettingKey.TIMER_INTEGRATION, true),
            splitPillEnabled = getBoolean(SettingKey.SPLIT_PILL_ENABLED, true),
            iconPack = IconPack.fromString(packId),
            freeformLaunchEnabled = getBoolean(SettingKey.FREEFORM_LAUNCH_ENABLED, true),
            freeformSmartGesture = getBoolean(SettingKey.FREEFORM_SMART_GESTURE, true),
            enableFreeformPortalAnim = getBoolean(SettingKey.ENABLE_FREEFORM_PORTAL_ANIM, true)
        )
    }
}
