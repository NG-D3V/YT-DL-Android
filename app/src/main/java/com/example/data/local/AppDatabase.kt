package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.DownloadFormat
import com.example.data.model.DownloadStatus
import com.example.data.model.VideoQuality

class Converters {
    @TypeConverter
    fun fromFormat(format: DownloadFormat): String = format.name

    @TypeConverter
    fun toFormat(value: String): DownloadFormat = runCatching {
        DownloadFormat.valueOf(value)
    }.getOrDefault(DownloadFormat.MP4)

    @TypeConverter
    fun fromQuality(quality: VideoQuality): String = quality.name

    @TypeConverter
    fun toQuality(value: String): VideoQuality = runCatching {
        VideoQuality.valueOf(value)
    }.getOrDefault(VideoQuality.RES_720P)

    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DownloadStatus = runCatching {
        DownloadStatus.valueOf(value)
    }.getOrDefault(DownloadStatus.QUEUED)
}

@Database(entities = [DownloadEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ytdl_local_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
