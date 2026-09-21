package com.strobingn.bowtune.ui.screens.gear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.BowSetup
import com.strobingn.bowtune.data.MaintenanceLog
import com.strobingn.bowtune.ui.common.EmptyState
import com.strobingn.bowtune.ui.common.LabeledField
import com.strobingn.bowtune.ui.common.daysFromNow
import com.strobingn.bowtune.ui.common.formatDay
import com.strobingn.bowtune.ui.common.formatWhen
import kotlinx.coroutines.launch

@Composable
fun MaintenancePane(setups: List<BowSetup>, activeId: Long) {
    val context = LocalContext.current
    val app = context.applicationContext as BowTuneApp
    val dao = remember { app.database.maintenanceLogDao() }
    val setupDao = remember { app.database.bowSetupDao() }
    val logs by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val active = setups.firstOrNull { it.id == activeId }

    var kind by remember { mutableStateOf(MaintenanceLog.CAM_TIMING) }
    var notes by remember { mutableStateOf("") }
    var dueDays by remember { mutableStateOf("30") }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cam timing / cable stretch", fontWeight = FontWeight.SemiBold)
            Text(
                "Active: ${active?.name ?: "none"}. Last cam note: ${active?.camTimingNotes?.ifBlank { "—" }}. Last cable: ${active?.cableStretchNotes?.ifBlank { "—" }}.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = kind == MaintenanceLog.CAM_TIMING,
                    onClick = { kind = MaintenanceLog.CAM_TIMING },
                    label = { Text("Cam timing") }
                )
                FilterChip(
                    selected = kind == MaintenanceLog.CABLE_STRETCH,
                    onClick = { kind = MaintenanceLog.CABLE_STRETCH },
                    label = { Text("Cable stretch") }
                )
            }
            LabeledField(notes, { notes = it }, "What you checked / changed", singleLine = false)
            LabeledField(dueDays, { dueDays = it }, "Next check in (days)")
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val due = dueDays.toIntOrNull()?.let { daysFromNow(it) }
                    scope.launch {
                        dao.upsert(
                            MaintenanceLog(
                                setupName = active?.name.orEmpty(),
                                kind = kind,
                                dateEpochMs = now,
                                notes = notes.trim(),
                                nextDueEpochMs = due
                            )
                        )
                        active?.let { setup ->
                            val updated = when (kind) {
                                MaintenanceLog.CAM_TIMING -> setup.copy(
                                    lastCamCheckEpochMs = now,
                                    camTimingNotes = notes.trim().ifBlank { setup.camTimingNotes },
                                    nextTuneDueEpochMs = due ?: setup.nextTuneDueEpochMs
                                )
                                else -> setup.copy(
                                    lastCableCheckEpochMs = now,
                                    cableStretchNotes = notes.trim().ifBlank { setup.cableStretchNotes },
                                    nextTuneDueEpochMs = due ?: setup.nextTuneDueEpochMs
                                )
                            }
                            setupDao.upsert(updated)
                        }
                        notes = ""
                    }
                },
                enabled = notes.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save service note") }
        }
        if (logs.isEmpty()) {
            Column(Modifier.padding(24.dp)) {
                EmptyState("No service notes", "Log cam-sync checks and cable stretch after a string job or leftover paper tears.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${MaintenanceLog.label(log.kind)} · ${log.setupName.ifBlank { "no setup" }}",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(formatWhen(log.dateEpochMs), style = MaterialTheme.typography.bodySmall)
                                Text(log.notes, style = MaterialTheme.typography.bodyMedium)
                                log.nextDueEpochMs?.let {
                                    Text("Next due ${formatDay(it)}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = { scope.launch { dao.delete(log) } }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
