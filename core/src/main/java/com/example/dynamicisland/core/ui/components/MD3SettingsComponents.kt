package com.example.dynamicisland.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.example.dynamicisland.shared.settings.AestheticStyle
import com.example.dynamicisland.shared.settings.IconPack
import com.example.dynamicisland.shared.settings.DesignLanguage
import com.example.dynamicisland.shared.settings.PhysicsStyle
import com.example.dynamicisland.shared.settings.ContentTransitionStyle
import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.shared.model.LocalIslandTheme
import com.example.dynamicisland.shared.model.IslandTheme
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.settings.*
@Composable
fun SettingsExpander(
    title: String,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = !expanded },
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp).size(24.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
            }
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, bottom = 8.dp)
                content()
    }
}
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(top = 16.dp)
    )
fun SettingsSwitch(
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
            if (icon != null) {
                    imageVector = icon,
                    modifier = Modifier.padding(end = 16.dp).size(24.dp)
            Column(modifier = Modifier.weight(1f)) {
                    color = MaterialTheme.colorScheme.onSurface
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.padding(start = 16.dp)
            )
fun SettingsMenuLink(
    onClick: () -> Unit
        onClick = onClick,
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
@OptIn(ExperimentalFoundationApi::class)
fun SettingsSlider(
    value: Float,
    defaultValue: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueFormatter: (Float) -> String = { it.toInt().toString() }
    var isEditingManually by remember { mutableStateOf(false) }
    var manualValue by remember { mutableStateOf(value.toInt().toString()) }
    Column(
            verticalAlignment = Alignment.CenterVertically,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 14.sp
            
            // Manual Input Toggle
            Surface(
                onClick = { 
                    isEditingManually = !isEditingManually 
                    manualValue = value.toInt().toString()
                },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
                    text = valueFormatter(value),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        if (isEditingManually) {
            OutlinedTextField(
                value = manualValue,
                onValueChange = { 
                    manualValue = it
                    it.toFloatOrNull()?.let { f ->
                        if (f in valueRange) onValueChange(f)
                    }
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(onClick = { isEditingManually = false }) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
        } else {
            Spacer(modifier = Modifier.height(8.dp))
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                IconButton(
                    onClick = { onValueChange((value - 1f).coerceIn(valueRange)) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary)
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                    onClick = { onValueChange((value + 1f).coerceIn(valueRange)) },
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                    onClick = { onValueChange(defaultValue) },
                        .size(36.dp)
                        .clip(CircleShape)
                        Icons.Default.RestartAlt, 
                        contentDescription = "Reset", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsChoiceChip(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
            options.forEach { option ->
                FilterChip(
                    selected = selectedOption == option,
                    onClick = { onOptionSelected(option) },
                    label = { Text(option.lowercase().capitalize()) },
private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
