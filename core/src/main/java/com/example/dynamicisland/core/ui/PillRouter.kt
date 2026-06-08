package com.example.dynamicisland.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.ui.components.OrbitalRingUI
import com.example.dynamicisland.core.ui.components.BrutalistContainer
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.LiveActivityModel

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
                OrbitalRingUI(
                    color = Color.White,
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
    BrutalistContainer {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = model.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = model.message,
                color = Color.White.copy(alpha = 0.7f),
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
    Spacer(modifier = Modifier.fillMaxSize())
}
