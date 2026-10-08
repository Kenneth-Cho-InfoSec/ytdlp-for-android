# ytdlp-for-android

Native Android UI wrapper around unmodified **yt-dlp** running locally on device.

* Engine: [`youtubedl-android`](https://github.com/yausername/youtubedl-android) (bundled CPython plus yt-dlp lazy extractors build plus `ffmpeg.so` plus `aria2c.so` plus quickjs), the same stack as [Seal](https://github.com/JunkFood02/Seal)
* UI: Kotlin plus Jetpack Compose Material 3, single activity
* Flow: paste URL or share from YouTube, Fetch for formats, pick quality, Download to `Download/ytdlp-for-android/`

yt-dlp itself is not forked. It ships as a versioned Maven artifact and stays updateable in app through Stable and Nightly channels.

## Features

* URL download with format picker: Best quality, 1080p, 720p, 480p presets plus an Advanced expander with raw `-f` ids
* Audio only mode (mp3 with thumbnail and metadata embedded)
* Optional parallel downloading through aria2c
* Custom yt-dlp args field for power users
* Background downloads through a long running WorkManager worker with progress notification and Cancel action, so killing the app does not kill the download
* Download history (Room database) with one tap redownload, per row delete, and Clear all
* Share to app: sharing a link from YouTube prefills the URL and auto fetches once the engine is warm
* Engine updater with Stable and Nightly channels (Nightly recommended, Stable goes stale fast)
* Open file button on completion, with media scan so gallery and file apps see the file
* Settings screen: theme mode (Light, Dark, Black AMOLED, System default), 8 color accent picker with circles, Match system accent switch for Material You dynamic color on Android 12 plus, persisted in SharedPreferences
* Support row for the developer (Kenneth-Cho-InfoSec) via Ko-fi, shared with [Fosser](https://github.com/Kenneth-Cho-InfoSec/Fosser)

## Project layout

```
settings.gradle.kts / build.gradle.kts / gradle/libs.versions.toml
app/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/res/ (mipmaps incl. adaptive icon, notification icon, FileProvider paths)
  src/main/java/com/ytdlp/forandroid/
    App.kt                  # engine init state, ThemeRepository, Room database
    MainActivity.kt         # edge to edge, theme, Scaffold nav (Home, History, Settings)
    engine/YtDlpEngine.kt   # thin wrapper: fetch info, download, cancel, update
    engine/FormatPresets.kt # friendly quality rows over raw -f ids
    engine/DownloadState.kt # UI models
    worker/DownloadWorker.kt# long running download with notification
    data/history/           # Room entity, DAO, database
    data/ThemeRepository.kt # persisted theme settings as StateFlow
    ui/HomeScreen.kt        # URL input, format picker, progress, log
    ui/HistoryScreen.kt     # past downloads, redownload, delete
    ui/HomeViewModel.kt     # orchestrates engine, worker, history
    ui/SettingsScreen.kt    # theme, accent, system match, Ko-fi row
    ui/theme/               # ThemeSettings, YtdlpTheme, palette (ported from Fosser)
    util/Browser.kt         # external link opener
    util/MediaFiles.kt      # media scan plus FileProvider open
```

## Requirements

* JDK 17 or later (tested with Temurin 21)
* Android SDK with `platforms;android-36` and `build-tools;36.0.0`
* `sdk.dir` in `local.properties` (git ignored) or `ANDROID_HOME`
* Device or emulator, `arm64-v8a` recommended (all four ABIs are built, arm64 covers nearly all phones)

## Build and install

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

First launch extracts Python plus yt-dlp (about 40 to 80 MB per ABI, one time). Grant notifications on Android 13 plus to see download progress.

ABI split APKs land in `app/build/outputs/apk/debug/`. Install the `arm64-v8a` one on most phones.

## Update yt-dlp

Home screen engine card, pick the channel, tap Update yt-dlp. Nightly is recommended for daily use because YouTube changes break Stable often.

## Licensing

* yt-dlp core is [Unlicense](https://github.com/yt-dlp/yt-dlp/blob/master/LICENSE) (public domain)
* The theme system is ported from [Fosser](https://github.com/Kenneth-Cho-InfoSec/Fosser) (MIT) by the same developer
* This app's own code is MIT, see `LICENSE`. Note: once bundled with yt-dlp, Python, and ffmpeg binaries through youtubedl-android, the shipped APK pulls in GPLv3 components, so treat distributed APKs under GPL-3.0 terms
* Not affiliated with yt-dlp, Seal, or YTDLnis. For YouTube ToS reasons prefer GitHub Releases or F-Droid over the Play Store

## Roadmap

* [x] Engine plus single screen download with format picker and updater
* [x] Fosser style theme system with settings menu
* [x] Download history with redownload
* [x] Share to app with auto fetch
* [x] Background downloads with notification
* [x] Media scan plus Open file
* [x] Friendly format presets
* [ ] Playlist handling and queue of multiple URLs
* [ ] SponsorBlock, chapters, and subtitle toggles in the UI
* [ ] Cookie import for private videos
* [ ] Plugin APK model for Python, ffmpeg, and Node (like YTDLnis) to shrink the base APK
