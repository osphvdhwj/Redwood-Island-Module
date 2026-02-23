package com.example.dynamicisland

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        val seekBarX = findViewById<SeekBar>(R.id.seekBarXOffset)
        val seekBarY = findViewById<SeekBar>(R.id.seekBarYOffset)
        val seekBarWidth = findViewById<SeekBar>(R.id.seekBarWidth)
        val seekBarHeight = findViewById<SeekBar>(R.id.seekBarHeight)
        val btnSave = findViewById<Button>(R.id.btnSave)

        try {
            val xOffset = Settings.System.getInt(contentResolver, "redwood_island_x_offset", 0)
            val yOffset = Settings.System.getInt(contentResolver, "redwood_island_y_offset", 0)
            val wCorrection = Settings.System.getInt(contentResolver, "redwood_island_width_correction", 0)
            val hCorrection = Settings.System.getInt(contentResolver, "redwood_island_height_correction", 0)

            seekBarX.progress = xOffset + 100
            seekBarY.progress = yOffset
            seekBarWidth.progress = wCorrection + 100
            seekBarHeight.progress = hCorrection + 50
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading settings: " + e.message, Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            val xOffset = seekBarX.progress - 100
            val yOffset = seekBarY.progress
            val wCorrection = seekBarWidth.progress - 100
            val hCorrection = seekBarHeight.progress - 50

            try {
                Settings.System.putInt(contentResolver, "redwood_island_x_offset", xOffset)
                Settings.System.putInt(contentResolver, "redwood_island_y_offset", yOffset)
                Settings.System.putInt(contentResolver, "redwood_island_width_correction", wCorrection)
                Settings.System.putInt(contentResolver, "redwood_island_height_correction", hCorrection)

                Toast.makeText(this, "Settings Saved. Please restart SystemUI.", Toast.LENGTH_LONG).show()
            } catch (e: SecurityException) {
                Toast.makeText(this, "Permission denied. Grant Write Settings.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error saving: " + e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
