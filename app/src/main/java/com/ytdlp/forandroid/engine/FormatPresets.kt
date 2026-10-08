package com.ytdlp.forandroid.engine

/**
 * F5: friendly quality rows over the raw -f id list. Specs use yt-dlp
 * format selection filters, so the engine call is unchanged.
 */
data class FormatPreset(val label: String, val spec: String?)

val FORMAT_PRESETS = listOf(
    FormatPreset("Best quality", null),
    FormatPreset("Full HD (up to 1080p)", "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best"),
    FormatPreset("HD (up to 720p)", "bestvideo[height<=720]+bestaudio/best[height<=720]/best"),
    FormatPreset("SD (up to 480p)", "bestvideo[height<=480]+bestaudio/best[height<=480]/best"),
)
