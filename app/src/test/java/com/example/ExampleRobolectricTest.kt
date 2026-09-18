package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DownloadFormat
import com.example.data.model.VideoQuality
import com.example.engine.YtDlpExtractorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("YT Downloader", appName)
    }

    @Test
    fun `extract video ID correctly from multiple YouTube URL formats`() {
        val engine = YtDlpExtractorEngine()
        assertEquals("dQw4w9WgXcQ", engine.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", engine.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", engine.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", engine.extractVideoId("dQw4w9WgXcQ"))
    }

    @Test
    fun `verify quality and format definitions`() {
        val videoQualities = VideoQuality.videoQualities()
        assertEquals(4, videoQualities.size)
        assertEquals("1080p", VideoQuality.RES_1080P.resolution)
        assertEquals("720p", VideoQuality.RES_720P.resolution)
        assertEquals("480p", VideoQuality.RES_480P.resolution)
        assertEquals("360p", VideoQuality.RES_360P.resolution)

        val audioQualities = VideoQuality.audioQualities()
        assertEquals(3, audioQualities.size)
        assertEquals(DownloadFormat.MP4.extension, "mp4")
        assertEquals(DownloadFormat.MP3.extension, "mp3")
    }
}
