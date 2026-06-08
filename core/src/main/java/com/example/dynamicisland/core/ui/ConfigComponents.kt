package com.example.dynamicisland.core.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import kotlinx.coroutines.CoroutineScope

@Composable
fun ThemeSlider(label: String, key: String, default: Float, range: ClosedFloatingPointRange<Float>, prefs: SharedPreferences, scope: CoroutineScope, context: Context) {
    var localValue by remember { mutableFloatStateOf(prefs.getFloat(key, default)) }
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = String.format("%.0f", localValue), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = localValue, 
            onValueChange = { localValue = it }, 
            onValueChangeFinished = { 
                ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat(key, localValue) }) { ConfigManager.sendGestureUpdate(context, prefs) }
            }, 
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            )
        )
    }
}

@Composable
fun PrecisionSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit) {
    var localValue by remember(value) { mutableFloatStateOf(value) } 
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { 
                localValue = (localValue - 1f).coerceIn(range)
                onValueChange(localValue)
                onValueChangeFinished() 
            }) { Icon(painter = painterResource(R.drawable.ic_close_vector), contentDescription = "-", tint = MaterialTheme.colorScheme.primary) }
            
            Slider(
                value = localValue, 
                onValueChange = { localValue = it }, 
                onValueChangeFinished = { onValueChange(localValue); onValueChangeFinished() }, 
                modifier = Modifier.weight(1f), 
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            
            IconButton(onClick = { 
                localValue = (localValue + 1f).coerceIn(range)
                onValueChange(localValue)
                onValueChangeFinished() 
            }) { Icon(Icons.Default.Add, contentDescription = "+", tint = MaterialTheme.colorScheme.primary) }
            Text(String.format("%.0f", localValue), modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun FeatureSwitch(label: String, key: String, default: Boolean, prefs: SharedPreferences, scope: CoroutineScope, context: Context) {
    var checked by remember { mutableStateOf(prefs.getBoolean(key, default)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp), 
        horizontalArrangement = Arrangement.SpaceBetween, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, 
            onCheckedChange = { 
                checked = it
                ConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean(key, it) }) { ConfigManager.sendGestureUpdate(context, prefs) }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureDropdown(label: String, options: Array<IslandAction>, prefs: SharedPreferences, prefKey: String, scope: CoroutineScope, context: Context) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(prefs.getString(prefKey, IslandAction.NONE.name) ?: IslandAction.NONE.name) }
    ExposedDropdownMenuBox(
        expanded = expanded, 
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = selectedOption.replace("_", " "), 
            onValueChange = {}, 
            readOnly = true, 
            label = { Text(label) }, 
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, 
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name.replace("_", " ")) },
                    onClick = {
                        selectedOption = option.name; expanded = false
                        ConfigManager.commitAndBroadcast(prefs, scope, context, { putString(prefKey, option.name) }) { ConfigManager.sendGestureUpdate(context, prefs) }
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .squishClickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
                exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}
