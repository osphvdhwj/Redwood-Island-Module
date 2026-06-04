package com.example.dynamicisland.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.rememberCoroutineScope
import com.example.dynamicisland.manager.SystemOptimizer
import kotlinx.coroutines.launch
import android.widget.Toast
import android.content.Context
import android.graphics.PixelFormat
import android.view.Choreographer
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.example.dynamicisland.data.repository.GameHubRepository
import kotlinx.coroutines.delay
import java.io.File

@SuppressLint("ViewConstructor")
class GameSpaceComposeView(val context: Context, val moduleContext: Context, val wm: WindowManager, private val gameHubRepo: GameHubRepository) : FrameLayout(context), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore get() = store

    val isExpanded = mutableStateOf(false)
    val fps = mutableIntStateOf(0)
    val cpu = mutableIntStateOf(0)
    val gpu = mutableIntStateOf(0)
    val perfLevel = mutableStateOf(GameHubRepository.PerformanceLevel.BALANCED)

    private var frameCount = 0
    private var lastTime = 0L

    init {
        setViewTreeLifecycleOwner(this)
        setViewTreeViewModelStoreOwner(this)
        setViewTreeSavedStateRegistryOwner(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        startStatsMonitor()

        val composeView = ComposeView(context).apply {
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    GameSpaceUI(
                        isExpanded = isExpanded.value,
                        onExpand = { expand(true) },
                        onCollapse = { expand(false) },
                        fps = fps.intValue,
                        cpu = cpu.intValue,
                        gpu = gpu.intValue,
                        perfLevel = perfLevel.value,
                        onPerfChange = { 
                            perfLevel.value = it
                            gameHubRepo.setPerformanceLevel(it)
                        },
                        gameHubRepo = gameHubRepo
                    )
                }
            }
        }
        addView(composeView)
    }

    private fun startStatsMonitor() {
        // FPS Monitoring
        Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastTime == 0L) lastTime = frameTimeNanos
                frameCount++
                val diff = (frameTimeNanos - lastTime) / 1_000_000
                if (diff >= 1000) {
                    fps.intValue = frameCount
                    frameCount = 0
                    lastTime = frameTimeNanos
                    updateHardwareStats()
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        })
    }

    private fun updateHardwareStats() {
        try {
            // GPU Busy %
            val gpuBusy = File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage").readText().trim().removeSuffix("%").toIntOrNull() ?: 0
            gpu.intValue = gpuBusy

            // CPU Load (Mock/Simple)
            cpu.intValue = (10..90).random() 
        } catch (_: Exception) {}
    }

    private fun expand(expand: Boolean) {
        isExpanded.value = expand
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
        gameHubRepo.onStop()
    }
}


@Composable
fun GameSpaceUI(
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    fps: Int,
    cpu: Int,
    gpu: Int,
    perfLevel: GameHubRepository.PerformanceLevel,
    onPerfChange: (GameHubRepository.PerformanceLevel) -> Unit,
    gameHubRepo: GameHubRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onCollapse() }
            )
        }

        // Xiaomi-style Edge Handle (Left)
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

        // Full Dashboard
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
                    // Stats Cluster
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        XiaomiStat(label = "FPS", value = fps.toString(), color = Color(0xFF00FFB2))
                        XiaomiStat(label = "CPU", value = "$cpu%", color = Color.White)
                        XiaomiStat(label = "GPU", value = "$gpu%", color = Color.White)
                        
                        IconButton(onClick = onCollapse) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))

                    // Performance Switcher
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
                        PerfButton("Battery", perfLevel == GameHubRepository.PerformanceLevel.BATTERY) { onPerfChange(GameHubRepository.PerformanceLevel.BATTERY) }
                        PerfButton("Balanced", perfLevel == GameHubRepository.PerformanceLevel.BALANCED) { onPerfChange(GameHubRepository.PerformanceLevel.BALANCED) }
                        PerfButton("Wild", perfLevel == GameHubRepository.PerformanceLevel.WILD) { onPerfChange(GameHubRepository.PerformanceLevel.WILD) }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                    
                    // Feature Grid
                    Text("OEM Tools", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            ToolIcon(Icons.Default.Speed, "Boost RAM") {
                                coroutineScope.launch {
                                    val freed = gameHubRepo.boostMemory()
                                    Toast.makeText(context, "Memory Boosted: ${freed / (1024 * 1024)} MB freed", Toast.LENGTH_SHORT).show()
                                }
                            }
                            ToolIcon(Icons.Default.CleaningServices, "Clean Junk") {
                                coroutineScope.launch {
                                    val freed = gameHubRepo.cleanJunk()
                                    Toast.makeText(context, "Junk Cleaned: ${freed / (1024 * 1024)} MB freed", Toast.LENGTH_SHORT).show()
                                }
                            }
                            ToolIcon(Icons.Default.DeleteSweep, "Deep Clean") {
                                coroutineScope.launch {
                                    val files = gameHubRepo.deepCleanScan()
                                    Toast.makeText(context, "Found ${files.size} large files to clean", Toast.LENGTH_SHORT).show()
                                }
                            }
                            ToolIcon(Icons.Default.Tune, "GPU Tuner") {}
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            ToolIcon(Icons.Default.VideoCall, "Voice") {}
                            ToolIcon(Icons.Default.ScreenShare, "Record") {}
                            ToolIcon(Icons.Default.Timer, "Timer") {}
                            ToolIcon(Icons.Default.Settings, "Config") {}
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                    
                    // Floating Apps
                    Text("Multi-Tasking", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        AppIcon(Icons.Default.Message, "WhatsApp")
                        AppIcon(Icons.Default.Forum, "Discord")
                        AppIcon(Icons.Default.Telegram, "Telegram")
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
    val alpha by animateFloatAsState(if (isActive) 1f else 0f)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF00FFB2).copy(alpha = alpha))
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

@Composable
fun AppIcon(icon: ImageVector, label: String) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFF1A1A1A))
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}
