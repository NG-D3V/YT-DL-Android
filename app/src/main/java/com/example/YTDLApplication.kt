package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.DownloadRepository
import com.example.engine.DownloadEngineManager
import com.example.engine.YtDlpExtractorEngine

class YTDLApplication : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { DownloadRepository(database.downloadDao()) }
    val extractorEngine by lazy { YtDlpExtractorEngine() }
    val downloadManager by lazy { DownloadEngineManager(this, database.downloadDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: YTDLApplication
            private set
    }
}
