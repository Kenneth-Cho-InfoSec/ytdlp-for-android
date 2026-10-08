package com.ytdlp.forandroid.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ytdlp.forandroid.R
import com.ytdlp.forandroid.engine.YtDlpEngine

/**
 * F3: long running download worker. Runs the blocking yt-dlp engine call on a
 * background thread, promoted with setForeground so the system keeps it alive
 * past the 10 minute background limit. Type dataSync is the documented type
 * for upload/download work. Cancel flows through onStopped into the engine.
 */
class DownloadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL)?.trim().orEmpty()
        if (url.isBlank()) return Result.failure(workDataOf(KEY_ERROR to "Empty URL"))
        val processId = id.toString()
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW),
        )
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Downloading")
            .setContentText(url.take(80))
            .setOngoing(true)
            .setProgress(100, 0, false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
        setForeground(ForegroundInfo(NOTIF_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
        return try {
            val path = YtDlpEngine.download(
                url = url,
                formatId = inputData.getString(KEY_FORMAT),
                audioOnly = inputData.getBoolean(KEY_AUDIO_ONLY, false),
                useAria2c = inputData.getBoolean(KEY_ARIA2C, false),
                customArgs = inputData.getString(KEY_CUSTOM_ARGS)
                    ?.split(ARG_SEP)?.filter { it.isNotEmpty() } ?: emptyList(),
                outputDir = YtDlpEngine.defaultOutputDir(),
                processId = processId,
                onProgress = { progress, eta, line ->
                    val capped = line.trim().takeLast(300)
                    // Callback runs on the engine thread, so use the async variant.
                    setProgressAsync(workDataOf(KEY_PROGRESS to progress, KEY_ETA to eta, KEY_LINE to capped))
                    builder.setProgress(100, progress.toInt().coerceIn(0, 100), false)
                    if (capped.isNotEmpty()) builder.setContentText(capped.take(120))
                    manager.notify(NOTIF_ID, builder.build())
                },
            )
            Result.success(workDataOf(KEY_FILE_PATH to path))
        } catch (t: Throwable) {
            YtDlpEngine.cancel(processId)
            if (t is kotlinx.coroutines.CancellationException) throw t
            if (t.javaClass.simpleName.contains("Cancel", ignoreCase = true)) {
                Result.failure(workDataOf(KEY_ERROR to "__CANCELLED__"))
            } else {
                Result.failure(workDataOf(KEY_ERROR to (t.message ?: t.toString())))
            }
        }
    }

    // No onStopped override: CoroutineWorker seals it. Stop cleanup rides on
    // coroutine cancellation, caught above, which kills the engine process.

    companion object {
        const val NOTIF_ID = 1001
        const val CHANNEL_ID = "downloads"
        const val ARG_SEP = "\u001F"
        const val KEY_URL = "url"
        const val KEY_FORMAT = "format"
        const val KEY_AUDIO_ONLY = "audio_only"
        const val KEY_ARIA2C = "aria2c"
        const val KEY_CUSTOM_ARGS = "custom_args"
        const val KEY_PROGRESS = "progress"
        const val KEY_ETA = "eta"
        const val KEY_LINE = "line"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_ERROR = "error"
    }
}
