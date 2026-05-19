package com.example.dynamicisland.manager
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView

import android.content.ClipData
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IslandStorageManager(private val context: Context) {

    // Creates a dedicated "IslandArchive" folder in your main Downloads directory
    private val archiveDir by lazy {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IslandArchive")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    suspend fun swallowDroppedData(clipData: ClipData): Boolean = withContext(Dispatchers.IO) {
        try {
            var savedAnything = false
            for (i in 0 until clipData.itemCount) {
                val item = clipData.getItemAt(i)

                // Scenario A: It's a physical file or image (URI)
                if (item.uri != null) {
                    savedAnything = saveUriToFile(item.uri) || savedAnything
                } 
                // Scenario B: It's text or a Web URL
                else if (!item.text.isNullOrEmpty()) {
                    savedAnything = saveTextAsNote(item.text.toString()) || savedAnything
                }
            }
            return@withContext savedAnything
        } catch (e: Throwable) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun saveUriToFile(uri: Uri): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "file"
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Archive_$timestamp.$extension"
            val destFile = File(archiveDir, fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Throwable) { false }
    }

    private fun saveTextAsNote(text: String): Boolean {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val snippet = text.take(15).replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "Note_${snippet}_$timestamp.txt"
            
            val destFile = File(archiveDir, fileName)
            destFile.writeText(text)
            true
        } catch (e: Throwable) { false }
    }
}
