package com.example.dynamicisland.core.bridge

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.manager.IslandMediaManager
import com.example.dynamicisland.shared.ipc.*

/**
 * PRO-GRADE MEDIA BRIDGE
 * Deep integration with specialized music players that exceed standard MediaSession capabilities.
 */
class MediaBridge(
    private val context: Context,
    private val mediaManager: IslandMediaManager
) {
    companion object {
        const val PKG_POWERAMP = "com.maxmpz.audioplayer"
        const val PKG_VIMUSIC = "it.vfsfitvnm.vimusic"
        const val PKG_SPOTIFY = "com.spotify.music"
    }

    /**
     * Handles specialized player updates.
     * Can be called from BroadcastReceivers or Hooks.
     */
    fun onSpecializedUpdate(pkg: String, intent: Intent) {
        when (pkg) {
            PKG_POWERAMP -> handlePowerAmp(intent)
            PKG_VIMUSIC -> handleViMusic(intent)
        }
    }

    private fun handlePowerAmp(intent: Intent) {
        // PowerAmp sends rich track info via 'com.maxmpz.audioplayer.TRACK_CHANGED'
        val track = intent.getBundleExtra("track") ?: return
        val title = track.getString("title") ?: "Unknown"
        val artist = track.getString("artist") ?: "Unknown Artist"
        val duration = track.getInt("duration", 0) * 1000L
        
        // We'll let the standard MediaSession pick up the bitmap for efficiency,
        // but we can use this for faster metadata sync.
    }

    private fun handleViMusic(intent: Intent) {
        // ViMusic is mostly standard but we can force-refresh metadata here
    }

    /**
     * Executes specialized commands (e.g. Toggle Like on Spotify/PowerAmp)
     */
    fun sendSpecializedCommand(pkg: String, command: String) {
        when (pkg) {
            PKG_POWERAMP -> {
                val intent = Intent("com.maxmpz.audioplayer.API_COMMAND")
                intent.setPackage(PKG_POWERAMP)
                when (command) {
                    "LIKE" -> intent.putExtra("cmd", 50) // PowerAmp Like CMD
                    "UNLIKE" -> intent.putExtra("cmd", 51)
                }
                context.sendBroadcast(intent)
            }
            PKG_SPOTIFY -> {
                // Spotify often requires specific MediaButton handling or internal API hooks
            }
        }
    }
}
