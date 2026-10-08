package com.ytdlp.forandroid.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Shared browser opener, ported from Fosser's Browser
 * (https://github.com/Kenneth-Cho-InfoSec/Fosser).
 */
object Browser {
    fun open(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
