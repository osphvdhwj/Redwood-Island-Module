import re

with open('./app/src/main/java/com/example/dynamicisland/DynamicIslandView.kt', 'r') as f:
    content = f.read()

# 1. Imports
imports = """// The missing Foundation and Drawing modifiers
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.draw.alpha

// Material You Dynamic Colors (CrDroid 11.8 integration)
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.platform.LocalContext

// Custom Canvas & Gradients for the Glowing Ring
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

// Haptic Feedback Engine
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// Hardware Gauge Icon
import androidx.compose.material.icons.filled.Info
"""

if "import androidx.compose.foundation.basicMarquee" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.*", "import androidx.compose.material.icons.filled.*\n" + imports)

# 2. Universal Mid Logic + Icons
universal_logic = """    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun UniversalMid(textColor: Color, activity: LiveActivityModel) {
        // Create a pulsing alpha animation for the charging state
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alphaPulse by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alphaPulse"
        )

        val progress = when(activity) {
            is LiveActivityModel.General -> activity.progress
            is LiveActivityModel.Charging -> activity.level / 100f
            else -> null
        }
        val colorInt = when(activity) {
            is LiveActivityModel.General -> activity.accentColor
            is LiveActivityModel.Charging -> android.graphics.Color.GREEN
            else -> android.graphics.Color.WHITE
        }
        val title = when(activity) {
            is LiveActivityModel.General -> activity.title
            is LiveActivityModel.Charging -> if (activity.isPluggedIn) "Charging" else "Disconnected"
            else -> ""
        }
        val dataText = when(activity) {
            is LiveActivityModel.General -> activity.dataText
            is LiveActivityModel.Charging -> "${activity.level}%"
            else -> ""
        }


        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                if (progress != null) {
                    CircularProgressIndicator(
                        progress = { progress },
                        color = Color(colorInt),
                        trackColor = textColor.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Apply the pulse animation ONLY if it is actively charging
                val iconAlpha = if (activity.type == ActivityType.CHARGING) alphaPulse else 1f

                Icon(
                    androidx.compose.ui.res.painterResource(getIconForType(activity.type)),
                    contentDescription = null,
                    tint = Color(colorInt), // This automatically applies the Red/Yellow/Green battery color
                    modifier = Modifier.size(24.dp).alpha(iconAlpha)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                 Text(text = dataText, color = textColor.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
            }
        }
    }

    @Composable
    fun ChargingMid(charging: LiveActivityModel.Charging) {
        UniversalMid(Color.White, charging)
    }

    @Composable
    fun GeneralMid(general: LiveActivityModel.General) {
        UniversalMid(Color.White, general)
    }"""

content = re.sub(r'    @Composable\n    fun GeneralMid.*?\}\n    \}', '', content, flags=re.DOTALL)
content = re.sub(r'    @Composable\n    fun ChargingMid.*?\}\n    \}', universal_logic, content, flags=re.DOTALL)


replacement_icon = """    private fun getIconForType(type: ActivityType): Int {
        return when(type) {
            ActivityType.CALL -> R.drawable.ic_call_vector
            ActivityType.NAVIGATION -> R.drawable.ic_map_vector
            ActivityType.TIMER -> R.drawable.ic_timer_vector
            ActivityType.MESSAGE -> R.drawable.ic_mail_vector
            ActivityType.ALARM -> R.drawable.ic_alarm_vector
            ActivityType.CHARGING -> R.drawable.ic_battery_charging_vector // Active charging
            ActivityType.BATTERY_LOW -> R.drawable.ic_battery_alert_vector // Disconnected/Low
            ActivityType.BLUETOOTH -> R.drawable.ic_bluetooth_vector
            ActivityType.WIFI -> R.drawable.ic_wifi_vector
            else -> R.drawable.ic_sync_vector
        }
    }"""
content = re.sub(r'    private fun getIconForType.*?\}\n    \}', replacement_icon, content, flags=re.DOTALL)

# 3. Canvas Ring
canvas_logic = """            // 1. THE GLOWING PROGRESS RING (Material You Canvas)
            val activity = liveActivityState.value
            if (state == IslandState.HIDDEN && activity != null && activity is LiveActivityModel.Charging) {
                val actProg = activity.level / 100f
                val safeProgress = if (actProg.isNaN() || actProg.isInfinite()) 0f else actProg.coerceIn(0f, 1f)

                val ringBrush = Brush.sweepGradient(
                    colors = listOf(Color.Cyan, Color.Blue, Color.Magenta, Color.Cyan)
                )

                Canvas(modifier = Modifier.size(camWidth.value.dp + 6.dp, camHeight.value.dp + 6.dp)) {
                    drawArc(
                        brush = ringBrush,
                        startAngle = -90f,
                        sweepAngle = safeProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            } else if (state == IslandState.HIDDEN && activity != null && activity is LiveActivityModel.General && activity.progress != null) {
                val actProg = activity.progress
                val safeProgress = if (actProg.isNaN() || actProg.isInfinite()) 0f else actProg.coerceIn(0f, 1f)

                val ringBrush = Brush.sweepGradient(
                    colors = listOf(Color.Cyan, Color.Blue, Color.Magenta, Color.Cyan)
                )

                Canvas(modifier = Modifier.size(camWidth.value.dp + 6.dp, camHeight.value.dp + 6.dp)) {
                    drawArc(
                        brush = ringBrush,
                        startAngle = -90f,
                        sweepAngle = safeProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }"""

content = re.sub(r'// THE PUNCH HOLE.*?\)\s*\}', canvas_logic, content, flags=re.DOTALL)

# 4. OptIns
optin_mini = """    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun GeneralMini(general: LiveActivityModel.General) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            val iconRes = getIconForType(general.type)
            Icon(androidx.compose.ui.res.painterResource(iconRes), contentDescription = null, tint = Color(general.accentColor), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "${general.title} • ${general.dataText}", color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
        }
    }"""
content = re.sub(r'    @Composable\n    fun GeneralMini.*?\}\n    \}', optin_mini, content, flags=re.DOTALL)

optin_music = """    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MusicMini(music: LiveActivityModel.Music) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(text = "${music.title} • ${music.artist}", color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
        }
    }"""
content = re.sub(r'    @Composable\n    fun MusicMini.*?\}\n    \}', optin_music, content, flags=re.DOTALL)


hw_gauge = """    @Composable
    fun HardwareGaugeMini(hw: LiveActivityModel.HardwareMonitor) {
        // Dynamically shift color based on thermal throttling thresholds
        val tempColor = when {
            hw.cpuTempCelsius > 45f -> Color.Red
            hw.cpuTempCelsius > 38f -> Color.Yellow
            else -> Color.Green
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = "Hardware", tint = tempColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))

            // Visual Line Gauge
            androidx.compose.material3.LinearProgressIndicator(
                progress = { (hw.cpuTempCelsius / 60f).coerceIn(0f, 1f) },
                modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = tempColor,
                trackColor = Color.DarkGray
            )

            Spacer(Modifier.width(8.dp))
            Text(text = "${hw.cpuFreqMhz} MHz", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }"""
content = re.sub(r'    @Composable\n    fun HardwareMini.*?\}\n    \}', hw_gauge, content, flags=re.DOTALL)
content = content.replace("HardwareMini(model)", "HardwareGaugeMini(model)")


with open('./app/src/main/java/com/example/dynamicisland/DynamicIslandView.kt', 'w') as f:
    f.write(content)
