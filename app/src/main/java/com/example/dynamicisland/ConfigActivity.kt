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
            // Make file world readable (deprecated but often needed for Xposed pre-A11,
            // though MainHook uses XSharedPreferences which handles permissions via root usually.
            // For modern Android, we rely on the prefs being in a standard location readable by XSharedPreferences)
            val editor = prefs.edit()
            editor.putInt("offset_y", sbY.progress)
            editor.commit() // commit() is synchronous, ensuring write before broadcast

            // Fix file permissions manually if needed (often required for Xposed modules)
            try {
                val file = java.io.File(applicationInfo.dataDir + "/shared_prefs/dynamic_island_prefs.xml")
                if (file.exists()) {
                    file.setReadable(true, false)
                }
            } catch (e: Exception) {}

            // Broadcast to SystemUI to reload
            val intent = Intent("com.example.dynamicisland.RELOAD_SETTINGS")
            // Sending without explicit package might be safer if receiver is registered globally,
            // but for security we can target SystemUI if we knew the exact package receiver context.
            // Since we registered in SystemUI via code, it's inside "android" or "com.android.systemui".
            // Let's try sending to both or just general.
            sendBroadcast(intent)

            Toast.makeText(this, "Settings Applied Successfully", Toast.LENGTH_SHORT).show()
        }
    }
}
