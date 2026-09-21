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
import com.strobingn.bowtune.ui.common.EmptyState
import com.strobingn.bowtune.ui.common.LabeledField
import com.strobingn.bowtune.ui.common.formatWhen
import kotlinx.coroutines.launch

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

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Limb Shift / rest / centershot log", fontWeight = FontWeight.SemiBold)
            Text(
                "One change per row. Active setup: ${activeName.ifBlank { "none" }}",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    AdjustmentLog.REST,
                    AdjustmentLog.LIMB_SHIFT,
                    AdjustmentLog.CENTERSHOT,
                    AdjustmentLog.NOCK,
                    AdjustmentLog.YOKE
                ).forEach { k ->
                    FilterChip(
                        selected = kind == k,
                        onClick = { kind = k },
                        label = { Text(AdjustmentLog.label(k)) }
                    )
                }
            }
            LabeledField(before, { before = it }, "Before")
            LabeledField(after, { after = it }, "After")
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
        if (logs.isEmpty()) {
            Column(Modifier.padding(24.dp)) {
                EmptyState("No adjustments yet", "Log rest clicks, Limb Shift, nock height, or yoke twists after each paper group.")
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
}
