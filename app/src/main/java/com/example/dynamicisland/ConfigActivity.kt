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

        // Read or default to 0
        sbY.progress = prefs.getInt("offset_y", 0)

        btn.setOnClickListener {
            val progress = sbY.progress
            val editor = prefs.edit()
            editor.putInt("offset_y", progress)
            editor.commit() // commit() is synchronous, ensuring write before broadcast

            // Fix file permissions manually for Xposed (XSharedPreferences requirement)
            try {
                val dataDir = applicationInfo.dataDir
                val prefsDir = java.io.File(dataDir, "shared_prefs")
                val prefsFile = java.io.File(prefsDir, "dynamic_island_prefs.xml")

                if (prefsDir.exists()) {
                    prefsDir.setExecutable(true, false)
                    prefsDir.setReadable(true, false)
                }
                if (prefsFile.exists()) {
                    prefsFile.setReadable(true, false)
                }
            } catch (e: Exception) {}

            // Broadcast to SystemUI to reload real-time
            val intent = Intent("com.example.dynamicisland.UPDATE_CONFIG")
            intent.putExtra("offset_y", progress)
            // Use explicit exported flag if needed, but here we just send
            sendBroadcast(intent)

            Toast.makeText(this, "Settings Applied Successfully", Toast.LENGTH_SHORT).show()
        }
    }
}
