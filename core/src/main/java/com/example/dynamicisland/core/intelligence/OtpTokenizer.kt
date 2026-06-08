package com.example.dynamicisland.core.intelligence

import android.util.Log
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

/**
 * BATCH 2: Multilingual OTP Tokenizer
 *
 * Replaces the fragile single-regex approach in SystemEventsHook with
 * a proper sentence-understanding pipeline that handles:
 *
 *   English:
 *     "Your verification code is 492817"
 *     "Use code: 4-2-9-1 to log in"
 *     "OTP: 8 8 1 4 (do not share)"
 *     "4491 is your WhatsApp code"
 *
 *   Hindi (Devanagari):
 *     "आपका OTP है: ५६७८"
 *     "सत्यापन कोड ४४९१ है"
 *     "आपका एकबारगी पासवर्ड 7823 है"
 *
 *   Hinglish (mixed):
 *     "Aapka OTP 4421 hai"
 *     "Your code hai 8812"
 *
 *   Urdu-influenced patterns:
 *     "Aapka password ek baar istemal karein: 5519"
 *
 * Architecture:
 *   Raw text → Normalizer → Tokenizer → CandidateFinder → ContextScorer → Result
 *
 * The ContextScorer is the key innovation — it looks at the tokens AROUND a
 * number to decide if that number is an OTP code or just a phone number,
 * amount, or date. This eliminates the false positives that plague regex-only
 * approaches.
 *
 * Confidence scoring:
 *   - A code preceded by an OTP keyword scores 0.95+
 *   - A 6-digit code in a 2FA-style message scores 0.80+
 *   - An isolated number without context scores <0.50 (rejected)
 */
object OtpTokenizer {

    private const val TAG = "OtpTokenizer"
    private const val MIN_CONFIDENCE = 0.55f

    // -------------------------------------------------------------------------
    // Multilingual keyword tables
    // -------------------------------------------------------------------------

    // English OTP signal words — any of these near a number → strong OTP signal
    private val EN_OTP_KEYWORDS = setOf(
        "otp", "code", "pin", "password", "passcode", "verification",
        "verify", "authenticate", "auth", "token", "secret",
        "one-time", "onetime", "2fa", "mfa", "login", "sign in", "signin"
    )

    // English context words that strengthen the signal (near but not adjacent)
    private val EN_CONTEXT_WORDS = setOf(
        "enter", "use", "your", "temporary", "secure", "expires",
        "valid", "do not share", "share", "account", "access"
    )

    // Hindi/Devanagari OTP keywords
    private val HI_OTP_KEYWORDS = setOf(
        "ओटीपी", "कोड", "पासवर्ड", "सत्यापन", "सत्यापित",
        "एकबारगी", "पासकोड", "जांच", "अधिकृत", "लॉगिन"
    )

    // Hinglish (romanized Hindi) OTP keywords
    private val HINGLISH_OTP_KEYWORDS = setOf(
        "otp", "code", "password", "passcode", "satya", "sapat",
        "istemal", "login", "darj", "prastut", "samstha"
    )

    // Words that REDUCE confidence — this number is probably not an OTP
    private val NEGATION_WORDS = setOf(
        "rs", "inr", "₹", "rupee", "rupees", "amount", "paid", "payment",
        "balance", "debit", "credit", "account", "ac", "a/c", "no.", "no ",
        "number", "phone", "mobile", "call", "tel", "date", "time",
        "order", "invoice", "bill", "zip", "postal", "pin code",
        "price", "total", "tax", "gst", "mrp", "cost"
    )

    // -------------------------------------------------------------------------
    // Devanagari → ASCII digit mapping
    // -------------------------------------------------------------------------

    private val DEVANAGARI_DIGIT_MAP = mapOf(
        '०' to '0', '१' to '1', '२' to '2', '३' to '3', '४' to '4',
        '५' to '5', '६' to '6', '७' to '7', '८' to '8', '९' to '9'
    )

    // Also handle Arabic-Indic digits (used in some Urdu SMS)
    private val ARABIC_INDIC_DIGIT_MAP = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    data class OtpResult(
        val code: String,               // The extracted OTP digits
        val confidence: Float,          // 0..1
        val language: DetectedLanguage,
        val context: String,            // Surrounding text snippet for display
        val isHighConfidence: Boolean   // Shorthand: confidence > 0.75
    )

    enum class DetectedLanguage { ENGLISH, HINDI, HINGLISH, UNKNOWN }

    // -------------------------------------------------------------------------
    // Main extraction API
    // -------------------------------------------------------------------------

    /**
     * Primary entry point.
     * Pass the raw notification text (title + body concatenated).
     * Returns null if no OTP was found with sufficient confidence.
     */
    fun extract(rawText: String, packageName: String = ""): OtpResult? {
        if (rawText.isBlank()) return null

        // Step 1: Normalize — transliterate non-ASCII digits, collapse whitespace
        val normalized = normalize(rawText)

        // Step 2: Detect language
        val language = detectLanguage(rawText)  // Use original for script detection

        // Step 3: Find all numeric candidates (4–8 digit sequences)
        val candidates = findNumericCandidates(normalized)
        if (candidates.isEmpty()) return null

        // Step 4: Score each candidate by context
        val scored = candidates.map { candidate ->
            val score = scoreCandidate(candidate, normalized, language, packageName)
            candidate to score
        }

        // Step 5: Pick the best candidate above the confidence threshold
        val best = scored.maxByOrNull { it.second } ?: return null
        val (candidate, confidence) = best

        if (confidence < MIN_CONFIDENCE) {
            Log.d(TAG, "Best candidate '${candidate.digits}' scored $confidence — below threshold, ignoring")
            return null
        }

        // Step 6: Extract a clean context snippet for the UI
        val contextSnippet = buildContextSnippet(normalized, candidate)

        return OtpResult(
            code = candidate.digits,
            confidence = confidence,
            language = language,
            context = contextSnippet,
            isHighConfidence = confidence > 0.75f
        )
    }

    /**
     * Lightweight check — returns true if the text is likely OTP-related
     * without doing full extraction. Used for fast pre-filtering in the hook.
     */
    fun looksLikeOtpMessage(text: String): Boolean {
        val lower = text.lowercase()
        val hasKeyword = EN_OTP_KEYWORDS.any { lower.contains(it) } ||
                         HI_OTP_KEYWORDS.any { text.contains(it) } ||
                         HINGLISH_OTP_KEYWORDS.any { lower.contains(it) }
        val hasDigitSequence = Regex("\\d{4,8}").containsMatchIn(text) ||
                               "[०-९]{4,8}".toRegex().containsMatchIn(text)
        return hasKeyword && hasDigitSequence
    }

    // -------------------------------------------------------------------------
    // Step 1: Normalizer
    // -------------------------------------------------------------------------

    private data class NormalizedText(
        val text: String,
        val originalToNormalized: Map<Int, Int> = emptyMap()
    )

    private fun normalize(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when {
                DEVANAGARI_DIGIT_MAP.containsKey(ch) -> sb.append(DEVANAGARI_DIGIT_MAP[ch])
                ARABIC_INDIC_DIGIT_MAP.containsKey(ch) -> sb.append(ARABIC_INDIC_DIGIT_MAP[ch])
                // Collapse OTP separators (dash, dot, space between single digits)
                // "4-2-9-1" → "4291", "8 8 1 4" → detected as separated sequence
                else -> sb.append(ch)
            }
        }

        var result = sb.toString()

        // Normalize common SMS abbreviations
        result = result
            .replace(Regex("\\bdo not share\\b", RegexOption.IGNORE_CASE), "donotshare")
            .replace(Regex("one[- ]time", RegexOption.IGNORE_CASE), "onetime")
            .replace(Regex("\\bexp(ires)?\\b", RegexOption.IGNORE_CASE), "expires")

        return result
    }

    // -------------------------------------------------------------------------
    // Step 2: Language detector
    // -------------------------------------------------------------------------

    private fun detectLanguage(original: String): DetectedLanguage {
        val devanagariCount = original.count { it in '\u0900'..'\u097F' }
        val latinCount = original.count { it.isLetter() && it < '\u0100' }

        return when {
            devanagariCount > latinCount / 2 -> DetectedLanguage.HINDI
            devanagariCount > 0              -> DetectedLanguage.HINGLISH
            else -> {
                // Check for Hinglish patterns without Devanagari
                val lower = original.lowercase()
                val hinglishHits = HINGLISH_OTP_KEYWORDS.count { kw ->
                    kw != "otp" && kw != "code" && lower.contains(kw)  // Exclude shared EN/HI words
                }
                if (hinglishHits >= 2) DetectedLanguage.HINGLISH else DetectedLanguage.ENGLISH
            }
        }
    }

    // -------------------------------------------------------------------------
    // Step 3: Candidate finder
    // -------------------------------------------------------------------------

    data class NumericCandidate(
        val digits: String,         // The pure digit string
        val startIndex: Int,        // Position in normalized text
        val endIndex: Int,
        val isSeparated: Boolean,   // True if digits were separated (e.g. "4-2-9-1")
        val windowBefore: String,   // 60 chars before the number
        val windowAfter: String     // 60 chars after the number
    )

    private fun findNumericCandidates(text: String): List<NumericCandidate> {
        val candidates = mutableListOf<NumericCandidate>()

        // Pattern 1: Contiguous digit sequence 4–8 digits
        val contiguousPattern = Regex("\\b(\\d{4,8})\\b")
        contiguousPattern.findAll(text).forEach { match ->
            val digits = match.value
            val start = match.range.first
            val end = match.range.last + 1
            candidates.add(
                NumericCandidate(
                    digits = digits,
                    startIndex = start,
                    endIndex = end,
                    isSeparated = false,
                    windowBefore = text.substring(maxOf(0, start - 60), start).lowercase(),
                    windowAfter = text.substring(end, minOf(text.length, end + 60)).lowercase()
                )
            )
        }

        // Pattern 2: Digit groups separated by dash, dot, or space
        // "4-2-9-1" or "44 91" or "4.4.9.1"
        val separatedPattern = Regex("\\b(\\d[- .]{1,2}){3,7}\\d\\b")
        separatedPattern.findAll(text).forEach { match ->
            val raw = match.value
            val digits = raw.filter { it.isDigit() }
            if (digits.length in 4..8) {
                val start = match.range.first
                val end = match.range.last + 1
                // Avoid double-counting if already captured by contiguous pattern
                val alreadyCaptured = candidates.any { it.startIndex == start }
                if (!alreadyCaptured) {
                    candidates.add(
                        NumericCandidate(
                            digits = digits,
                            startIndex = start,
                            endIndex = end,
                            isSeparated = true,
                            windowBefore = text.substring(maxOf(0, start - 60), start).lowercase(),
                            windowAfter = text.substring(end, minOf(text.length, end + 60)).lowercase()
                        )
                    )
                }
            }
        }

        return candidates
    }

    // -------------------------------------------------------------------------
    // Step 4: Context scorer
    // -------------------------------------------------------------------------

    private fun scoreCandidate(
        candidate: NumericCandidate,
        normalizedText: String,
        language: DetectedLanguage,
        packageName: String
    ): Float {
        var score = 0.30f  // Base score for any 4-8 digit number in an OTP-flagged message

        val before = candidate.windowBefore
        val after = candidate.windowAfter
        val combined = before + after

        // --- Positive signals ---

        // OTP keyword immediately adjacent (within 15 chars) → very strong signal
        val adjacentBefore = before.takeLast(15)
        val adjacentAfter = after.take(15)
        val adjacentCombined = adjacentBefore + adjacentAfter

        val enKeywordAdjacent = EN_OTP_KEYWORDS.any { adjacentCombined.contains(it) }
        val hiKeywordAdjacent = HI_OTP_KEYWORDS.any { normalizedText.contains(it) }
        val hinglishKeywordAdjacent = HINGLISH_OTP_KEYWORDS.any { adjacentCombined.contains(it) }

        if (enKeywordAdjacent || hiKeywordAdjacent || hinglishKeywordAdjacent) {
            score += 0.50f
        }

        // OTP keyword present anywhere in the message (weaker signal)
        val enKeywordPresent = EN_OTP_KEYWORDS.any { combined.contains(it) }
        val hiKeywordPresent = HI_OTP_KEYWORDS.any { normalizedText.contains(it) }
        if ((enKeywordPresent || hiKeywordPresent) && !enKeywordAdjacent && !hiKeywordAdjacent) {
            score += 0.20f
        }

        // Context words boost
        val contextHits = EN_CONTEXT_WORDS.count { combined.contains(it) }
        score += (contextHits * 0.05f).coerceAtMost(0.15f)

        // "do not share" is a very strong OTP signal — banks always write this
        if (combined.contains("donotshare") || combined.contains("do not share")) {
            score += 0.25f
        }

        // Separated digits format ("4-2-9-1") is almost exclusively OTP
        if (candidate.isSeparated) {
            score += 0.20f
        }

        // 6-digit codes are the most common OTP length globally
        if (candidate.digits.length == 6) {
            score += 0.10f
        }

        // Known OTP-sending apps boost
        if (packageName.contains("bank") || packageName.contains("pay") ||
            packageName.contains("wallet") || packageName.contains("upi") ||
            packageName.contains("sms") || packageName.contains("message")) {
            score += 0.10f
        }

        // Colon or "is" pattern: "code: 4921" or "code is 4921"
        val colonPattern = Regex("(?:code|otp|pin|password)[:\\s]+${Regex.escape(candidate.digits)}", RegexOption.IGNORE_CASE)
        if (colonPattern.containsMatchIn(normalizedText)) {
            score += 0.20f
        }

        // Number appears at END of sentence (typical in Indian SMS: "Your OTP is 4921.")
        val afterTrimmed = after.trimStart()
        if (afterTrimmed.startsWith(".") || afterTrimmed.startsWith("।") ||  // Devanagari full stop
            afterTrimmed.startsWith("hai") || afterTrimmed.isEmpty()) {
            score += 0.10f
        }

        // --- Negative signals ---

        // Financial/non-OTP indicators near the number
        val negationHits = NEGATION_WORDS.count { combined.contains(it) }
        score -= (negationHits * 0.12f).coerceAtMost(0.35f)

        // Very long digit strings are probably account/phone numbers, not OTPs
        if (candidate.digits.length > 8) {
            score -= 0.40f
        }

        // If the number is a round number (e.g. 5000, 10000), probably an amount
        val asInt = candidate.digits.toLongOrNull()
        if (asInt != null && asInt % 100 == 0L && candidate.digits.length >= 4) {
            score -= 0.20f
        }

        // Date-like patterns: "12/04", "04-2024" — 4-digit year or day/month
        val yearPattern = Regex("\\b(19|20)\\d{2}\\b")
        if (yearPattern.matches(candidate.digits)) {
            score -= 0.30f
        }

        return score.coerceIn(0f, 1f)
    }

    // -------------------------------------------------------------------------
    // Step 6: Context snippet
    // -------------------------------------------------------------------------

    private fun buildContextSnippet(text: String, candidate: NumericCandidate): String {
        val snippetStart = maxOf(0, candidate.startIndex - 30)
        val snippetEnd = minOf(text.length, candidate.endIndex + 20)
        return text.substring(snippetStart, snippetEnd).trim()
    }
}
