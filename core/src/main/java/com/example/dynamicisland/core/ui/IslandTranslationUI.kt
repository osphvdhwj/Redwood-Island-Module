package com.example.dynamicisland.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.intelligence.IslandTranslationEngine
import com.example.dynamicisland.shared.ipc.*

@Composable
fun DynamicIslandView.TranslationMid(result: IslandTranslationEngine.TranslationResult) {
    val context = LocalContext.current
    val theme   = LocalIslandTheme.current

    var copyDone by remember { mutableStateOf(false) }
    val copyScale by animateFloatAsState(
        targetValue   = if (copyDone) 1.25f else 1f,
        animationSpec = IslandPhysics.springFloat,
        label         = "copy"
    )

    LaunchedEffect(copyDone) {
        if (copyDone) { kotlinx.coroutines.delay(1200); copyDone = false }
    }

    fun langFlag(code: String): String = when (code.take(2).lowercase()) {
        "ja" -> "🇯🇵"; "zh" -> "🇨🇳"; "ko" -> "🇰🇷"; "ar" -> "🇸🇦"
        "fr" -> "🇫🇷"; "de" -> "🇩🇪"; "es" -> "🇪🇸"; "pt" -> "🇧🇷"
        "ru" -> "🇷🇺"; "it" -> "🇮🇹"; "hi" -> "🇮🇳"; "tr" -> "🇹🇷"
        "nl" -> "🇳🇱"; "pl" -> "🇵🇱"; "vi" -> "🇻🇳"; "th" -> "🇹🇭"
        "id" -> "🇮🇩"; "ms" -> "🇲🇾"; "uk" -> "🇺🇦"; "cs" -> "🇨🇿"
        else -> "🌐"
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Smart Icon Backdrop ---
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (result.isModelDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFF007AFF))
            } else {
                Text(langFlag(result.sourceLanguage), fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // --- Result Content ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                result.originalText,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AnimatedContent(targetState = result.translatedText, label = "text") { translated ->
                Text(
                    text = translated.ifEmpty { "Translating..." },
                    color = if (translated.isEmpty()) Color(0xFF007AFF).copy(alpha = 0.6f) else Color(0xFF007AFF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // --- Copy Success Button ---
        Box(
            modifier = Modifier
                .size(42.dp)
                .graphicsLayer { scaleX = copyScale; scaleY = copyScale }
                .background(if (copyDone) Color(0xFF34C759).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f), CircleShape)
                .squishClickable {
                    if (result.translatedText.isNotEmpty()) {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("translation", result.translatedText))
                        copyDone = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (copyDone) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint = if (copyDone) Color(0xFF34C759) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
