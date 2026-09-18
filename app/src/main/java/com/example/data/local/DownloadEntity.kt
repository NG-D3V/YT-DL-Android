package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadFormat
import com.example.data.model.DownloadStatus
import com.example.data.model.VideoQuality

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: String,
    val title: String,
    val author: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val sourceUrl: String,
    val directStreamUrl: String,
    val format: DownloadFormat,
    val quality: VideoQuality,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f, // 0.0 to 1.0
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val speedBytesPerSec: Long = 0,
    val etaSeconds: Long = 0,
    val localFilePath: String = "",
    val fileSize: Long = 0,
    val mimeType: String = "",
    val errorMessage: String? = null,
    val backendEngine: String = "yt-dlp v2026.09 (Local)",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
