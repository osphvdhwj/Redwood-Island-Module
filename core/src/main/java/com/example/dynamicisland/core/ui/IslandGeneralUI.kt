package com.example.dynamicisland.core.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.IslandState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.GeneralMid(general: LiveActivityModel.General) {
    val theme = LocalIslandTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.size(44.dp).background(Color(general.accentColor).copy(alpha=0.2f), CircleShape).border(1.dp, Color(general.accentColor).copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(general.accentColor), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
             Text(text = general.title, color = Color.White, fontSize = theme.alertTitleSize, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
             Text(text = general.dataText, color = Color.White.copy(alpha=0.8f), fontSize = theme.alertMsgSize, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.GeneralMini(general: LiveActivityModel.General) { 
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) { 
        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(general.accentColor), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = "${general.title} • ${general.dataText}", color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value)) 
    } 
}

@Composable
fun DynamicIslandView.HardwareGaugeMini(hw: LiveActivityModel.HardwareMonitor) { 
    if (hw.isGamingModeOn) {
        GamingHUDMini(
            fps = hw.fps,
            frameMs = hw.frameMs,
            jankPct = hw.jankPct,
            cpuTemp = hw.cpuTempCelsius,
            cpuFreqMhz = hw.cpuFreqMhz
        )
    } else {
        val tempColor = when { hw.cpuTempCelsius > 45f -> Color.Red; hw.cpuTempCelsius > 38f -> Color.Yellow; else -> Color.Green }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) { 
            Icon(imageVector = Icons.Default.Info, contentDescription = "Hardware", tint = tempColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.LinearProgressIndicator(progress = { (hw.cpuTempCelsius / 60f).coerceIn(0f, 1f) }, modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)), color = tempColor, trackColor = Color.White.copy(alpha=0.2f))
            Spacer(Modifier.width(8.dp))
            Text(text = "${hw.cpuFreqMhz} MHz", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) 
        } 
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.RealityPillMini(model: LiveActivityModel.RealityPill) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Icon(Icons.Default.Notifications, contentDescription = "Session Time", tint = Color(0xFF00FF00), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = "${model.appName} • ${model.sessionMinutes}m", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
    }
}

fun DynamicIslandView.setState(newState: IslandState) { islandState.value = newState }
fun DynamicIslandView.setModel(model: LiveActivityModel?) { activeModel.value = model }
fun DynamicIslandView.setSplitModel(model: LiveActivityModel?) { splitModel.value = model }

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.safeMarquee(state: IslandState): Modifier {
    return if (state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING && state != IslandState.TYPE_CUBE) {
        this.basicMarquee()
    } else {
        this
    }
}

@Suppress("DEPRECATION")
fun performCustomHaptic(context: Context, strength: Int, forPackage: String = "") {
    if (strength == 0) return
    if (forPackage.isNotEmpty() &&
        com.example.dynamicisland.core.manager.PerAppProfileManager
            .getProfile(forPackage).hapticSuppression) return
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val effect = when (strength) {
                1 -> android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK) 
                2 -> android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK) 
                3 -> android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_HEAVY_CLICK) 
                else -> android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK)
            }
            vibrator.vibrate(effect)
        } else {
            val ms = when (strength) { 1 -> 10L; 2 -> 30L; 3 -> 60L; else -> 30L }
            vibrator.vibrate(ms)
        }
    } catch (e: Exception) {}
}
