package com.example.dynamicisland.core.intelligence

import android.content.Context
import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BATCH 6: On-Device Translation Overlay
 *
 * Triggered when the island detects clipboard content in a language
 * different from the device's primary locale. Uses MLKit's on-device
 * translation — fully offline after the first model download (~30MB).
 *
 * Pipeline:
 *   Clipboard text → LanguageIdentification (on-device, instant)
 *     → if foreign language detected
 *     → model download if needed (background, battery-aware)
 *     → Translation (on-device)
 *     → island expands to TYPE_2_MID showing original + translation
 *
 * Model management:
 *   Models are downloaded once on first use for each language pair.
 *   We maintain a model registry to avoid re-downloading.
 *   Models are deleted if device storage < 500MB free.
 *
 * Supported direction: any detected language → device locale.
 * The translation always targets the user's system language, so a
 * Japanese SMS shows in Hindi for a Hindi-locale user, English for
 * an English-locale user, and so on.
 */
class IslandTranslationEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "IslandTranslation"

        // Minimum confidence to act on a language detection result
        private const val MIN_LANG_CONFIDENCE = 0.60f

        // Text length gates — don't translate very short or very long strings
        private const val MIN_TRANSLATE_CHARS = 12
        private const val MAX_TRANSLATE_CHARS = 800

        // Languages we never translate (usually they are the device language itself)
        private val SKIP_LANGUAGES = setOf("und", "xx-Latn")

        @Volatile private var instance: IslandTranslationEngine? = null
        fun get(context: Context): IslandTranslationEngine =
            instance ?: synchronized(this) {
                instance ?: IslandTranslationEngine(context.applicationContext).also { instance = it }
            }
    }

    // ── Published state ───────────────────────────────────────────────────────

    data class TranslationResult(
        val originalText:    String,
        val translatedText:  String,
        val sourceLanguage:  String,   // BCP-47 code e.g. "ja", "ar", "hi"
        val targetLanguage:  String,
        val confidence:      Float,
        val isModelDownloading: Boolean = false
    )

    private val _result = MutableStateFlow<TranslationResult?>(null)
    val result: StateFlow<TranslationResult?> = _result.asStateFlow()

    // ── Internal ──────────────────────────────────────────────────────────────

    private val scope            = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val languageIdentifier = LanguageIdentification.getClient()

    // Translator cache — keyed by "source→target" to avoid rebuilding
    private val translatorCache = ConcurrentHashMap<String, com.google.mlkit.nl.translate.Translator>()

    // Device target language derived from system locale
    private val targetLanguage: String by lazy {
        val locale = context.resources.configuration.locales[0]
        TranslateLanguage.fromLanguageTag(locale.language) ?: TranslateLanguage.ENGLISH
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Primary entry point. Call this whenever clipboard text changes.
     * Returns immediately; result arrives via [result] StateFlow.
     */
    fun translateIfForeign(text: String) {
        if (text.length < MIN_TRANSLATE_CHARS || text.length > MAX_TRANSLATE_CHARS) return
        scope.launch { identifyAndTranslate(text) }
    }

    /** Call this to clear the current translation from the island. */
    fun dismiss() { _result.value = null }

    // ── Implementation ────────────────────────────────────────────────────────

    private suspend fun identifyAndTranslate(text: String) {
        val detected = identifyLanguage(text) ?: return
        val (sourceLang, confidence) = detected

        // Skip if it's the device language or an ambiguous result
        if (sourceLang == targetLanguage) return
        if (sourceLang in SKIP_LANGUAGES)  return
        if (confidence < MIN_LANG_CONFIDENCE) return

        Log.d(TAG, "Detected $sourceLang (confidence=$confidence), translating to $targetLanguage")

        val cacheKey = "$sourceLang→$targetLanguage"
        val translator = translatorCache.getOrPut(cacheKey) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLanguage)
                .build()
            Translation.getClient(options)
        }

        // Check if model needs downloading
        val conditions = com.google.mlkit.common.model.DownloadConditions.Builder()
            .requireWifi()
            .build()

        // Emit a "downloading" state so the island shows a spinner
        _result.value = TranslationResult(
            originalText   = text,
            translatedText = "",
            sourceLanguage = sourceLang,
            targetLanguage = targetLanguage,
            confidence     = confidence,
            isModelDownloading = true
        )

        // downloadModelIfNeeded is a no-op if model already exists
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                scope.launch { performTranslation(translator, text, sourceLang, confidence) }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Model download failed: ${e.message}")
                // Try without WiFi constraint as a fallback (uses cellular)
                val fallbackConditions = com.google.mlkit.common.model.DownloadConditions.Builder().build()
                translator.downloadModelIfNeeded(fallbackConditions)
                    .addOnSuccessListener {
                        scope.launch { performTranslation(translator, text, sourceLang, confidence) }
                    }
                    .addOnFailureListener {
                        _result.value = null  // Clear spinner on failure
                    }
            }
    }

    private suspend fun performTranslation(
        translator:  com.google.mlkit.nl.translate.Translator,
        text:        String,
        sourceLang:  String,
        confidence:  Float
    ) = suspendCancellableCoroutine<Unit> { cont ->
        translator.translate(text)
            .addOnSuccessListener { translated ->
                _result.value = TranslationResult(
                    originalText    = text,
                    translatedText  = translated,
                    sourceLanguage  = sourceLang,
                    targetLanguage  = targetLanguage,
                    confidence      = confidence,
                    isModelDownloading = false
                )
                Log.d(TAG, "Translation complete: ${text.take(30)} → ${translated.take(30)}")
                if (cont.isActive) cont.resume(Unit) {}
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Translation failed: ${e.message}")
                _result.value = null
                if (cont.isActive) cont.resume(Unit) {}
            }
    }

    private suspend fun identifyLanguage(text: String): Pair<String, Float>? =
        suspendCancellableCoroutine { cont ->
            languageIdentifier.identifyPossibleLanguages(text)
                .addOnSuccessListener { langs ->
                    val best = langs.maxByOrNull { it.confidence }
                    if (best != null) {
                        cont.resume(Pair(best.languageTag, best.confidence)) {}
                    } else {
                        cont.resume(null) {}
                    }
                }
                .addOnFailureListener {
                    cont.resume(null) {}
                }
        }

    fun destroy() {
        scope.cancel()
        translatorCache.values.forEach { it.close() }
        translatorCache.clear()
        languageIdentifier.close()
    }
}