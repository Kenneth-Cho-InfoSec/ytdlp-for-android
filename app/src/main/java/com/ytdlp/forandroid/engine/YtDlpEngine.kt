package com.ytdlp.forandroid.engine

import android.content.Context
import android.os.Environment
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Thin wrapper around the unmodified yt-dlp engine shipped via youtubedl-android.
 *
 * Engine binary = CPython + yt-dlp (lazy-extractors) + ffmpeg.so + aria2c.so + quickjs,
 * executed as `python yt-dlp <args>`. See
 * https://github.com/yausername/youtubedl-android/blob/master/library/src/main/java/com/yausername/youtubedl_android/YoutubeDL.kt
 */
object YtDlpEngine {

    fun defaultOutputDir(): File {
        @Suppress("DEPRECATION")
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloads, "ytdlp-for-android").apply { if (!exists()) mkdirs() }
    }

    // Truncate titles to 150 bytes and append the video id: CJK/full titles
    // otherwise exceed the 255-byte filename limit (Errno 36, see issue on
    // youtube.com/shorts/vJ-ZaehFz3E). Output-template truncation
    // "%(title).150B" is documented in yt-dlp's OUTPUT TEMPLATE section.
    fun outputTemplate(outputDir: File = defaultOutputDir()): String =
        File(outputDir, "%(title).150B [%(id)s].%(ext)s").absolutePath

    suspend fun fetchInfo(url: String): MediaInfo = withContext(Dispatchers.IO) {
        val info = YoutubeDL.getInstance().getInfo(url.trim())
        info.toMediaInfo()
    }

    suspend fun download(
        url: String,
        formatId: String?,
        audioOnly: Boolean,
        useAria2c: Boolean,
        customArgs: List<String>,
        outputDir: File = defaultOutputDir(),
        processId: String = UUID.randomUUID().toString(),
        onProgress: (Float, Long, String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        outputDir.mkdirs()
        val startTime = System.currentTimeMillis()
        val request = YoutubeDLRequest(url.trim())
        request.addOption("-o", outputTemplate(outputDir))
        request.addOption("--no-mtime")

        when {
            audioOnly -> {
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--embed-thumbnail")
                request.addOption("--embed-metadata")
            }
            !formatId.isNullOrBlank() && formatId != "best" -> request.addOption("-f", formatId)
        }

        if (useAria2c) {
            request.addOption("--downloader", "libaria2c.so")
        }
        if (customArgs.isNotEmpty()) {
            request.addCommands(customArgs)
        }

        YoutubeDL.getInstance().execute(request, processId) { progress, eta, line ->
            onProgress(progress, eta, line ?: "")
        }
        // F4: return the actual file, not the folder, so History and Open
        // file point at something playable.
        val newest = outputDir.listFiles()
            ?.filter { it.isFile && it.lastModified() >= startTime - 2000 }
            ?.maxByOrNull { it.lastModified() }
        newest?.absolutePath ?: outputDir.absolutePath
    }

    fun cancel(processId: String): Boolean =
        YoutubeDL.getInstance().destroyProcessById(processId)

    suspend fun updateWithContext(context: Context, channel: UpdateChannel): String =
        withContext(Dispatchers.IO) {
            val libChannel = when (channel) {
                UpdateChannel.STABLE -> YoutubeDL.UpdateChannel.STABLE
                UpdateChannel.NIGHTLY -> YoutubeDL.UpdateChannel.NIGHTLY
            }
            val status = YoutubeDL.getInstance().updateYoutubeDL(context, libChannel)
            (status?.name ?: "UNKNOWN") + " @ " + (version(context) ?: "?")
        }

    fun version(context: Context): String? =
        try {
            YoutubeDL.getInstance().version(context)
        } catch (_: Throwable) {
            null
        }

    /** Split a raw custom-args string on whitespace, respecting simple double quotes. */
    fun parseCustomArgs(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        for (c in raw) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c.isWhitespace() && !inQuotes -> {
                    if (cur.isNotEmpty()) {
                        out += cur.toString()
                        cur.clear()
                    }
                }
                else -> cur.append(c)
            }
        }
        if (cur.isNotEmpty()) out += cur.toString()
        return out
    }

    private fun com.yausername.youtubedl_android.mapper.VideoInfo.toMediaInfo(): MediaInfo {
        val title = this.fulltitle ?: this.title ?: "(no title)"
        val fmts = (this.formats ?: arrayListOf()).mapNotNull { f ->
            val id = f.formatId ?: return@mapNotNull null
            val res = when {
                f.height > 0 && f.width > 0 -> "${f.width}x${f.height}"
                f.height > 0 -> "${f.height}p"
                else -> null
            }
            val label = listOfNotNull(
                id,
                f.ext,
                f.formatNote,
                res,
                f.vcodec?.takeIf { it != "none" },
                f.acodec?.takeIf { it != "none" },
            ).joinToString(" · ")
            FormatOption(
                formatId = id,
                label = label.ifBlank { id },
                ext = f.ext,
                resolution = res,
                fileSizeApprox = f.fileSizeApproximate.takeIf { it > 0 }
                    ?: f.fileSize.takeIf { it > 0 },
            )
        }
        // Most useful first: video+audio hybrids, then best resolution first.
        val sorted = fmts.sortedByDescending { it.resolution?.filter(Char::isDigit)?.toIntOrNull() ?: -1 }
        return MediaInfo(
            title = title,
            uploader = this.uploader,
            durationSec = this.duration.takeIf { it > 0 }?.toLong(),
            thumbnailUrl = this.thumbnail,
            formats = sorted.take(40),
            rawFormats = fmts.size,
        )
    }
}
