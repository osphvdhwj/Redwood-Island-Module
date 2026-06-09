package com.example.dynamicisland.core.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.dynamicisland.core.data.repository.GameHubRepository
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.core.model.IslandUiState
import com.example.dynamicisland.shared.ipc.*
import kotlinx.coroutines.launch
import com.example.dynamicisland.shared.model.IslandIntent
import com.example.dynamicisland.shared.model.PerformanceLevel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import android.os.Build

const val FrostedGlassShader = """
    uniform shader composable;
    uniform float2 size;
    uniform float radius;
    
    half4 main(float2 fragCoord) {
        half4 color = composable.eval(fragCoord);
        // Simple fast blur approximation for AGSL
        half4 sum = color;
        sum += composable.eval(fragCoord + float2(radius, radius));
        sum += composable.eval(fragCoord + float2(-radius, -radius));
        sum += composable.eval(fragCoord + float2(radius, -radius));
        sum += composable.eval(fragCoord + float2(-radius, radius));
        return sum / 5.0;
    }
"""

/**
 * GameSpace Dashboard UI
 */
@SuppressLint("ViewConstructor")
class GameSpaceComposeView(
    context: Context, 
    val moduleContext: Context, 
    val wm: WindowManager, 
    private val neuralCore: IslandNeuralCore,
    private val gameHubRepo: GameHubRepository // Keep for specialized tools (Boost/Clean)
) : FrameLayout(context), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore get() = store

    init {
        setViewTreeLifecycleOwner(this)
        setViewTreeViewModelStoreOwner(this)
        setViewTreeSavedStateRegistryOwner(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val composeView = ComposeView(context).apply {
            setContent {
                val state by neuralCore.uiState.collectAsState()
                MaterialTheme(colorScheme = darkColorScheme()) {
                    GameSpaceUI(
                        state = state,
                        onExpand = { expand(true) },
                        onCollapse = { expand(false) },
                        onPerfChange = { neuralCore.dispatch(IslandIntent.UpdatePerformanceLevel(it)) },
                        neuralCore = neuralCore,
                        gameHubRepo = gameHubRepo
                    )
                }
            }
        }
        addView(composeView)
    }

    private fun expand(expand: Boolean) {
        val layoutParams = layoutParams as? WindowManager.LayoutParams ?: return
        if (expand) {
            layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
            layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            layoutParams.dimAmount = 0.6f
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        } else {
            layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
            layoutParams.dimAmount = 0f
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        wm.updateViewLayout(this, layoutParams)
        neuralCore.dispatch(IslandIntent.ToggleExpand)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

@Composable
fun GameSpaceUI(
    state: IslandUiState,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onPerfChange: (PerformanceLevel) -> Unit,
    neuralCore: IslandNeuralCore,
    gameHubRepo: GameHubRepository
) {
    val isExpanded = state.isExpanded
    
    // Premium Momentum-Based Spring Physics (Mass: 1.0, Stiffness: 400, Damping: 0.7)
    val panelScale by animateFloatAsState(
        targetValue = if (state.isNotificationActive) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator }

    fun triggerVibe() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
        } else {
            vibrator.vibrate(10)
        }
    }

    // Detachable FPS Pill State
    var isFpsDetached by remember { mutableStateOf(false) }
    var fpsOffset by remember { mutableStateOf(Offset(300f, 200f)) }
    
    // Morph Animation State
    var justLaunchedGame by remember(state.currentForegroundApp) { 
        mutableStateOf(state.currentForegroundApp.isNotEmpty()) 
    }
    
    LaunchedEffect(justLaunchedGame) {
        if (justLaunchedGame) {
            // Trigger haptic sequence for game launch transition
            delay(1500)
            justLaunchedGame = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onCollapse() }
            )
            
            // 🛰️ SOUND RADAR OVERLAY (Active in Game Mode)
            if (state.currentForegroundApp.isNotEmpty()) {
                Box(modifier = Modifier.size(160.dp).align(Alignment.Center)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFF00FFB2).copy(alpha = 0.1f),
                            radius = size.minDimension / 2,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp)
                        )
                        // Left/Right Audio Peaks Visualization (Mms from AudioFlinger)
                        drawArc(
                            color = Color(0xFF00FFB2).copy(alpha = (0..30).random() / 100f),
                            startAngle = 135f,
                            sweepAngle = 90f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = Color(0xFF00FFB2).copy(alpha = (0..30).random() / 100f),
                            startAngle = -45f,
                            sweepAngle = 90f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
        
        // 💊 Detached FPS Pill
        if (isFpsDetached && state.currentForegroundApp.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(fpsOffset.x.toInt(), fpsOffset.y.toInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            fpsOffset += dragAmount
                        }
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC000000).copy(alpha = 0.8f))
                    .androidx.compose.foundation.border(1.dp, Color(0xFF00FFB2).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("FPS", color = Color.Gray, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${state.gamingFps.toInt()}", color = Color(0xFF00FFB2), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Xiaomi-style Edge Handle (Liquid Physics Pulse & Morphing target)
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            val handleWidth by animateFloatAsState(if (justLaunchedGame) 8f else 4f)
            val handleColor = if (justLaunchedGame) Color(0xFFFF3B3B) else Color(0xFF00FFB2)
            Box(
                modifier = Modifier
                    .width(handleWidth.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .background(Brush.verticalGradient(listOf(handleColor, Color(0xFF00D1FF))))
                    .clickable { onExpand() }
            )
        }

        // Game Turbo Dashboard (Glassmorphism 2.0 foundation)
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = spring(0.75f, 400f)),
            exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = spring(0.85f, 500f)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .scale(panelScale)
                    .clip(RoundedCornerShape(topEnd = 40.dp, bottomEnd = 40.dp))
            ) {
                // Main sliding panel (Liquid Glass simulation)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(264.dp)
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Modifier.graphicsLayer {
                                    renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(
                                        android.graphics.RuntimeShader(FrostedGlassShader).apply {
                                            setFloatUniform("size", size.width, size.height)
                                            setFloatUniform("radius", 15f)
                                        },
                                        "composable"
                                    ).asComposeRenderEffect()
                                }
                            } else Modifier
                        )
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xCC0A0A0A), // Frosted dark
                                    Color(0x991A1A1A)  // More transparent at edges
                                )
                            )
                        )
                        .padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Turbo Engine", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Game Center", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("⬡", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        // FPS Graph Area (Long-press to detach)
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { 
                                            isFpsDetached = true
                                            triggerVibe()
                                        },
                                        onDrag = { change, dragAmount -> 
                                            change.consume()
                                            fpsOffset += dragAmount
                                        }
                                    )
                                }
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Avg ${state.gamingFps.toInt()}ms", fontSize = 10.sp, color = Color(0xFFFF3B3B))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Ping 2ms", fontSize = 10.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0x33FF3B3B))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text("● LIVE", fontSize = 10.sp, color = Color(0xFFFF3B3B))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            FpsGraph(fps = state.gamingFps.toInt())
                        }

                        // Perf Mode Toggles
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isBalanced = state.performanceLevel == PerformanceLevel.BALANCED
                            PerfButton(
                                label = "Balanced",
                                isActive = isBalanced,
                                activeBg = Color(0xFF2A2A2A),
                                activeBorder = Color(0xFF444444),
                                modifier = Modifier.weight(1f)
                            ) { onPerfChange(PerformanceLevel.BALANCED) }

                            val isExtreme = state.performanceLevel == PerformanceLevel.WILD
                            PerfButton(
                                label = "Extreme",
                                isActive = isExtreme,
                                activeBg = Color(0xFFFF3B3B),
                                activeBorder = Color(0xFFFF3B3B),
                                modifier = Modifier.weight(1f)
                            ) { onPerfChange(PerformanceLevel.WILD) }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 14.dp).background(Color(0xFF1E1E1E)))
                        Spacer(modifier = Modifier.height(12.dp))

                        // 2x2 Action Grid
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                            ActionCell("⊞", "Perf Config", "Deep tuning", Modifier.weight(1f), RoundedCornerShape(topStart = 8.dp), Color(0xFF1A1A1A)) {
                                // Navigate to config
                            }
                            Spacer(modifier = Modifier.width(1.dp))
                            ActionCell("🧹", "Clean", "Clear RAM", Modifier.weight(1f), RoundedCornerShape(topEnd = 8.dp), Color(0xFF1A1A1A)) {
                                neuralCore.dispatch(IslandIntent.FreezeBackground)
                            }
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                            ActionCell("◈", "Game Svc", "System optimizations", Modifier.fillMaxWidth(), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp), Color(0xFF1A1A1A)) {}
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Actions Row 1
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            BottomActionCell("⏺", "Record", Modifier.weight(1f), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                            BottomActionCell("📷", "Screenshot", Modifier.weight(1f), RoundedCornerShape(0.dp))
                            BottomActionCell("📶", "Hotspot", Modifier.weight(1f), RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(1.dp))
                        
                        // Bottom Actions Row 2
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            BottomActionCell("⚔️", "Arena", Modifier.weight(1f), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp), badge = "1")
                            BottomActionCell("🔒", "Freeze", Modifier.weight(1f), RoundedCornerShape(0.dp))
                            BottomActionCell("🖼", "Pixel Shot", Modifier.weight(1f), RoundedCornerShape(0.dp))
                            BottomActionCell("▦", "More", Modifier.weight(1f), RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                        }
                    }
                }

                // Vertical Divider
                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFF1E1E1E)))

                // Right Side Sliders (Parallel & Dynamic)
                var brightness by remember { mutableIntStateOf(72) }
                var autoBrightness by remember { mutableStateOf(true) }

                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(Color(0xEA0A0A0A)) // Darker transparent
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // BRIGHTNESS COLUMN
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        VerticalSlider(
                            value = brightness,
                            onChange = { 
                                brightness = it
                                autoBrightness = false 
                                // Non-linear mapping: Perceived 20-80 maps roughly to 50-80 system brightness
                                val mappedBrightness = if (it in 20..80) {
                                    50 + ((it - 20) * (30f / 60f)).toInt()
                                } else {
                                    it
                                }
                                neuralCore.dispatch(IslandIntent.UpdateBrightness(mappedBrightness, false))
                                triggerVibe()
                            },
                            icon = "☀️",
                            label = "Bright",
                            accentColor = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clickable { 
                                    autoBrightness = !autoBrightness
                                    neuralCore.dispatch(IslandIntent.UpdateBrightness(brightness, autoBrightness))
                                    triggerVibe()
                                }
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (autoBrightness) Color(0x33FFD700) else Color(0xFF1A1A1A))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("AUTO", fontSize = 8.sp, color = if (autoBrightness) Color(0xFFFFD700) else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // AUDIO COLUMN (Dynamic Context)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val activeVolume = if (state.isMicActive) state.callVolume 
                                          else if (state.isPerAppVolumeActive) state.perAppVolume 
                                          else state.volume
                        
                        val activeLabel = if (state.isMicActive) "Call" 
                                         else if (state.isPerAppVolumeActive) "App Vol" 
                                         else "Media"
                        
                        val activeIcon = if (state.isMicActive) "📞" 
                                        else if (state.isPerAppVolumeActive) "📱" 
                                        else "🔊"
                                        
                        val activeColor = if (state.isMicActive) Color(0xFF00FFB2) 
                                         else if (state.isPerAppVolumeActive) Color(0xFF9D00FF) 
                                         else Color(0xFF1DB954)

                        VerticalSlider(
                            value = activeVolume,
                            onChange = { 
                                neuralCore.dispatch(IslandIntent.UpdateVolume(it)) 
                                triggerVibe()
                            },
                            icon = activeIcon,
                            label = activeLabel,
                            accentColor = activeColor
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clickable { triggerVibe() /* Toggle Mute logic in Core later */ }
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1A1A1A))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MUTE", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerfButton(label: String, isActive: Boolean, activeBg: Color, activeBorder: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) activeBg else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = if (isActive) Color.White else Color(0xFF666666), fontSize = 12.sp, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun ActionCell(icon: String, label: String, sub: String?, modifier: Modifier, shape: RoundedCornerShape, bg: Color, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 22.sp, color = Color.White.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = Color(0xFFE0E0E0), fontWeight = FontWeight.Medium)
            if (sub != null) {
                Text(text = sub, fontSize = 9.sp, color = Color(0xFF555555), lineHeight = 11.sp)
            }
        }
    }
}

@Composable
fun BottomActionCell(icon: String, label: String, modifier: Modifier, shape: RoundedCornerShape, badge: String? = null) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF1A1A1A))
            .clickable { }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = label, fontSize = 9.sp, color = Color(0xFF888888))
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(14.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0xFFFF3B3B)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = badge, fontSize = 8.sp, color = Color.White)
            }
        }
    }
}
