package com.strobingn.bowtune.ui.screens.gear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.BowSetup
import com.strobingn.bowtune.ui.common.EmptyState
import com.strobingn.bowtune.ui.common.daysFromNow
import com.strobingn.bowtune.ui.common.formatDay
import kotlinx.coroutines.launch

private enum class GearPane { SETUPS, ARROWS, LOGBOOK, COMPARE, SERVICE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GearScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BowTuneApp
    val dao = remember { app.database.bowSetupDao() }
    val setups by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    val activeId by app.preferences.activeSetupId.collectAsStateWithLifecycle(0L)
    val scope = rememberCoroutineScope()
    var pane by remember { mutableStateOf(GearPane.SETUPS) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BowSetup?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gear / Bow Setups") }) },
        floatingActionButton = {
            if (pane == GearPane.SETUPS) {
                FloatingActionButton(
                    onClick = {
                        editing = null
                        showEditor = true
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add setup")
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GearPane.entries.forEach { p ->
                    FilterChip(
                        selected = pane == p,
                        onClick = { pane = p },
                        label = {
                            Text(
                                when (p) {
                                    GearPane.SETUPS -> "Setups"
                                    GearPane.ARROWS -> "Arrows"
                                    GearPane.LOGBOOK -> "Logbook"
                                    GearPane.COMPARE -> "Compare"
                                    GearPane.SERVICE -> "Service"
                                }
                            )
                        }
                    )
                }
            }
            when (pane) {
                GearPane.SETUPS -> SetupList(
                    setups = setups,
                    activeId = activeId,
                    onEdit = {
                        editing = it
                        showEditor = true
                    },
                    onDelete = { scope.launch { dao.delete(it) } },
                    onActivate = { scope.launch { app.preferences.setActiveSetupId(it.id) } }
                )
                GearPane.ARROWS -> ArrowsPane(setups = setups, activeId = activeId)
                GearPane.LOGBOOK -> AdjustmentLogbookPane(setups = setups, activeId = activeId)
                GearPane.COMPARE -> CompareSetupsPane(setups)
                GearPane.SERVICE -> MaintenancePane(setups = setups, activeId = activeId)
            }
        }
    }

    if (showEditor) {
        SetupEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { setup ->
                scope.launch {
                    val id = dao.upsert(setup)
                    if (activeId == 0L) app.preferences.setActiveSetupId(id)
                    showEditor = false
                }
            }
        )
    }
}

@Composable
private fun SetupList(
    setups: List<BowSetup>,
    activeId: Long,
    onEdit: (BowSetup) -> Unit,
    onDelete: (BowSetup) -> Unit,
    onActivate: (BowSetup) -> Unit
) {
    if (setups.isEmpty()) {
        Column(Modifier.padding(24.dp)) {
            EmptyState(
                "No bow setups yet",
                "Tap + to log a Mathews, PSE, Hoyt, Bowtech, or Elite setup. Set one active so Home, Sessions, and Vision attach to it."
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(setups, key = { it.id }) { setup ->
            val active = setup.id == activeId
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onEdit(setup) }
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(setup.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                listOf(setup.brand, setup.model).filter { it.isNotBlank() }.joinToString(" · ")
                                    .ifBlank { "Unspecified brand/model" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (active) {
                                Text("ACTIVE PROFILE", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        IconButton(onClick = { onDelete(setup) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                    val meta = buildList {
                        if (setup.drawWeightLbs.isNotBlank()) add("${setup.drawWeightLbs} lb")
                        if (setup.drawLengthIn.isNotBlank()) add("${setup.drawLengthIn}\" DL")
                        if (setup.rest.isNotBlank()) add("Rest: ${setup.rest}")
                        if (setup.limbShiftSetting.isNotBlank()) add("Limb shift: ${setup.limbShiftSetting}")
                        setup.nextTuneDueEpochMs?.let { add("Due ${formatDay(it)}") }
                    }
                    if (meta.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    }
                    if (!active) {
                        OutlinedButton(onClick = { onActivate(setup) }) {
                            Icon(Icons.Filled.Star, contentDescription = null)
                            Text("  Set active")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupEditorDialog(
    initial: BowSetup?,
    onDismiss: () -> Unit,
    onSave: (BowSetup) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var brand by remember { mutableStateOf(initial?.brand.orEmpty()) }
    var model by remember { mutableStateOf(initial?.model.orEmpty()) }
    var drawWeight by remember { mutableStateOf(initial?.drawWeightLbs.orEmpty()) }
    var drawLength by remember { mutableStateOf(initial?.drawLengthIn.orEmpty()) }
    var rest by remember { mutableStateOf(initial?.rest.orEmpty()) }
    var sight by remember { mutableStateOf(initial?.sight.orEmpty()) }
    var arrows by remember { mutableStateOf(initial?.arrows.orEmpty()) }
    var limbShift by remember { mutableStateOf(initial?.limbShiftSetting.orEmpty()) }
    var yoke by remember { mutableStateOf(initial?.yokeTwistNotes.orEmpty()) }
    var cam by remember { mutableStateOf(initial?.camSystem.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var ibo by remember { mutableStateOf(initial?.iboSpeedFps.orEmpty()) }
    var speed by remember { mutableStateOf(initial?.arrowSpeedFps.orEmpty()) }
    var dueDays by remember { mutableStateOf("") }
    var camNotes by remember { mutableStateOf(initial?.camTimingNotes.orEmpty()) }
    var cableNotes by remember { mutableStateOf(initial?.cableStretchNotes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New setup" else "Edit setup") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(brand, { brand = it }, label = { Text("Brand") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(model, { model = it }, label = { Text("Model") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(drawWeight, { drawWeight = it }, label = { Text("Draw weight (lb)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(drawLength, { drawLength = it }, label = { Text("Draw length (in)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(rest, { rest = it }, label = { Text("Rest") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(sight, { sight = it }, label = { Text("Sight") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(arrows, { arrows = it }, label = { Text("Arrows") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(limbShift, { limbShift = it }, label = { Text("Limb shift / cable guard") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(yoke, { yoke = it }, label = { Text("Yoke twist notes") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(cam, { cam = it }, label = { Text("Cam system") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(ibo, { ibo = it }, label = { Text("IBO / rated speed (fps)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(speed, { speed = it }, label = { Text("Chrono arrow speed (fps)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    dueDays,
                    { dueDays = it },
                    label = { Text("Next tune due in (days)") },
                    placeholder = {
                        Text(
                            initial?.nextTuneDueEpochMs?.let { "Currently ${formatDay(it)}" } ?: "e.g. 30"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(camNotes, { camNotes = it }, label = { Text("Cam timing notes") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(cableNotes, { cableNotes = it }, label = { Text("Cable stretch notes") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val due = dueDays.toIntOrNull()?.let { daysFromNow(it) }
                        ?: initial?.nextTuneDueEpochMs
                    onSave(
                        BowSetup(
                            id = initial?.id ?: 0,
                            name = name.trim(),
                            brand = brand.trim(),
                            model = model.trim(),
                            drawWeightLbs = drawWeight.trim(),
                            drawLengthIn = drawLength.trim(),
                            rest = rest.trim(),
                            sight = sight.trim(),
                            arrows = arrows.trim(),
                            notes = notes.trim(),
                            limbShiftSetting = limbShift.trim(),
                            yokeTwistNotes = yoke.trim(),
                            camSystem = cam.trim(),
                            nextTuneDueEpochMs = due,
                            iboSpeedFps = ibo.trim(),
                            arrowSpeedFps = speed.trim(),
                            lastCamCheckEpochMs = initial?.lastCamCheckEpochMs,
                            camTimingNotes = camNotes.trim(),
                            lastCableCheckEpochMs = initial?.lastCableCheckEpochMs,
                            cableStretchNotes = cableNotes.trim()
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
