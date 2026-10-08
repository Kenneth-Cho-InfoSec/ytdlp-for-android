# Changelog

## 0.1.0, first release

Native Android wrapper around unmodified yt-dlp running locally on device.

Included:

* URL download with quality presets (Best, 1080p, 720p, 480p) plus raw format ids behind Advanced
* Audio only mode, parallel downloading through aria2c, custom yt-dlp args field
* Background downloads with progress notification and Cancel, downloads survive the app being closed
* Download history with one tap redownload (Room database)
* Share to app from YouTube with auto fetch
* In app yt-dlp updater, Stable and Nightly channels
* Open file button with media scan on completion
* Settings with Light, Dark, Black AMOLED, and System theme modes, 8 accent colors, and Match system accent on Android 12 plus
* Ko-fi support row for the developer

Install:

* Most phones including Pixel 8: `app-arm64-v8a-release.apk`
* Unsure or other architectures: `app-universal-release.apk` (large, contains all ABIs)
* Requires Android 8.0 or later. First launch extracts the Python plus yt-dlp engine, allow about a minute.

Release signing, self signed RSA-4096, valid 2026 to 2056:

* SHA-256: `26:B9:3F:8B:25:EE:F8:E6:62:B9:50:A0:30:2B:0E:A5:56:6B:C3:B2:7B:35:3D:F4:26:A6:8B:E3:ED:26:F0:A8`

Note: the release app id is `com.ytdlp.forandroid` (no `.debug` suffix), so it installs next to the debug build instead of replacing it. History and settings start fresh.
