package com.ytdlp.forandroid.engine

/** Small UI-friendly models; engine itself returns youtubedl-android types. */
data class FormatOption(
    val formatId: String,
    val label: String,
    val ext: String?,
    val resolution: String?,
    val fileSizeApprox: Long?,
)

data class MediaInfo(
    val title: String,
    val uploader: String?,
    val durationSec: Long?,
    val thumbnailUrl: String?,
    val formats: List<FormatOption>,
    val rawFormats: Int,
)

enum class UpdateChannel { STABLE, NIGHTLY }

sealed interface FetchState {
    data object Idle : FetchState
    data object Loading : FetchState
    data class Ready(val info: MediaInfo) : FetchState
    data class Error(val message: String) : FetchState
}

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Running(val progress: Float, val etaSec: Long?, val logLine: String) : DownloadState
    data class Done(val fileHint: String) : DownloadState
    data class Error(val message: String) : DownloadState
    data object Cancelled : DownloadState
}
