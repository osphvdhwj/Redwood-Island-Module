package com.example.dynamicisland.core.util

import androidx.compose.runtime.compositionLocalOf

/**
 * 🌍 OMNI-TRANSLATION ENGINE
 * 
 * Ported from Vyxel Apps to provide industry-leading localization.
 */
data class RedwoodStrings(
    val islandTitle: String = "Redwood Island",
    val settings: String = "Settings",
    val appearance: String = "Appearance",
    val performance: String = "Performance",
    val gameHub: String = "Game Hub",
    val boost: String = "Boost",
    val clean: String = "Clean",
    val charging: String = "Charging",
    val batteryLow: String = "Battery Low",
    val temperature: String = "Temperature",
    val memoryFreed: String = "Memory Freed",
    val search: String = "Search",
    val installed: String = "Installed",
    val profile: String = "Profile"
)

val LocalRedwoodStrings = compositionLocalOf { RedwoodStrings() }

object TranslationProvider {
    
    fun getStrings(language: String): RedwoodStrings = when (language) {
        "Hindi" -> RedwoodStrings(
            islandTitle = "रेडवुड आइलैंड",
            settings = "सेटिंग्स",
            appearance = "दिखावट",
            performance = "प्रदर्शन",
            gameHub = "गेम हब",
            boost = "बूस्ट",
            clean = "साफ करें",
            charging = "चार्जिंग",
            batteryLow = "बैटरी कम है",
            temperature = "तापमान",
            memoryFreed = "मेमोरी मुक्त",
            search = "खोजें",
            installed = "इंस्टॉल्ड",
            profile = "प्रोफाइल"
        )
        "Spanish" -> RedwoodStrings(
            islandTitle = "Redwood Island",
            settings = "Ajustes",
            appearance = "Apariencia",
            performance = "Rendimiento",
            gameHub = "Game Hub",
            boost = "Potenciar",
            clean = "Limpiar",
            charging = "Cargando",
            batteryLow = "Batería baja",
            temperature = "Temperatura",
            memoryFreed = "Memoria liberada",
            search = "Buscar",
            installed = "Instalado",
            profile = "Perfil"
        )
        // Additional languages from Vyxel port can be added here
        else -> RedwoodStrings()
    }
}
