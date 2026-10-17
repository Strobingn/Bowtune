package com.strobingn.bowtune.ui.screens.gear

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.ArrowShaft
import com.strobingn.bowtune.data.BowSetup
import com.strobingn.bowtune.data.Calculators
import com.strobingn.bowtune.data.FocResult
import com.strobingn.bowtune.data.formatNumber
import com.strobingn.bowtune.data.strictNumberOrNull
import com.strobingn.bowtune.data.toDoubleOrNullLenient
import com.strobingn.bowtune.ui.charts.ArcGauge
import com.strobingn.bowtune.ui.charts.ArrowWeightBreakdown
import com.strobingn.bowtune.ui.charts.BarChart
import com.strobingn.bowtune.ui.charts.BarDatum
import com.strobingn.bowtune.ui.charts.FocBands
import com.strobingn.bowtune.ui.charts.LineStyle
import com.strobingn.bowtune.ui.charts.ReferenceLine
import com.strobingn.bowtune.ui.charts.StackedBar
import com.strobingn.bowtune.ui.charts.arrowSegments
import com.strobingn.bowtune.ui.charts.seriesPatterns
import com.strobingn.bowtune.ui.common.ChartCard
import com.strobingn.bowtune.ui.common.EmptyState
import com.strobingn.bowtune.ui.common.FormSection
import com.strobingn.bowtune.ui.common.HintCard
import com.strobingn.bowtune.ui.common.IntStepper
import com.strobingn.bowtune.ui.common.LabeledField
import com.strobingn.bowtune.ui.common.NumberSliderField
import kotlinx.coroutines.launch

private fun focFor(arrow: ArrowShaft): FocResult = Calculators.foc(
    arrow.lengthIn.toDoubleOrNullLenient(),
    arrow.balanceFromNockIn.toDoubleOrNullLenient(),
    arrow.gpi.toDoubleOrNullLenient(),
    arrow.pointWeightGr.toDoubleOrNullLenient(),
    arrow.insertWeightGr.toDoubleOrNullLenient(),
    arrow.nockWeightGr.toDoubleOrNullLenient(),
    arrow.vaneWeightGr.toDoubleOrNullLenient(),
    arrow.wrapWeightGr.toDoubleOrNullLenient()
)

@Composable
fun ArrowsPane(setups: List<BowSetup>, activeId: Long) {
    val context = LocalContext.current
    val dao = remember { (context.applicationContext as BowTuneApp).database.arrowShaftDao() }
    val arrows by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<ArrowShaft?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    val active = setups.firstOrNull { it.id == activeId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = {
                    editing = ArrowShaft(
                        name = "",
                        setupId = active?.id,
                        lengthIn = active?.drawLengthIn.orEmpty()
                    )
                    showEditor = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add arrow")
            }
        }
        if (arrows.isEmpty()) {
            item {
                Column(Modifier.padding(8.dp)) {
                    EmptyState(
                        "No arrows in inventory",
                        "Log spine, length, and component weights. FOC is calculated from length + balance point."
                    )
                }
            }
        } else {
            val withFoc = arrows.map { it to focFor(it) }
            item {
                ChartCard(title = "FOC by arrow", subtitle = "Reference lines mark the 6 / 10 / 15% band edges") {
                    BarChart(
                        bars = withFoc.mapIndexed { i, (arrow, foc) ->
                            BarDatum(
                                label = arrow.name.take(10),
                                value = foc.focPercent ?: 0.0,
                                pattern = seriesPatterns[i % seriesPatterns.size],
                                valueLabel = foc.focPercent?.let { formatNumber(it, 1) + "%" } ?: "—"
                            )
                        },
                        references = listOf(
                            ReferenceLine(6.0, "6%", LineStyle.DOTTED),
                            ReferenceLine(10.0, "10%", LineStyle.DASHED),
                            ReferenceLine(15.0, "15%", LineStyle.DASH_DOT)
                        ),
                        description = "FOC percent per arrow"
                    )
                }
            }
            item {
                ChartCard(title = "Finished weight by arrow", subtitle = "Sum of logged components (grains)") {
                    BarChart(
                        bars = withFoc.mapIndexed { i, (arrow, foc) ->
                            BarDatum(
                                label = arrow.name.take(10),
                                value = foc.totalGrains ?: 0.0,
                                pattern = seriesPatterns[(i + 1) % seriesPatterns.size]
                            )
                        },
                        valueFormatter = { formatNumber(it, 0) },
                        description = "Total grains per arrow"
                    )
                }
            }
            items(arrows, key = { it.id }) { arrow ->
                val foc = focFor(arrow)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        editing = arrow
                        showEditor = true
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(arrow.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    listOf(
                                        arrow.brand,
                                        arrow.spine.takeIf { it.isNotBlank() }?.let { "spine $it" },
                                        arrow.lengthIn.takeIf { it.isNotBlank() }?.let { "$it\"" },
                                        arrow.quantity.takeIf { it.isNotBlank() }?.let { "qty $it" }
                                    ).filter { !it.isNullOrBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    foc.focPercent?.let { formatNumber(it, 1) + "%" } ?: "—",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("FOC", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { scope.launch { dao.delete(arrow) } }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                        val segs = arrowSegments(arrow)
                        if (segs.isNotEmpty()) {
                            StackedBar(segs, height = 14.dp, unit = " gr", showLegend = false)
                        }
                        Text(foc.note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showEditor && editing != null) {
        ArrowEditor(
            initial = editing!!,
            setupNames = setups,
            onDismiss = { showEditor = false },
            onSave = { arrow ->
                scope.launch {
                    dao.upsert(arrow)
                    showEditor = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArrowEditor(
    initial: ArrowShaft,
    setupNames: List<BowSetup>,
    onDismiss: () -> Unit,
    onSave: (ArrowShaft) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var brand by remember { mutableStateOf(initial.brand) }
    var spine by remember { mutableStateOf(initial.spine) }
    var length by remember { mutableStateOf(initial.lengthIn) }
    var gpi by remember { mutableStateOf(initial.gpi) }
    var point by remember { mutableStateOf(initial.pointWeightGr) }
    var insert by remember { mutableStateOf(initial.insertWeightGr) }
    var nock by remember { mutableStateOf(initial.nockWeightGr) }
    var vane by remember { mutableStateOf(initial.vaneWeightGr) }
    var wrap by remember { mutableStateOf(initial.wrapWeightGr) }
    var balance by remember { mutableStateOf(initial.balanceFromNockIn) }
    var qty by remember { mutableStateOf(initial.quantity) }
    var notes by remember { mutableStateOf(initial.notes) }
    var setupId by remember { mutableStateOf(initial.setupId) }
    val foc = Calculators.foc(
        length.toDoubleOrNullLenient(),
        balance.toDoubleOrNullLenient(),
        gpi.toDoubleOrNullLenient(),
        point.toDoubleOrNullLenient(),
        insert.toDoubleOrNullLenient(),
        nock.toDoubleOrNullLenient(),
        vane.toDoubleOrNullLenient(),
        wrap.toDoubleOrNullLenient()
    )

    fun save() {
        onSave(
            initial.copy(
                name = name.trim(),
                brand = brand.trim(),
                spine = spine.trim(),
                lengthIn = length.trim(),
                gpi = gpi.trim(),
                pointWeightGr = point.trim(),
                insertWeightGr = insert.trim(),
                nockWeightGr = nock.trim(),
                vaneWeightGr = vane.trim(),
                wrapWeightGr = wrap.trim(),
                balanceFromNockIn = balance.trim(),
                quantity = qty.trim(),
                notes = notes.trim(),
                setupId = setupId
            )
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(if (initial.id == 0L && initial.name.isBlank()) "New arrow" else "Edit arrow") },
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
                ArcGauge(
                    value = foc.focPercent,
                    min = 0.0,
                    max = 25.0,
                    bands = FocBands,
                    unit = "%",
                    caption = "Front of center"
                )
                ArrowWeightBreakdown(
                    arrowSegments(
                        gpi.toDoubleOrNullLenient(),
                        point.toDoubleOrNullLenient(),
                        insert.toDoubleOrNullLenient(),
                        nock.toDoubleOrNullLenient(),
                        vane.toDoubleOrNullLenient(),
                        wrap.toDoubleOrNullLenient()
                    )
                )
                HintCard(foc.note)
                FormSection("Arrow") {
                    LabeledField(name, { name = it }, "Name *")
                    LabeledField(brand, { brand = it }, "Brand / model")
                    LabeledField(spine, { spine = it }, "Spine")
                    NumberSliderField("Length", length, { length = it }, 24.0, 34.0, 0.125, unit = "in", defaultValue = 28.5)
                    NumberSliderField(
                        "Balance from nock",
                        balance,
                        { balance = it },
                        8.0,
                        24.0,
                        0.125,
                        unit = "in",
                        defaultValue = (length.toDoubleOrNullLenient() ?: 28.5) * 0.6
                    )
                    IntStepper(
                        label = "Quantity",
                        value = strictNumberOrNull(qty)?.toInt(),
                        onValueChange = { qty = it?.toString().orEmpty() },
                        min = 0,
                        max = 99,
                        defaultValue = 12
                    )
                }
                FormSection("Component weights") {
                    NumberSliderField("Shaft (GPI × length)", gpi, { gpi = it }, 100.0, 600.0, 1.0, unit = "gr", defaultValue = 270.0)
                    NumberSliderField("Point", point, { point = it }, 50.0, 300.0, 5.0, unit = "gr", defaultValue = 100.0)
                    NumberSliderField("Insert", insert, { insert = it }, 0.0, 120.0, 1.0, unit = "gr", defaultValue = 16.0)
                    NumberSliderField("Nock", nock, { nock = it }, 0.0, 40.0, 0.5, unit = "gr", defaultValue = 10.0)
                    NumberSliderField("Vanes total", vane, { vane = it }, 0.0, 60.0, 0.5, unit = "gr", defaultValue = 18.0)
                    NumberSliderField("Wrap", wrap, { wrap = it }, 0.0, 40.0, 0.5, unit = "gr", defaultValue = 0.0)
                }
                if (setupNames.isNotEmpty()) {
                    FormSection("Linked setup") {
                        setupNames.forEach { setup ->
                            FilterChip(
                                selected = setupId == setup.id,
                                onClick = { setupId = setup.id },
                                label = { Text(setup.name) }
                            )
                        }
                    }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = ::save, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save arrow") }
                Box(Modifier.height(24.dp))
            }
        }
    }
}
