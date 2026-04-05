package com.example.dynamicisland.ui
import com.example.dynamicisland.R
import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*
import com.example.dynamicisland.manager.*

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope

@Composable
fun ThemeSlider(label: String, key: String, default: Float, range: ClosedFloatingPointRange<Float>, prefs: SharedPreferences, scope: CoroutineScope, context: Context) {
    var localValue by remember { mutableFloatStateOf(prefs.getFloat(key, default)) }
    Text(text = "$label: ${localValue.toInt()}", color = Color.White, modifier = Modifier.padding(top = 8.dp))
    Slider(
        value = localValue, 
        onValueChange = { localValue = it }, 
        onValueChangeFinished = { 
            ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat(key, localValue) }) { ConfigManager.sendGestureUpdate(context, prefs) }
        }, 
        valueRange = range
    )
}

@Composable
fun PrecisionSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit) {
    var localValue by remember(value) { mutableFloatStateOf(value) } 
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.width(60.dp), fontSize = 14.sp)
        IconButton(onClick = { 
            localValue = (localValue - 1f).coerceIn(range)
            onValueChange(localValue)
            onValueChangeFinished() 
        }) { Icon(Icons.Default.Remove, contentDescription = "-") }
        
        Slider(
            value = localValue, 
            onValueChange = { localValue = it }, 
            onValueChangeFinished = { onValueChange(localValue); onValueChangeFinished() }, 
            modifier = Modifier.weight(1f), 
            valueRange = range
        )
        
        IconButton(onClick = { 
            localValue = (localValue + 1f).coerceIn(range)
            onValueChange(localValue)
            onValueChangeFinished() 
        }) { Icon(Icons.Default.Add, contentDescription = "+") }
        Text(String.format("%.0f", localValue), modifier = Modifier.width(40.dp), fontSize = 14.sp)
    }
}

@Composable
fun FeatureSwitch(label: String, key: String, default: Boolean, prefs: SharedPreferences, scope: CoroutineScope, context: Context) {
    var checked by remember { mutableStateOf(prefs.getBoolean(key, default)) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { 
            checked = it
            ConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean(key, it) }) { ConfigManager.sendGestureUpdate(context, prefs) }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureDropdown(label: String, options: Array<IslandAction>, prefs: SharedPreferences, prefKey: String, scope: CoroutineScope, context: Context) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(prefs.getString(prefKey, IslandAction.NONE.name) ?: IslandAction.NONE.name) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = selectedOption.replace("_", " "), onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
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
