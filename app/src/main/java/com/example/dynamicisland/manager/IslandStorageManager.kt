package com.example.dynamicisland.manager
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView

import android.content.ClipData
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StashedItem(
    val id: String,
    val type: StashType,
    val content: String, // Text content or File path
    val timestamp: Long = System.currentTimeMillis()
)

enum class StashType { TEXT, IMAGE, FILE }

class IslandStorageManager(private val context: Context) {

    private val _stashHistory = MutableStateFlow<List<StashedItem>>(emptyList())
    val stashHistory: StateFlow<List<StashedItem>> = _stashHistory.asStateFlow()

    // Creates a dedicated "IslandArchive" folder in your main Downloads directory
    private val archiveDir by lazy {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IslandArchive")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    suspend fun swallowDroppedData(clipData: ClipData): Boolean = withContext(Dispatchers.IO) {
        try {
            var savedAnything = false
            val newItems = mutableListOf<StashedItem>()

            for (i in 0 until clipData.itemCount) {
                val item = clipData.getItemAt(i)

                // Scenario A: It's a physical file or image (URI)
                if (item.uri != null) {
                    val savedFile = saveUriToFile(item.uri)
                    if (savedFile != null) {
                        val type = if (context.contentResolver.getType(item.uri)?.startsWith("image") == true) 
                            StashType.IMAGE else StashType.FILE
                        newItems.add(StashedItem(id = savedFile.name, type = type, content = savedFile.absolutePath))
                        savedAnything = true
                    }
                } 
                // Scenario B: It's text or a Web URL
                else if (!item.text.isNullOrEmpty()) {
                    val text = item.text.toString()
                    val savedNote = saveTextAsNote(text)
                    if (savedNote != null) {
                        newItems.add(StashedItem(id = savedNote.name, type = StashType.TEXT, content = text))
                        savedAnything = true
                    }
                }
            }

            if (newItems.isNotEmpty()) {
                _stashHistory.value = (newItems + _stashHistory.value).take(20) // Keep last 20
            }

            return@withContext savedAnything
        } catch (e: Throwable) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun saveUriToFile(uri: Uri): File? {
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
            destFile
        } catch (e: Throwable) { null }
    }

    private fun saveTextAsNote(text: String): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val snippet = text.take(15).replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "Note_${snippet}_$timestamp.txt"
            
            val destFile = File(archiveDir, fileName)
            destFile.writeText(text)
            destFile
        } catch (e: Throwable) { null }
    }
}
