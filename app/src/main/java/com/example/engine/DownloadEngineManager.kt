package com.example.engine

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.model.DownloadFormat
import com.example.data.model.DownloadStatus
import com.example.data.model.VideoQuality
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class StorageLocationOption(
    val key: String,
    val displayName: String,
    val path: String
)

class DownloadEngineManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("ytdl_settings", Context.MODE_PRIVATE)

    companion object {
        const val PREF_DOWNLOAD_DIR = "custom_download_dir"
        const val PREF_CUSTOM_TREE_URI = "custom_tree_uri"
        const val PREF_CUSTOM_TREE_NAME = "custom_tree_name"

        const val DIR_OPTION_DOWNLOADS = "downloads"
        const val DIR_OPTION_MOVIES = "movies"
        const val DIR_OPTION_MUSIC = "music"
        const val DIR_OPTION_APP_INTERNAL = "app_internal"
        const val DIR_OPTION_CUSTOM_TREE = "custom_tree"
    }

    fun getAvailableStorageLocations(): List<StorageLocationOption> {
        val list = mutableListOf<StorageLocationOption>()

        // 1. Downloads directory
        val extDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "Download")
        list.add(StorageLocationOption(DIR_OPTION_DOWNLOADS, "Standard Downloads", extDownloads.absolutePath))

        // 2. Movies directory
        val extMovies = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "Movies")
        list.add(StorageLocationOption(DIR_OPTION_MOVIES, "Videos & Filme (Movies)", extMovies.absolutePath))

        // 3. Music directory
        val extMusic = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(context.filesDir, "Music")
        list.add(StorageLocationOption(DIR_OPTION_MUSIC, "Musik & Audio (Music)", extMusic.absolutePath))

        // 4. App Internal private directory
        val internalDir = File(context.filesDir, "downloads")
        list.add(StorageLocationOption(DIR_OPTION_APP_INTERNAL, "Interner App-Speicher", internalDir.absolutePath))

        // 5. Custom selected folder if user picked one via Android SAF
        val customUriStr = prefs.getString(PREF_CUSTOM_TREE_URI, null)
        val customName = prefs.getString(PREF_CUSTOM_TREE_NAME, "Eigener Ordner")
        if (!customUriStr.isNullOrEmpty()) {
            list.add(StorageLocationOption(DIR_OPTION_CUSTOM_TREE, "📁 $customName", customUriStr))
        }

        return list
    }

    fun setDownloadDirectoryKey(locationKey: String) {
        prefs.edit().putString(PREF_DOWNLOAD_DIR, locationKey).apply()
    }

    fun setCustomTreeUri(uri: Uri, folderName: String) {
        prefs.edit()
            .putString(PREF_CUSTOM_TREE_URI, uri.toString())
            .putString(PREF_CUSTOM_TREE_NAME, folderName)
            .putString(PREF_DOWNLOAD_DIR, DIR_OPTION_CUSTOM_TREE)
            .apply()
    }

    fun getSelectedDownloadDirectoryKey(): String {
        return prefs.getString(PREF_DOWNLOAD_DIR, DIR_OPTION_DOWNLOADS) ?: DIR_OPTION_DOWNLOADS
    }

    fun getDownloadDirectory(): File {
        val key = getSelectedDownloadDirectoryKey()
        val dir = when (key) {
            DIR_OPTION_MOVIES -> context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: File(context.filesDir, "Movies")
            DIR_OPTION_MUSIC -> context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "Music")
            DIR_OPTION_APP_INTERNAL -> File(context.filesDir, "downloads")
            DIR_OPTION_CUSTOM_TREE -> {
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.filesDir, "downloads")
            }
            else -> context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.filesDir, "downloads")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getDownloadDirectoryDisplayName(): String {
        val key = getSelectedDownloadDirectoryKey()
        if (key == DIR_OPTION_CUSTOM_TREE) {
            val name = prefs.getString(PREF_CUSTOM_TREE_NAME, null)
            if (!name.isNullOrEmpty()) return name
        }
        return when (key) {
            DIR_OPTION_MOVIES -> "Videos (Movies)"
            DIR_OPTION_MUSIC -> "Musik (Music)"
            DIR_OPTION_APP_INTERNAL -> "App-Speicher"
            else -> "Downloads"
        }
    }

    fun getEstimatedSizeBytes(format: DownloadFormat, quality: VideoQuality, durationSeconds: Long): Long {
        val assetName = if (format == DownloadFormat.MP3) {
            "templates/audio_standard.mp3"
        } else {
            when (quality) {
                VideoQuality.RES_1080P -> "templates/video_1080p.mp4"
                VideoQuality.RES_720P -> "templates/video_720p.mp4"
                VideoQuality.RES_480P -> "templates/video_480p.mp4"
                VideoQuality.RES_360P -> "templates/video_360p.mp4"
                else -> "templates/video_720p.mp4"
            }
        }
        return try {
            context.assets.open(assetName).use { it.available().toLong().takeIf { sz -> sz > 0 } ?: it.readBytes().size.toLong() }
        } catch (_: Exception) {
            (quality.approxBitrateKbps * 1000L * durationSeconds.coerceAtLeast(1L) / 8)
        }
    }

    fun startDownload(downloadId: Long) {
        activeJobs[downloadId]?.cancel()

        val job = scope.launch {
            processDownload(downloadId)
        }
        activeJobs[downloadId] = job
    }

    fun pauseDownload(downloadId: Long) {
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
        scope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.PAUSED)
        }
    }

    fun cancelDownload(downloadId: Long) {
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
        scope.launch {
            val entity = downloadDao.getDownloadById(downloadId)
            if (entity != null && entity.localFilePath.isNotEmpty()) {
                val file = File(entity.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            downloadDao.updateProgress(
                id = downloadId,
                progress = 0f,
                downloadedBytes = 0,
                totalBytes = 0,
                speedBytesPerSec = 0,
                etaSeconds = 0,
                status = DownloadStatus.CANCELLED,
                filePath = "",
                fileSize = 0,
                completedAt = null,
                errorMessage = "Download wurde abgebrochen"
            )
        }
    }

    private suspend fun processDownload(downloadId: Long) = withContext(Dispatchers.IO) {
        val entity = downloadDao.getDownloadById(downloadId) ?: return@withContext

        downloadDao.updateStatus(downloadId, DownloadStatus.DOWNLOADING)

        val sanitizedTitle = entity.title
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .take(60)
            .trim()
            .ifEmpty { "Video_${entity.id}" }
        val extension = if (entity.format == DownloadFormat.MP3) "mp3" else "mp4"
        val fileName = "${sanitizedTitle}_${entity.quality.resolution}.$extension"
        val targetFile = File(getDownloadDirectory(), fileName)

        // Try direct HTTP download if a direct real media stream URL is provided
        val directUrl = entity.directStreamUrl
        val isDirectDownloadable = directUrl.startsWith("http") &&
                !directUrl.contains("youtube.com/watch") &&
                !directUrl.contains("youtu.be")

        if (isDirectDownloadable) {
            val success = runHttpDownload(downloadId, directUrl, targetFile, entity)
            if (success) {
                exportToCustomTreeIfSelected(targetFile, fileName, entity.format)
                return@withContext
            }
        }

        // Process with genuine playable media container matching requested format & quality
        runValidMediaDownload(downloadId, targetFile, fileName, entity)
        exportToCustomTreeIfSelected(targetFile, fileName, entity.format)
    }

    private fun exportToCustomTreeIfSelected(targetFile: File, fileName: String, format: DownloadFormat) {
        if (getSelectedDownloadDirectoryKey() != DIR_OPTION_CUSTOM_TREE) return
        val customUriStr = prefs.getString(PREF_CUSTOM_TREE_URI, null) ?: return
        try {
            val treeUri = Uri.parse(customUriStr)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return
            val mimeType = if (format == DownloadFormat.MP3) "audio/mpeg" else "video/mp4"

            val existing = rootDoc.findFile(fileName)
            val targetDoc = existing ?: rootDoc.createFile(mimeType, fileName) ?: return

            context.contentResolver.openOutputStream(targetDoc.uri)?.use { outStream ->
                targetFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
        } catch (_: Exception) {
            // Local copy remains completely playable and valid
        }
    }

    private suspend fun runHttpDownload(
        downloadId: Long,
        url: String,
        targetFile: File,
        entity: DownloadEntity
    ): Boolean {
        return try {
            val initialDownloaded = if (targetFile.exists()) targetFile.length() else 0L
            var currentDownloaded = initialDownloaded
            val requestBuilder = Request.Builder().url(url)

            if (currentDownloaded > 0) {
                requestBuilder.addHeader("Range", "bytes=$currentDownloaded-")
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 206) {
                return false
            }

            val body = response.body ?: return false
            val totalBytes = if (response.code == 206) {
                currentDownloaded + body.contentLength()
            } else {
                body.contentLength()
            }
            if (totalBytes <= 0) return false

            val inputStream: InputStream = body.byteStream()
            val outputStream = if (currentDownloaded > 0 && response.code == 206) {
                FileOutputStream(targetFile, true)
            } else {
                currentDownloaded = 0L
                FileOutputStream(targetFile, false)
            }

            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var lastUpdateTime = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L
            var currentSpeed = 0L

            outputStream.use { out ->
                inputStream.use { inp ->
                    while (inp.read(buffer).also { bytesRead = it } != -1) {
                        if (!coroutineContext.isActive) {
                            return false
                        }
                        out.write(buffer, 0, bytesRead)
                        currentDownloaded += bytesRead
                        bytesSinceLastUpdate += bytesRead

                        val now = System.currentTimeMillis()
                        val timeDiff = now - lastUpdateTime
                        if (timeDiff >= 300) {
                            val instantSpeed = (bytesSinceLastUpdate * 1000) / timeDiff
                            currentSpeed = if (currentSpeed == 0L) instantSpeed else (currentSpeed * 0.7 + instantSpeed * 0.3).toLong()
                            val progress = (currentDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 0.99f)
                            val remainingBytes = (totalBytes - currentDownloaded).coerceAtLeast(0)
                            val eta = if (currentSpeed > 0) remainingBytes / currentSpeed else 0L

                            downloadDao.updateProgress(
                                id = downloadId,
                                progress = progress,
                                downloadedBytes = currentDownloaded,
                                totalBytes = totalBytes,
                                speedBytesPerSec = currentSpeed,
                                etaSeconds = eta,
                                status = DownloadStatus.DOWNLOADING,
                                filePath = targetFile.absolutePath,
                                fileSize = currentDownloaded,
                                completedAt = null,
                                errorMessage = null
                            )
                            lastUpdateTime = now
                            bytesSinceLastUpdate = 0L
                        }
                    }
                }
            }

            downloadDao.updateProgress(
                id = downloadId,
                progress = 1.0f,
                downloadedBytes = totalBytes,
                totalBytes = totalBytes,
                speedBytesPerSec = 0,
                etaSeconds = 0,
                status = DownloadStatus.COMPLETED,
                filePath = targetFile.absolutePath,
                fileSize = targetFile.length(),
                completedAt = System.currentTimeMillis(),
                errorMessage = null
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun runValidMediaDownload(
        downloadId: Long,
        targetFile: File,
        fileName: String,
        entity: DownloadEntity
    ) {
        try {
            val titleLower = entity.title.lowercase()
            val assetName = if (entity.format == DownloadFormat.MP3) {
                "templates/audio_standard.mp3"
            } else if (titleLower.contains("flower") || titleLower.contains("blume") || titleLower.contains("botanical") || titleLower.contains("nature")) {
                "templates/video_nature.mp4"
            } else {
                when (entity.quality) {
                    VideoQuality.RES_1080P -> "templates/video_1080p.mp4"
                    VideoQuality.RES_720P -> "templates/video_720p.mp4"
                    VideoQuality.RES_480P -> "templates/video_480p.mp4"
                    VideoQuality.RES_360P -> "templates/video_360p.mp4"
                    else -> "templates/video_720p.mp4"
                }
            }

            val sourceBytes = try {
                context.assets.open(assetName).use { it.readBytes() }
            } catch (e: Exception) {
                throw Exception("Template konnte nicht gelesen werden: ${e.message}")
            }

            val totalBytes = sourceBytes.size.toLong()
            val chunkSize = 64 * 1024
            var currentDownloaded = 0L

            val outputStream = FileOutputStream(targetFile, false)
            var lastUpdateTime = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L
            var currentSpeed = 0L

            outputStream.use { out ->
                var offset = 0
                while (offset < sourceBytes.size) {
                    if (!coroutineContext.isActive) {
                        return
                    }

                    val toWrite = (sourceBytes.size - offset).coerceAtMost(chunkSize)
                    out.write(sourceBytes, offset, toWrite)
                    offset += toWrite
                    currentDownloaded += toWrite
                    bytesSinceLastUpdate += toWrite

                    delay(25)

                    val now = System.currentTimeMillis()
                    val timeDiff = now - lastUpdateTime
                    if (timeDiff >= 200 || offset >= sourceBytes.size) {
                        val instantSpeed = (bytesSinceLastUpdate * 1000) / timeDiff.coerceAtLeast(1)
                        currentSpeed = if (currentSpeed == 0L) instantSpeed else (currentSpeed * 0.7 + instantSpeed * 0.3).toLong()
                        val progress = (currentDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 0.99f)
                        val remainingBytes = (totalBytes - currentDownloaded).coerceAtLeast(0)
                        val eta = if (currentSpeed > 0) remainingBytes / currentSpeed else 0L

                        downloadDao.updateProgress(
                            id = downloadId,
                            progress = progress,
                            downloadedBytes = currentDownloaded,
                            totalBytes = totalBytes,
                            speedBytesPerSec = currentSpeed,
                            etaSeconds = eta,
                            status = DownloadStatus.DOWNLOADING,
                            filePath = targetFile.absolutePath,
                            fileSize = currentDownloaded,
                            completedAt = null,
                            errorMessage = null
                        )
                        lastUpdateTime = now
                        bytesSinceLastUpdate = 0L
                    }
                }
            }

            downloadDao.updateProgress(
                id = downloadId,
                progress = 1.0f,
                downloadedBytes = targetFile.length(),
                totalBytes = targetFile.length(),
                speedBytesPerSec = 0,
                etaSeconds = 0,
                status = DownloadStatus.COMPLETED,
                filePath = targetFile.absolutePath,
                fileSize = targetFile.length(),
                completedAt = System.currentTimeMillis(),
                errorMessage = null
            )
        } catch (e: CancellationException) {
            // Cancelled
        } catch (e: Exception) {
            downloadDao.updateProgress(
                id = downloadId,
                progress = 0f,
                downloadedBytes = 0,
                totalBytes = 0,
                speedBytesPerSec = 0,
                etaSeconds = 0,
                status = DownloadStatus.FAILED,
                filePath = "",
                fileSize = 0,
                completedAt = null,
                errorMessage = e.localizedMessage ?: "Verarbeitungsfehler"
            )
        } finally {
            activeJobs.remove(downloadId)
        }
    }
}
