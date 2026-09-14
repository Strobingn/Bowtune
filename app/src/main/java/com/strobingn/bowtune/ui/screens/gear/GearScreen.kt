package com.strobingn.bowtune.ui.screens.gear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.BowSetup
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GearScreen() {
    val context = LocalContext.current
    val dao = remember {
        (context.applicationContext as BowTuneApp).database.bowSetupDao()
    }
    val setups by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(null as BowSetup?) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gear / Bow Setups") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showEditor = true
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add setup")
            }
        }
    ) { padding ->
        if (setups.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No bow setups yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap + to log a Mathews, PSE, Hoyt, Bowtech, or Elite setup with limb shift, yoke, and cam notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(setups, key = { it.id }) { setup ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            editing = setup
                            showEditor = true
                        }
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        setup.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        listOf(setup.brand, setup.model)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" · ")
                                            .ifBlank { "Unspecified brand/model" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { scope.launch { dao.delete(setup) } }
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                            val meta = buildList {
                                if (setup.drawWeightLbs.isNotBlank()) add("${setup.drawWeightLbs} lb")
                                if (setup.drawLengthIn.isNotBlank()) add("${setup.drawLengthIn}\" DL")
                                if (setup.rest.isNotBlank()) add("Rest: ${setup.rest}")
                                if (setup.limbShiftSetting.isNotBlank()) add("Limb shift: ${setup.limbShiftSetting}")
                            }
                            if (meta.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        SetupEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { setup ->
                scope.launch {
                    dao.upsert(setup)
                    showEditor = false
                }
            }
        )
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
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
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
                            camSystem = cam.trim()
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
