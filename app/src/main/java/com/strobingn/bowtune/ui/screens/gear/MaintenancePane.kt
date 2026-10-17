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
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.BowSetup
import com.strobingn.bowtune.data.ChartData
import com.strobingn.bowtune.data.MaintenanceLog
import com.strobingn.bowtune.ui.charts.EventTimeline
import com.strobingn.bowtune.ui.charts.ProgressRing
import com.strobingn.bowtune.ui.charts.TimelineEvent
import com.strobingn.bowtune.ui.common.ChartCard
import com.strobingn.bowtune.ui.common.EmptyState
import com.strobingn.bowtune.ui.common.FormSection
import com.strobingn.bowtune.ui.common.LabeledField
import com.strobingn.bowtune.ui.common.NumberSlider
import com.strobingn.bowtune.ui.common.SegmentedChoice
import com.strobingn.bowtune.ui.common.daysFromNow
import com.strobingn.bowtune.ui.common.formatDay
import com.strobingn.bowtune.ui.common.formatWhen
import com.strobingn.bowtune.ui.common.shortDate
import kotlinx.coroutines.launch

private val SERVICE_KINDS = listOf(MaintenanceLog.CAM_TIMING, MaintenanceLog.CABLE_STRETCH)

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
    var dueDays by remember { mutableStateOf<Double?>(30.0) }
    val now = System.currentTimeMillis()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ChartCard(title = "Service countdown", subtitle = "Days until the next check (ring = 30-day window)") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val due = listOf(
                        "Cam timing" to logs.firstOrNull { it.kind == MaintenanceLog.CAM_TIMING }?.nextDueEpochMs,
                        "Cable stretch" to logs.firstOrNull { it.kind == MaintenanceLog.CABLE_STRETCH }?.nextDueEpochMs,
                        "Next tune" to active?.nextTuneDueEpochMs
                    )
                    due.forEach { (label, at) ->
                        val days = at?.let { ChartData.daysUntil(it, now) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ProgressRing(
                                progress = (days ?: 0L).coerceIn(0L, 30L) / 30f,
                                size = 76.dp,
                                stroke = 8.dp,
                                centerText = when {
                                    days == null -> "—"
                                    days < 0 -> "Due"
                                    else -> "${days}d"
                                },
                                caption = label,
                                dashedTrack = days == null
                            )
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        item {
            FormSection("Cam timing / cable stretch") {
                Text(
                    "Active: ${active?.name ?: "none"}. Last cam note: ${active?.camTimingNotes?.ifBlank { "—" }}. Last cable: ${active?.cableStretchNotes?.ifBlank { "—" }}.",
                    style = MaterialTheme.typography.bodySmall
                )
                SegmentedChoice(
                    options = listOf(MaintenanceLog.CAM_TIMING to "Cam timing", MaintenanceLog.CABLE_STRETCH to "Cable stretch"),
                    selected = kind,
                    onSelect = { kind = it }
                )
                LabeledField(notes, { notes = it }, "What you checked / changed", singleLine = false)
                NumberSlider(
                    label = "Next check in",
                    value = dueDays,
                    onValueChange = { dueDays = it },
                    min = 0.0,
                    max = 180.0,
                    step = 1.0,
                    unit = "days",
                    defaultValue = 30.0,
                    supporting = dueDays?.let { "Due ${formatDay(daysFromNow(it.toInt()))}" } ?: "No reminder"
                )
                Button(
                    onClick = {
                        val stamp = System.currentTimeMillis()
                        val due = dueDays?.let { daysFromNow(it.toInt()) }
                        scope.launch {
                            dao.upsert(
                                MaintenanceLog(
                                    setupName = active?.name.orEmpty(),
                                    kind = kind,
                                    dateEpochMs = stamp,
                                    notes = notes.trim(),
                                    nextDueEpochMs = due
                                )
                            )
                            active?.let { setup ->
                                val updated = when (kind) {
                                    MaintenanceLog.CAM_TIMING -> setup.copy(
                                        lastCamCheckEpochMs = stamp,
                                        camTimingNotes = notes.trim().ifBlank { setup.camTimingNotes },
                                        nextTuneDueEpochMs = due ?: setup.nextTuneDueEpochMs
                                    )
                                    else -> setup.copy(
                                        lastCableCheckEpochMs = stamp,
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
        }
        if (logs.isEmpty()) {
            item {
                Column(Modifier.padding(8.dp)) {
                    EmptyState("No service notes", "Log cam-sync checks and cable stretch after a string job or leftover paper tears.")
                }
            }
        } else {
            item {
                ChartCard(title = "Service history") {
                    EventTimeline(
                        rows = SERVICE_KINDS.map { MaintenanceLog.label(it) },
                        events = logs.mapNotNull { log ->
                            val row = SERVICE_KINDS.indexOf(log.kind)
                            if (row < 0) null else TimelineEvent(row, log.dateEpochMs)
                        },
                        dateLabel = { shortDate(it) }
                    )
                }
            }
            items(logs, key = { it.id }) { log ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
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
