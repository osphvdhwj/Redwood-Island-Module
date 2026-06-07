package com.example.dynamicisland.core.ui

import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.shared.model.LiveActivityModel

@Composable
fun DynamicIslandView.VolumeMixerMax(model: LiveActivityModel.VolumeMixer) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Volume Mixer", 
            color = Color.White, 
            fontSize = 18.sp, 
            fontWeight = FontWeight.Black
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Media Stream
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.weight(1f)) {
                    VerticalLiquidSlider(
                        value = model.mediaLevel.toFloat(),
                        iconRes = R.drawable.ic_play_vector, // Use music icon if available
                        activeColor = Color(0xFF00E5FF)
                    ) { newValue ->
                        onStreamVolumeDrag?.invoke(AudioManager.STREAM_MUSIC, newValue.toInt())
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Media", color = Color.White.copy(alpha=0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Ring Stream
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.weight(1f)) {
                    VerticalLiquidSlider(
                        value = model.ringLevel.toFloat(),
                        iconRes = R.drawable.ic_phone_vector,
                        activeColor = Color(0xFF7C4DFF)
                    ) { newValue ->
                        onStreamVolumeDrag?.invoke(AudioManager.STREAM_RING, newValue.toInt())
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Ring", color = Color.White.copy(alpha=0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Alarm Stream
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.weight(1f)) {
                    VerticalLiquidSlider(
                        value = model.alarmLevel.toFloat(),
                        iconRes = R.drawable.ic_timer_vector,
                        activeColor = Color(0xFFFF9E80)
                    ) { newValue ->
                        onStreamVolumeDrag?.invoke(AudioManager.STREAM_ALARM, newValue.toInt())
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Alarm", color = Color.White.copy(alpha=0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // System Stream
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.weight(1f)) {
                    VerticalLiquidSlider(
                        value = model.systemLevel.toFloat(),
                        iconRes = R.drawable.ic_sync_vector,
                        activeColor = Color(0xFFB0BEC5)
                    ) { newValue ->
                        onStreamVolumeDrag?.invoke(AudioManager.STREAM_SYSTEM, newValue.toInt())
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("System", color = Color.White.copy(alpha=0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
