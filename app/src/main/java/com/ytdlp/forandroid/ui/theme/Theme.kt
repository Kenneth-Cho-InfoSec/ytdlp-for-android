package com.ytdlp.forandroid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

/**
 * App theme, driven by [ThemeSettings]. Ported from Fosser's FosserTheme
 * (https://github.com/Kenneth-Cho-InfoSec/Fosser).
 *
 * - matchSystemAccent on Android 12+: framework Material You dynamic schemes.
 * - Otherwise a custom scheme built from the accent seed, in Light / Dark /
 *   true-Black variants. Recomposes on change, no activity recreation needed.
 */
@Composable
fun YtdlpTheme(
    settings: ThemeSettings,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = settings.mode.isDark(systemDark)
    val black = settings.mode == ThemeMode.BLACK
    val scheme = if (settings.matchSystemAccent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val dynamic = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (black) dynamic.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF141414),
            onSurfaceVariant = BrandMuted,
        ) else dynamic
    } else {
        customScheme(Color(settings.accent.seedArgb), dark, black)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Pure custom-scheme builder (unit-testable): accent seed + brand surfaces. */
fun customScheme(seed: Color, dark: Boolean, black: Boolean): ColorScheme {
    val onSeed: Color = if (seed.luminance() > 0.5f) BrandInk else Color.White
    return if (!dark) {
        lightColorScheme(
            primary = seed,
            onPrimary = onSeed,
            primaryContainer = lerp(seed, Color.White, 0.82f),
            onPrimaryContainer = lerp(seed, BrandInk, 0.65f),
            secondary = BrandAmber,
            onSecondary = BrandInk,
            tertiary = BrandSuccess,
            background = BrandMist,
            onBackground = BrandInk,
            surface = Color.White,
            onSurface = BrandInk,
            surfaceVariant = BrandCard,
            onSurfaceVariant = BrandMist,
        )
    } else if (black) {
        darkColorScheme(
            primary = lerp(seed, Color.White, 0.15f),
            onPrimary = BrandInk,
            primaryContainer = lerp(seed, Color.Black, 0.55f),
            onPrimaryContainer = lerp(seed, Color.White, 0.8f),
            secondary = BrandAmber,
            onSecondary = BrandInk,
            tertiary = BrandSuccess,
            background = Color.Black,
            onBackground = BrandMist,
            surface = Color.Black,
            onSurface = BrandMist,
            surfaceVariant = Color(0xFF141414),
            onSurfaceVariant = BrandMuted,
        )
    } else {
        darkColorScheme(
            primary = seed,
            onPrimary = onSeed,
            primaryContainer = lerp(seed, Color.Black, 0.55f),
            onPrimaryContainer = lerp(seed, Color.White, 0.8f),
            secondary = BrandAmber,
            onSecondary = BrandInk,
            tertiary = BrandSuccess,
            background = BrandInk,
            onBackground = BrandMist,
            surface = BrandSlate,
            onSurface = BrandMist,
            surfaceVariant = BrandCard,
            onSurfaceVariant = BrandMist,
        )
    }
}
