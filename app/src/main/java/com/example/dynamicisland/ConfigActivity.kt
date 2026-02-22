package com.example.dynamicisland

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        val prefs = getSharedPreferences("dynamic_island_prefs", Context.MODE_PRIVATE)

        val seekBarY = findViewById<SeekBar>(R.id.seekBarYOffset)
        val seekBarWidth = findViewById<SeekBar>(R.id.seekBarWidth)
        val seekBarHeight = findViewById<SeekBar>(R.id.seekBarHeight)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Load current values (defaults: Y=0, W=100 (offset), H=50 (offset))
        seekBarY.progress = prefs.getInt("offset_y", 0)
        seekBarWidth.progress = prefs.getInt("offset_width", 100)
        seekBarHeight.progress = prefs.getInt("offset_height", 50)

        btnSave.setOnClickListener {
            val editor = prefs.edit()
            editor.putInt("offset_y", seekBarY.progress)
            editor.putInt("offset_width", seekBarWidth.progress)
            editor.putInt("offset_height", seekBarHeight.progress)
            editor.apply()

            // In a real module, we might need to broadcast a reload intent or similar,
            // but Xposed usually requires a reboot or force stop of the target app to reload prefs
            // unless we implement a file observer.
            Toast.makeText(this, "Settings Saved. Please restart SystemUI.", Toast.LENGTH_LONG).show()
        }
    }
}
