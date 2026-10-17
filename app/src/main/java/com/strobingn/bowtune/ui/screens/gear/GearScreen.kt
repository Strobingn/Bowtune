package com.strobingn.bowtune.ui.screens.gear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.BowSetup
import com.strobingn.bowtune.data.ChartData
import com.strobingn.bowtune.data.toDoubleOrNullLenient
import com.strobingn.bowtune.ui.charts.ProgressRing
import com.strobingn.bowtune.ui.charts.rememberChartPalette
import com.strobingn.bowtune.ui.common.EmptyState
import com.strobingn.bowtune.ui.common.FormSection
import com.strobingn.bowtune.ui.common.NumberSlider
import com.strobingn.bowtune.ui.common.NumberSliderField
import com.strobingn.bowtune.ui.common.daysFromNow
import com.strobingn.bowtune.ui.common.formatDay
import kotlinx.coroutines.launch

private enum class GearPane(val label: String) {
    SETUPS("Setups"),
    ARROWS("Arrows"),
    LOGBOOK("Logbook"),
    COMPARE("Compare"),
    SERVICE("Service")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GearScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BowTuneApp
    val dao = remember { app.database.bowSetupDao() }
    val setups by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    val activeId by app.preferences.activeSetupId.collectAsStateWithLifecycle(0L)
    val scope = rememberCoroutineScope()
    var pane by rememberSaveable { mutableStateOf(GearPane.SETUPS) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BowSetup?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gear / Bow Setups", fontWeight = FontWeight.SemiBold) }) },
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
            ScrollableTabRow(
                selectedTabIndex = pane.ordinal,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                GearPane.entries.forEach { p ->
                    Tab(
                        selected = pane == p,
                        onClick = { pane = p },
                        text = { Text(p.label) },
                        icon = {
                            Icon(
                                when (p) {
                                    GearPane.SETUPS -> Icons.Filled.Tune
                                    GearPane.ARROWS -> Icons.Filled.Straighten
                                    GearPane.LOGBOOK -> Icons.AutoMirrored.Filled.ListAlt
                                    GearPane.COMPARE -> Icons.AutoMirrored.Filled.CompareArrows
                                    GearPane.SERVICE -> Icons.Filled.Build
                                },
                                contentDescription = null
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

/** Typical ranges used to scale the spec bars on setup cards. */
private data class SpecRange(val label: String, val unit: String, val min: Double, val max: Double)

private val DW_RANGE = SpecRange("Draw wt", "lb", 20.0, 90.0)
private val DL_RANGE = SpecRange("Draw len", "in", 24.0, 34.0)
private val IBO_RANGE = SpecRange("IBO", "fps", 250.0, 380.0)
private val CHRONO_RANGE = SpecRange("Chrono", "fps", 150.0, 420.0)

@Composable
private fun SpecBar(range: SpecRange, raw: String, modifier: Modifier = Modifier) {
    val palette = rememberChartPalette()
    val value = raw.toDoubleOrNullLenient()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(range.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (raw.isBlank()) "—" else "$raw ${range.unit}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
        ) {
            drawRoundRect(palette.track, cornerRadius = CornerRadius(3.dp.toPx()))
            if (value != null) {
                val f = ((value - range.min) / (range.max - range.min)).toFloat().coerceIn(0.02f, 1f)
                drawRoundRect(palette.ink, size = Size(size.width * f, size.height), cornerRadius = CornerRadius(3.dp.toPx()))
            }
        }
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
    val now = System.currentTimeMillis()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(setups, key = { it.id }) { setup ->
            val active = setup.id == activeId
            val cs = MaterialTheme.colorScheme
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onEdit(setup) },
                colors = CardDefaults.cardColors(containerColor = if (active) cs.surfaceContainerHighest else cs.surfaceContainerLow),
                border = if (active) CardDefaults.outlinedCardBorder().copy(width = 2.dp) else null
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            if (active) {
                                Text("ACTIVE PROFILE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(setup.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                listOf(setup.brand, setup.model).filter { it.isNotBlank() }.joinToString(" · ")
                                    .ifBlank { "Unspecified brand/model" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant
                            )
                        }
                        setup.nextTuneDueEpochMs?.let { due ->
                            val days = ChartData.daysUntil(due, now)
                            ProgressRing(
                                progress = (days.coerceIn(0L, 30L) / 30f),
                                size = 56.dp,
                                stroke = 6.dp,
                                centerText = if (days < 0) "Due" else "${days}d",
                                caption = "Next tune"
                            )
                        }
                        IconButton(onClick = { onDelete(setup) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SpecBar(DW_RANGE, setup.drawWeightLbs, Modifier.weight(1f))
                        SpecBar(DL_RANGE, setup.drawLengthIn, Modifier.weight(1f))
                        SpecBar(IBO_RANGE, setup.iboSpeedFps, Modifier.weight(1f))
                        SpecBar(CHRONO_RANGE, setup.arrowSpeedFps, Modifier.weight(1f))
                    }
                    val meta = buildList {
                        if (setup.rest.isNotBlank()) add("Rest: ${setup.rest}")
                        if (setup.limbShiftSetting.isNotBlank()) add("Limb shift: ${setup.limbShiftSetting}")
                        setup.nextTuneDueEpochMs?.let { add("Due ${formatDay(it)}") }
                    }
                    if (meta.isNotEmpty()) {
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var dueDays by remember { mutableStateOf<Double?>(null) }
    var camNotes by remember { mutableStateOf(initial?.camTimingNotes.orEmpty()) }
    var cableNotes by remember { mutableStateOf(initial?.cableStretchNotes.orEmpty()) }

    fun save() {
        val due = dueDays?.let { daysFromNow(it.toInt()) } ?: initial?.nextTuneDueEpochMs
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(if (initial == null) "New setup" else "Edit setup") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
                    },
                    actions = {
                        TextButton(onClick = ::save, enabled = name.isNotBlank()) { Text("Save", fontWeight = FontWeight.Bold) }
                    }
                )
            }
        ) { inner ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FormSection("Bow") {
                    OutlinedTextField(name, { name = it }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(brand, { brand = it }, label = { Text("Brand") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(model, { model = it }, label = { Text("Model") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(cam, { cam = it }, label = { Text("Cam system") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                FormSection("Specs") {
                    NumberSliderField("Draw weight", drawWeight, { drawWeight = it }, 20.0, 90.0, 0.5, unit = "lb", defaultValue = 60.0)
                    NumberSliderField("Draw length", drawLength, { drawLength = it }, 24.0, 34.0, 0.5, unit = "in", defaultValue = 28.5)
                    NumberSliderField("IBO / rated speed", ibo, { ibo = it }, 250.0, 380.0, 1.0, unit = "fps", defaultValue = 330.0)
                    NumberSliderField("Chrono arrow speed", speed, { speed = it }, 150.0, 420.0, 1.0, unit = "fps", defaultValue = 285.0)
                }
                FormSection("Accessories") {
                    OutlinedTextField(rest, { rest = it }, label = { Text("Rest") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(sight, { sight = it }, label = { Text("Sight") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(arrows, { arrows = it }, label = { Text("Arrows") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                FormSection("Tuning") {
                    OutlinedTextField(limbShift, { limbShift = it }, label = { Text("Limb shift / cable guard") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(yoke, { yoke = it }, label = { Text("Yoke twist notes") }, modifier = Modifier.fillMaxWidth())
                }
                FormSection("Service") {
                    NumberSlider(
                        label = "Next tune due in",
                        value = dueDays,
                        onValueChange = { dueDays = it },
                        min = 0.0,
                        max = 180.0,
                        step = 1.0,
                        unit = "days",
                        defaultValue = 30.0,
                        supporting = initial?.nextTuneDueEpochMs?.let { "Currently ${formatDay(it)} — leave unset to keep it." }
                            ?: "Optional reminder shown on Home."
                    )
                    OutlinedTextField(camNotes, { camNotes = it }, label = { Text("Cam timing notes") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(cableNotes, { cableNotes = it }, label = { Text("Cable stretch notes") }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = ::save, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save setup") }
                Box(Modifier.height(24.dp))
            }
        }
    }
}
