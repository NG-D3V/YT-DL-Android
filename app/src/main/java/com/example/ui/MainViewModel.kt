package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DownloadEntity
import com.example.data.model.DownloadFormat
import com.example.data.model.DownloadStatus
import com.example.data.model.VideoMetadata
import com.example.data.model.VideoQuality
import com.example.data.repository.DownloadRepository
import com.example.engine.DownloadEngineManager
import com.example.engine.StorageLocationOption
import com.example.engine.YtDlpExtractorEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DownloadFilter(val title: String) {
    ALL("Alle"),
    ACTIVE("Aktiv"),
    COMPLETED("Fertig"),
    VIDEOS("MP4"),
    AUDIO("MP3")
}

data class MainUiState(
    val urlInput: String = "",
    val isAnalyzing: Boolean = false,
    val analysisError: String? = null,
    val parsedMetadata: VideoMetadata? = null,
    val selectedFormat: DownloadFormat = DownloadFormat.MP4,
    val selectedQuality: VideoQuality = VideoQuality.RES_720P,
    val filterTab: DownloadFilter = DownloadFilter.ALL,
    val searchQuery: String = "",
    val playingDownload: DownloadEntity? = null,
    val showSettings: Boolean = false,
    val infoMessage: String? = null
)

class MainViewModel(
    private val repository: DownloadRepository,
    private val extractorEngine: YtDlpExtractorEngine,
    private val downloadManager: DownloadEngineManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Observe all downloads from Room
    val downloads: StateFlow<List<DownloadEntity>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered downloads
    val filteredDownloads: StateFlow<List<DownloadEntity>> = combine(
        downloads,
        _uiState
    ) { list, state ->
        var res = list
        // Filter by tab
        res = when (state.filterTab) {
            DownloadFilter.ALL -> res
            DownloadFilter.ACTIVE -> res.filter {
                it.status == DownloadStatus.DOWNLOADING ||
                it.status == DownloadStatus.QUEUED ||
                it.status == DownloadStatus.PAUSED
            }
            DownloadFilter.COMPLETED -> res.filter { it.status == DownloadStatus.COMPLETED }
            DownloadFilter.VIDEOS -> res.filter { it.format == DownloadFormat.MP4 }
            DownloadFilter.AUDIO -> res.filter { it.format == DownloadFormat.MP3 }
        }
        // Filter by search
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim().lowercase()
            res = res.filter { it.title.lowercase().contains(q) || it.author.lowercase().contains(q) }
        }
        res
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onUrlChange(newUrl: String) {
        _uiState.value = _uiState.value.copy(
            urlInput = newUrl,
            analysisError = null
        )
    }

    fun pasteAndAnalyze(url: String) {
        _uiState.value = _uiState.value.copy(urlInput = url, analysisError = null)
        analyzeUrl()
    }

    fun analyzeUrl() {
        val rawUrl = _uiState.value.urlInput.trim()
        if (rawUrl.isEmpty()) {
            _uiState.value = _uiState.value.copy(analysisError = "Bitte eine YouTube-URL eingeben")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, analysisError = null)
            val result = extractorEngine.fetchVideoInfo(rawUrl)
            result.onSuccess { metadata ->
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    parsedMetadata = metadata,
                    analysisError = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    analysisError = "Fehler beim Laden: ${error.localizedMessage ?: "Video nicht gefunden"}"
                )
            }
        }
    }

    fun selectFormat(format: DownloadFormat) {
        val newQuality = if (format == DownloadFormat.MP4) {
            if (_uiState.value.selectedQuality.isAudioOnly) VideoQuality.RES_720P else _uiState.value.selectedQuality
        } else {
            if (!_uiState.value.selectedQuality.isAudioOnly) VideoQuality.AUDIO_320K else _uiState.value.selectedQuality
        }
        _uiState.value = _uiState.value.copy(
            selectedFormat = format,
            selectedQuality = newQuality
        )
    }

    fun selectQuality(quality: VideoQuality) {
        _uiState.value = _uiState.value.copy(selectedQuality = quality)
    }

    fun getEstimatedSizeBytes(format: DownloadFormat, quality: VideoQuality): Long {
        val metadata = _uiState.value.parsedMetadata
        val duration = metadata?.durationSeconds ?: 180L
        val matchedStream = metadata?.availableStreams?.find {
            it.format == format && it.quality == quality
        }
        val isDirectHttp = matchedStream?.directUrl?.startsWith("http") == true &&
                !matchedStream.directUrl.contains("youtube.com") &&
                !matchedStream.directUrl.contains("youtu.be")

        if (isDirectHttp && (matchedStream?.approximateSizeBytes ?: 0L) > 0L) {
            return matchedStream!!.approximateSizeBytes
        }

        return downloadManager.getEstimatedSizeBytes(format, quality, duration)
    }

    fun startDownload() {
        val metadata = _uiState.value.parsedMetadata ?: return
        val format = _uiState.value.selectedFormat
        val quality = _uiState.value.selectedQuality

        viewModelScope.launch {
            // Find stream matching format & quality
            val matchedStream = metadata.availableStreams.find {
                it.format == format && it.quality == quality
            }
            val directStreamUrl = matchedStream?.directUrl ?: metadata.originalUrl
            val isDirectHttp = directStreamUrl.startsWith("http") &&
                    !directStreamUrl.contains("youtube.com") &&
                    !directStreamUrl.contains("youtu.be")

            val approxSize = if (isDirectHttp && (matchedStream?.approximateSizeBytes ?: 0L) > 0L) {
                matchedStream!!.approximateSizeBytes
            } else {
                downloadManager.getEstimatedSizeBytes(format, quality, metadata.durationSeconds)
            }

            val newEntity = DownloadEntity(
                videoId = metadata.id,
                title = metadata.title,
                author = metadata.author,
                durationSeconds = metadata.durationSeconds,
                thumbnailUrl = metadata.thumbnailUrl,
                sourceUrl = metadata.originalUrl,
                directStreamUrl = directStreamUrl,
                format = format,
                quality = quality,
                status = DownloadStatus.QUEUED,
                totalBytes = approxSize,
                mimeType = format.mimeType
            )

            val downloadId = repository.insert(newEntity)
            downloadManager.startDownload(downloadId)

            _uiState.value = _uiState.value.copy(
                infoMessage = "Download gestartet: ${metadata.title.take(30)}...",
                parsedMetadata = null,
                urlInput = ""
            )
        }
    }

    fun pauseDownload(id: Long) {
        downloadManager.pauseDownload(id)
    }

    fun resumeDownload(id: Long) {
        downloadManager.startDownload(id)
    }

    fun cancelDownload(id: Long) {
        downloadManager.cancelDownload(id)
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun setFilterTab(tab: DownloadFilter) {
        _uiState.value = _uiState.value.copy(filterTab = tab)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun openPlayer(download: DownloadEntity) {
        _uiState.value = _uiState.value.copy(playingDownload = download)
    }

    fun closePlayer() {
        _uiState.value = _uiState.value.copy(playingDownload = null)
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(showSettings = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(showSettings = false)
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    fun getDownloadDirectoryPath(): String {
        return downloadManager.getDownloadDirectory().absolutePath
    }

    fun getDownloadDirectoryDisplayName(): String {
        return downloadManager.getDownloadDirectoryDisplayName()
    }

    fun getStorageLocationOptions(): List<StorageLocationOption> {
        return downloadManager.getAvailableStorageLocations()
    }

    fun getSelectedStorageLocationKey(): String {
        return downloadManager.getSelectedDownloadDirectoryKey()
    }

    fun setStorageLocation(key: String) {
        downloadManager.setDownloadDirectoryKey(key)
        _uiState.value = _uiState.value.copy(
            infoMessage = "Speicherort geändert: ${downloadManager.getDownloadDirectoryDisplayName()}"
        )
    }

    fun setCustomDirectoryUri(uri: Uri, folderName: String) {
        downloadManager.setCustomTreeUri(uri, folderName)
        _uiState.value = _uiState.value.copy(
            infoMessage = "Eigener Ordner gewählt: $folderName"
        )
    }
}

class MainViewModelFactory(
    private val repository: DownloadRepository,
    private val extractorEngine: YtDlpExtractorEngine,
    private val downloadManager: DownloadEngineManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository, extractorEngine, downloadManager) as T
    }
}
