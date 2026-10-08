package com.ytdlp.forandroid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ytdlp.forandroid.engine.DownloadState
import com.ytdlp.forandroid.engine.FORMAT_PRESETS
import com.ytdlp.forandroid.engine.FetchState
import com.ytdlp.forandroid.engine.UpdateChannel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel, modifier: Modifier = Modifier) {
    val url by vm.url.collectAsState()
    val fetch by vm.fetchState.collectAsState()
    val dl by vm.downloadState.collectAsState()
    val fmt by vm.selectedFormatId.collectAsState()
    val audioOnly by vm.audioOnly.collectAsState()
    val aria by vm.useAria2c.collectAsState()
    val customRaw by vm.customArgsRaw.collectAsState()
    val log by vm.log.collectAsState()
    val engineBanner by vm.engineStatus.collectAsState()
    val openError by vm.openError.collectAsState()
    val context = LocalContext.current
    val version by vm.engineVersion.collectAsState()
    val channel by vm.updateChannel.collectAsState()
    val updating by vm.updating.collectAsState()

    Column(
        modifier
            .fillMaxSize()
            // Scaffold innerPadding (from the caller) already clears the
            // status bar, cutout and gesture nav on enforced edge-to-edge.
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Local yt-dlp engine + native UI. Paste URL → Fetch → Download.",
            style = MaterialTheme.typography.bodyMedium,
        )
        // F2: engine warmup / failure banner for share intents.
        engineBanner?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Expressive-style input: large rounded shape, leading icon,
        // supporting text and URL keyboard (M3 Expressive keeps the same
        // outlined/filled field structure, the modern look comes from
        // shape + icons + dynamic color).
        // See https://m3.material.io/components/text-fields/overview
        OutlinedTextField(
            value = url,
            onValueChange = { vm.url.value = it },
            label = { Text("Video URL") },
            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
            supportingText = { Text("Paste a video link, or share one from YouTube") },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.startDownload() }) {
                Text("Download")
            }
            OutlinedButton(onClick = { vm.fetch() }, enabled = fetch !is FetchState.Loading) {
                Text("Fetch info")
            }
            OutlinedButton(onClick = { vm.cancelDownload() }) {
                Text("Cancel")
            }
        }

        when (val f = fetch) {
            is FetchState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Spacer(Modifier.padding(4.dp))
                Text("Asking yt-dlp for formats (dump-json)…")
            }
            is FetchState.Ready -> {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(f.info.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOfNotNull(
                                f.info.uploader,
                                f.info.durationSec?.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')}" },
                                "${f.info.rawFormats} formats",
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        // F5: friendly presets first, raw -f ids behind an expander.
                        val preset by vm.selectedPreset.collectAsState()
                        val advanced by vm.showAdvancedFormats.collectAsState()
                        FORMAT_PRESETS.forEachIndexed { index, item ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { vm.selectPreset(index) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = preset == index,
                                    onClick = { vm.selectPreset(index) },
                                )
                                Text(item.label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { vm.showAdvancedFormats.value = !advanced }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (advanced) "Hide advanced formats" else "Advanced formats",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (advanced) {
                            FormatPicker(
                                options = listOf("best" to "best (auto)") +
                                    f.info.formats.map { it.formatId to it.label },
                                selected = fmt,
                                onSelect = { vm.selectAdvancedFormat(it) },
                            )
                        }
                    }
                }
            }
            is FetchState.Error -> Text("Fetch error: ${f.message}", color = MaterialTheme.colorScheme.error)
            FetchState.Idle -> {}
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = audioOnly, onCheckedChange = { vm.audioOnly.value = it })
            Text("Audio only (mp3)")
            Checkbox(checked = aria, onCheckedChange = { vm.useAria2c.value = it })
            Text("parallel (aria2c)")
        }

        OutlinedTextField(
            value = customRaw,
            onValueChange = { vm.customArgsRaw.value = it },
            label = { Text("Custom yt-dlp args (optional)") },
            placeholder = { Text("--embed-subs --embed-chapters") },
            leadingIcon = { Icon(Icons.Filled.Code, contentDescription = null) },
            supportingText = { Text("Raw flags passed straight to the yt-dlp engine") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        when (val d = dl) {
            is DownloadState.Running -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(progress = { d.progress }, modifier = Modifier.fillMaxWidth())
                Text("${(d.progress * 100).toInt()}% ${d.etaSec?.let { "· ETA ${it}s" } ?: ""}")
                Text(d.logLine, style = MaterialTheme.typography.bodySmall)
            }
            is DownloadState.Done -> {
                Text("Saved to: ${d.fileHint}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.openFile(context, d.fileHint) }) {
                        Text("Open file")
                    }
                }
            }
            is DownloadState.Error -> Text("Download error: ${d.message}", color = MaterialTheme.colorScheme.error)
            DownloadState.Cancelled -> Text("Cancelled.")
            DownloadState.Idle -> {}
        }
        // F4: viewer missing feedback.
        openError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("yt-dlp engine: ${version ?: "bundled (tap Update to check)"}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            vm.updateChannel.value =
                                if (channel == UpdateChannel.STABLE) UpdateChannel.NIGHTLY else UpdateChannel.STABLE
                        },
                    ) {
                        Text("Channel: ${channel.name}")
                    }
                    Button(onClick = { vm.updateEngine() }, enabled = !updating) {
                        Text(if (updating) "Updating…" else "Update yt-dlp")
                    }
                }
            }
        }

        Text("Log", style = MaterialTheme.typography.titleSmall)
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                log.ifBlank { "No log yet." },
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatPicker(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected } ?: options.firstOrNull()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = current?.second ?: selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Format (-f)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}
