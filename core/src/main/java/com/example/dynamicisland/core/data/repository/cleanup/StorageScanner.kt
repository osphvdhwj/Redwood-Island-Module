package com.example.dynamicisland.core.data.repository.cleanup

import android.os.Environment
import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.util.RedwoodLogger
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/**
 * 🔍 STORAGE SCANNER
 *
 * Scans the filesystem for large files and unused APKs.
 * Optimized for low-RAM environments (Termux/Android).
 */
@Singleton
class StorageScanner @Inject constructor(
    private val dispatchers: DispatcherProvider
) {
    private val TAG = "StorageScanner"
    private val LARGE_FILE_THRESHOLD = 50 * 1024 * 1024 // 50MB

    /**
     * Data class representing a junk/large file finding.
     */
    data class ScanResult(
        val file: File,
        val sizeBytes: Long,
        val isApk: Boolean
    )

    /**
     * Performs a deep scan of the primary external storage.
     */
    suspend fun performDeepScan(): List<ScanResult> = withContext(dispatchers.io()) {
        val results = mutableListOf<ScanResult>()
        try {
            val root = Environment.getExternalStorageDirectory()
            RedwoodLogger.d(TAG, "Starting deep scan at: ${root.absolutePath}")
            
            // Recursive scan with depth limit to prevent context explosion
            scanDirectory(root, results, 0)
            
            RedwoodLogger.d(TAG, "Scan complete. Found ${results.size} targets.")
        } catch (e: Exception) {
            RedwoodLogger.e(TAG, "Scan failed: ${e.message}")
        }
        return@withContext results
    }

    private fun scanDirectory(dir: File, results: MutableList<ScanResult>, depth: Int) {
        if (depth > 5) return // Safety depth limit
        
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                // Skip Android/data and hidden folders for speed
                if (file.name == "Android" || file.name.startsWith(".")) continue
                scanDirectory(file, results, depth + 1)
            } else {
                val size = file.length()
                val isApk = file.extension.lowercase() == "apk"
                
                if (size > LARGE_FILE_THRESHOLD || isApk) {
                    results.add(ScanResult(file, size, isApk))
                }
            }
        }
    }
}
