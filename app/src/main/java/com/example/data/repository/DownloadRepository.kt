package com.example.data.repository

import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

class DownloadRepository(private val downloadDao: DownloadDao) {

    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    val activeDownloads: Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()

    suspend fun getDownload(id: Long): DownloadEntity? = downloadDao.getDownloadById(id)

    fun observeDownload(id: Long): Flow<DownloadEntity?> = downloadDao.observeDownloadById(id)

    suspend fun insert(download: DownloadEntity): Long = downloadDao.insert(download)

    suspend fun update(download: DownloadEntity) = downloadDao.update(download)

    suspend fun updateStatus(id: Long, status: DownloadStatus) = downloadDao.updateStatus(id, status)

    suspend fun delete(id: Long) {
        val item = downloadDao.getDownloadById(id)
        if (item != null && item.localFilePath.isNotEmpty()) {
            runCatching {
                val file = File(item.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
        downloadDao.deleteById(id)
    }

    suspend fun clearAll() {
        downloadDao.clearAll()
    }
}
