package com.strobingn.bowtune.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.strobingn.bowtune.data.BackupManager
import com.strobingn.bowtune.data.Calculators
import com.strobingn.bowtune.data.ChecklistTemplates
import com.strobingn.bowtune.data.CoachingLevel
import com.strobingn.bowtune.data.ReportBuilder
import com.strobingn.bowtune.data.TearType
import com.strobingn.bowtune.data.toDoubleOrNullLenient
import com.strobingn.bowtune.ui.common.HintCard
import com.strobingn.bowtune.ui.common.LabeledField
import com.strobingn.bowtune.ui.common.SectionLabel
import com.strobingn.bowtune.ui.common.formatDay
import com.strobingn.bowtune.ui.common.formatWhen
import com.strobingn.bowtune.ui.common.shareText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class HomeTool { DASH, FOC, SPINE, KE, BACKUP, SETTINGS, REPORT }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onOpenPaperTear: () -> Unit,
    onOpenGear: () -> Unit,
    onOpenChecklist: () -> Unit,
    onOpenSessions: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as BowTuneApp
    val scope = rememberCoroutineScope()
    val prefs = app.preferences
    val db = app.database

    val setups by db.bowSetupDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val sessions by db.tuneSessionDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val tears by db.paperTearLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val adjustments by db.adjustmentLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val arrows by db.arrowShaftDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val maintenance by db.maintenanceLogDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val activeId by prefs.activeSetupId.collectAsStateWithLifecycle(0L)
    val templateId by prefs.checklistTemplateId.collectAsStateWithLifecycle(ChecklistTemplates.FULL.id)
    val checked by app.checklistStore.checkedIds(templateId).collectAsStateWithLifecycle(emptySet())
    val coaching by prefs.coachingLevel.collectAsStateWithLifecycle(CoachingLevel.STANDARD)
    val haptic by prefs.hapticEnabled.collectAsStateWithLifecycle(true)
    val tts by prefs.ttsEnabled.collectAsStateWithLifecycle(false)
    val largeText by prefs.largeText.collectAsStateWithLifecycle(false)
    val highContrast by prefs.highContrast.collectAsStateWithLifecycle(false)

    val active = setups.firstOrNull { it.id == activeId }
    val template = ChecklistTemplates.byId(templateId)
    val percent = if (template.items.isEmpty()) 0 else (checked.size * 100 / template.items.size)
    val lastTear = tears.firstOrNull()
    val nextDue = listOfNotNull(active?.nextTuneDueEpochMs)
        .plus(maintenance.mapNotNull { it.nextDueEpochMs })
        .minOrNull()

    var tool by remember { mutableStateOf(HomeTool.DASH) }
    var status by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            status = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                        ?: error("Could not read file")
                }
                BackupManager.importJson(text, db, prefs, app.checklistStore)
                "Restore complete."
            }.getOrElse { "Restore failed: ${it.message}" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (tool) {
                            HomeTool.DASH -> "Home"
                            HomeTool.FOC -> "FOC calculator"
                            HomeTool.SPINE -> "Spine helper"
                            HomeTool.KE -> "KE & momentum"
                            HomeTool.BACKUP -> "Backup & restore"
                            HomeTool.SETTINGS -> "Coaching & access"
                            HomeTool.REPORT -> "Tuning report"
                        }
                    )
                },
                navigationIcon = {
                    if (tool != HomeTool.DASH) {
                        IconButton(onClick = { tool = HomeTool.DASH }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (tool == HomeTool.DASH) {
                        IconButton(onClick = { tool = HomeTool.SETTINGS }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (tool) {
            HomeTool.DASH -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    DashCard(
                        title = if (active == null) "No active bow" else active.name,
                        body = if (active == null) {
                            "Add a setup in Gear and tap Set active. Logs and calculators will use it."
                        } else {
                            listOf(active.brand, active.model, active.drawWeightLbs.takeIf { it.isNotBlank() }?.let { "$it lb" })
                                .filter { !it.isNullOrBlank() }
                                .joinToString(" · ")
                                .ifBlank { "Open Gear to edit draw weight, Limb Shift, and due dates." }
                        },
                        onClick = onOpenGear
                    )
                }
                item {
                    DashCard(
                        title = "Last paper tear",
                        body = lastTear?.let {
                            val label = TearType.entries.firstOrNull { t -> t.name == it.tearType }?.label
                                ?: it.tearType
                            "${formatWhen(it.dateEpochMs)} · $label · ${it.setupName.ifBlank { "no setup" }}"
                        } ?: "No tear logged yet. Diagnose on the Tear tab, then Save tear.",
                        onClick = onOpenPaperTear
                    )
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenChecklist)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "Checklist · ${template.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { percent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("$percent% complete · ${checked.size} of ${template.items.size}")
                        }
                    }
                }
                item {
                    DashCard(
                        title = "Next due",
                        body = nextDue?.let { "Next cam/tune check ${formatDay(it)}" }
                            ?: "No due date. Set one on a setup or a Service log in Gear.",
                        onClick = onOpenGear
                    )
                }
                item {
                    DashCard(
                        title = "Last session",
                        body = sessions.firstOrNull()?.let {
                            "${formatWhen(it.dateEpochMs)} · ${it.distanceYd} yd · ${it.scoreOrGroup.ifBlank { it.sessionKind }}"
                        } ?: "No sessions yet. Log a group in Logs.",
                        onClick = onOpenSessions
                    )
                }
                item { SectionLabel("Tools") }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "FOC" to HomeTool.FOC,
                            "Spine" to HomeTool.SPINE,
                            "KE" to HomeTool.KE,
                            "Report" to HomeTool.REPORT,
                            "Backup" to HomeTool.BACKUP
                        ).forEach { (label, dest) ->
                            FilterChip(
                                selected = false,
                                onClick = { tool = dest },
                                label = { Text(label) },
                                leadingIcon = { Icon(Icons.Filled.Calculate, contentDescription = null) }
                            )
                        }
                    }
                }
                item {
                    Text(
                        "Coaching: ${coaching.label}. Paper distance 4-6 ft. Grayscale theme stays put.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HomeTool.FOC -> ToolPane(padding) {
                FocTool(arrows.firstOrNull())
            }
            HomeTool.SPINE -> ToolPane(padding) {
                SpineTool(
                    defaultDw = active?.drawWeightLbs.orEmpty(),
                    defaultLen = active?.drawLengthIn.orEmpty()
                )
            }
            HomeTool.KE -> ToolPane(padding) {
                EnergyTool(defaultSpeed = active?.arrowSpeedFps?.ifBlank { active.iboSpeedFps }.orEmpty())
            }
            HomeTool.BACKUP -> ToolPane(padding) {
                Text("Export writes a JSON snapshot of setups, sessions, tears, arrows, logs, checklist, and preferences. Restore replaces local data.")
                status?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                val json = BackupManager.exportJson(db, prefs, app.checklistStore)
                                context.shareText("Bow Tune backup", json)
                                status = "Share sheet opened."
                            }.onFailure { status = it.message }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Export / share backup") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Restore from file") }
            }
            HomeTool.SETTINGS -> ToolPane(padding) {
                Text("Difficulty-adaptive coaching changes Paper Tear step depth and Vision tip density.")
                CoachingLevel.entries.forEach { level ->
                    FilterChip(
                        selected = coaching == level,
                        onClick = { scope.launch { prefs.setCoachingLevel(level) } },
                        label = { Text(level.label) }
                    )
                    if (coaching == level) {
                        Text(level.blurb, style = MaterialTheme.typography.bodySmall)
                    }
                }
                PrefSwitch("Haptic on Vision freeze / caution tips", haptic) {
                    scope.launch { prefs.setHapticEnabled(it) }
                }
                PrefSwitch("Read tear steps aloud when a tear is selected", tts) {
                    scope.launch { prefs.setTtsEnabled(it) }
                }
                PrefSwitch("Larger text", largeText) {
                    scope.launch { prefs.setLargeText(it) }
                }
                PrefSwitch("High-contrast grayscale", highContrast) {
                    scope.launch { prefs.setHighContrast(it) }
                }
            }
            HomeTool.REPORT -> ToolPane(padding) {
                val report = remember(active, sessions, tears, adjustments, arrows, maintenance, percent, template) {
                    ReportBuilder.build(
                        active, sessions, tears, adjustments, arrows, maintenance, percent, template.name
                    )
                }
                Button(
                    onClick = { context.shareText("Bow Tune report", report) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text("  Share report")
                }
                Text(report, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ToolPane(padding: PaddingValues, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

@Composable
private fun DashCard(title: String, body: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(body, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun PrefSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun FocTool(sample: com.strobingn.bowtune.data.ArrowShaft?) {
    var length by remember { mutableStateOf(sample?.lengthIn.orEmpty()) }
    var balance by remember { mutableStateOf(sample?.balanceFromNockIn.orEmpty()) }
    var shaft by remember { mutableStateOf(sample?.gpi.orEmpty()) }
    var point by remember { mutableStateOf(sample?.pointWeightGr.orEmpty()) }
    var insert by remember { mutableStateOf(sample?.insertWeightGr.orEmpty()) }
    var nock by remember { mutableStateOf(sample?.nockWeightGr.orEmpty()) }
    var vane by remember { mutableStateOf(sample?.vaneWeightGr.orEmpty()) }
    var wrap by remember { mutableStateOf(sample?.wrapWeightGr.orEmpty()) }
    val result = Calculators.foc(
        length.toDoubleOrNullLenient(),
        balance.toDoubleOrNullLenient(),
        shaft.toDoubleOrNullLenient(),
        point.toDoubleOrNullLenient(),
        insert.toDoubleOrNullLenient(),
        nock.toDoubleOrNullLenient(),
        vane.toDoubleOrNullLenient(),
        wrap.toDoubleOrNullLenient()
    )
    LabeledField(length, { length = it }, "Arrow length (in)")
    LabeledField(balance, { balance = it }, "Balance point from nock (in)")
    LabeledField(shaft, { shaft = it }, "Shaft grains (or GPI × length)")
    LabeledField(point, { point = it }, "Point (gr)")
    LabeledField(insert, { insert = it }, "Insert (gr)")
    LabeledField(nock, { nock = it }, "Nock (gr)")
    LabeledField(vane, { vane = it }, "Vanes total (gr)")
    LabeledField(wrap, { wrap = it }, "Wrap (gr)")
    HintCard(result.note)
}

@Composable
private fun SpineTool(defaultDw: String, defaultLen: String) {
    var dw by remember { mutableStateOf(defaultDw) }
    var length by remember { mutableStateOf(defaultLen) }
    var point by remember { mutableStateOf("100") }
    val result = Calculators.spine(
        dw.toDoubleOrNullLenient(),
        length.toDoubleOrNullLenient(),
        point.toDoubleOrNullLenient()
    )
    LabeledField(dw, { dw = it }, "Draw weight (lb)")
    LabeledField(length, { length = it }, "Arrow length (in)")
    LabeledField(point, { point = it }, "Point weight (gr)")
    Text(result.suggestedRange, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    result.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun EnergyTool(defaultSpeed: String) {
    var grains by remember { mutableStateOf("400") }
    var fps by remember { mutableStateOf(defaultSpeed) }
    val result = Calculators.energy(grains.toDoubleOrNullLenient(), fps.toDoubleOrNullLenient())
    LabeledField(grains, { grains = it }, "Finished arrow (gr)")
    LabeledField(fps, { fps = it }, "Speed (fps)")
    HintCard(result.note)
}
