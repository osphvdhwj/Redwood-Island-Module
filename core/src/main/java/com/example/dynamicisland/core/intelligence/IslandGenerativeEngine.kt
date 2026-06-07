package com.example.dynamicisland.core.intelligence

import android.content.Context
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.ActivityType
import com.example.dynamicisland.shared.model.IslandIntent
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.settings.*
import com.google.mlkit.nl.entityextraction.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🧠 ELITE ISLAND GENERATIVE ENGINE (Feature B)
 *
 * Processes screen content using on-device ML Kit Entity Extraction.
 * Predicts user intent and generates proactive Dynamic Island states.
 */
@Singleton
class IslandGenerativeEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val neuralCore: IslandNeuralCore,
    private val controller: com.example.dynamicisland.core.domain.state.IslandController
) {
    
    // ... rest of engine ...

    /**
     * Entry point for processing raw text from Ghost Satellites.
     */
    fun processScreenContent(pkg: String, rawText: String) {
        if (rawText.isBlank()) return
        
        // 🛡️ Proactive Performance Hook
        controller.applyProactivePerformance(pkg)
        
        entityExtractor.annotate(rawText)
            .addOnSuccessListener { annotations ->
                val bestIntent = selectBestIntent(pkg, annotations)
                bestIntent?.let { neuralCore.dispatch(it) }
            }
    }

    private fun selectBestIntent(pkg: String, annotations: List<EntityAnnotation>): IslandIntent? {
        if (annotations.isEmpty()) return null

        // Priority 1: Flight Tracking
        val flight = annotations.find { it.entities.any { e -> e.type == Entity.TYPE_FLIGHT_NUMBER } }
        if (flight != null) {
            return buildGenerativeIntent("Flight Live", flight.annotatedText, 0xFF007AFF.toInt())
        }

        // Priority 2: Tracking Numbers (Parcel)
        val tracking = annotations.find { it.entities.any { e -> e.type == Entity.TYPE_TRACKING_NUMBER } }
        if (tracking != null) {
            return buildGenerativeIntent("Parcel Tracking", tracking.annotatedText, 0xFF34C759.toInt())
        }

        // Priority 3: Money/Payments
        val money = annotations.find { it.entities.any { e -> e.type == Entity.TYPE_MONEY } }
        if (money != null) {
             return buildGenerativeIntent("Payment Detected", money.annotatedText, 0xFF5856D6.toInt())
        }

        // Priority 4: OTP/Codes (via Regex fallback for specific apps)
        if (pkg.contains("messaging") || pkg.contains("whatsapp")) {
            val codeRegex = Regex("\\b\\d{4,6}\\b")
            val match = codeRegex.find(annotations.first().annotatedText)
            if (match != null) {
                return buildGenerativeIntent("Security Code", match.value, 0xFFFF3B30.toInt())
            }
        }

        return null
    }

    private fun buildGenerativeIntent(title: String, body: String, color: Int): IslandIntent {
        val model = LiveActivityModel.General(
            id = "gen_${System.currentTimeMillis()}",
            type = ActivityType.MESSAGE,
            title = title,
            body = body,
            accentColor = color
        )
        return IslandIntent.SyncState(
            state = com.example.dynamicisland.shared.model.IslandState.TYPE_2_MID,
            activeModel = model,
            splitModel = null
        )
    }
}
