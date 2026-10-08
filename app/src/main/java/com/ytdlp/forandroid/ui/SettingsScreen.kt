package com.ytdlp.forandroid.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ytdlp.forandroid.data.ThemeRepository
import com.ytdlp.forandroid.ui.theme.AppAccent
import com.ytdlp.forandroid.ui.theme.ThemeMode
import com.ytdlp.forandroid.util.Browser
import kotlinx.coroutines.launch

/** Same developer as Fosser, shared Ko-fi support link. */
private const val DONATE_URL = "https://ko-fi.com/kennethchoinfosec"

/**
 * Settings screen. Appearance section (theme mode + 8-circle accent picker +
 * match-system switch) is ported from Fosser's SettingsScreen
 * (https://github.com/Kenneth-Cho-InfoSec/Fosser). The activity provides the
 * TopAppBar/Scaffold; this is pure content.
 */
@Composable
fun SettingsScreen(
    themeRepo: ThemeRepository,
    modifier: Modifier = Modifier,
) {
    val theme by themeRepo.settings.collectAsState()
    var showThemePicker by remember { mutableStateOf(false) }
    var showAccentPicker by remember { mutableStateOf(false) }
    val systemAccentAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text("Theme") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    themeRepo.setMode(mode)
                                    showThemePicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = theme.mode == mode,
                                onClick = {
                                    themeRepo.setMode(mode)
                                    showThemePicker = false
                                },
                            )
                            Text(mode.displayName(), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePicker = false }) { Text("Close") }
            },
        )
    }

    if (showAccentPicker) {
        AlertDialog(
            onDismissRequest = { showAccentPicker = false },
            title = { Text("Accent colour") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(190.dp),
                ) {
                    items(AppAccent.entries) { accent ->
                        val selected = theme.accent == accent && !theme.matchSystemAccent
                        Box(
                            Modifier.padding(8.dp).size(64.dp)
                                .background(Color(accent.seedArgb), CircleShape)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                )
                                .clickable {
                                    themeRepo.setAccent(accent)
                                    showAccentPicker = false
                                }
                                .semantics { contentDescription = accent.displayName() },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccentPicker = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SettingsRow(
            title = "Theme",
            summary = theme.mode.displayName(),
            onClick = { showThemePicker = true },
        )
        SettingsRow(
            title = "Accent colour",
            summary = if (theme.matchSystemAccent) "Match system accent"
            else theme.accent.displayName(),
            onClick = { showAccentPicker = true },
        )
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Match system accent", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (systemAccentAvailable) "Follow your wallpaper colours (Android 12+)"
                    else "Requires Android 12 or later",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = theme.matchSystemAccent,
                enabled = systemAccentAvailable,
                onCheckedChange = { themeRepo.setMatchSystemAccent(it) },
                modifier = Modifier.semantics { contentDescription = "Match system accent" },
            )
        }

        Spacer(Modifier.height(8.dp))
        Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "ytdlp-for-android wraps the unmodified yt-dlp engine " +
                "(via youtubedl-android) in a native Material You interface. " +
                "Downloads land in Download/ytdlp-for-android/.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Theme system ported from Fosser (MIT).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsRow(
            title = "Developer",
            summary = "Kenneth-Cho-InfoSec, tap to support via Ko-fi",
            onClick = {
                if (!Browser.open(context, DONATE_URL)) {
                    uiScope.launch { snackbar.showSnackbar("Could not open the donation page.") }
                }
            },
        )
        Text(
            "Version 0.1.0 • com.ytdlp.forandroid",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        } // Scaffold content
    } // Scaffold
}

@Composable
private fun SettingsRow(title: String, summary: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Text(
            summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
