package com.example.dynamicisland.core.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
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
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.core.model.IslandUiState
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.IslandIntent
import com.example.dynamicisland.shared.model.PerformanceLevel
import com.example.dynamicisland.shared.settings.*
import kotlinx.coroutines.launch

/**
 * GameSpace Dashboard UI
 * 
 * Mandate Compliance:
 * 1. Clean Architecture: Pure UI layer, dispatches intents to NeuralCore.
 * 2. Reactive State: Observes IslandNeuralCore StateFlow.
 * 3. Lifecycle Security: Manages view-tree owners for Compose stability.
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
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isExpanded = state.isExpanded // This would need a separate flag in state if shared with Island

    Box(modifier = Modifier.fillMaxSize()) {
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onCollapse() }
            )
        }

        // Xiaomi-style Edge Handle
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .background(Color(0xFF00FFB2).copy(alpha = 0.8f))
                    .clickable { onExpand() }
            )
        }

        // Dashboard
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(380.dp)
                    .clip(RoundedCornerShape(topEnd = 40.dp, bottomEnd = 40.dp))
                    .background(Color(0xE60A0A0A))
                    .padding(32.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        XiaomiStat(label = "FPS", value = state.gamingFps.toInt().toString(), color = Color(0xFF00FFB2))
                        XiaomiStat(label = "CPU", value = "${state.gamingCpuUsage}%", color = Color.White)
                        XiaomiStat(label = "GPU", value = "${state.gamingGpuUsage}%", color = Color.White)
                        
                        IconButton(onClick = onCollapse) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))

                    Text("System Optimizer", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1A1A1A))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PerfButton("Battery", state.performanceLevel == PerformanceLevel.BATTERY) { onPerfChange(PerformanceLevel.BATTERY) }
                        PerfButton("Balanced", state.performanceLevel == PerformanceLevel.BALANCED) { onPerfChange(PerformanceLevel.BALANCED) }
                        PerfButton("Wild", state.performanceLevel == PerformanceLevel.WILD) { onPerfChange(PerformanceLevel.WILD) }
                    }

                    Text("Extreme Profiles", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1A1A1A))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PerfButton("Ultra Bat", state.isUltraBatteryActive) { neuralCore.dispatch(IslandIntent.ToggleUltraBattery(!state.isUltraBatteryActive)) }
                        PerfButton("Bypass", state.isThermalBypassActive) { neuralCore.dispatch(IslandIntent.ToggleThermalBypass(!state.isThermalBypassActive)) }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Text("OEM Tools", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            ToolIcon(Icons.Default.Speed, "Boost RAM") {
                                neuralCore.dispatch(IslandIntent.FreezeBackground)
                            }
                            ToolIcon(Icons.Default.CleaningServices, "Clean Junk") {
                                neuralCore.dispatch(IslandIntent.CleanupStorage)
                            }
                            ToolIcon(Icons.Default.DeleteSweep, "Deep Clean") {
                                neuralCore.dispatch(IslandIntent.CleanupStorage)
                            }
                            ToolIcon(Icons.Default.Tune, "GPU Tuner")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun XiaomiStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun PerfButton(label: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) Color(0xFF00FFB2) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = if (isActive) Color.Black else Color.White, fontSize = 13.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun ToolIcon(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp)) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF222222))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = Color.White, fontSize = 11.sp)
    }
}
