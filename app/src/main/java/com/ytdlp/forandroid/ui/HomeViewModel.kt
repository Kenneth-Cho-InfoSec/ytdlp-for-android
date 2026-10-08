package com.ytdlp.forandroid.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ytdlp.forandroid.App
import com.ytdlp.forandroid.EngineState
import com.ytdlp.forandroid.data.history.DownloadRecord
import com.ytdlp.forandroid.engine.DownloadState
import com.ytdlp.forandroid.engine.FORMAT_PRESETS
import com.ytdlp.forandroid.engine.FetchState
import com.ytdlp.forandroid.engine.UpdateChannel
import com.ytdlp.forandroid.engine.YtDlpEngine
import com.ytdlp.forandroid.util.MediaFiles
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ytdlp.forandroid.worker.DownloadWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val historyDao = (app as App).historyDb.historyDao()

    /** F1: past downloads, newest first. Empty until the first success. */
    val history: StateFlow<List<DownloadRecord>> = historyDao.all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** F2: banner for engine warmup, null once ready or on failure text. */
    val engineStatus: MutableStateFlow<String?> = MutableStateFlow("Starting engine...")

    private var pendingAutoFetch = false

    val url = MutableStateFlow("")
    val fetchState: MutableStateFlow<FetchState> = MutableStateFlow(FetchState.Idle)
    val downloadState: MutableStateFlow<DownloadState> = MutableStateFlow(DownloadState.Idle)
    val selectedFormatId: MutableStateFlow<String> = MutableStateFlow("best")
    /** F5: preset index into FORMAT_PRESETS, or null when an advanced id is picked. */
    val selectedPreset: MutableStateFlow<Int?> = MutableStateFlow(0)
    val showAdvancedFormats: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val audioOnly: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val useAria2c: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val customArgsRaw: MutableStateFlow<String> = MutableStateFlow("")
    val log: MutableStateFlow<String> = MutableStateFlow("")
    val engineVersion: MutableStateFlow<String?> = MutableStateFlow(null)
    val updateChannel: MutableStateFlow<UpdateChannel> = MutableStateFlow(UpdateChannel.NIGHTLY)
    val updating: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val openError: MutableStateFlow<String?> = MutableStateFlow(null)

    private var downloadJob: Job? = null
    private var currentWorkId: UUID? = null

    val engineLog: StateFlow<String> = log.asStateFlow()

    init {
        refreshVersion()
        // F2: follow engine init; auto fetch a shared URL once ready.
        viewModelScope.launch {
            (getApplication<Application>() as App).engineState.collect { state ->
                engineStatus.value = when (state) {
                    EngineState.Starting -> "Starting engine..."
                    is EngineState.Ready -> {
                        if (state.version != null) engineVersion.value = state.version
                        null
                    }
                    is EngineState.Failed -> "Engine failed: ${state.message}"
                }
                if (state is EngineState.Ready && pendingAutoFetch &&
                    url.value.isNotBlank() && fetchState.value is FetchState.Idle
                ) {
                    pendingAutoFetch = false
                    fetch()
                }
            }
        }
    }

    fun prefillUrl(shared: String?, autoFetch: Boolean = false) {
        if (shared.isNullOrBlank()) return
        if (url.value.isBlank()) url.value = shared.trim()
        if (autoFetch && url.value.isNotBlank() && fetchState.value is FetchState.Idle) {
            // Engine may still start; the collector above picks this up.
            pendingAutoFetch = true
            if (engineStatus.value == null) {
                pendingAutoFetch = false
                fetch()
            }
        }
    }

    fun refreshVersion() {
        viewModelScope.launch {
            engineVersion.value = YtDlpEngine.version(getApplication())
        }
    }

    fun fetch() {
        val u = url.value.trim()
        if (u.isBlank()) {
            fetchState.value = FetchState.Error("Paste a video URL first.")
            return
        }
        openError.value = null
        downloadJob?.cancel()
        viewModelScope.launch {
            fetchState.value = FetchState.Loading
            downloadState.value = DownloadState.Idle
            try {
                val info = YtDlpEngine.fetchInfo(u)
                fetchState.value = FetchState.Ready(info)
                selectedFormatId.value = "best"
                selectedPreset.value = 0
                showAdvancedFormats.value = false
                appendLog("Fetched: ${info.title} (${info.rawFormats} formats)")
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                fetchState.value = FetchState.Error(t.message ?: t.toString())
                appendLog("Fetch failed: ${t.message}")
            }
        }
    }

    /** F3: enqueue a long running worker instead of blocking the UI scope. */
    fun startDownload() {
        val u = url.value.trim()
        if (u.isBlank()) {
            downloadState.value = DownloadState.Error("Paste a video URL first.")
            return
        }
        downloadJob?.cancel()
        currentWorkId?.let { WorkManager.getInstance(getApplication()).cancelWorkById(it) }
        openError.value = null
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_URL to u,
                    DownloadWorker.KEY_FORMAT to effectiveFormatSpec(),
                    DownloadWorker.KEY_AUDIO_ONLY to audioOnly.value,
                    DownloadWorker.KEY_ARIA2C to useAria2c.value,
                    DownloadWorker.KEY_CUSTOM_ARGS to YtDlpEngine.parseCustomArgs(customArgsRaw.value)
                        .joinToString(DownloadWorker.ARG_SEP),
                ),
            )
            .build()
        val wm = WorkManager.getInstance(getApplication<Application>())
        wm.enqueue(request)
        currentWorkId = request.id
        downloadJob = viewModelScope.launch {
            downloadState.value = DownloadState.Running(0f, null, "starting yt-dlp…")
            wm.getWorkInfoByIdFlow(request.id).collect { info ->
                if (info == null) return@collect
                when (info.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                        val p = info.progress.getFloat(DownloadWorker.KEY_PROGRESS, -1f)
                        if (p >= 0f) {
                            val line = info.progress.getString(DownloadWorker.KEY_LINE).orEmpty()
                            val eta = info.progress.getLong(DownloadWorker.KEY_ETA, -1)
                                .takeIf { it >= 0 }
                            downloadState.value = DownloadState.Running(
                                progress = (p / 100f).coerceIn(0f, 1f),
                                etaSec = eta,
                                logLine = line.ifBlank { "downloading…" },
                            )
                            if (line.isNotBlank()) appendLogThrottled(line)
                        }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val path = info.outputData.getString(DownloadWorker.KEY_FILE_PATH).orEmpty()
                        downloadState.value = DownloadState.Done(path.ifBlank { u })
                        appendLog("Done → $path")
                        viewModelScope.launch {
                            val title = (fetchState.value as? FetchState.Ready)?.info?.title ?: u
                            historyDao.insert(DownloadRecord(url = u, title = title, filePath = path))
                            // F4: surface the file to gallery/file apps.
                            runCatching {
                                MediaFiles.scan(getApplication(), java.io.File(path))
                            }
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        val err = info.outputData.getString(DownloadWorker.KEY_ERROR).orEmpty()
                        if (err == "__CANCELLED__") {
                            downloadState.value = DownloadState.Cancelled
                            appendLog("Cancelled.")
                        } else {
                            downloadState.value = DownloadState.Error(err.ifBlank { "Download failed" })
                            appendLog("Download failed: $err")
                        }
                    }
                    WorkInfo.State.CANCELLED -> {
                        downloadState.value = DownloadState.Cancelled
                        appendLog("Cancelled.")
                    }
                }
            }
        }
    }

    /** F5: preset spec wins unless the user picked an advanced id. */
    private fun effectiveFormatSpec(): String {
        val preset = selectedPreset.value
        if (preset != null && !audioOnly.value) {
            return FORMAT_PRESETS.getOrNull(preset)?.spec ?: "best"
        }
        return selectedFormatId.value
    }

    fun selectPreset(index: Int) {
        selectedPreset.value = index
    }

    fun selectAdvancedFormat(id: String) {
        selectedFormatId.value = id
        selectedPreset.value = null
    }

    fun cancelDownload() {
        currentWorkId?.let { WorkManager.getInstance(getApplication()).cancelWorkById(it) }
        downloadJob?.cancel()
    }

    /** F4: open a finished file in a viewer app. Sets openError when impossible. */
    fun openFile(context: android.content.Context, path: String) {
        openError.value = null
        val ok = runCatching { MediaFiles.open(context, java.io.File(path)) }.getOrDefault(false)
        if (!ok) openError.value = "No app can open this file."
    }

    fun clearOpenError() {
        openError.value = null
    }

    /** F1: fill the URL box from history and fetch its formats. */
    fun redownload(record: DownloadRecord) {
        downloadJob?.cancel()
        url.value = record.url
        fetchState.value = FetchState.Idle
        downloadState.value = DownloadState.Idle
        fetch()
    }

    fun deleteRecord(record: DownloadRecord) {
        viewModelScope.launch { historyDao.delete(record) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyDao.clear() }
    }

    fun updateEngine() {
        viewModelScope.launch {
            updating.value = true
            try {
                val res = YtDlpEngine.updateWithContext(getApplication(), updateChannel.value)
                appendLog("yt-dlp update: $res")
                refreshVersion()
            } catch (t: Throwable) {
                appendLog("Update failed: ${t.message}")
            } finally {
                updating.value = false
            }
        }
    }

    private var lastLogAt = 0L
    private fun appendLogThrottled(line: String) {
        val now = System.currentTimeMillis()
        if (now - lastLogAt < 1500) return // engine emits per-percent lines; don't flood UI
        lastLogAt = now
        appendLog(line)
    }

    private fun appendLog(line: String) {
        val trimmed = line.trim().takeLast(500)
        if (trimmed.isEmpty()) return
        val cur = log.value
        log.value = (if (cur.isEmpty()) trimmed else "$cur\n$trimmed").takeLastChars(6000)
    }

    private fun String.takeLastChars(n: Int): String =
        if (length <= n) this else substring(length - n)
}
