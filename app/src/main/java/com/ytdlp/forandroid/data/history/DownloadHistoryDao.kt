package com.ytdlp.forandroid.data.history

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadHistoryDao {
    @Insert
    suspend fun insert(record: DownloadRecord): Long

    @Query("SELECT * FROM download_history ORDER BY timestamp DESC")
    fun all(): Flow<List<DownloadRecord>>

    @Delete
    suspend fun delete(record: DownloadRecord)

    @Query("DELETE FROM download_history")
    suspend fun clear()
}
