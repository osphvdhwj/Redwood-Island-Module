package com.example.dynamicisland.settings

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*

/**
 * Pro-Grade Icon Engine (Pillar 5)
 * Defines the visual soul of the Island's iconography.
 */
sealed class IconPack(val id: String) {
    object MaterialYou : IconPack("MATERIAL_YOU")
    object iOS : IconPack("IOS")
    object OxygenOS : IconPack("OXYGEN_OS")
    object OneUI : IconPack("ONE_UI")
    object AmoledCyberpunk : IconPack("AMOLED_CYBERPUNK")
    object CupertinoGlass : IconPack("CUPERTINO_GLASS")

    companion object {
        fun fromString(id: String): IconPack {
            return when (id.uppercase()) {
                "IOS" -> iOS
                "OXYGEN_OS" -> OxygenOS
                "ONE_UI" -> OneUI
                "AMOLED_CYBERPUNK" -> AmoledCyberpunk
                "CUPERTINO_GLASS" -> CupertinoGlass
                else -> MaterialYou
            }
        }
    }
}
