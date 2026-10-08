package com.ytdlp.forandroid.ui.theme

/**
 * Theme settings, ported from Fosser's ThemeSettings
 * (https://github.com/Kenneth-Cho-InfoSec/Fosser).
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    BLACK,
    SYSTEM;

    fun displayName(): String = when (this) {
        LIGHT -> "Light"
        DARK -> "Dark"
        BLACK -> "Black (AMOLED)"
        SYSTEM -> "System default"
    }

    /** Resolve SYSTEM against the OS night mode. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        BLACK -> true
        SYSTEM -> systemDark
    }

    companion object {
        fun fromOrdinal(value: Int): ThemeMode = entries.getOrNull(value) ?: SYSTEM
    }
}

/**
 * Accent palettes for the circle picker. 8 seeds, same set as Fosser.
 */
enum class AppAccent(val seedArgb: Long) {
    TEAL(0xFF0E7C6B),
    BLUE(0xFF2563EB),
    INDIGO(0xFF4F46E5),
    PURPLE(0xFF7C3AED),
    PINK(0xFFDB2777),
    RED(0xFFDC2626),
    ORANGE(0xFFEA580C),
    GREEN(0xFF16A34A);

    fun displayName(): String = when (this) {
        TEAL -> "Teal"
        BLUE -> "Blue"
        INDIGO -> "Indigo"
        PURPLE -> "Purple"
        PINK -> "Pink"
        RED -> "Red"
        ORANGE -> "Orange"
        GREEN -> "Green"
    }

    companion object {
        fun fromOrdinal(value: Int): AppAccent = entries.getOrNull(value) ?: TEAL
    }
}

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AppAccent = AppAccent.TEAL,
    val matchSystemAccent: Boolean = true,
)
