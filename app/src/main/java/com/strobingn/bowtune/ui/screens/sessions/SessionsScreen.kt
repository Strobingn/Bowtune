package com.strobingn.bowtune.ui.screens.sessions

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strobingn.bowtune.BowTuneApp
import com.strobingn.bowtune.data.BrandWizardCatalog
import com.strobingn.bowtune.data.ChecklistTemplates
import com.strobingn.bowtune.data.LiftVerticalTunePlan
import com.strobingn.bowtune.data.ReportBuilder
import com.strobingn.bowtune.data.SessionKind
import com.strobingn.bowtune.data.TuneHints
import com.strobingn.bowtune.data.TuneSession
import com.strobingn.bowtune.data.toDoubleOrNullLenient
import com.strobingn.bowtune.ui.common.EmptyState
import com.strobingn.bowtune.ui.common.HintCard
import com.strobingn.bowtune.ui.common.formatWhen
import com.strobingn.bowtune.ui.common.shareText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionsScreen(
    onOpenLiftVerticalTune: () -> Unit = {},
    onOpenWizard: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as BowTuneApp
    val dao = remember { app.database.tuneSessionDao() }
    val sessions by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    val setups by app.database.bowSetupDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val tears by app.database.paperTearLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val adjustments by app.database.adjustmentLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val arrows by app.database.arrowShaftDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val maintenance by app.database.maintenanceLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val activeId by app.preferences.activeSetupId.collectAsStateWithLifecycle(0L)
    val templateId by app.preferences.checklistTemplateId.collectAsStateWithLifecycle(ChecklistTemplates.FULL.id)
    val checked by app.checklistStore.checkedIds(templateId).collectAsStateWithLifecycle(emptySet())
    val scope = rememberCoroutineScope()
    var showEditor by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("all") }
    val active = setups.firstOrNull { it.id == activeId }
    val template = ChecklistTemplates.byId(templateId)
    val percent = if (template.items.isEmpty()) 0 else checked.size * 100 / template.items.size

    val visible = sessions.filter { filter == "all" || it.sessionKind == filter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tune Sessions") },
                actions = {
                    IconButton(
                        onClick = {
                            val report = ReportBuilder.build(
                                active, sessions, tears, adjustments, arrows, maintenance, percent, template.name
                            )
                            context.shareText("Bow Tune report", report)
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share tuning report")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add session")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                WizardCard(
                    title = LiftVerticalTunePlan.CARD_LABEL,
                    subtitle = "Guided bare-shaft-high vertical tune · ${LiftVerticalTunePlan.BOW}",
                    onClick = onOpenLiftVerticalTune
                )
            }
            items(BrandWizardCatalog.all, key = { it.id }) { wizard ->
                WizardCard(
                    title = wizard.title,
                    subtitle = wizard.summary,
                    onClick = { onOpenWizard(wizard.id) }
                )
            }
            item {
                Text("Filter", fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf("all" to "All") + SessionKind.all.map { it to SessionKind.label(it) }
                    filters.forEach { (id, label) ->
                        FilterChip(
                            selected = filter == id,
                            onClick = { filter = id },
                            label = { Text(label) }
                        )
                    }
                }
            }
            if (visible.isEmpty()) {
                item {
                    EmptyState(
                        "No sessions in this filter",
                        "Start a guided wizard above, or tap + to log a group, paper, bare shaft, walk-back, or broadhead comparison."
                    )
                }
            } else {
                items(visible, key = { it.id }) { session ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(formatWhen(session.dateEpochMs), fontWeight = FontWeight.Bold)
                                    Text(
                                        buildString {
                                            append(SessionKind.label(session.sessionKind))
                                            append(" · ")
                                            append(session.distanceYd.ifBlank { "?" })
                                            append(" yd")
                                            if (session.setupName.isNotBlank()) {
                                                append(" · ")
                                                append(session.setupName)
                                            }
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { scope.launch { dao.delete(session) } }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                            if (session.scoreOrGroup.isNotBlank()) {
                                Text("Group / score: ${session.scoreOrGroup}")
                            }
                            val cond = listOf(session.indoorOutdoor, session.windNote, session.tempNote)
                                .filter { it.isNotBlank() }
                            if (cond.isNotEmpty()) {
                                Text("Conditions: ${cond.joinToString(" · ")}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (session.tearType.isNotBlank()) {
                                Text("Tear: ${session.tearType}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (session.notes.isNotBlank()) {
                                Text(session.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        SessionEditorDialog(
            defaultSetup = active?.name.orEmpty(),
            onDismiss = { showEditor = false },
            onSave = { session ->
                scope.launch {
                    dao.upsert(session)
                    showEditor = false
                }
            }
        )
    }
}

@Composable
private fun WizardCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionEditorDialog(
    defaultSetup: String,
    onDismiss: () -> Unit,
    onSave: (TuneSession) -> Unit
) {
    var kind by remember { mutableStateOf(SessionKind.GROUP) }
    var distance by remember { mutableStateOf("20") }
    var score by remember { mutableStateOf("") }
    var setupName by remember { mutableStateOf(defaultSetup) }
    var notes by remember { mutableStateOf("") }
    var indoor by remember { mutableStateOf("indoor") }
    var wind by remember { mutableStateOf("") }
    var temp by remember { mutableStateOf("") }
    var tearType by remember { mutableStateOf("") }
    var bareV by remember { mutableStateOf("") }
    var bareH by remember { mutableStateOf("") }
    var fan by remember { mutableStateOf<Boolean?>(null) }
    var offset by remember { mutableStateOf<Boolean?>(null) }
    var bhH by remember { mutableStateOf("") }
    var bhV by remember { mutableStateOf("") }

    val hint = when (kind) {
        SessionKind.BARE_SHAFT -> TuneHints.bareShaft(bareV.toDoubleOrNullLenient(), bareH.toDoubleOrNullLenient())
        SessionKind.WALKBACK -> TuneHints.walkBack(fan, offset)
        SessionKind.BROADHEAD -> TuneHints.broadhead(bhH.toDoubleOrNullLenient(), bhV.toDoubleOrNullLenient())
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log session") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        SessionKind.GROUP,
                        SessionKind.PAPER,
                        SessionKind.BARE_SHAFT,
                        SessionKind.WALKBACK,
                        SessionKind.BROADHEAD
                    ).forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(SessionKind.label(k)) }
                        )
                    }
                }
                OutlinedTextField(distance, { distance = it }, label = { Text("Distance (yd)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(setupName, { setupName = it }, label = { Text("Setup name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(score, { score = it }, label = { Text("Score / group size") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Conditions", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = indoor == "indoor", onClick = { indoor = "indoor" }, label = { Text("Indoor") })
                    FilterChip(selected = indoor == "outdoor", onClick = { indoor = "outdoor" }, label = { Text("Outdoor") })
                }
                OutlinedTextField(wind, { wind = it }, label = { Text("Wind note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(temp, { temp = it }, label = { Text("Temp / weather") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (kind == SessionKind.PAPER) {
                    OutlinedTextField(tearType, { tearType = it }, label = { Text("Tear type") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if (kind == SessionKind.BARE_SHAFT) {
                    OutlinedTextField(bareV, { bareV = it }, label = { Text("Bare vs fletched high/low (in)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(bareH, { bareH = it }, label = { Text("Bare vs fletched left/right (in, +left)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if (kind == SessionKind.WALKBACK) {
                    Text("Did the error grow with distance?")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = fan == true, onClick = { fan = true }, label = { Text("Fan / grows") })
                        FilterChip(selected = fan == false, onClick = { fan = false }, label = { Text("No fan") })
                    }
                    Text("Whole line offset equally?")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = offset == true, onClick = { offset = true }, label = { Text("Parallel offset") })
                        FilterChip(selected = offset == false, onClick = { offset = false }, label = { Text("Not parallel") })
                    }
                }
                if (kind == SessionKind.BROADHEAD) {
                    OutlinedTextField(bhH, { bhH = it }, label = { Text("BH vs field left/right (in, +left)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(bhV, { bhV = it }, label = { Text("BH vs field high/low (in)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                hint?.let { HintCard(it) }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val extra = hint?.let { "Hint: $it" }.orEmpty()
                    onSave(
                        TuneSession(
                            dateEpochMs = System.currentTimeMillis(),
                            distanceYd = distance.trim(),
                            scoreOrGroup = score.trim(),
                            notes = listOf(notes.trim(), extra).filter { it.isNotBlank() }.joinToString("\n"),
                            setupName = setupName.trim(),
                            sessionKind = kind,
                            indoorOutdoor = indoor,
                            windNote = wind.trim(),
                            tempNote = temp.trim(),
                            tearType = tearType.trim()
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
