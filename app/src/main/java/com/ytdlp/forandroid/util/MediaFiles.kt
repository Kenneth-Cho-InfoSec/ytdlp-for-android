package com.ytdlp.forandroid.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

/**
 * F4: make finished downloads visible to gallery/file apps and openable.
 * scanFile hands the file to the media scanner; openFile serves it through
 * FileProvider (direct file:// URIs crash since Android 7).
 */
object MediaFiles {

    fun scan(context: Context, file: File) {
        if (!file.isFile) return
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
    }

    /** Returns false when no app can open the file. */
    fun open(context: Context, file: File): Boolean {
        if (!file.isFile) return false
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val mime = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(file.extension.lowercase())
                ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
