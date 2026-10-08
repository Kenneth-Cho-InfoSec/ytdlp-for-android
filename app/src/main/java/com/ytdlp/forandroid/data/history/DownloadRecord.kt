package com.ytdlp.forandroid.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One finished download. Shown in History with redownload support. */
@Entity(tableName = "download_history")
data class DownloadRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
)
