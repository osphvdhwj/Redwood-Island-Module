package com.example.dynamicisland.ui

import androidx.compose.runtime.collectAsState
import com.example.dynamicisland.model.LocalIslandTheme
import com.example.dynamicisland.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.dynamicisland.intelligence.IslandTranslationEngine
import com.example.dynamicisland.model.LiveActivityModel

@Composable
fun DynamicIslandView.TranslationMid(result: IslandTranslationEngine.TranslationResult) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current
    val theme   = LocalIslandTheme.current

    var copyDone by remember { mutableStateOf(false) }
    val copyScale by animateFloatAsState(
        targetValue   = if (copyDone) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label         = "copy_scale"
    )

    LaunchedEffect(copyDone) {
        if (copyDone) {
            kotlinx.coroutines.delay(1200)
            copyDone = false
        }
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
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(theme.batIconSize + 4.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (result.isModelDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF4FC3F7)
                )
            } else {
                Text(
                    text     = langFlag(result.sourceLanguage),
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = result.originalText,
                color    = Color.White.copy(alpha = 0.55f),
                fontSize = (theme.alertMsgSize.value - 1f).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            AnimatedContent(
                targetState = result.translatedText,
                label       = "translation_text"
            ) { translated ->
                if (translated.isEmpty()) {
                    Text(
                        text     = "Translating…",
                        color    = Color(0xFF4FC3F7).copy(alpha = 0.60f),
                        fontSize = theme.alertTitleSize,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text       = translated,
                        color      = Color(0xFF4FC3F7),
                        fontSize   = theme.alertTitleSize,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (result.translatedText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (copyDone) Color(0xFF4CAF50).copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("translation", result.translatedText)
                        )
                        copyDone = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (copyDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Copy done",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp).graphicsLayer { scaleX = copyScale; scaleY = copyScale }
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_sync_vector),
                        contentDescription = "Copy translation",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
