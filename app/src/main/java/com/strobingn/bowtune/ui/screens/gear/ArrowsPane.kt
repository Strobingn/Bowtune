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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.strobingn.bowtune.data.ArrowShaft
import com.strobingn.bowtune.data.BowSetup
import com.strobingn.bowtune.data.Calculators
import com.strobingn.bowtune.data.toDoubleOrNullLenient
import com.strobingn.bowtune.ui.common.EmptyState
import com.strobingn.bowtune.ui.common.HintCard
import com.strobingn.bowtune.ui.common.LabeledField
import kotlinx.coroutines.launch

@Composable
fun ArrowsPane(setups: List<BowSetup>, activeId: Long) {
    val context = LocalContext.current
    val dao = remember { (context.applicationContext as BowTuneApp).database.arrowShaftDao() }
    val arrows by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<ArrowShaft?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    val active = setups.firstOrNull { it.id == activeId }

    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = {
                editing = ArrowShaft(
                    name = "",
                    setupId = active?.id,
                    lengthIn = active?.drawLengthIn.orEmpty()
                )
                showEditor = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("  Add arrow")
        }
        if (arrows.isEmpty()) {
            Column(Modifier.padding(24.dp)) {
                EmptyState(
                    "No arrows in inventory",
                    "Log spine, length, and component weights. FOC is calculated from length + balance point."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(arrows, key = { it.id }) { arrow ->
                    val foc = Calculators.foc(
                        arrow.lengthIn.toDoubleOrNullLenient(),
                        arrow.balanceFromNockIn.toDoubleOrNullLenient(),
                        arrow.gpi.toDoubleOrNullLenient(),
                        arrow.pointWeightGr.toDoubleOrNullLenient(),
                        arrow.insertWeightGr.toDoubleOrNullLenient(),
                        arrow.nockWeightGr.toDoubleOrNullLenient(),
                        arrow.vaneWeightGr.toDoubleOrNullLenient(),
                        arrow.wrapWeightGr.toDoubleOrNullLenient()
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            editing = arrow
                            showEditor = true
                        }
                    ) {
                        Row(Modifier.padding(14.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(arrow.name, fontWeight = FontWeight.Bold)
                                Text(
                                    listOf(arrow.brand, "spine ${arrow.spine}", "${arrow.lengthIn}\"", "qty ${arrow.quantity}")
                                        .filter { it.isNotBlank() && it != "spine " && it != "\"\"}" }
                                        .joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(foc.note, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { scope.launch { dao.delete(arrow) } }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L && initial.name.isBlank()) "New arrow" else "Edit arrow") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LabeledField(name, { name = it }, "Name *")
                LabeledField(brand, { brand = it }, "Brand / model")
                LabeledField(spine, { spine = it }, "Spine")
                LabeledField(length, { length = it }, "Length (in)")
                LabeledField(gpi, { gpi = it }, "Shaft grains (or GPI × length)")
                LabeledField(point, { point = it }, "Point (gr)")
                LabeledField(insert, { insert = it }, "Insert (gr)")
                LabeledField(nock, { nock = it }, "Nock (gr)")
                LabeledField(vane, { vane = it }, "Vanes total (gr)")
                LabeledField(wrap, { wrap = it }, "Wrap (gr)")
                LabeledField(balance, { balance = it }, "Balance from nock (in)")
                LabeledField(qty, { qty = it }, "Quantity")
                LabeledField(notes, { notes = it }, "Notes", singleLine = false)
                if (setupNames.isNotEmpty()) {
                    Text("Linked setup", style = MaterialTheme.typography.labelLarge)
                    setupNames.forEach { setup ->
                        TextButton(onClick = { setupId = setup.id }) {
                            Text(if (setupId == setup.id) "● ${setup.name}" else setup.name)
                        }
                    }
                }
                HintCard(foc.note)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
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
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
