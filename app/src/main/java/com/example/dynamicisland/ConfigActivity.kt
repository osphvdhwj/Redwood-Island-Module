package com.example.dynamicisland

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        val prefs = getSharedPreferences("dynamic_island_prefs", Context.MODE_PRIVATE)
        val sbY = findViewById<SeekBar>(R.id.seekBarYOffset)
        val btn = findViewById<Button>(R.id.btnSave)

        sbY.progress = prefs.getInt("offset_y", 0)

        btn.setOnClickListener {
            prefs.edit().putInt("offset_y", sbY.progress).apply()

            // Broadcast to SystemUI to reload
            val intent = Intent("com.example.dynamicisland.RELOAD_SETTINGS")
            intent.setPackage("com.android.systemui")
            sendBroadcast(intent)

            Toast.makeText(this, "Settings Applied Successfully", Toast.LENGTH_SHORT).show()
        }
    }
}
