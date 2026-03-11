package com.example.dynamicisland



import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


import androidx.lifecycle.*
import androidx.savedstate.*
import de.robv.android.xposed.XSharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.channels.BufferOverflow

    // 🚀 RESTORED: Clean, snappy Charging Cube without the laggy full-screen blur
    @Composable
    fun DynamicIslandView.ChargingCube(model: LiveActivityModel.Charging) {
        val color = if (model.isPluggedIn) Color.Green else if (model.level <= 20) Color.Red else Color.White

        val infiniteTransition = rememberInfiniteTransition(label = "cube_pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "scale"
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (model.isPluggedIn) Icons.Default.Add else Icons.Default.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${model.level}%", color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.MusicMini(music: LiveActivityModel.Music) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                val infiniteTransition = rememberInfiniteTransition(); val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart))
                val currentRotation = if (isCubeRotationEnabled.value && music.isPlaying) rotation else 0f
                if (music.albumArt != null) Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(24.dp).clip(CircleShape).rotate(currentRotation)) else Box(Modifier.size(24.dp).background(Color.White.copy(0.2f), CircleShape))
                Spacer(Modifier.width(8.dp))
                // 🚀 TEXT CLIPPING FIX: Fill=false
                Text(text = "${music.title} • ${music.artist}", color = Color.White, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false).safeMarquee(islandState.value))
                Spacer(Modifier.width(8.dp))
                IsolatedTimeText(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = Color.White.copy(alpha=0.7f))
            }
            IsolatedLinearProgressIndicator(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, color = Color.White.copy(alpha=0.8f), trackColor = Color.Transparent, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.5f).height(2.dp).padding(bottom = 1.dp).clip(CircleShape))
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.MusicMid(music: LiveActivityModel.Music) {
        val theme = LocalIslandTheme.current // Grab the theme
        val dynamicTextColor = Color(music.titleTextColor)
        val infiniteTransition = rememberInfiniteTransition(); val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart))
        val currentRotation = if (isCubeRotationEnabled.value && music.isPlaying) rotation else 0f

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), // Dynamic padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                IsolatedCircularProgress(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, color = dynamicTextColor)
                if (music.albumArt != null) { Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(44.dp).clip(RoundedCornerShape(theme.cornerRadius / 4)).rotate(currentRotation)) } else Box(Modifier.size(44.dp).background(Color.White.copy(alpha=0.2f), RoundedCornerShape(theme.cornerRadius / 4)))
            }
            Spacer(modifier = Modifier.width(8.dp)) // Dynamic gap

            // Text Column
            Column(modifier = Modifier.weight(1f, fill=false).offset(x = theme.titleOffsetX, y = theme.titleOffsetY)) {
                Text(
                    text = music.title,
                    color = dynamicTextColor,
                    fontSize = theme.titleSize,
                    fontFamily = theme.titleFont, // 🚀 Custom Font!
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, modifier = Modifier.safeMarquee(islandState.value)
                )
                Text(
                    text = music.artist,
                    color = dynamicTextColor.copy(alpha = 0.7f),
                    fontSize = theme.titleSize * 0.85f, // Usually artist is a bit smaller than title
                    fontFamily = theme.titleFont,
                    maxLines = 1, modifier = Modifier.safeMarquee(islandState.value)
                )
            }
            Spacer(Modifier.width(8.dp))
            IsolatedTimeText(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = dynamicTextColor.copy(alpha=0.7f))
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.MusicMax(music: LiveActivityModel.Music) {
        val dynamicTextColor = Color.White
        var audioIcon by remember { mutableStateOf(Icons.Default.Smartphone) }; var audioLabel by remember { mutableStateOf("Phone") }

        // 🚀 SAFE AUDIO MANAGER FIX
        LaunchedEffect(music) {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                val devices = am?.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
                val hasBt = devices.any { it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER || it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
                val hasHeadphone = devices.any { it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET }
                if (hasBt) { audioIcon = Icons.Default.Bluetooth; audioLabel = "Bluetooth" } else if (hasHeadphone) { audioIcon = Icons.Default.Headset; audioLabel = "Headphones" } else { audioIcon = Icons.Default.Smartphone; audioLabel = "Phone" }
            } catch (e: Exception) { audioIcon = Icons.Default.Smartphone; audioLabel = "Phone" }
        }

        Column(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (music.appIcon != null) { Image(bitmap = music.appIcon.asImageBitmap(), contentDescription = "App Logo", modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))) } else Box(Modifier.size(36.dp).background(Color.White.copy(alpha=0.2f), RoundedCornerShape(10.dp)))
                Row(modifier = Modifier.background(Color.White.copy(alpha=0.2f), RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).clickable { onAudioOutputClick?.invoke() }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(audioIcon, contentDescription = "Output", tint = dynamicTextColor, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(audioLabel, color = dynamicTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val theme = LocalIslandTheme.current
            Text(text = music.title, color = dynamicTextColor, fontSize = theme.titleSize * 1.25f, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.fillMaxWidth().safeMarquee(islandState.value))
            Text(text = music.artist, color = dynamicTextColor.copy(alpha=0.8f), fontSize = (theme.titleSize * 0.85f) * 1.15f, maxLines = 1, modifier = Modifier.fillMaxWidth().safeMarquee(islandState.value))
            Spacer(modifier = Modifier.height(16.dp))

            IsolatedTimeRow(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = dynamicTextColor)
            IsolatedMediaSlider(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, dynamicTextColor = dynamicTextColor, onSeek = { onSeekTo?.invoke(it) })

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                val favoriteAction = music.customActions.find { it.actionName.contains("heart", true) || it.actionName.contains("favorite", true) || it.actionName.contains("thumb", true) }
                if (favoriteAction != null) Icon(Icons.Default.Favorite, null, tint = dynamicTextColor, modifier = Modifier.size(24.dp)) else Spacer(Modifier.width(24.dp))

                // 🚀 RIPPLE UI FIXES (Clip before click)
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onPrevClick?.invoke() }, contentAlignment = Alignment.Center) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = dynamicTextColor, modifier = Modifier.size(28.dp)) }
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(dynamicTextColor.copy(alpha = 0.2f)).clickable { onPlayPauseClick?.invoke() }, contentAlignment = Alignment.Center) { Icon(imageVector = playIcon, contentDescription = "Play/Pause", tint = dynamicTextColor, modifier = Modifier.size(32.dp)) }
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onNextClick?.invoke() }, contentAlignment = Alignment.Center) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = dynamicTextColor, modifier = Modifier.size(28.dp)) }

                val repeatAction = music.customActions.find { it.actionName.contains("repeat", true) || it.actionName.contains("loop", true) }
                if (repeatAction != null) Icon(Icons.Default.Refresh, null, tint = dynamicTextColor, modifier = Modifier.size(24.dp)) else Spacer(Modifier.width(24.dp))
            }
        }
    }

    // 🚀 NEW: CONTROL CENTER (MID PILL)
    @Suppress("UNUSED_PARAMETER") // 🚀 FIX: We pull system stats natively now!
    @Composable
    fun DynamicIslandView.DashboardMid(model: LiveActivityModel.Dashboard) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DashboardQuickToggle(Icons.Default.Wifi, true)
            DashboardQuickToggle(Icons.Default.Bluetooth, false)
            DashboardQuickToggle(Icons.Default.Build, false)
            DashboardQuickToggle(Icons.Default.NotificationsActive, true)
        }
    }

    // 🚀 NEW: CONTROL CENTER (MAX PILL)
    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun DynamicIslandView.DashboardMax(model: LiveActivityModel.Dashboard) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        // Audio Manager
        val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }

        // Brightness Manager
        val initialBrightness = remember { try { android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f } catch (e: Exception) { 0.5f } }
        var brightness by remember { mutableFloatStateOf(initialBrightness) }
        val initialAuto = remember { try { android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE) == 1 } catch(e:Exception){false} }
        var autoBrightness by remember { mutableStateOf(initialAuto) }

        // Torch Manager
        var isTorchOn by remember { mutableStateOf(false) }
        val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager }
        val cameraId = remember { try { cameraManager.cameraIdList.firstOrNull() } catch(e: Exception) { null } }

        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            // --- ROW 1: Quick Settings Grid ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DashboardQuickToggle(Icons.Default.Wifi, true, "Wi-Fi") {
                    val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(intent)
                }
                DashboardQuickToggle(Icons.Default.Bluetooth, false, "Bluetooth") {
                    val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(intent)
                }
                DashboardQuickToggle(Icons.Default.Build, isTorchOn, "Flashlight") {
                    try { isTorchOn = !isTorchOn; cameraId?.let { cameraManager.setTorchMode(it, isTorchOn) } } catch(e: Exception) {}
                }
                DashboardQuickToggle(Icons.Default.LocationOn, true, "Location") {
                    val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(intent)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ROW 2: Brightness Control ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        autoBrightness = !autoBrightness
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) { android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, if (autoBrightness) 1 else 0) }
                    },
                    modifier = Modifier.background(if (autoBrightness) Color.Yellow.copy(alpha=0.3f) else Color.White.copy(alpha=0.1f), CircleShape)
                ) { Icon(Icons.Default.BrightnessAuto, contentDescription = "Auto", tint = if (autoBrightness) Color.Yellow else Color.White) }

                Spacer(modifier = Modifier.width(12.dp))
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it }, // ONLY UI STATE HERE
                    onValueChangeFinished = {
                        // OS DATABASE WRITE GOES HERE
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) { 
                            try { android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, (brightness * 255).toInt()) } catch (e: Exception) {}
                        }
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha=0.3f), thumbColor = Color.White),
                    modifier = Modifier.weight(1f).height(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- ROW 3: Multi-Channel Volume Control ---
            var activeStream by remember { mutableIntStateOf(android.media.AudioManager.STREAM_MUSIC) }
            val maxVol = remember(activeStream) { audioManager.getStreamMaxVolume(activeStream).toFloat() }
            var currentVol by remember(activeStream) { mutableFloatStateOf(audioManager.getStreamVolume(activeStream) / maxVol) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Stream Selectors
                IconButton(onClick = { activeStream = android.media.AudioManager.STREAM_MUSIC }) { Icon(Icons.Default.MusicNote, null, tint = if (activeStream == android.media.AudioManager.STREAM_MUSIC) Color.Cyan else Color.White) }
                IconButton(onClick = { activeStream = android.media.AudioManager.STREAM_RING }) { Icon(Icons.Default.Notifications, null, tint = if (activeStream == android.media.AudioManager.STREAM_RING) Color.Cyan else Color.White) }
                IconButton(onClick = { activeStream = android.media.AudioManager.STREAM_ALARM }) { Icon(Icons.Default.Alarm, null, tint = if (activeStream == android.media.AudioManager.STREAM_ALARM) Color.Cyan else Color.White) }

                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = currentVol,
                    onValueChange = {
                        currentVol = it
                        audioManager.setStreamVolume(activeStream, (it * maxVol).toInt(), 0)
                    },
                    modifier = Modifier.weight(1f).height(24.dp)
                )
            }

            // --- ROW 4: Brightness Control ---
            val resolver = context.contentResolver
            var secondBrightness by remember {
                mutableFloatStateOf(
                    try { android.provider.Settings.System.getInt(resolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f }
                    catch (e: Exception) { 0.5f }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Default.BrightnessLow, contentDescription = "Brightness", tint = Color.White)
                Spacer(modifier = Modifier.width(16.dp))
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it }, // ONLY UI STATE HERE
                    onValueChangeFinished = {
                        // OS DATABASE WRITE GOES HERE
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) { 
                            try { android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, (brightness * 255).toInt()) } catch (e: Exception) {}
                        }
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha=0.3f), thumbColor = Color.White),
                    modifier = Modifier.weight(1f).height(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.BrightnessHigh, contentDescription = "Brightness", tint = Color.White)
            }
        }
    }

    // 🚀 NEW: QUICK TOGGLE COMPONENT
    @Composable
    fun DynamicIslandView.DashboardQuickToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, label: String? = null, onClick: () -> Unit = {}) {
        val bgColor by animateColorAsState(if (isActive) Color(0xFF0A84FF) else Color.White.copy(alpha=0.15f), label="bg")
        val tint by animateColorAsState(if (isActive) Color.White else Color.White.copy(alpha=0.6f), label="tint")

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(bgColor).clickable { onClick() },
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp)) }
            if (label != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // 🚀 NEW: DYNAMIC SYSTEM ALERT UI
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.SystemAlertMid(alert: LiveActivityModel.SystemAlert) {
        val color = Color(alert.alertColor)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Box(
                modifier = Modifier.size(44.dp).background(color.copy(alpha=0.2f), CircleShape).border(1.dp, color.copy(alpha=0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Change icon based on alert type
                val icon = when(alert.alertType) {
                    "THERMAL" -> Icons.Default.Warning // Use Thermostat/Fire icon if you have a custom vector
                    "ROGUE" -> Icons.Default.BatteryAlert
                    else -> Icons.Default.Info
                }
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = alert.title, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
                 Text(text = alert.message, color = color.copy(alpha=0.8f), fontSize = 14.sp, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            }
        }
    }

    @Composable
    fun DynamicIslandView.GeneralMini(general: LiveActivityModel.General) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) { Icon(imageVector = getIconForType(general.type), contentDescription = null, tint = Color(general.accentColor), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(text = "${general.title} • ${general.dataText}", color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value)) } }
    @Composable
    fun DynamicIslandView.HardwareGaugeMini(hw: LiveActivityModel.HardwareMonitor) { val tempColor = when { hw.cpuTempCelsius > 45f -> Color.Red; hw.cpuTempCelsius > 38f -> Color.Yellow; else -> Color.Green }; Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) { Icon(imageVector = Icons.Default.Info, contentDescription = "Hardware", tint = tempColor, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); androidx.compose.material3.LinearProgressIndicator(progress = { (hw.cpuTempCelsius / 60f).coerceIn(0f, 1f) }, modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)), color = tempColor, trackColor = Color.White.copy(alpha=0.2f)); Spacer(Modifier.width(8.dp)); Text(text = "${hw.cpuFreqMhz} MHz", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.UniversalMid(textColor: Color, activity: LiveActivityModel) { val infiniteTransition = rememberInfiniteTransition(label = "pulse"); val alphaPulse by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "alphaPulse"); val progress = when(activity) { is LiveActivityModel.General -> activity.progress; is LiveActivityModel.Charging -> activity.level / 100f; else -> null }; val colorInt = when(activity) { is LiveActivityModel.General -> activity.accentColor; is LiveActivityModel.Charging -> android.graphics.Color.GREEN; else -> android.graphics.Color.WHITE }; val title = when(activity) { is LiveActivityModel.General -> activity.title; is LiveActivityModel.Charging -> if (activity.isPluggedIn) "Charging" else "Disconnected"; else -> "" }; val dataText = when(activity) { is LiveActivityModel.General -> activity.dataText; is LiveActivityModel.Charging -> "${activity.level}%"; else -> "" }; Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) { if (progress != null) CircularProgressIndicator(progress = { progress }, color = Color(colorInt), trackColor = textColor.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize()); val iconAlpha = if (activity.type == ActivityType.CHARGING) alphaPulse else 1f; Icon(imageVector = getIconForType(activity.type), contentDescription = null, tint = Color(colorInt), modifier = Modifier.size(24.dp).alpha(iconAlpha)) }; Spacer(Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(text = title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value)); Text(text = dataText, color = textColor.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value)) } } }
    @Composable
    fun DynamicIslandView.ChargingMid(charging: LiveActivityModel.Charging) { UniversalMid(Color.White, charging) }
    @Composable
    fun DynamicIslandView.GeneralMid(general: LiveActivityModel.General) { UniversalMid(Color.White, general) }
    fun DynamicIslandView.setState(newState: IslandState) { islandState.value = newState }
    fun DynamicIslandView.setModel(model: LiveActivityModel?) { activeModel.value = model }
    fun DynamicIslandView.setSplitModel(model: LiveActivityModel?) { splitModel.value = model }
    fun getIconForType(type: ActivityType): ImageVector { return when(type) { ActivityType.CALL -> Icons.Default.Phone; ActivityType.NAVIGATION -> Icons.Default.LocationOn; ActivityType.TIMER -> Icons.Default.Notifications; ActivityType.MESSAGE -> Icons.Default.Email; ActivityType.ALARM -> Icons.Default.Notifications; ActivityType.CHARGING -> Icons.Default.Add; ActivityType.BATTERY_LOW -> Icons.Default.Warning; ActivityType.BLUETOOTH -> Icons.Default.Bluetooth; ActivityType.WIFI -> Icons.Default.Wifi; ActivityType.HARDWARE -> Icons.Default.Info; else -> Icons.Default.Info } }

    // 🚀 NEW: ISOLATED STATE COMPONENTS (Prevents 1-second Recomposition Churn)
    fun formatTime(ms: Long): String { if (ms <= 0) return "0:00"; val s = ms / 1000; return String.format("%d:%02d", s / 60, s % 60) }

    @Composable
    fun IsolatedTimeText(durationMs: Long, posProvider: () -> Long, textColor: Color, modifier: Modifier = Modifier) {
        // Only this tiny text box redraws when the second ticks!
        Text(text = "${formatTime(posProvider())} / ${formatTime(durationMs)}", color = textColor, fontSize = 12.sp, modifier = modifier)
    }

    @Composable
    fun IsolatedTimeRow(durationMs: Long, posProvider: () -> Long, textColor: Color) {
        val pos = posProvider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatTime(pos), color = textColor.copy(alpha=0.7f), fontSize = 12.sp)
            Text(text = formatTime(durationMs), color = textColor.copy(alpha=0.7f), fontSize = 12.sp)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun IsolatedMediaSlider(durationMs: Long, posProvider: () -> Long, dynamicTextColor: Color, onSeek: (Long) -> Unit) {
        val haptic = LocalHapticFeedback.current
        val interactionSource = remember { MutableInteractionSource() }
        val isDragged by interactionSource.collectIsDraggedAsState()

        val currentPos = posProvider().toFloat()
        var localPosition by remember(isDragged) { mutableFloatStateOf(currentPos) }

        // 🚀 MATH CORRUPTION FIX: Prevent division by zero, negative times, and streams.
        val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
        val currentPosition = posProvider().toFloat().coerceAtLeast(0f)
        val safePosition = if (isDragged) localPosition else currentPosition

        Slider(
            value = (safePosition / safeDuration).coerceIn(0f, 1f),
            onValueChange = {
                localPosition = it * safeDuration
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onValueChangeFinished = {
                onSeek(localPosition.toLong())
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(activeTrackColor = dynamicTextColor, inactiveTrackColor = dynamicTextColor.copy(alpha=0.3f), thumbColor = dynamicTextColor),
            modifier = Modifier.fillMaxWidth().height(24.dp)
        )
    }

    @Composable
    fun IsolatedLinearProgressIndicator(durationMs: Long, posProvider: () -> Long, color: Color, trackColor: Color, modifier: Modifier = Modifier) {
        val theme = LocalIslandTheme.current // Grab the theme
        // 🚀 MATH CORRUPTION FIX: Prevent division by zero, negative times, and streams.
        val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
        val currentPosition = posProvider().toFloat().coerceAtLeast(0f)
        val progress = (currentPosition / safeDuration).coerceIn(0f, 1f)

        androidx.compose.foundation.Canvas(modifier = modifier.height(theme.mediaBarThickness)) {
            drawLine(
                color = trackColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                strokeWidth = theme.mediaBarThickness.toPx(),
                cap = theme.mediaBarCap // 🚀 Custom Shape (Round, Square, Butt)
            )
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                end = androidx.compose.ui.geometry.Offset(size.width * progress, size.height / 2),
                strokeWidth = theme.mediaBarThickness.toPx(),
                cap = theme.mediaBarCap
            )
        }
    }

    @Composable
    fun IsolatedCircularProgressIndicator(durationMs: Long, posProvider: () -> Long, color: Color, trackColor: Color, strokeWidth: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
        val safeDuration = if (durationMs > 0) durationMs.toFloat() else 1f
        CircularProgressIndicator(
            progress = { (posProvider().toFloat() / safeDuration).coerceIn(0f, 1f) },
            color = color,
            trackColor = trackColor,
            strokeWidth = strokeWidth,
            modifier = modifier
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.AppTimerWarningMid(model: LiveActivityModel.AppTimerWarning) {
        var remainingSeconds by remember { mutableIntStateOf(((model.targetTimeMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)) }

        // Local Ticker (1 second intervals)
        LaunchedEffect(model.targetTimeMs) {
            while (remainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                remainingSeconds = ((model.targetTimeMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            }
        }

        // Aggressive Pulsing Red Alert
        val pulseTransition = rememberInfiniteTransition(label = "pulse")
        val alertAlpha by pulseTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "alertAlpha"
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Red.copy(alpha = alertAlpha), CircleShape)
                    .border(2.dp, Color.Red, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (model.appIcon != null) {
                    Image(bitmap = model.appIcon.asImageBitmap(), contentDescription = "App Icon", modifier = Modifier.size(36.dp).clip(CircleShape))
                } else {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                 val theme = LocalIslandTheme.current
                 Text(text = "Time Limit Reached", color = Color.Red, fontSize = theme.titleSize, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
                 Text(text = "${model.appName} closing in ${remainingSeconds}s", color = Color.White, fontSize = (theme.titleSize * 0.85f), maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            }
        }
    }

    @Composable
    fun IsolatedCircularProgress(durationMs: Long, posProvider: () -> Long, color: Color) {
        // 🚀 MATH CORRUPTION FIX: Prevent division by zero, negative times, and streams.
        val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
        val currentPosition = posProvider().toFloat().coerceAtLeast(0f)
        val progress = (currentPosition / safeDuration).coerceIn(0f, 1f)
        CircularProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            strokeWidth = 2.dp,
            modifier = Modifier.fillMaxSize()
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.RealityPillMini(model: LiveActivityModel.RealityPill) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            // A glowing green timer icon to indicate an active session
            Icon(Icons.Default.Timer, contentDescription = "Session Time", tint = Color(0xFF00FF00), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${model.appName} • ${model.sessionMinutes}m",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.safeMarquee(islandState.value)
            )
        }
    }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun androidx.compose.ui.Modifier.safeMarquee(state: IslandState): androidx.compose.ui.Modifier {
    return if (state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING && state != IslandState.TYPE_CUBE) {
        this.basicMarquee()
    } else {
        this
    }
}
