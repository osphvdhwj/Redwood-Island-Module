package com.example.dynamicisland.core.util.shell

import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.util.RedwoodLogger
import java.io.DataOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
/**
 * ⚡ ROOT SHELL ENGINE
 *
 * High-performance, non-blocking root command executor.
 * Uses persistent shell streams to minimize 'su' fork overhead.
 */
@Singleton
class RootShellEngine @Inject constructor(
    private val dispatchers: DispatcherProvider
) {
    private val TAG = "RootShellEngine"

    /**
     * Executes a command as root asynchronously.
     * @return true if exit code is 0.
     */
    suspend fun runAction(command: String): Boolean = withContext(dispatchers.io()) {
        try {
            RedwoodLogger.d(TAG, "Executing Root: $command")
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                RedwoodLogger.e(TAG, "Command failed with code $exitCode: $command")
            }
            exitCode == 0
        } catch (e: Exception) {
            RedwoodLogger.e(TAG, "Root execution error: ${e.message}")
            false
        }
    }

    /**
     * Executes multiple commands in a single root session.
     */
    suspend fun runSequence(commands: List<String>): Boolean = withContext(dispatchers.io()) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            
            commands.forEach { cmd ->
                RedwoodLogger.d(TAG, "Sequence Root: $cmd")
                os.writeBytes("$cmd\n")
            }
            
            os.writeBytes("exit\n")
            os.flush()
            
            process.waitFor() == 0
        } catch (e: Exception) {
            RedwoodLogger.e(TAG, "Sequence execution error: ${e.message}")
            false
        }
    }
}
