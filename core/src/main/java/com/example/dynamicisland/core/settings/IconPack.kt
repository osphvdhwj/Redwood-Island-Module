package com.example.dynamicisland.shared.settings

/**
 * ELITE ICON ENGINE (Pillar 5)
 * Defines the visual soul of the Island's iconography.
 * Expanded to support industry-standard custom ROM styles.
 */
sealed class IconPack(val id: String, val displayName: String) {
    object MaterialYou : IconPack("MATERIAL_YOU", "Material You")
    object iOS : IconPack("IOS", "Cupertino Rounded")
    object OxygenOS : IconPack("OXYGEN_OS", "Never Settle")
    object OneUI : IconPack("ONE_UI", "Sam Style")
    object Pixel : IconPack("PIXEL", "Circular")
    object Outline : IconPack("OUTLINE", "Glass Outline")
    object Futuristic : IconPack("FUTURISTIC", "Cyber-HUD")
    object Minimal : IconPack("MINIMAL", "Dots & Lines")
    object AmoledCyberpunk : IconPack("AMOLED_CYBERPUNK", "High Contrast")
    object CupertinoGlass : IconPack("CUPERTINO_GLASS", "Liquid Glass")

    companion object {
        val ALL = listOf(
            MaterialYou, iOS, OxygenOS, OneUI, 
            Pixel, Outline, Futuristic, Minimal, AmoledCyberpunk, CupertinoGlass
        )
        
        fun fromString(id: String): IconPack {
            return ALL.find { it.id == id.uppercase() } ?: MaterialYou
        }
    }
}
