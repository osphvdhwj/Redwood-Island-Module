package com.example.dynamicisland.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import com.example.dynamicisland.ipc.IslandState
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.mvi.IslandIntent

/**
 * PRO-GRADE PILL ROUTER
 *
 * Central dispatcher that maps the active model to its specialized UI
 * based on the current Island visual state.
 */
@Composable
fun DynamicIslandView.PillRouter(
    state: IslandState,
    model: LiveActivityModel?,
    onIntent: (IslandIntent) -> Unit
) {
    if (model == null) return

    when (state) {
        IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> MiniPillRouter(model, onIntent)
        IslandState.TYPE_2_MID -> MidPillRouter(model, onIntent)
        IslandState.TYPE_3_MAX -> MaxPillRouter(model, onIntent)
        IslandState.TYPE_CUBE -> {
            if (model is LiveActivityModel.Charging) ChargingCube(model)
        }
        IslandState.TYPE_ORBITAL -> {
            if (model is LiveActivityModel.HardwareMonitor) {
                com.example.dynamicisland.ui.components.OrbitalRingUI(
                    color = androidx.compose.ui.graphics.Color.White,
                    speedFactor = (model.fps / 60f).coerceIn(0.1f, 2.0f)
                )
            }
        }
        IslandState.TYPE_BRUTALIST -> {
            if (model is LiveActivityModel.SystemAlert) {
                BrutalistAlert(model)
            }
        }
        else -> {}
    }
}

@Composable
fun DynamicIslandView.BrutalistAlert(model: LiveActivityModel.SystemAlert) {
    com.example.dynamicisland.ui.components.BrutalistContainer {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(8.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Text(
                text = model.title,
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 14.sp
            )
            androidx.compose.material3.Text(
                text = model.message,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun DynamicIslandView.MiniPillRouter(model: LiveActivityModel, onIntent: (IslandIntent) -> Unit) {
    when (model) {
        is LiveActivityModel.Music           -> MusicMini(model)
        is LiveActivityModel.Call            -> CallMini(model)
        is LiveActivityModel.HardwareMonitor -> HardwareGaugeMini(model)
        is LiveActivityModel.RealityPill     -> RealityPillMini(model)
        is LiveActivityModel.General         -> GeneralMini(model)
        is LiveActivityModel.Dashboard       -> NavLauncherMini(model)
        else -> MiniDefaultView()
    }
}

@Composable
fun DynamicIslandView.MidPillRouter(model: LiveActivityModel, onIntent: (IslandIntent) -> Unit) {
    when (model) {
        is LiveActivityModel.Music -> MusicMid(model)
        is LiveActivityModel.Call  -> CallMid(model)
        is LiveActivityModel.Charging -> ChargingMid(model)
        is LiveActivityModel.OngoingTask -> OngoingTaskMid(model)
        is LiveActivityModel.SystemAlert -> SystemAlertMid(model)
        is LiveActivityModel.Dashboard -> DashboardMid(model)
        is LiveActivityModel.General -> {
            // Smart delegation for general models
            when {
                model.id.contains("translation") -> TranslationGeneralMid(model)
                model.id.contains("barcode")     -> BarcodeGeneralMid(model)
                else -> GeneralMid(model)
            }
        }
        else -> GeneralMid(LiveActivityModel.General("fallback", ActivityType.NONE, "Activity", "Active"))
    }
}

@Composable
fun DynamicIslandView.MaxPillRouter(model: LiveActivityModel, onIntent: (IslandIntent) -> Unit) {
    val controller = this.controller ?: return
    when (model) {
        is LiveActivityModel.Music -> MusicMax(model)
        is LiveActivityModel.Charging -> ChargingMax(model)
        is LiveActivityModel.Dashboard -> DashboardMax(model, controller)
        is LiveActivityModel.NotificationStack -> NotificationStackMax(model)
        is LiveActivityModel.VolumeMixer -> VolumeMixerMax(model)
        is LiveActivityModel.QuickNote -> NoteEditorMax(controller.settingsState.allowedNotesApps.firstOrNull()) { controller.evaluatePriority() }
        else -> DashboardMax(model as? LiveActivityModel.Dashboard ?: LiveActivityModel.Dashboard(), controller)
    }
}

@Composable
private fun MiniDefaultView() {
    // Elegant fallback for unknown types
    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.fillMaxSize())
}
