package com.example.dynamicisland.core.experimental

import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*

class MorseCodeInputHandler {
    private val morseMap = mapOf(
        ".-" to 'A', "-..." to 'B', "-.-." to 'C', "-.." to 'D',
        "." to 'E', "..-." to 'F', "--." to 'G', "...." to 'H',
        ".." to 'I', ".---" to 'J', "-.-" to 'K', ".-.." to 'L',
        "--" to 'M', "-." to 'N', "---" to 'O', ".--." to 'P',
        "--.-" to 'Q', ".-." to 'R', "..." to 'S', "-" to 'T',
        "..-" to 'U', "...-" to 'V', ".--" to 'W', "-..-" to 'X',
        "-.--" to 'Y', "--.." to 'Z',
        "-----" to '0', ".----" to '1', "..---" to '2', "...--" to '3',
        "....-" to '4', "....." to '5', "-...." to '6', "--..." to '7',
        "---.." to '8', "----." to '9'
    )

    private var currentSequence = ""
    private var lastPressTime = 0L
    private var output = StringBuilder()

    fun processPress(duration: Long): Char? {
        val now = System.currentTimeMillis()

        // If gap after previous press > 700 ms, finish current letter
        if (now - lastPressTime > 700 && currentSequence.isNotEmpty()) {
            val char = morseMap[currentSequence]
            currentSequence = ""
            if (char != null) {
                output.append(char)
                return char
            }
        }

        // Determine dot or dash
        currentSequence += if (duration < 200) "." else "-"
        lastPressTime = now

        // If gap > 1500 ms, treat as word separator
        if (now - lastPressTime > 1500) {
            output.append(" ")
        }

        return null
    }

    fun getText(): String = output.toString()

    fun reset() {
        currentSequence = ""
        output.clear()
        lastPressTime = 0L
    }
}
