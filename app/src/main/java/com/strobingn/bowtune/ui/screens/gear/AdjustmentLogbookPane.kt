package com.strobingn.bowtune.ui.screens.gear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.strobingn.bowtune.data.AdjustmentLog
import com.strobingn.bowtune.data.BowSetup
import com.strobingn.bowtune.ui.charts.BarChart
import com.strobingn.bowtune.ui.charts.BarDatum
import com.strobingn.bowtune.ui.charts.EventTimeline
import com.strobingn.bowtune.ui.charts.TimelineEvent
import com.strobingn.bowtune.ui.charts.seriesPatterns
import com.strobingn.bowtune.ui.common.ChartCard
import com.strobingn.bowtune.ui.common.EmptyState
import com.strobingn.bowtune.ui.common.FormSection
import com.strobingn.bowtune.ui.common.LabeledField
import com.strobingn.bowtune.ui.common.formatWhen
import com.strobingn.bowtune.ui.common.shortDate
import kotlinx.coroutines.launch

private val ADJUSTMENT_KINDS = listOf(
    AdjustmentLog.REST,
    AdjustmentLog.LIMB_SHIFT,
    AdjustmentLog.CENTERSHOT,
    AdjustmentLog.NOCK,
    AdjustmentLog.YOKE
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdjustmentLogbookPane(setups: List<BowSetup>, activeId: Long) {
    val context = LocalContext.current
    val dao = remember { (context.applicationContext as BowTuneApp).database.adjustmentLogDao() }
    val logs by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val activeName = setups.firstOrNull { it.id == activeId }?.name.orEmpty()

    var kind by remember { mutableStateOf(AdjustmentLog.REST) }
    var before by remember { mutableStateOf("") }
    var after by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (logs.isNotEmpty()) {
            item {
                ChartCard(title = "Adjustment timeline", subtitle = "One lane per adjustment type — spot when you chased one variable") {
                    EventTimeline(
                        rows = ADJUSTMENT_KINDS.map { AdjustmentLog.label(it) },
                        events = logs.mapNotNull { log ->
                            val row = ADJUSTMENT_KINDS.indexOf(log.kind)
                            if (row < 0) null else TimelineEvent(row, log.dateEpochMs)
                        },
                        dateLabel = { shortDate(it) }
                    )
                }
            }
            item {
                ChartCard(title = "Changes by type", subtitle = "${logs.size} logged") {
                    BarChart(
                        bars = ADJUSTMENT_KINDS.mapIndexed { i, k ->
                            BarDatum(
                                label = AdjustmentLog.label(k).substringBefore(' '),
                                value = logs.count { it.kind == k }.toDouble(),
                                pattern = seriesPatterns[i % seriesPatterns.size]
                            )
                        },
                        height = 140.dp,
                        valueFormatter = { it.toInt().toString() },
                        description = "Adjustment counts by type"
                    )
                }
            }
        }
        item {
            FormSection("Limb Shift / rest / centershot log") {
                Text(
                    "One change per row. Active setup: ${activeName.ifBlank { "none" }}",
                    style = MaterialTheme.typography.bodySmall
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ADJUSTMENT_KINDS.forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(AdjustmentLog.label(k)) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField(before, { before = it }, "Before", Modifier.weight(1f))
                    LabeledField(after, { after = it }, "After", Modifier.weight(1f))
                }
                LabeledField(notes, { notes = it }, "Notes", singleLine = false)
                Button(
                    onClick = {
                        scope.launch {
                            dao.upsert(
                                AdjustmentLog(
                                    dateEpochMs = System.currentTimeMillis(),
                                    setupName = activeName,
                                    kind = kind,
                                    beforeValue = before.trim(),
                                    afterValue = after.trim(),
                                    notes = notes.trim()
                                )
                            )
                            before = ""
                            after = ""
                            notes = ""
                        }
                    },
                    enabled = before.isNotBlank() || after.isNotBlank() || notes.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save adjustment") }
            }
        }
        if (logs.isEmpty()) {
            item {
                Column(Modifier.padding(8.dp)) {
                    EmptyState("No adjustments yet", "Log rest clicks, Limb Shift, nock height, or yoke twists after each paper group.")
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${AdjustmentLog.label(log.kind)} · ${log.setupName.ifBlank { "no setup" }}",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${formatWhen(log.dateEpochMs)} · ${log.beforeValue} → ${log.afterValue}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (log.notes.isNotBlank()) {
                                Text(log.notes, style = MaterialTheme.typography.bodySmall)
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
