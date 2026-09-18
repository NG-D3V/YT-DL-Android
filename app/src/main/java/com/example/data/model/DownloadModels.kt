package com.example.data.model

enum class DownloadFormat(val displayName: String, val extension: String, val mimeType: String) {
    MP4("Video (MP4)", "mp4", "video/mp4"),
    MP3("Audio (MP3)", "mp3", "audio/mpeg")
}

enum class VideoQuality(
    val label: String,
    val resolution: String,
    val isAudioOnly: Boolean,
    val height: Int,
    val approxBitrateKbps: Int
) {
    RES_1080P("1080p (Full HD)", "1080p", false, 1080, 4500),
    RES_720P("720p (HD)", "720p", false, 720, 2500),
    RES_480P("480p (SD)", "480p", false, 480, 1200),
    RES_360P("360p (Data Saver)", "360p", false, 360, 600),

    AUDIO_320K("320 kbps (High)", "320kbps", true, 0, 320),
    AUDIO_192K("192 kbps (Medium)", "192kbps", true, 0, 192),
    AUDIO_128K("128 kbps (Standard)", "128kbps", true, 0, 128);

    companion object {
        fun videoQualities() = listOf(RES_1080P, RES_720P, RES_480P, RES_360P)
        fun audioQualities() = listOf(AUDIO_320K, AUDIO_192K, AUDIO_128K)

        fun fromString(value: String): VideoQuality {
            return entries.find { it.name == value || it.label == value || it.resolution == value } ?: RES_720P
        }
    }
}

enum class DownloadStatus(val label: String) {
    QUEUED("In Warteschlange"),
    DOWNLOADING("Wird heruntergeladen"),
    PAUSED("Pausiert"),
    COMPLETED("Abgeschlossen"),
    FAILED("Fehlgeschlagen"),
    CANCELLED("Abgebrochen")
}

data class VideoStreamOption(
    val quality: VideoQuality,
    val format: DownloadFormat,
    val directUrl: String,
    val approximateSizeBytes: Long = 0,
    val container: String = "mp4",
    val isAdaptive: Boolean = false
)

data class VideoMetadata(
    val id: String,
    val title: String,
    val author: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val viewCount: Long = 0,
    val availableStreams: List<VideoStreamOption> = emptyList(),
    val originalUrl: String
)

data class DownloadProgressState(
    val id: Long,
    val status: DownloadStatus,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
    val errorMessage: String? = null
)
