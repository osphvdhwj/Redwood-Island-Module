package com.example.dynamicisland.settings

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*

sealed class IconPack(val name: String) {
    object CupertinoGlass : IconPack("Cupertino Glass")
    object MaterialYou : IconPack("Material You")
    object AmoledCyberpunk : IconPack("AMOLED Cyberpunk")

    companion object {
        fun fromString(name: String): IconPack {
            return when (name) {
                "CUPERTINO_GLASS" -> CupertinoGlass
                "MATERIAL_YOU" -> MaterialYou
                "AMOLED_CYBERPUNK" -> AmoledCyberpunk
                else -> MaterialYou
            }
        }
    }
}
