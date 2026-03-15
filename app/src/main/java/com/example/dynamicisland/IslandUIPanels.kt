package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput 
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import de.robv.android.xposed.XSharedPreferences


    // 🚀 NEW: The Master Micro-Interaction Engine
    @Composable
    fun InteractiveIconButton(icon: ImageVector, tint: Color, baseSize: Dp, bgAlpha: Float = 0f, onClick: () -> Unit) {
        val theme = LocalIslandTheme.current
        var isClicked by remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current

        LaunchedEffect(isClicked) {
            if (isClicked) {
                kotlinx.coroutines.delay(if(theme.actionAnimType == "CHECKMARK") 1000 else 300)
                isClicked = false
            }
        }

        val scale by animateFloatAsState(if (isClicked && theme.actionAnimType == "BOUNCE") 1.3f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label="scale")
        val alpha by animateFloatAsState(if (isClicked && theme.actionAnimType == "PULSE") 0.3f else 1f, tween(150), label="alpha")
        val currentIcon = if (isClicked && theme.actionAnimType == "CHECKMARK") Icons.Default.Check else icon
        val currentTint = if (isClicked && theme.actionAnimType == "CHECKMARK") Color.Green else tint

        Box(
            modifier = Modifier
                .size(baseSize)
                .clip(RoundedCornerShape(theme.buttonCornerRadius))
                .background(currentTint.copy(alpha = bgAlpha))
                .clickable { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isClicked = true
                    onClick() 
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = currentIcon,
                contentDescription = null,
                tint = currentTint.copy(alpha = alpha),
                modifier = Modifier.size(baseSize * 0.55f).graphicsLayer { scaleX = scale; scaleY = scale }
            )
        }
    }

    @Composable
    fun DynamicIslandView.ChargingCube(model: LiveActivityModel.Charging) {
        val color = if (model.isPluggedIn) Color.Green else if (model.level <= 20) Color.Red else Color.White
        val infiniteTransition = rememberInfiniteTransition(label = "cube_pulse")
        val pulseScale by infiniteTransition.animateFloat(initialValue = 0.85f, targetValue = 1.15f, animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector = if (model.isPluggedIn) Icons.Default.Add else Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(36.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale })
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
                Text(text = "${music.title} • ${music.artist}", color = Color.White, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false).safeMarquee(islandState.value))
                Spacer(Modifier.width(8.dp))
                IsolatedTimeText(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = Color.White.copy(alpha=0.7f))
            }
            IsolatedLinearProgressIndicator(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, color = Color.White.copy(alpha=0.8f), trackColor = Color.White.copy(alpha=0.2f), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.5f).height(2.dp).padding(bottom = 1.dp).clip(CircleShape))
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.MusicMid(music: LiveActivityModel.Music) {
        val theme = LocalIslandTheme.current
        val dynamicTextColor = Color(music.titleTextColor)
        val infiniteTransition = rememberInfiniteTransition(); val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart))
        val currentRotation = if (isCubeRotationEnabled.value && music.isPlaying) rotation else 0f

        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                IsolatedCircularProgress(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, color = dynamicTextColor)
                if (music.albumArt != null) { Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(44.dp).clip(RoundedCornerShape(theme.cornerRadius / 4)).rotate(currentRotation)) } else Box(Modifier.size(44.dp).background(Color.White.copy(alpha=0.2f), RoundedCornerShape(theme.cornerRadius / 4)))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f, fill=false).offset(x = theme.titleOffsetX, y = theme.titleOffsetY)) {
                Text(text = music.title, color = dynamicTextColor, fontSize = theme.titleSize, fontFamily = theme.titleFont, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
                Text(text = music.artist, color = dynamicTextColor.copy(alpha = 0.7f), fontSize = theme.titleSize * 0.85f, fontFamily = theme.titleFont, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            }
            Spacer(Modifier.width(8.dp))
            IsolatedTimeText(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = dynamicTextColor.copy(alpha=0.7f))
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.MusicMax(music: LiveActivityModel.Music) {
        val dynamicTextColor = Color.White
        val theme = LocalIslandTheme.current
        var audioIcon by remember { mutableStateOf(Icons.Default.Smartphone) }; var audioLabel by remember { mutableStateOf("Phone") }

        LaunchedEffect(music) {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                val devices = am?.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
                val hasBt = devices.any { it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER || it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
                val hasHeadphone = devices.any { it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET }
                if (hasBt) { audioIcon = Icons.Default.Bluetooth; audioLabel = "Bluetooth" } else if (hasHeadphone) { audioIcon = Icons.Default.Headset; audioLabel = "Headphones" } else { audioIcon = Icons.Default.Smartphone; audioLabel = "Phone" }
            } catch (e: Exception) { audioIcon = Icons.Default.Smartphone; audioLabel = "Phone" }
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (music.appIcon != null) { Image(bitmap = music.appIcon.asImageBitmap(), contentDescription = "App Logo", modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))) } else Box(Modifier.size(36.dp).background(Color.White.copy(alpha=0.2f), RoundedCornerShape(10.dp)))
                Row(modifier = Modifier.background(Color.White.copy(alpha=0.2f), RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).clickable { onAudioOutputClick?.invoke() }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(audioIcon, contentDescription = "Output", tint = dynamicTextColor, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(audioLabel, color = dynamicTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.weight(0.5f))
            
            Text(text = music.title, color = dynamicTextColor, fontSize = theme.titleSize * 1.25f, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.fillMaxWidth().safeMarquee(islandState.value))
            Text(text = music.artist, color = dynamicTextColor.copy(alpha=0.8f), fontSize = (theme.titleSize * 0.85f) * 1.15f, maxLines = 1, modifier = Modifier.fillMaxWidth().safeMarquee(islandState.value))
            
            Spacer(modifier = Modifier.weight(0.5f))

            IsolatedTimeRow(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = dynamicTextColor)
            IsolatedMediaSlider(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, dynamicTextColor = dynamicTextColor, onSeek = { onSeekTo?.invoke(it) })

            Spacer(modifier = Modifier.weight(1f))
            
            // 🚀 UPDATED: Perfectly spaced and customized Media Buttons using new Config
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(theme.buttonSpacing, Alignment.CenterHorizontally)) {
                val favoriteAction = music.customActions.find { it.actionName.contains("heart", true) || it.actionName.contains("favorite", true) || it.actionName.contains("thumb", true) }
                if (favoriteAction != null) {
                    InteractiveIconButton(icon = Icons.Default.Favorite, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0f) {}
                } else {
                    Spacer(Modifier.width(theme.buttonSize))
                }
                
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0f) { onPrevClick?.invoke() }
                
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                InteractiveIconButton(icon = playIcon, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0.2f) { onPlayPauseClick?.invoke() }
                
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowForward, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0f) { onNextClick?.invoke() }

                val repeatAction = music.customActions.find { it.actionName.contains("repeat", true) || it.actionName.contains("loop", true) }
                if (repeatAction != null) {
                    InteractiveIconButton(icon = Icons.Default.Refresh, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0f) {}
                } else {
                    Spacer(Modifier.width(theme.buttonSize))
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER") 
    @Composable
    fun DynamicIslandView.DashboardMid(model: LiveActivityModel.Dashboard) {
        val theme = LocalIslandTheme.current
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            DashboardQuickToggle(Icons.Default.Wifi, true)
            DashboardQuickToggle(Icons.Default.Bluetooth, false)
            DashboardQuickToggle(Icons.Default.Build, false)
            DashboardQuickToggle(Icons.Default.NotificationsActive, true)
        }
    }

    @Composable
    fun AppleControlCenterSlider(value: Float, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit = {}, activeColor: Color, icon: ImageVector) {
        var width by remember { mutableIntStateOf(1) }
        Box(modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha=0.15f))
            .onGloballyPositioned { width = it.size.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onValueChangeFinished() }
                ) { change, _ ->
                    change.consume()
                    onValueChange((change.position.x / width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset -> onValueChange((offset.x / width).coerceIn(0f, 1f)); onValueChangeFinished() }
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction = value.coerceIn(0f, 1f)).background(activeColor))
            Icon(icon, null, modifier = Modifier.align(Alignment.CenterStart).padding(start=14.dp).size(22.dp), tint = if (value > 0.15f) Color.Black else Color.White)
        }
    }

    @Suppress("UNUSED_PARAMETER", "DEPRECATION")
    @Composable
    fun DynamicIslandView.DashboardMax(model: LiveActivityModel.Dashboard) {
        val context = LocalContext.current
        val theme = LocalIslandTheme.current
        val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
        val wifiManager = remember { try { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager } catch(e: Throwable) { null } }
        val btAdapter = remember { try { (context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter } catch(e: Throwable) { null } }

        val initialBrightness = remember { try { android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f } catch (e: Throwable) { 0.5f } }
        var brightness by remember { mutableFloatStateOf(initialBrightness) }

        LaunchedEffect(brightness) { kotlinx.coroutines.delay(100); try { android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, (brightness * 255).toInt()) } catch (e: Throwable) {} }

        var isWifiOn by remember { mutableStateOf(try { wifiManager?.isWifiEnabled == true } catch(e: Throwable) { false }) }
        var isBtOn by remember { mutableStateOf(try { btAdapter?.isEnabled == true } catch(e: Throwable) { false }) }
        var isTorchOn by remember { mutableStateOf(false) }
        val cameraManager = remember { try { context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager } catch(e: Throwable) { null } }
        val cameraId = remember { try { cameraManager?.cameraIdList?.firstOrNull() } catch(e: Throwable) { null } }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
            
            // 🚀 UPDATED: Perfectly spaced QS Grid using custom config gaps
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(theme.buttonSpacing, Alignment.CenterHorizontally)) {
                val activeQS = qsTiles.filter { it.isNotEmpty() && it != "None" }
                if (activeQS.isEmpty()) {
                    DashboardQuickToggle(Icons.Default.Settings, true, "Settings") { try { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(e: Throwable) {} }
                } else {
                    activeQS.forEach { tile ->
                        when (tile) {
                            "WiFi" -> DashboardQuickToggle(Icons.Default.Wifi, isWifiOn, "Wi-Fi") { try { val newState = !isWifiOn; wifiManager?.isWifiEnabled = newState; isWifiOn = newState } catch(e: Throwable) { try { context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(ex: Throwable) {} } }
                            "Bluetooth" -> DashboardQuickToggle(Icons.Default.Bluetooth, isBtOn, "Bluetooth") { try { val newState = !isBtOn; @SuppressLint("MissingPermission") if (newState) btAdapter?.enable() else btAdapter?.disable(); isBtOn = newState } catch(e: Throwable) { try { context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(ex: Throwable) {} } }
                            "Torch" -> DashboardQuickToggle(Icons.Default.FlashlightOn, isTorchOn, "Torch") { try { isTorchOn = !isTorchOn; cameraId?.let { cameraManager?.setTorchMode(it, isTorchOn) } } catch(e: Throwable) {} }
                            "Location" -> DashboardQuickToggle(Icons.Default.LocationOn, true, "Location") { try { context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(e: Throwable) {} }
                            "Airplane" -> DashboardQuickToggle(Icons.Default.AirplanemodeActive, false, "Airplane") { try { context.startActivity(Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(e: Throwable) {} }
                            "DND" -> DashboardQuickToggle(Icons.Default.DoNotDisturbOn, false, "DND") { try { context.startActivity(Intent(android.provider.Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(e: Throwable) {} }
                            "Settings" -> DashboardQuickToggle(Icons.Default.Settings, true, "Settings") { try { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(e: Throwable) {} }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val pm = context.packageManager
            Row(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val validApps = pinnedApps.filter { it.isNotEmpty() }
                if (validApps.isEmpty()) {
                    Box(Modifier.size(36.dp).background(Color.White.copy(0.05f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(16.dp)) }
                } else {
                    validApps.forEach { pkg ->
                        val iconBmp = remember(pkg) { 
                            try { 
                                val drawable = pm.getApplicationIcon(pkg)
                                val bmp = android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bmp)
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(canvas)
                                bmp.asImageBitmap()
                            } catch(e: Throwable) { null } 
                        }
                        if (iconBmp != null) {
                            Image(bitmap = iconBmp, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape).clickable {
                                try { val launchIntent = pm.getLaunchIntentForPackage(pkg); if (launchIntent != null) { launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(launchIntent) } } catch(e: Throwable) {}
                            })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AppleControlCenterSlider(value = brightness, onValueChange = { brightness = it }, activeColor = Color.Yellow, icon = Icons.Default.BrightnessHigh)
            
            Spacer(modifier = Modifier.weight(0.5f))

            var activeStream by remember { mutableIntStateOf(android.media.AudioManager.STREAM_MUSIC) }
            val maxVol = remember(activeStream) { val mv = audioManager.getStreamMaxVolume(activeStream).toFloat(); if (mv <= 0f) 1f else mv }
            var currentVol by remember(activeStream) { mutableFloatStateOf( (audioManager.getStreamVolume(activeStream) / maxVol).coerceIn(0f, 1f) ) }
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppleControlCenterSlider(value = currentVol, onValueChange = { currentVol = it }, onValueChangeFinished = { try{ audioManager.setStreamVolume(activeStream, (currentVol * maxVol).toInt(), 0) } catch(e: Throwable){} }, activeColor = Color.Cyan, icon = Icons.Default.VolumeUp)
            }
        }
    }

    // 🚀 UPDATED: Tiles now strictly respect user Config sizes, shapes, and spacing
    @Composable
    fun DynamicIslandView.DashboardQuickToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, label: String? = null, onClick: () -> Unit = {}) {
        val theme = LocalIslandTheme.current
        val bgColor = if (isActive) Color(0xFF00FFCC) else Color.White.copy(alpha=0.1f)
        val tint = if (isActive) Color.Black else Color.White
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InteractiveIconButton(icon = icon, tint = tint, baseSize = theme.buttonSize, bgAlpha = if (isActive) 1f else 0.1f) { onClick() }
            if (label != null) { Spacer(modifier = Modifier.height(6.dp)); Text(label, color = Color.White.copy(alpha=0.9f), fontSize = 10.sp, fontWeight = FontWeight.Medium) }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.SystemAlertMid(alert: LiveActivityModel.SystemAlert) {
        val color = Color(alert.alertColor)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Box(modifier = Modifier.size(44.dp).background(color.copy(alpha=0.2f), CircleShape).border(1.dp, color.copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
                val icon = when(alert.alertType) { "THERMAL" -> Icons.Default.Warning; "ROGUE" -> Icons.Default.BatteryAlert; else -> Icons.Default.Info }
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

    fun formatTime(ms: Long): String { if (ms <= 0) return "0:00"; val s = ms / 1000; return String.format("%d:%02d", s / 60, s % 60) }

    @Composable
    fun IsolatedTimeText(durationMs: Long, posProvider: () -> Long, textColor: Color, modifier: Modifier = Modifier) {
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
        val theme = LocalIslandTheme.current 
        val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
        val currentPosition = posProvider().toFloat().coerceAtLeast(0f)
        
        val targetProgress = (currentPosition / safeDuration).coerceIn(0f, 1f)
        val animatedProgress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(durationMillis = 1000, easing = LinearEasing), label = "liquid_progress")

        androidx.compose.foundation.Canvas(modifier = modifier.height(theme.mediaBarThickness)) {
            drawLine(color = trackColor, start = androidx.compose.ui.geometry.Offset(0f, size.height / 2), end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2), strokeWidth = theme.mediaBarThickness.toPx(), cap = theme.mediaBarCap)
            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, size.height / 2), end = androidx.compose.ui.geometry.Offset(size.width * animatedProgress, size.height / 2), strokeWidth = theme.mediaBarThickness.toPx(), cap = theme.mediaBarCap)
        }
    }

    @Composable
    fun IsolatedCircularProgressIndicator(durationMs: Long, posProvider: () -> Long, color: Color, trackColor: Color, strokeWidth: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
        val safeDuration = if (durationMs > 0) durationMs.toFloat() else 1f
        CircularProgressIndicator(progress = { (posProvider().toFloat() / safeDuration).coerceIn(0f, 1f) }, color = color, trackColor = trackColor, strokeWidth = strokeWidth, modifier = modifier)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.AppTimerWarningMid(model: LiveActivityModel.AppTimerWarning) {
        var remainingSeconds by remember { mutableIntStateOf(((model.targetTimeMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)) }

        LaunchedEffect(model.targetTimeMs) {
            while (remainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                remainingSeconds = ((model.targetTimeMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            }
        }

        val pulseTransition = rememberInfiniteTransition(label = "pulse")
        val alertAlpha by pulseTransition.animateFloat(initialValue = 0.2f, targetValue = 0.6f, animationSpec = infiniteRepeatable(animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "alertAlpha")

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Box(modifier = Modifier.size(48.dp).background(Color.Red.copy(alpha = alertAlpha), CircleShape).border(2.dp, Color.Red, CircleShape), contentAlignment = Alignment.Center) {
                if (model.appIcon != null) { Image(bitmap = model.appIcon.asImageBitmap(), contentDescription = "App Icon", modifier = Modifier.size(36.dp).clip(CircleShape)) } else { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
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
        val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
        val currentPosition = posProvider().toFloat().coerceAtLeast(0f)
        CircularProgressIndicator(progress = { (currentPosition / safeDuration).coerceIn(0f, 1f) }, color = color, trackColor = color.copy(alpha = 0.2f), strokeWidth = 2.dp, modifier = Modifier.fillMaxSize())
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.RealityPillMini(model: LiveActivityModel.RealityPill) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Icon(Icons.Default.Timer, contentDescription = "Session Time", tint = Color(0xFF00FF00), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "${model.appName} • ${model.sessionMinutes}m", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
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
