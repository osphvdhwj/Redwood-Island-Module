package com.example.dynamicisland.core.util.shell

/**
 * STAFF-LEVEL SHELL ABSTRACTION
 *
 * Decouples kernel/system command execution from business logic.
 * Enables unit testing of OEM-level features without requiring root access during tests.
 */
interface ShellExecutor {
    /** Executes a command with root privileges (su -c). */
    fun executeRoot(command: String): Boolean

    /** Executes a standard shell command (sh -c). */
    fun execute(command: String): Boolean
}

class AndroidShellExecutor : ShellExecutor {
    override fun executeRoot(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    override fun execute(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
