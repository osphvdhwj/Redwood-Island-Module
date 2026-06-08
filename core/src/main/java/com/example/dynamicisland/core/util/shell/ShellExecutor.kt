package com.example.dynamicisland.core.util.shell

/**
 * STAFF-LEVEL SHELL ABSTRACTION
 *
 * Decouples kernel/system command execution from business logic.
 * Enables unit testing of OEM-level features without requiring root access during tests.
 */
interface ShellExecutor {
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
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
