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
        val color = if (model.isPluggedIn) Color(0xFF00FF41) else if (model.level <= 20) Color.Red else Color.White
        val infiniteTransition = rememberInfiniteTransition(label = "cube_pulse")
        val pulseScale by infiniteTransition.animateFloat(initialValue = 0.9f, targetValue = 1.1f, animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")
        val glowAlpha by infiniteTransition.animateFloat(initialValue = 0.15f, targetValue = 0.45f, animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Ambient glowing background
            if (model.isPluggedIn) {
                Box(modifier = Modifier.size(50.dp).background(color.copy(alpha = glowAlpha), CircleShape).blur(12.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale })
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(imageVector = if (model.isPluggedIn) Icons.Default.Add else Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(32.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale })
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "${model.level}%", color = color, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.MusicMini(music: LiveActivityModel.Music) {
        // Fallback to a nice Cyan if the dominant color is too dark
        val dynamicTextColor = Color(music.titleTextColor).takeIf { it != Color.Transparent && it != Color.Black } ?: Color(0xFF00FFCC) 
        
        val safeDuration = if (music.durationMs <= 0L) 1f else music.durationMs.toFloat()
        val currentPosition = currentMediaPos.longValue.toFloat().coerceAtLeast(0f)
        val targetProgress = (currentPosition / safeDuration).coerceIn(0f, 1f)
        
        // Fluid animation for the climbing vine border
        val animatedProgress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(durationMillis = 1000, easing = LinearEasing), label = "vine_progress")

        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier
                .fillMaxSize()
                .androidx.compose.ui.draw.drawWithCache {
                    val cornerRadius = size.height / 2f
                    // Create the exact physical boundary of the pill
                    val path = androidx.compose.ui.graphics.Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                rect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                            )
                        )
                    }
                    // Measure the path and extract just the portion based on song progress
                    val pathMeasure = androidx.compose.ui.graphics.PathMeasure()
                    pathMeasure.setPath(path, forceClosed = false)
                    val segmentPath = androidx.compose.ui.graphics.Path()
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = pathMeasure.length * animatedProgress,
                        destination = segmentPath,
                        startWithMoveTo = true
                    )

                    onDrawWithContent {
                        drawContent() // Draw the disk and text first
                        // Draw the glowing vine on top of the borders
                        drawPath(
                            path = segmentPath,
                            color = dynamicTextColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                .padding(horizontal = 12.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "spin")
            val currentRotation = if (isCubeRotationEnabled.value && music.isPlaying) rotation else 0f
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp)) {
                if (music.albumArt != null) {
                    Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.fillMaxSize().clip(CircleShape).rotate(currentRotation))
                } else {
                    Box(Modifier.fillMaxSize().background(Color.White.copy(0.2f), CircleShape))
                }
            }
            
            Spacer(Modifier.width(10.dp))
            
            Text(text = music.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f).safeMarquee(islandState.value))
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.MusicMid(music: LiveActivityModel.Music) {
        val theme = LocalIslandTheme.current
        val dynamicTextColor = Color(music.titleTextColor)
        val infiniteTransition = rememberInfiniteTransition()
        val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "spin")
        val currentRotation = if (isCubeRotationEnabled.value && music.isPlaying) rotation else 0f

        // Shifted padding to left-align the disk
        Row(modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            
            // Disk moved to far left & enlarged
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                IsolatedCircularProgress(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, color = dynamicTextColor)
                if (music.albumArt != null) { 
                    Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(50.dp).clip(CircleShape).rotate(currentRotation)) 
                } else {
                    Box(Modifier.size(50.dp).background(Color.White.copy(alpha=0.2f), CircleShape))
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Column for Text and Wavy Progress
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = music.title, color = dynamicTextColor, fontSize = 15.sp, fontFamily = theme.titleFont, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
                Text(text = music.artist, color = dynamicTextColor.copy(alpha = 0.7f), fontSize = 13.sp, fontFamily = theme.titleFont, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
                
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(text = formatTime(currentMediaPos.longValue), color = dynamicTextColor.copy(alpha=0.7f), fontSize = 10.sp)
                    Spacer(Modifier.width(6.dp))
                    // Replaced with Interactive Wavy Bar
                    InteractiveWavyMediaBar(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, color = dynamicTextColor, trackColor = dynamicTextColor.copy(alpha=0.2f), onSeek = { onSeekTo?.invoke(it) }, modifier = Modifier.weight(1f).height(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(text = formatTime(music.durationMs), color = dynamicTextColor.copy(alpha=0.7f), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Transport Buttons (Now includes Custom Media Actions)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val favoriteAction = music.customActions.find { it.actionName.contains("heart", true) || it.actionName.contains("favorite", true) || it.actionName.contains("thumb", true) || it.actionName.contains("like", true) }
                if (favoriteAction != null) {
                    InteractiveIconButton(icon = Icons.Default.FavoriteBorder, tint = dynamicTextColor, baseSize = 26.dp, bgAlpha = 0f) { onCustomMediaAction?.invoke(favoriteAction.actionName) }
                }

                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, tint = dynamicTextColor, baseSize = 28.dp, bgAlpha = 0f) { onPrevClick?.invoke() }
                
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                InteractiveIconButton(icon = playIcon, tint = dynamicTextColor, baseSize = 38.dp, bgAlpha = 0.15f) { onPlayPauseClick?.invoke() }
                
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowForward, tint = dynamicTextColor, baseSize = 28.dp, bgAlpha = 0f) { onNextClick?.invoke() }

                val repeatAction = music.customActions.find { it.actionName.contains("repeat", true) || it.actionName.contains("loop", true) || it.actionName.contains("shuffle", true) }
                if (repeatAction != null) {
                    val icon = if (repeatAction.actionName.contains("shuffle", true)) Icons.Default.Shuffle else Icons.Default.Refresh
                    InteractiveIconButton(icon = icon, tint = dynamicTextColor, baseSize = 26.dp, bgAlpha = 0f) { onCustomMediaAction?.invoke(repeatAction.actionName) }
                }
            }
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
                // FIXED: Reordered clip -> background -> clickable -> padding for a perfect touch target and ripple
                Row(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha=0.2f)).clickable { onAudioOutputClick?.invoke() }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(audioIcon, contentDescription = "Output", tint = dynamicTextColor, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(audioLabel, color = dynamicTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.weight(0.5f))
            
            Text(text = music.title, color = dynamicTextColor, fontSize = theme.titleSize * 1.25f, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.fillMaxWidth().safeMarquee(islandState.value))
            Text(text = music.artist, color = dynamicTextColor.copy(alpha=0.8f), fontSize = (theme.titleSize * 0.85f) * 1.15f, maxLines = 1, modifier = Modifier.fillMaxWidth().safeMarquee(islandState.value))
            
            Spacer(modifier = Modifier.weight(0.5f))

            IsolatedTimeRow(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = dynamicTextColor)
            InteractiveWavyMediaBar(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, color = dynamicTextColor, trackColor = dynamicTextColor.copy(alpha=0.2f), onSeek = { onSeekTo?.invoke(it) }, modifier = Modifier.padding(vertical = 8.dp))
            Spacer(modifier = Modifier.weight(1f))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(theme.buttonSpacing, Alignment.CenterHorizontally)) {
                
                // Custom Actions mapped properly!
                val favoriteAction = music.customActions.find { it.actionName.contains("heart", true) || it.actionName.contains("favorite", true) || it.actionName.contains("thumb", true) || it.actionName.contains("like", true) }
                if (favoriteAction != null) {
                    InteractiveIconButton(icon = Icons.Default.FavoriteBorder, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0f) { onCustomMediaAction?.invoke(favoriteAction.actionName) }
                } else {
                    Spacer(Modifier.width(theme.buttonSize))
                }
                
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0f) { onPrevClick?.invoke() }
                
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                InteractiveIconButton(icon = playIcon, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0.2f) { onPlayPauseClick?.invoke() }
                
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowForward, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0f) { onNextClick?.invoke() }

                val repeatAction = music.customActions.find { it.actionName.contains("repeat", true) || it.actionName.contains("loop", true) || it.actionName.contains("shuffle", true) }
                if (repeatAction != null) {
                    val icon = if (repeatAction.actionName.contains("shuffle", true)) Icons.Default.Shuffle else Icons.Default.Refresh
                    InteractiveIconButton(icon = icon, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0f) { onCustomMediaAction?.invoke(repeatAction.actionName) }
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
        
        val safeQsTiles: List<String> = this.qsTiles.toList()
        val safePinnedApps: List<String> = this.pinnedApps.toList()

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

        var torchLevel by remember { mutableIntStateOf(1) }
        var maxTorchLevel by remember { mutableIntStateOf(1) }
        
        LaunchedEffect(Unit) {
            try {
                if (cameraId != null) {
                    val characteristics = cameraManager?.getCameraCharacteristics(cameraId)
                    maxTorchLevel = characteristics?.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                }
            } catch(e: Throwable) {}
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
            
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(theme.buttonSpacing, Alignment.CenterHorizontally)) {
                val activeQS = safeQsTiles.filter { it.isNotEmpty() && it != "None" }
                if (activeQS.isEmpty()) {
                    DashboardQuickToggle(Icons.Default.Settings, true, "Settings") { try { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(e: Throwable) {} }
                } else {
                    activeQS.forEach { tileStr ->
                        val tile: String = tileStr
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
                val validApps = safePinnedApps.filter { it.isNotEmpty() }
                if (validApps.isEmpty()) {
                    Box(Modifier.size(36.dp).background(Color.White.copy(0.05f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(16.dp)) }
                } else {
                    validApps.forEach { pkgStr ->
                        val pkg: String = pkgStr
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

            if (isTorchOn && maxTorchLevel > 1) {
                AppleControlCenterSlider(
                    value = torchLevel.toFloat() / maxTorchLevel.toFloat(), 
                    onValueChange = { 
                        torchLevel = (it * maxTorchLevel).toInt().coerceAtLeast(1)
                        try { cameraId?.let { id -> cameraManager?.turnOnTorchWithStrengthLevel(id, torchLevel) } } catch(e:Throwable){}
                    }, 
                    activeColor = Color.White, 
                    icon = Icons.Default.FlashlightOn
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                AppleControlCenterSlider(value = brightness, onValueChange = { brightness = it }, activeColor = Color.Yellow, icon = Icons.Default.BrightnessHigh)
                Spacer(modifier = Modifier.height(12.dp))
            }

            var activeStream by remember { mutableIntStateOf(android.media.AudioManager.STREAM_MUSIC) }
            val maxVol = remember(activeStream) { val mv = audioManager.getStreamMaxVolume(activeStream).toFloat(); if (mv <= 0f) 1f else mv }
            var currentVol by remember(activeStream) { mutableFloatStateOf( (audioManager.getStreamVolume(activeStream) / maxVol).coerceIn(0f, 1f) ) }
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppleControlCenterSlider(value = currentVol, onValueChange = { currentVol = it }, onValueChangeFinished = { try{ audioManager.setStreamVolume(activeStream, (currentVol * maxVol).toInt(), 0) } catch(e: Throwable){} }, activeColor = Color.Cyan, icon = Icons.Default.VolumeUp)
            }
        }
    }

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

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.OngoingTaskMid(task: LiveActivityModel.OngoingTask) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Box(modifier = Modifier.size(44.dp).background(Color.White.copy(alpha=0.2f), CircleShape).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
                IsolatedCircularProgressIndicator(durationMs = task.progressMax.toLong(), posProvider = { task.progress.toLong() }, color = Color.Cyan, trackColor = Color.White.copy(alpha=0.2f), strokeWidth = 2.dp)
                Icon(Icons.Default.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = task.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
                 Text(text = task.text, color = Color.White.copy(alpha=0.8f), fontSize = 14.sp, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
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
    
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DynamicIslandView.ChargingMid(charging: LiveActivityModel.Charging) {
        val batteryColor = if (charging.isPluggedIn) Color(0xFF00FF41) else if (charging.level <= 20) Color.Red else Color.White
        
        // Glow animation for charging
        val infiniteTransition = rememberInfiniteTransition(label = "charging_glow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 0.6f,
            animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alpha"
        )
        
        // Fluid fill animation for battery level
        val targetFill = charging.level / 100f
        val animatedFill by animateFloatAsState(targetValue = targetFill, animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "fill")

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Animated Charging Icon
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                Box(modifier = Modifier.size(36.dp).background(batteryColor.copy(alpha = if (charging.isPluggedIn) glowAlpha else 0.05f), CircleShape).blur(if (charging.isPluggedIn) 8.dp else 0.dp))
                Icon(imageVector = if (charging.isPluggedIn) Icons.Default.Add else Icons.Default.Warning, contentDescription = null, tint = batteryColor, modifier = Modifier.size(24.dp))
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = if (charging.isPluggedIn) "Charging" else "Battery", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
                 Text(text = "${charging.level}% Available", color = Color.White.copy(alpha=0.7f), fontSize = 13.sp, maxLines = 1)
            }
            
            Spacer(Modifier.width(12.dp))
            
            // Custom Visual Battery Indicator
            Box(modifier = Modifier.width(42.dp).height(20.dp).border(1.5.dp, Color.White.copy(alpha=0.4f), RoundedCornerShape(4.dp)).padding(2.dp), contentAlignment = Alignment.CenterStart) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction = animatedFill).background(batteryColor, RoundedCornerShape(2.dp)))
            }
            // Battery Tip
            Box(modifier = Modifier.width(3.dp).height(8.dp).background(Color.White.copy(alpha=0.4f), RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)))
        }
    }

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

@Composable
    fun InteractiveWavyMediaBar(
        durationMs: Long,
        posProvider: () -> Long,
        color: Color,
        trackColor: Color,
        onSeek: (Long) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
        var isDragging by remember { mutableStateOf(false) }
        var dragProgress by remember { mutableFloatStateOf(0f) }
        val currentProgress = (posProvider() / safeDuration).coerceIn(0f, 1f)
        val displayProgress = if (isDragging) dragProgress else currentProgress

        // Sine wave animation phase
        val phaseShift by rememberInfiniteTransition(label = "wave").animateFloat(
            initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "phase"
        )

        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(24.dp) // Larger invisible hit-box for easier grabbing
                .pointerInput(Unit) {
                    val velocityTracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            velocityTracker.resetTracking()
                        },
                        onDragEnd = {
                            val finalVelocity = velocityTracker.calculateVelocity().x
                            isDragging = false
                            // THROW TO CANCEL: If horizontal velocity is > 1500px/sec, ignore the seek
                            if (kotlin.math.abs(finalVelocity) < 1500f) {
                                onSeek((dragProgress * safeDuration).toLong())
                            }
                        },
                        onDragCancel = { isDragging = false }
                    ) { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val tappedProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek((tappedProgress * safeDuration).toLong())
                        }
                    )
                }
        ) {
            val midY = size.height / 2
            val activeWidth = size.width * displayProgress

            // 1. Draw inactive straight track
            drawLine(color = trackColor, start = androidx.compose.ui.geometry.Offset(activeWidth, midY), end = androidx.compose.ui.geometry.Offset(size.width, midY), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)

            // 2. Draw active WAVY track
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(0f, midY)
            val amplitude = if (isDragging) 4.dp.toPx() else 2.5.dp.toPx() // Wave gets bigger when grabbing
            val frequency = 0.08f
            
            for (x in 0..activeWidth.toInt() step 4) {
                val y = midY + kotlin.math.sin((x * frequency) + phaseShift) * amplitude
                path.lineTo(x.toFloat(), y)
            }
            path.lineTo(activeWidth, midY)

            drawPath(path = path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            
            // 3. Draw Thumb
            drawCircle(color = Color.White, radius = if (isDragging) 7.dp.toPx() else 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(activeWidth, midY))
        }
    }
