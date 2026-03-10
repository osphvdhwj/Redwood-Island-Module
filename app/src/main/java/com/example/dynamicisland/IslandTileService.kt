package com.example.dynamicisland

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class IslandTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.updateTile()
    }

    @Suppress("DEPRECATION")
    override fun onClick() {
        super.onClick()
        // Open the Config App when the user taps the QS Tile
        val intent = Intent(this, ConfigActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityAndCollapse(intent)
    }
}
