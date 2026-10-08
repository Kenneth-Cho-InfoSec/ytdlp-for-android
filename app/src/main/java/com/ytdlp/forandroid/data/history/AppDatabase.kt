package com.ytdlp.forandroid.data.history

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DownloadRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): DownloadHistoryDao
}
