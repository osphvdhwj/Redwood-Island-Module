package com.example.dynamicisland.core.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

@Composable
fun NoteEditorMax(targetNotesApp: String?, onSave: () -> Unit) {
    var noteText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, null, tint = IslandColors.accentCyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Quick Note", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(12.dp)
        ) {
            BasicTextField(
                value = noteText,
                onValueChange = { noteText = it },
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(IslandColors.accentCyan),
                decorationBox = { innerTextField ->
                    if (noteText.isEmpty()) {
                        Text("Start typing your note...", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                    }
                    innerTextField()
                }
            )
        }

        Button(
            onClick = {
                if (noteText.isNotEmpty()) {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, noteText)
                        type = "text/plain"
                        if (!targetNotesApp.isNullOrEmpty()) {
                            setPackage(targetNotesApp)
                        }
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(shareIntent)
                    onSave()
                }
            },
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(containerColor = IslandColors.accentCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Save to Notes", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
