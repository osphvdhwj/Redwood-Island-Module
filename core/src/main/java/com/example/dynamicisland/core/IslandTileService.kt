package com.example.dynamicisland.core

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class IslandTileService : TileService() {
    
    @Inject
    lateinit var controller: IslandController

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val isEnabled = controller.settingsState.islandEnabled
        qsTile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.label = "Redwood Island"
        qsTile.subtitle = if (isEnabled) "Enabled" else "Disabled"
        qsTile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val currentState = controller.settingsState.islandEnabled
        
        // PANIC BUTTON: Toggle the global enabled state
        // This will trigger the controller to remove/add windows immediately
        controller.updateSettings { 
            it.copy(islandEnabled = !currentState) 
        }
        
        updateTileState()
    }
}
