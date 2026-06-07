package com.example.dynamicisland.core.manager

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import com.example.dynamicisland.shared.model.*
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IslandDownloadMonitor @Inject constructor(
    private val context: Context
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var monitoringJob: Job? = null

    fun startMonitoring(scope: CoroutineScope, onUpdate: (LiveActivityModel.OngoingTask?) -> Unit) {
        if (monitoringJob?.isActive == true) return
        
        monitoringJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val query = DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_RUNNING)
                val cursor = downloadManager.query(query)
                
                if (cursor != null && cursor.moveToFirst()) {
                    val id = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
                    val title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
                    val soFar = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    
                    val progress = if (total > 0) (soFar.toFloat() / total.toFloat()) else 0f
                    
                    withContext(Dispatchers.Main) {
                        onUpdate(
                            LiveActivityModel.OngoingTask(
                                id = "dl_$id",
                                type = ActivityType.ONGOING_TASK,
                                pkgName = "com.android.providers.downloads",
                                title = "Downloading...",
                                text = title,
                                progress = progress.toInt(),
                                progressMax = 100,
                                networkSpeed = null
                            )
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) { onUpdate(null) }
                }
                cursor?.close()
                delay(2000)
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
}
