package com.ytdlp.forandroid

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import com.ytdlp.forandroid.data.SharedPrefsStore
import com.ytdlp.forandroid.data.ThemeRepository
import com.ytdlp.forandroid.data.history.AppDatabase
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Engine init state, so the UI can show warmup progress and auto fetch (F2). */
sealed interface EngineState {
    data object Starting : EngineState
    data class Ready(val version: String?) : EngineState
    data class Failed(val message: String) : EngineState
}

class App : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val themeRepo: ThemeRepository by lazy {
        ThemeRepository(
            SharedPrefsStore(
                getSharedPreferences(ThemeRepository.PREFS_NAME, Context.MODE_PRIVATE),
            ),
        )
    }

    val historyDb: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "ytdlp-history.db").build()
    }

    private val _engineState = MutableStateFlow<EngineState>(EngineState.Starting)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        // Heavy native extraction (Python stdlib + yt-dlp + ffmpeg), off main thread.
        appScope.launch {
            try {
                YoutubeDL.getInstance().init(this@App)
                FFmpeg.getInstance().init(this@App)
                Aria2c.getInstance().init(this@App)
                val v = YoutubeDL.getInstance().version(this@App)
                _engineState.value = EngineState.Ready(v)
                Log.i(TAG, "yt-dlp engine ready: $v")
            } catch (t: Throwable) {
                _engineState.value = EngineState.Failed(t.message ?: t.toString())
                Log.e(TAG, "failed to init youtubedl-android", t)
            }
        }
    }

    companion object {
        private const val TAG = "ytdlp-for-android"
    }
}
