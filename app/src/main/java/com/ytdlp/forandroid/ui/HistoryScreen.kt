package com.ytdlp.forandroid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ytdlp.forandroid.data.history.DownloadRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** F1: past downloads with one tap redownload. */
@Composable
fun HistoryScreen(vm: HomeViewModel, modifier: Modifier = Modifier, onRedownload: () -> Unit = {}) {
    val records by vm.history.collectAsState()
    var showClear by remember { mutableStateOf(false) }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("Clear history?") },
            text = { Text("This removes all download records. Files on disk stay.") },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); showClear = false }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (records.isEmpty()) {
            Text(
                "No downloads yet. Finished downloads appear here for one tap redownload.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = { showClear = true }) { Text("Clear all") }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records, key = { it.id }) { record ->
                    HistoryRow(
                        record = record,
                        onRedownload = { vm.redownload(record); onRedownload() },
                        onDelete = { vm.deleteRecord(record) },
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Composable
private fun HistoryRow(
    record: DownloadRecord,
    onRedownload: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    record.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    dateFormat.format(Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRedownload) {
                Icon(Icons.Filled.Refresh, contentDescription = "Redownload")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete record")
            }
        }
    }
}
