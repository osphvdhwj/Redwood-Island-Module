package com.example.dynamicisland.system.window

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Region
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.dynamicisland.R
import com.example.dynamicisland.ipc.IslandState
import com.example.dynamicisland.ui.state.IslandViewModel
import com.example.dynamicisland.ui.state.IslandUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.PowerManager
import com.example.dynamicisland.ui.state.IslandIntent

import kotlinx.coroutines.flow.sample

/**
 * Pillar 2: Decoupled Window Management Service.
 * This service is responsible for:
 * 1. Creating and managing the Overlay Window.
 * 2. Hosting the Jetpack Compose UI.
 * 3. Updating WindowManager flags based on MVI state.
 */
@AndroidEntryPoint
class IslandWindowService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = viewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var composeView: ComposeView
    
    private val viewModel: IslandViewModel by lazy {
        ViewModelProvider(this)[IslandViewModel::class.java]
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Pillar 5: Lifecycle Awareness & Pillar 6: Doze Mode Lifecycle Awareness
    private val lifecycleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    viewModel.handleIntent(IslandIntent.UpdateScreenState(false))
                    // Hide overlay to preserve battery when screen is OFF
                    composeView.visibility = View.GONE
                }
                Intent.ACTION_SCREEN_ON -> {
                    viewModel.handleIntent(IslandIntent.UpdateScreenState(true))
                    // Resume observation and show overlay when screen is ON
                    composeView.visibility = View.VISIBLE
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isPowerSaveMode = powerManager?.isPowerSaveMode ?: false
                    viewModel.handleIntent(IslandIntent.UpdatePowerSaveMode(isPowerSaveMode))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        setupWindowManager()
        setupViewModel()
        setupComposeView()
        
        startForegroundService()
        registerLifecycleReceiver()
        
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        observeState()
    }
    
    private fun registerLifecycleReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        registerReceiver(lifecycleReceiver, filter)
    }

    private fun setupWindowManager() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            title = "IslandWindow"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }

    private fun setupViewModel() {
        // ViewModel is lazily initialized via Hilt
    }

    private fun setupComposeView() {
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@IslandWindowService)
            setViewTreeViewModelStoreOwner(this@IslandWindowService)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setViewTreeSavedStateRegistryOwner(this@IslandWindowService)
            }
            
            // Set content will be called here or in a separate UI component later
            setContent {
                IslandApp(viewModel)
            }
        }
        
        windowManager.addView(composeView, params)
    }

    private fun startForegroundService() {
        val channelId = "island_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Island Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Dynamic Island Active")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()

        startForeground(1, notification)
    }

    private fun observeState() {
        serviceScope.launch {
            // Pillar 6: Global Event Debouncing using .sample(250L) to limit updates to max 4 per sec
            viewModel.uiState
                .sample(250L)
                .collectLatest { state ->
                    updateWindowFlags(state)
                }
        }
    }

    private fun updateWindowFlags(state: IslandUiState) {
        var changed = false
        
        // Example: Make window non-touchable if hidden
        val isTouchable = state.islandState != IslandState.HIDDEN
        val touchFlag = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        
        if (isTouchable && (params.flags and touchFlag) != 0) {
            params.flags = params.flags and touchFlag.inv()
            changed = true
        } else if (!isTouchable && (params.flags and touchFlag) == 0) {
            params.flags = params.flags or touchFlag
            changed = true
        }
        
        if (changed) {
            windowManager.updateViewLayout(composeView, params)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        unregisterReceiver(lifecycleReceiver)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        serviceScope.cancel()
        windowManager.removeView(composeView)
        super.onDestroy()
    }
}
